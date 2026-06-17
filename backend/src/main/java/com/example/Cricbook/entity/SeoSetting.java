package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "seo_settings", indexes = @Index(name = "idx_seo_page_slug", columnList = "page_slug"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeoSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "page_slug", nullable = false, unique = true)
    private String pageSlug;

    @Column(name = "meta_title", nullable = false)
    private String metaTitle;

    @Column(name = "meta_description", nullable = false, length = 500)
    private String metaDescription;

    @Column(length = 500)
    private String keywords;

    @Column(name = "open_graph_image")
    private String openGraphImage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
