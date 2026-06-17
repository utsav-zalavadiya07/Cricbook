package com.example.Cricbook.controller.ai;

import com.example.Cricbook.dto.TurfResponse;
import com.example.Cricbook.entity.Turf;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.UserRepository;
import com.example.Cricbook.service.ai.AIRecommendationService;
import com.example.Cricbook.service.ai.AISearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIRecommendationController {

    private final AIRecommendationService recommendationService;
    private final AISearchService searchService;
    private final UserRepository userRepository;

    @GetMapping("/recommendations")
    public ResponseEntity<?> getPersonalizedRecommendations(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "category", required = false) String category) {
        
        User user = userRepository.findByEmailOrPhone(userDetails.getUsername())
                .orElseThrow(() -> ApiException.notFound("User account was not found."));

        if (category != null && !category.trim().isEmpty()) {
            List<TurfResponse> turfs = recommendationService.getRecommendations(user.getId(), category.toUpperCase()).stream()
                    .map(this::mapTurf)
                    .toList();
            return ResponseEntity.ok(turfs);
        }

        Map<String, List<TurfResponse>> feeds = new HashMap<>();
        feeds.put("FOR_YOU", recommendationService.getRecommendations(user.getId(), "FOR_YOU").stream().map(this::mapTurf).toList());
        feeds.put("NEARBY", recommendationService.getRecommendations(user.getId(), "NEARBY").stream().map(this::mapTurf).toList());
        feeds.put("WEEKEND", recommendationService.getRecommendations(user.getId(), "WEEKEND").stream().map(this::mapTurf).toList());
        return ResponseEntity.ok(feeds);
    }

    @GetMapping("/search/smart")
    public ResponseEntity<List<TurfResponse>> smartSearch(
            @RequestParam("query") String query,
            @RequestParam(value = "city", required = false) String city) {
        List<TurfResponse> results = searchService.searchSmart(query, city).stream()
                .map(this::mapTurf)
                .toList();
        return ResponseEntity.ok(results);
    }

    private TurfResponse mapTurf(Turf turf) {
        return TurfResponse.builder()
                .id(turf.getId())
                .name(turf.getName())
                .description(turf.getDescription())
                .location(turf.getLocation())
                .city(turf.getCity())
                .latitude(turf.getLatitude())
                .longitude(turf.getLongitude())
                .pricePerHour(turf.getPricePerHour())
                .pitchType(turf.getPitchType())
                .boxType(turf.getBoxType())
                .status(turf.getStatus())
                .rating(turf.getRating())
                .totalReviews(turf.getTotalReviews())
                .amenities(Collections.emptyList())
                .images(Collections.emptyList())
                .build();
    }
}
