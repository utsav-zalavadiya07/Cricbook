package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CityConfigResponse {
    private Long id;
    private String city;
    private String country;
    private String currency;
    private String locale;
    private String timeZone;
    private Boolean active;
}
