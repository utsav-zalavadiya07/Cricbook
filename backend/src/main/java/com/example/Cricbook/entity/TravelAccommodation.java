package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "travel_accommodations", indexes = {
        @Index(name = "idx_travel_accommodation_city", columnList = "city"),
        @Index(name = "idx_travel_accommodation_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelAccommodation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    private TravelPartner partner;

    @Column(nullable = false)
    private String name;

    @Column(name = "accommodation_type", nullable = false)
    private String accommodationType;

    @Column(nullable = false)
    private String city;

    @Column(name = "room_type", nullable = false)
    private String roomType;

    @Column(name = "price_per_night", nullable = false)
    private BigDecimal pricePerNight;

    @Column(name = "total_rooms", nullable = false)
    private Integer totalRooms;

    @Column(name = "booked_rooms", nullable = false)
    private Integer bookedRooms;

    @Column(nullable = false)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
