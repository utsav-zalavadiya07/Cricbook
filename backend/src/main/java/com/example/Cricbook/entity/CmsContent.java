package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cms_content")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type; // BANNER, PROMOTION, FEATURED

    @Column(nullable = false)
    private String title;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(nullable = false)
    private String status; // ACTIVE, INACTIVE

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
