package com.example.Cricbook.controller;

import com.example.Cricbook.dto.BookingResponse;
import com.example.Cricbook.dto.ConfirmBookingRequest;
import com.example.Cricbook.dto.CreateBookingRequest;
import com.example.Cricbook.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.jupiter.api.BeforeEach;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class BookingControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private BookingService bookingService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser
    void testCreateBooking() throws Exception {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setTurfId(1L);
        request.setSlotId(1L);

        BookingResponse response = BookingResponse.builder()
                .id(1L)
                .turfId(1L)
                .slotId(1L)
                .build();

        when(bookingService.createBooking(any(CreateBookingRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser
    void testConfirmBooking() throws Exception {
        ConfirmBookingRequest request = new ConfirmBookingRequest("pay_123", "SUCCESS");

        BookingResponse response = BookingResponse.builder()
                .id(1L)
                .build();

        when(bookingService.confirmBooking(eq(1L), any(ConfirmBookingRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/bookings/1/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser
    void testCancelBooking() throws Exception {
        BookingResponse response = BookingResponse.builder()
                .id(1L)
                .build();

        when(bookingService.cancelBooking(1L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/bookings/1/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser
    void testGetBooking() throws Exception {
        BookingResponse response = BookingResponse.builder()
                .id(1L)
                .build();

        when(bookingService.getBooking(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/bookings/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }
}
