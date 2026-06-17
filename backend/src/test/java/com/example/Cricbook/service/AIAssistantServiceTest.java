package com.example.Cricbook.service;

import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.TurfStatus;
import com.example.Cricbook.repository.*;
import com.example.Cricbook.service.ai.AIAssistantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AIAssistantServiceTest {

    @Mock
    private TurfRepository turfRepository;
    @Mock
    private SlotRepository slotRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AIAssistantService assistantService;

    @Test
    void testProcessMessage_BookingIntent() {
        Long userId = 1L;
        String message = "Book slot for 8 players Surat Saturday 7PM";

        Turf turf = Turf.builder()
                .id(100L)
                .name("Surat Sports Arena")
                .city("Surat")
                .status(TurfStatus.ACTIVE)
                .pricePerHour(BigDecimal.valueOf(1000))
                .build();

        Slot slot = Slot.builder()
                .id(200L)
                .turf(turf)
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(20, 0))
                .price(BigDecimal.valueOf(1000))
                .isActive(true)
                .build();

        when(turfRepository.findAll()).thenReturn(List.of(turf));
        when(slotRepository.findByTurfId(100L)).thenReturn(List.of(slot));

        Map<String, Object> result = assistantService.processMessage(userId, message);

        assertEquals("CHECKOUT", result.get("action"));
        assertTrue(result.get("reply").toString().contains("Surat Sports Arena"));
        assertTrue(result.get("checkoutLink").toString().contains("/checkout?turfId=100&slotId=200"));
    }
}
