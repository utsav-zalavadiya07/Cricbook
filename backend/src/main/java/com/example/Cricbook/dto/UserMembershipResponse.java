package com.example.Cricbook.dto;

import com.example.Cricbook.enums.MembershipPlanType;
import com.example.Cricbook.enums.MembershipStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMembershipResponse {
    private Long id;
    private Long planId;
    private String planName;
    private MembershipPlanType type;
    private Long turfId;
    private String turfName;
    private Integer discountPercent;
    private Boolean prioritySlots;
    private Boolean featuredListing;
    private MembershipStatus status;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
}
