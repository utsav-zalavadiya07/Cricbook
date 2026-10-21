package com.example.Cricbook.dto;

import com.example.Cricbook.enums.RoleName;
import com.example.Cricbook.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class UserProfileDto {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String city;
    private String bio;
    private String designation;
    private String alternatePhone;
    private String businessName;
    private String businessType;
    private String gstin;
    private String address;
    private String supportEmail;
    private String preferredLanguage;
    private String preferredTheme;
    private UserStatus status;
    private Set<RoleName> roles;
    private Integer profileCompletionPercentage;
    private LocalDateTime createdAt;
}
