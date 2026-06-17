package com.example.Cricbook.service;

import com.example.Cricbook.dto.CityConfigRequest;
import com.example.Cricbook.dto.CityConfigResponse;
import com.example.Cricbook.entity.CityConfig;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.CityConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GlobalizationService {

    private final CityConfigRepository cityConfigRepository;

    @Transactional(readOnly = true)
    public List<CityConfigResponse> getActiveCities() {
        return cityConfigRepository.findByActiveTrueOrderByCityAsc().stream().map(this::map).toList();
    }

    @Transactional
    public CityConfigResponse createCity(CityConfigRequest request) {
        if (request == null) {
            throw new ApiException("City details are required");
        }
        String city = required(request.getCity(), "City is required");
        String country = required(request.getCountry(), "Country is required");
        if (cityConfigRepository.existsByCityAndCountry(city, country)) {
            throw new ApiException("City already exists");
        }
        CityConfig config = cityConfigRepository.save(CityConfig.builder()
                .city(city)
                .country(country)
                .currency(defaultValue(request.getCurrency(), "INR"))
                .locale(defaultValue(request.getLocale(), "en-IN"))
                .timeZone(defaultValue(request.getTimeZone(), "Asia/Kolkata"))
                .active(request.getActive() == null || request.getActive())
                .build());
        return map(config);
    }

    private CityConfigResponse map(CityConfig config) {
        return CityConfigResponse.builder()
                .id(config.getId())
                .city(config.getCity())
                .country(config.getCountry())
                .currency(config.getCurrency())
                .locale(config.getLocale())
                .timeZone(config.getTimeZone())
                .active(config.getActive())
                .build();
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(message);
        }
        return value.trim();
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
