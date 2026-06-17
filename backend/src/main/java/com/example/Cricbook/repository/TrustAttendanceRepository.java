package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TrustAttendance;
import com.example.Cricbook.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrustAttendanceRepository extends JpaRepository<TrustAttendance, Long> {
    Optional<TrustAttendance> findByBookingId(Long bookingId);
    List<TrustAttendance> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserId(Long userId);
    long countByUserIdAndStatus(Long userId, AttendanceStatus status);
}
