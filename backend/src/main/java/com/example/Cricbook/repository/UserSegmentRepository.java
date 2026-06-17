package com.example.Cricbook.repository;

import com.example.Cricbook.entity.UserSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSegmentRepository extends JpaRepository<UserSegment, Long> {
    Optional<UserSegment> findByName(String name);
}
