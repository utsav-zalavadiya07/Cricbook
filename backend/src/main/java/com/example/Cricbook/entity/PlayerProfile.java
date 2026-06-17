package com.example.Cricbook.entity;

import com.example.Cricbook.enums.BattingStyle;
import com.example.Cricbook.enums.BowlingStyle;
import com.example.Cricbook.enums.PlayerPosition;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "player_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String city;

    @Column(name = "preferred_time")
    private String preferredTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "batting_style")
    private BattingStyle battingStyle;

    @Enumerated(EnumType.STRING)
    @Column(name = "bowling_style")
    private BowlingStyle bowlingStyle;

    @Enumerated(EnumType.STRING)
    private PlayerPosition position;

    private String experience;

    @Column(name = "matches_played", nullable = false)
    private Integer matchesPlayed;

    @Column(nullable = false)
    private Integer wins;

    @Column(name = "mvp_count", nullable = false)
    private Integer mvpCount;

    @Column(nullable = false)
    private Double rating;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
