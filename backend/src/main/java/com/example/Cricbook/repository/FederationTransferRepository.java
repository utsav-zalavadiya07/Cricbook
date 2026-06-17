package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FederationTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FederationTransferRepository extends JpaRepository<FederationTransfer, Long> {
    List<FederationTransfer> findAllByOrderByCreatedAtDesc();
    List<FederationTransfer> findByStatusOrderByCreatedAtDesc(String status);
}
