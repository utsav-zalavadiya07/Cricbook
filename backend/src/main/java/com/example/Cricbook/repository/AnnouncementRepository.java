package com.example.Cricbook.repository;

import com.example.Cricbook.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findAllByOrderByCreatedAtDesc();
    List<Announcement> findByStatusInOrderByCreatedAtDesc(List<String> statuses);
}
