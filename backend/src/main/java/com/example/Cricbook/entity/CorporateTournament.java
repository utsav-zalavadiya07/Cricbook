package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "corporate_tournaments", indexes = {
        @Index(name = "idx_corporate_tournament_company", columnList = "companyId"),
        @Index(name = "idx_corporate_tournament_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorporateTournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String sport;

    @Column(nullable = false)
    private String scope;

    private LocalDate startDate;

    private LocalDate endDate;

    @Builder.Default
    private Integer teamCount = 0;

    @Builder.Default
    private Boolean fixturesGenerated = false;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "DRAFT";

    private String rankingStatus;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
