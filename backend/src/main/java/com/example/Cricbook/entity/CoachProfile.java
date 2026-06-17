package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coach_profiles", indexes = {
        @Index(name = "idx_coach_status", columnList = "status"),
        @Index(name = "idx_coach_sport", columnList = "sport"),
        @Index(name = "idx_coach_city", columnList = "city")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_coach_email", columnNames = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoachProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long academyId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    private String phone;

    private String sport;

    private String city;

    private Integer experienceYears;

    private BigDecimal pricePerSession;

    @Column(columnDefinition = "TEXT")
    private String certifications;

    @Column(columnDefinition = "TEXT")
    private String achievements;

    private String profilePhotoUrl;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "PENDING_VERIFICATION";

    @Builder.Default
    private Double rating = 0.0;

    @Builder.Default
    private Integer studentsCoached = 0;

    @Column(columnDefinition = "TEXT")
    private String reviewNotes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
