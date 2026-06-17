package com.example.Cricbook.repository;

import com.example.Cricbook.entity.ReleaseVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReleaseVersionRepository extends JpaRepository<ReleaseVersion, Long> {
    Optional<ReleaseVersion> findByVersion(String version);
    List<ReleaseVersion> findAllByOrderByCreatedAtDesc();
}
