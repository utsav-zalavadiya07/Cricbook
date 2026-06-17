package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_billing_invoices", indexes = {
        @Index(name = "idx_tenant_billing_invoice_number", columnList = "invoice_number", unique = true),
        @Index(name = "idx_tenant_billing_invoice_tenant", columnList = "tenant_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantBillingInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 64)
    private String invoiceNumber;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    private Long subscriptionId;

    @Builder.Default
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private String currency = "INR";

    @Column(nullable = false)
    private LocalDate dueDate;

    @Builder.Default
    @Column(nullable = false)
    private String status = "OPEN";

    private String paymentReference;

    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
