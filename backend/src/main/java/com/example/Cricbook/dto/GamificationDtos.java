package com.example.Cricbook.dto;

import com.example.Cricbook.enums.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class GamificationDtos {
    private GamificationDtos() {
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AwardXpRequest {
        private GamificationActivityType activityType;
        private Integer points;
        private String reason;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PlayerXpResponse {
        private Long userId;
        private String playerName;
        private Integer xp;
        private Integer level;
        private Integer nextLevelXp;
        private Integer progressPercent;
        private String title;
        private String lastActivity;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StreakResponse {
        private StreakType streakType;
        private Integer currentStreak;
        private Integer bestStreak;
        private LocalDate lastActivityDate;
        private String label;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AchievementResponse {
        private Long id;
        private String name;
        private AchievementCategory category;
        private GamificationActivityType triggerType;
        private Integer threshold;
        private Integer xpReward;
        private Boolean earned;
        private LocalDateTime earnedAt;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BadgeResponse {
        private Long id;
        private String badge;
        private LocalDateTime earnedAt;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ChallengeResponse {
        private Long id;
        private String title;
        private String description;
        private GamificationActivityType activityType;
        private Integer target;
        private Integer progress;
        private Integer xpReward;
        private String rewardLabel;
        private ChallengeStatus status;
        private LocalDateTime expiresAt;
        private Integer progressPercent;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RewardResponse {
        private Long id;
        private String rewardType;
        private String label;
        private GamificationRewardStatus status;
        private LocalDateTime unlockedAt;
        private LocalDateTime claimedAt;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LeaderboardEntryResponse {
        private Long userId;
        private String playerName;
        private Integer rank;
        private Integer score;
        private Integer level;
        private String city;
        private LeaderboardType leaderboardType;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SeasonalEventResponse {
        private Long id;
        private String name;
        private String description;
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TeamCompetitionResponse {
        private Long teamId;
        private String teamName;
        private Integer points;
        private Integer rank;
        private String seasonName;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class GamificationProfileResponse {
        private PlayerXpResponse xp;
        private List<StreakResponse> streaks;
        private List<AchievementResponse> achievements;
        private List<BadgeResponse> badges;
        private List<ChallengeResponse> challenges;
        private List<RewardResponse> rewards;
        private List<LeaderboardEntryResponse> leaderboard;
        private List<SeasonalEventResponse> seasonalEvents;
        private String insight;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class GamificationAnalyticsResponse {
        private long activePlayers;
        private long completedChallenges;
        private long availableRewards;
        private double challengeCompletionRate;
        private List<LeaderboardEntryResponse> topPlayers;
        private List<TeamCompetitionResponse> teamCompetitions;
        private String insight;
    }
}
