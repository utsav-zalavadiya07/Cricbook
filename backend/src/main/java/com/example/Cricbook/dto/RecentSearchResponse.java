package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentSearchResponse {
    private Long id;
    private String city;
    private LocalDate date;
    private String boxType;
    private String pitchType;
    private LocalDateTime createdAt;
}
