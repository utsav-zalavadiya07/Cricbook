package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "training_programs", indexes = {
        @Index(name = "idx_training_program_status", columnList = "status"),
        @Index(name = "idx_training_program_sport", columnList = "sport")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long academyId;

    private Long coachId;

    @Column(nullable = false)
    private String title;

    private String sport;

    private String level;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer durationWeeks;

    private Integer capacity;

    private BigDecimal price;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "DRAFT";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
