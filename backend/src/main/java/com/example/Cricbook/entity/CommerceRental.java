package com.example.Cricbook.entity;

import com.example.Cricbook.enums.CommerceRentalStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "commerce_rentals", indexes = {
        @Index(name = "idx_commerce_rental_user", columnList = "user_id"),
        @Index(name = "idx_commerce_rental_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommerceRental {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false, unique = true)
    private CommerceOrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private CommerceProduct product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate rentalStart;

    @Column(nullable = false)
    private LocalDate rentalEnd;

    @Column(nullable = false)
    private BigDecimal depositAmount;

    @Column(nullable = false)
    private BigDecimal lateFee;

    @Column(nullable = false)
    private BigDecimal damageFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommerceRentalStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
