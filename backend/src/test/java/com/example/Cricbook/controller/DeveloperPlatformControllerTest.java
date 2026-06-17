package com.example.Cricbook.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
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
class DeveloperPlatformControllerTest {

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
    void adminDeveloperDashboardReturnsEcosystemControls() throws Exception {
        mockMvc.perform(get("/api/v1/admin/developer-platform/dashboard")
                        .with(user("api_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.developers", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.apiProducts").isArray())
                .andExpect(jsonPath("$.ratePlans").isArray())
                .andExpect(jsonPath("$.docs").isArray())
                .andExpect(jsonPath("$.securityControls").isArray())
                .andExpect(jsonPath("$.sandbox.enabled").value(true))
                .andExpect(jsonPath("$.priorityActions").isArray());
    }

    @Test
    void developerRegistrationAppKeyAndGatewayFlowWork() throws Exception {
        DeveloperFixture fixture = createDeveloperApplicationAndKey("PRO");

        mockMvc.perform(get("/api/v1/platform-api/v1/turfs/search")
                        .header("X-Truf-Api-Key", fixture.apiKey())
                        .param("city", "Surat")
                        .param("sport", "CRICKET"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiVersion").value("v1"))
                .andExpect(jsonPath("$.application.id").value(fixture.applicationId().intValue()))
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.rateLimit.plan").value("PRO"))
                .andExpect(jsonPath("$.rateLimit.remaining", greaterThanOrEqualTo(0)));
    }

    @Test
    void oauthAndWebhookSimulationReturnSecurityMetadata() throws Exception {
        DeveloperFixture fixture = createDeveloperApplicationAndKey("ENTERPRISE");

        mockMvc.perform(post("/api/v1/developer-platform/oauth/token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "grantType", "CLIENT_CREDENTIALS",
                                "clientId", fixture.clientId(),
                                "clientSecret", fixture.clientSecret()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.clientId").value(fixture.clientId()))
                .andExpect(jsonPath("$.claims.aud").value("developer-platform"))
                .andExpect(jsonPath("$.scopes").isArray());

        JsonNode webhook = postAdmin("/api/v1/admin/developer-platform/webhooks", Map.of(
                "applicationId", fixture.applicationId(),
                "url", "https://hooks.example.com/truf",
                "eventTypes", List.of("booking.created", "payment.completed")
        ));

        mockMvc.perform(post("/api/v1/admin/developer-platform/webhooks/{id}/test", webhook.get("id").asLong())
                        .with(user("api_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf())
                        .param("eventType", "booking.created"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.eventType").value("booking.created"))
                .andExpect(jsonPath("$.signature").exists())
                .andExpect(jsonPath("$.deliveryStatus").value("DELIVERED"));
    }

    private DeveloperFixture createDeveloperApplicationAndKey(String plan) throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        JsonNode developer = postPublic("/api/v1/developers/register", Map.of(
                "name", "API Partner " + suffix,
                "email", "api-partner-" + suffix + "@example.com",
                "companyName", "Partner Labs",
                "website", "https://partner.example.com",
                "plan", plan
        ));

        JsonNode application = postAdmin("/api/v1/admin/developer-platform/applications", Map.of(
                "developerId", developer.get("id").asLong(),
                "name", "Booking API App " + suffix,
                "environment", "SANDBOX",
                "callbackUrl", "https://partner.example.com/callback",
                "scopes", List.of("turfs:read", "bookings:write", "webhooks:write")
        ));

        JsonNode key = postAdmin("/api/v1/admin/developer-platform/api-keys", Map.of(
                "applicationId", application.get("id").asLong(),
                "environment", "SANDBOX"
        ));

        return new DeveloperFixture(
                developer.get("id").asLong(),
                application.get("id").asLong(),
                application.get("clientId").asText(),
                application.get("clientSecret").asText(),
                key.get("apiKey").asText()
        );
    }

    private JsonNode postPublic(String url, Map<String, Object> payload) throws Exception {
        MvcResult result = mockMvc.perform(post(url)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode postAdmin(String url, Map<String, Object> payload) throws Exception {
        MvcResult result = mockMvc.perform(post(url)
                        .with(user("api_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String json(Map<String, Object> payload) throws Exception {
        return objectMapper.writeValueAsString(payload);
    }

    private record DeveloperFixture(Long developerId, Long applicationId, String clientId, String clientSecret, String apiKey) {
    }
}
