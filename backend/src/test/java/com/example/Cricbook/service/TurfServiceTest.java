package com.example.Cricbook.service;

import com.example.Cricbook.dto.CreateTurfRequest;
import com.example.Cricbook.dto.TurfResponse;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.RoleName;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.mapper.TurfMapper;
import com.example.Cricbook.repository.SportTurfRepository;
import com.example.Cricbook.repository.ReviewRepository;
import com.example.Cricbook.repository.TurfAmenityRepository;
import com.example.Cricbook.repository.TurfRepository;
import com.example.Cricbook.repository.UserRepository;
import com.example.Cricbook.repository.SlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TurfServiceTest {

    @Mock
    private TurfRepository turfRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TurfMapper turfMapper;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private MultiSportService multiSportService;
    @Mock
    private SportTurfRepository sportTurfRepository;
    @Mock
    private TurfAmenityRepository turfAmenityRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private SlotRepository slotRepository;
    @Mock
    private CacheService cacheService;

    @InjectMocks
    private TurfService turfService;

    @Test
    void testCreateTurf_Success() {
        String email = "owner@example.com";
        CreateTurfRequest request = validTurfRequest("Royal Turf");

        Role ownerRole = new Role(1L, RoleName.ROLE_OWNER);
        User owner = User.builder().id(1L).email(email).build();
        owner.getRoles().add(ownerRole);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(owner));
        when(turfRepository.save(any(Turf.class))).thenAnswer(i -> i.getArguments()[0]);
        when(turfMapper.toTurfResponse(any(Turf.class))).thenReturn(TurfResponse.builder().build());

        TurfResponse result = turfService.createTurf(email, request, Collections.emptyList());

        assertNotNull(result);
        org.mockito.ArgumentCaptor<Turf> captor = org.mockito.ArgumentCaptor.forClass(Turf.class);
        verify(turfRepository).save(captor.capture());
        Turf saved = captor.getValue();
        assertEquals(12, saved.getCapacity());
        assertEquals("Royal Turf", saved.getName());
        verify(cacheService).evictByPrefix("search:");
    }

    @Test
    void testCreateTurf_NoPermission() {
        String email = "user@example.com";
        CreateTurfRequest request = validTurfRequest("Royal Turf");

        User user = User.builder().id(1L).email(email).build(); // No owner role

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        assertThrows(ApiException.class, () -> turfService.createTurf(email, request, Collections.emptyList()));
    }

    @Test
    void testGetTurf_Success() {
        Long id = 1L;
        Turf turf = Turf.builder().id(id).name("Royal Turf").pricePerHour(BigDecimal.valueOf(1500)).build();
        when(turfRepository.findById(id)).thenReturn(Optional.of(turf));
        when(turfMapper.toTurfResponse(turf)).thenReturn(TurfResponse.builder().build());
        when(slotRepository.findMinimumActivePriceByTurfId(id)).thenReturn(Optional.of(BigDecimal.valueOf(500)));
        when(slotRepository.findMaximumActivePriceByTurfId(id)).thenReturn(Optional.of(BigDecimal.valueOf(1500)));

        TurfResponse result = turfService.getTurf(id);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(500), result.getStartingSlotPrice());
        assertEquals(BigDecimal.valueOf(1500), result.getMaximumSlotPrice());
    }

    @Test
    void testGetTurfsByOwner_UsesPublishedReviewsInsteadOfStaleCachedRating() {
        String email = "owner@example.com";
        User owner = User.builder().id(7L).email(email).build();
        Turf turf = Turf.builder().id(21L).owner(owner).rating(0.0).totalReviews(0).build();
        TurfResponse mapped = TurfResponse.builder().id(21L).rating(0.0).totalReviews(0).build();
        ReviewRepository.PublishedRatingAggregate aggregate = mock(ReviewRepository.PublishedRatingAggregate.class);

        when(userRepository.findByEmailOrPhone(email)).thenReturn(Optional.of(owner));
        when(turfRepository.findByOwnerId(owner.getId())).thenReturn(List.of(turf));
        when(turfMapper.toTurfResponse(turf)).thenReturn(mapped);
        when(reviewRepository.findPublishedRatingAggregates(List.of(turf.getId())))
                .thenReturn(List.of(aggregate));
        when(aggregate.getTurfId()).thenReturn(turf.getId());
        when(aggregate.getAverageRating()).thenReturn(4.26);
        when(aggregate.getTotalReviews()).thenReturn(3L);

        List<TurfResponse> result = turfService.getTurfsByOwner(email);

        assertEquals(1, result.size());
        assertEquals(4.3, result.get(0).getRating());
        assertEquals(3, result.get(0).getTotalReviews());
    }

    @Test
    void testGetTurfsByOwner_ReturnsZeroWhenNoPublishedReviewsExist() {
        String email = "owner@example.com";
        User owner = User.builder().id(7L).email(email).build();
        Turf turf = Turf.builder().id(21L).owner(owner).rating(4.8).totalReviews(6).build();
        TurfResponse mapped = TurfResponse.builder().id(21L).rating(4.8).totalReviews(6).build();

        when(userRepository.findByEmailOrPhone(email)).thenReturn(Optional.of(owner));
        when(turfRepository.findByOwnerId(owner.getId())).thenReturn(List.of(turf));
        when(turfMapper.toTurfResponse(turf)).thenReturn(mapped);
        when(reviewRepository.findPublishedRatingAggregates(List.of(turf.getId())))
                .thenReturn(List.of());

        List<TurfResponse> result = turfService.getTurfsByOwner(email);

        assertEquals(0.0, result.get(0).getRating());
        assertEquals(0, result.get(0).getTotalReviews());
    }

    @Test
    void testGetAmenityCatalog_NormalizesAndDeduplicatesStoredAmenities() {
        when(turfAmenityRepository.findDistinctAmenityNames())
                .thenReturn(List.of(" Floodlights ", "Parking", "Parking", "", "Washroom"));

        List<String> result = turfService.getAmenityCatalog();

        assertEquals(List.of("Floodlights", "Parking", "Washroom"), result);
    }

    @Test
    void testUpdateTurf_Success() {
        Long turfId = 1L;
        String email = "owner@example.com";
        CreateTurfRequest request = validTurfRequest("Updated Turf");

        User owner = User.builder().id(1L).email(email).build();
        Turf turf = Turf.builder().id(turfId).owner(owner).amenities(new java.util.ArrayList<>()).images(new java.util.ArrayList<>()).build();

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(turf));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(owner));
        when(turfRepository.save(any(Turf.class))).thenReturn(turf);
        when(turfMapper.toTurfResponse(any(Turf.class))).thenReturn(TurfResponse.builder().build());

        TurfResponse result = turfService.updateTurf(turfId, email, request, null);

        assertNotNull(result);
        assertEquals("Updated Turf", turf.getName());
        verify(cacheService).evictByPrefix("search:");
    }

    @Test
    void testUpdateTurf_SyncsSelectedSportForSearchFilters() {
        Long turfId = 1L;
        String email = "owner@example.com";
        CreateTurfRequest request = validTurfRequest("Football Arena");
        request.setSport("FOOTBALL");
        request.setSportCode("FOOTBALL");
        request.setSports(List.of("Football"));

        User owner = User.builder().id(1L).email(email).build();
        Turf turf = Turf.builder()
                .id(turfId)
                .owner(owner)
                .amenities(new java.util.ArrayList<>())
                .images(new java.util.ArrayList<>())
                .build();

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(turf));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(owner));
        when(turfRepository.save(any(Turf.class))).thenReturn(turf);
        when(multiSportService.findActiveSport("FOOTBALL")).thenReturn(Optional.of(Sport.builder().id(2L).code("FOOTBALL").isActive(true).build()));
        when(turfMapper.toTurfResponse(any(Turf.class))).thenReturn(TurfResponse.builder().build());

        TurfResponse result = turfService.updateTurf(turfId, email, request, null);

        assertEquals(List.of("FOOTBALL"), result.getSports());
        assertEquals("FOOTBALL", result.getSport());
        InOrder mappingOrder = inOrder(multiSportService, sportTurfRepository);
        mappingOrder.verify(multiSportService).ensureDefaultSportsAvailable();
        mappingOrder.verify(sportTurfRepository).deleteByTurfId(turfId);
        mappingOrder.verify(sportTurfRepository).flush();
        mappingOrder.verify(multiSportService).findActiveSport("FOOTBALL");
        org.mockito.ArgumentCaptor<SportTurf> mappingCaptor = org.mockito.ArgumentCaptor.forClass(SportTurf.class);
        verify(sportTurfRepository).save(mappingCaptor.capture());
        assertEquals(2L, mappingCaptor.getValue().getSportId());
        assertEquals(turfId, mappingCaptor.getValue().getTurfId());
        verify(sportTurfRepository, times(2)).flush();
    }

    @Test
    void testUpdateTurf_RetainsSelectedExistingImagesAndAddsNewUploads() throws Exception {
        Long turfId = 1L;
        String email = "owner@example.com";
        CreateTurfRequest request = validTurfRequest("Updated Turf");
        request.setImages(List.of("/uploads/turf/1/keep.jpg"));

        User owner = User.builder().id(1L).email(email).build();
        Turf turf = Turf.builder()
                .id(turfId)
                .owner(owner)
                .amenities(new java.util.ArrayList<>())
                .images(new java.util.ArrayList<>(List.of(
                        TurfImage.builder().turf(null).imagePath("/uploads/turf/1/remove.jpg").build(),
                        TurfImage.builder().turf(null).imagePath("/uploads/turf/1/keep.jpg").build()
                )))
                .build();
        turf.getImages().forEach(image -> image.setTurf(turf));

        MultipartFile newImage = mock(MultipartFile.class);

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(turf));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(owner));
        when(fileStorageService.saveFile(newImage, turfId)).thenReturn("/uploads/turf/1/new.jpg");
        when(turfRepository.save(any(Turf.class))).thenReturn(turf);
        when(turfMapper.toTurfResponse(any(Turf.class))).thenReturn(TurfResponse.builder().build());

        TurfResponse result = turfService.updateTurf(turfId, email, request, List.of(newImage));

        assertNotNull(result);
        assertEquals(
                List.of("/uploads/turf/1/keep.jpg", "/uploads/turf/1/new.jpg"),
                turf.getImages().stream().map(TurfImage::getImagePath).toList()
        );
        verify(fileStorageService).saveFile(newImage, turfId);
    }

    @Test
    void testCreateTurf_InvalidPayload() {
        String email = "owner@example.com";
        CreateTurfRequest request = validTurfRequest("Royal Turf");
        request.setPricePerHour(BigDecimal.ZERO);

        ApiException exception = assertThrows(ApiException.class, () -> turfService.createTurf(email, request, Collections.emptyList()));

        assertEquals("Price per hour must be greater than zero.", exception.getMessage());
        verify(turfRepository, never()).save(any(Turf.class));
    }

    @Test
    void testCreateTurf_RejectsDuplicateNameForSameOwnerCity() {
        String email = "owner@example.com";
        CreateTurfRequest request = validTurfRequest("Royal Turf");

        Role ownerRole = new Role(1L, RoleName.ROLE_OWNER);
        User owner = User.builder().id(1L).email(email).build();
        owner.getRoles().add(ownerRole);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(owner));
        when(turfRepository.existsByOwnerIdAndCityIgnoreCaseAndNameIgnoreCase(1L, "Ahmedabad", "Royal Turf"))
                .thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> turfService.createTurf(email, request, Collections.emptyList()));

        assertEquals("You already have a turf with this name in this city.", exception.getMessage());
        verify(turfRepository, never()).save(any(Turf.class));
    }

    @Test
    void testCreateTurf_DeduplicatesAmenitiesAndRejectsTooManyImages() {
        String email = "owner@example.com";
        CreateTurfRequest request = validTurfRequest("Royal Turf");
        request.setAmenities(List.of(" Parking ", "Parking", "Floodlights"));

        Role ownerRole = new Role(1L, RoleName.ROLE_OWNER);
        User owner = User.builder().id(1L).email(email).build();
        owner.getRoles().add(ownerRole);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(owner));
        when(turfRepository.save(any(Turf.class))).thenAnswer(i -> i.getArguments()[0]);
        when(turfMapper.toTurfResponse(any(Turf.class))).thenReturn(TurfResponse.builder().build());

        turfService.createTurf(email, request, Collections.emptyList());

        org.mockito.ArgumentCaptor<Turf> captor = org.mockito.ArgumentCaptor.forClass(Turf.class);
        verify(turfRepository).save(captor.capture());
        assertEquals(List.of("Parking", "Floodlights"), captor.getValue().getAmenities().stream().map(TurfAmenity::getAmenity).toList());

        CreateTurfRequest imageRequest = validTurfRequest("Another Turf");
        ApiException exception = assertThrows(ApiException.class, () -> turfService.createTurf(
                email,
                imageRequest,
                java.util.Collections.nCopies(21, mock(MultipartFile.class))
        ));
        assertEquals("A turf can have up to 20 images.", exception.getMessage());
    }

    @Test
    void testCreateTurf_SyncsSelectedSportsForSearchFilters() {
        String email = "owner@example.com";
        CreateTurfRequest request = validTurfRequest("Multi Sport Arena");
        request.setSports(List.of(" cricket ", "Football", "FOOTBALL"));

        Role ownerRole = new Role(1L, RoleName.ROLE_OWNER);
        User owner = User.builder().id(1L).email(email).build();
        owner.getRoles().add(ownerRole);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(owner));
        when(turfRepository.save(any(Turf.class))).thenAnswer(invocation -> {
            Turf turf = invocation.getArgument(0);
            turf.setId(10L);
            return turf;
        });
        when(multiSportService.findActiveSport("CRICKET")).thenReturn(Optional.of(Sport.builder().id(1L).code("CRICKET").isActive(true).build()));
        when(multiSportService.findActiveSport("FOOTBALL")).thenReturn(Optional.of(Sport.builder().id(2L).code("FOOTBALL").isActive(true).build()));
        when(turfMapper.toTurfResponse(any(Turf.class))).thenReturn(TurfResponse.builder().build());

        TurfResponse result = turfService.createTurf(email, request, Collections.emptyList());

        assertEquals(List.of("CRICKET", "FOOTBALL"), result.getSports());
        assertEquals("CRICKET", result.getSport());
        verify(sportTurfRepository).deleteByTurfId(10L);
        verify(multiSportService).ensureDefaultSportsAvailable();
        verify(sportTurfRepository, times(2)).flush();
        org.mockito.ArgumentCaptor<SportTurf> mappingCaptor = org.mockito.ArgumentCaptor.forClass(SportTurf.class);
        verify(sportTurfRepository, times(2)).save(mappingCaptor.capture());
        assertEquals(List.of(1L, 2L), mappingCaptor.getAllValues().stream().map(SportTurf::getSportId).toList());
        assertEquals(List.of(10L, 10L), mappingCaptor.getAllValues().stream().map(SportTurf::getTurfId).toList());
    }

    @Test
    void testUpdateStatus_Success() {
        Long turfId = 1L;
        String email = "owner@example.com";

        User owner = User.builder().id(1L).email(email).build();
        Turf turf = Turf.builder().id(turfId).owner(owner).build();

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(turf));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(owner));

        turfService.updateStatus(turfId, email, "INACTIVE");

        assertEquals(com.example.Cricbook.enums.TurfStatus.INACTIVE, turf.getStatus());
        verify(turfRepository).save(turf);
    }

    private CreateTurfRequest validTurfRequest(String name) {
        CreateTurfRequest request = new CreateTurfRequest();
        request.setName(name);
        request.setDescription("Premium cricket turf");
        request.setLocation("SG Highway");
        request.setCity("Ahmedabad");
        request.setCapacity(12);
        request.setLatitude(23.0225);
        request.setLongitude(72.5714);
        request.setPricePerHour(BigDecimal.valueOf(1200));
        request.setPitchType(com.example.Cricbook.enums.PitchType.TURF);
        request.setBoxType(com.example.Cricbook.enums.BoxType.SIX_V_SIX);
        return request;
    }
}
