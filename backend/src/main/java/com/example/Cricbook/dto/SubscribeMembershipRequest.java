package com.example.Cricbook.dto;

import lombok.Data;

@Data
public class SubscribeMembershipRequest {
    private Long planId;
    private Long turfId;
}
