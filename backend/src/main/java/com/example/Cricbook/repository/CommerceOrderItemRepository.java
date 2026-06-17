package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CommerceOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommerceOrderItemRepository extends JpaRepository<CommerceOrderItem, Long> {
    List<CommerceOrderItem> findByOrderIdOrderByIdAsc(Long orderId);
}
