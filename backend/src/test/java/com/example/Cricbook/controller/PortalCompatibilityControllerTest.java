package com.example.Cricbook.controller;

import com.example.Cricbook.dto.RebookDtos.BookingIntelligenceResponse;
import com.example.Cricbook.service.RebookService;
import com.example.Cricbook.service.ResaleService;
import com.example.Cricbook.service.SocialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
class PortalCompatibilityControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private SocialService socialService;

    @MockitoBean
    private RebookService rebookService;

    @MockitoBean
    private ResaleService resaleService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(username = "player@test.com")
    void socialInvitesCompatUsesTeamInvitesPayload() throws Exception {
        when(socialService.getMyInvites("player@test.com")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/social/invites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = "player@test.com")
    void bookingAnalyticsCompatUsesIntelligencePayload() throws Exception {
        when(rebookService.myAnalytics("player@test.com")).thenReturn(BookingIntelligenceResponse.builder()
                .frequentBookings(0)
                .recurringBookings(0)
                .templates(0)
                .autoWaitlists(0)
                .conflicts(0)
                .rebookReadiness(0)
                .topFrequentBookings(List.of())
                .predictions(List.of())
                .build());

        mockMvc.perform(get("/api/v1/bookings/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rebookReadiness").value(0));
    }

    @Test
    @WithMockUser(username = "player@test.com")
    void resaleLegacyUserRoutesDoNotHitListingIdRoute() throws Exception {
        when(resaleService.recommendations("player@test.com")).thenReturn(List.of());
        when(resaleService.myListings("player@test.com")).thenReturn(List.of());
        when(resaleService.myWaitlist("player@test.com")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/resale/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        mockMvc.perform(get("/api/v1/resale/my-listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        mockMvc.perform(get("/api/v1/resale/waitlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
