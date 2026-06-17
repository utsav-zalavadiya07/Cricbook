package com.example.Cricbook.repository;

import com.example.Cricbook.entity.WatchParty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatchPartyRepository extends JpaRepository<WatchParty, Long> {
    boolean existsByPartyCode(String partyCode);

    List<WatchParty> findAllByOrderByScheduledAtDesc();

    List<WatchParty> findByStatusOrderByScheduledAtAsc(String status);
}
