package com.example.Cricbook.repository;

import com.example.Cricbook.entity.BookingTemplate;
import com.example.Cricbook.enums.BookingTemplateStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingTemplateRepository extends JpaRepository<BookingTemplate, Long> {
    List<BookingTemplate> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, BookingTemplateStatus status);
}
