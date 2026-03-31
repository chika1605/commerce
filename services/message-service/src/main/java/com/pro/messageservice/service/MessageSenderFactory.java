package com.pro.messageservice.service;

import com.pro.messageservice.model.SenderType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MessageSenderFactory {

    private final Map<SenderType, MessageSenderStrategy> senders;

    public MessageSenderFactory(List<MessageSenderStrategy> senders) {
        this.senders = senders.stream()
                .collect(Collectors.toMap(MessageSenderStrategy::getSenderType,
                        s -> s));
    }

    public MessageSenderStrategy getStrategy(SenderType senderType) {
        MessageSenderStrategy strategy = senders.get(senderType);
        if (strategy == null) throw new IllegalArgumentException("Unknown sender type: " + senderType);
        return strategy;
    }

}
