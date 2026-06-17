package com.example.Cricbook.entity;

import com.example.Cricbook.enums.LeaderboardType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "leaderboard_cache", indexes = {
        @Index(name = "idx_leaderboard_type_city", columnList = "leaderboard_type, city")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardCache {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "leaderboard_type", nullable = false)
    private LeaderboardType leaderboardType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "leaderboard_rank", nullable = false)
    private Integer rank;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false)
    @Builder.Default
    private String city = "Global";

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
