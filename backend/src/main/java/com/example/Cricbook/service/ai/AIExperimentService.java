package com.example.Cricbook.service.ai;

import com.example.Cricbook.entity.RevenueExperiment;
import com.example.Cricbook.repository.RevenueExperimentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIExperimentService {

    private final RevenueExperimentRepository experimentRepository;

    public String getVariantForUser(Long userId, String experimentName) {
        // Hash-based variant split for deterministic A/B assignment
        String variant = (userId % 2 == 0) ? "A" : "B";
        log.info("A/B experiment assignment for user {}: experiment='{}', variant='{}'", userId, experimentName, variant);
        return variant;
    }

    @Transactional
    public void recordImpression(String experimentName, String variant) {
        log.info("Recording impression for experiment: '{}', variant: '{}'", experimentName, variant);
        RevenueExperiment exp = getOrCreateExperiment(experimentName, variant);
        exp.setImpressions(exp.getImpressions() + 1);
        updateConversionRate(exp);
        experimentRepository.save(exp);
    }

    @Transactional
    public void recordConversion(String experimentName, String variant) {
        log.info("Recording conversion for experiment: '{}', variant: '{}'", experimentName, variant);
        RevenueExperiment exp = getOrCreateExperiment(experimentName, variant);
        exp.setConversions(exp.getConversions() + 1);
        updateConversionRate(exp);
        experimentRepository.save(exp);
    }

    private RevenueExperiment getOrCreateExperiment(String name, String variant) {
        return experimentRepository.findByExperimentNameAndVariant(name, variant)
                .orElseGet(() -> experimentRepository.save(RevenueExperiment.builder()
                        .experimentName(name)
                        .variant(variant)
                        .impressions(0)
                        .conversions(0)
                        .conversionRate(BigDecimal.ZERO)
                        .build()));
    }

    private void updateConversionRate(RevenueExperiment exp) {
        if (exp.getImpressions() == 0) {
            exp.setConversionRate(BigDecimal.ZERO);
            return;
        }
        double rate = ((double) exp.getConversions() / exp.getImpressions()) * 100;
        exp.setConversionRate(BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_UP));
    }

    public List<RevenueExperiment> getAllExperiments() {
        return experimentRepository.findAll();
    }
}
