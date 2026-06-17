package com.example.Cricbook.dto;

import com.example.Cricbook.enums.LiveMatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerPerformanceResponse {
    private PlayerProfileResponse profile;
    private CareerSummary career;
    private BattingSummary batting;
    private BowlingSummary bowling;
    private FieldingSummary fielding;
    private List<MatchSummary> recentMatches;
    private List<AchievementSummary> achievements;
    private List<TimelineEvent> timeline;
    private String insight;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CareerSummary {
        private Integer matches;
        private Integer wins;
        private Integer losses;
        private Integer draws;
        private Integer mvpAwards;
        private Integer reputationScore;
        private Integer impactScore;
        private Integer consistencyScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BattingSummary {
        private Integer innings;
        private Integer runs;
        private Integer balls;
        private Integer fours;
        private Integer sixes;
        private Integer highestScore;
        private Double average;
        private Double strikeRate;
        private Integer fifties;
        private Integer hundreds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BowlingSummary {
        private Integer innings;
        private Integer wickets;
        private Integer balls;
        private String overs;
        private Integer runsConceded;
        private Double average;
        private Double economyRate;
        private Double strikeRate;
        private String bestFigures;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldingSummary {
        private Integer catches;
        private Integer runouts;
        private Integer dismissals;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchSummary {
        private Long liveScoreId;
        private String title;
        private LiveMatchStatus status;
        private String result;
        private Integer runs;
        private Integer balls;
        private Integer fours;
        private Integer sixes;
        private Integer wickets;
        private String overs;
        private Integer runsConceded;
        private Integer catches;
        private Integer runouts;
        private LocalDateTime playedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AchievementSummary {
        private String code;
        private String name;
        private String description;
        private Boolean unlocked;
        private Integer progress;
        private Integer target;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineEvent {
        private String type;
        private String title;
        private String description;
        private Long liveScoreId;
        private LocalDateTime occurredAt;
    }
}
