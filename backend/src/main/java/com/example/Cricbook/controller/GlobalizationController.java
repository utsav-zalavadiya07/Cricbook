package com.example.Cricbook.controller;

import com.example.Cricbook.dto.CityConfigRequest;
import com.example.Cricbook.dto.CityConfigResponse;
import com.example.Cricbook.service.GlobalizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/global")
@RequiredArgsConstructor
public class GlobalizationController {

    private final GlobalizationService globalizationService;

    @GetMapping("/cities")
    public ResponseEntity<List<CityConfigResponse>> getActiveCities() {
        return ResponseEntity.ok(globalizationService.getActiveCities());
    }

    @PostMapping("/cities")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CityConfigResponse> createCity(@RequestBody CityConfigRequest request) {
        return ResponseEntity.ok(globalizationService.createCity(request));
    }
}
