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
class EnterprisePlatformControllerTest {

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
    void adminEnterpriseDashboardReturnsKpisDrillDownAndGovernance() throws Exception {
        mockMvc.perform(get("/api/v1/admin/enterprise/dashboard")
                        .with(user("enterprise_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.totalCenters").exists())
                .andExpect(jsonPath("$.kpis.totalTurfs").exists())
                .andExpect(jsonPath("$.kpis.totalStaff").exists())
                .andExpect(jsonPath("$.drillDown.platformToCity").exists())
                .andExpect(jsonPath("$.centerRankings").isArray())
                .andExpect(jsonPath("$.crm").exists())
                .andExpect(jsonPath("$.governance.status").exists())
                .andExpect(jsonPath("$.insights").isArray());
    }

    @Test
    void adminEnterpriseListsExposeCentersStaffAndFranchises() throws Exception {
        mockMvc.perform(get("/api/v1/admin/enterprise/centers")
                        .with(user("enterprise_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/admin/enterprise/staff")
                        .with(user("enterprise_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/admin/enterprise/franchises")
                        .with(user("enterprise_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void adminEnterpriseReportsAndGovernanceAreAuditReady() throws Exception {
        mockMvc.perform(get("/api/v1/admin/enterprise/reports")
                        .with(user("enterprise_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenueReport").exists())
                .andExpect(jsonPath("$.occupancyReport").isArray())
                .andExpect(jsonPath("$.staffPerformanceReport").isArray())
                .andExpect(jsonPath("$.customerGrowthReport.crmAggregation").exists())
                .andExpect(jsonPath("$.exports").isArray());

        mockMvc.perform(get("/api/v1/admin/enterprise/governance")
                        .with(user("enterprise_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.controls").isArray())
                .andExpect(jsonPath("$.settings").isArray());
    }
}
