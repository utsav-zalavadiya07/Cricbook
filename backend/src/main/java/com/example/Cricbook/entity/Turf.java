package com.example.Cricbook.entity;

import com.example.Cricbook.enums.BoxType;
import com.example.Cricbook.enums.PitchType;
import com.example.Cricbook.enums.TurfStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "turfs", indexes = {
    @Index(name = "idx_turf_city", columnList = "city"),
    @Index(name = "idx_turf_price", columnList = "price_per_hour"),
    @Index(name = "idx_turf_rating", columnList = "rating"),
    @Index(name = "idx_turf_status_city", columnList = "status, city"),
    @Index(name = "idx_turf_status_price", columnList = "status, price_per_hour"),
    @Index(name = "idx_turf_status_rating", columnList = "status, rating")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Turf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String city;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "timezone", length = 100)
    private String timezone;

    @Column(name = "holiday_source", length = 255)
    private String holidaySource;

    @Column(name = "capacity")
    @Builder.Default
    private Integer capacity = 10;

    private Double latitude;

    private Double longitude;

    @Column(name = "price_per_hour", nullable = false)
    private BigDecimal pricePerHour;

    @Enumerated(EnumType.STRING)
    @Column(name = "pitch_type", nullable = false, length = 32)
    private PitchType pitchType;

    @Enumerated(EnumType.STRING)
    @Column(name = "box_type", nullable = false, length = 32)
    private BoxType boxType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TurfStatus status;

    @Builder.Default
    @OneToMany(mappedBy = "turf", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TurfImage> images = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "turf", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TurfAmenity> amenities = new ArrayList<>();

    @Column(name = "rating", columnDefinition = "DECIMAL(2,1) DEFAULT 0")
    @Builder.Default
    private Double rating = 0.0;

    @Column(name = "total_reviews", columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer totalReviews = 0;

    @Column(name = "featured_until")
    private LocalDateTime featuredUntil;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
