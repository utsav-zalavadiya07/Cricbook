package com.example.Cricbook.repository;

import com.example.Cricbook.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByCaptainIdOrderByCreatedAtDesc(Long captainId);
    boolean existsByNameIgnoreCase(String name);

    @Query("""
            SELECT t FROM Team t
            WHERE t.visibility IN :visibilities
            AND (:query IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(t.captain.name) LIKE LOWER(CONCAT('%', :query, '%')))
            AND (:city IS NULL OR LOWER(t.city) LIKE LOWER(CONCAT('%', :city, '%')))
            ORDER BY t.createdAt DESC
            """)
    List<Team> discoverTeams(
            @Param("query") String query,
            @Param("city") String city,
            @Param("visibilities") List<com.example.Cricbook.enums.TeamVisibility> visibilities);
}
