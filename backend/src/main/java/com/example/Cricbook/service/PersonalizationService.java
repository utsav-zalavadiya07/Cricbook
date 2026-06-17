package com.example.Cricbook.service;

import com.example.Cricbook.dto.PersonalizationResponse;
import com.example.Cricbook.dto.RecentSearchRequest;
import com.example.Cricbook.dto.RecentSearchResponse;
import com.example.Cricbook.dto.TurfResponse;
import com.example.Cricbook.entity.RecentSearch;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.mapper.TurfMapper;
import com.example.Cricbook.repository.BookingRepository;
import com.example.Cricbook.repository.FavoriteTurfRepository;
import com.example.Cricbook.repository.RecentSearchRepository;
import com.example.Cricbook.repository.TurfRepository;
import com.example.Cricbook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonalizationService {

    private final UserRepository userRepository;
    private final RecentSearchRepository recentSearchRepository;
    private final FavoriteTurfRepository favoriteTurfRepository;
    private final BookingRepository bookingRepository;
    private final TurfRepository turfRepository;
    private final TurfMapper turfMapper;

    @Transactional
    public RecentSearchResponse saveRecentSearch(String username, RecentSearchRequest request) {
        User user = findUser(username);
        RecentSearch saved = recentSearchRepository.save(RecentSearch.builder()
                .user(user)
                .city(request.getCity())
                .date(request.getDate())
                .boxType(request.getBoxType())
                .pitchType(request.getPitchType())
                .build());
        return map(saved);
    }

    @Transactional(readOnly = true)
    public PersonalizationResponse getPersonalization(String username) {
        User user = findUser(username);
        List<RecentSearchResponse> recentSearches = recentSearchRepository.findTop10ByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::map)
                .toList();

        List<String> preferredLocations = recentSearches.stream()
                .map(RecentSearchResponse::getCity)
                .filter(city -> city != null && !city.isBlank())
                .distinct()
                .limit(5)
                .toList();

        List<TurfResponse> favorites = favoriteTurfRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(favorite -> turfMapper.toTurfResponse(favorite.getTurf()))
                .toList();

        List<TurfResponse> recommendedTurfs = favorites.isEmpty()
                ? turfRepository.findAll().stream()
                        .sorted(Comparator.comparing(turf -> turf.getRating() == null ? 0.0 : turf.getRating(), Comparator.reverseOrder()))
                        .limit(8)
                        .map(turfMapper::toTurfResponse)
                        .toList()
                : favorites;

        return PersonalizationResponse.builder()
                .recommendedTurfs(recommendedTurfs)
                .recentSearches(recentSearches)
                .recentlyBooked(bookingRepository.findByUserId(user.getId()).stream()
                        .limit(6)
                        .map(booking -> com.example.Cricbook.dto.BookingResponse.builder()
                                .id(booking.getId())
                                .turfId(booking.getTurf().getId())
                                .turfName(booking.getTurf().getName())
                                .bookingDate(booking.getBookingDate())
                                .status(booking.getStatus())
                                .amount(booking.getAmount())
                                .slotId(booking.getSlot().getId())
                                .startTime(booking.getSlot().getStartTime())
                                .endTime(booking.getSlot().getEndTime())
                                .build())
                        .toList())
                .preferredLocations(preferredLocations)
                .recommendedTimings(List.of(LocalTime.of(6, 0).toString(), LocalTime.of(18, 0).toString(), LocalTime.of(20, 0).toString()))
                .build();
    }

    @Transactional(readOnly = true)
    public List<RecentSearchResponse> getRecentSearches(String username) {
        User user = findUser(username);
        return recentSearchRepository.findTop10ByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::map)
                .collect(Collectors.toList());
    }

    private RecentSearchResponse map(RecentSearch recentSearch) {
        return RecentSearchResponse.builder()
                .id(recentSearch.getId())
                .city(recentSearch.getCity())
                .date(recentSearch.getDate())
                .boxType(recentSearch.getBoxType())
                .pitchType(recentSearch.getPitchType())
                .createdAt(recentSearch.getCreatedAt())
                .build();
    }

    private User findUser(String username) {
        return userRepository.findByEmailOrPhone(username)
                .orElseThrow(() -> new ApiException("User not found"));
    }
}
