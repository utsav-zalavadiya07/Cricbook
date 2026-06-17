package com.example.Cricbook.repository;

import com.example.Cricbook.entity.PlayerSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerSettingsRepository extends JpaRepository<PlayerSettings, Long> {
    Optional<PlayerSettings> findByUserId(Long userId);
}
