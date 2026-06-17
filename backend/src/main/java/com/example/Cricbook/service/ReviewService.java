package com.example.Cricbook.service;

import com.example.Cricbook.dto.ReviewActionRequests;
import com.example.Cricbook.dto.ReviewRequest;
import com.example.Cricbook.dto.ReviewResponse;
import com.example.Cricbook.dto.ReviewSummaryResponse;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.enums.RoleName;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_FLAGGED = "FLAGGED";
    private static final String STATUS_HIDDEN = "HIDDEN";
    private static final String STATUS_DELETED = "DELETED";
    private static final int MIN_COMMENT_LENGTH = 10;
    private static final int MAX_COMMENT_LENGTH = 1000;
    private static final int MAX_TITLE_LENGTH = 140;
    private static final int MAX_REPLY_LENGTH = 800;
    private static final List<BookingStatus> REVIEW_ELIGIBLE_STATUSES = List.of(BookingStatus.COMPLETED);
    private static final List<String> MODERATION_TERMS = List.of("spam", "scam", "fake", "abuse", "hate", "idiot", "stupid", "fraud");

    private final ReviewRepository reviewRepository;
    private final TurfRepository turfRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final ReviewModerationRepository reviewModerationRepository;
    private final ReviewVoteRepository reviewVoteRepository;

    @Transactional
    public ReviewResponse addReview(ReviewRequest request, Long userId) {
        validateReviewRequest(request);
        Turf turf = turfRepository.findById(request.getTurfId())
                .orElseThrow(() -> ApiException.notFound("Turf was not found."));

        boolean hasBooked = bookingRepository.existsByUserIdAndTurfIdAndStatusIn(userId, request.getTurfId(), REVIEW_ELIGIBLE_STATUSES);
        if (!hasBooked) {
            throw ApiException.forbidden("You can submit a review only after completing a booking.");
        }

        Review existing = reviewRepository.findByUserIdAndTurfId(userId, request.getTurfId()).orElse(null);
        if (existing != null && !STATUS_DELETED.equals(normalizeStatus(existing))) {
            throw ApiException.conflict("You have already submitted a review for this turf.");
        }

        Review review = existing == null ? Review.builder().userId(userId).turfId(turf.getId()).build() : existing;
        applyReviewFields(review, request);
        Review saved = reviewRepository.save(review);
        recalculateTurfRating(turf.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public ReviewResponse editReview(Long reviewId, ReviewRequest request, Long userId) {
        validateReviewRequest(request);
        Review review = requireReview(reviewId);
        if (!Objects.equals(review.getUserId(), userId)) {
            throw ApiException.forbidden("You can edit only your own review.");
        }
        if (STATUS_DELETED.equals(normalizeStatus(review))) {
            throw ApiException.conflict("Deleted reviews cannot be edited.");
        }
        if (review.getCreatedAt() != null && review.getCreatedAt().isBefore(LocalDateTime.now().minusDays(7))) {
            throw ApiException.conflict("Reviews can be edited only within 7 days.");
        }
        applyReviewFields(review, request);
        Review saved = reviewRepository.save(review);
        recalculateTurfRating(saved.getTurfId());
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteOwnReview(Long reviewId, Long userId) {
        Review review = requireReview(reviewId);
        if (!Objects.equals(review.getUserId(), userId)) {
            throw ApiException.forbidden("You can delete only your own review.");
        }
        softDeleteReview(review, "Deleted by reviewer");
    }

    @Transactional
    public void deleteReviewByAdmin(Long reviewId) {
        Review review = requireReview(reviewId);
        review.setStatus(STATUS_DELETED);
        review.setModerationReason("Deleted by admin");
        reviewRepository.save(review);
        reviewModerationRepository.save(ReviewModeration.builder()
                .reviewId(reviewId)
                .action("DELETE")
                .moderatorId(currentActor())
                .reason("Deleted by admin")
                .build());
        recalculateTurfRating(review.getTurfId());
    }

    @Transactional
    public ReviewResponse saveOwnerReply(Long reviewId, ReviewActionRequests.OwnerReplyRequest request, String username) {
        Review review = requireReview(reviewId);
        if (STATUS_DELETED.equals(normalizeStatus(review)) || STATUS_HIDDEN.equals(normalizeStatus(review))) {
            throw ApiException.conflict("Cannot reply to a deleted or hidden review.");
        }
        User owner = findUser(username);
        Turf turf = turfRepository.findById(review.getTurfId())
                .orElseThrow(() -> ApiException.notFound("Turf was not found."));
        if (!isAdmin(owner) && (turf.getOwner() == null || !Objects.equals(turf.getOwner().getId(), owner.getId()))) {
            throw ApiException.forbidden("Only the turf owner can reply to this review.");
        }

        String reply = cleanText(request == null ? null : request.getReply(), "Reply", 2, MAX_REPLY_LENGTH);
        review.setOwnerReply(reply);
        review.setOwnerReplyAt(LocalDateTime.now());
        Review saved = reviewRepository.save(review);
        reviewModerationRepository.save(ReviewModeration.builder()
                .reviewId(review.getId())
                .action("OWNER_REPLY")
                .moderatorId(username)
                .reason("Owner replied to customer review")
                .build());
        return mapToResponse(saved);
    }

    @Transactional
    public ReviewResponse voteReview(Long reviewId, Long userId, Boolean helpful) {
        if (helpful == null) {
            throw ApiException.badRequest("Helpful vote is required.");
        }
        Review review = requireReview(reviewId);
        if (!STATUS_PUBLISHED.equals(normalizeStatus(review))) {
            throw ApiException.conflict("Only published reviews can be voted on.");
        }
        if (Objects.equals(review.getUserId(), userId)) {
            throw ApiException.conflict("You cannot vote on your own review.");
        }
        ReviewVote vote = reviewVoteRepository.findByReviewIdAndUserId(reviewId, userId)
                .orElseGet(() -> ReviewVote.builder().reviewId(reviewId).userId(userId).build());
        vote.setHelpful(helpful);
        reviewVoteRepository.save(vote);
        refreshVoteCounts(review);
        return mapToResponse(reviewRepository.save(review));
    }

    @Transactional
    public ReviewResponse reportReview(Long reviewId, Long reporterId, String reason) {
        Review review = requireReview(reviewId);
        if (Objects.equals(review.getUserId(), reporterId)) {
            throw ApiException.conflict("You cannot report your own review.");
        }
        if (reviewReportRepository.findByReviewIdAndReporterId(reviewId, reporterId).isPresent()) {
            throw ApiException.conflict("You have already reported this review.");
        }
        String cleanedReason = cleanText(reason, "Report reason", 5, 500);
        reviewReportRepository.save(ReviewReport.builder()
                .reviewId(reviewId)
                .reporterId(reporterId)
                .reason(cleanedReason)
                .status("PENDING")
                .build());
        int reports = reviewReportRepository.findByReviewId(reviewId).size();
        if (reports >= 3 && STATUS_PUBLISHED.equals(normalizeStatus(review))) {
            review.setStatus(STATUS_FLAGGED);
            review.setModerationReason("Review received multiple user reports.");
            reviewModerationRepository.save(ReviewModeration.builder()
                    .reviewId(reviewId)
                    .action("AUTO_FLAGGED")
                    .moderatorId("system")
                    .reason("Review received " + reports + " reports")
                    .build());
            reviewRepository.save(review);
            recalculateTurfRating(review.getTurfId());
        }
        return mapToResponse(review);
    }

    @Transactional
    public ReviewResponse moderateReview(Long reviewId, ReviewActionRequests.ModerationRequest request, String moderator) {
        Review review = requireReview(reviewId);
        String action = request == null || request.getAction() == null ? "APPROVE" : request.getAction().trim().toUpperCase(Locale.ROOT);
        String rawReason = request == null ? null : request.getReason();
        String reason = "APPROVE".equals(action) && (rawReason == null || rawReason.isBlank())
                ? "Approved by moderator"
                : cleanText(rawReason, "Moderation reason", 3, 500);
        switch (action) {
            case "APPROVE" -> {
                review.setStatus(STATUS_PUBLISHED);
                review.setModerationReason(null);
            }
            case "REJECT", "HIDE" -> {
                review.setStatus(STATUS_HIDDEN);
                review.setModerationReason(reason);
            }
            case "DELETE" -> {
                review.setStatus(STATUS_DELETED);
                review.setModerationReason(reason);
            }
            default -> throw ApiException.badRequest("Moderation action must be APPROVE, HIDE, REJECT, or DELETE.");
        }
        reviewModerationRepository.save(ReviewModeration.builder()
                .reviewId(reviewId)
                .action(action)
                .moderatorId(moderator)
                .reason(reason)
                .build());
        Review saved = reviewRepository.save(review);
        recalculateTurfRating(saved.getTurfId());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviews(Long turfId) {
        return reviewRepository.findByTurfIdOrderByCreatedAtDesc(turfId)
                .stream()
                .filter(review -> STATUS_PUBLISHED.equals(normalizeStatus(review)))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReviewSummaryResponse getReviewSummary(Long turfId) {
        List<Review> all = reviewRepository.findByTurfIdOrderByCreatedAtDesc(turfId);
        List<Review> published = all.stream()
                .filter(review -> STATUS_PUBLISHED.equals(normalizeStatus(review)))
                .toList();
        Map<Integer, Long> breakdown = new LinkedHashMap<>();
        for (int rating = 5; rating >= 1; rating--) {
            int currentRating = rating;
            breakdown.put(rating, published.stream().filter(review -> review.getRating() == currentRating).count());
        }
        double average = published.isEmpty() ? 0 : published.stream().mapToInt(Review::getRating).average().orElse(0);
        double responseRate = published.isEmpty()
                ? 0
                : (published.stream().filter(review -> review.getOwnerReply() != null && !review.getOwnerReply().isBlank()).count() * 100.0) / published.size();
        long flagged = all.stream().filter(review -> STATUS_FLAGGED.equals(normalizeStatus(review))).count();
        int reputationScore = reputationScore(average, published.size(), responseRate, flagged);
        return ReviewSummaryResponse.builder()
                .turfId(turfId)
                .averageRating(Math.round(average * 10.0) / 10.0)
                .totalReviews((long) published.size())
                .ratingBreakdown(breakdown)
                .reputationScore(reputationScore)
                .ownerResponseRate(Math.round(responseRate * 10.0) / 10.0)
                .flaggedReviews(flagged)
                .reputationLabel(reputationScore >= 85 ? "Excellent" : reputationScore >= 70 ? "Good" : reputationScore >= 50 ? "Watchlist" : "At Risk")
                .build();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews(Long userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(review -> !STATUS_DELETED.equals(normalizeStatus(review)))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getOwnerReviews(String username) {
        Long ownerId = userRepository.findByEmailOrPhone(username)
                .orElseThrow(() -> ApiException.notFound("User account was not found."))
                .getId();

        return reviewRepository.findByTurfOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ReviewResponse mapToResponse(Review review) {
        String turfName = turfRepository.findById(review.getTurfId())
                .map(Turf::getName)
                .orElse(null);
        String userName = userRepository.findById(review.getUserId())
                .map(User::getName)
                .orElse("Player");

        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUserId())
                .userName(userName)
                .turfId(review.getTurfId())
                .turfName(turfName)
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .status(normalizeStatus(review))
                .moderationReason(review.getModerationReason())
                .verifiedBooking(Boolean.TRUE.equals(review.getVerifiedBooking()))
                .ownerReply(review.getOwnerReply())
                .ownerReplyAt(review.getOwnerReplyAt())
                .helpfulCount(review.getHelpfulCount() == null ? 0 : review.getHelpfulCount())
                .notHelpfulCount(review.getNotHelpfulCount() == null ? 0 : review.getNotHelpfulCount())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    private void recalculateTurfRating(Long turfId) {
        Turf turf = turfRepository.findById(turfId)
                .orElseThrow(() -> ApiException.notFound("Turf was not found."));
        List<Review> published = reviewRepository.findByTurfIdOrderByCreatedAtDesc(turfId).stream()
                .filter(review -> STATUS_PUBLISHED.equals(normalizeStatus(review)))
                .toList();
        double updated = published.stream().mapToInt(Review::getRating).average().orElse(0);
        turf.setRating(Math.round(updated * 10.0) / 10.0);
        turf.setTotalReviews(published.size());
        turfRepository.save(turf);
    }

    private void applyReviewFields(Review review, ReviewRequest request) {
        review.setRating(request.getRating());
        review.setTitle(cleanOptionalText(request.getTitle(), MAX_TITLE_LENGTH));
        review.setComment(cleanText(request.getComment(), "Review", MIN_COMMENT_LENGTH, MAX_COMMENT_LENGTH));
        boolean flagged = shouldAutoFlag(review.getTitle(), review.getComment());
        review.setStatus(flagged ? STATUS_FLAGGED : STATUS_PUBLISHED);
        review.setModerationReason(flagged ? "Review is pending moderation because it may contain spam or abusive content." : null);
        review.setVerifiedBooking(Boolean.TRUE);
        review.setHelpfulCount(review.getHelpfulCount() == null ? 0 : review.getHelpfulCount());
        review.setNotHelpfulCount(review.getNotHelpfulCount() == null ? 0 : review.getNotHelpfulCount());
    }

    private void validateReviewRequest(ReviewRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Review details are required.");
        }
        if (request.getTurfId() == null) {
            throw ApiException.badRequest("Turf is required.");
        }
        if (request.getRating() == null) {
            throw ApiException.badRequest("Rating is required.");
        }
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw ApiException.badRequest("Rating must be between 1 and 5 stars.");
        }
        cleanText(request.getComment(), "Review", MIN_COMMENT_LENGTH, MAX_COMMENT_LENGTH);
        cleanOptionalText(request.getTitle(), MAX_TITLE_LENGTH);
    }

    private Review requireReview(Long reviewId) {
        return reviewRepository.findById(reviewId).orElseThrow(() -> ApiException.notFound("Review was not found."));
    }

    private User findUser(String username) {
        return userRepository.findByEmailOrPhone(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
    }

    private void softDeleteReview(Review review, String reason) {
        if (STATUS_DELETED.equals(normalizeStatus(review))) {
            throw ApiException.conflict("Review is already deleted.");
        }
        review.setStatus(STATUS_DELETED);
        review.setModerationReason(reason);
        reviewRepository.save(review);
        recalculateTurfRating(review.getTurfId());
    }

    private void refreshVoteCounts(Review review) {
        List<ReviewVote> votes = reviewVoteRepository.findByReviewId(review.getId());
        review.setHelpfulCount((int) votes.stream().filter(vote -> Boolean.TRUE.equals(vote.getHelpful())).count());
        review.setNotHelpfulCount((int) votes.stream().filter(vote -> Boolean.FALSE.equals(vote.getHelpful())).count());
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);
    }

    private boolean shouldAutoFlag(String title, String comment) {
        String combined = ((title == null ? "" : title) + " " + (comment == null ? "" : comment)).toLowerCase(Locale.ROOT);
        long linkCount = combined.split("http", -1).length - 1L;
        return linkCount > 2 || MODERATION_TERMS.stream().anyMatch(combined::contains);
    }

    private String cleanText(String value, String field, int min, int max) {
        String cleaned = cleanOptionalText(value, max);
        if (cleaned == null || cleaned.length() < min) {
            throw ApiException.badRequest(field + " must be at least " + min + " characters.");
        }
        return cleaned;
    }

    private String cleanOptionalText(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String cleaned = value.trim()
                .replaceAll("<[^>]*>", "")
                .replace("<", "")
                .replace(">", "")
                .replaceAll("\\s+", " ");
        if (cleaned.length() > max) {
            throw ApiException.badRequest("Text cannot exceed " + max + " characters.");
        }
        return cleaned;
    }

    private String normalizeStatus(Review review) {
        return review.getStatus() == null || review.getStatus().isBlank() ? STATUS_PUBLISHED : review.getStatus().trim().toUpperCase(Locale.ROOT);
    }

    private int reputationScore(double average, long totalReviews, double responseRate, long flagged) {
        int ratingScore = (int) Math.round((average / 5.0) * 55);
        int volumeScore = (int) Math.min(totalReviews * 2, 25);
        int responseScore = (int) Math.round((responseRate / 100.0) * 20);
        int penalty = (int) Math.min(flagged * 8, 30);
        return Math.max(Math.min(ratingScore + volumeScore + responseScore - penalty, 100), 0);
    }

    private String currentActor() {
        return SecurityContextHolder.getContext().getAuthentication() == null ? "system" : SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
