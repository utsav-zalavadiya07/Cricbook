package com.example.Cricbook.controller;

import com.example.Cricbook.dto.SecuritySessionsResponse;
import com.example.Cricbook.dto.SessionRequestContext;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.UserRepository;
import com.example.Cricbook.security.JwtUtil;
import com.example.Cricbook.service.SessionManagementService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/security/sessions")
@RequiredArgsConstructor
public class SecuritySessionController {

    private final SessionManagementService sessionManagementService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<SecuritySessionsResponse> listSessions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        User user = findUser(userDetails);
        return ResponseEntity.ok(sessionManagementService.listSessions(user, currentSessionId(authHeader)));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> revokeSession(@AuthenticationPrincipal UserDetails userDetails,
                                           @PathVariable String sessionId,
                                           HttpServletRequest servletRequest) {
        User user = findUser(userDetails);
        sessionManagementService.revokeSession(user, sessionId, SessionRequestContext.from(servletRequest));
        return ResponseEntity.ok(Map.of("message", "Session revoked successfully"));
    }

    private User findUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw ApiException.unauthorized("Please sign in to manage sessions.");
        }
        return userRepository.findByEmailOrPhone(userDetails.getUsername())
                .orElseThrow(() -> ApiException.unauthorized("User session was not found."));
    }

    private String currentSessionId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            return jwtUtil.extractSessionId(authHeader.substring(7));
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
