package com.example.Cricbook.service;

import com.example.Cricbook.dto.AdminAnalyticsResponse;
import com.example.Cricbook.dto.AdminReviewResponse;
import com.example.Cricbook.dto.AdminUserResponse;
import com.example.Cricbook.dto.TurfResponse;
import com.example.Cricbook.entity.Booking;
import com.example.Cricbook.entity.Review;
import com.example.Cricbook.entity.Turf;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.enums.RoleName;
import com.example.Cricbook.enums.TurfStatus;
import com.example.Cricbook.enums.UserStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.mapper.TurfMapper;
import com.example.Cricbook.repository.BookingRepository;
import com.example.Cricbook.repository.ReviewRepository;
import com.example.Cricbook.repository.TurfRepository;
import com.example.Cricbook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SuperAdminService {

    private final UserRepository userRepository;
    private final TurfRepository turfRepository;
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final TurfMapper turfMapper;
    private final ReviewService reviewService;

    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getAnalytics() {
        List<User> users = userRepository.findAll();
        List<Turf> turfs = turfRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();
        BigDecimal totalRevenue = bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
                .map(Booking::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AdminAnalyticsResponse.builder()
                .totalUsers((long) users.size())
                .activeUsers(users.stream().filter(user -> user.getStatus() == UserStatus.ACTIVE).count())
                .totalOwners(users.stream().filter(user -> hasRole(user, RoleName.ROLE_OWNER)).count())
                .totalTurfs((long) turfs.size())
                .activeTurfs(turfs.stream().filter(turf -> turf.getStatus() == TurfStatus.ACTIVE).count())
                .totalBookings((long) bookings.size())
                .totalRevenue(totalRevenue)
                .reviewCount(reviewRepository.count())
                .flaggedRiskCount(bookings.stream().filter(booking -> booking.getStatus() == BookingStatus.CANCELLED).count())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getOwners() {
        return userRepository.findAll().stream()
                .filter(user -> hasRole(user, RoleName.ROLE_OWNER))
                .map(this::mapUser)
                .toList();
    }

    @Transactional
    public AdminUserResponse updateUserStatus(Long userId, UserStatus status) {
        if (status == null) {
            throw new ApiException("Status is required");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));
        user.setStatus(status);
        return mapUser(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<TurfResponse> getTurfsForVerification() {
        return turfRepository.findAll().stream()
                .filter(turf -> turf.getStatus() != TurfStatus.ACTIVE)
                .map(turfMapper::toTurfResponse)
                .toList();
    }

    @Transactional
    public TurfResponse updateTurfStatus(Long turfId, TurfStatus status) {
        if (status == null) {
            throw new ApiException("Status is required");
        }
        Turf turf = turfRepository.findById(turfId)
                .orElseThrow(() -> new ApiException("Turf not found"));
        turf.setStatus(status);
        return turfMapper.toTurfResponse(turfRepository.save(turf));
    }

    @Transactional(readOnly = true)
    public List<AdminReviewResponse> getReviews() {
        return reviewRepository.findAll().stream()
                .filter(review -> review.getStatus() == null || !"DELETED".equalsIgnoreCase(review.getStatus()))
                .map(this::mapReview)
                .toList();
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        reviewService.deleteReviewByAdmin(reviewId);
    }

    private AdminUserResponse mapUser(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .roles(user.getRoles().stream().map(role -> role.getName().name()).toList())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private AdminReviewResponse mapReview(Review review) {
        Turf turf = turfRepository.findById(review.getTurfId()).orElse(null);
        return AdminReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUserId())
                .turfId(review.getTurfId())
                .turfName(turf == null ? null : turf.getName())
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .status(review.getStatus() == null ? "PUBLISHED" : review.getStatus())
                .moderationReason(review.getModerationReason())
                .ownerReply(review.getOwnerReply())
                .helpfulCount(review.getHelpfulCount() == null ? 0 : review.getHelpfulCount())
                .notHelpfulCount(review.getNotHelpfulCount() == null ? 0 : review.getNotHelpfulCount())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
    }
}
