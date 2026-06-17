package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "creator_contents", indexes = {
        @Index(name = "idx_creator_content_profile", columnList = "creator_profile_id, status"),
        @Index(name = "idx_creator_content_sport", columnList = "sport, visibility")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "creator_profile_id", nullable = false)
    private Long creatorProfileId;

    @Column(nullable = false)
    private String title;

    @Builder.Default
    @Column(nullable = false)
    private String contentType = "POST";

    @Builder.Default
    @Column(nullable = false)
    private String sport = "CRICKET";

    private String mediaUrl;

    @Builder.Default
    @Column(nullable = false)
    private String visibility = "PUBLIC";

    @Builder.Default
    @Column(nullable = false)
    private Boolean sponsored = false;

    @Builder.Default
    @Column(nullable = false)
    private Integer engagementCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private String status = "PUBLISHED";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
