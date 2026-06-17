package com.example.Cricbook.repository;

import com.example.Cricbook.entity.Innings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InningsRepository extends JpaRepository<Innings, Long> {
    List<Innings> findByLiveScoreIdOrderByInningsNumberAsc(Long liveScoreId);
}
