package com.example.Cricbook.service.ai;

import com.example.Cricbook.entity.RecommendationCache;
import com.example.Cricbook.entity.UserVector;
import com.example.Cricbook.entity.Turf;
import com.example.Cricbook.entity.Booking;
import com.example.Cricbook.repository.RecommendationCacheRepository;
import com.example.Cricbook.repository.UserVectorRepository;
import com.example.Cricbook.repository.TurfRepository;
import com.example.Cricbook.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIRecommendationService {

    private final RecommendationCacheRepository cacheRepository;
    private final UserVectorRepository userVectorRepository;
    private final TurfRepository turfRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public UserVector getOrCreateUserVector(Long userId) {
        return userVectorRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Computing new UserVector for user ID: {}", userId);
                    List<Booking> bookings = bookingRepository.findByUserId(userId);
                    
                    String preferredCity = "Surat";
                    int preferredHour = 18;
                    BigDecimal minPrice = BigDecimal.valueOf(500);
                    BigDecimal maxPrice = BigDecimal.valueOf(1500);

                    if (!bookings.isEmpty()) {
                        // Compute preferred city
                        Map<String, Long> cityCounts = bookings.stream()
                                .filter(b -> b.getTurf() != null)
                                .map(b -> b.getTurf().getCity())
                                .filter(Objects::nonNull)
                                .collect(Collectors.groupingBy(city -> city, Collectors.counting()));
                        if (!cityCounts.isEmpty()) {
                            preferredCity = cityCounts.entrySet().stream()
                                    .max(Map.Entry.comparingByValue())
                                    .map(Map.Entry::getKey)
                                    .orElse("Surat");
                        }

                        // Compute preferred hour
                        Map<Integer, Long> hourCounts = bookings.stream()
                                .filter(b -> b.getSlot() != null && b.getSlot().getStartTime() != null)
                                .map(b -> b.getSlot().getStartTime().getHour())
                                .collect(Collectors.groupingBy(h -> h, Collectors.counting()));
                        if (!hourCounts.isEmpty()) {
                            preferredHour = hourCounts.entrySet().stream()
                                    .max(Map.Entry.comparingByValue())
                                    .map(Map.Entry::getKey)
                                    .orElse(18);
                        }

                        // Compute price range
                        List<BigDecimal> prices = bookings.stream()
                                .map(Booking::getAmount)
                                .filter(Objects::nonNull)
                                .sorted()
                                .collect(Collectors.toList());
                        if (!prices.isEmpty()) {
                            minPrice = prices.get(0);
                            maxPrice = prices.get(prices.size() - 1);
                        }
                    }

                    UserVector vector = UserVector.builder()
                            .userId(userId)
                            .preferredCity(preferredCity)
                            .preferredHour(preferredHour)
                            .preferredPriceMin(minPrice)
                            .preferredPriceMax(maxPrice)
                            .build();

                    return userVectorRepository.save(vector);
                });
    }

    @Transactional
    public void generateAllRecommendations(Long userId) {
        log.info("Generating personalized intelligence recommendations cache for user ID: {}", userId);
        cacheRepository.deleteByUserId(userId);

        UserVector vector = getOrCreateUserVector(userId);
        List<Turf> allActiveTurfs = turfRepository.findAll().stream()
                .filter(t -> t.getStatus() == com.example.Cricbook.enums.TurfStatus.ACTIVE)
                .collect(Collectors.toList());

        if (allActiveTurfs.isEmpty()) {
            return;
        }

        // 1. FOR_YOU: vector time/price match
        List<Map.Entry<Turf, Double>> scoredForYou = new ArrayList<>();
        for (Turf turf : allActiveTurfs) {
            double score = 0.0;
            if (turf.getCity() != null && turf.getCity().equalsIgnoreCase(vector.getPreferredCity())) {
                score += 0.5;
            }
            if (turf.getPricePerHour() != null) {
                if (turf.getPricePerHour().compareTo(vector.getPreferredPriceMin()) >= 0 &&
                    turf.getPricePerHour().compareTo(vector.getPreferredPriceMax()) <= 0) {
                    score += 0.3;
                } else {
                    score += 0.1;
                }
            }
            if (turf.getRating() != null) {
                score += (turf.getRating() / 5.0) * 0.2;
            }
            scoredForYou.add(new AbstractMap.SimpleEntry<>(turf, score));
        }
        scoredForYou.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        String forYouIds = scoredForYou.stream()
                .limit(5)
                .map(e -> e.getKey().getId().toString())
                .collect(Collectors.joining(","));

        cacheRepository.save(RecommendationCache.builder()
                .userId(userId)
                .category("FOR_YOU")
                .recommendedTurfIds(forYouIds)
                .build());

        // 2. NEARBY: Trending / Featured / High rated in preferred city
        List<Turf> nearbyTurfs = allActiveTurfs.stream()
                .filter(t -> t.getCity() != null && t.getCity().equalsIgnoreCase(vector.getPreferredCity()))
                .sorted((a, b) -> {
                    boolean aFeatured = a.getFeaturedUntil() != null && a.getFeaturedUntil().isAfter(LocalDateTime.now());
                    boolean bFeatured = b.getFeaturedUntil() != null && b.getFeaturedUntil().isAfter(LocalDateTime.now());
                    if (aFeatured != bFeatured) {
                        return aFeatured ? -1 : 1;
                    }
                    double aRating = a.getRating() != null ? a.getRating() : 0.0;
                    double bRating = b.getRating() != null ? b.getRating() : 0.0;
                    return Double.compare(bRating, aRating);
                })
                .limit(5)
                .collect(Collectors.toList());
        if (nearbyTurfs.isEmpty()) {
            nearbyTurfs = allActiveTurfs.stream().limit(5).collect(Collectors.toList());
        }
        String nearbyIds = nearbyTurfs.stream()
                .map(t -> t.getId().toString())
                .collect(Collectors.joining(","));

        cacheRepository.save(RecommendationCache.builder()
                .userId(userId)
                .category("NEARBY")
                .recommendedTurfIds(nearbyIds)
                .build());

        // 3. WEEKEND: High rating
        List<Turf> weekendSuggestions = allActiveTurfs.stream()
                .sorted((a, b) -> {
                    double aRating = a.getRating() != null ? a.getRating() : 0.0;
                    double bRating = b.getRating() != null ? b.getRating() : 0.0;
                    return Double.compare(bRating, aRating);
                })
                .limit(5)
                .collect(Collectors.toList());
        String weekendIds = weekendSuggestions.stream()
                .map(t -> t.getId().toString())
                .collect(Collectors.joining(","));

        cacheRepository.save(RecommendationCache.builder()
                .userId(userId)
                .category("WEEKEND")
                .recommendedTurfIds(weekendIds)
                .build());
    }

    @Transactional
    public List<Turf> getRecommendations(Long userId, String category) {
        Optional<RecommendationCache> cacheOpt = cacheRepository.findByUserIdAndCategory(userId, category);
        if (cacheOpt.isEmpty()) {
            generateAllRecommendations(userId);
            cacheOpt = cacheRepository.findByUserIdAndCategory(userId, category);
        }

        if (cacheOpt.isEmpty() || cacheOpt.get().getRecommendedTurfIds().isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> turfIds = Arrays.stream(cacheOpt.get().getRecommendedTurfIds().split(","))
                .filter(s -> !s.trim().isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());

        Map<Long, Turf> turfMap = turfRepository.findAllById(turfIds).stream()
                .collect(Collectors.toMap(Turf::getId, t -> t));

        List<Turf> orderedTurfs = new ArrayList<>();
        for (Long id : turfIds) {
            if (turfMap.containsKey(id)) {
                orderedTurfs.add(turfMap.get(id));
            }
        }
        return orderedTurfs;
    }
}
