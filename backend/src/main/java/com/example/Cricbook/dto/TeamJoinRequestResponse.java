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
public class TeamJoinRequestResponse {
    private Long id;
    private Long teamId;
    private String teamName;
    private Long requesterId;
    private String requesterName;
    private Long reviewerId;
    private String reviewerName;
    private String message;
    private InviteStatus status;
    private LocalDateTime createdAt;
}
