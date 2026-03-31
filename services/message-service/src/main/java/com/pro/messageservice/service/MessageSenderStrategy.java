package com.pro.messageservice.service;

import com.pro.messageservice.model.SenderType;

public interface MessageSenderStrategy {

    SenderType getSenderType();

    void send(String target, String message);

}
