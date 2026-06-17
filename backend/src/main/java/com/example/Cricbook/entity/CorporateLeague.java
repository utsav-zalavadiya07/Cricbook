package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "corporate_leagues", indexes = {
        @Index(name = "idx_corporate_league_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorporateLeague {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String sport;

    private Long organizerCompanyId;

    @Column(length = 1000)
    private String invitedCompanyIds;

    @Builder.Default
    private Integer registeredCompanies = 0;

    @Builder.Default
    private Integer teamCount = 0;

    @Builder.Default
    private Boolean fixturesGenerated = false;

    private String rankingStatus;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "DRAFT";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
