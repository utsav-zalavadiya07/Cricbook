package com.example.Cricbook.repository;

import com.example.Cricbook.entity.NotificationQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationQueue, Long> {
    List<NotificationQueue> findTop10ByStatusOrderByCreatedAtAsc(String status);
    List<NotificationQueue> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<NotificationQueue> findByIdAndUserId(Long id, Long userId);
    List<NotificationQueue> findByUserIdAndIdIn(Long userId, List<Long> ids);

    @Modifying
    @Query("UPDATE NotificationQueue n SET n.status = CASE WHEN n.status = 'ARCHIVED' THEN 'ARCHIVED' ELSE 'READ' END, n.readAt = CURRENT_TIMESTAMP WHERE n.id = :id AND n.userId = :userId")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE NotificationQueue n SET n.status = CASE WHEN n.status = 'ARCHIVED' THEN 'ARCHIVED' ELSE 'READ' END, n.readAt = CURRENT_TIMESTAMP WHERE n.userId = :userId AND n.readAt IS NULL")
    int markAllAsRead(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE NotificationQueue n SET n.status = CASE WHEN n.status = 'ARCHIVED' THEN 'ARCHIVED' ELSE 'READ' END, n.readAt = CURRENT_TIMESTAMP WHERE n.userId = :userId AND n.id IN :ids")
    int markManyAsRead(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    @Modifying
    @Query("UPDATE NotificationQueue n SET n.status = 'ARCHIVED', n.archivedAt = CURRENT_TIMESTAMP WHERE n.id = :id AND n.userId = :userId")
    int archive(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE NotificationQueue n SET n.status = 'ARCHIVED', n.archivedAt = CURRENT_TIMESTAMP WHERE n.userId = :userId AND n.id IN :ids")
    int archiveMany(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    @Modifying
    @Query("UPDATE NotificationQueue n SET n.status = 'ARCHIVED', n.archivedAt = CURRENT_TIMESTAMP WHERE n.createdAt < :cutoff AND n.status != 'ARCHIVED' AND n.archivedAt IS NULL")
    int autoArchiveOlderThan(@Param("cutoff") java.time.LocalDateTime cutoff);

    @Modifying
    @Query("DELETE FROM NotificationQueue n WHERE n.id = :id AND n.userId = :userId")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM NotificationQueue n WHERE n.userId = :userId AND n.id IN :ids")
    int deleteManyByUserId(@Param("userId") Long userId, @Param("ids") List<Long> ids);
}
