package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FederationAuctionBid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FederationAuctionBidRepository extends JpaRepository<FederationAuctionBid, Long> {
    List<FederationAuctionBid> findByAuctionIdOrderByBidAmountDesc(Long auctionId);
    List<FederationAuctionBid> findAllByOrderByCreatedAtDesc();
}
