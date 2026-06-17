package com.example.Cricbook.repository;

import com.example.Cricbook.entity.DigitalCollectible;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DigitalCollectibleRepository extends JpaRepository<DigitalCollectible, Long> {
    boolean existsByCollectibleCode(String collectibleCode);

    List<DigitalCollectible> findAllByOrderByCreatedAtDesc();

    List<DigitalCollectible> findByStatusOrderByCreatedAtDesc(String status);
}
