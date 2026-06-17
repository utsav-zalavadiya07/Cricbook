package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FavoriteTurf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteTurfRepository extends JpaRepository<FavoriteTurf, Long> {
    List<FavoriteTurf> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<FavoriteTurf> findByUserIdAndTurfId(Long userId, Long turfId);
    boolean existsByUserIdAndTurfId(Long userId, Long turfId);
    void deleteByUserIdAndTurfId(Long userId, Long turfId);
}
