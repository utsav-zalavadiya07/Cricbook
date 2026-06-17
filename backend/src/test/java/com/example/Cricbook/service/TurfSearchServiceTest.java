package com.example.Cricbook.service;

import com.example.Cricbook.dto.TurfSearchRequest;
import com.example.Cricbook.dto.TurfSearchResponse;
import com.example.Cricbook.entity.Turf;
import com.example.Cricbook.entity.TurfAmenity;
import com.example.Cricbook.enums.TurfStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.TurfRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TurfSearchServiceTest {

    @Mock
    private TurfRepository turfRepository;
    @Mock
    private CacheService cacheService;
    @Mock
    private MultiSportService multiSportService;

    @InjectMocks
    private TurfSearchService turfSearchService;

    @Test
    void testSearch_Success() {
        TurfSearchRequest request = TurfSearchRequest.builder()
                .city("Test City")
                .page(0)
                .size(10)
                .build();

        Turf turf = Turf.builder().id(1L).name("Royal Turf").city("Test City").build();
        Page<Turf> page = new PageImpl<>(Collections.singletonList(turf));

        executeCachedSupplier(60L);
        when(multiSportService.activeSportCodesByTurfId(any())).thenReturn(Map.of(1L, List.of("CRICKET")));

        when(turfRepository.searchActiveTurfs(
                any(), eq("Test City"), any(), any(), any(), any(), any(), any(), eq(true), any(Pageable.class)
        )).thenReturn(page);

        List<TurfSearchResponse> result = turfSearchService.search(request);

        assertFalse(result.isEmpty());
        assertEquals("Royal Turf", result.get(0).getName());
    }

    @Test
    void searchRejectsInvalidPriceRangeBeforeRepositoryCall() {
        TurfSearchRequest request = TurfSearchRequest.builder()
                .minPrice(BigDecimal.valueOf(2000))
                .maxPrice(BigDecimal.valueOf(1000))
                .build();

        ApiException exception = assertThrows(ApiException.class, () -> turfSearchService.search(request));

        assertEquals("Minimum price cannot be greater than maximum price.", exception.getMessage());
        verifyNoInteractions(cacheService, turfRepository, multiSportService);
    }

    @Test
    void searchRequiresAllRequestedAmenities() {
        TurfSearchRequest request = TurfSearchRequest.builder()
                .amenities(List.of("Parking", "Washroom"))
                .page(0)
                .size(10)
                .build();

        Turf parkingOnly = Turf.builder()
                .id(1L)
                .name("Parking Only")
                .city("Surat")
                .status(TurfStatus.ACTIVE)
                .amenities(List.of(TurfAmenity.builder().amenity("Parking").build()))
                .build();
        Turf fullMatch = Turf.builder()
                .id(2L)
                .name("Full Match")
                .city("Surat")
                .status(TurfStatus.ACTIVE)
                .amenities(List.of(
                        TurfAmenity.builder().amenity("Parking").build(),
                        TurfAmenity.builder().amenity("Washroom").build()
                ))
                .build();

        executeCachedSupplier(60L);
        when(multiSportService.activeSportCodesByTurfId(any())).thenReturn(Map.of(2L, List.of("CRICKET")));
        when(turfRepository.searchActiveTurfs(
                any(), any(), any(), any(), any(), any(), any(), eq(List.of("Parking", "Washroom")), eq(false), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(parkingOnly, fullMatch)));

        List<TurfSearchResponse> result = turfSearchService.search(request);

        assertEquals(1, result.size());
        assertEquals("Full Match", result.get(0).getName());
    }

    @Test
    void searchBatchesSportFilteringAndResponseSportCodes() {
        Turf cricket = Turf.builder()
                .id(1L)
                .name("Cricket Arena")
                .city("Surat")
                .status(TurfStatus.ACTIVE)
                .build();
        Turf football = Turf.builder()
                .id(2L)
                .name("Football Arena")
                .city("Surat")
                .status(TurfStatus.ACTIVE)
                .build();

        TurfSearchRequest request = TurfSearchRequest.builder()
                .sportCode("FOOTBALL")
                .page(0)
                .size(10)
                .build();

        executeCachedSupplier(60L);
        when(turfRepository.searchActiveTurfs(
                any(), any(), any(), any(), any(), any(), any(), any(), eq(true), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(cricket, football)));
        when(multiSportService.turfSupportByTurfId(eq(List.of(1L, 2L)), eq("FOOTBALL")))
                .thenReturn(Map.of(1L, false, 2L, true));
        when(multiSportService.activeSportCodesByTurfId(eq(List.of(1L, 2L))))
                .thenReturn(Map.of(1L, List.of("CRICKET"), 2L, List.of("FOOTBALL")));

        List<TurfSearchResponse> result = turfSearchService.search(request);

        assertEquals(1, result.size());
        assertEquals("Football Arena", result.get(0).getName());
        assertEquals(List.of("FOOTBALL"), result.get(0).getSports());
        verify(multiSportService).turfSupportByTurfId(eq(List.of(1L, 2L)), eq("FOOTBALL"));
        verify(multiSportService, never()).turfSupportsSport(any(), any());
    }

    @Test
    void suggestionsReturnDistinctNamesCitiesAndLocationsFromProjection() {
        executeCachedSupplier(30L);
        when(turfRepository.findSearchSuggestions(eq("Royal"), any(Pageable.class)))
                .thenReturn(List.of(suggestion("Royal Cricket Turf", "Ahmedabad", "Navrangpura")));

        List<String> suggestions = turfSearchService.suggestions(" Royal ", 10);

        assertEquals(List.of("Royal Cricket Turf", "Ahmedabad", "Navrangpura"), suggestions);
        verify(turfRepository, never()).searchActiveTurfs(any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any(Pageable.class));
    }

    private void executeCachedSupplier(long ttlSeconds) {
        when(cacheService.getOrCompute(any(), eq(ttlSeconds), any())).thenAnswer(invocation -> {
            Supplier<List<TurfSearchResponse>> supplier = invocation.getArgument(2);
            return supplier.get();
        });
    }

    private TurfRepository.SearchSuggestionView suggestion(String name, String city, String location) {
        return new TurfRepository.SearchSuggestionView() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getCity() {
                return city;
            }

            @Override
            public String getLocation() {
                return location;
            }
        };
    }
}
