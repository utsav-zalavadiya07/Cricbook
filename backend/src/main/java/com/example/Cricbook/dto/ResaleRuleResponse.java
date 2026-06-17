package com.example.Cricbook.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ResaleRuleResponse {
    private Long id;
    private Long ownerId;
    private Boolean allowResale;
    private Boolean autoApproveTransfer;
    private Boolean restrictLowTrustUsers;
    private Integer maxMarkupPercent;
    private Integer minHoursBeforeStart;
    private LocalDateTime updatedAt;
}
