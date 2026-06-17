package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "facility_inspection_reports", indexes = {
        @Index(name = "idx_facility_inspection_facility", columnList = "facility_id"),
        @Index(name = "idx_facility_inspection_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacilityInspectionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long facilityId;

    @Column(nullable = false)
    private String inspectorName;

    @Builder.Default
    @Column(nullable = false)
    private String inspectionType = "ROUTINE";

    @Builder.Default
    private Integer score = 0;

    @Builder.Default
    @Column(nullable = false)
    private String safetyStatus = "PENDING";

    @Builder.Default
    @Column(nullable = false)
    private String maintenanceStatus = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    @Builder.Default
    @Column(nullable = false)
    private String status = "SCHEDULED";

    private LocalDateTime scheduledAt;

    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
