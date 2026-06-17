package com.example.Cricbook.controller;

import com.example.Cricbook.dto.UserProfileDto;
import com.example.Cricbook.dto.ChangePasswordRequest;
import com.example.Cricbook.dto.DeactivateAccountRequest;
import com.example.Cricbook.dto.UserLanguageRequest;
import com.example.Cricbook.dto.UserThemeRequest;
import com.example.Cricbook.dto.PlayerAvailabilityRequest;
import com.example.Cricbook.dto.PlayerAvailabilityResponse;
import com.example.Cricbook.dto.PlayerProfileResponse;
import com.example.Cricbook.dto.UpdatePlayerProfileRequest;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final com.example.Cricbook.service.BookingService bookingService;
    private final com.example.Cricbook.service.ReviewService reviewService;
    private final com.example.Cricbook.repository.UserRepository userRepository;

    public UserController(UserService userService, 
                          com.example.Cricbook.service.BookingService bookingService,
                          com.example.Cricbook.service.ReviewService reviewService,
                          com.example.Cricbook.repository.UserRepository userRepository) {
        this.userService = userService;
        this.bookingService = bookingService;
        this.reviewService = reviewService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getCurrentUser(requireUsername(userDetails)));
    }

    @GetMapping("/language")
    public ResponseEntity<java.util.Map<String, String>> getLanguage(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(java.util.Map.of("preferredLanguage", userService.getPreferredLanguage(requireUsername(userDetails))));
    }

    @org.springframework.web.bind.annotation.PutMapping("/language")
    public ResponseEntity<java.util.Map<String, String>> updateLanguage(@AuthenticationPrincipal UserDetails userDetails,
                                                                        @org.springframework.web.bind.annotation.RequestBody UserLanguageRequest request) {
        String preferredLanguage = userService.updatePreferredLanguage(requireUsername(userDetails), request.getPreferredLanguage());
        return ResponseEntity.ok(java.util.Map.of("preferredLanguage", preferredLanguage));
    }

    @GetMapping("/theme")
    public ResponseEntity<java.util.Map<String, String>> getTheme(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(java.util.Map.of("preferredTheme", userService.getPreferredTheme(requireUsername(userDetails))));
    }

    @org.springframework.web.bind.annotation.PutMapping("/theme")
    public ResponseEntity<java.util.Map<String, String>> updateTheme(@AuthenticationPrincipal UserDetails userDetails,
                                                                     @org.springframework.web.bind.annotation.RequestBody UserThemeRequest request) {
        String preferredTheme = userService.updatePreferredTheme(requireUsername(userDetails), request.getPreferredTheme());
        return ResponseEntity.ok(java.util.Map.of("preferredTheme", preferredTheme));
    }

    @org.springframework.web.bind.annotation.PutMapping("/update")
    public ResponseEntity<String> updateProfile(@org.springframework.web.bind.annotation.RequestBody com.example.Cricbook.dto.UpdateUserRequest request,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        userService.updateProfile(requireUsername(userDetails), request);
        return ResponseEntity.ok("Profile updated");
    }

    @org.springframework.web.bind.annotation.PostMapping("/change-password")
    public ResponseEntity<java.util.Map<String, String>> changePassword(@org.springframework.web.bind.annotation.RequestBody ChangePasswordRequest request,
                                                                        @AuthenticationPrincipal UserDetails userDetails) {
        userService.changePassword(requireUsername(userDetails), request);
        return ResponseEntity.ok(java.util.Map.of("message", "Password changed successfully. Please sign in again on other devices."));
    }

    @org.springframework.web.bind.annotation.PostMapping("/deactivate")
    public ResponseEntity<java.util.Map<String, String>> deactivateAccount(@org.springframework.web.bind.annotation.RequestBody DeactivateAccountRequest request,
                                                                          @AuthenticationPrincipal UserDetails userDetails) {
        userService.deactivateAccount(requireUsername(userDetails), request);
        return ResponseEntity.ok(java.util.Map.of("message", "Account deactivated successfully."));
    }

    @GetMapping("/bookings")
    public ResponseEntity<java.util.List<com.example.Cricbook.dto.BookingResponse>> getBookings() {
        return ResponseEntity.ok(bookingService.getMyBookings());
    }

    @GetMapping("/reviews")
    public ResponseEntity<java.util.List<com.example.Cricbook.dto.ReviewResponse>> getReviews(@AuthenticationPrincipal UserDetails userDetails) {
        com.example.Cricbook.entity.User user = userRepository.findByEmailOrPhone(requireUsername(userDetails))
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
        return ResponseEntity.ok(reviewService.getMyReviews(user.getId()));
    }

    @GetMapping("/player-profile")
    public ResponseEntity<PlayerProfileResponse> getMyPlayerProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getMyPlayerProfile(requireUsername(userDetails)));
    }

    @GetMapping("/player/{userId}")
    public ResponseEntity<PlayerProfileResponse> getPlayerProfile(@org.springframework.web.bind.annotation.PathVariable Long userId) {
        return ResponseEntity.ok(userService.getPlayerProfile(userId));
    }

    @org.springframework.web.bind.annotation.PutMapping("/player-profile")
    public ResponseEntity<PlayerProfileResponse> updatePlayerProfile(@AuthenticationPrincipal UserDetails userDetails,
                                                                     @org.springframework.web.bind.annotation.RequestBody UpdatePlayerProfileRequest request) {
        return ResponseEntity.ok(userService.updatePlayerProfile(requireUsername(userDetails), request));
    }

    @GetMapping("/player-profile/availability")
    public ResponseEntity<PlayerAvailabilityResponse> getMyAvailability(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getMyAvailability(requireUsername(userDetails)));
    }

    @org.springframework.web.bind.annotation.PutMapping("/player-profile/availability")
    public ResponseEntity<PlayerAvailabilityResponse> updateAvailability(@AuthenticationPrincipal UserDetails userDetails,
                                                                        @org.springframework.web.bind.annotation.RequestBody PlayerAvailabilityRequest request) {
        return ResponseEntity.ok(userService.updateAvailability(requireUsername(userDetails), request));
    }

    private String requireUsername(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw ApiException.unauthorized("Please sign in again to continue.");
        }
        return userDetails.getUsername();
    }
}
