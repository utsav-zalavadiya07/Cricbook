package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SportsCreditLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportsCreditLedgerRepository extends JpaRepository<SportsCreditLedger, Long> {
    List<SportsCreditLedger> findByIdentityIdOrderByCreatedAtDesc(Long identityId);

    List<SportsCreditLedger> findAllByOrderByCreatedAtDesc();
}
