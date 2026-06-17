package com.example.Cricbook.repository;

import com.example.Cricbook.entity.WeatherCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeatherCacheRepository extends JpaRepository<WeatherCache, Long> {
    Optional<WeatherCache> findByCityIgnoreCaseAndForecastDateAndForecastHour(String city, LocalDate forecastDate, Integer forecastHour);
    List<WeatherCache> findByCityIgnoreCaseAndForecastDateOrderByForecastHourAsc(String city, LocalDate forecastDate);
}
