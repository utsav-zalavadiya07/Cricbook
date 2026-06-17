package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FederationDraftSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FederationDraftSelectionRepository extends JpaRepository<FederationDraftSelection, Long> {
    boolean existsByDraftIdAndPlayerRegistrationId(Long draftId, Long playerRegistrationId);
    long countByDraftId(Long draftId);
    List<FederationDraftSelection> findByDraftIdOrderByPickNumberAsc(Long draftId);
    List<FederationDraftSelection> findAllByOrderBySelectedAtDesc();
}
