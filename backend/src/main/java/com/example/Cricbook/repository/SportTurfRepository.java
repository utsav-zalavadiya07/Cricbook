package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SportTurf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SportTurfRepository extends JpaRepository<SportTurf, Long> {
    List<SportTurf> findByTurfId(Long turfId);
    List<SportTurf> findByTurfIdIn(Collection<Long> turfIds);
    List<SportTurf> findBySportId(Long sportId);
    boolean existsBySportIdAndTurfId(Long sportId, Long turfId);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM SportTurf mapping WHERE mapping.turfId = :turfId")
    void deleteByTurfId(@Param("turfId") Long turfId);
    void deleteBySportIdAndTurfId(Long sportId, Long turfId);
}
