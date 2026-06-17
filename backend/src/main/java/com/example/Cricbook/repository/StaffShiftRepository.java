package com.example.Cricbook.repository;

import com.example.Cricbook.entity.StaffShift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StaffShiftRepository extends JpaRepository<StaffShift, Long> {
    List<StaffShift> findByStaffIdOrderByShiftDateAscStartTimeAsc(Long staffId);
    List<StaffShift> findByStaffIdAndShiftDateBetweenOrderByShiftDateAscStartTimeAsc(Long staffId, LocalDate start, LocalDate end);
    List<StaffShift> findByCenterIdInOrderByShiftDateAscStartTimeAsc(List<Long> centerIds);
}
