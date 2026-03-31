package com.pro.messageservice.service;

import com.pro.messageservice.model.SenderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailMessageSender implements MessageSenderStrategy {

    @Override
    public SenderType getSenderType() {
        return SenderType.EMAIL;
    }

    @Override
    public void send(String target, String message) {
        log.info("Sending Email to {}: {}", target, message);
    }

}
