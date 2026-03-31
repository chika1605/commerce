package com.pro.messageservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pro.messageservice.dto.OtpSession;
import com.pro.messageservice.model.SenderType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageRedisService {

    private static final String OTP_KEY = "otp:";
    private static final String LOCK_KEY = "otp:lock:";
    private static final long TTL_SECONDS = 240L;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     *  Атомарно проверяем lock и ставим его
     */
    private static final String ACQUIRE_LOCK_SCRIPT = """
            local lockKey = KEYS[1]
            local existing = redis.call('GET', lockKey)
            if existing then
                return 0
            end
            redis.call('SET', lockKey, '1', 'EX', ARGV[1])
            return 1
            """;

    /**
     * Атомарно читаем сессию и инкрементируем attempts
     */
    private static final String GET_AND_INCREMENT_SCRIPT = """
             local sessionKey = KEYS[1]
             local session = redis.call('GET', sessionKey)
    
             if not session then
                 return 'SESSION_NOT_FOUND'
             end
    
             local data = cjson.decode(session)
    
             if data.verified then
                 return 'ALREADY_VERIFIED'
             end
    
             if data.attempts >= 3 then
                 return 'TOO_MANY_ATTEMPTS'
             end
    
             data.attempts = data.attempts + 1
    
             local ttl = redis.call('TTL', sessionKey)
             if ttl < 1 then ttl = 240 end
    
             redis.call('SET', sessionKey, cjson.encode(data), 'EX', ttl)
    
             return cjson.encode(data)
            """;

    /**
     * Атомарно помечаем verified и удаляем lock
     */
    private static final String VERIFY_AND_RELEASE_SCRIPT = """
            local sessionKey = KEYS[1]
            local lockKey = KEYS[2]
            local session = redis.call('GET', sessionKey)
            if not session then
                return {err = 'SESSION_NOT_FOUND'}
            end
            local data = cjson.decode(session)
            data.verified = true
            redis.call('SET', sessionKey, cjson.encode(data), 'EX', 60)
            redis.call('DEL', lockKey)
            return {ok = 'VERIFIED'}
            """;

    public void deleteSession(String uuid) {
        redisTemplate.delete(OTP_KEY + uuid);
    }

    public void releaseLock(SenderType channel, String target) {
        redisTemplate.delete(LOCK_KEY + channel.getId() + ":" + target);
    }

    public void saveSession(String uuid, OtpSession session) {
        try {
            String json = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(OTP_KEY + uuid, json, Duration.ofSeconds(TTL_SECONDS));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize OtpSession", e);
        }
    }

    public boolean acquireLock(SenderType channel, String target) {
        String lockKey = LOCK_KEY + channel.getId() + ":" + target;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(ACQUIRE_LOCK_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, List.of(lockKey), String.valueOf(TTL_SECONDS));
        return Long.valueOf(1L).equals(result);
    }

    public OtpSession getAndIncrementAttempts(String uuid) {
        String sessionKey = OTP_KEY + uuid;
        DefaultRedisScript<String> script = new DefaultRedisScript<>(GET_AND_INCREMENT_SCRIPT, String.class);
        String result = redisTemplate.execute(script, List.of(sessionKey));

        if (result == null || "SESSION_NOT_FOUND".equals(result)) {
            throw new RuntimeException("SESSION_NOT_FOUND");
        }
        if (result.contains("ALREADY_VERIFIED")) throw new RuntimeException("ALREADY_VERIFIED");
        if (result.contains("TOO_MANY_ATTEMPTS")) throw new RuntimeException("TOO_MANY_ATTEMPTS");

        try {
            return objectMapper.readValue(result, OtpSession.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize OtpSession", e);
        }
    }

    public void verifyAndReleaseLock(String uuid, SenderType channel, String target) {
        String sessionKey = OTP_KEY + uuid;
        String lockKey = LOCK_KEY + channel.getId() + ":" + target;
        DefaultRedisScript<String> script = new DefaultRedisScript<>(VERIFY_AND_RELEASE_SCRIPT, String.class);
        redisTemplate.execute(script, List.of(sessionKey, lockKey));
    }

}
