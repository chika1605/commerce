package com.pro.messageservice.controller;

import com.pro.messageservice.dto.MessageRequestDto;
import com.pro.messageservice.dto.MessageResponseDto;
import com.pro.messageservice.dto.MessageVerifyDto;
import com.pro.messageservice.service.MessageSenderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageSenderService senderService;

    @PostMapping("/send")
    public ResponseEntity<MessageResponseDto> send(@Valid @RequestBody MessageRequestDto dto,
                                                   HttpServletRequest request) {
        MessageResponseDto response = senderService.send(dto, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify/{uuid}")
    public ResponseEntity<Void> verify(@PathVariable String uuid,
                                       @Valid @RequestBody MessageVerifyDto dto,
                                       HttpServletRequest request) {
        senderService.verify(uuid, dto, request);
        return ResponseEntity.ok().build();
    }

}
