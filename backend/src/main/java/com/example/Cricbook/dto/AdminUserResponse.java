package com.example.Cricbook.dto;

import com.example.Cricbook.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private UserStatus status;
    private List<String> roles;
    private LocalDateTime createdAt;
}
