package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "media_assets", indexes = {
        @Index(name = "idx_media_owner", columnList = "owner_type, owner_id"),
        @Index(name = "idx_media_status", columnList = "moderation_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_type", nullable = false, length = 32)
    private String ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "media_type", nullable = false, length = 32)
    private String mediaType;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "thumbnail_url", nullable = false)
    private String thumbnailUrl;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String visibility = "PUBLIC";

    @Column(name = "moderation_status", nullable = false, length = 32)
    @Builder.Default
    private String moderationStatus = "APPROVED";

    @Column(name = "moderation_reason")
    private String moderationReason;

    @Column(name = "content_type", nullable = false, length = 80)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
