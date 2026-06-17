package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_analytics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", unique = true)
    private Long campaignId;

    @Column(nullable = false)
    @Builder.Default
    private Long views = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long clicks = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long sends = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long conversions = 0L;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
