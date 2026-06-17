package com.example.Cricbook.repository;

import com.example.Cricbook.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByUserIdAndTurfId(Long userId, Long turfId);
    List<Review> findByTurfIdOrderByCreatedAtDesc(Long turfId);
    List<Review> findByTurfIdAndStatusOrderByCreatedAtDesc(Long turfId, String status);
    List<Review> findByTurfIdAndStatusNotOrderByCreatedAtDesc(Long turfId, String status);
    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Review> findByUserIdAndStatusNotOrderByCreatedAtDesc(Long userId, String status);

    @org.springframework.data.jpa.repository.Query("""
        SELECT r FROM Review r
        JOIN Turf t ON t.id = r.turfId
        WHERE t.owner.id = :ownerId
        AND (r.status IS NULL OR r.status <> 'DELETED')
        ORDER BY r.createdAt DESC
    """)
    List<Review> findByTurfOwnerIdOrderByCreatedAtDesc(Long ownerId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT r FROM Review r
        JOIN Turf t ON t.id = r.turfId
        WHERE t.owner.id = :ownerId
        AND r.status = :status
        ORDER BY r.createdAt DESC
    """)
    List<Review> findByTurfOwnerIdAndStatusOrderByCreatedAtDesc(Long ownerId, String status);
}
