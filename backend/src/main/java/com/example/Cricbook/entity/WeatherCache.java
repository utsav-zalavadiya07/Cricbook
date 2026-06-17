package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weather_cache", uniqueConstraints = {
        @UniqueConstraint(name = "uk_weather_city_date_hour", columnNames = {"city", "forecast_date", "forecast_hour"})
}, indexes = {
        @Index(name = "idx_weather_city_date", columnList = "city, forecast_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherCache {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String city;

    @Column(name = "forecast_date", nullable = false)
    private LocalDate forecastDate;

    @Column(name = "forecast_hour", nullable = false)
    private Integer forecastHour;

    @Column(nullable = false)
    private Double temperature;

    @Column(name = "rain_probability", nullable = false)
    private Double rainProbability;

    @Column(nullable = false)
    private Double humidity;

    @Column(name = "wind_speed", nullable = false)
    private Double windSpeed;

    @Column(name = "uv_index", nullable = false)
    private Double uvIndex;

    @Column(length = 500)
    private String summary;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
