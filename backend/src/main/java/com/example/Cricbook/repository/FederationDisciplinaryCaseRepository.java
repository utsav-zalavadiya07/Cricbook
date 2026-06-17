package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FederationDisciplinaryCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FederationDisciplinaryCaseRepository extends JpaRepository<FederationDisciplinaryCase, Long> {
    List<FederationDisciplinaryCase> findAllByOrderByCreatedAtDesc();
    List<FederationDisciplinaryCase> findByStatusOrderByCreatedAtDesc(String status);
}
