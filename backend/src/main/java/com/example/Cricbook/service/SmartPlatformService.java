package com.example.Cricbook.service;

import com.example.Cricbook.dto.AdminUserResponse;
import com.example.Cricbook.dto.SmartRecommendationResponse;
import com.example.Cricbook.dto.TurfResponse;
import com.example.Cricbook.entity.Turf;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.enums.RoleName;
import com.example.Cricbook.mapper.TurfMapper;
import com.example.Cricbook.repository.BookingRepository;
import com.example.Cricbook.repository.TurfRepository;
import com.example.Cricbook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SmartPlatformService {

    private final TurfRepository turfRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final TurfMapper turfMapper;

    @Transactional(readOnly = true)
    public SmartRecommendationResponse getSmartRecommendations(String city, Long turfId, LocalDate date) {
        List<TurfResponse> bestTurfs = turfRepository.findAll().stream()
                .filter(turf -> city == null || city.isBlank() || turf.getCity().equalsIgnoreCase(city))
                .sorted(Comparator.comparing((Turf turf) -> turf.getRating() == null ? 0.0 : turf.getRating()).reversed()
                        .thenComparing(Turf::getPricePerHour))
                .limit(8)
                .map(turfMapper::toTurfResponse)
                .toList();

        List<AdminUserResponse> players = userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ROLE_USER))
                .limit(8)
                .map(this::mapUser)
                .toList();

        Turf pricingTurf = turfId == null ? null : turfRepository.findById(turfId).orElse(null);
        BigDecimal suggestedPrice = pricingTurf == null ? BigDecimal.ZERO : suggestPrice(pricingTurf, date == null ? LocalDate.now() : date);

        return SmartRecommendationResponse.builder()
                .bestTurfs(bestTurfs)
                .bestTimings(List.of("06:00", "18:00", "20:00"))
                .playerSuggestions(players)
                .suggestedPrice(suggestedPrice)
                .pricingReason(pricingTurf == null ? "Select a turf for pricing" : "Based on demand, weekend and recent confirmed bookings")
                .build();
    }

    private BigDecimal suggestPrice(Turf turf, LocalDate date) {
        long confirmedBookings = bookingRepository.findByTurfOwnerId(turf.getOwner().getId()).stream()
                .filter(booking -> booking.getTurf().getId().equals(turf.getId()))
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
                .count();
        BigDecimal multiplier = BigDecimal.ONE;
        if (date.getDayOfWeek().getValue() >= 6) {
            multiplier = multiplier.add(BigDecimal.valueOf(0.15));
        }
        if (confirmedBookings >= 10) {
            multiplier = multiplier.add(BigDecimal.valueOf(0.10));
        }
        return turf.getPricePerHour().multiply(multiplier).setScale(0, RoundingMode.HALF_UP);
    }

    private AdminUserResponse mapUser(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .roles(user.getRoles().stream().map(role -> role.getName().name()).toList())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
