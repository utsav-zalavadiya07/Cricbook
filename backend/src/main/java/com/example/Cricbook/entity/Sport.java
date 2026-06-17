package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(unique = true, length = 64)
    private String code;

    @Column(nullable = false)
    private String icon;

    @Column(columnDefinition = "TEXT")
    private String rules;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    private Integer displayOrder;

    private Integer minPlayers;

    private Integer maxPlayers;

    private String scoringType;

    private String matchType;

    private String rankingType;

    private Boolean teamBased;

    private Boolean tournamentEnabled;

    private Boolean statisticsEnabled;

    @Column(columnDefinition = "TEXT")
    private String configurationJson;

    @Column(columnDefinition = "TEXT")
    private String metadataJson;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
