package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TenantReseller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenantResellerRepository extends JpaRepository<TenantReseller, Long> {
    boolean existsByEmailIgnoreCase(String email);

    List<TenantReseller> findAllByOrderByCreatedAtDesc();
}
