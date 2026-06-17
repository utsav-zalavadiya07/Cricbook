package com.example.Cricbook.dto;

import com.example.Cricbook.enums.InviteStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamInviteResponse {
    private Long id;
    private Long teamId;
    private String teamName;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String receiverName;
    private InviteStatus status;
    private LocalDateTime createdAt;
}
