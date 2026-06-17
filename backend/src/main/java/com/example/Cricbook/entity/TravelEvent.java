package com.example.Cricbook.entity;

import com.example.Cricbook.enums.TravelEventStatus;
import com.example.Cricbook.enums.TravelEventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "travel_events", indexes = {
        @Index(name = "idx_travel_event_status", columnList = "status"),
        @Index(name = "idx_travel_event_type", columnList = "event_type"),
        @Index(name = "idx_travel_event_city", columnList = "city")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private TravelEventType eventType;

    @Column(nullable = false)
    private String sport;

    @Column(nullable = false)
    private String city;

    private String venue;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "registered_count", nullable = false)
    private Integer registeredCount;

    @Column(nullable = false)
    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TravelEventStatus status;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
