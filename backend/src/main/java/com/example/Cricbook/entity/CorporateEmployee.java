package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "corporate_employees", indexes = {
        @Index(name = "idx_corporate_employee_company", columnList = "companyId"),
        @Index(name = "idx_corporate_employee_email", columnList = "email")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_corporate_employee_company_email", columnNames = {"companyId", "email"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorporateEmployee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    private Long locationId;

    private Long departmentId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    private String phone;

    private String employeeCode;

    private String role;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "ACTIVE";

    @Builder.Default
    private Integer wellnessScore = 0;

    @Builder.Default
    private Integer participationScore = 0;

    @Builder.Default
    private Integer rewardBalance = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
