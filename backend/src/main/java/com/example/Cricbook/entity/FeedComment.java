package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "feed_comments", indexes = {
        @Index(name = "idx_feed_comment_event", columnList = "feed_event_id"),
        @Index(name = "idx_feed_comment_parent", columnList = "parent_comment_id"),
        @Index(name = "idx_feed_comment_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_event_id", nullable = false)
    private FeedEvent feedEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
