package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SportsVolunteerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportsVolunteerAssignmentRepository extends JpaRepository<SportsVolunteerAssignment, Long> {
    List<SportsVolunteerAssignment> findAllByOrderByAssignedAtDesc();

    List<SportsVolunteerAssignment> findByVolunteerIdOrderByAssignedAtDesc(Long volunteerId);
}
