package com.example.Cricbook.dto;

import com.example.Cricbook.enums.ChatRoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private Long id;
    private ChatRoomType roomType;
    private Long roomId;
    private Long senderId;
    private String senderName;
    private String message;
    private String messageType;
    private String mediaUrl;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
