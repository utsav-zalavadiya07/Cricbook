package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TeamCollection;
import com.example.Cricbook.enums.CollectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamCollectionRepository extends JpaRepository<TeamCollection, Long> {
    Optional<TeamCollection> findByBookingId(Long bookingId);
    List<TeamCollection> findByCaptainIdOrderByCreatedAtDesc(Long captainId);
    long countByStatus(CollectionStatus status);
}
