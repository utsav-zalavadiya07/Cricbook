package com.example.Cricbook.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class TeamPollResponse {
    private Long id;
    private Long teamId;
    private String question;
    private List<String> options;
    private Map<String, Long> voteCounts;
    private LocalDateTime expiresAt;
}
