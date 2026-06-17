package com.example.Cricbook.repository;

import com.example.Cricbook.entity.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {
    List<ReviewReport> findByReviewId(Long reviewId);
    Optional<ReviewReport> findByReviewIdAndReporterId(Long reviewId, Long reporterId);
    List<ReviewReport> findByStatus(String status);
}
