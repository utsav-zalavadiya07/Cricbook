package com.example.Cricbook.controller;

import com.example.Cricbook.dto.SmartRecommendationResponse;
import com.example.Cricbook.service.SmartPlatformService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/smart")
@RequiredArgsConstructor
public class SmartPlatformController {

    private final SmartPlatformService smartPlatformService;

    @GetMapping("/recommendations")
    public ResponseEntity<SmartRecommendationResponse> getSmartRecommendations(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Long turfId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(smartPlatformService.getSmartRecommendations(city, turfId, date));
    }
}
