package com.example.Cricbook.controller;

import com.example.Cricbook.dto.*;
import com.example.Cricbook.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        String response = authService.register(request);
        return ResponseEntity.ok(Map.of("message", response));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        String message = authService.resendOtp(request.getEmail());
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest servletRequest) {
        return ResponseEntity.ok(authService.login(request, SessionRequestContext.from(servletRequest)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String message = authService.requestPasswordReset(request);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String message = authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request,
                                                     HttpServletRequest servletRequest) {
        return ResponseEntity.ok(authService.refreshToken(request, SessionRequestContext.from(servletRequest)));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                    @RequestBody(required = false) RefreshTokenRequest request,
                                    HttpServletRequest servletRequest) {
        String refreshToken = request != null ? request.getRefreshToken() : null;
        authService.logout(authHeader, refreshToken, SessionRequestContext.from(servletRequest));
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                       HttpServletRequest servletRequest) {
        authService.logoutAll(authHeader, SessionRequestContext.from(servletRequest));
        return ResponseEntity.ok(Map.of("message", "Logged out from all devices successfully"));
    }
}
