package com.example.Cricbook.dto;

import com.example.Cricbook.enums.TournamentStatus;
import lombok.Data;

@Data
public class UpdateTournamentStatusRequest {
    private TournamentStatus status;
    private String note;
}
