package com.example.Cricbook.controller;

import com.example.Cricbook.dto.NotificationDtos.NotificationResponse;
import com.example.Cricbook.dto.NotificationDtos.NotificationSummaryResponse;
import com.example.Cricbook.entity.NotificationQueue;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.NotificationChannel;
import com.example.Cricbook.repository.AnnouncementRepository;
import com.example.Cricbook.repository.NotificationPreferenceRepository;
import com.example.Cricbook.repository.NotificationRepository;
import com.example.Cricbook.repository.NotificationTemplateRepository;
import com.example.Cricbook.repository.UserRepository;
import com.example.Cricbook.service.EventBusService;
import com.example.Cricbook.service.NotificationAnalyticsService;
import com.example.Cricbook.service.NotificationService;
import com.example.Cricbook.service.TemplateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private NotificationAnalyticsService analyticsService;

    @Mock
    private EventBusService eventBusService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TemplateService templateService;

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private AnnouncementRepository announcementRepository;

    @InjectMocks
    private NotificationController controller;

    @Test
    void getMyNotificationsMovesWeekOldItemsIntoArchiveView() {
        User user = User.builder().id(7L).email("owner@test.com").build();
        NotificationQueue freshUnread = notification(1L, "Fresh", LocalDateTime.now().minusDays(1), null, null, "SENT");
        NotificationQueue archivedRead = notification(2L, "Archived read", LocalDateTime.now().minusDays(8), LocalDateTime.now().minusDays(7), null, "SENT");
        NotificationQueue archivedUnread = notification(3L, "Archived unread", LocalDateTime.now().minusDays(9), null, null, "SENT");

        when(userRepository.findByEmailOrPhone("owner@test.com")).thenReturn(Optional.of(user));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(freshUnread, archivedRead, archivedUnread));

        ResponseEntity<List<NotificationResponse>> allResponse = controller.getMyNotifications(principal(), "ALL", null, null, null, null, null, null);
        ResponseEntity<List<NotificationResponse>> archivedResponse = controller.getMyNotifications(principal(), "ARCHIVED", null, null, null, null, null, null);

        assertEquals(1, allResponse.getBody().size());
        assertEquals(1L, allResponse.getBody().get(0).getId());

        assertEquals(2, archivedResponse.getBody().size());
        NotificationResponse readArchived = archivedResponse.getBody().stream().filter(item -> item.getId() == 2L).findFirst().orElseThrow();
        NotificationResponse unreadArchived = archivedResponse.getBody().stream().filter(item -> item.getId() == 3L).findFirst().orElseThrow();
        assertTrue(readArchived.isArchived());
        assertTrue(readArchived.isRead());
        assertTrue(unreadArchived.isArchived());
        assertFalse(unreadArchived.isRead());
    }

    @Test
    void getSummaryExcludesAutoArchivedNotificationsFromUnreadCount() {
        User user = User.builder().id(7L).email("owner@test.com").build();
        NotificationQueue freshUnread = notification(1L, "Fresh", LocalDateTime.now().minusDays(1), null, null, "SENT");
        NotificationQueue archivedRead = notification(2L, "Archived read", LocalDateTime.now().minusDays(8), LocalDateTime.now().minusDays(7), null, "SENT");
        NotificationQueue archivedUnread = notification(3L, "Archived unread", LocalDateTime.now().minusDays(9), null, null, "SENT");

        when(userRepository.findByEmailOrPhone("owner@test.com")).thenReturn(Optional.of(user));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(freshUnread, archivedRead, archivedUnread));

        ResponseEntity<NotificationSummaryResponse> response = controller.getSummary(principal());

        assertEquals(3, response.getBody().getTotal());
        assertEquals(1, response.getBody().getUnread());
        assertEquals(1, response.getBody().getRead());
        assertEquals(2, response.getBody().getArchived());
    }

    private NotificationQueue notification(Long id, String title, LocalDateTime createdAt, LocalDateTime readAt, LocalDateTime archivedAt, String status) {
        NotificationQueue notification = new NotificationQueue();
        notification.setId(id);
        notification.setUserId(7L);
        notification.setEmail("owner@test.com");
        notification.setTitle(title);
        notification.setMessage(title + " message");
        notification.setChannel(NotificationChannel.PUSH);
        notification.setCategory("GENERAL");
        notification.setPriority("NORMAL");
        notification.setStatus(status);
        notification.setCreatedAt(createdAt);
        notification.setReadAt(readAt);
        notification.setArchivedAt(archivedAt);
        return notification;
    }

    private UserDetails principal() {
        return org.springframework.security.core.userdetails.User.withUsername("owner@test.com")
                .password("password")
                .authorities("ROLE_USER")
                .build();
    }
}
