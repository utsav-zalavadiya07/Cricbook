package com.example.Cricbook.entity;

import com.example.Cricbook.enums.TravelPartnerStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "travel_partners", indexes = {
        @Index(name = "idx_travel_partner_status", columnList = "status"),
        @Index(name = "idx_travel_partner_type", columnList = "partner_type"),
        @Index(name = "idx_travel_partner_city", columnList = "city")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelPartner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_name", nullable = false)
    private String partnerName;

    @Column(name = "partner_type", nullable = false)
    private String partnerType;

    @Column(nullable = false)
    private String email;

    private String phone;

    private String city;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TravelPartnerStatus status;

    @Column(name = "commission_percent", nullable = false)
    private BigDecimal commissionPercent;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
