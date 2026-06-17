package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_segments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "rule_type", nullable = false)
    private String ruleType; // BOOKINGS_COUNT, CITY, INACTIVITY

    @Column(name = "rule_value", nullable = false)
    private String ruleValue; // e.g. "5", "Ahmedabad", "30"

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
