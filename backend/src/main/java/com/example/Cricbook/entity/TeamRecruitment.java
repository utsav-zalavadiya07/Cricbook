package com.example.Cricbook.entity;

import com.example.Cricbook.enums.TeamRecruitmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "team_recruitment", indexes = {
        @Index(name = "idx_team_recruitment_status_city", columnList = "status, city"),
        @Index(name = "idx_team_recruitment_team", columnList = "team_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamRecruitment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "captain_id", nullable = false)
    private User captain;

    @Column(name = "role_needed", nullable = false)
    private String roleNeeded;

    private String skillLevel;
    private String city;
    private String availability;

    @Column(length = 1000)
    private String description;

    private LocalDate matchDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamRecruitmentStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
