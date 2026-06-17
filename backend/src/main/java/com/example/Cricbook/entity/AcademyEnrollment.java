package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "academy_enrollments", indexes = {
        @Index(name = "idx_academy_enrollment_batch", columnList = "batchId"),
        @Index(name = "idx_academy_enrollment_student", columnList = "studentEmail"),
        @Index(name = "idx_academy_enrollment_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademyEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long academyId;

    private Long programId;

    private Long batchId;

    @Column(nullable = false)
    private String studentName;

    @Column(nullable = false)
    private String studentEmail;

    private Integer studentAge;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "CONFIRMED";

    private BigDecimal amountPaid;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
