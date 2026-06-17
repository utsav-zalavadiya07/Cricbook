package com.example.Cricbook.repository;

import com.example.Cricbook.entity.AcademyCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademyCertificateRepository extends JpaRepository<AcademyCertificate, Long> {
    Optional<AcademyCertificate> findByCertificateCodeIgnoreCase(String certificateCode);
    List<AcademyCertificate> findAllByOrderByIssuedAtDesc();
}
