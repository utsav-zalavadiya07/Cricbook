package com.example.Cricbook.dto;

import com.example.Cricbook.enums.TournamentRegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentRegistrationResponse {
    private Long id;
    private Long tournamentId;
    private Long teamId;
    private String teamName;
    private Long registeredById;
    private String registeredByName;
    private TournamentRegistrationStatus status;
    private Boolean paid;
    private String note;
    private LocalDateTime createdAt;
}
