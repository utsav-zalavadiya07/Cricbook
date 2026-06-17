package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "corporate_invoices", indexes = {
        @Index(name = "idx_corporate_invoice_company", columnList = "companyId"),
        @Index(name = "idx_corporate_invoice_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_corporate_invoice_number", columnNames = "invoiceNumber")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorporateInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private String invoiceNumber;

    @Column(nullable = false)
    private String billingPeriod;

    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal gstAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    private LocalDate dueDate;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "GENERATED";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
