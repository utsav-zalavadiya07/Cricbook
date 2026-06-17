package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "developer_invoices", indexes = {
        @Index(name = "idx_dev_invoice_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeveloperInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "developer_id", nullable = false)
    private DeveloperAccount developer;

    @Column(nullable = false)
    private String invoiceNumber;

    @Column(nullable = false, length = 40)
    private String plan;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal taxAmount;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    private Long usageCount;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "PENDING";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
