package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CommerceInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommerceInventoryRepository extends JpaRepository<CommerceInventory, Long> {
    boolean existsBySkuIgnoreCase(String sku);
    Optional<CommerceInventory> findBySkuIgnoreCase(String sku);
    List<CommerceInventory> findByProductIdOrderByVariantLabelAsc(Long productId);
}
