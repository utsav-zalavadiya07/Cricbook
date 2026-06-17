package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_prediction", indexes = {
        @Index(name = "idx_booking_prediction_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingPrediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predicted_turf_id")
    private Turf predictedTurf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predicted_slot_id")
    private Slot predictedSlot;

    @Column(name = "predicted_date")
    private LocalDate predictedDate;

    @Column(nullable = false)
    private Double confidence;

    @Column(length = 500)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
