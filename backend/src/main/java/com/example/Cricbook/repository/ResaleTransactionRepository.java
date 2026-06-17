package com.example.Cricbook.repository;

import com.example.Cricbook.entity.ResaleTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResaleTransactionRepository extends JpaRepository<ResaleTransaction, Long> {
    boolean existsByListingId(Long listingId);
}
