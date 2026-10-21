package com.example.Cricbook.service;

import com.example.Cricbook.dto.ReviewActionRequests;
import com.example.Cricbook.dto.ReviewRequest;
import com.example.Cricbook.dto.ReviewResponse;
import com.example.Cricbook.dto.ReviewSummaryResponse;
import com.example.Cricbook.entity.Review;
import com.example.Cricbook.entity.ReviewReport;
import com.example.Cricbook.entity.ReviewVote;
import com.example.Cricbook.entity.Turf;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.BookingRepository;
import com.example.Cricbook.repository.ReviewModerationRepository;
import com.example.Cricbook.repository.ReviewReportRepository;
import com.example.Cricbook.repository.ReviewRepository;
import com.example.Cricbook.repository.ReviewVoteRepository;
import com.example.Cricbook.repository.TurfRepository;
import com.example.Cricbook.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long OWNER_ID = 5L;
    private static final Long TURF_ID = 10L;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private TurfRepository turfRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewReportRepository reviewReportRepository;

    @Mock
    private ReviewModerationRepository reviewModerationRepository;

    @Mock
    private ReviewVoteRepository reviewVoteRepository;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void addReviewRequiresCompletedBookingAndRecalculatesPublishedRating() {
        ReviewRequest request = validRequest();
        Turf turf = turf();

        when(turfRepository.findById(TURF_ID)).thenReturn(Optional.of(turf));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID, "Player One", "player@test.com")));
        when(bookingRepository.existsByUserIdAndTurfIdAndStatusIn(eq(USER_ID), eq(TURF_ID), eq(List.of(BookingStatus.COMPLETED)))).thenReturn(true);
        when(reviewRepository.findByUserIdAndTurfId(USER_ID, TURF_ID)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(99L);
            review.setCreatedAt(LocalDateTime.now());
            return review;
        });
        when(reviewRepository.findByTurfIdOrderByCreatedAtDesc(TURF_ID)).thenReturn(List.of(
                review(20L, 3L, 4, "PUBLISHED"),
                review(21L, 4L, 4, "PUBLISHED"),
                review(99L, USER_ID, 5, "PUBLISHED")
        ));

        ReviewResponse response = reviewService.addReview(request, USER_ID);

        assertEquals("PUBLISHED", response.getStatus());
        assertEquals("Well maintained", response.getTitle());
        assertTrue(response.getVerifiedBooking());
        assertEquals(4.3, turf.getRating());
        assertEquals(3, turf.getTotalReviews());
        verify(turfRepository).save(turf);
    }

    @Test
    void addReviewRejectsInvalidRatingBeforeSaving() {
        ReviewRequest request = validRequest();
        request.setRating(6);

        ApiException exception = assertThrows(ApiException.class, () -> reviewService.addReview(request, USER_ID));

        assertEquals("Rating must be between 1 and 5 stars.", exception.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void addReviewRequiresCompletedBooking() {
        ReviewRequest request = validRequest();

        when(turfRepository.findById(TURF_ID)).thenReturn(Optional.of(turf()));
        when(bookingRepository.existsByUserIdAndTurfIdAndStatusIn(eq(USER_ID), eq(TURF_ID), eq(List.of(BookingStatus.COMPLETED)))).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class, () -> reviewService.addReview(request, USER_ID));

        assertEquals("You can submit a review only after completing a booking.", exception.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void editReviewRejectsExpiredEditWindow() {
        Review oldReview = review(31L, USER_ID, 4, "PUBLISHED");
        oldReview.setCreatedAt(LocalDateTime.now().minusDays(8));

        when(reviewRepository.findById(31L)).thenReturn(Optional.of(oldReview));

        ApiException exception = assertThrows(ApiException.class, () -> reviewService.editReview(31L, validRequest(), USER_ID));

        assertEquals("Reviews can be edited only within 7 days.", exception.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void ownerReplyRequiresTurfOwnerAndStoresReply() {
        Review review = review(32L, USER_ID, 5, "PUBLISHED");
        ReviewActionRequests.OwnerReplyRequest request = new ReviewActionRequests.OwnerReplyRequest();
        request.setReply("Thanks for visiting. We will keep improving the lights.");

        when(reviewRepository.findById(32L)).thenReturn(Optional.of(review));
        when(userRepository.findByEmailOrPhone("owner@test.com")).thenReturn(Optional.of(user(OWNER_ID, "Owner", "owner@test.com")));
        when(turfRepository.findById(TURF_ID)).thenReturn(Optional.of(turf()));
        when(reviewRepository.save(review)).thenReturn(review);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID, "Player One", "player@test.com")));

        ReviewResponse response = reviewService.saveOwnerReply(32L, request, "owner@test.com");

        assertEquals(request.getReply(), response.getOwnerReply());
        assertTrue(response.getOwnerReplyAt() != null);
        verify(reviewModerationRepository).save(any());
    }

    @Test
    void voteReviewUpdatesHelpfulCountsAndBlocksOwnVote() {
        Review ownReview = review(40L, USER_ID, 4, "PUBLISHED");
        when(reviewRepository.findById(40L)).thenReturn(Optional.of(ownReview));

        ApiException ownVote = assertThrows(ApiException.class, () -> reviewService.voteReview(40L, USER_ID, true));
        assertEquals("You cannot vote on your own review.", ownVote.getMessage());

        when(reviewVoteRepository.findByReviewIdAndUserId(40L, OTHER_USER_ID)).thenReturn(Optional.empty());
        when(reviewVoteRepository.findByReviewId(40L)).thenReturn(List.of(
                ReviewVote.builder().reviewId(40L).userId(OTHER_USER_ID).helpful(true).build()
        ));
        when(reviewRepository.save(ownReview)).thenReturn(ownReview);
        when(turfRepository.findById(TURF_ID)).thenReturn(Optional.of(turf()));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID, "Player One", "player@test.com")));

        ReviewResponse response = reviewService.voteReview(40L, OTHER_USER_ID, true);

        assertEquals(1, response.getHelpfulCount());
        assertEquals(0, response.getNotHelpfulCount());
        verify(reviewVoteRepository).save(any());
    }

    @Test
    void reportReviewAutoFlagsAfterThreeReports() {
        Review review = review(50L, USER_ID, 2, "PUBLISHED");

        when(reviewRepository.findById(50L)).thenReturn(Optional.of(review));
        when(reviewReportRepository.findByReviewIdAndReporterId(50L, OTHER_USER_ID)).thenReturn(Optional.empty());
        when(reviewReportRepository.findByReviewId(50L)).thenReturn(List.of(
                ReviewReport.builder().reviewId(50L).reporterId(OTHER_USER_ID).reason("Poor conduct").status("PENDING").build(),
                ReviewReport.builder().reviewId(50L).reporterId(3L).reason("Spam").status("PENDING").build(),
                ReviewReport.builder().reviewId(50L).reporterId(4L).reason("Fake review").status("PENDING").build()
        ));
        when(reviewRepository.save(review)).thenReturn(review);
        when(turfRepository.findById(TURF_ID)).thenReturn(Optional.of(turf()));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID, "Player One", "player@test.com")));
        when(reviewRepository.findByTurfIdOrderByCreatedAtDesc(TURF_ID)).thenReturn(List.of(review));

        ReviewResponse response = reviewService.reportReview(50L, OTHER_USER_ID, "This looks like fake feedback");

        assertEquals("FLAGGED", response.getStatus());
        assertEquals("Review received multiple user reports.", response.getModerationReason());
        verify(reviewReportRepository).save(any());
        verify(reviewModerationRepository).save(any());
    }

    @Test
    void reportReviewBlocksDuplicateReporter() {
        Review review = review(51L, USER_ID, 2, "PUBLISHED");

        when(reviewRepository.findById(51L)).thenReturn(Optional.of(review));
        when(reviewReportRepository.findByReviewIdAndReporterId(51L, OTHER_USER_ID))
                .thenReturn(Optional.of(ReviewReport.builder().reviewId(51L).reporterId(OTHER_USER_ID).reason("Spam").status("PENDING").build()));

        ApiException exception = assertThrows(ApiException.class, () -> reviewService.reportReview(51L, OTHER_USER_ID, "Still suspicious"));

        assertEquals("You have already reported this review.", exception.getMessage());
        verify(reviewReportRepository, never()).save(any());
    }

    @Test
    void summaryBuildsBreakdownAndReputationScore() {
        Review replied = review(60L, USER_ID, 5, "PUBLISHED");
        replied.setOwnerReply("Thank you");
        Review hidden = review(62L, 3L, 1, "HIDDEN");
        Review flagged = review(63L, 4L, 2, "FLAGGED");

        when(reviewRepository.findByTurfIdOrderByCreatedAtDesc(TURF_ID)).thenReturn(List.of(
                replied,
                review(61L, OTHER_USER_ID, 4, "APPROVED"),
                hidden,
                flagged
        ));

        ReviewSummaryResponse summary = reviewService.getReviewSummary(TURF_ID);

        assertEquals(2, summary.getTotalReviews());
        assertEquals(4.5, summary.getAverageRating());
        assertEquals(1, summary.getRatingBreakdown().get(5));
        assertEquals(1, summary.getRatingBreakdown().get(4));
        assertEquals(1, summary.getFlaggedReviews());
        assertEquals(50.0, summary.getOwnerResponseRate());
        assertTrue(summary.getReputationScore() > 0);
    }

    private ReviewRequest validRequest() {
        ReviewRequest request = new ReviewRequest();
        request.setTurfId(TURF_ID);
        request.setRating(5);
        request.setTitle("Well maintained");
        request.setComment("Great turf and strong lighting.");
        return request;
    }

    private Turf turf() {
        return Turf.builder()
                .id(TURF_ID)
                .name("Arena Turf")
                .owner(user(OWNER_ID, "Owner", "owner@test.com"))
                .rating(0.0)
                .totalReviews(0)
                .build();
    }

    private User user(Long id, String name, String email) {
        return User.builder()
                .id(id)
                .name(name)
                .email(email)
                .phone("90000000" + id)
                .password("secret")
                .build();
    }

    private Review review(Long id, Long userId, int rating, String status) {
        return Review.builder()
                .id(id)
                .userId(userId)
                .turfId(TURF_ID)
                .rating(rating)
                .title("Review title")
                .comment("Review comment text")
                .status(status)
                .verifiedBooking(true)
                .helpfulCount(0)
                .notHelpfulCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
