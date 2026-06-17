package com.example.Cricbook.repository;

import com.example.Cricbook.entity.RecentSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecentSearchRepository extends JpaRepository<RecentSearch, Long> {
    List<RecentSearch> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
}
