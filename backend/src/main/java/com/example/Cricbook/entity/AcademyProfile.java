package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "academy_profiles", indexes = {
        @Index(name = "idx_academy_status", columnList = "status"),
        @Index(name = "idx_academy_city", columnList = "city")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_academy_name", columnNames = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademyProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 1000)
    private String sportsOffered;

    private String city;

    private String address;

    private String website;

    private String email;

    private String phone;

    private String logoUrl;

    private String documentUrl;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "PENDING_APPROVAL";

    private String accreditationLevel;

    @Column(columnDefinition = "TEXT")
    private String reviewNotes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
