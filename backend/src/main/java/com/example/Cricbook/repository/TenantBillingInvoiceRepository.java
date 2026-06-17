package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TenantBillingInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenantBillingInvoiceRepository extends JpaRepository<TenantBillingInvoice, Long> {
    boolean existsByInvoiceNumber(String invoiceNumber);

    List<TenantBillingInvoice> findAllByOrderByCreatedAtDesc();

    List<TenantBillingInvoice> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
