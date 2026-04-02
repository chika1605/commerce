package com.pro.messageservice.service;

import com.pro.messageservice.dto.MessageRequestDto;
import com.pro.messageservice.dto.MessageResponseDto;
import com.pro.messageservice.dto.MessageVerifyDto;
import com.pro.messageservice.dto.OtpSession;
import com.pro.messageservice.model.SenderType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageSenderService {

    private final MessageRedisService messageRedisService;
    private final RateLimitService rateLimitService;
    private final MessageSenderFactory messageSenderFactory;
    private final PasswordEncoder passwordEncoder;

    public void sendMessage(String senderType, String taget, String message) {
        SenderType type = SenderType.valueOf(senderType.toUpperCase());
        messageSenderFactory.getStrategy(type).send(taget, message);
    }

    public MessageResponseDto send(MessageRequestDto dto, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        SenderType senderType = getSenderTypeFromString(dto.getChannel());

        if (!rateLimitService.checkIpRateLimit(ip)) {
            throw new RuntimeException("IP_RATE_LIMIT_EXCEEDED");
        }

        if (!rateLimitService.checkTargetRateLimit(senderType, dto.getTarget())) {
            throw new RuntimeException("TARGET_RATE_LIMIT_EXCEEDED");
        }

        if (!messageRedisService.acquireLock(senderType, dto.getTarget())) {
            throw new RuntimeException("ACTIVE_SESSION_EXISTS");
        }

        String otp = generateOtp();
        String uuid = generateUUID();
        String otpHash = passwordEncoder.encode(otp);

        OtpSession session = new OtpSession(senderType.getId(), dto.getTarget(), otpHash, 0, false);
        messageRedisService.saveSession(uuid, session);

        try {
            messageSenderFactory.getStrategy(senderType).send(dto.getTarget(), otp);
        } catch (Exception e) {
            messageRedisService.deleteSession(uuid);
            messageRedisService.releaseLock(senderType, dto.getTarget());
            throw e;
        }

        MessageResponseDto response = new MessageResponseDto();
        response.setUuid(uuid);
        return response;
    }


    public boolean verify(String uuid, MessageVerifyDto dto, HttpServletRequest request) {
        String ip = request.getRemoteAddr();

        if (!rateLimitService.checkIpRateLimit(ip)) {
            throw new RuntimeException("IP_RATE_LIMIT_EXCEEDED");
        }

        OtpSession session = messageRedisService.getAndIncrementAttempts(uuid);

        if (!passwordEncoder.matches(dto.getValue(), session.getOtpHash())) {
            throw new RuntimeException("INVALID_OTP");
        }

        messageRedisService.verifyAndReleaseLock(
                uuid,
                SenderType.fromId(session.getChannelId()),
                session.getTarget());
        return true;
    }

    private String generateOtp() {
        int otp = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(otp);
    }

    private String generateUUID() {
        return UUID.randomUUID().toString();
    }

    private SenderType getSenderTypeFromString(String value) {
        for (SenderType type : SenderType.values()) {
            if (type.name().equalsIgnoreCase(value)) return type;
        }
        throw new IllegalArgumentException("Unknown SenderType | value: " + value);
    }

}
