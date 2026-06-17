package com.example.Cricbook.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AIIntelligenceControllerTest {

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
    void adminAiDashboardReturnsIntelligenceGovernanceAndActions() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ai/dashboard")
                        .with(user("ai_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelVersion").exists())
                .andExpect(jsonPath("$.kpis.recommendationCoverage").exists())
                .andExpect(jsonPath("$.assistant.supportedIntents").isArray())
                .andExpect(jsonPath("$.recommendations.engines").isArray())
                .andExpect(jsonPath("$.forecasts").isArray())
                .andExpect(jsonPath("$.governance.guardrails.promptInjection").exists())
                .andExpect(jsonPath("$.priorityActions").isArray());
    }

    @Test
    void adminAiGovernanceReturnsResponsibleAiControls() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ai/governance")
                        .with(user("ai_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driftStatus").exists())
                .andExpect(jsonPath("$.accuracyMonitoring.recommendation").exists())
                .andExpect(jsonPath("$.biasControls").isArray())
                .andExpect(jsonPath("$.guardrails.dataExfiltration").exists())
                .andExpect(jsonPath("$.responsibleAiControls").isArray());
    }

    @Test
    void adminCopilotAnswersAuditedFraudQuery() throws Exception {
        mockMvc.perform(post("/api/v1/admin/ai/copilot/query")
                        .with(user("ai_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"Show fraud risk summary\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").value("FRAUD_INTELLIGENCE"))
                .andExpect(jsonPath("$.answer").exists())
                .andExpect(jsonPath("$.sources").isArray())
                .andExpect(jsonPath("$.recommendedActions").isArray());
    }
}
