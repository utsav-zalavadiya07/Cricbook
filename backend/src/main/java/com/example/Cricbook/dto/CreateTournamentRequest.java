package com.example.Cricbook.dto;

import com.example.Cricbook.enums.TournamentFormat;
import com.example.Cricbook.enums.TournamentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateTournamentRequest {
    private Long turfId;
    private String name;
    private String sport;
    private String city;
    private String venue;
    private String description;
    private String bannerUrl;
    private String rules;
    private Boolean requiresApproval;
    private TournamentFormat format;
    private TournamentStatus status;
    private Integer maxTeams;
    private BigDecimal entryFee;
    private BigDecimal prizePool;
    private LocalDate startDate;
    private LocalDate endDate;
}
