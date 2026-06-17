package com.example.Cricbook.entity;

import com.example.Cricbook.enums.TurfEnvironmentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "turf_environment", uniqueConstraints = {
        @UniqueConstraint(name = "uk_turf_environment_turf", columnNames = "turf_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurfEnvironment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turf_id", nullable = false)
    private Turf turf;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TurfEnvironmentType type;

    @Column(name = "rain_protection", nullable = false)
    private Boolean rainProtection;

    @Column(name = "heat_protection", nullable = false)
    private Boolean heatProtection;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
