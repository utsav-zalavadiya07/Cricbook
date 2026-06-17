package com.example.Cricbook.entity;

import com.example.Cricbook.enums.WeatherRescheduleStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weather_reschedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherReschedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggested_slot_id", nullable = false)
    private Slot suggestedSlot;

    @Column(name = "suggested_date", nullable = false)
    private LocalDate suggestedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WeatherRescheduleStatus status;

    @Column(length = 700)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
