package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalizationResponse {
    private List<TurfResponse> recommendedTurfs;
    private List<RecentSearchResponse> recentSearches;
    private List<BookingResponse> recentlyBooked;
    private List<String> preferredLocations;
    private List<String> recommendedTimings;
}
