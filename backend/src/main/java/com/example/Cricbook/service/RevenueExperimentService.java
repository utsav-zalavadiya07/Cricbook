package com.example.Cricbook.service;

import com.example.Cricbook.entity.RevenueExperiment;
import com.example.Cricbook.repository.RevenueExperimentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueExperimentService {

    private final RevenueExperimentRepository experimentRepository;

    @Transactional
    public String getAssignedVariant(Long userId) {
        // Simple deterministic bucketing
        return (userId % 2 == 0) ? "B" : "A";
    }

    @Transactional
    public void trackImpression(String variant) {
        String name = "PRICING_TEST";
        RevenueExperiment exp = experimentRepository.findByExperimentNameAndVariant(name, variant)
                .orElseGet(() -> RevenueExperiment.builder()
                        .experimentName(name)
                        .variant(variant)
                        .impressions(0)
                        .conversions(0)
                        .conversionRate(BigDecimal.ZERO)
                        .build());

        exp.setImpressions(exp.getImpressions() + 1);
        updateConversionRate(exp);
        experimentRepository.save(exp);
        log.info("A/B Pricing Test: Recorded impression for Variant {}", variant);
    }

    @Transactional
    public void trackConversion(String variant) {
        String name = "PRICING_TEST";
        RevenueExperiment exp = experimentRepository.findByExperimentNameAndVariant(name, variant)
                .orElseGet(() -> RevenueExperiment.builder()
                        .experimentName(name)
                        .variant(variant)
                        .impressions(1) // Default to 1 to avoid division by zero
                        .conversions(0)
                        .conversionRate(BigDecimal.ZERO)
                        .build());

        exp.setConversions(exp.getConversions() + 1);
        updateConversionRate(exp);
        experimentRepository.save(exp);
        log.info("A/B Pricing Test: Recorded conversion for Variant {}", variant);
    }

    private void updateConversionRate(RevenueExperiment exp) {
        if (exp.getImpressions() > 0) {
            BigDecimal rate = BigDecimal.valueOf(exp.getConversions())
                    .divide(BigDecimal.valueOf(exp.getImpressions()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            exp.setConversionRate(rate);
        } else {
            exp.setConversionRate(BigDecimal.ZERO);
        }
    }
}
