package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TeamRecruitment;
import com.example.Cricbook.enums.TeamRecruitmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRecruitmentRepository extends JpaRepository<TeamRecruitment, Long> {
    List<TeamRecruitment> findByStatusOrderByCreatedAtDesc(TeamRecruitmentStatus status);
    List<TeamRecruitment> findByCaptainIdOrderByCreatedAtDesc(Long captainId);
    long countByStatus(TeamRecruitmentStatus status);
}
