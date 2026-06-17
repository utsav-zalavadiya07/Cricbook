package com.example.Cricbook.service;

import com.example.Cricbook.dto.TrustReportRequest;
import com.example.Cricbook.dto.TrustReportResponse;
import com.example.Cricbook.dto.TrustScoreResponse;
import com.example.Cricbook.entity.PlayerTrustScore;
import com.example.Cricbook.entity.TrustReport;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.AttendanceStatus;
import com.example.Cricbook.enums.TrustReportReason;
import com.example.Cricbook.enums.TrustReportStatus;
import com.example.Cricbook.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrustScoreServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private PlayerTrustScoreRepository trustScoreRepository;
    @Mock private TrustAttendanceRepository attendanceRepository;
    @Mock private PunctualityLogRepository punctualityLogRepository;
    @Mock private CancellationHistoryRepository cancellationHistoryRepository;
    @Mock private TrustReportRepository trustReportRepository;
    @Mock private TrustRiskRepository trustRiskRepository;
    @Mock private NotificationService notificationService;
    @InjectMocks private TrustScoreService trustScoreService;

    @Test
    void calculateScoreUsesWeightedReliabilityFactors() {
        User user = user(1L, "Player");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(attendanceRepository.countByUserId(1L)).thenReturn(10L);
        when(attendanceRepository.countByUserIdAndStatus(1L, AttendanceStatus.NO_SHOW)).thenReturn(1L);
        when(attendanceRepository.countByUserIdAndStatus(1L, AttendanceStatus.LATE)).thenReturn(2L);
        when(cancellationHistoryRepository.countByUserId(1L)).thenReturn(1L);
        when(trustReportRepository.countByReportedUserId(1L)).thenReturn(0L);
        when(trustScoreRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(trustScoreRepository.save(any(PlayerTrustScore.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(trustRiskRepository.findByUserId(1L)).thenReturn(Optional.empty());

        TrustScoreResponse result = trustScoreService.calculateScore(1L);

        assertNotNull(result);
        assertTrue(result.getScore() >= 70);
        assertEquals("Trusted", result.getTier());
        assertTrue(result.getAttendanceScore() < 100);
        assertTrue(result.getCancellationScore() < 100);
    }

    @Test
    void submitReportCreatesOpenReportAndRecalculatesReportedUser() {
        User reporter = user(1L, "Reporter");
        User reported = user(2L, "Reported");
        TrustReportRequest request = new TrustReportRequest();
        request.setReportedUserId(2L);
        request.setReason(TrustReportReason.MISCONDUCT);
        request.setDescription("Bad conduct after match.");

        when(userRepository.findByEmailOrPhone("reporter@test.com")).thenReturn(Optional.of(reporter));
        when(userRepository.findById(2L)).thenReturn(Optional.of(reported));
        when(trustReportRepository.save(any(TrustReport.class))).thenAnswer(invocation -> {
            TrustReport report = invocation.getArgument(0);
            report.setId(9L);
            return report;
        });
        when(trustScoreRepository.findByUserId(2L)).thenReturn(Optional.empty());
        when(trustScoreRepository.save(any(PlayerTrustScore.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(trustRiskRepository.findByUserId(2L)).thenReturn(Optional.empty());
        when(trustReportRepository.countByReportedUserId(2L)).thenReturn(1L);

        TrustReportResponse result = trustScoreService.submitReport("reporter@test.com", request);

        assertEquals(9L, result.getId());
        assertEquals(TrustReportStatus.OPEN, result.getStatus());
        assertEquals(2L, result.getReportedUserId());
    }

    private User user(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(name.toLowerCase() + "@test.com");
        return user;
    }
}
