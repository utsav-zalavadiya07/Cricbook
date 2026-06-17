package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "trust_adjustments", indexes = @Index(name = "idx_trust_adjustment_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrustAdjustment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "old_score", nullable = false)
    private Integer oldScore;

    @Column(name = "new_score", nullable = false)
    private Integer newScore;

    @Column(nullable = false, length = 800)
    private String reason;

    @Column(name = "admin_id", length = 160)
    private String adminId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
