package com.example.Cricbook.entity;

import com.example.Cricbook.enums.TravelBookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "travel_bookings", indexes = {
        @Index(name = "idx_travel_booking_user", columnList = "user_id"),
        @Index(name = "idx_travel_booking_package", columnList = "package_id"),
        @Index(name = "idx_travel_booking_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_travel_booking_ref", columnNames = "booking_reference")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private TravelPackage travelPackage;

    @Column(name = "booking_reference", nullable = false)
    private String bookingReference;

    @Column(name = "booking_type", nullable = false)
    private String bookingType;

    @Column(name = "participant_count", nullable = false)
    private Integer participantCount;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal discount;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "refund_amount", nullable = false)
    private BigDecimal refundAmount;

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus;

    private String couponCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TravelBookingStatus status;

    @Column(columnDefinition = "TEXT")
    private String travelerNames;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
