package com.example.Cricbook.entity;

import com.example.Cricbook.enums.CommerceOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "commerce_orders", indexes = {
        @Index(name = "idx_commerce_order_user", columnList = "user_id"),
        @Index(name = "idx_commerce_order_status", columnList = "status"),
        @Index(name = "idx_commerce_order_number", columnList = "order_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommerceOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommerceOrderStatus status;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(nullable = false)
    private BigDecimal discount;

    @Column(nullable = false)
    private Integer loyaltyPointsRedeemed;

    @Column(nullable = false)
    private BigDecimal shippingFee;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(nullable = false)
    private String paymentStatus;

    private String couponCode;

    @Column(columnDefinition = "TEXT")
    private String shippingAddress;

    private String shippingProvider;

    private String trackingNumber;

    @Column(columnDefinition = "TEXT")
    private String auditNote;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
