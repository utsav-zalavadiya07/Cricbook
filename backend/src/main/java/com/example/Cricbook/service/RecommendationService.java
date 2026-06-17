package com.example.Cricbook.service;

import com.example.Cricbook.entity.Recommendation;
import com.example.Cricbook.entity.Booking;
import com.example.Cricbook.entity.Turf;
import com.example.Cricbook.entity.Tournament;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.enums.TurfStatus;
import com.example.Cricbook.repository.RecommendationRepository;
import com.example.Cricbook.repository.BookingRepository;
import com.example.Cricbook.repository.TurfRepository;
import com.example.Cricbook.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final BookingRepository bookingRepository;
    private final TurfRepository turfRepository;
    private final TournamentRepository tournamentRepository;

    @Transactional
    public List<Recommendation> generateAndSaveRecommendations(Long userId) {
        log.info("Generating personalized recommendations for user ID: {}", userId);
        recommendationRepository.deleteByUserId(userId);

        List<Recommendation> generated = new ArrayList<>();

        // 1. "BOOK_AGAIN" Suggestion
        List<Booking> userBookings = bookingRepository.findByUserId(userId);
        if (!userBookings.isEmpty()) {
            Map<Turf, Long> turfBookingCounts = userBookings.stream()
                    .filter(b -> b.getTurf() != null && b.getTurf().getStatus() == TurfStatus.ACTIVE)
                    .collect(Collectors.groupingBy(Booking::getTurf, Collectors.counting()));
            
            Turf favoriteTurf = turfBookingCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            if (favoriteTurf != null) {
                generated.add(recommendationRepository.save(Recommendation.builder()
                        .userId(userId)
                        .type("BOOK_AGAIN")
                        .score(0.95)
                        .itemId(favoriteTurf.getId())
                        .message("Book " + favoriteTurf.getName() + " again! Your favorite spot has open slots.")
                        .build()));
            }
        }

        // 2. "RECOMMENDED_TURF" Suggestion (Top-rated active turfs)
        Set<Long> alreadyRecommendedTurfs = generated.stream()
                .filter(recommendation -> "BOOK_AGAIN".equals(recommendation.getType()))
                .map(Recommendation::getItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Turf> turfs = turfRepository.findAll().stream()
                .filter(turf -> turf.getStatus() == TurfStatus.ACTIVE)
                .filter(turf -> !alreadyRecommendedTurfs.contains(turf.getId()))
                .collect(Collectors.toList());
        if (!turfs.isEmpty()) {
            Turf topRated = turfs.stream()
                    .filter(t -> t.getRating() != null)
                    .max(Comparator.comparing(Turf::getRating))
                    .orElse(turfs.get(0));

            generated.add(recommendationRepository.save(Recommendation.builder()
                    .userId(userId)
                    .type("RECOMMENDED_TURF")
                    .score(0.85)
                    .itemId(topRated.getId())
                    .message("Spotlight Turf: Checkout top-rated " + topRated.getName() + " with rating " + topRated.getRating())
                    .build()));
        }

        // 3. "UPCOMING_TOURNAMENT" Suggestion
        List<Tournament> tournaments = tournamentRepository.findAll();
        if (!tournaments.isEmpty()) {
            Tournament nextTournament = tournaments.stream()
                    .filter(t -> t.getStartDate() != null)
                    .min(Comparator.comparing(Tournament::getStartDate))
                    .orElse(tournaments.get(0));

            generated.add(recommendationRepository.save(Recommendation.builder()
                    .userId(userId)
                    .type("UPCOMING_TOURNAMENT")
                    .score(0.75)
                    .itemId(nextTournament.getId())
                    .message("Join the tournament: " + nextTournament.getName() + " starting soon!")
                    .build()));
        }

        return generated;
    }

    @Transactional(readOnly = true)
    public List<Recommendation> getRecommendations(Long userId) {
        List<Recommendation> current = recommendationRepository.findByUserIdOrderByScoreDesc(userId);
        if (current.isEmpty()) {
            // Dynamic self-heal fallback generating on the fly
            return generateAndSaveRecommendations(userId);
        }
        return current;
    }
}
