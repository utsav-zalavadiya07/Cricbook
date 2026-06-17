package com.example.Cricbook.entity;

import com.example.Cricbook.enums.TeamAvailabilityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "team_availability", uniqueConstraints = {
        @UniqueConstraint(name = "unique_team_availability", columnNames = {"team_id", "user_id", "available_date"})
}, indexes = @Index(name = "idx_team_availability_team_date", columnList = "team_id, available_date"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamAvailability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "available_date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamAvailabilityStatus status;

    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
