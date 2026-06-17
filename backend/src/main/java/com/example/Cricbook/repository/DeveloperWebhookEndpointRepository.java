package com.example.Cricbook.repository;

import com.example.Cricbook.entity.DeveloperWebhookEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeveloperWebhookEndpointRepository extends JpaRepository<DeveloperWebhookEndpoint, Long> {
    List<DeveloperWebhookEndpoint> findByApplication_IdOrderByCreatedAtDesc(Long applicationId);
    List<DeveloperWebhookEndpoint> findAllByOrderByCreatedAtDesc();
    List<DeveloperWebhookEndpoint> findByStatusOrderByCreatedAtDesc(String status);
}
