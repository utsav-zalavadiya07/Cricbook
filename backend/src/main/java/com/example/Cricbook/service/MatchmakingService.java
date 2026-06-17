package com.example.Cricbook.service;

import com.example.Cricbook.dto.*;
import com.example.Cricbook.entity.PublicMatch;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.MatchVisibility;
import com.example.Cricbook.enums.PublicMatchStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.MatchParticipantRepository;
import com.example.Cricbook.repository.PlayerAvailabilityRepository;
import com.example.Cricbook.repository.PublicMatchRepository;
import com.example.Cricbook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchmakingService {
    private final SocialService socialService;
    private final TrustScoreService trustScoreService;
    private final UserRepository userRepository;
    private final PublicMatchRepository publicMatchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final PlayerAvailabilityRepository playerAvailabilityRepository;

    @Transactional
    public MatchmakingMatchResponse create(String username, MatchmakingCreateRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Matchmaking details are required.");
        }
        CreatePublicMatchRequest socialRequest = new CreatePublicMatchRequest();
        socialRequest.setTurfId(request.getTurfId());
        socialRequest.setTitle(request.getTitle());
        socialRequest.setSport(request.getSport() == null || request.getSport().isBlank() ? "Cricket" : request.getSport());
        socialRequest.setCity(request.getCity());
        socialRequest.setLocation(request.getLocation());
        socialRequest.setMatchDate(request.getMatchDate());
        socialRequest.setStartTime(request.getStartTime());
        socialRequest.setEndTime(request.getEndTime());
        socialRequest.setMaxPlayers(request.getMaxPlayers());
        socialRequest.setVisibility(request.getVisibility() == null ? MatchVisibility.PUBLIC : request.getVisibility());
        socialRequest.setFormat(request.getFormat());
        socialRequest.setOvers(request.getOvers());
        socialRequest.setEntryFee(request.getEntryFee());
        socialRequest.setHomeTeamId(request.getHomeTeamId());
        socialRequest.setAwayTeamId(request.getAwayTeamId());
        socialRequest.setNotes(buildNotes(request));
        PublicMatchResponse created = socialService.createPublicMatch(username, socialRequest);
        List<MatchmakingCandidateResponse> candidates = candidates(username, created.getId(), request.getCity(), request.getSkillLevel(), Boolean.TRUE.equals(request.getTrustedOnly()));
        return map(created, scoreForMatch(created, findUser(username), candidates.size()), "Created open match and ranked available players.", candidates);
    }

    @Transactional(readOnly = true)
    public List<MatchmakingMatchResponse> open(String username, String city, String skillLevel, Boolean trustedOnly) {
        User user = findUser(username);
        return socialService.getOpenMatches().stream()
                .filter(match -> city == null || city.isBlank() || contains(match.getCity(), city))
                .map(match -> {
                    List<MatchmakingCandidateResponse> candidates = candidates(username, match.getId(), match.getCity(), skillLevel, Boolean.TRUE.equals(trustedOnly));
                    return map(match, scoreForMatch(match, user, candidates.size()), "Open match with smart fill potential.", candidates);
                })
                .sorted(Comparator.comparing(MatchmakingMatchResponse::getMatchScore).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MatchmakingMatchResponse> recommendations(String username) {
        User user = findUser(username);
        return socialService.getOpenMatches().stream()
                .filter(match -> !matchParticipantRepository.existsByMatchIdAndUserId(match.getId(), user.getId()))
                .map(match -> map(match, scoreForMatch(match, user, 0), reasonFor(user, match), List.of()))
                .sorted(Comparator.comparing(MatchmakingMatchResponse::getMatchScore).reversed())
                .limit(12)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MatchmakingMatchResponse> nearby(String username, String city) {
        String resolvedCity = city;
        if (resolvedCity == null || resolvedCity.isBlank()) {
            resolvedCity = trustSafeProfileCity(username);
        }
        return open(username, resolvedCity, null, false);
    }

    @Transactional
    public MatchmakingMatchResponse join(String username, Long matchId) {
        PublicMatchResponse joined = socialService.joinMatch(username, matchId);
        List<MatchmakingCandidateResponse> candidates = candidates(username, matchId, joined.getCity(), null, false);
        return map(joined, 100, "Joined match. Match chat is now available.", candidates);
    }

    @Transactional(readOnly = true)
    public List<MatchmakingCandidateResponse> candidates(String username, Long matchId, String city, String skillLevel, boolean trustedOnly) {
        PublicMatch match = publicMatchRepository.findById(matchId)
                .orElseThrow(() -> ApiException.notFound("Match was not found."));
        return socialService.discoverPlayers(username, city == null ? match.getCity() : city, null, null).stream()
                .filter(player -> !matchParticipantRepository.existsByMatchIdAndUserId(matchId, player.getUserId()))
                .map(player -> mapCandidate(player, skillLevel))
                .filter(candidate -> !trustedOnly || candidate.getTrustScore() >= 80)
                .sorted(Comparator.comparing(MatchmakingCandidateResponse::getRecommendationScore).reversed())
                .limit(Math.max(6, match.getMaxPlayers()))
                .toList();
    }

    @Transactional(readOnly = true)
    public MatchmakingAnalyticsResponse analytics() {
        List<PublicMatch> matches = publicMatchRepository.findByStatusInOrderByMatchDateAscStartTimeAsc(List.of(PublicMatchStatus.OPEN, PublicMatchStatus.FULL));
        long totalParticipants = matches.stream().mapToLong(match -> matchParticipantRepository.countByMatchId(match.getId())).sum();
        double fillRate = matches.isEmpty() ? 0 : matches.stream()
                .mapToDouble(match -> matchParticipantRepository.countByMatchId(match.getId()) * 100.0 / Math.max(match.getMaxPlayers(), 1))
                .average()
                .orElse(0);
        return MatchmakingAnalyticsResponse.builder()
                .openMatches(matches.stream().filter(match -> match.getStatus() == PublicMatchStatus.OPEN).count())
                .fullMatches(matches.stream().filter(match -> match.getStatus() == PublicMatchStatus.FULL).count())
                .totalParticipants(totalParticipants)
                .averageFillRate(Math.round(fillRate * 10.0) / 10.0)
                .mostFillableMatches(matches.stream()
                        .sorted(Comparator.comparing((PublicMatch match) -> matchParticipantRepository.countByMatchId(match.getId())).reversed())
                        .limit(8)
                        .map(match -> map(rawMap(match), 75, "High community activity.", List.of()))
                        .toList())
                .build();
    }

    private MatchmakingCandidateResponse mapCandidate(PlayerDiscoverResponse player, String requestedSkill) {
        TrustScoreResponse trust = trustScoreService.getProfile(player.getUserId());
        int skillScore = skillScore(player, requestedSkill);
        int trustScore = trust.getScore() == null ? 70 : trust.getScore();
        int availabilityScore = player.getAvailability() == null ? 50 : 80;
        int ratingScore = (int) Math.min(100, Math.round((player.getRating() == null ? 0 : player.getRating()) * 20));
        int total = (int) Math.round(skillScore * 0.25 + trustScore * 0.35 + availabilityScore * 0.20 + ratingScore * 0.20);
        return MatchmakingCandidateResponse.builder()
                .userId(player.getUserId())
                .name(player.getName())
                .city(player.getCity() == null && player.getAvailability() != null ? player.getAvailability().getHomeCity() : player.getCity())
                .position(player.getPosition())
                .preferredTime(player.getPreferredTime())
                .skillLevel(resolveSkill(player))
                .trustScore(trustScore)
                .rating(player.getRating())
                .matches(player.getMatches())
                .recommendationScore(total)
                .reason("Trust " + trustScore + ", " + resolveSkill(player) + " skill, availability match " + availabilityScore + ".")
                .build();
    }

    private MatchmakingMatchResponse map(PublicMatchResponse match, int matchScore, String reason, List<MatchmakingCandidateResponse> candidates) {
        int joined = match.getJoinedPlayers() == null ? 0 : match.getJoinedPlayers();
        int max = match.getMaxPlayers() == null ? 0 : match.getMaxPlayers();
        return MatchmakingMatchResponse.builder()
                .id(match.getId())
                .title(match.getTitle())
                .city(match.getCity())
                .location(match.getLocation())
                .matchDate(match.getMatchDate())
                .startTime(match.getStartTime())
                .endTime(match.getEndTime())
                .maxPlayers(max)
                .joinedPlayers(joined)
                .slotsLeft(Math.max(max - joined, 0))
                .skillLevel(extractTag(match.getNotes(), "Skill"))
                .autoApprove(Boolean.parseBoolean(extractTag(match.getNotes(), "AutoApprove")))
                .trustedOnly(Boolean.parseBoolean(extractTag(match.getNotes(), "TrustedOnly")))
                .status(match.getStatus())
                .matchScore(matchScore)
                .recommendationReason(reason)
                .match(match)
                .candidates(candidates)
                .build();
    }

    private PublicMatchResponse rawMap(PublicMatch match) {
        return PublicMatchResponse.builder()
                .id(match.getId())
                .creatorId(match.getCreator().getId())
                .creatorName(match.getCreator().getName())
                .turfId(match.getTurf() == null ? null : match.getTurf().getId())
                .turfName(match.getTurf() == null ? null : match.getTurf().getName())
                .title(match.getTitle())
                .sport(match.getSport())
                .city(match.getCity())
                .location(match.getLocation())
                .matchDate(match.getMatchDate())
                .startTime(match.getStartTime())
                .endTime(match.getEndTime())
                .maxPlayers(match.getMaxPlayers())
                .joinedPlayers((int) matchParticipantRepository.countByMatchId(match.getId()))
                .status(match.getStatus())
                .notes(match.getNotes())
                .build();
    }

    private String buildNotes(MatchmakingCreateRequest request) {
        String base = request.getNotes() == null ? "" : request.getNotes().trim();
        return (base + "\nSkill:" + defaultValue(request.getSkillLevel(), "Intermediate")
                + "\nRequiredPlayers:" + (request.getRequiredPlayers() == null ? "" : request.getRequiredPlayers())
                + "\nAutoApprove:" + Boolean.TRUE.equals(request.getAutoApprove())
                + "\nTrustedOnly:" + Boolean.TRUE.equals(request.getTrustedOnly())).trim();
    }

    private int scoreForMatch(PublicMatchResponse match, User user, int candidatesCount) {
        String homeCity = playerAvailabilityRepository.findByUserId(user.getId()).map(availability -> availability.getHomeCity()).orElse(null);
        int cityScore = contains(match.getCity(), homeCity) ? 15 : 0;
        int fillScore = match.getMaxPlayers() == null || match.getMaxPlayers() == 0 ? 40 : (int) Math.round((match.getJoinedPlayers() == null ? 0 : match.getJoinedPlayers()) * 100.0 / match.getMaxPlayers());
        int timeScore = timeScore(match.getStartTime());
        return clamp((fillScore / 2) + timeScore + Math.min(candidatesCount * 4, 20) + cityScore);
    }

    private String reasonFor(User user, PublicMatchResponse match) {
        return "Recommended for " + match.getCity() + " at " + match.getStartTime() + " with " + Math.max((match.getMaxPlayers() == null ? 0 : match.getMaxPlayers()) - (match.getJoinedPlayers() == null ? 0 : match.getJoinedPlayers()), 0) + " spots left.";
    }

    private int skillScore(PlayerDiscoverResponse player, String requestedSkill) {
        if (requestedSkill == null || requestedSkill.isBlank()) return 70;
        return resolveSkill(player).equalsIgnoreCase(requestedSkill) ? 100 : 65;
    }

    private String resolveSkill(PlayerDiscoverResponse player) {
        double rating = player.getRating() == null ? 0 : player.getRating();
        int matches = player.getMatches() == null ? 0 : player.getMatches();
        if (rating >= 4.5 || matches >= 80) return "Advanced";
        if (rating >= 3.5 || matches >= 25) return "Intermediate";
        if (matches >= 5) return "Beginner";
        return "New";
    }

    private int timeScore(LocalTime time) {
        if (time == null) return 20;
        int hour = time.getHour();
        if (hour >= 18 && hour <= 22) return 35;
        if (hour >= 6 && hour <= 10) return 25;
        return 15;
    }

    private String trustSafeProfileCity(String username) {
        return userRepository.findByEmailOrPhone(username)
                .flatMap(user -> playerAvailabilityRepository.findByUserId(user.getId()))
                .map(availability -> availability.getHomeCity())
                .orElse(null);
    }

    private User findUser(String username) {
        return userRepository.findByEmailOrPhone(username)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
    }

    private String extractTag(String notes, String key) {
        if (notes == null) return "";
        return java.util.Arrays.stream(notes.split("\\R"))
                .filter(line -> line.startsWith(key + ":"))
                .map(line -> line.substring((key + ":").length()).trim())
                .findFirst()
                .orElse("");
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private boolean contains(String value, String probe) {
        return value != null && probe != null && value.toLowerCase().contains(probe.toLowerCase());
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
