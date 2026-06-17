package com.example.Cricbook.entity;

import com.example.Cricbook.enums.GamificationRewardStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "player_rewards", indexes = {
        @Index(name = "idx_player_rewards_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerReward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "reward_type", nullable = false)
    private String rewardType;

    @Column(nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GamificationRewardStatus status = GamificationRewardStatus.AVAILABLE;

    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
