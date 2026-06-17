package com.example.Cricbook.repository;

import com.example.Cricbook.entity.BannedEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BannedEntityRepository extends JpaRepository<BannedEntity, Long> {
    List<BannedEntity> findByActiveTrueOrderByCreatedAtDesc();
    List<BannedEntity> findTop50ByOrderByCreatedAtDesc();
}
