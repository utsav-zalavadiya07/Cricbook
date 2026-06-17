package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "academy_talent_audits", indexes = {
        @Index(name = "idx_academy_talent_audit_action", columnList = "action")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademyTalentAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long academyId;

    private Long coachId;

    private Long enrollmentId;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String actor;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
