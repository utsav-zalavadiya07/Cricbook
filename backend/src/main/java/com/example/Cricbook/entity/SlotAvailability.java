package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "slot_availability", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"slot_id", "date"})
}, indexes = {
        @Index(name = "idx_sa_date_avail", columnList = "date, is_available"),
        @Index(name = "idx_sa_date_avail_blocked", columnList = "date, is_available, is_blocked")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private Slot slot;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable;

    @Column(name = "is_blocked", nullable = false)
    private Boolean isBlocked;
}
