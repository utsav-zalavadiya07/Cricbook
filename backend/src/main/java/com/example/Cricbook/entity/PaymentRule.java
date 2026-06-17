package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_rules", indexes = @Index(name = "idx_payment_rules_owner", columnList = "owner_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false, unique = true)
    private User owner;

    @Column(name = "minimum_percentage", nullable = false)
    private Integer minimumPercentage;

    @Column(name = "auto_cancel", nullable = false)
    private Boolean autoCancel;

    @Column(name = "full_payment_required", nullable = false)
    private Boolean fullPaymentRequired;

    @Column(name = "reminder_hours_before", nullable = false)
    private Integer reminderHoursBefore;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
