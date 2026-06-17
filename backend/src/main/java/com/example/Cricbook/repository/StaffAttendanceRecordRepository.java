package com.example.Cricbook.repository;

import com.example.Cricbook.entity.StaffAttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StaffAttendanceRecordRepository extends JpaRepository<StaffAttendanceRecord, Long> {
    List<StaffAttendanceRecord> findByStaffIdOrderByCreatedAtDesc(Long staffId);
    List<StaffAttendanceRecord> findByCenterIdInOrderByCreatedAtDesc(List<Long> centerIds);
    Optional<StaffAttendanceRecord> findFirstByStaffIdAndWorkDateAndShiftEndIsNullOrderByCreatedAtDesc(Long staffId, LocalDate workDate);
}
