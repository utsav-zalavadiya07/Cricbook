package com.example.Cricbook.repository;

import com.example.Cricbook.entity.DeveloperMarketplaceListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeveloperMarketplaceListingRepository extends JpaRepository<DeveloperMarketplaceListing, Long> {
    List<DeveloperMarketplaceListing> findByStatusOrderByCreatedAtDesc(String status);
    List<DeveloperMarketplaceListing> findAllByOrderByCreatedAtDesc();
    List<DeveloperMarketplaceListing> findByDeveloper_IdOrderByCreatedAtDesc(Long developerId);
}
