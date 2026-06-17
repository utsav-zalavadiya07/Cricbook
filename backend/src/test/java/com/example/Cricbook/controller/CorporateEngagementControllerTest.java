package com.example.Cricbook.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CorporateEngagementControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @org.springframework.beans.factory.annotation.Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void adminCorporateDashboardReturnsB2BGovernanceControls() throws Exception {
        mockMvc.perform(get("/api/v1/admin/corporate/dashboard")
                        .with(user("corporate_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.companies", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.billing").exists())
                .andExpect(jsonPath("$.analytics").exists())
                .andExpect(jsonPath("$.crm").exists())
                .andExpect(jsonPath("$.governanceControls").isArray())
                .andExpect(jsonPath("$.priorityActions").isArray());
    }

    @Test
    void corporateLifecycleFromCompanyToWellnessBillingAndLeagueWorks() throws Exception {
        JsonNode company = createApprovedCompany();

        JsonNode location = postAdmin("/api/v1/admin/corporate/locations", Map.of(
                "companyId", company.get("id").asLong(),
                "name", "Ahmedabad HQ",
                "city", "Ahmedabad",
                "employeeCount", 250
        ));

        JsonNode department = postAdmin("/api/v1/admin/corporate/departments", Map.of(
                "companyId", company.get("id").asLong(),
                "name", "Engineering",
                "costCenter", "ENG-001"
        ));

        JsonNode employee = postAdmin("/api/v1/admin/corporate/employees", Map.of(
                "companyId", company.get("id").asLong(),
                "locationId", location.get("id").asLong(),
                "departmentId", department.get("id").asLong(),
                "name", "Corporate Player",
                "email", "employee-" + System.nanoTime() + "@example.com",
                "employeeCode", "E-1001",
                "role", "PLAYER"
        ));

        JsonNode team = postAdmin("/api/v1/admin/corporate/teams", Map.of(
                "companyId", company.get("id").asLong(),
                "departmentId", department.get("id").asLong(),
                "name", "Engineering Strikers",
                "sport", "CRICKET",
                "captainEmployeeId", employee.get("id").asLong()
        ));

        postAdmin("/api/v1/admin/corporate/team-memberships", Map.of(
                "teamId", team.get("id").asLong(),
                "employeeId", employee.get("id").asLong(),
                "role", "CAPTAIN"
        ));

        JsonNode bookingPlan = postAdmin("/api/v1/admin/corporate/booking-plans", Map.of(
                "companyId", company.get("id").asLong(),
                "departmentId", department.get("id").asLong(),
                "sport", "CRICKET",
                "turfName", "Prime Corporate Turf",
                "city", "Ahmedabad",
                "recurrenceType", "WEEKLY",
                "startDate", "2026-06-03",
                "endDate", "2026-07-03",
                "monthlySpend", 180000
        ));

        JsonNode tournament = postAdmin("/api/v1/admin/corporate/tournaments", Map.of(
                "companyId", company.get("id").asLong(),
                "name", "Engineering Premier League",
                "sport", "CRICKET",
                "scope", "DEPARTMENT",
                "teamCount", 8,
                "fixturesGenerated", true,
                "status", "ONGOING"
        ));

        JsonNode wellness = postAdmin("/api/v1/admin/corporate/wellness-programs", Map.of(
                "companyId", company.get("id").asLong(),
                "title", "Friday Fitness League",
                "sport", "CRICKET",
                "programType", "PARTICIPATION",
                "rewardPoints", 100,
                "status", "PUBLISHED"
        ));

        JsonNode activity = postAdmin("/api/v1/admin/corporate/wellness-activities", Map.of(
                "programId", wellness.get("id").asLong(),
                "employeeId", employee.get("id").asLong(),
                "activityType", "MATCH_ATTENDANCE",
                "attendanceStatus", "PRESENT",
                "sportsHours", 2,
                "completed", true
        ));

        JsonNode reward = postAdmin("/api/v1/admin/corporate/rewards", Map.of(
                "employeeId", employee.get("id").asLong(),
                "sourceType", "TOURNAMENT_WIN",
                "points", 50,
                "summary", "Won weekly challenge"
        ));

        JsonNode invoice = postAdmin("/api/v1/admin/corporate/invoices", Map.of(
                "companyId", company.get("id").asLong(),
                "billingPeriod", "JUNE_2026",
                "amount", 180000,
                "taxAmount", 0
        ));

        JsonNode paidInvoice = postAdmin("/api/v1/admin/corporate/invoices/" + invoice.get("id").asLong() + "/payment", Map.of(
                "status", "PAID"
        ));

        JsonNode contract = postAdmin("/api/v1/admin/corporate/contracts", Map.of(
                "companyId", company.get("id").asLong(),
                "planName", "Annual Wellness Pro",
                "annualValue", 2400000,
                "startDate", "2026-06-01",
                "endDate", "2027-05-31",
                "renewalProbability", 85,
                "status", "ACTIVE"
        ));

        JsonNode league = postAdmin("/api/v1/admin/corporate/leagues", Map.of(
                "name", "Ahmedabad Corporate Cricket League",
                "sport", "CRICKET",
                "organizerCompanyId", company.get("id").asLong(),
                "registeredCompanies", 4,
                "teamCount", 16,
                "fixturesGenerated", true,
                "status", "ONGOING"
        ));

        mockMvc.perform(get("/api/v1/admin/corporate/analytics")
                        .with(user("corporate_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sportsAdoptionRate").exists())
                .andExpect(jsonPath("$.wellnessScore").exists());

        mockMvc.perform(get("/api/v1/admin/corporate/billing")
                        .with(user("corporate_admin@cricbook.com").roles("SUPER_ADMIN"))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeArr").exists())
                .andExpect(jsonPath("$.paidInvoices", greaterThanOrEqualTo(1)));

        assert bookingPlan.get("generatedBookings").asInt() >= 4;
        assert tournament.get("fixturesGenerated").asBoolean();
        assert activity.get("completed").asBoolean();
        assert reward.get("balanceAfter").asInt() >= 150;
        assert "PAID".equals(paidInvoice.get("status").asText());
        assert contract.get("renewalProbability").asInt() == 85;
        assert league.get("fixturesGenerated").asBoolean();
    }

    @Test
    void publicCorporateMarketplaceShowsOnlyApprovedCompanies() throws Exception {
        JsonNode company = createApprovedCompany();

        mockMvc.perform(get("/api/v1/corporates/marketplace")
                        .param("city", company.get("city").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(company.get("id").asInt()));
    }

    private JsonNode createApprovedCompany() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", "Truf Corporate " + suffix);
        payload.put("gstNumber", "24ABCDE1234F1Z5");
        payload.put("industry", "Technology");
        payload.put("city", "Ahmedabad");
        payload.put("locations", "Ahmedabad HQ, Gandhinagar Office");
        payload.put("hrContactName", "HR Lead");
        payload.put("hrEmail", "hr-" + suffix + "@example.com");
        payload.put("adminEmail", "admin-" + suffix + "@example.com");
        payload.put("monthlyBudget", 250000);
        payload.put("annualContractValue", 2400000);
        JsonNode company = postPublic("/api/v1/corporates/register", payload);
        postAdmin("/api/v1/admin/corporate/companies/" + company.get("id").asLong() + "/status", Map.of(
                "status", "APPROVED",
                "notes", "Verified HR and documents"
        ));
        return company;
    }

    private JsonNode postPublic(String path, Map<String, Object> payload) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode postAdmin(String path, Map<String, Object> payload) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .with(user("corporate_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
