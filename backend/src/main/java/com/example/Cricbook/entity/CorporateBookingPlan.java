package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "corporate_booking_plans", indexes = {
        @Index(name = "idx_corporate_booking_company", columnList = "companyId"),
        @Index(name = "idx_corporate_booking_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorporateBookingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    private Long departmentId;

    @Column(nullable = false)
    private String sport;

    private String turfName;

    private String city;

    @Column(nullable = false)
    private String recurrenceType;

    private LocalDate startDate;

    private LocalDate endDate;

    @Builder.Default
    private Integer generatedBookings = 0;

    @Builder.Default
    private BigDecimal monthlySpend = BigDecimal.ZERO;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(columnDefinition = "TEXT")
    private String conflictNotes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
