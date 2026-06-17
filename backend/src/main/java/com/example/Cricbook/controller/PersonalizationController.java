package com.example.Cricbook.controller;

import com.example.Cricbook.dto.PersonalizationResponse;
import com.example.Cricbook.dto.RecentSearchRequest;
import com.example.Cricbook.dto.RecentSearchResponse;
import com.example.Cricbook.service.PersonalizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/personalization")
@RequiredArgsConstructor
public class PersonalizationController {

    private final PersonalizationService personalizationService;

    @GetMapping
    public ResponseEntity<PersonalizationResponse> getPersonalization(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(personalizationService.getPersonalization(userDetails.getUsername()));
    }

    @GetMapping("/recent-searches")
    public ResponseEntity<List<RecentSearchResponse>> recentSearches(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(personalizationService.getRecentSearches(userDetails.getUsername()));
    }

    @PostMapping("/recent-searches")
    public ResponseEntity<RecentSearchResponse> saveRecentSearch(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody RecentSearchRequest request) {
        return ResponseEntity.ok(personalizationService.saveRecentSearch(userDetails.getUsername(), request));
    }
}
