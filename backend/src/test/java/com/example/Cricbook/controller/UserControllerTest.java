package com.example.Cricbook.controller;

import com.example.Cricbook.dto.UserProfileDto;
import com.example.Cricbook.dto.PlayerProfileResponse;
import com.example.Cricbook.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private UserService userService;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(username = "test@test.com")
    void testGetCurrentUser() throws Exception {
        UserProfileDto response = UserProfileDto.builder()
                .email("test@test.com")
                .name("John Doe")
                .build();

        when(userService.getCurrentUser("test@test.com")).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }

    @MockitoBean
    private com.example.Cricbook.service.BookingService bookingService;

    @MockitoBean
    private com.example.Cricbook.service.ReviewService reviewService;

    @MockitoBean
    private com.example.Cricbook.repository.UserRepository userRepository;

    @Test
    @WithMockUser(username = "test@test.com")
    void testUpdateProfile() throws Exception {
        org.mockito.Mockito.doNothing().when(userService)
                .updateProfile(org.mockito.ArgumentMatchers.eq("test@test.com"), org.mockito.ArgumentMatchers.any(com.example.Cricbook.dto.UpdateUserRequest.class));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/users/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New Name\", \"phone\":\"1234567890\"}"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("Profile updated"));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    void testGetBookings() throws Exception {
        com.example.Cricbook.dto.BookingResponse booking = new com.example.Cricbook.dto.BookingResponse();
        booking.setId(1L);
        when(bookingService.getMyBookings()).thenReturn(java.util.Collections.singletonList(booking));

        mockMvc.perform(get("/api/v1/users/bookings")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    void testGetReviews() throws Exception {
        com.example.Cricbook.entity.User user = new com.example.Cricbook.entity.User();
        user.setId(1L);
        when(userRepository.findByEmailOrPhone("test@test.com")).thenReturn(java.util.Optional.of(user));

        com.example.Cricbook.dto.ReviewResponse review = com.example.Cricbook.dto.ReviewResponse.builder().id(1L).comment("Great!").build();
        when(reviewService.getMyReviews(1L)).thenReturn(java.util.Collections.singletonList(review));

        mockMvc.perform(get("/api/v1/users/reviews")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].comment").value("Great!"));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    void testGetMyPlayerProfile() throws Exception {
        PlayerProfileResponse response = PlayerProfileResponse.builder()
                .userId(1L)
                .name("John Doe")
                .email("test@test.com")
                .city("Surat")
                .matches(12)
                .wins(7)
                .build();

        when(userService.getMyPlayerProfile("test@test.com")).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/player-profile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Surat"))
                .andExpect(jsonPath("$.matches").value(12));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    void testUpdatePlayerProfile() throws Exception {
        PlayerProfileResponse response = PlayerProfileResponse.builder()
                .userId(1L)
                .name("John Doe")
                .email("test@test.com")
                .battingStyle(com.example.Cricbook.enums.BattingStyle.RIGHT_HAND)
                .bowlingStyle(com.example.Cricbook.enums.BowlingStyle.RIGHT_ARM_MEDIUM)
                .position(com.example.Cricbook.enums.PlayerPosition.ALL_ROUNDER)
                .experience("Club level")
                .build();

        when(userService.updatePlayerProfile(org.mockito.ArgumentMatchers.eq("test@test.com"), org.mockito.ArgumentMatchers.any(com.example.Cricbook.dto.UpdatePlayerProfileRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/users/player-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "city": "Surat",
                                  "preferredTime": "Evening",
                                  "battingStyle": "RIGHT_HAND",
                                  "bowlingStyle": "RIGHT_ARM_MEDIUM",
                                  "position": "ALL_ROUNDER",
                                  "experience": "Club level"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value("ALL_ROUNDER"))
                .andExpect(jsonPath("$.battingStyle").value("RIGHT_HAND"));
    }
}
