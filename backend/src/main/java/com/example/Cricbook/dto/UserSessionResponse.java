package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSessionResponse {
    private String sessionId;
    private String deviceName;
    private String browser;
    private String os;
    private String ipAddress;
    private String loginTime;
    private String lastActivity;
    private String expiresAt;
    private boolean current;
    private boolean rememberMe;
}
