package com.example.Cricbook.controller;

import com.example.Cricbook.entity.Recommendation;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.UserRepository;
import com.example.Cricbook.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<Recommendation>> getMyRecommendations(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmailOrPhone(userDetails.getUsername())
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
        
        List<Recommendation> recommendations = recommendationService.getRecommendations(user.getId());
        return ResponseEntity.ok(recommendations);
    }
}
