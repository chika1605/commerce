package com.pro.messageservice.service;

import com.pro.messageservice.model.SenderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WhatsAppMessageSender implements MessageSenderStrategy {

    @Override
    public SenderType getSenderType() {
        return SenderType.WHATSAPP;
    }

    @Override
    public void send(String target, String message) {
        log.info("Sending WhatsApp to {}: {}", target, message);
    }

}
