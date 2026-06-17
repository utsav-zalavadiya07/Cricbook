package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CsatFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CsatFeedbackRepository extends JpaRepository<CsatFeedback, Long> {
    List<CsatFeedback> findTop50ByOrderByCreatedAtDesc();
}
