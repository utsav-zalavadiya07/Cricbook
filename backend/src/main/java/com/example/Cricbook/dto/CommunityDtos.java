package com.example.Cricbook.dto;

import com.example.Cricbook.enums.PlayerConnectionStatus;
import com.example.Cricbook.enums.PlayerConnectionType;
import com.example.Cricbook.enums.RecruitmentApplicationStatus;
import com.example.Cricbook.enums.TeamRecruitmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class CommunityDtos {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectionRequest {
        private Long userId;
        private PlayerConnectionType type;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectionDecisionRequest {
        private boolean accept;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerConnectionResponse {
        private Long id;
        private Long fromUserId;
        private String fromUserName;
        private Long toUserId;
        private String toUserName;
        private PlayerConnectionType type;
        private PlayerConnectionStatus status;
        private String message;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecruitmentRequest {
        private Long teamId;
        private String roleNeeded;
        private String skillLevel;
        private String city;
        private String availability;
        private String description;
        private LocalDate matchDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecruitmentApplyRequest {
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecruitmentApplicationResponse {
        private Long id;
        private Long recruitmentId;
        private Long userId;
        private String userName;
        private RecruitmentApplicationStatus status;
        private String message;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecruitmentResponse {
        private Long id;
        private Long teamId;
        private String teamName;
        private Long captainId;
        private String captainName;
        private String roleNeeded;
        private String skillLevel;
        private String city;
        private String availability;
        private String description;
        private LocalDate matchDate;
        private TeamRecruitmentStatus status;
        private Integer applicationCount;
        private List<RecruitmentApplicationResponse> applications;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommunityAnalyticsResponse {
        private Long discoverablePlayers;
        private Long acceptedConnections;
        private Long pendingConnections;
        private Long openRecruitments;
        private Long pendingApplications;
        private Long feedEvents;
        private Long activePosts;
        private Long comments;
        private Long moderationItems;
        private Integer engagementRate;
        private String insight;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedPostRequest {
        private String title;
        private String content;
        private String visibility;
        private Long teamId;
        private Long tournamentId;
        private Long matchId;
        private List<String> mediaUrls;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedReactionRequest {
        private String reactionType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedCommentRequest {
        private String message;
        private Long parentCommentId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedCommentResponse {
        private Long id;
        private Long feedEventId;
        private Long authorId;
        private String authorName;
        private Long parentCommentId;
        private String message;
        private String status;
        private LocalDateTime editedAt;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentReportRequest {
        private String contentType;
        private Long contentId;
        private String reason;
        private String details;
    }
}
