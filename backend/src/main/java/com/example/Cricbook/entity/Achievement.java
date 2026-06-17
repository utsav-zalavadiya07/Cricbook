package com.example.Cricbook.entity;

import com.example.Cricbook.enums.AchievementCategory;
import com.example.Cricbook.enums.GamificationActivityType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "achievements", indexes = {
        @Index(name = "idx_achievement_trigger", columnList = "trigger_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Achievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AchievementCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private GamificationActivityType triggerType;

    @Column(nullable = false)
    private Integer threshold;

    @Column(name = "xp_reward", nullable = false)
    private Integer xpReward;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
