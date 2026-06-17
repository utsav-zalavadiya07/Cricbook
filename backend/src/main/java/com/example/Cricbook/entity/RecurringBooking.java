package com.example.Cricbook.entity;

import com.example.Cricbook.enums.RecurringBookingFrequency;
import com.example.Cricbook.enums.RecurringBookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "recurring_booking", indexes = {
        @Index(name = "idx_recurring_user", columnList = "user_id"),
        @Index(name = "idx_recurring_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "team_id")
    private Long teamId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turf_id", nullable = false)
    private Turf turf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private Slot slot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurringBookingFrequency frequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_day", nullable = false)
    private DayOfWeek preferredDay;

    @Column(name = "next_booking_date", nullable = false)
    private LocalDate nextBookingDate;

    @Column(name = "auto_confirm", nullable = false)
    private Boolean autoConfirm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurringBookingStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
