package com.example.Cricbook.repository;

import com.example.Cricbook.entity.Booking;
import com.example.Cricbook.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDate;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    List<Booking> findByUserIdOrderByBookingDateDescCreatedAtDesc(Long userId);
    List<Booking> findByTurfId(Long turfId);
    List<Booking> findByStatus(BookingStatus status);
    List<Booking> findByStatusAndExpiryTimeBefore(BookingStatus status, java.time.LocalDateTime expiryTime);
    boolean existsByUserIdAndTurfIdAndStatus(Long userId, Long turfId, BookingStatus status);
    boolean existsByUserIdAndTurfIdAndStatusIn(Long userId, Long turfId, List<BookingStatus> statuses);
    boolean existsByUserIdAndBookingDateAndSlotIdAndStatusIn(Long userId, LocalDate bookingDate, Long slotId, List<BookingStatus> statuses);
    boolean existsByBookingDateAndSlotIdAndStatusIn(LocalDate bookingDate, Long slotId, List<BookingStatus> statuses);
    List<Booking> findByBookingDateAndSlotIdAndStatusIn(LocalDate bookingDate, Long slotId, List<BookingStatus> statuses);
    List<Booking> findBySlotIdAndBookingDateGreaterThanEqualAndStatusIn(Long slotId, LocalDate bookingDate, List<BookingStatus> statuses);
    List<Booking> findByTurfIdAndBookingDateAndStatusIn(Long turfId, LocalDate bookingDate, List<BookingStatus> statuses);
    boolean existsByTurfIdAndBookingDateGreaterThanEqualAndStatusIn(Long turfId, LocalDate bookingDate, List<BookingStatus> statuses);

    @Query("SELECT b FROM Booking b WHERE b.turf.owner.id = :ownerId ORDER BY b.bookingDate DESC, b.createdAt DESC")
    List<Booking> findByTurfOwnerId(Long ownerId);

    @Query("SELECT b FROM Booking b WHERE b.turf.owner.id = :ownerId AND b.status = :status ORDER BY b.bookingDate DESC, b.createdAt DESC")
    List<Booking> findByTurfOwnerIdAndStatus(Long ownerId, BookingStatus status);

    @Query("""
            SELECT b.slot.id FROM Booking b
            WHERE b.turf.id = :turfId
            AND b.bookingDate = :date
            AND b.status IN :statuses
            """)
    List<Long> findBookedSlotIdsByTurfAndDate(
            @org.springframework.data.repository.query.Param("turfId") Long turfId,
            @org.springframework.data.repository.query.Param("date") LocalDate date,
            @org.springframework.data.repository.query.Param("statuses") List<BookingStatus> statuses);
}
