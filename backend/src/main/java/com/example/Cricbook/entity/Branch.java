package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "branches", indexes = {
        @Index(name = "idx_branch_org", columnList = "org_id"),
        @Index(name = "idx_branch_manager", columnList = "manager_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "turf_id", nullable = false)
    private Long turfId;

    @Column(name = "manager_id", nullable = false)
    private Long managerId; // User ID of the Branch Manager

    @Column(nullable = false)
    private String city;
}
