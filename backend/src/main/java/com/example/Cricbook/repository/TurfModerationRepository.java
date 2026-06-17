package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TurfModeration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TurfModerationRepository extends JpaRepository<TurfModeration, Long> {
    List<TurfModeration> findByTurfId(Long turfId);
}
