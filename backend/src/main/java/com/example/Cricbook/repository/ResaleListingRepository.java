package com.example.Cricbook.repository;

import com.example.Cricbook.entity.ResaleListing;
import com.example.Cricbook.enums.ResaleListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResaleListingRepository extends JpaRepository<ResaleListing, Long> {
    Optional<ResaleListing> findByBookingId(Long bookingId);
    Optional<ResaleListing> findByBookingIdAndStatus(Long bookingId, ResaleListingStatus status);
    List<ResaleListing> findByStatusOrderByExpiresAtAsc(ResaleListingStatus status);
    List<ResaleListing> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
    long countByStatus(ResaleListingStatus status);

    @Query("SELECT r FROM ResaleListing r WHERE r.booking.turf.owner.id = :ownerId ORDER BY r.createdAt DESC")
    List<ResaleListing> findByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT COALESCE(SUM(r.price), 0) FROM ResaleListing r WHERE r.status = :status")
    BigDecimal sumPriceByStatus(ResaleListingStatus status);

    @Query("""
            SELECT r FROM ResaleListing r
            WHERE r.status = com.example.Cricbook.enums.ResaleListingStatus.ACTIVE
            AND (:city IS NULL OR LOWER(r.booking.turf.city) = LOWER(:city))
            ORDER BY r.expiresAt ASC
            """)
    List<ResaleListing> findActiveMarketplace(@Param("city") String city);
}
