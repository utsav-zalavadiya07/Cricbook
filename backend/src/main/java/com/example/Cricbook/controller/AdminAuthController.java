package com.example.Cricbook.controller;

import com.example.Cricbook.entity.AdminUser;
import com.example.Cricbook.repository.AdminUserRepository;
import com.example.Cricbook.security.AdminUserDetails;
import com.example.Cricbook.security.JwtUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AdminLoginRequest request) {
        Optional<AdminUser> adminOpt = adminUserRepository.findByEmail(request.getEmail());
        if (adminOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid admin credentials."));
        }

        AdminUser admin = adminOpt.get();
        if (!"ACTIVE".equalsIgnoreCase(admin.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Your admin account is blocked."));
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid admin credentials."));
        }

        // Return OTP Required step for premium 2FA flow simulation
        return ResponseEntity.ok(Map.of(
                "status", "OTP_REQUIRED",
                "email", admin.getEmail(),
                "message", "2FA OTP sent to registered security device.",
                "simulatedOtp", "123456"
        ));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody AdminVerifyOtpRequest request) {
        Optional<AdminUser> adminOpt = adminUserRepository.findByEmail(request.getEmail());
        if (adminOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Admin user not found."));
        }

        AdminUser admin = adminOpt.get();
        if (!"ACTIVE".equalsIgnoreCase(admin.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Your admin account is blocked."));
        }

        // Simulate 2FA verification code checking
        if (!"123456".equals(request.getOtp())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid 2FA security code. Please check your authenticator app."));
        }

        AdminUserDetails userDetails = new AdminUserDetails(admin);
        String token = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(Map.of(
                "accessToken", token,
                "refreshToken", UUID.randomUUID().toString(),
                "expiresIn", jwtExpiration / 1000,
                "role", admin.getRole(),
                "email", admin.getEmail()
        ));
    }

    @Data
    public static class AdminLoginRequest {
        private String email;
        private String password;
    }

    @Data
    public static class AdminVerifyOtpRequest {
        private String email;
        private String otp;
    }
}
