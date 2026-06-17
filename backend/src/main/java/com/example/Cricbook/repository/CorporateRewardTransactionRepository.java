package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CorporateRewardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorporateRewardTransactionRepository extends JpaRepository<CorporateRewardTransaction, Long> {
    List<CorporateRewardTransaction> findAllByOrderByCreatedAtDesc();
    List<CorporateRewardTransaction> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
}
