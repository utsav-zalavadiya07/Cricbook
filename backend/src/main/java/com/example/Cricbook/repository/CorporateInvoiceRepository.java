package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CorporateInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorporateInvoiceRepository extends JpaRepository<CorporateInvoice, Long> {
    List<CorporateInvoice> findAllByOrderByCreatedAtDesc();
    List<CorporateInvoice> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
}
