package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TaxReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaxReportRepository extends JpaRepository<TaxReport, Long> {
    List<TaxReport> findTop50ByOrderByGeneratedAtDesc();
}
