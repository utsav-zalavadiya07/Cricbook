package com.example.Cricbook.repository;

import com.example.Cricbook.entity.NotificationPreference;
import com.example.Cricbook.enums.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    List<NotificationPreference> findByUserId(Long userId);
    Optional<NotificationPreference> findByUserIdAndChannel(Long userId, NotificationChannel channel);
}
