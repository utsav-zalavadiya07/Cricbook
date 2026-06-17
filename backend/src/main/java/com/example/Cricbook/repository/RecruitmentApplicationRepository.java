package com.example.Cricbook.repository;

import com.example.Cricbook.entity.RecruitmentApplication;
import com.example.Cricbook.enums.RecruitmentApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecruitmentApplicationRepository extends JpaRepository<RecruitmentApplication, Long> {
    Optional<RecruitmentApplication> findByRecruitmentIdAndUserId(Long recruitmentId, Long userId);
    List<RecruitmentApplication> findByRecruitmentIdOrderByCreatedAtDesc(Long recruitmentId);
    List<RecruitmentApplication> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByStatus(RecruitmentApplicationStatus status);
}
