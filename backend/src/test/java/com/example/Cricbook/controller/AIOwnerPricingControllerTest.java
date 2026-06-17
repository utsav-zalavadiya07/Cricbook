package com.example.Cricbook.controller;

import com.example.Cricbook.entity.PricingRule;
import com.example.Cricbook.service.ai.AIDynamicPricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
class AIOwnerPricingControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AIDynamicPricingService pricingService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "OWNER")
    void ownerCanReadPricingRulesForOwnedTurf() throws Exception {
        PricingRule rule = PricingRule.builder()
                .id(1L)
                .turfId(10L)
                .ruleName("WEEKEND")
                .adjustmentPercent(BigDecimal.valueOf(20))
                .isActive(true)
                .build();
        when(pricingService.getRules("owner@example.com", 10L)).thenReturn(List.of(rule));

        mockMvc.perform(get("/api/v1/owner/pricing/rules")
                        .param("turfId", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ruleName").value("WEEKEND"));
    }

    @Test
    @WithMockUser(username = "player@example.com", roles = "USER")
    void playerCannotReadOwnerPricingRules() throws Exception {
        mockMvc.perform(get("/api/v1/owner/pricing/rules")
                        .param("turfId", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(pricingService, never()).getRules(eq("player@example.com"), eq(10L));
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "OWNER")
    void ownerCanToggleRuleWithPercent() throws Exception {
        mockMvc.perform(post("/api/v1/owner/pricing/toggle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "turfId": 10,
                                  "ruleName": "PEAK_HOUR",
                                  "active": false,
                                  "adjustmentPercent": 25
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleName").value("PEAK_HOUR"))
                .andExpect(jsonPath("$.active").value(false));

        verify(pricingService).toggleRule(
                eq("owner@example.com"),
                eq(10L),
                eq("PEAK_HOUR"),
                eq(false),
                eq(BigDecimal.valueOf(25))
        );
    }
}
