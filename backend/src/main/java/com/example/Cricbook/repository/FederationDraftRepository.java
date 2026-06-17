package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FederationDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FederationDraftRepository extends JpaRepository<FederationDraft, Long> {
    List<FederationDraft> findAllByOrderByCreatedAtDesc();
    List<FederationDraft> findByFederationIdOrderByCreatedAtDesc(Long federationId);
}
