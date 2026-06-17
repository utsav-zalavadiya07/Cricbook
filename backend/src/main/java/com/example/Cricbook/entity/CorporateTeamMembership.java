package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "corporate_team_memberships", indexes = {
        @Index(name = "idx_corporate_membership_team", columnList = "teamId"),
        @Index(name = "idx_corporate_membership_employee", columnList = "employeeId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_corporate_team_employee", columnNames = {"teamId", "employeeId"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorporateTeamMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long teamId;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String role = "MEMBER";

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
