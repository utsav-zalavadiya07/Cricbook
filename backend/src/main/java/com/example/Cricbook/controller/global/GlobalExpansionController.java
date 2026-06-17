package com.example.Cricbook.controller.global;

import com.example.Cricbook.dto.CityConfigResponse;
import com.example.Cricbook.entity.ExchangeRate;
import com.example.Cricbook.entity.Translation;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.ExchangeRateRepository;
import com.example.Cricbook.repository.TranslationRepository;
import com.example.Cricbook.service.GlobalizationService;
import com.example.Cricbook.service.global.GlobalSportsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/global")
@RequiredArgsConstructor
public class GlobalExpansionController {

    private final GlobalSportsService globalSportsService;
    private final GlobalizationService globalizationService;
    private final TranslationRepository translationRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    @GetMapping("/configs")
    public ResponseEntity<List<CityConfigResponse>> getExpansionConfigs() {
        globalSportsService.seedDefaultData();
        return ResponseEntity.ok(globalizationService.getActiveCities());
    }

    @GetMapping("/translations")
    public ResponseEntity<Map<String, String>> getTranslations(@RequestParam("lang") String lang) {
        if (lang == null || lang.isBlank()) {
            throw ApiException.badRequest("Language parameter is required.");
        }

        globalSportsService.seedDefaultData();
        List<Translation> translations = translationRepository.findByLocale(lang.toLowerCase().trim());
        Map<String, String> translationMap = translations.stream()
                .collect(Collectors.toMap(Translation::getTranslationKey, Translation::getTranslatedValue, (existing, replacement) -> existing));
        return ResponseEntity.ok(translationMap);
    }

    @GetMapping("/exchange-rates")
    public ResponseEntity<List<ExchangeRate>> getExchangeRates() {
        globalSportsService.seedDefaultData();
        return ResponseEntity.ok(exchangeRateRepository.findAll());
    }

    @PostMapping("/seed")
    public ResponseEntity<Map<String, String>> seedConfigs() {
        globalSportsService.seedDefaultData();
        return ResponseEntity.ok(Map.of("message", "Global expansion configuration seeded successfully."));
    }
}
