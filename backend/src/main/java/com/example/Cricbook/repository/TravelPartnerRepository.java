package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TravelPartner;
import com.example.Cricbook.enums.TravelPartnerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravelPartnerRepository extends JpaRepository<TravelPartner, Long> {
    boolean existsByEmailIgnoreCase(String email);
    List<TravelPartner> findByStatusOrderByCreatedAtDesc(TravelPartnerStatus status);
    List<TravelPartner> findAllByOrderByCreatedAtDesc();
}
