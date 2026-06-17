package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sponsored_ad_events", indexes = {
        @Index(name = "idx_sponsored_ad_event_type", columnList = "event_type"),
        @Index(name = "idx_sponsored_ad_event_campaign", columnList = "campaign_id"),
        @Index(name = "idx_sponsored_ad_event_session", columnList = "session_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SponsoredAdEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private SponsoredCampaign campaign;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "user_reference")
    private String userReference;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "lead_reference")
    private String leadReference;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal cost = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
