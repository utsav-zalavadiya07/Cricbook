package com.example.Cricbook.dto;

import com.example.Cricbook.enums.BookingTransferStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BookingTransferResponse {
    private Long id;
    private Long bookingId;
    private Long fromUserId;
    private String fromUserName;
    private Long toUserId;
    private String toUserName;
    private BookingTransferStatus status;
    private String transferType;
    private LocalDateTime acceptedAt;
    private LocalDateTime createdAt;
}
