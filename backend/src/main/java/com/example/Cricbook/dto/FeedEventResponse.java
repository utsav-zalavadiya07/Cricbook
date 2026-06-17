package com.example.Cricbook.dto;

import com.example.Cricbook.enums.FeedEventType;
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
public class FeedEventResponse {
    private Long id;
    private Long actorId;
    private String actorName;
    private FeedEventType eventType;
    private String title;
    private String summary;
    private Long entityId;
    private String entityType;
    private String deepLink;
    private String visibility;
    private String status;
    private List<String> mediaUrls;
    private Integer reactionCount;
    private Integer commentCount;
    private Integer shareCount;
    private Integer viewCount;
    private Integer engagementScore;
    private String viewerReaction;
    private boolean ownPost;
    private LocalDateTime editedAt;
    private LocalDateTime createdAt;
}
