package com.example.Cricbook.service;

import com.example.Cricbook.dto.CreateTurfRequest;
import com.example.Cricbook.dto.TurfResponse;
import com.example.Cricbook.entity.Turf;
import com.example.Cricbook.entity.TurfAmenity;
import com.example.Cricbook.entity.TurfImage;
import com.example.Cricbook.entity.Sport;
import com.example.Cricbook.entity.SportTurf;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.RoleName;
import com.example.Cricbook.enums.TurfStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.mapper.TurfMapper;
import com.example.Cricbook.repository.ReviewRepository;
import com.example.Cricbook.repository.TurfRepository;
import com.example.Cricbook.repository.TurfAmenityRepository;
import com.example.Cricbook.repository.SportTurfRepository;
import com.example.Cricbook.repository.UserRepository;
import com.example.Cricbook.repository.SlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TurfService {
    private static final int MAX_TURF_IMAGES = 20;
    private static final int MAX_AMENITIES = 20;
    private static final BigDecimal MAX_PRICE_PER_HOUR = BigDecimal.valueOf(100000);

    private final TurfRepository turfRepository;
    private final UserRepository userRepository;
    private final TurfMapper turfMapper;
    private final FileStorageService fileStorageService;
    private final MultiSportService multiSportService;
    private final SportTurfRepository sportTurfRepository;
    private final TurfAmenityRepository turfAmenityRepository;
    private final ReviewRepository reviewRepository;
    private final SlotRepository slotRepository;
    private final CacheService cacheService;
    @org.springframework.context.annotation.Lazy
    private final MonetizationGateService monetizationGateService;

    @Transactional
    public TurfResponse createTurf(String ownerEmail, CreateTurfRequest request, List<MultipartFile> imageFiles) {
        validateTurfRequest(request);
        validateImageBatch(imageFiles);

        User owner = findUser(ownerEmail)
                .orElseThrow(() -> ApiException.notFound("Owner account was not found."));

        boolean isOwner = owner.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_OWNER || role.getName() == RoleName.ROLE_ADMIN);

        if (!isOwner) {
            throw ApiException.forbidden("You do not have permission to create a turf.");
        }
        ensureUniqueTurfName(owner.getId(), request.getCity(), request.getName(), null);

        if (monetizationGateService != null) {
            monetizationGateService.trackAndCheckUsage(owner.getId(), "TURFS");
        }

        String country = request.getCountry() != null && !request.getCountry().isBlank() ? request.getCountry().trim() : "India";
        String state = request.getState() != null && !request.getState().isBlank() ? request.getState().trim() : "Gujarat";
        String timezone = request.getTimezone() != null && !request.getTimezone().isBlank() ? request.getTimezone().trim() : "Asia/Kolkata";
        String holidaySource = request.getHolidaySource() != null && !request.getHolidaySource().isBlank() ? request.getHolidaySource().trim() : "India + Gujarat Calendar";

        Turf turf = Turf.builder()
                .owner(owner)
                .name(request.getName())
                .description(request.getDescription())
                .location(request.getLocation())
                .city(request.getCity())
                .country(country)
                .state(state)
                .timezone(timezone)
                .holidaySource(holidaySource)
                .capacity(request.getCapacity())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .pricePerHour(request.getPricePerHour())
                .pitchType(request.getPitchType())
                .boxType(request.getBoxType())
                .status(TurfStatus.ACTIVE)
                .build();

        if (request.getAmenities() != null) {
            List<TurfAmenity> amenities = request.getAmenities().stream()
                    .map(amenity -> TurfAmenity.builder().turf(turf).amenity(amenity).build())
                    .collect(Collectors.toList());
            turf.setAmenities(amenities);
        }

        Turf savedTurf = turfRepository.save(turf);
        syncRequestedSports(savedTurf.getId(), request.getSports());

        if (imageFiles != null && !imageFiles.isEmpty()) {
            List<TurfImage> images = new ArrayList<>();
            for (MultipartFile file : imageFiles) {
                try {
                    String path = fileStorageService.saveFile(file, savedTurf.getId());
                    images.add(TurfImage.builder().turf(savedTurf).imagePath(path).build());
                } catch (IOException e) {
                    throw ApiException.serviceUnavailable("Failed to save turf image. Please try again.");
                }
            }
            savedTurf.setImages(images);
            savedTurf = turfRepository.save(savedTurf); // Update with images
        }

        cacheService.evictByPrefix("search:");
        return toTurfResponse(savedTurf, request.getSports());
    }

    @Transactional
    public TurfResponse updateTurf(Long turfId, String userEmail, CreateTurfRequest request, List<MultipartFile> imageFiles) {
        validateTurfRequest(request);
        validateImageBatch(imageFiles);

        Turf turf = getTurfEntity(turfId);
        validateOwnershipOrAdmin(turf, userEmail);
        ensureUniqueTurfName(turf.getOwner().getId(), request.getCity(), request.getName(), turfId);

        turf.setName(request.getName());
        turf.setDescription(request.getDescription());
        turf.setLocation(request.getLocation());
        turf.setCity(request.getCity());
        if (request.getCountry() != null) turf.setCountry(request.getCountry().trim());
        if (request.getState() != null) turf.setState(request.getState().trim());
        if (request.getTimezone() != null) turf.setTimezone(request.getTimezone().trim());
        if (request.getHolidaySource() != null) turf.setHolidaySource(request.getHolidaySource().trim());
        turf.setCapacity(request.getCapacity());
        turf.setLatitude(request.getLatitude());
        turf.setLongitude(request.getLongitude());
        turf.setPricePerHour(request.getPricePerHour());
        turf.setPitchType(request.getPitchType());
        turf.setBoxType(request.getBoxType());

        turf.getAmenities().clear();
        if (request.getAmenities() != null) {
            turf.getAmenities().addAll(request.getAmenities().stream()
                    .map(amenity -> TurfAmenity.builder().turf(turf).amenity(amenity).build())
                    .collect(Collectors.toList()));
        }

        updateTurfImages(turf, request.getImages(), imageFiles);

        Turf updatedTurf = turfRepository.save(turf);
        syncRequestedSports(updatedTurf.getId(), request.getSports());
        cacheService.evictByPrefix("search:");
        return toTurfResponse(updatedTurf, request.getSports());
    }

    @Transactional(readOnly = true)
    public TurfResponse getTurf(Long turfId) {
        Turf turf = getTurfEntity(turfId);
        return toTurfResponse(turf);
    }

    @Transactional(readOnly = true)
    public List<TurfResponse> getTurfsByOwner(String ownerEmail) {
        User owner = userRepository.findByEmailOrPhone(ownerEmail)
                .orElseThrow(() -> ApiException.notFound("Owner account was not found."));
        List<Turf> turfs = turfRepository.findByOwnerId(owner.getId());
        if (turfs.isEmpty()) {
            return List.of();
        }

        Map<Long, ReviewRepository.PublishedRatingAggregate> ratingByTurfId = reviewRepository
                .findPublishedRatingAggregates(turfs.stream().map(Turf::getId).toList())
                .stream()
                .collect(Collectors.toMap(
                        ReviewRepository.PublishedRatingAggregate::getTurfId,
                        Function.identity()
                ));

        return turfs.stream()
                .map(turf -> applyCurrentRating(toTurfResponse(turf), ratingByTurfId.get(turf.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getAmenityCatalog() {
        return turfAmenityRepository.findDistinctAmenityNames().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(amenity -> !amenity.isBlank())
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new
                ));
    }

    @Transactional
    public void updateStatus(Long turfId, String userEmail, String status) {
        Turf turf = getTurfEntity(turfId);
        validateOwnershipOrAdmin(turf, userEmail);

        TurfStatus nextStatus;
        try {
            nextStatus = TurfStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Turf status must be ACTIVE, INACTIVE, or MAINTENANCE.");
        }
        turf.setStatus(nextStatus);
        turfRepository.save(turf);
    }

    private Turf getTurfEntity(Long turfId) {
        return turfRepository.findById(turfId)
                .orElseThrow(() -> ApiException.notFound("Turf was not found."));
    }

    private void validateOwnershipOrAdmin(Turf turf, String userEmail) {
        User user = findUser(userEmail)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);

        if (!isAdmin && !turf.getOwner().getId().equals(user.getId())) {
            throw ApiException.forbidden("You do not have permission to manage this turf.");
        }
    }

    private void validateTurfRequest(CreateTurfRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Turf details are required.");
        }
        request.setName(trim(request.getName()));
        request.setDescription(trimToNull(request.getDescription()));
        request.setLocation(trim(request.getLocation()));
        request.setCity(trim(request.getCity()));

        if (isBlank(request.getName())) {
            throw ApiException.badRequest("Turf name is required.");
        }
        if (request.getName().length() > 100) {
            throw ApiException.badRequest("Turf name must be 100 characters or fewer.");
        }
        if (isBlank(request.getLocation())) {
            throw ApiException.badRequest("Turf address is required.");
        }
        if (request.getLocation().length() > 255) {
            throw ApiException.badRequest("Turf address must be 255 characters or fewer.");
        }
        if (isBlank(request.getCity())) {
            throw ApiException.badRequest("City is required.");
        }
        if (request.getCity().length() > 100) {
            throw ApiException.badRequest("City must be 100 characters or fewer.");
        }
        if (request.getDescription() != null && request.getDescription().length() > 2000) {
            throw ApiException.badRequest("Description must be 2,000 characters or fewer.");
        }
        if (request.getCapacity() == null || request.getCapacity() < 1 || request.getCapacity() > 200) {
            throw ApiException.badRequest("Capacity must be between 1 and 200 players.");
        }
        if (request.getPricePerHour() == null || request.getPricePerHour().compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("Price per hour must be greater than zero.");
        }
        if (request.getPricePerHour().compareTo(MAX_PRICE_PER_HOUR) > 0) {
            throw ApiException.badRequest("Price per hour cannot exceed Rs 100000.");
        }
        if (request.getPitchType() == null) {
            throw ApiException.badRequest("Pitch type is required.");
        }
        if (request.getBoxType() == null) {
            throw ApiException.badRequest("Box type is required.");
        }
        normalizeSports(request);
        if (request.getLatitude() != null && (request.getLatitude() < -90 || request.getLatitude() > 90)) {
            throw ApiException.badRequest("Latitude must be between -90 and 90.");
        }
        if (request.getLongitude() != null && (request.getLongitude() < -180 || request.getLongitude() > 180)) {
            throw ApiException.badRequest("Longitude must be between -180 and 180.");
        }
        if (request.getAmenities() != null) {
            List<String> amenities = request.getAmenities().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(amenity -> !amenity.isBlank())
                    .map(amenity -> {
                        if (amenity.length() > 50) {
                            throw ApiException.badRequest("Amenity names must be 50 characters or fewer.");
                        }
                        return amenity;
                    })
                    .toList();
            if (amenities.size() > MAX_AMENITIES) {
                throw ApiException.badRequest("A turf can have up to 20 amenities.");
            }
            Set<String> uniqueAmenities = new LinkedHashSet<>(amenities);
            request.setAmenities(new ArrayList<>(uniqueAmenities));
        }
    }

    private void validateImageBatch(List<MultipartFile> imageFiles) {
        if (imageFiles == null || imageFiles.isEmpty()) {
            return;
        }
        if (imageFiles.size() > MAX_TURF_IMAGES) {
            throw ApiException.badRequest("A turf can have up to 20 images.");
        }
    }

    private void updateTurfImages(Turf turf, List<String> retainedImages, List<MultipartFile> imageFiles) {
        boolean hasRetainedSelection = retainedImages != null;
        List<String> retainedPaths = normalizeImagePaths(retainedImages);
        int newImageCount = imageFiles == null ? 0 : imageFiles.size();

        if (hasRetainedSelection && retainedPaths.size() + newImageCount > MAX_TURF_IMAGES) {
            throw ApiException.badRequest("A turf can have up to 20 images.");
        }

        if (hasRetainedSelection) {
            Set<String> retainedSet = new LinkedHashSet<>(retainedPaths);
            turf.getImages().removeIf(image -> !retainedSet.contains(image.getImagePath()));
        } else if (newImageCount > 0) {
            turf.getImages().clear();
        }

        if (newImageCount == 0) {
            return;
        }

        for (MultipartFile file : imageFiles) {
            try {
                String path = fileStorageService.saveFile(file, turf.getId());
                turf.getImages().add(TurfImage.builder().turf(turf).imagePath(path).build());
            } catch (IOException e) {
                throw ApiException.serviceUnavailable("Failed to save turf image. Please try again.");
            }
        }
    }

    private List<String> normalizeImagePaths(List<String> images) {
        if (images == null) {
            return List.of();
        }
        return images.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(path -> !path.isBlank())
                .distinct()
                .toList();
    }

    private void ensureUniqueTurfName(Long ownerId, String city, String name, Long existingTurfId) {
        boolean duplicate = existingTurfId == null
                ? turfRepository.existsByOwnerIdAndCityIgnoreCaseAndNameIgnoreCase(ownerId, city, name)
                : turfRepository.existsByOwnerIdAndCityIgnoreCaseAndNameIgnoreCaseAndIdNot(ownerId, city, name, existingTurfId);
        if (duplicate) {
            throw ApiException.conflict("You already have a turf with this name in this city.");
        }
    }

    private void normalizeSports(CreateTurfRequest request) {
        List<String> requestedSports = new ArrayList<>();
        addSport(requestedSports, request.getSport());
        addSport(requestedSports, request.getSportCode());
        if (request.getSports() != null) {
            request.getSports().forEach(value -> addSport(requestedSports, value));
        }
        if (request.getSports() == null && request.getSport() == null && request.getSportCode() == null) {
            return;
        }
        if (requestedSports.isEmpty()) {
            throw ApiException.badRequest("At least one supported sport must be selected.");
        }
        if (requestedSports.size() > 8) {
            throw ApiException.badRequest("A turf can support up to 8 sports.");
        }
        request.setSports(requestedSports);
        request.setSport(requestedSports.get(0));
        request.setSportCode(requestedSports.get(0));
    }

    private void addSport(List<String> sports, String value) {
        String normalized = normalizeSportCode(value);
        if (normalized != null && !sports.contains(normalized)) {
            sports.add(normalized);
        }
    }

    private String normalizeSportCode(String value) {
        String cleaned = trimToNull(value);
        if (cleaned == null) {
            return null;
        }
        String normalized = cleaned.toUpperCase(java.util.Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.length() > 64) {
            throw ApiException.badRequest("Sport code must be 64 characters or fewer.");
        }
        return normalized.isBlank() ? null : normalized;
    }

    private void syncRequestedSports(Long turfId, List<String> sports) {
        if (turfId == null || sports == null) {
            return;
        }
        multiSportService.ensureDefaultSportsAvailable();
        sportTurfRepository.deleteByTurfId(turfId);
        sportTurfRepository.flush();
        for (String code : sports) {
            Sport sport = multiSportService.findActiveSport(code)
                    .orElseThrow(() -> ApiException.badRequest("Sport is not active or does not exist: " + code));
            sportTurfRepository.save(SportTurf.builder()
                    .sportId(sport.getId())
                    .turfId(turfId)
                    .build());
        }
        sportTurfRepository.flush();
    }

    private TurfResponse toTurfResponse(Turf turf) {
        return toTurfResponse(turf, null);
    }

    private TurfResponse toTurfResponse(Turf turf, List<String> preferredSports) {
        TurfResponse response = turfMapper.toTurfResponse(turf);
        if (response == null || turf == null || turf.getId() == null) {
            return response;
        }
        List<String> sports = preferredSports == null
                ? safeSports(multiSportService.activeSportCodesForTurf(turf.getId()))
                : safeSports(preferredSports);
        response.setSports(sports);
        response.setSport(sports.isEmpty() ? null : sports.get(0));
        response.setStartingSlotPrice(slotRepository.findMinimumActivePriceByTurfId(turf.getId()).orElse(turf.getPricePerHour()));
        response.setMaximumSlotPrice(slotRepository.findMaximumActivePriceByTurfId(turf.getId()).orElse(turf.getPricePerHour()));
        return response;
    }

    private TurfResponse applyCurrentRating(
            TurfResponse response,
            ReviewRepository.PublishedRatingAggregate aggregate
    ) {
        if (response == null) {
            return null;
        }
        if (aggregate == null) {
            response.setRating(0.0);
            response.setTotalReviews(0);
            return response;
        }
        double averageRating = aggregate.getAverageRating() == null ? 0.0 : aggregate.getAverageRating();
        long totalReviews = aggregate.getTotalReviews() == null ? 0L : aggregate.getTotalReviews();
        response.setRating(Math.round(averageRating * 10.0) / 10.0);
        response.setTotalReviews(Math.toIntExact(totalReviews));
        return response;
    }

    private List<String> safeSports(List<String> sports) {
        return sports == null ? List.of() : sports;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed;
    }

    private java.util.Optional<User> findUser(String loginId) {
        if (loginId != null && loginId.contains("@")) {
            java.util.Optional<User> byEmail = userRepository.findByEmail(loginId);
            if (byEmail.isPresent()) {
                return byEmail;
            }
        }
        return userRepository.findByEmailOrPhone(loginId);
    }
}
