package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "developer_marketplace_listings", indexes = {
        @Index(name = "idx_dev_marketplace_status", columnList = "status"),
        @Index(name = "idx_dev_marketplace_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeveloperMarketplaceListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "developer_id", nullable = false)
    private DeveloperAccount developer;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String website;

    private String pricingModel;

    private BigDecimal revenueSharePercent;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "PENDING_APPROVAL";

    @Column(length = 1000)
    private String reviewNotes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
