package com.example.Cricbook.repository;

import com.example.Cricbook.entity.Otp;
import com.example.Cricbook.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findByUserAndVerifiedFalse(User user);
    Optional<Otp> findTopByUserIdOrderByIdDesc(Long userId);
    void deleteByExpiryBefore(java.time.LocalDateTime time);
    void deleteByUser(com.example.Cricbook.entity.User user);
}
