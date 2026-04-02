package com.pro.events.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderCreatedEvent {
    private String senderType;
    private String target;
    private Long orderId;
    private String orderCode;
}