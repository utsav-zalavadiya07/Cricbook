package com.example.Cricbook.repository;

import com.example.Cricbook.entity.Turf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface TurfRepository extends JpaRepository<Turf, Long>, JpaSpecificationExecutor<Turf> {
    interface SearchSuggestionView {
        String getName();
        String getCity();
        String getLocation();
    }

    List<Turf> findByOwnerId(Long ownerId);
    long countByOwnerId(Long ownerId);
    boolean existsByOwnerIdAndCityIgnoreCaseAndNameIgnoreCase(Long ownerId, String city, String name);
    boolean existsByOwnerIdAndCityIgnoreCaseAndNameIgnoreCaseAndIdNot(Long ownerId, String city, String name, Long id);

    @Query(value = """
        SELECT t.*, 
        (6371 * acos(
            cos(radians(:lat)) *
            cos(radians(t.latitude)) *
            cos(radians(t.longitude) - radians(:lon)) +
            sin(radians(:lat)) *
            sin(radians(t.latitude))
        )) AS distance
        FROM turfs t
        HAVING distance < :radius
        ORDER BY distance
    """, nativeQuery = true)
    List<Object[]> findNearbyTurfs(@Param("lat") Double lat, @Param("lon") Double lon, @Param("radius") Double radius);

    @Query("""
    SELECT DISTINCT t FROM Turf t
    JOIN Slot s ON s.turf.id = t.id
    JOIN SlotAvailability sa ON sa.slot.id = s.id
    LEFT JOIN t.amenities a
    WHERE (:query IS NULL OR
        LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%')) OR
        LOWER(t.city) LIKE LOWER(CONCAT('%', :query, '%')) OR
        LOWER(t.location) LIKE LOWER(CONCAT('%', :query, '%')) OR
        LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')))
    AND (:city IS NULL OR LOWER(t.city) = LOWER(:city))
    AND t.status = com.example.Cricbook.enums.TurfStatus.ACTIVE
    AND sa.date = :date
    AND sa.isAvailable = true
    AND sa.isBlocked = false
    AND (:minPrice IS NULL OR t.pricePerHour >= :minPrice)
    AND (:maxPrice IS NULL OR t.pricePerHour <= :maxPrice)
    AND (:rating IS NULL OR t.rating >= :rating)
    AND (:boxType IS NULL OR CAST(t.boxType AS string) = :boxType)
    AND (:pitchType IS NULL OR CAST(t.pitchType AS string) = :pitchType)
    AND (:amenitiesEmpty = true OR a.amenity IN :amenities)
    """)
    org.springframework.data.domain.Page<Turf> searchTurfs(
        @Param("query") String query,
        @Param("city") String city,
        @Param("date") java.time.LocalDate date,
        @Param("minPrice") java.math.BigDecimal minPrice,
        @Param("maxPrice") java.math.BigDecimal maxPrice,
        @Param("rating") Double rating,
        @Param("boxType") String boxType,
        @Param("pitchType") String pitchType,
        @Param("amenities") java.util.List<String> amenities,
        @Param("amenitiesEmpty") boolean amenitiesEmpty,
        org.springframework.data.domain.Pageable pageable
    );

    @Query("""
    SELECT DISTINCT t FROM Turf t
    LEFT JOIN t.amenities a
    WHERE (:query IS NULL OR
        LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%')) OR
        LOWER(t.city) LIKE LOWER(CONCAT('%', :query, '%')) OR
        LOWER(t.location) LIKE LOWER(CONCAT('%', :query, '%')) OR
        LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')))
    AND (:city IS NULL OR LOWER(t.city) = LOWER(:city))
    AND t.status = com.example.Cricbook.enums.TurfStatus.ACTIVE
    AND (:minPrice IS NULL OR t.pricePerHour >= :minPrice)
    AND (:maxPrice IS NULL OR t.pricePerHour <= :maxPrice)
    AND (:rating IS NULL OR t.rating >= :rating)
    AND (:boxType IS NULL OR CAST(t.boxType AS string) = :boxType)
    AND (:pitchType IS NULL OR CAST(t.pitchType AS string) = :pitchType)
    AND (:amenitiesEmpty = true OR a.amenity IN :amenities)
    """)
    org.springframework.data.domain.Page<Turf> searchActiveTurfs(
        @Param("query") String query,
        @Param("city") String city,
        @Param("minPrice") java.math.BigDecimal minPrice,
        @Param("maxPrice") java.math.BigDecimal maxPrice,
        @Param("rating") Double rating,
        @Param("boxType") String boxType,
        @Param("pitchType") String pitchType,
        @Param("amenities") java.util.List<String> amenities,
        @Param("amenitiesEmpty") boolean amenitiesEmpty,
        org.springframework.data.domain.Pageable pageable
    );

    @Query("""
    SELECT DISTINCT t.name AS name, t.city AS city, t.location AS location
    FROM Turf t
    WHERE (:query IS NULL OR
        LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%')) OR
        LOWER(t.city) LIKE LOWER(CONCAT('%', :query, '%')) OR
        LOWER(t.location) LIKE LOWER(CONCAT('%', :query, '%')))
    AND t.status = com.example.Cricbook.enums.TurfStatus.ACTIVE
    """)
    List<SearchSuggestionView> findSearchSuggestions(
        @Param("query") String query,
        org.springframework.data.domain.Pageable pageable
    );
}
