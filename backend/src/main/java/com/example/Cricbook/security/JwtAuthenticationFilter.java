package com.example.Cricbook.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.example.Cricbook.service.SessionManagementService;
import org.springframework.lang.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final SessionManagementService sessionManagementService;

    @Autowired
    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService, SessionManagementService sessionManagementService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.sessionManagementService = sessionManagementService;
    }

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.sessionManagementService = null;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            username = jwtUtil.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if (userDetails.isEnabled()
                        && userDetails.isAccountNonLocked()
                        && userDetails.isAccountNonExpired()
                        && userDetails.isCredentialsNonExpired()
                        && jwtUtil.isTokenValid(jwt, userDetails)
                        && isSessionActive(jwt, username)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            writeUnauthorizedResponse(response, request);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isSessionActive(String jwt, String username) {
        if (sessionManagementService == null) {
            return true;
        }
        String sessionId = jwtUtil.extractSessionId(jwt);
        return sessionManagementService.touchAndValidateSession(sessionId, username);
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, HttpServletRequest request) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("""
                {"timestamp":"%s","status":401,"error":"Unauthorized","code":"INVALID_TOKEN","message":"Session token is invalid or expired. Please sign in again.","path":"%s","details":null}
                """.formatted(OffsetDateTime.now(), request.getRequestURI()));
    }
}
