package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sports_volunteer_profiles", indexes = {
        @Index(name = "idx_sports_volunteer_code", columnList = "volunteer_code", unique = true),
        @Index(name = "idx_sports_volunteer_email", columnList = "email", unique = true),
        @Index(name = "idx_sports_volunteer_geo", columnList = "state_name, district_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SportsVolunteerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "volunteer_code", nullable = false, unique = true, length = 48)
    private String volunteerCode;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Column(nullable = false)
    private String stateName;

    @Column(nullable = false)
    private String districtName;

    @Builder.Default
    @Column(nullable = false)
    private String category = "EVENT";

    @Builder.Default
    @Column(nullable = false)
    private String verificationStatus = "PENDING";

    @Builder.Default
    private Integer assignmentsCompleted = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
