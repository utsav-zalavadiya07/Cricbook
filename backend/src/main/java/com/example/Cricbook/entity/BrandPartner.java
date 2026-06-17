package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "brand_partners", indexes = {
        @Index(name = "idx_brand_partner_status", columnList = "status"),
        @Index(name = "idx_brand_partner_type", columnList = "brand_type"),
        @Index(name = "idx_brand_partner_city", columnList = "city")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_brand_partner_email", columnNames = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandPartner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "brand_type", nullable = false, length = 60)
    @Builder.Default
    private String brandType = "LOCAL_BUSINESS";

    @Column(length = 15)
    private String gstNumber;

    private String website;

    @Column(nullable = false)
    private String email;

    @Column(length = 30)
    private String phone;

    private String city;

    private String logoUrl;

    private String documentUrl;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "PENDING_VERIFICATION";

    @Column(length = 1000)
    private String verificationNotes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
