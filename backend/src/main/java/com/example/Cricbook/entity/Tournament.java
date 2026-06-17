package com.example.Cricbook.entity;

import com.example.Cricbook.enums.TournamentFormat;
import com.example.Cricbook.enums.TournamentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tournaments", indexes = {
        @Index(name = "idx_tournament_city", columnList = "city"),
        @Index(name = "idx_tournament_status", columnList = "status"),
        @Index(name = "idx_tournament_start", columnList = "start_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turf_id")
    private Turf turf;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String sport;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String venue;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "banner_url")
    private String bannerUrl;

    @Column(columnDefinition = "TEXT")
    private String rules;

    @Column(name = "requires_approval", nullable = false)
    private Boolean requiresApproval;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentFormat format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentStatus status;

    @Column(name = "max_teams", nullable = false)
    private Integer maxTeams;

    @Column(name = "entry_fee", nullable = false)
    private BigDecimal entryFee;

    @Column(name = "prize_pool")
    private BigDecimal prizePool;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
