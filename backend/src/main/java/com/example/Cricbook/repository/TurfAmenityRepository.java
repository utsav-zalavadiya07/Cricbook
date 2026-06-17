package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TurfAmenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TurfAmenityRepository extends JpaRepository<TurfAmenity, Long> {
    @Query("""
            SELECT DISTINCT amenity.amenity
            FROM TurfAmenity amenity
            WHERE amenity.amenity IS NOT NULL
              AND amenity.amenity <> ''
            ORDER BY amenity.amenity
            """)
    List<String> findDistinctAmenityNames();
}
