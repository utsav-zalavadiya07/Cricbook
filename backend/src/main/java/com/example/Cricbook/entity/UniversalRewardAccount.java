package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "universal_reward_accounts", indexes = {
        @Index(name = "idx_universal_reward_identity", columnList = "identity_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UniversalRewardAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identity_id", nullable = false, unique = true)
    private Long identityId;

    @Builder.Default
    @Column(nullable = false)
    private Integer pointsBalance = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer lifetimeEarned = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer lifetimeRedeemed = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
