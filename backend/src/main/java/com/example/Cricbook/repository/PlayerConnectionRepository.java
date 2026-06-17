package com.example.Cricbook.repository;

import com.example.Cricbook.entity.PlayerConnection;
import com.example.Cricbook.enums.PlayerConnectionStatus;
import com.example.Cricbook.enums.PlayerConnectionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerConnectionRepository extends JpaRepository<PlayerConnection, Long> {
    Optional<PlayerConnection> findByFromUserIdAndToUserIdAndType(Long fromUserId, Long toUserId, PlayerConnectionType type);
    List<PlayerConnection> findByFromUserIdOrToUserIdOrderByCreatedAtDesc(Long fromUserId, Long toUserId);
    long countByStatus(PlayerConnectionStatus status);
    long countByFromUserIdOrToUserId(Long fromUserId, Long toUserId);
}
