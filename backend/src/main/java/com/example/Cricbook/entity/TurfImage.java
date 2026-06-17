package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "turf_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurfImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turf_id", nullable = false)
    private Turf turf;

    @Column(name = "image_path", nullable = false)
    private String imagePath;
}
