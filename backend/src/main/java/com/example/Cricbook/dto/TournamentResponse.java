package com.example.Cricbook.dto;

import com.example.Cricbook.enums.TournamentFormat;
import com.example.Cricbook.enums.TournamentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentResponse {
    private Long id;
    private Long organizerId;
    private String organizerName;
    private Long turfId;
    private String turfName;
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
    private Integer registeredTeams;
    private BigDecimal entryFee;
    private BigDecimal prizePool;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
}
