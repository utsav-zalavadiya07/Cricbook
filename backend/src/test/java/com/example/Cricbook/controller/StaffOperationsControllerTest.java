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
class StaffOperationsControllerTest {

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
    void adminWorkforceDashboardReturnsExecutionKpis() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workforce/dashboard")
                        .with(user("workforce_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.todayBookings").exists())
                .andExpect(jsonPath("$.kpis.pendingCheckIns").exists())
                .andExpect(jsonPath("$.kpis.openTasks").exists())
                .andExpect(jsonPath("$.kpis.maintenanceIssues").exists())
                .andExpect(jsonPath("$.analytics.performanceScore").exists())
                .andExpect(jsonPath("$.announcements").isArray());
    }

    @Test
    void adminWorkforceListsExposeOperationalQueues() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workforce/tasks")
                        .with(user("workforce_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/admin/workforce/maintenance")
                        .with(user("workforce_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/admin/workforce/equipment")
                        .with(user("workforce_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/admin/workforce/emergencies")
                        .with(user("workforce_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void adminWorkforceReportsExposeExportReadyPayload() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workforce/reports")
                        .with(user("workforce_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendanceReport").isArray())
                .andExpect(jsonPath("$.taskReport").isArray())
                .andExpect(jsonPath("$.maintenanceReport").isArray())
                .andExpect(jsonPath("$.staffPerformanceReport").isArray())
                .andExpect(jsonPath("$.exports").isArray());
    }
}
