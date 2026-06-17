package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "corporate_companies", indexes = {
        @Index(name = "idx_corporate_company_status", columnList = "status"),
        @Index(name = "idx_corporate_company_city", columnList = "city")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_corporate_company_name", columnNames = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorporateCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String gstNumber;

    private String industry;

    private String city;

    @Column(length = 1000)
    private String locations;

    private String hrContactName;

    private String hrEmail;

    private String hrPhone;

    private String adminEmail;

    private String logoUrl;

    private String documentUrl;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "PENDING_APPROVAL";

    @Builder.Default
    private BigDecimal annualContractValue = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal monthlyBudget = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String reviewNotes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
