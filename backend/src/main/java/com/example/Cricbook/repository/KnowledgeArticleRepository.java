package com.example.Cricbook.repository;

import com.example.Cricbook.entity.KnowledgeArticle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long> {
    List<KnowledgeArticle> findAllByOrderByUpdatedAtDesc();
}
