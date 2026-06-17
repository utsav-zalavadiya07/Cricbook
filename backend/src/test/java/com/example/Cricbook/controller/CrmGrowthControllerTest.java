package com.example.Cricbook.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CrmGrowthControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void adminGrowthDashboardReturnsClvAndRetentionControls() throws Exception {
        mockMvc.perform(get("/api/v1/admin/growth/dashboard")
                        .with(user("growth_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scorecard.customerLifetimeValue").exists())
                .andExpect(jsonPath("$.loyalty.pointsOutstanding").exists())
                .andExpect(jsonPath("$.memberships.active").exists())
                .andExpect(jsonPath("$.retention.repeatBookingRate").exists())
                .andExpect(jsonPath("$.segments").isArray())
                .andExpect(jsonPath("$.insights").isArray());
    }

    @Test
    void adminGrowthSegmentsAndAutomationExposeCampaignInputs() throws Exception {
        mockMvc.perform(get("/api/v1/admin/growth/segments")
                        .with(user("growth_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].recommendedAction").exists());

        mockMvc.perform(get("/api/v1/admin/growth/automation")
                        .with(user("growth_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].trigger").exists());
    }

    @Test
    void adminGrowthReconciliationReturnsAuditReadyChecks() throws Exception {
        mockMvc.perform(get("/api/v1/admin/growth/reconciliation")
                        .with(user("growth_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.score").exists())
                .andExpect(jsonPath("$.checks").isArray());
    }
}
