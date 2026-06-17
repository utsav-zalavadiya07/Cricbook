package com.example.Cricbook.repository;

import com.example.Cricbook.entity.OwnerExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface OwnerExpenseRepository extends JpaRepository<OwnerExpense, Long> {
    List<OwnerExpense> findByOwnerIdAndExpenseDateBetweenOrderByExpenseDateDesc(Long ownerId, LocalDate start, LocalDate end);

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM OwnerExpense e
            WHERE e.owner.id = :ownerId
            AND e.expenseDate BETWEEN :start AND :end
            """)
    BigDecimal sumByOwnerAndDateRange(@Param("ownerId") Long ownerId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
