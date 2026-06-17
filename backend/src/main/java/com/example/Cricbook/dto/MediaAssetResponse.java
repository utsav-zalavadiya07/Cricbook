package com.example.Cricbook.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MediaAssetResponse {
    private Long id;
    private String ownerType;
    private Long ownerId;
    private String mediaType;
    private String imageUrl;
    private String thumbnailUrl;
    private String visibility;
    private String moderationStatus;
    private String moderationReason;
    private String contentType;
    private Long fileSizeBytes;
    private String uploadedAt;
}
