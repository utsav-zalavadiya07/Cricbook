package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CityConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityConfigRepository extends JpaRepository<CityConfig, Long> {
    List<CityConfig> findByActiveTrueOrderByCityAsc();
    boolean existsByCityAndCountry(String city, String country);
}
