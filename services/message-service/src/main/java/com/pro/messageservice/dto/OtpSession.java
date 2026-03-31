package com.pro.messageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpSession {

    private short channelId;
    private String target;
    private String otpHash;
    private int attempts;
    private boolean verified;

}