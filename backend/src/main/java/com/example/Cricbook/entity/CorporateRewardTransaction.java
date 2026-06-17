package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "corporate_reward_transactions", indexes = {
        @Index(name = "idx_corporate_reward_company", columnList = "companyId"),
        @Index(name = "idx_corporate_reward_employee", columnList = "employeeId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorporateRewardTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private String sourceType;

    @Builder.Default
    private Integer points = 0;

    @Builder.Default
    private Integer balanceAfter = 0;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "CREDITED";

    @Column(columnDefinition = "TEXT")
    private String summary;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
