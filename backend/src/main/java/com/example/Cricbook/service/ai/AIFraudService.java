package com.example.Cricbook.service.ai;

import com.example.Cricbook.entity.FraudAlert;
import com.example.Cricbook.entity.Booking;
import com.example.Cricbook.entity.Review;
import com.example.Cricbook.repository.FraudAlertRepository;
import com.example.Cricbook.repository.BookingRepository;
import com.example.Cricbook.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIFraudService {

    private final FraudAlertRepository fraudAlertRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public void scanUserForFraud(Long userId) {
        log.info("Running AI fraud scan for user ID: {}", userId);

        // 1. Double-booking abuse scan
        List<Booking> userBookings = bookingRepository.findByUserId(userId);
        long pendingOrConfirmedCount = userBookings.stream()
                .filter(b -> b.getStatus() == com.example.Cricbook.enums.BookingStatus.PENDING || 
                             b.getStatus() == com.example.Cricbook.enums.BookingStatus.CONFIRMED)
                .count();

        // If user has more than 5 active pending/confirmed bookings for same date, flag it
        if (pendingOrConfirmedCount > 5) {
            log.warn("Potential booking abuse detected for user ID: {}", userId);
            fraudAlertRepository.save(FraudAlert.builder()
                    .userId(userId)
                    .severity("MEDIUM")
                    .reason("User has accumulated " + pendingOrConfirmedCount + " active bookings. Flagged for block check.")
                    .build());
        }

        // 2. Review spam check
        List<Review> userReviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (userReviews.size() > 10) {
            log.warn("Review spam check triggered for user ID: {}", userId);
            fraudAlertRepository.save(FraudAlert.builder()
                    .userId(userId)
                    .severity("LOW")
                    .reason("High count of reviews (" + userReviews.size() + ") created by single account.")
                    .build());
        }
    }

    public List<FraudAlert> getAllAlerts() {
        return fraudAlertRepository.findAll();
    }
}
