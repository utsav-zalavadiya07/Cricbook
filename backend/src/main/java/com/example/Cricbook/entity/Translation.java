package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "translations", uniqueConstraints = {
        @UniqueConstraint(name = "unique_locale_key", columnNames = {"locale", "translation_key"})
}, indexes = {
        @Index(name = "idx_translation_locale", columnList = "locale")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Translation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String locale; // en, hi, gu, ar, fr

    @Column(name = "translation_key", nullable = false)
    private String translationKey;

    @Column(name = "translated_value", columnDefinition = "TEXT", nullable = false)
    private String translatedValue;
}
