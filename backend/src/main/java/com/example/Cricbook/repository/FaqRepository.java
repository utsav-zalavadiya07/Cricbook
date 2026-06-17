package com.example.Cricbook.repository;

import com.example.Cricbook.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Long> {
    List<Faq> findAllByOrderByUpdatedAtDesc();
}
