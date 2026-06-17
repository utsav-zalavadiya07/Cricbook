package com.example.Cricbook.controller;

import com.example.Cricbook.entity.AdminUser;
import com.example.Cricbook.repository.AdminUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class AdminOperationsTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Seed a test admin if it does not exist
        if (!adminUserRepository.existsByEmail("test_ops_admin@cricbook.com")) {
            AdminUser admin = AdminUser.builder()
                    .email("test_ops_admin@cricbook.com")
                    .password(passwordEncoder.encode("securepass"))
                    .role("SUPER_ADMIN")
                    .status("ACTIVE")
                    .build();
            adminUserRepository.save(admin);
        }
    }

    @Test
    public void testAdminLoginSimulated2FA() throws Exception {
        Map<String, String> loginReq = Map.of(
                "email", "test_ops_admin@cricbook.com",
                "password", "securepass"
        );

        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OTP_REQUIRED"))
                .andExpect(jsonPath("$.simulatedOtp").value("123456"));
    }

    @Test
    public void testAdminVerifyOtp() throws Exception {
        Map<String, String> verifyReq = Map.of(
                "email", "test_ops_admin@cricbook.com",
                "otp", "123456"
        );

        mockMvc.perform(post("/api/v1/admin/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.role").value("SUPER_ADMIN"));
    }

    @Test
    public void testAdminOpsSecureUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ops/users")
                        .with(csrf()))
                .andExpect(status().isUnauthorized()); // Blocked without valid token
    }

    @Test
    public void testAdminGovernanceDashboard() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ops/governance/dashboard")
                        .with(user("test_ops_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.governanceScore").exists())
                .andExpect(jsonPath("$.riskLevel").exists())
                .andExpect(jsonPath("$.kpis.users").exists())
                .andExpect(jsonPath("$.rbacSummary.roles").exists());
    }

    @Test
    public void testAdminRbacMatrix() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ops/rbac/matrix")
                        .with(user("test_ops_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.roleCount").exists())
                .andExpect(jsonPath("$.summary.permissionCount").exists())
                .andExpect(jsonPath("$.catalog").isArray());
    }
}
