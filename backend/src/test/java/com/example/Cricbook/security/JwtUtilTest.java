package com.example.Cricbook.security;

import com.example.Cricbook.entity.Role;
import com.example.Cricbook.enums.RoleName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", 3600000L);
    }

    @Test
    void testGenerateAndExtract() {
        UserDetails userDetails = new User("test@test.com", "pass", Collections.emptyList());
        String token = jwtUtil.generateToken(userDetails);
        
        assertNotNull(token);
        assertEquals("test@test.com", jwtUtil.extractUsername(token));
        assertTrue(jwtUtil.isTokenValid(token, userDetails));
    }

    @Test
    void testIsTokenValid_InvalidUser() {
        UserDetails user1 = new User("user1@test.com", "pass", Collections.emptyList());
        UserDetails user2 = new User("user2@test.com", "pass", Collections.emptyList());
        
        String token = jwtUtil.generateToken(user1);
        assertFalse(jwtUtil.isTokenValid(token, user2));
    }

    @Test
    void testGenerateTokenIncludesUserClaims() {
        com.example.Cricbook.entity.User user = com.example.Cricbook.entity.User.builder()
                .id(42L)
                .email("owner@example.com")
                .password("hash")
                .tokenVersion(7)
                .build();
        user.getRoles().add(new Role(2L, RoleName.ROLE_OWNER));

        String token = jwtUtil.generateToken(new CustomUserDetails(user));

        assertEquals("owner@example.com", jwtUtil.extractUsername(token));
        Number userId = jwtUtil.extractClaim(token, claims -> claims.get("userId", Number.class));
        assertEquals(42L, userId.longValue());
        assertEquals("OWNER", jwtUtil.extractClaim(token, claims -> claims.get("role", String.class)));
        assertEquals("owner@example.com", jwtUtil.extractClaim(token, claims -> claims.get("email", String.class)));
        assertEquals(7, jwtUtil.extractTokenVersion(token));
    }

    @Test
    void testTokenInvalidWhenUserTokenVersionChanges() {
        com.example.Cricbook.entity.User user = com.example.Cricbook.entity.User.builder()
                .id(11L)
                .email("player@example.com")
                .password("hash")
                .tokenVersion(1)
                .build();
        user.getRoles().add(new Role(1L, RoleName.ROLE_USER));
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtUtil.generateToken(userDetails);

        user.setTokenVersion(2);

        assertFalse(jwtUtil.isTokenValid(token, userDetails));
    }
}
