package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SponsorInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SponsorInvoiceRepository extends JpaRepository<SponsorInvoice, Long> {
    boolean existsByInvoiceNumber(String invoiceNumber);
    List<SponsorInvoice> findAllByOrderByCreatedAtDesc();
    List<SponsorInvoice> findByStatusOrderByCreatedAtDesc(String status);
}
