package com.pro.messageservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MessageRequestDto {

    @NotNull
    private String channel;

    @NotBlank
    private String target;

}
