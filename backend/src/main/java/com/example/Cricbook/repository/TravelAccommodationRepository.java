package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TravelAccommodation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravelAccommodationRepository extends JpaRepository<TravelAccommodation, Long> {
    @Query("""
            SELECT a FROM TravelAccommodation a
            WHERE a.status = 'ACTIVE'
              AND (:city IS NULL OR LOWER(a.city) = LOWER(:city))
              AND (:accommodationType IS NULL OR LOWER(a.accommodationType) = LOWER(:accommodationType))
            ORDER BY a.pricePerNight ASC
            """)
    List<TravelAccommodation> searchAvailable(@Param("city") String city, @Param("accommodationType") String accommodationType);
}
