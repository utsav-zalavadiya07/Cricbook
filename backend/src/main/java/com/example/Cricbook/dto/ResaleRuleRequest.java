package com.example.Cricbook.dto;

import lombok.Data;

@Data
public class ResaleRuleRequest {
    private Boolean allowResale;
    private Boolean autoApproveTransfer;
    private Boolean restrictLowTrustUsers;
    private Integer maxMarkupPercent;
    private Integer minHoursBeforeStart;
}
