package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sports_os_facilities", indexes = {
        @Index(name = "idx_sports_os_facility_code", columnList = "facility_code", unique = true),
        @Index(name = "idx_sports_os_facility_status", columnList = "status"),
        @Index(name = "idx_sports_os_facility_geo", columnList = "state_name, district_name, city_name"),
        @Index(name = "idx_sports_os_facility_type", columnList = "facility_type, ownership_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SportsOsFacility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "facility_code", nullable = false, unique = true, length = 48)
    private String facilityCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String facilityType;

    @Column(nullable = false)
    private String ownershipType;

    @Column(nullable = false)
    private String jurisdictionLevel;

    @Column(nullable = false)
    private String stateName;

    @Column(nullable = false)
    private String districtName;

    private String cityName;

    @Column(precision = 10, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 6)
    private BigDecimal longitude;

    @Builder.Default
    private Integer capacity = 0;

    @Builder.Default
    private Integer activeSlots = 0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean publicBookingEnabled = false;

    @Builder.Default
    @Column(nullable = false)
    private String status = "ACTIVE";

    @Builder.Default
    @Column(nullable = false)
    private String maintenanceStatus = "OPERATIONAL";

    @Builder.Default
    @Column(precision = 5, scale = 2)
    private BigDecimal utilizationPercent = BigDecimal.ZERO;

    private String ownerDepartment;

    @Column(columnDefinition = "TEXT")
    private String images;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
