package com.example.Cricbook.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StartSubscriptionRequest {
    private Long planId;
    private Long turfId; // Optional, for owner featured plan
}
