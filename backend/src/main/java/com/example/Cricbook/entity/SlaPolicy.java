package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sla_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlaPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String priority;

    @Column(name = "response_time_hours", nullable = false)
    private Integer responseTimeHours;

    @Column(name = "resolution_time_hours", nullable = false)
    private Integer resolutionTimeHours;
}
