package com.example.Cricbook.repository;

import com.example.Cricbook.entity.OwnerSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OwnerSettingsRepository extends JpaRepository<OwnerSettings, Long> {
    Optional<OwnerSettings> findByOwnerId(Long ownerId);
}
