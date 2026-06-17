package com.example.Cricbook.entity;

import com.example.Cricbook.enums.StreakType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "player_streak", uniqueConstraints = {
        @UniqueConstraint(name = "uk_player_streak_user_type", columnNames = {"user_id", "streak_type"})
}, indexes = {
        @Index(name = "idx_player_streak_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerStreak {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "streak_type", nullable = false)
    private StreakType streakType;

    @Column(name = "current_streak", nullable = false)
    @Builder.Default
    private Integer currentStreak = 0;

    @Column(name = "best_streak", nullable = false)
    @Builder.Default
    private Integer bestStreak = 0;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
