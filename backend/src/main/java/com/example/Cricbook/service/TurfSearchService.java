package com.example.Cricbook.service;

import com.example.Cricbook.dto.TurfSearchRequest;
import com.example.Cricbook.dto.TurfSearchResponse;
import com.example.Cricbook.entity.Turf;
import com.example.Cricbook.entity.TurfAmenity;
import com.example.Cricbook.entity.TurfImage;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.TurfRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TurfSearchService {

    private final TurfRepository turfRepository;
    private final CacheService cacheService;
    private final MultiSportService multiSportService;

    @Transactional(readOnly = true)
    public List<TurfSearchResponse> search(TurfSearchRequest request) {
        TurfSearchRequest normalized = normalizeAndValidate(request);
        log.debug("Searching turfs with request: {}", normalized);
        return cacheService.getOrCompute(searchKey(normalized), 60, () -> executeSearch(normalized));
    }

    @Transactional(readOnly = true)
    public List<String> suggestions(String query, Integer limit) {
        String normalizedQuery = cleanText(query);
        if (normalizedQuery == null || normalizedQuery.length() < 2) {
            return List.of();
        }

        int safeLimit = limit == null ? 8 : Math.max(1, Math.min(limit, 20));
        return cacheService.getOrCompute(suggestionKey(normalizedQuery, safeLimit), 30, () ->
                turfRepository.findSearchSuggestions(normalizedQuery, PageRequest.of(0, safeLimit)).stream()
                        .flatMap(turf -> Stream.of(turf.getName(), turf.getCity(), turf.getLocation()))
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .distinct()
                        .limit(safeLimit)
                        .collect(Collectors.toList())
        );
    }

    private List<TurfSearchResponse> executeSearch(TurfSearchRequest request) {
        Sort sort = Sort.unsorted();
        if ("PRICE_ASC".equalsIgnoreCase(request.getSortBy())) {
            sort = Sort.by(Sort.Direction.ASC, "pricePerHour");
        } else if ("PRICE_DESC".equalsIgnoreCase(request.getSortBy())) {
            sort = Sort.by(Sort.Direction.DESC, "pricePerHour");
        } else if ("RATING".equalsIgnoreCase(request.getSortBy())) {
            sort = Sort.by(Sort.Direction.DESC, "rating");
        } else if ("MOST_BOOKED".equalsIgnoreCase(request.getSortBy())) {
            sort = Sort.by(Sort.Direction.DESC, "totalReviews");
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        boolean amenitiesEmpty = request.getAmenities() == null || request.getAmenities().isEmpty();
        List<String> amenities = amenitiesEmpty ? List.of("__none__") : request.getAmenities();

        Page<Turf> turfPage = request.getDate() == null
                ? turfRepository.searchActiveTurfs(
                        request.getQuery(),
                        request.getCity(),
                        request.getMinPrice(),
                        request.getMaxPrice(),
                        request.getRating(),
                        request.getBoxType(),
                        request.getPitchType(),
                        amenities,
                        amenitiesEmpty,
                        pageable
                )
                : turfRepository.searchTurfs(
                        request.getQuery(),
                        request.getCity(),
                        request.getDate(),
                        request.getMinPrice(),
                        request.getMaxPrice(),
                        request.getRating(),
                        request.getBoxType(),
                        request.getPitchType(),
                        amenities,
                        amenitiesEmpty,
                        pageable
                );

        List<Turf> matchedTurfs = turfPage.stream()
                .filter(turf -> hasAllRequestedAmenities(turf, request.getAmenities()))
                .filter(turf -> isWithinRadius(turf, request))
                .toList();

        List<Long> turfIds = matchedTurfs.stream()
                .map(Turf::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        String requestedSport = primarySport(request);
        Map<Long, Boolean> sportSupport = requestedSport == null
                ? Map.of()
                : multiSportService.turfSupportByTurfId(turfIds, requestedSport);
        Map<Long, List<String>> activeSportsByTurfId = multiSportService.activeSportCodesByTurfId(turfIds);

        List<TurfSearchResponse> responses = matchedTurfs.stream()
                .filter(turf -> requestedSport == null || sportSupport.getOrDefault(turf.getId(), false))
                .map(turf -> mapToResponse(turf, request, activeSportsByTurfId.getOrDefault(turf.getId(), List.of())))
                .collect(Collectors.toList());
                
        if ("DISTANCE".equalsIgnoreCase(request.getSortBy())) {
             responses.sort(Comparator.comparing(r -> r.getDistance() == null ? Double.MAX_VALUE : r.getDistance()));
        }
        
        return responses;
    }

    private String searchKey(TurfSearchRequest request) {
        return String.join(":",
                "search",
                String.valueOf(request.getQuery()),
                String.valueOf(request.getCity()),
                String.valueOf(request.getDate()),
                String.valueOf(request.getMinPrice()),
                String.valueOf(request.getMaxPrice()),
                String.valueOf(request.getRating()),
                String.valueOf(request.getBoxType()),
                String.valueOf(request.getPitchType()),
                String.valueOf(request.getSport()),
                String.valueOf(request.getSportCode()),
                String.valueOf(request.getAmenities()),
                String.valueOf(request.getLatitude()),
                String.valueOf(request.getLongitude()),
                String.valueOf(request.getRadius()),
                String.valueOf(request.getSortBy()),
                String.valueOf(request.getPage()),
                String.valueOf(request.getSize()));
    }

    private String suggestionKey(String query, int limit) {
        return String.join(":",
                "searchSuggestions",
                query.toLowerCase(Locale.ROOT),
                String.valueOf(limit));
    }

    private TurfSearchRequest normalizeAndValidate(TurfSearchRequest request) {
        TurfSearchRequest source = request == null ? new TurfSearchRequest() : request;
        String sortBy = normalizeSort(source.getSortBy());
        BigDecimal minPrice = source.getMinPrice();
        BigDecimal maxPrice = source.getMaxPrice();

        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw ApiException.badRequest("Minimum price cannot be negative.");
        }
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw ApiException.badRequest("Maximum price cannot be negative.");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw ApiException.badRequest("Minimum price cannot be greater than maximum price.");
        }
        if (source.getRating() != null && (source.getRating() < 0 || source.getRating() > 5)) {
            throw ApiException.badRequest("Rating filter must be between 0 and 5.");
        }
        if (source.getLatitude() != null && (source.getLatitude() < -90 || source.getLatitude() > 90)) {
            throw ApiException.badRequest("Latitude must be between -90 and 90.");
        }
        if (source.getLongitude() != null && (source.getLongitude() < -180 || source.getLongitude() > 180)) {
            throw ApiException.badRequest("Longitude must be between -180 and 180.");
        }
        if ((source.getLatitude() == null) != (source.getLongitude() == null)) {
            throw ApiException.badRequest("Latitude and longitude must be provided together.");
        }
        if (source.getRadius() != null && source.getRadius() <= 0) {
            throw ApiException.badRequest("Search radius must be greater than zero.");
        }
        if (source.getRadius() != null && source.getRadius() > 100) {
            throw ApiException.badRequest("Search radius cannot exceed 100 km.");
        }

        return TurfSearchRequest.builder()
                .query(cleanText(source.getQuery()))
                .city(cleanText(source.getCity()))
                .latitude(source.getLatitude())
                .longitude(source.getLongitude())
                .radius(source.getRadius())
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .boxType(cleanText(source.getBoxType()))
                .pitchType(cleanText(source.getPitchType()))
                .sport(cleanText(source.getSport()))
                .sportCode(cleanText(source.getSportCode()))
                .amenities(normalizeAmenities(source.getAmenities()))
                .date(source.getDate())
                .startTime(source.getStartTime())
                .sortBy(sortBy)
                .rating(source.getRating())
                .page(Math.max(0, source.getPage()))
                .size(Math.max(1, Math.min(source.getSize() <= 0 ? 20 : source.getSize(), 100)))
                .build();
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll("[<>]", "");
        return cleaned.isBlank() ? null : cleaned;
    }

    private List<String> normalizeAmenities(List<String> amenities) {
        if (amenities == null || amenities.isEmpty()) {
            return null;
        }
        return amenities.stream()
                .map(this::cleanText)
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        values -> values.isEmpty() ? null : List.copyOf(values)
                ));
    }

    private String normalizeSort(String sortBy) {
        String normalized = cleanText(sortBy);
        if (normalized == null) {
            return null;
        }

        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case "PRICE", "PRICE_ASC", "PRICE_LOW_TO_HIGH" -> "PRICE_ASC";
            case "PRICE_DESC", "PRICE_HIGH_TO_LOW" -> "PRICE_DESC";
            case "RATING", "HIGHEST_RATED", "POPULAR", "RECOMMENDED" -> "RATING";
            case "DISTANCE", "NEAREST", "NEAREST_FIRST" -> "DISTANCE";
            case "MOST_BOOKED" -> "MOST_BOOKED";
            default -> throw ApiException.badRequest("Sort option is not supported.");
        };
    }

    private boolean hasAllRequestedAmenities(Turf turf, List<String> requestedAmenities) {
        if (requestedAmenities == null || requestedAmenities.isEmpty()) {
            return true;
        }
        if (turf.getAmenities() == null || turf.getAmenities().isEmpty()) {
            return false;
        }

        Set<String> availableAmenities = turf.getAmenities().stream()
                .map(TurfAmenity::getAmenity)
                .filter(Objects::nonNull)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        return requestedAmenities.stream()
                .filter(Objects::nonNull)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .allMatch(availableAmenities::contains);
    }

    private boolean isWithinRadius(Turf turf, TurfSearchRequest request) {
        if (request.getLatitude() == null || request.getLongitude() == null || request.getRadius() == null) {
            return true; // No location filter
        }
        if (turf.getLatitude() == null || turf.getLongitude() == null) {
            return false; // Turf has no location
        }
        
        double distance = calculateDistance(request.getLatitude(), request.getLongitude(), turf.getLatitude(), turf.getLongitude());
        return distance <= request.getRadius();
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Haversine formula
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private TurfSearchResponse mapToResponse(Turf turf, TurfSearchRequest request, List<String> availableSports) {
        Double distance = null;
        if (request.getLatitude() != null && request.getLongitude() != null && turf.getLatitude() != null && turf.getLongitude() != null) {
            distance = calculateDistance(request.getLatitude(), request.getLongitude(), turf.getLatitude(), turf.getLongitude());
        }

        return TurfSearchResponse.builder()
                .id(turf.getId())
                .name(turf.getName())
                .city(turf.getCity())
                .location(turf.getLocation())
                .pricePerHour(turf.getPricePerHour())
                .rating(turf.getRating())
                .totalReviews(turf.getTotalReviews())
                .boxType(turf.getBoxType() != null ? turf.getBoxType().name() : null)
                .pitchType(turf.getPitchType() != null ? turf.getPitchType().name() : null)
                .sport(primarySport(request))
                .sports(availableSports)
                .status(turf.getStatus() != null ? turf.getStatus().name() : null)
                .distance(distance)
                .amenities(turf.getAmenities() == null ? List.of() : turf.getAmenities().stream().map(TurfAmenity::getAmenity).collect(Collectors.toList()))
                .images(turf.getImages() == null ? List.of() : turf.getImages().stream().map(TurfImage::getImagePath).collect(Collectors.toList()))
                .available(true) // If it's returned by the query, it has an available slot
                .build();
    }

    private String primarySport(TurfSearchRequest request) {
        return request.getSportCode() != null ? request.getSportCode() : request.getSport();
    }

}
