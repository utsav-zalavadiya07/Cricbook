package com.example.Cricbook.repository;

import com.example.Cricbook.entity.Invoice;
import com.example.Cricbook.enums.MembershipPlanType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("""
            SELECT i
            FROM Invoice i
            JOIN FETCH i.user u
            LEFT JOIN FETCH i.subscription s
            LEFT JOIN FETCH s.plan
            WHERE u.id = :userId
            ORDER BY i.createdAt DESC
            """)
    List<Invoice> findDetailedByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("""
            SELECT i
            FROM Invoice i
            JOIN FETCH i.user u
            LEFT JOIN FETCH i.subscription s
            LEFT JOIN FETCH s.plan p
            WHERE u.id = :userId
              AND p.type = :type
            ORDER BY i.createdAt DESC
            """)
    List<Invoice> findDetailedByUserIdAndPlanTypeOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("type") MembershipPlanType type
    );

    @Query("""
            SELECT i
            FROM Invoice i
            JOIN FETCH i.user
            LEFT JOIN FETCH i.subscription s
            LEFT JOIN FETCH s.plan
            WHERE i.id = :id
            """)
    Optional<Invoice> findDetailedById(@Param("id") Long id);

    @Query("""
            SELECT i
            FROM Invoice i
            JOIN FETCH i.user
            LEFT JOIN FETCH i.subscription s
            LEFT JOIN FETCH s.plan
            WHERE i.invoiceNumber = :invoiceNumber
            """)
    Optional<Invoice> findDetailedByInvoiceNumber(@Param("invoiceNumber") String invoiceNumber);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
}
