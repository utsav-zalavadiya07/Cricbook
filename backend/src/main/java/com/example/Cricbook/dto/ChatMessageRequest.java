package com.example.Cricbook.dto;

import lombok.Data;

@Data
public class ChatMessageRequest {
    private String message;
    private String messageType;
    private String mediaUrl;
}
