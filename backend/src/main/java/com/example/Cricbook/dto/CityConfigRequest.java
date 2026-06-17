package com.example.Cricbook.dto;

import lombok.Data;

@Data
public class CityConfigRequest {
    private String city;
    private String country;
    private String currency;
    private String locale;
    private String timeZone;
    private Boolean active;
}
