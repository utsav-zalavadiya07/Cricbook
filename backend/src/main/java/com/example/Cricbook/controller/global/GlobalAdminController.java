package com.example.Cricbook.controller.global;

import com.example.Cricbook.entity.WhiteLabelBranding;
import com.example.Cricbook.repository.WhiteLabelBrandingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class GlobalAdminController {

    private final WhiteLabelBrandingRepository brandingRepository;

    @GetMapping("/expansion/analytics")
    public ResponseEntity<Map<String, Object>> getExpansionAnalytics() {
        Map<String, Object> data = new HashMap<>();
        data.put("activeCountries", List.of("India", "UAE", "UK", "Singapore", "Australia"));
        data.put("activeCitiesCount", 6);
        
        List<Map<String, Object>> cityMetrics = List.of(
                Map.of("city", "Ahmedabad", "revenue", 450000, "bookingsCount", 312, "occupancyRate", "78.2%"),
                Map.of("city", "Mumbai", "revenue", 780000, "bookingsCount", 540, "occupancyRate", "84.5%"),
                Map.of("city", "Dubai", "revenue", 1250000, "bookingsCount", 880, "occupancyRate", "72.4%"),
                Map.of("city", "London", "revenue", 920000, "bookingsCount", 610, "occupancyRate", "68.9%")
        );
        data.put("cityMetrics", cityMetrics);
        
        Map<String, Object> conversions = new HashMap<>();
        conversions.put("VariantA_CTR", "4.8%");
        conversions.put("VariantB_CTR", "6.2%");
        conversions.put("A_B_Winner", "Variant B (Neural Time Alignment)");
        data.put("searchExperiments", conversions);

        return ResponseEntity.ok(data);
    }

    @GetMapping("/whitelabel/branding")
    public ResponseEntity<WhiteLabelBranding> getBranding(@RequestParam("tenantId") Long tenantId) {
        WhiteLabelBranding branding = brandingRepository.findByTenantId(tenantId)
                .orElseGet(() -> WhiteLabelBranding.builder()
                        .tenantId(tenantId)
                        .logoUrl("/assets/brand-default.png")
                        .primaryColor("222.2 47.4% 11.2%") // default HSL dark theme
                        .secondaryColor("142.1 76.2% 36.3%") // emerald
                        .build());
        return ResponseEntity.ok(branding);
    }

    @PostMapping("/whitelabel/branding")
    public ResponseEntity<WhiteLabelBranding> saveBranding(@RequestBody WhiteLabelBranding request) {
        WhiteLabelBranding branding = brandingRepository.findByTenantId(request.getTenantId())
                .orElse(WhiteLabelBranding.builder().tenantId(request.getTenantId()).build());
        
        branding.setLogoUrl(request.getLogoUrl());
        branding.setPrimaryColor(request.getPrimaryColor());
        branding.setSecondaryColor(request.getSecondaryColor());
        
        return ResponseEntity.ok(brandingRepository.save(branding));
    }
}
