package com.example.Cricbook.controller;

import com.example.Cricbook.dto.AuthResponse;
import com.example.Cricbook.dto.LoginRequest;
import com.example.Cricbook.dto.RegisterRequest;
import com.example.Cricbook.dto.VerifyOtpRequest;
import com.example.Cricbook.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void testRegister() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("John");
        request.setEmail("test@test.com");
        request.setPassword("Password123!");
        request.setPhone("1234567890");

        when(authService.register(any(RegisterRequest.class))).thenReturn("Success");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success"));
    }

    @Test
    void testVerifyOtp() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("test@test.com");
        request.setOtp("123456");

        AuthResponse response = AuthResponse.builder()
                .accessToken("token")
                .refreshToken("refreshToken")
                .build();

        when(authService.verifyOtp(any(VerifyOtpRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token"));
    }

    @Test
    void testRegisterRejectsWeakPasswordBeforeServiceCall() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("John");
        request.setEmail("test@test.com");
        request.setPassword("Password123");
        request.setPhone("1234567890");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    void testLoginRejectsInvalidUsernameBeforeServiceCall() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("not-an-email-or-phone");
        request.setPassword("Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    void testVerifyOtpRejectsMalformedOtpBeforeServiceCall() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("test@test.com");
        request.setOtp("12X");

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(authService, never()).verifyOtp(any(VerifyOtpRequest.class));
    }

    @Test
    void testSecuredApiRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
