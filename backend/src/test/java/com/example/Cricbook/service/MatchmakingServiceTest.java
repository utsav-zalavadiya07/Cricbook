package com.example.Cricbook.service;

import com.example.Cricbook.dto.MatchmakingCandidateResponse;
import com.example.Cricbook.dto.MatchmakingMatchResponse;
import com.example.Cricbook.dto.PlayerDiscoverResponse;
import com.example.Cricbook.dto.PublicMatchResponse;
import com.example.Cricbook.dto.TrustScoreResponse;
import com.example.Cricbook.entity.PlayerAvailability;
import com.example.Cricbook.entity.PublicMatch;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.PublicMatchStatus;
import com.example.Cricbook.repository.MatchParticipantRepository;
import com.example.Cricbook.repository.PlayerAvailabilityRepository;
import com.example.Cricbook.repository.PublicMatchRepository;
import com.example.Cricbook.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchmakingServiceTest {
    @Mock private SocialService socialService;
    @Mock private TrustScoreService trustScoreService;
    @Mock private UserRepository userRepository;
    @Mock private PublicMatchRepository publicMatchRepository;
    @Mock private MatchParticipantRepository matchParticipantRepository;
    @Mock private PlayerAvailabilityRepository playerAvailabilityRepository;
    @InjectMocks private MatchmakingService matchmakingService;

    @Test
    void recommendationsRankOpenMatchesAndExcludeJoinedMatches() {
        User user = user(1L, "Player");
        when(userRepository.findByEmailOrPhone("player@test.com")).thenReturn(Optional.of(user));
        when(playerAvailabilityRepository.findByUserId(1L)).thenReturn(Optional.of(availability("Ahmedabad")));
        when(matchParticipantRepository.existsByMatchIdAndUserId(10L, 1L)).thenReturn(false);
        when(matchParticipantRepository.existsByMatchIdAndUserId(11L, 1L)).thenReturn(true);
        when(socialService.getOpenMatches()).thenReturn(List.of(match(10L, "Ahmedabad", 8, 14), match(11L, "Surat", 4, 14)));

        List<MatchmakingMatchResponse> result = matchmakingService.recommendations("player@test.com");

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
        assertTrue(result.get(0).getMatchScore() > 0);
        assertEquals(6, result.get(0).getSlotsLeft());
    }

    @Test
    void candidatesApplyTrustedOnlyFilterAndSortByRecommendationScore() {
        PublicMatch match = PublicMatch.builder()
                .id(20L)
                .city("Ahmedabad")
                .maxPlayers(14)
                .status(PublicMatchStatus.OPEN)
                .build();
        when(publicMatchRepository.findById(20L)).thenReturn(Optional.of(match));
        when(socialService.discoverPlayers(eq("captain@test.com"), eq("Ahmedabad"), isNull(), isNull()))
                .thenReturn(List.of(player(2L, "Trusted", 4.7), player(3L, "Risky", 3.1)));
        when(matchParticipantRepository.existsByMatchIdAndUserId(anyLong(), anyLong())).thenReturn(false);
        when(trustScoreService.getProfile(2L)).thenReturn(TrustScoreResponse.builder().userId(2L).score(92).build());
        when(trustScoreService.getProfile(3L)).thenReturn(TrustScoreResponse.builder().userId(3L).score(55).build());

        List<MatchmakingCandidateResponse> result = matchmakingService.candidates("captain@test.com", 20L, "Ahmedabad", "Advanced", true);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getUserId());
        assertTrue(result.get(0).getRecommendationScore() >= 80);
    }

    private User user(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(name.toLowerCase() + "@test.com");
        return user;
    }

    private PlayerAvailability availability(String city) {
        return PlayerAvailability.builder().homeCity(city).preferredWindow("Evening").canTravel(true).build();
    }

    private PublicMatchResponse match(Long id, String city, int joined, int max) {
        return PublicMatchResponse.builder()
                .id(id)
                .title("Open Match " + id)
                .city(city)
                .location(city + " Turf")
                .matchDate(LocalDate.now())
                .startTime(LocalTime.of(20, 0))
                .maxPlayers(max)
                .joinedPlayers(joined)
                .status(PublicMatchStatus.OPEN)
                .notes("Skill:Intermediate\nTrustedOnly:false")
                .build();
    }

    private PlayerDiscoverResponse player(Long id, String name, double rating) {
        return PlayerDiscoverResponse.builder()
                .userId(id)
                .name(name)
                .city("Ahmedabad")
                .rating(rating)
                .matches(40)
                .wins(20)
                .build();
    }
}
