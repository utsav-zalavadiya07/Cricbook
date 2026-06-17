package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CmsContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CmsContentRepository extends JpaRepository<CmsContent, Long> {
    List<CmsContent> findByTypeAndStatus(String type, String status);
    List<CmsContent> findByStatus(String status);
}
