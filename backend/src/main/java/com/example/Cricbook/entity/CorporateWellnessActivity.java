package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "corporate_wellness_activities", indexes = {
        @Index(name = "idx_corporate_activity_program", columnList = "programId"),
        @Index(name = "idx_corporate_activity_employee", columnList = "employeeId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorporateWellnessActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long programId;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private String activityType;

    @Column(nullable = false)
    private String attendanceStatus;

    @Builder.Default
    private Integer sportsHours = 0;

    @Builder.Default
    private Boolean completed = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
