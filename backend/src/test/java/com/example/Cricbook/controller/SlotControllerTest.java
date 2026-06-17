package com.example.Cricbook.controller;

import com.example.Cricbook.dto.SlotAvailabilityResponse;
import com.example.Cricbook.service.SlotService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class SlotControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private SlotService slotService;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser
    void testGetAvailability() throws Exception {
        when(slotService.getAvailability(eq(1L), any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/turfs/1/availability")
                .param("date", "2024-05-10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void testGetCalendar() throws Exception {
        when(slotService.getCalendar(eq(1L), any(), any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/turfs/1/availability/calendar")
                .param("startDate", "2024-05-10")
                .param("endDate", "2024-05-20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
