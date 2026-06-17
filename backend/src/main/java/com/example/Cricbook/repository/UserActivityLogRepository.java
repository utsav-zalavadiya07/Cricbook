package com.example.Cricbook.repository;

import com.example.Cricbook.entity.UserActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {
    List<UserActivityLog> findTop50ByOrderByCreatedAtDesc();
    List<UserActivityLog> findTop30ByUserIdOrderByCreatedAtDesc(Long userId);
}
