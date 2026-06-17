package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "enterprise_centers", uniqueConstraints = {
        @UniqueConstraint(name = "unique_enterprise_center_name", columnNames = {"business_id", "name", "city"})
}, indexes = {
        @Index(name = "idx_enterprise_center_business", columnList = "business_id"),
        @Index(name = "idx_enterprise_center_city", columnList = "city"),
        @Index(name = "idx_enterprise_center_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnterpriseCenter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private EnterpriseBusiness business;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String city;

    private String state;

    private Double latitude;

    private Double longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @Column(nullable = false)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
