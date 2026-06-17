package com.example.Cricbook.controller;

import com.example.Cricbook.dto.ReviewActionRequests;
import com.example.Cricbook.dto.ReviewRequest;
import com.example.Cricbook.dto.ReviewResponse;
import com.example.Cricbook.dto.ReviewSummaryResponse;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.UserRepository;
import com.example.Cricbook.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ReviewResponse> addReview(@RequestBody ReviewRequest request) {
        User user = currentUser();
        return ResponseEntity.ok(reviewService.addReview(request, user.getId()));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> editReview(@PathVariable Long reviewId, @RequestBody ReviewRequest request) {
        User user = currentUser();
        return ResponseEntity.ok(reviewService.editReview(reviewId, request, user.getId()));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        User user = currentUser();
        reviewService.deleteOwnReview(reviewId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/turf/{turfId}")
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable Long turfId) {
        return ResponseEntity.ok(reviewService.getReviews(turfId));
    }

    @GetMapping("/turf/{turfId}/summary")
    public ResponseEntity<ReviewSummaryResponse> getReviewSummary(@PathVariable Long turfId) {
        return ResponseEntity.ok(reviewService.getReviewSummary(turfId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReviewResponse>> getMyReviews() {
        User user = currentUser();
        return ResponseEntity.ok(reviewService.getMyReviews(user.getId()));
    }

    @GetMapping("/owner")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<List<ReviewResponse>> getOwnerReviews() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(reviewService.getOwnerReviews(email));
    }

    @PostMapping("/{reviewId}/owner-reply")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<ReviewResponse> ownerReply(@PathVariable Long reviewId, @RequestBody ReviewActionRequests.OwnerReplyRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(reviewService.saveOwnerReply(reviewId, request, email));
    }

    @PostMapping("/{reviewId}/vote")
    public ResponseEntity<ReviewResponse> voteReview(@PathVariable Long reviewId, @RequestBody ReviewActionRequests.VoteRequest request) {
        User user = currentUser();
        return ResponseEntity.ok(reviewService.voteReview(reviewId, user.getId(), request == null ? null : request.getHelpful()));
    }

    @PostMapping("/{reviewId}/report")
    public ResponseEntity<ReviewResponse> reportReview(@PathVariable Long reviewId, @RequestBody ReviewActionRequests.ReportRequest request) {
        User user = currentUser();
        return ResponseEntity.ok(reviewService.reportReview(reviewId, user.getId(), request == null ? null : request.getReason()));
    }

    @PostMapping("/{reviewId}/moderate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('MEDIA_MODERATE') or hasAuthority('REPORT_RESOLVE')")
    public ResponseEntity<ReviewResponse> moderateReview(@PathVariable Long reviewId, @RequestBody ReviewActionRequests.ModerationRequest request) {
        String moderator = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(reviewService.moderateReview(reviewId, request, moderator));
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailOrPhone(email)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
    }
}
