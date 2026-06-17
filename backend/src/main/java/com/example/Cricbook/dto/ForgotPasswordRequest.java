package com.example.Cricbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordRequest {

    @NotBlank(message = "Email or mobile number is required")
    @Pattern(regexp = "^(?:\\d{10}|[^\\s@]+@[^\\s@]+\\.[^\\s@]+)$", message = "Enter a valid email or 10 digit mobile number")
    private String loginId;
}
