package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "corporate_wellness_programs", indexes = {
        @Index(name = "idx_corporate_wellness_company", columnList = "companyId"),
        @Index(name = "idx_corporate_wellness_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorporateWellnessProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private String title;

    private String sport;

    @Column(nullable = false)
    private String programType;

    private LocalDate startDate;

    private LocalDate endDate;

    @Builder.Default
    private Integer enrolledEmployees = 0;

    @Builder.Default
    private Integer completedEmployees = 0;

    @Builder.Default
    private Integer rewardPoints = 0;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "DRAFT";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
