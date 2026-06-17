package com.example.Cricbook.controller;

import com.example.Cricbook.dto.TurfResponse;
import com.example.Cricbook.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{turfId}")
    public ResponseEntity<TurfResponse> saveFavorite(
            @PathVariable Long turfId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(favoriteService.saveFavorite(userDetails.getUsername(), turfId));
    }

    @GetMapping
    public ResponseEntity<List<TurfResponse>> getFavorites(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(favoriteService.getFavorites(userDetails.getUsername()));
    }

    @GetMapping("/{turfId}")
    public ResponseEntity<Map<String, Boolean>> isFavorite(
            @PathVariable Long turfId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(Map.of("favorite", favoriteService.isFavorite(userDetails.getUsername(), turfId)));
    }

    @DeleteMapping("/{turfId}")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable Long turfId,
            @AuthenticationPrincipal UserDetails userDetails) {
        favoriteService.removeFavorite(userDetails.getUsername(), turfId);
        return ResponseEntity.noContent().build();
    }
}
