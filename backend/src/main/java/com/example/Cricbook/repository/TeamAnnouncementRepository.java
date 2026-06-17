package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TeamAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamAnnouncementRepository extends JpaRepository<TeamAnnouncement, Long> {
    List<TeamAnnouncement> findByTeamIdOrderByPinnedDescCreatedAtDesc(Long teamId);
}
