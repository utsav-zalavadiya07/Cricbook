package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "academy_certificates", indexes = {
        @Index(name = "idx_certificate_code", columnList = "certificateCode"),
        @Index(name = "idx_certificate_player", columnList = "playerEmail")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_certificate_code", columnNames = "certificateCode")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademyCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long academyId;

    private Long enrollmentId;

    @Column(nullable = false, length = 80)
    private String certificateCode;

    @Column(nullable = false)
    private String playerName;

    @Column(nullable = false)
    private String playerEmail;

    private String certificateType;

    private String sport;

    private String pdfUrl;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "ISSUED";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime issuedAt;
}
