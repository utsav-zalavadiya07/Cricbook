package com.example.Cricbook.repository;

import com.example.Cricbook.entity.WatchPartyParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchPartyParticipantRepository extends JpaRepository<WatchPartyParticipant, Long> {
    Optional<WatchPartyParticipant> findByWatchPartyIdAndFanProfileIdAndStatus(Long watchPartyId, Long fanProfileId, String status);

    List<WatchPartyParticipant> findAllByOrderByJoinedAtDesc();
}
