package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CmsPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CmsPageRepository extends JpaRepository<CmsPage, Long> {
    Optional<CmsPage> findBySlug(String slug);
    List<CmsPage> findAllByOrderByUpdatedAtDesc();
}
