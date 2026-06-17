package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "career_highlight_videos", indexes = {
        @Index(name = "idx_career_video_profile", columnList = "profile_id"),
        @Index(name = "idx_career_video_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerHighlightVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private CareerProfile profile;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String videoUrl;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "PROCESSED";

    @Column(nullable = false)
    @Builder.Default
    private Long bookmarks = 0L;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
