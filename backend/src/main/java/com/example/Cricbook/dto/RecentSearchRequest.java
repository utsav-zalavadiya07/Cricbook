package com.example.Cricbook.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class RecentSearchRequest {
    private String city;
    private LocalDate date;
    private String boxType;
    private String pitchType;
}
