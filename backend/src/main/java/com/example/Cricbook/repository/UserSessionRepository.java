package com.example.Cricbook.repository;

import com.example.Cricbook.entity.User;
import com.example.Cricbook.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findBySessionId(String sessionId);

    Optional<UserSession> findByRefreshToken(String refreshToken);

    Optional<UserSession> findByPreviousRefreshToken(String previousRefreshToken);

    List<UserSession> findByUserAndActiveTrueOrderByLastActivityDesc(User user);

    List<UserSession> findByUserAndActiveTrueOrderByLoginTimeAsc(User user);
}
