package com.example.Cricbook.repository;

import com.example.Cricbook.entity.MarketingBanner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketingBannerRepository extends JpaRepository<MarketingBanner, Long> {
    List<MarketingBanner> findAllByOrderByCreatedAtDesc();
    List<MarketingBanner> findByStatusOrderByCreatedAtDesc(String status);
}
