package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sports_volunteer_assignments", indexes = {
        @Index(name = "idx_sports_volunteer_assignment_volunteer", columnList = "volunteer_id"),
        @Index(name = "idx_sports_volunteer_assignment_type", columnList = "assignment_type, assignment_ref_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SportsVolunteerAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long volunteerId;

    @Column(nullable = false)
    private String assignmentType;

    private Long assignmentRefId;

    @Column(nullable = false)
    private String title;

    private String districtName;

    @Builder.Default
    @Column(nullable = false)
    private String attendanceStatus = "ASSIGNED";

    @CreationTimestamp
    @Column(name = "assigned_at", updatable = false)
    private LocalDateTime assignedAt;
}
