package com.example.Cricbook.entity;

import com.example.Cricbook.enums.TournamentRegistrationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tournament_registrations", uniqueConstraints = {
        @UniqueConstraint(name = "unique_tournament_team", columnNames = {"tournament_id", "team_id"})
}, indexes = {
        @Index(name = "idx_tournament_registration_team", columnList = "team_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by", nullable = false)
    private User registeredBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentRegistrationStatus status;

    @Column(nullable = false)
    private Boolean paid;

    @Column(length = 500)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
