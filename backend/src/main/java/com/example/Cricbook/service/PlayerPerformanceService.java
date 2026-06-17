package com.example.Cricbook.service;

import com.example.Cricbook.dto.PlayerPerformanceLeaderboardResponse;
import com.example.Cricbook.dto.PlayerPerformanceResponse;
import com.example.Cricbook.dto.PlayerProfileResponse;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.TeamMemberStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.PlayerMatchStatsRepository;
import com.example.Cricbook.repository.PlayerProfileRepository;
import com.example.Cricbook.repository.TeamMemberRepository;
import com.example.Cricbook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerPerformanceService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private final UserRepository userRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final PlayerMatchStatsRepository playerMatchStatsRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Transactional(readOnly = true)
    public PlayerPerformanceResponse myPerformance(String loginId) {
        User user = userRepository.findByEmailOrPhone(loginId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Please sign in again to continue."));
        return playerPerformance(user.getId());
    }

    @Transactional(readOnly = true)
    public PlayerPerformanceResponse playerPerformance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Player profile was not found."));
        PlayerProfile profile = playerProfileRepository.findByUserId(user.getId()).orElse(null);
        List<PlayerMatchStats> stats = playerMatchStatsRepository.findByUserIdWithScoreOrderByRecent(user.getId());
        return aggregate(user, profile, stats);
    }

    @Transactional(readOnly = true)
    public List<PlayerPerformanceLeaderboardResponse> leaderboard(String type, String city, Integer limit) {
        String resolvedType = normalizeLeaderboardType(type);
        String cityFilter = normalize(city);
        int resolvedLimit = Math.max(1, Math.min(limit == null ? DEFAULT_LIMIT : limit, MAX_LIMIT));

        Map<Long, List<PlayerMatchStats>> byUser = playerMatchStatsRepository.findAllWithUserAndScoreOrderByRecent().stream()
                .filter(stats -> matchesCity(stats.getUser().getId(), cityFilter))
                .collect(Collectors.groupingBy(stats -> stats.getUser().getId(), LinkedHashMap::new, Collectors.toList()));

        List<PlayerPerformanceLeaderboardResponse> entries = byUser.values().stream()
                .map(userStats -> leaderboardEntry(userStats.get(0).getUser(), userStats, resolvedType))
                .sorted(Comparator.comparing(PlayerPerformanceLeaderboardResponse::getScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PlayerPerformanceLeaderboardResponse::getPlayerName, String.CASE_INSENSITIVE_ORDER))
                .limit(resolvedLimit)
                .toList();

        for (int index = 0; index < entries.size(); index++) {
            entries.get(index).setRank(index + 1);
        }
        return entries;
    }

    private PlayerPerformanceResponse aggregate(User user, PlayerProfile profile, List<PlayerMatchStats> stats) {
        Set<Long> matchIds = stats.stream()
                .map(PlayerMatchStats::getLiveScore)
                .filter(Objects::nonNull)
                .map(LiveMatchScore::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        int matches = matchIds.size();
        int wins = (int) stats.stream().filter(this::isWin).count();
        int draws = (int) stats.stream().filter(this::isDraw).count();
        int losses = Math.max(0, matches - wins - draws);

        int battingInnings = (int) stats.stream().filter(item -> safe(item.getRunsScored()) > 0 || safe(item.getBallsFaced()) > 0).count();
        int runs = stats.stream().mapToInt(item -> safe(item.getRunsScored())).sum();
        int balls = stats.stream().mapToInt(item -> safe(item.getBallsFaced())).sum();
        int fours = stats.stream().mapToInt(item -> safe(item.getFours())).sum();
        int sixes = stats.stream().mapToInt(item -> safe(item.getSixes())).sum();
        int highestScore = stats.stream().mapToInt(item -> safe(item.getRunsScored())).max().orElse(0);
        int fifties = (int) stats.stream().filter(item -> safe(item.getRunsScored()) >= 50 && safe(item.getRunsScored()) < 100).count();
        int hundreds = (int) stats.stream().filter(item -> safe(item.getRunsScored()) >= 100).count();

        int bowlingBalls = stats.stream().mapToInt(item -> ballsFromOvers(item.getOversBowled())).sum();
        int wickets = stats.stream().mapToInt(item -> safe(item.getWicketsTaken())).sum();
        int runsConceded = stats.stream().mapToInt(item -> safe(item.getRunsConceded())).sum();
        int bowlingInnings = (int) stats.stream().filter(item -> ballsFromOvers(item.getOversBowled()) > 0 || safe(item.getWicketsTaken()) > 0).count();

        int catches = stats.stream().mapToInt(item -> safe(item.getCatches())).sum();
        int runouts = stats.stream().mapToInt(item -> safe(item.getRunouts())).sum();
        int impactScore = impactScore(matches, wins, runs, balls, wickets, bowlingBalls, catches, runouts);
        int reputationScore = reputationScore(profile, impactScore, matches);

        PlayerPerformanceResponse.BattingSummary batting = PlayerPerformanceResponse.BattingSummary.builder()
                .innings(battingInnings)
                .runs(runs)
                .balls(balls)
                .fours(fours)
                .sixes(sixes)
                .highestScore(highestScore)
                .average(round(battingInnings == 0 ? 0.0 : runs / (double) battingInnings))
                .strikeRate(round(balls == 0 ? 0.0 : runs * 100.0 / balls))
                .fifties(fifties)
                .hundreds(hundreds)
                .build();

        PlayerPerformanceResponse.BowlingSummary bowling = PlayerPerformanceResponse.BowlingSummary.builder()
                .innings(bowlingInnings)
                .wickets(wickets)
                .balls(bowlingBalls)
                .overs(formatBalls(bowlingBalls))
                .runsConceded(runsConceded)
                .average(round(wickets == 0 ? 0.0 : runsConceded / (double) wickets))
                .economyRate(round(bowlingBalls == 0 ? 0.0 : runsConceded * 6.0 / bowlingBalls))
                .strikeRate(round(wickets == 0 ? 0.0 : bowlingBalls / (double) wickets))
                .bestFigures(bestFigures(stats))
                .build();

        PlayerPerformanceResponse.CareerSummary career = PlayerPerformanceResponse.CareerSummary.builder()
                .matches(matches)
                .wins(wins)
                .losses(losses)
                .draws(draws)
                .mvpAwards(profile == null ? 0 : safe(profile.getMvpCount()))
                .reputationScore(reputationScore)
                .impactScore(impactScore)
                .consistencyScore(consistencyScore(stats))
                .build();

        PlayerPerformanceResponse.FieldingSummary fielding = PlayerPerformanceResponse.FieldingSummary.builder()
                .catches(catches)
                .runouts(runouts)
                .dismissals(catches + runouts)
                .build();

        return PlayerPerformanceResponse.builder()
                .profile(mapProfile(user, profile, matches, wins))
                .career(career)
                .batting(batting)
                .bowling(bowling)
                .fielding(fielding)
                .recentMatches(stats.stream().limit(8).map(this::mapMatch).toList())
                .achievements(achievements(matches, wins, batting, bowling, fielding))
                .timeline(timeline(stats, matches, batting, bowling))
                .insight(insight(matches, batting, bowling, impactScore))
                .build();
    }

    private PlayerPerformanceLeaderboardResponse leaderboardEntry(User user, List<PlayerMatchStats> stats, String type) {
        PlayerProfile profile = playerProfileRepository.findByUserId(user.getId()).orElse(null);
        PlayerPerformanceResponse aggregate = aggregate(user, profile, stats);
        double score = switch (type) {
            case "WICKETS" -> aggregate.getBowling().getWickets();
            case "MATCHES" -> aggregate.getCareer().getMatches();
            case "AVERAGE" -> aggregate.getBatting().getAverage();
            case "IMPACT" -> aggregate.getCareer().getImpactScore();
            default -> aggregate.getBatting().getRuns();
        };
        String label = switch (type) {
            case "WICKETS" -> aggregate.getBowling().getWickets() + " wickets";
            case "MATCHES" -> aggregate.getCareer().getMatches() + " matches";
            case "AVERAGE" -> aggregate.getBatting().getAverage() + " avg";
            case "IMPACT" -> aggregate.getCareer().getImpactScore() + " impact";
            default -> aggregate.getBatting().getRuns() + " runs";
        };
        return PlayerPerformanceLeaderboardResponse.builder()
                .userId(user.getId())
                .playerName(user.getName())
                .city(profile == null ? null : profile.getCity())
                .rank(0)
                .leaderboardType(type)
                .score(round(score))
                .statLabel(label)
                .matches(aggregate.getCareer().getMatches())
                .runs(aggregate.getBatting().getRuns())
                .wickets(aggregate.getBowling().getWickets())
                .impactScore(aggregate.getCareer().getImpactScore())
                .build();
    }

    private PlayerProfileResponse mapProfile(User user, PlayerProfile profile, int matches, int wins) {
        return PlayerProfileResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .photoUrl(profile == null ? null : profile.getPhotoUrl())
                .bio(profile == null ? null : profile.getBio())
                .city(profile == null ? null : profile.getCity())
                .preferredTime(profile == null ? null : profile.getPreferredTime())
                .battingStyle(profile == null ? null : profile.getBattingStyle())
                .bowlingStyle(profile == null ? null : profile.getBowlingStyle())
                .position(profile == null ? null : profile.getPosition())
                .experience(profile == null ? null : profile.getExperience())
                .matches(matches)
                .wins(wins)
                .mvp(profile == null ? 0 : safe(profile.getMvpCount()))
                .rating(profile == null ? 0.0 : round(profile.getRating()))
                .profileCompletionPercentage(profileCompletion(user, profile))
                .build();
    }

    private PlayerPerformanceResponse.MatchSummary mapMatch(PlayerMatchStats stats) {
        LiveMatchScore score = stats.getLiveScore();
        return PlayerPerformanceResponse.MatchSummary.builder()
                .liveScoreId(score == null ? null : score.getId())
                .title(score == null ? "Tracked match" : score.getTitle())
                .status(score == null ? null : score.getStatus())
                .result(isDraw(stats) ? "DRAW" : isWin(stats) ? "WIN" : score == null ? "TRACKED" : "COMPLETED")
                .runs(safe(stats.getRunsScored()))
                .balls(safe(stats.getBallsFaced()))
                .fours(safe(stats.getFours()))
                .sixes(safe(stats.getSixes()))
                .wickets(safe(stats.getWicketsTaken()))
                .overs(formatBalls(ballsFromOvers(stats.getOversBowled())))
                .runsConceded(safe(stats.getRunsConceded()))
                .catches(safe(stats.getCatches()))
                .runouts(safe(stats.getRunouts()))
                .playedAt(score == null ? firstNonNull(stats.getUpdatedAt(), stats.getCreatedAt()) : firstNonNull(score.getUpdatedAt(), score.getCreatedAt()))
                .build();
    }

    private List<PlayerPerformanceResponse.AchievementSummary> achievements(int matches,
                                                                            int wins,
                                                                            PlayerPerformanceResponse.BattingSummary batting,
                                                                            PlayerPerformanceResponse.BowlingSummary bowling,
                                                                            PlayerPerformanceResponse.FieldingSummary fielding) {
        return List.of(
                achievement("FIRST_MATCH", "First Match", "Complete your first scored match.", matches, 1),
                achievement("FIRST_WIN", "First Win", "Win a tracked team match.", wins, 1),
                achievement("FIRST_FIFTY", "First Fifty", "Score 50+ in a match.", batting.getFifties() + batting.getHundreds(), 1),
                achievement("FIRST_CENTURY", "First Century", "Score 100+ in a match.", batting.getHundreds(), 1),
                achievement("FIRST_WICKET", "First Wicket", "Take your first wicket.", bowling.getWickets(), 1),
                achievement("FIVE_WICKET_HAUL", "Five Wicket Haul", "Take five wickets across tracked matches.", bowling.getWickets(), 5),
                achievement("SAFE_HANDS", "Safe Hands", "Record five fielding dismissals.", fielding.getDismissals(), 5),
                achievement("ALL_ROUNDER", "All-Round Impact", "Reach 100 runs and 5 wickets.", Math.min(batting.getRuns(), bowling.getWickets() * 20), 100)
        );
    }

    private PlayerPerformanceResponse.AchievementSummary achievement(String code, String name, String description, int progress, int target) {
        int normalizedProgress = Math.max(0, Math.min(progress, target));
        return PlayerPerformanceResponse.AchievementSummary.builder()
                .code(code)
                .name(name)
                .description(description)
                .unlocked(progress >= target)
                .progress(normalizedProgress)
                .target(target)
                .build();
    }

    private List<PlayerPerformanceResponse.TimelineEvent> timeline(List<PlayerMatchStats> stats,
                                                                   int matches,
                                                                   PlayerPerformanceResponse.BattingSummary batting,
                                                                   PlayerPerformanceResponse.BowlingSummary bowling) {
        List<PlayerPerformanceResponse.TimelineEvent> events = new ArrayList<>();
        stats.stream().limit(8).forEach(item -> events.add(PlayerPerformanceResponse.TimelineEvent.builder()
                .type("MATCH")
                .title(item.getLiveScore() == null ? "Tracked match" : item.getLiveScore().getTitle())
                .description(safe(item.getRunsScored()) + " runs, " + safe(item.getWicketsTaken()) + " wickets")
                .liveScoreId(item.getLiveScore() == null ? null : item.getLiveScore().getId())
                .occurredAt(item.getLiveScore() == null ? firstNonNull(item.getUpdatedAt(), item.getCreatedAt()) : firstNonNull(item.getLiveScore().getUpdatedAt(), item.getLiveScore().getCreatedAt()))
                .build()));
        if (matches > 0) {
            events.add(milestone("CAREER_STARTED", "Career tracking started", "First completed score added to your profile.", stats));
        }
        if (batting.getHighestScore() >= 50) {
            events.add(milestone("BATTING_MILESTONE", "Batting milestone", "Highest score reached " + batting.getHighestScore() + ".", stats));
        }
        if (bowling.getWickets() > 0) {
            events.add(milestone("BOWLING_MILESTONE", "Bowling milestone", bowling.getWickets() + " wickets tracked.", stats));
        }
        return events.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(PlayerPerformanceResponse.TimelineEvent::getOccurredAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(12)
                .toList();
    }

    private PlayerPerformanceResponse.TimelineEvent milestone(String type, String title, String description, List<PlayerMatchStats> stats) {
        LocalDateTime occurredAt = stats.isEmpty() ? LocalDateTime.now() : firstNonNull(stats.get(stats.size() - 1).getCreatedAt(), LocalDateTime.now());
        return PlayerPerformanceResponse.TimelineEvent.builder()
                .type(type)
                .title(title)
                .description(description)
                .occurredAt(occurredAt)
                .build();
    }

    private boolean isWin(PlayerMatchStats stats) {
        LiveMatchScore score = stats.getLiveScore();
        if (score == null || stats.getUser() == null) return false;
        Team winner = null;
        if (score.getFixture() != null) {
            winner = score.getFixture().getWinner();
        } else if (score.getPublicMatch() != null) {
            winner = score.getPublicMatch().getWinnerTeam();
        }
        return winner != null && teamMemberRepository.existsByTeamIdAndUserIdAndStatus(winner.getId(), stats.getUser().getId(), TeamMemberStatus.ACTIVE);
    }

    private boolean isDraw(PlayerMatchStats stats) {
        return stats.getLiveScore() != null && Boolean.TRUE.equals(stats.getLiveScore().getIsTie());
    }

    private boolean matchesCity(Long userId, String cityFilter) {
        if (cityFilter == null) return true;
        return playerProfileRepository.findByUserId(userId)
                .map(PlayerProfile::getCity)
                .map(this::normalize)
                .map(cityFilter::equals)
                .orElse(false);
    }

    private String normalizeLeaderboardType(String type) {
        if (type == null || type.isBlank()) return "RUNS";
        String normalized = type.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "RUNS", "WICKETS", "MATCHES", "AVERAGE", "IMPACT" -> normalized;
            default -> "RUNS";
        };
    }

    private String bestFigures(List<PlayerMatchStats> stats) {
        return stats.stream()
                .max(Comparator.comparing((PlayerMatchStats item) -> safe(item.getWicketsTaken()))
                        .thenComparing(item -> -safe(item.getRunsConceded())))
                .map(item -> safe(item.getWicketsTaken()) + "/" + safe(item.getRunsConceded()))
                .orElse("0/0");
    }

    private int impactScore(int matches, int wins, int runs, int balls, int wickets, int bowlingBalls, int catches, int runouts) {
        if (matches == 0) return 0;
        double batting = Math.min(35, runs / (double) matches);
        double battingRate = balls == 0 ? 0 : Math.min(15, (runs * 100.0 / balls) / 10.0);
        double bowling = Math.min(30, wickets * 8.0);
        double fielding = Math.min(10, catches * 3.0 + runouts * 4.0);
        double winning = Math.min(10, wins * 4.0);
        double participation = Math.min(10, matches * 2.0);
        return bound((int) Math.round(batting + battingRate + bowling + fielding + winning + participation), 0, 100);
    }

    private int reputationScore(PlayerProfile profile, int impactScore, int matches) {
        double ratingScore = profile == null || profile.getRating() == null ? 60 : profile.getRating() * 20.0;
        return bound((int) Math.round((ratingScore * 0.6) + (impactScore * 0.35) + Math.min(5, matches)), 0, 100);
    }

    private int consistencyScore(List<PlayerMatchStats> stats) {
        if (stats.isEmpty()) return 0;
        int scoringMatches = (int) stats.stream().filter(item -> safe(item.getRunsScored()) > 0 || safe(item.getWicketsTaken()) > 0).count();
        return bound((int) Math.round(scoringMatches * 100.0 / stats.size()), 0, 100);
    }

    private String insight(int matches,
                           PlayerPerformanceResponse.BattingSummary batting,
                           PlayerPerformanceResponse.BowlingSummary bowling,
                           int impactScore) {
        if (matches == 0) {
            return "No completed scored matches yet. Complete a live score to start your cricket profile.";
        }
        if (batting.getRuns() >= 100 && bowling.getWickets() >= 5) {
            return "All-round profile is improving: batting volume and wicket impact are both visible.";
        }
        if (batting.getStrikeRate() >= 120) {
            return "Batting tempo is strong. Keep adding completed scorecards for reliable rankings.";
        }
        if (bowling.getWickets() >= 5) {
            return "Bowling impact is trending well. Economy and wicket rate will improve ranking accuracy.";
        }
        return "Impact score is " + impactScore + ". More completed scored matches will improve ranking confidence.";
    }

    private int profileCompletion(User user, PlayerProfile profile) {
        int score = 0;
        if (hasText(user.getName())) score += 20;
        if (hasText(user.getEmail())) score += 20;
        if (profile != null && hasText(profile.getPhotoUrl())) score += 20;
        if (profile != null && hasText(profile.getBio())) score += 20;
        if (profile != null && hasText(profile.getCity())) score += 20;
        return score;
    }

    private int ballsFromOvers(Double overs) {
        if (overs == null || overs <= 0) return 0;
        return (int) Math.round(overs * 6);
    }

    private String formatBalls(int balls) {
        return (balls / 6) + "." + (balls % 6);
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }

    private double round(Double value) {
        if (value == null) return 0.0;
        return Math.round(value * 100.0) / 100.0;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int bound(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) return value;
        }
        return null;
    }
}
