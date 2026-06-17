package com.example.Cricbook.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 80, message = "Name must be between 2 and 80 characters")
    @Pattern(regexp = "^[\\p{L} .'-]+$", message = "Name can contain only letters, spaces, apostrophes, periods, and hyphens")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 254, message = "Email is too long")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\d{10}$", message = "Phone must be 10 digits")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(max = 72, message = "Password must be at most 72 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,72}$",
            message = "Password must be 8-72 characters and include uppercase, lowercase, number, and special character")
    private String password;

    @Pattern(regexp = "(?i)USER|OWNER", message = "Role must be USER or OWNER")
    private String role;
}
