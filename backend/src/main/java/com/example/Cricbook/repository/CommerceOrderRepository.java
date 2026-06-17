package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CommerceOrder;
import com.example.Cricbook.enums.CommerceOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CommerceOrderRepository extends JpaRepository<CommerceOrder, Long> {
    List<CommerceOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<CommerceOrder> findTop20ByOrderByCreatedAtDesc();
    long countByStatus(CommerceOrderStatus status);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM CommerceOrder o WHERE o.status <> :cancelled")
    BigDecimal totalGmvExcluding(@Param("cancelled") CommerceOrderStatus cancelled);
}
