package com.example.Cricbook.repository;

import com.example.Cricbook.entity.UserVector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserVectorRepository extends JpaRepository<UserVector, Long> {
    Optional<UserVector> findByUserId(Long userId);
}
