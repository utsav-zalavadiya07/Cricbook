package com.example.Cricbook.repository;

import com.example.Cricbook.entity.DeveloperInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeveloperInvoiceRepository extends JpaRepository<DeveloperInvoice, Long> {
    List<DeveloperInvoice> findByDeveloper_IdOrderByCreatedAtDesc(Long developerId);
    List<DeveloperInvoice> findAllByOrderByCreatedAtDesc();
    List<DeveloperInvoice> findByStatusOrderByCreatedAtDesc(String status);
}
