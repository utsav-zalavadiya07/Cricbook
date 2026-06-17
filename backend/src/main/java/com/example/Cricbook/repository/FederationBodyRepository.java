package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FederationBody;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FederationBodyRepository extends JpaRepository<FederationBody, Long> {
    boolean existsByNameIgnoreCaseAndBodyTypeIgnoreCase(String name, String bodyType);
    Optional<FederationBody> findByNameIgnoreCase(String name);
    List<FederationBody> findByStatusOrderByCreatedAtDesc(String status);
    List<FederationBody> findByParentBodyIdOrderByCreatedAtDesc(Long parentBodyId);
    List<FederationBody> findAllByOrderByCreatedAtDesc();
}
