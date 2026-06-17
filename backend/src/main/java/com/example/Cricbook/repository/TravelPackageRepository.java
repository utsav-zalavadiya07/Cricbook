package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TravelPackage;
import com.example.Cricbook.enums.TravelPackageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TravelPackageRepository extends JpaRepository<TravelPackage, Long> {
    boolean existsByPackageCodeIgnoreCase(String packageCode);

    @Query("""
            SELECT p FROM TravelPackage p
            WHERE p.status = :status
              AND (:query IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(p.destination) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(p.itinerary) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:sport IS NULL OR LOWER(p.sport) = LOWER(:sport))
              AND (:originCity IS NULL OR LOWER(p.originCity) = LOWER(:originCity))
              AND (:destination IS NULL OR LOWER(p.destination) LIKE LOWER(CONCAT('%', :destination, '%')))
              AND (:maxPrice IS NULL OR p.pricePerPerson <= :maxPrice)
            ORDER BY p.startDate ASC, p.updatedAt DESC
            """)
    List<TravelPackage> searchPublished(
            @Param("status") TravelPackageStatus status,
            @Param("query") String query,
            @Param("sport") String sport,
            @Param("originCity") String originCity,
            @Param("destination") String destination,
            @Param("maxPrice") BigDecimal maxPrice);

    List<TravelPackage> findTop8ByStatusOrderByStartDateAsc(TravelPackageStatus status);
}
