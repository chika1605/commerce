package com.pro.messageservice.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SenderType {

    SMS((short) 1),
    WHATSAPP((short) 2),
    EMAIL((short) 3);

    private final short id;

    public static SenderType fromId(short id) {
        for (SenderType type : values()) {
            if (type.id == id) return type;
        }
        throw new IllegalArgumentException("Unknown SenderType id: " + id);
    }

}
