package com.example.Cricbook.repository;

import com.example.Cricbook.entity.OwnerVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OwnerVerificationRepository extends JpaRepository<OwnerVerification, Long> {
    List<OwnerVerification> findByOwnerId(Long ownerId);
}
