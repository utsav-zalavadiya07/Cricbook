package com.example.Cricbook.entity;

import com.example.Cricbook.enums.CommerceSellerStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "commerce_sellers", indexes = {
        @Index(name = "idx_commerce_seller_status", columnList = "status"),
        @Index(name = "idx_commerce_seller_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommerceSeller {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false)
    private String contactName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    private String gstNumber;

    private String city;

    private String documentUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommerceSellerStatus status;

    @Column(nullable = false)
    private Integer trustScore;

    @Column(columnDefinition = "TEXT")
    private String verificationNotes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
