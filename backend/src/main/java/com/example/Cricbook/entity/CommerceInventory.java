package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "commerce_inventory", indexes = {
        @Index(name = "idx_commerce_inventory_product", columnList = "product_id"),
        @Index(name = "idx_commerce_inventory_sku", columnList = "sku")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommerceInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private CommerceProduct product;

    @Column(nullable = false)
    private String variantLabel;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private Integer stockOnHand;

    @Column(nullable = false)
    private Integer reservedStock;

    @Column(nullable = false)
    private Integer lowStockThreshold;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
