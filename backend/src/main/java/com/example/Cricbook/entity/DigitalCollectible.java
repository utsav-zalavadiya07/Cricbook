package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "digital_collectibles", indexes = {
        @Index(name = "idx_digital_collectible_code", columnList = "collectible_code", unique = true),
        @Index(name = "idx_digital_collectible_status", columnList = "status"),
        @Index(name = "idx_digital_collectible_source", columnList = "source_module, source_ref_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DigitalCollectible {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "collectible_code", nullable = false, unique = true, length = 48)
    private String collectibleCode;

    @Column(nullable = false)
    private String title;

    @Builder.Default
    @Column(nullable = false)
    private String collectibleType = "CARD";

    @Builder.Default
    @Column(nullable = false)
    private String rarity = "COMMON";

    private String sourceModule;

    @Column(name = "source_ref_id")
    private Long sourceRefId;

    @Builder.Default
    @Column(nullable = false)
    private Integer supplyLimit = 1;

    @Builder.Default
    @Column(nullable = false)
    private Integer mintedCount = 0;

    private String mediaUrl;

    private String credentialReference;

    @Builder.Default
    @Column(nullable = false)
    private String status = "ACTIVE";

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
