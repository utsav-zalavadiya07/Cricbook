package com.example.Cricbook.repository;

import com.example.Cricbook.entity.RevenueRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RevenueRecordRepository extends JpaRepository<RevenueRecord, Long> {

    List<RevenueRecord> findByCreatedAtAfter(LocalDateTime date);
}
