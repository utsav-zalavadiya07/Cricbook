package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FinanceReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface FinanceReportRepository extends JpaRepository<FinanceReport, Long> {
    Optional<FinanceReport> findByDate(LocalDate date);
}
