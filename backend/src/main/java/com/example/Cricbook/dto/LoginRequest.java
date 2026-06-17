package com.example.Cricbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Email/Phone is required")
    @Pattern(regexp = "^(?:\\d{10}|[^\\s@]+@[^\\s@]+\\.[^\\s@]+)$", message = "Username must be a valid email or 10 digit mobile number")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    private boolean rememberMe;
}
