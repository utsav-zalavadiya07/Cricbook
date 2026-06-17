package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "federation_disciplinary_cases", indexes = {
        @Index(name = "idx_federation_case_body", columnList = "federationId"),
        @Index(name = "idx_federation_case_subject", columnList = "subjectType,subjectId"),
        @Index(name = "idx_federation_case_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FederationDisciplinaryCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long federationId;

    @Column(nullable = false, length = 40)
    private String subjectType;

    @Column(nullable = false)
    private Long subjectId;

    @Column(nullable = false)
    private String caseType;

    @Column(nullable = false, length = 40)
    private String severity;

    private String investigator;

    @Column(columnDefinition = "TEXT")
    private String evidenceSummary;

    @Column(columnDefinition = "TEXT")
    private String resolutionNotes;

    private LocalDate penaltyUntil;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String penalty = "NONE";

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "OPEN";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
