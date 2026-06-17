package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "corporate_contracts", indexes = {
        @Index(name = "idx_corporate_contract_company", columnList = "companyId"),
        @Index(name = "idx_corporate_contract_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_corporate_contract_number", columnNames = "contractNumber")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorporateContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private String contractNumber;

    private String planName;

    @Builder.Default
    private BigDecimal annualValue = BigDecimal.ZERO;

    private LocalDate startDate;

    private LocalDate endDate;

    @Builder.Default
    private Integer renewalProbability = 0;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "DRAFT";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
