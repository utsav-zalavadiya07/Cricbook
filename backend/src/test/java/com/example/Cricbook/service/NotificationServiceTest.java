package com.example.Cricbook.service;

import com.example.Cricbook.dto.NotificationDtos;
import com.example.Cricbook.entity.Role;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.entity.NotificationPreference;
import com.example.Cricbook.entity.NotificationQueue;
import com.example.Cricbook.enums.NotificationChannel;
import com.example.Cricbook.enums.RoleName;
import com.example.Cricbook.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private EmailService emailService;

    @Mock
    private com.example.Cricbook.repository.NotificationPreferenceRepository preferenceRepository;

    @Mock
    private com.example.Cricbook.repository.DeliveryLogRepository deliveryLogRepository;

    @Mock
    private NotificationAnalyticsService analyticsService;

    @Mock
    private com.example.Cricbook.repository.NotificationAnalyticsRepository notificationAnalyticsRepository;

    @Mock
    private com.example.Cricbook.repository.UserRepository userRepository;

    @Mock
    private ReliabilityEngine reliabilityEngine;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void testPushNotification() {
        when(preferenceRepository.findByUserIdAndChannel(any(), any())).thenReturn(java.util.Optional.empty());
        notificationService.pushNotification(1L, "test@test.com", "Title", "Message");

        verify(repository).save(any(NotificationQueue.class));
    }

    @Test
    void testPushNotification_CapturesCategoryAndPriority() {
        when(preferenceRepository.findByUserIdAndChannel(any(), any())).thenReturn(Optional.empty());

        notificationService.pushNotification(
                1L,
                "test@test.com",
                " Booking alert ",
                " Slot confirmed ",
                NotificationChannel.PUSH,
                10L,
                "/bookings",
                "booking",
                "high"
        );

        ArgumentCaptor<NotificationQueue> captor = ArgumentCaptor.forClass(NotificationQueue.class);
        verify(repository).save(captor.capture());
        NotificationQueue saved = captor.getValue();
        assertEquals("Booking alert", saved.getTitle());
        assertEquals("Slot confirmed", saved.getMessage());
        assertEquals("BOOKING", saved.getCategory());
        assertEquals("HIGH", saved.getPriority());
        assertEquals("PENDING", saved.getStatus());
    }

    @Test
    void testPushNotification_SkipsDisabledCategory() {
        when(preferenceRepository.findByUserIdAndChannel(1L, NotificationChannel.PUSH))
                .thenReturn(Optional.of(NotificationPreference.builder()
                        .userId(1L)
                        .channel(NotificationChannel.PUSH)
                        .enabled(true)
                        .disabledCategories("BOOKING,MARKETING")
                        .build()));

        notificationService.pushNotification(
                1L,
                "test@test.com",
                "Title",
                "Message",
                NotificationChannel.PUSH,
                null,
                null,
                "booking",
                "normal"
        );

        verify(repository, never()).save(any(NotificationQueue.class));
    }

    @Test
    void testBroadcastQueuesOnlyTargetRoleAcrossChannels() {
        User player = User.builder()
                .id(1L)
                .name("Player")
                .email("player@test.com")
                .phone("999")
                .roles(Set.of(Role.builder().name(RoleName.ROLE_USER).build()))
                .build();
        User owner = User.builder()
                .id(2L)
                .name("Owner")
                .email("owner@test.com")
                .phone("888")
                .roles(Set.of(Role.builder().name(RoleName.ROLE_OWNER).build()))
                .build();
        NotificationDtos.BroadcastRequest request = new NotificationDtos.BroadcastRequest();
        request.setTitle("Weekend cricket");
        request.setMessage("New slots are available.");
        request.setTargetRole("USER");
        request.setChannels(List.of("PUSH", "EMAIL"));
        request.setCategory("MARKETING");
        request.setPriority("HIGH");

        when(userRepository.findAll()).thenReturn(List.of(player, owner));
        when(preferenceRepository.findByUserIdAndChannel(anyLong(), any())).thenReturn(Optional.empty());

        NotificationDtos.BroadcastResponse response = notificationService.broadcast(request);

        assertEquals(1, response.getRecipientCount());
        assertEquals(2, response.getQueuedCount());
        assertEquals(List.of("PUSH", "EMAIL"), response.getChannels());
        verify(repository, times(2)).save(any(NotificationQueue.class));
    }

    @Test
    void testProcessNotifications_Success() {
        ReflectionTestUtils.setField(notificationService, "emailEnabled", true);
        NotificationQueue nq = new NotificationQueue();
        nq.setEmail("test@test.com");
        nq.setTitle("Title");
        nq.setMessage("Message");
        nq.setStatus("PENDING");

        when(repository.findTop10ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(nq));

        notificationService.processNotifications();

        verify(emailService).send("test@test.com", "Title", "Message");
        assertEquals("SENT", nq.getStatus());
        verify(repository).save(nq);
        verify(analyticsService).recordSend(nq.getCampaignId());
    }

    @Test
    void testProcessNotifications_Failure() {
        ReflectionTestUtils.setField(notificationService, "emailEnabled", true);
        NotificationQueue nq = new NotificationQueue();
        nq.setEmail("test@test.com");
        nq.setTitle("Title");
        nq.setMessage("Message");
        nq.setStatus("PENDING");
        nq.setRetryCount(2); // Next failure will make it FAILED

        when(repository.findTop10ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(nq));
        doThrow(new RuntimeException("Email failed")).when(emailService).send(anyString(), anyString(), anyString());

        notificationService.processNotifications();

        assertEquals("FAILED", nq.getStatus());
        assertEquals(3, nq.getRetryCount());
        verify(repository).save(nq);
        verify(analyticsService, never()).recordSend(any());
    }
}
