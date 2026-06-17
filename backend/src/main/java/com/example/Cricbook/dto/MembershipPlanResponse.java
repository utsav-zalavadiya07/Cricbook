package com.example.Cricbook.dto;

import com.example.Cricbook.enums.MembershipPlanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipPlanResponse {
    private Long id;
    private String name;
    private MembershipPlanType type;
    private BigDecimal price;
    private Integer durationDays;
    private Integer discountPercent;
    private Boolean prioritySlots;
    private Boolean featuredListing;
    private String description;
}
