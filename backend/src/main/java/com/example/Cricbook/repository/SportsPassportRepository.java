package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SportsPassport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SportsPassportRepository extends JpaRepository<SportsPassport, Long> {
    boolean existsByShareCode(String shareCode);

    Optional<SportsPassport> findByIdentityId(Long identityId);

    Optional<SportsPassport> findByShareCode(String shareCode);

    List<SportsPassport> findAllByOrderByCreatedAtDesc();
}
