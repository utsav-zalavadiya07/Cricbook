package com.example.Cricbook.controller;

import com.example.Cricbook.dto.TurfSearchResponse;
import com.example.Cricbook.service.TurfSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class TurfSearchControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private TurfSearchService turfSearchService;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser
    void testSearch() throws Exception {
        when(turfSearchService.search(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/search")
                .param("city", "Test City")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void testSuggestions() throws Exception {
        when(turfSearchService.suggestions("royal", 5)).thenReturn(List.of("Royal Cricket Turf"));

        mockMvc.perform(get("/api/v1/search/suggestions")
                .param("query", "royal")
                .param("limit", "5"))
                .andExpect(status().isOk());
    }
}
