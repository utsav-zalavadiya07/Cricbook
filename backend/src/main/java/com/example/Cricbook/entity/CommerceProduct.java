package com.example.Cricbook.entity;

import com.example.Cricbook.enums.CommerceProductStatus;
import com.example.Cricbook.enums.CommerceProductType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "commerce_products", indexes = {
        @Index(name = "idx_commerce_product_status", columnList = "status"),
        @Index(name = "idx_commerce_product_sport", columnList = "sport"),
        @Index(name = "idx_commerce_product_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommerceProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private CommerceSeller seller;

    @Column(nullable = false)
    private String name;

    private String brand;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String sport;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    private BigDecimal mrp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommerceProductType productType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommerceProductStatus status;

    private String imageUrl;

    private String affiliateUrl;

    private BigDecimal rentalFeePerDay;

    private BigDecimal depositAmount;

    private Double rating;

    @Column(nullable = false)
    private Boolean featured;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
