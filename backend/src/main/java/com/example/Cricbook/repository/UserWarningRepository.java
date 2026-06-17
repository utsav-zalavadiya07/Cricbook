package com.example.Cricbook.repository;

import com.example.Cricbook.entity.UserWarning;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserWarningRepository extends JpaRepository<UserWarning, Long> {
    List<UserWarning> findTop50ByOrderByCreatedAtDesc();
    long countByUserId(Long userId);
}
