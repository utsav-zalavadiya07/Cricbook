package com.example.Cricbook.dto;

import com.example.Cricbook.enums.TrustReportStatus;
import lombok.Data;

@Data
public class TrustModerationRequest {
    private TrustReportStatus status;
    private Integer scoreAdjustment;
    private String reason;
}
