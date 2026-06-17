package com.example.Cricbook.service;

import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.TurfStatus;
import com.example.Cricbook.repository.*;
import com.example.Cricbook.service.ai.AIRecommendationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AIRecommendationServiceTest {

    @Mock
    private RecommendationCacheRepository cacheRepository;
    @Mock
    private UserVectorRepository userVectorRepository;
    @Mock
    private TurfRepository turfRepository;
    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private AIRecommendationService recommendationService;

    @Test
    void testGetOrCreateUserVector_NewUser() {
        Long userId = 10L;
        when(userVectorRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(bookingRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
        
        when(userVectorRepository.save(any(UserVector.class))).thenAnswer(i -> i.getArguments()[0]);

        UserVector vector = recommendationService.getOrCreateUserVector(userId);

        assertNotNull(vector);
        assertEquals("Surat", vector.getPreferredCity());
        assertEquals(18, vector.getPreferredHour());
        assertEquals(0, BigDecimal.valueOf(500).compareTo(vector.getPreferredPriceMin()));
    }
}
