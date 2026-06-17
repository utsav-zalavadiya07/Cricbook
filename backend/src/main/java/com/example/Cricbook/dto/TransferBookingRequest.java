package com.example.Cricbook.dto;

import lombok.Data;

@Data
public class TransferBookingRequest {
    private Long bookingId;
    private Long toUserId;
    private String transferType;
}
