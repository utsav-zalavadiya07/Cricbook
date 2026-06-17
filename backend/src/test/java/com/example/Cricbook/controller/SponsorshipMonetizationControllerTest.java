package com.example.Cricbook.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
class SponsorshipMonetizationControllerTest {

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
    void adminMonetizationDashboardReturnsKpisAndControlQueues() throws Exception {
        mockMvc.perform(get("/api/v1/admin/monetization/dashboard")
                        .with(user("monetization_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.brands").exists())
                .andExpect(jsonPath("$.kpis.paidRevenue").exists())
                .andExpect(jsonPath("$.revenueBreakdown.SPONSORSHIP").exists())
                .andExpect(jsonPath("$.marketplace").isArray())
                .andExpect(jsonPath("$.brandPipeline").isArray())
                .andExpect(jsonPath("$.proposalBoard").isArray())
                .andExpect(jsonPath("$.campaignPerformance").isArray())
                .andExpect(jsonPath("$.priorityActions").isArray());
    }

    @Test
    void sponsorshipMarketplaceReturnsOpenOpportunities() throws Exception {
        mockMvc.perform(get("/api/v1/sponsorship/marketplace")
                        .with(user("brand_partner@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void adminCanCreateBrandOpportunityProposalCampaignAndTrackAdEvent() throws Exception {
        String unique = String.valueOf(System.nanoTime());
        MvcResult brandResult = mockMvc.perform(post("/api/v1/admin/monetization/brands")
                        .with(user("monetization_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyName": "Boundary Gear %s",
                                  "brandType": "SPORTS_BRAND",
                                  "gstNumber": "22AAAAA0000A1Z5",
                                  "website": "https://boundarygear.example",
                                  "email": "boundary-%s@example.com",
                                  "phone": "9999999999",
                                  "city": "Ahmedabad"
                                }
                                """.formatted(unique, unique)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
                .andReturn();
        Number brandId = JsonPath.read(brandResult.getResponse().getContentAsString(), "$.id");

        MvcResult opportunityResult = mockMvc.perform(post("/api/v1/admin/monetization/opportunities")
                        .with(user("monetization_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Ahmedabad Premier League Title Sponsor %s",
                                  "opportunityType": "TOURNAMENT",
                                  "city": "Ahmedabad",
                                  "sportCategory": "CRICKET",
                                  "audienceSize": 2500,
                                  "amount": 150000,
                                  "benefits": "Naming rights, banners, sponsored posts"
                                }
                                """.formatted(unique)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();
        Number opportunityId = JsonPath.read(opportunityResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/v1/sponsorship/opportunities/{id}/proposals", opportunityId.longValue())
                        .with(user("brand_partner@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "brandId": %d,
                                  "message": "We want naming rights and homepage promotion.",
                                  "proposedAmount": 125000,
                                  "deliverables": "Logo, banners, lead report"
                                }
                                """.formatted(brandId.longValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brandId").value(brandId.longValue()))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        MvcResult campaignResult = mockMvc.perform(post("/api/v1/admin/monetization/campaigns")
                        .with(user("monetization_admin@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "brandId": %d,
                                  "name": "Featured Turf Banner %s",
                                  "campaignType": "BANNER_AD",
                                  "placement": "HOMEPAGE",
                                  "targetAudience": "PLAYERS",
                                  "city": "Ahmedabad",
                                  "dailyBudget": 5000,
                                  "lifetimeBudget": 50000
                                }
                                """.formatted(brandId.longValue(), unique)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        Number campaignId = JsonPath.read(campaignResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/v1/sponsorship/campaigns/{id}/track", campaignId.longValue())
                        .with(user("brand_partner@cricbook.com").roles("SUPER_ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "CLICK",
                                  "sessionId": "session-%s",
                                  "cost": 25
                                }
                                """.formatted(unique)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clicks").value(1))
                .andExpect(jsonPath("$.spend").value(25));
    }
}
