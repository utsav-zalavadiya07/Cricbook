package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sport_turfs", uniqueConstraints = {
        @UniqueConstraint(name = "unique_sport_turf", columnNames = {"sport_id", "turf_id"})
}, indexes = {
        @Index(name = "idx_sport_turf_turf", columnList = "turf_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SportTurf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sport_id", nullable = false)
    private Long sportId;

    @Column(name = "turf_id", nullable = false)
    private Long turfId;
}
