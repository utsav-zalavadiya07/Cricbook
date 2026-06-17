package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "staff_equipment_items", indexes = {
        @Index(name = "idx_staff_equipment_center", columnList = "center_id"),
        @Index(name = "idx_staff_equipment_type", columnList = "equipment_type"),
        @Index(name = "idx_staff_equipment_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffEquipmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id")
    private EnterpriseCenter center;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turf_id")
    private Turf turf;

    @Column(nullable = false)
    private String name;

    @Column(name = "equipment_type", nullable = false)
    private String equipmentType;

    @Column(nullable = false)
    private String status;

    @Column(name = "stock_count", nullable = false)
    @Builder.Default
    private Integer stockCount = 0;

    @Column(name = "in_use_count", nullable = false)
    @Builder.Default
    private Integer inUseCount = 0;

    @Column(name = "damaged_count", nullable = false)
    @Builder.Default
    private Integer damagedCount = 0;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "last_action_at")
    private LocalDateTime lastActionAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
