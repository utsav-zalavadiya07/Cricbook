package com.example.Cricbook.repository;

import com.example.Cricbook.entity.PasswordResetToken;
import com.example.Cricbook.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHashAndUsedFalse(String tokenHash);
    void deleteByExpiryBefore(LocalDateTime time);
    void deleteByUser(User user);
}
