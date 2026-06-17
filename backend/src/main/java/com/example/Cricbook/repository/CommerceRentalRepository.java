package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CommerceRental;
import com.example.Cricbook.enums.CommerceRentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommerceRentalRepository extends JpaRepository<CommerceRental, Long> {
    long countByStatus(CommerceRentalStatus status);
}
