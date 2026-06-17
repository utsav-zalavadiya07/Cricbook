package com.example.Cricbook.entity;

import com.example.Cricbook.enums.TravelPackageStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "travel_packages", indexes = {
        @Index(name = "idx_travel_package_status", columnList = "status"),
        @Index(name = "idx_travel_package_sport", columnList = "sport"),
        @Index(name = "idx_travel_package_destination", columnList = "destination")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_travel_package_code", columnNames = "package_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelPackage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "package_code", nullable = false)
    private String packageCode;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String sport;

    @Column(name = "origin_city")
    private String originCity;

    @Column(nullable = false)
    private String destination;

    @Column(name = "package_type", nullable = false)
    private String packageType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "cancellation_deadline")
    private LocalDate cancellationDeadline;

    @Column(name = "price_per_person", nullable = false)
    private BigDecimal pricePerPerson;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "booked_count", nullable = false)
    private Integer bookedCount;

    @Column(name = "includes_accommodation", nullable = false)
    private Boolean includesAccommodation;

    @Column(name = "includes_transport", nullable = false)
    private Boolean includesTransport;

    @Column(name = "linked_tournament_id")
    private Long linkedTournamentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    private TravelPartner partner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TravelPackageStatus status;

    @Column(columnDefinition = "TEXT")
    private String itinerary;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
