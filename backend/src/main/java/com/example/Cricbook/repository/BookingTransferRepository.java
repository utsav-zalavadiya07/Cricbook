package com.example.Cricbook.repository;

import com.example.Cricbook.entity.BookingTransfer;
import com.example.Cricbook.enums.BookingTransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingTransferRepository extends JpaRepository<BookingTransfer, Long> {
    List<BookingTransfer> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
    long countByStatus(BookingTransferStatus status);
}
