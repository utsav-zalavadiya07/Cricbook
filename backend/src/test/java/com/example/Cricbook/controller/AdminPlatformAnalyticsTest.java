package com.example.Cricbook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
class AdminPlatformAnalyticsTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void biWorkspaceReturnsDecisionLayer() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/bi")
                        .with(user("analytics_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scorecard.analyticsConfidenceScore").exists())
                .andExpect(jsonPath("$.dashboardBuilder.widgets").isArray())
                .andExpect(jsonPath("$.reportCatalog").isArray())
                .andExpect(jsonPath("$.reconciliation.status").exists());
    }

    @Test
    void reportAndExportReturnRowsAndAuditPayload() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/reports")
                        .param("type", "REVENUE")
                        .param("period", "MONTHLY")
                        .with(user("analytics_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("REVENUE"))
                .andExpect(jsonPath("$.accuracyStatus").exists())
                .andExpect(jsonPath("$.rows").isArray());

        mockMvc.perform(get("/api/v1/admin/analytics/reports/export")
                        .param("type", "REVENUE")
                        .param("format", "CSV")
                        .with(user("analytics_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").exists())
                .andExpect(jsonPath("$.report.type").value("REVENUE"));
    }

    @Test
    void dashboardSavePersistsBuilderSummary() throws Exception {
        Map<String, Object> request = Map.of(
                "name", "Executive Test Dashboard",
                "audience", "ADMIN",
                "widgets", java.util.List.of("revenue_trend", "booking_funnel")
        );

        mockMvc.perform(post("/api/v1/admin/analytics/dashboards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("analytics_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true))
                .andExpect(jsonPath("$.savedDashboard.name").value("Executive Test Dashboard"));
    }
}
