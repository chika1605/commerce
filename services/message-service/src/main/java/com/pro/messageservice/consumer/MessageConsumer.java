package com.pro.messageservice.consumer;

import com.pro.events.model.EventTopics;
import com.pro.events.model.OrderCreatedEvent;
import com.pro.messageservice.service.MessageSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageConsumer {

    private final MessageSenderService messageSenderService;

    @KafkaListener(topics = EventTopics.ORDER_CREATED)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        String message = "Order by ID: %d created. Order code: %s".formatted(event.getOrderId(), event.getOrderCode());
        messageSenderService.sendMessage(event.getSenderType(), event.getTarget(), message);
    }

}