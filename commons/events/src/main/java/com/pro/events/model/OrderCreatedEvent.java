package com.pro.events.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private String senderType;
    private String target;
    private Long orderId;
    private String orderCode;
}