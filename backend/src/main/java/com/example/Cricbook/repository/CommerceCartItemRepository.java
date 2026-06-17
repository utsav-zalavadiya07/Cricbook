package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CommerceCartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommerceCartItemRepository extends JpaRepository<CommerceCartItem, Long> {
    List<CommerceCartItem> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<CommerceCartItem> findByUserIdAndInventoryId(Long userId, Long inventoryId);
    void deleteByUserId(Long userId);
}
