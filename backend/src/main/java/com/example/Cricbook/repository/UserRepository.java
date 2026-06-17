package com.example.Cricbook.repository;

import com.example.Cricbook.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByPhone(String phone);
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:loginId) OR u.phone = :loginId")
    Optional<User> findByEmailOrPhone(@org.springframework.data.repository.query.Param("loginId") String loginId);
    
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByPhone(String phone);
}
