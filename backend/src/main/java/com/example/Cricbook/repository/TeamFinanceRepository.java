package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TeamFinance;
import com.example.Cricbook.enums.TeamFinanceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TeamFinanceRepository extends JpaRepository<TeamFinance, Long> {
    List<TeamFinance> findByTeamIdOrderByCreatedAtDesc(Long teamId);

    @Query("SELECT COALESCE(SUM(f.amount), 0) FROM TeamFinance f WHERE f.team.id = :teamId AND f.type = :type")
    BigDecimal sumByTeamIdAndType(@Param("teamId") Long teamId, @Param("type") TeamFinanceType type);
}
