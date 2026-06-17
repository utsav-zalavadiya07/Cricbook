package com.example.Cricbook.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TeamPollRequest {
    private String question;
    private List<String> options;
    private LocalDateTime expiresAt;
}
