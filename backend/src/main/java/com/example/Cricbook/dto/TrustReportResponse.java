package com.example.Cricbook.dto;

import com.example.Cricbook.enums.TrustReportReason;
import com.example.Cricbook.enums.TrustReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrustReportResponse {
    private Long id;
    private Long reporterId;
    private String reporterName;
    private Long reportedUserId;
    private String reportedUserName;
    private Long bookingId;
    private TrustReportReason reason;
    private String description;
    private TrustReportStatus status;
    private LocalDateTime createdAt;
}
