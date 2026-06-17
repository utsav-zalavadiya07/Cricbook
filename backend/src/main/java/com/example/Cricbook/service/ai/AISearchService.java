package com.example.Cricbook.service.ai;

import com.example.Cricbook.entity.Turf;
import com.example.Cricbook.repository.TurfRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AISearchService {

    private final TurfRepository turfRepository;

    public List<Turf> searchSmart(String query, String city) {
        log.info("Executing smart AI search for query: '{}', city: '{}'", query, city);
        
        List<Turf> turfs = turfRepository.findAll().stream()
                .filter(t -> t.getStatus() == com.example.Cricbook.enums.TurfStatus.ACTIVE)
                .collect(Collectors.toList());

        if (city != null && !city.trim().isEmpty()) {
            turfs = turfs.stream()
                    .filter(t -> t.getCity() != null && t.getCity().equalsIgnoreCase(city))
                    .collect(Collectors.toList());
        }

        if (turfs.isEmpty()) {
            return Collections.emptyList();
        }

        // Compute max price for ratio scaling
        BigDecimal maxPriceVal = turfs.stream()
                .map(Turf::getPricePerHour)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.valueOf(2000));
        
        double maxPrice = maxPriceVal.doubleValue() > 0 ? maxPriceVal.doubleValue() : 2000.0;

        // Intent parsing weights
        boolean preferCheap = false;
        boolean preferPremium = false;

        if (query != null) {
            String lowerQuery = query.toLowerCase();
            if (lowerQuery.contains("cheap") || lowerQuery.contains("affordable") || lowerQuery.contains("budget") || lowerQuery.contains("low cost")) {
                preferCheap = true;
            }
            if (lowerQuery.contains("premium") || lowerQuery.contains("best") || lowerQuery.contains("luxury") || lowerQuery.contains("top")) {
                preferPremium = true;
            }
        }

        final boolean finalPreferCheap = preferCheap;
        final boolean finalPreferPremium = preferPremium;

        List<Map.Entry<Turf, Double>> scoredTurfs = new ArrayList<>();

        for (Turf turf : turfs) {
            double rating = turf.getRating() != null ? turf.getRating() : 0.0;
            double ratingScore = rating / 5.0; // Scale to 0..1

            double price = turf.getPricePerHour() != null ? turf.getPricePerHour().doubleValue() : 1000.0;
            double priceRatio = price / maxPrice; // Scale to 0..1

            boolean isFeatured = turf.getFeaturedUntil() != null && turf.getFeaturedUntil().isAfter(LocalDateTime.now());
            double featuredScore = isFeatured ? 1.0 : 0.0;

            // Base weighted formula: Score = (Rating * 0.4) - (PriceRatio * 0.3) + (Featured * 0.3)
            double score = (ratingScore * 0.4) - (priceRatio * 0.3) + (featuredScore * 0.3);

            // Apply search intent corrections
            if (finalPreferCheap) {
                // Heavily penalize high prices
                score -= priceRatio * 0.5;
            }
            if (finalPreferPremium) {
                // Heavily reward high ratings
                score += ratingScore * 0.5;
            }

            // Keyword match boosts
            if (query != null && !query.trim().isEmpty()) {
                String q = query.toLowerCase();
                if (turf.getName() != null && turf.getName().toLowerCase().contains(q)) {
                    score += 1.0;
                }
                if (turf.getDescription() != null && turf.getDescription().toLowerCase().contains(q)) {
                    score += 0.5;
                }
                if (turf.getLocation() != null && turf.getLocation().toLowerCase().contains(q)) {
                    score += 0.5;
                }
            }

            scoredTurfs.add(new AbstractMap.SimpleEntry<>(turf, score));
        }

        // Sort by score descending
        scoredTurfs.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        return scoredTurfs.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
