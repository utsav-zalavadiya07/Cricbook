package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CommerceProduct;
import com.example.Cricbook.enums.CommerceProductStatus;
import com.example.Cricbook.enums.CommerceProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CommerceProductRepository extends JpaRepository<CommerceProduct, Long> {
    boolean existsBySkuIgnoreCase(String sku);

    @Query("""
            SELECT p FROM CommerceProduct p
            WHERE p.status = :status
              AND (:query IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:category IS NULL OR LOWER(p.category) = LOWER(:category))
              AND (:sport IS NULL OR LOWER(p.sport) = LOWER(:sport))
              AND (:productType IS NULL OR p.productType = :productType)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice OR p.rentalFeePerDay <= :maxPrice)
            ORDER BY p.featured DESC, p.updatedAt DESC, p.id DESC
            """)
    List<CommerceProduct> searchPublished(
            @Param("status") CommerceProductStatus status,
            @Param("query") String query,
            @Param("category") String category,
            @Param("sport") String sport,
            @Param("productType") CommerceProductType productType,
            @Param("maxPrice") BigDecimal maxPrice);

    List<CommerceProduct> findTop8ByStatusOrderByFeaturedDescUpdatedAtDesc(CommerceProductStatus status);
}
