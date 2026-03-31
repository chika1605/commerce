package com.pro.messageservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MessageVerifyDto {

    @NotBlank
    private String value;

}
