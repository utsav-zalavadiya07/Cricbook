package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FanMarketplaceListing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FanMarketplaceListingRepository extends JpaRepository<FanMarketplaceListing, Long> {
    boolean existsByListingCode(String listingCode);

    List<FanMarketplaceListing> findAllByOrderByCreatedAtDesc();

    List<FanMarketplaceListing> findByStatusOrderByCreatedAtDesc(String status);
}
