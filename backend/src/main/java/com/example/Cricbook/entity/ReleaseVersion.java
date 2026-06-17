package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "release_versions", indexes = @Index(name = "idx_release_version_status", columnList = "status"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleaseVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String version;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(nullable = false, length = 3000)
    private String notes;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "rollout_percentage", nullable = false)
    @Builder.Default
    private Integer rolloutPercentage = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
