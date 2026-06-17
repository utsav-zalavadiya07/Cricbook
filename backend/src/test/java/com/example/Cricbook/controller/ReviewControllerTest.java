package com.example.Cricbook.controller;

import com.example.Cricbook.dto.ReviewRequest;
import com.example.Cricbook.dto.ReviewResponse;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.repository.UserRepository;
import com.example.Cricbook.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class ReviewControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(username = "test@test.com")
    void testAddReview() throws Exception {
        ReviewRequest request = new ReviewRequest();
        request.setTurfId(1L);
        request.setRating(5);
        request.setTitle("Great turf");
        request.setComment("Great turf and lighting.");

        User mockUser = User.builder().id(1L).email("test@test.com").build();
        when(userRepository.findByEmailOrPhone("test@test.com")).thenReturn(Optional.of(mockUser));

        when(reviewService.addReview(any(ReviewRequest.class), eq(1L))).thenReturn(ReviewResponse.builder()
                .id(10L)
                .userId(1L)
                .turfId(1L)
                .rating(5)
                .title("Great turf")
                .comment("Great turf and lighting.")
                .status("PUBLISHED")
                .verifiedBooking(true)
                .build());

        mockMvc.perform(post("/api/v1/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
