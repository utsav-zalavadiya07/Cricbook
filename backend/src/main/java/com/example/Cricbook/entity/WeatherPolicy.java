package com.example.Cricbook.entity;

import com.example.Cricbook.enums.WeatherRefundType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "weather_policy", uniqueConstraints = {
        @UniqueConstraint(name = "uk_weather_policy_owner", columnNames = "owner_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "rain_refund_allowed", nullable = false)
    private Boolean rainRefundAllowed;

    @Column(name = "flexible_reschedule", nullable = false)
    private Boolean flexibleReschedule;

    @Column(name = "rain_threshold", nullable = false)
    private Integer rainThreshold;

    @Column(name = "heat_threshold_celsius", nullable = false)
    private Integer heatThresholdCelsius;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_type", nullable = false)
    private WeatherRefundType refundType;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
