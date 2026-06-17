package com.example.Cricbook.repository;

import com.example.Cricbook.entity.DeveloperAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeveloperAccountRepository extends JpaRepository<DeveloperAccount, Long> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<DeveloperAccount> findByEmailIgnoreCase(String email);
    List<DeveloperAccount> findAllByOrderByCreatedAtDesc();
    List<DeveloperAccount> findByStatusOrderByCreatedAtDesc(String status);
}
