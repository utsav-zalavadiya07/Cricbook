package com.example.Cricbook.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class MultiSportExpansionControllerTest {

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
    void adminSportsDashboardReturnsConfigurationReadiness() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sports/dashboard")
                        .with(user("sports_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.configuredSports", greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.sportTemplates").isArray())
                .andExpect(jsonPath("$.turfCoverage").isArray())
                .andExpect(jsonPath("$.crossModuleStatus").isArray())
                .andExpect(jsonPath("$.futureSportReadiness.noCodeOnboarding").value(true))
                .andExpect(jsonPath("$.governanceControls").isArray())
                .andExpect(jsonPath("$.priorityActions").isArray());
    }

    @Test
    void publicCatalogAndRulesExposeActiveSports() throws Exception {
        mockMvc.perform(get("/api/v1/sports/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").exists())
                .andExpect(jsonPath("$[0].scoringType").exists());

        mockMvc.perform(get("/api/v1/sports/CRICKET/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CRICKET"))
                .andExpect(jsonPath("$.scoreModel.primaryMetric").value("runs"))
                .andExpect(jsonPath("$.winnerLogic").exists())
                .andExpect(jsonPath("$.dynamicUiHints").isArray());
    }

    @Test
    void adminCanCreateFutureSportAndSimulateRules() throws Exception {
        String code = "PADEL_TEST_" + System.nanoTime();
        String payload = """
                {
                  "name": "%s",
                  "code": "%s",
                  "icon": "PD",
                  "active": true,
                  "minPlayers": 2,
                  "maxPlayers": 4,
                  "scoringType": "POINTS",
                  "matchType": "DOUBLES",
                  "rankingType": "SET_POINTS",
                  "rules": "Padel points and set rules"
                }
                """.formatted("Padel Test " + code, code);

        mockMvc.perform(post("/api/v1/admin/sports")
                        .with(user("sports_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.scoringType").value("POINTS"))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(post("/api/v1/sports/rules/simulate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sportCode\":\"" + code + "\",\"playersPerSide\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sportCode").value(code))
                .andExpect(jsonPath("$.scoreModel.primaryMetric").value("points"))
                .andExpect(jsonPath("$.winnerLogic").exists())
                .andExpect(jsonPath("$.explainability").exists());
    }
}
