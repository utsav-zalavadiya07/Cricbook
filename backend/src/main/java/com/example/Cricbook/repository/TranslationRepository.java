package com.example.Cricbook.repository;

import com.example.Cricbook.entity.Translation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TranslationRepository extends JpaRepository<Translation, Long> {
    List<Translation> findByLocale(String locale);
    Optional<Translation> findByLocaleAndTranslationKey(String locale, String translationKey);
}
