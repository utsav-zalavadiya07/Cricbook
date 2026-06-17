package com.example.Cricbook.repository;

import com.example.Cricbook.entity.WeatherPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WeatherPolicyRepository extends JpaRepository<WeatherPolicy, Long> {
    Optional<WeatherPolicy> findByOwnerId(Long ownerId);
}
