package com.example.Cricbook.repository;

import com.example.Cricbook.entity.Sport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SportRepository extends JpaRepository<Sport, Long> {
    Optional<Sport> findByNameIgnoreCase(String name);
    Optional<Sport> findByCodeIgnoreCase(String code);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByCodeIgnoreCase(String code);
    List<Sport> findByIsActiveTrueOrderByDisplayOrderAscNameAsc();
    List<Sport> findAllByOrderByDisplayOrderAscNameAsc();
}
