package com.example.Cricbook.repository;

import com.example.Cricbook.entity.DeveloperApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeveloperApiKeyRepository extends JpaRepository<DeveloperApiKey, Long> {
    Optional<DeveloperApiKey> findByKeyHashAndStatus(String keyHash, String status);
    List<DeveloperApiKey> findByApplication_IdOrderByCreatedAtDesc(Long applicationId);
    List<DeveloperApiKey> findAllByOrderByCreatedAtDesc();
    List<DeveloperApiKey> findByStatusOrderByCreatedAtDesc(String status);
}
