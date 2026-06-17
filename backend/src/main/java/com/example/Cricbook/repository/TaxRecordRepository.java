package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TaxRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxRecordRepository extends JpaRepository<TaxRecord, Long> {

    List<TaxRecord> findByInvoiceId(Long invoiceId);
}
