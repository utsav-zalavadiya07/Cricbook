package com.example.Cricbook.dto;

import com.example.Cricbook.enums.TrustReportReason;
import lombok.Data;

@Data
public class TrustReportRequest {
    private Long reportedUserId;
    private Long bookingId;
    private TrustReportReason reason;
    private String description;
}
