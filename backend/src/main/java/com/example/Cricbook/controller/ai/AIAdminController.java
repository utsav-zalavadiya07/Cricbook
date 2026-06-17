package com.example.Cricbook.controller.ai;

import com.example.Cricbook.entity.FraudAlert;
import com.example.Cricbook.entity.ChurnScore;
import com.example.Cricbook.entity.RevenueExperiment;
import com.example.Cricbook.repository.ChurnScoreRepository;
import com.example.Cricbook.service.ai.AIFraudService;
import com.example.Cricbook.service.ai.AIExperimentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/ai")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasAuthority('AI_INSIGHTS_VIEW') or hasAuthority('ANALYTICS_VIEW')")
public class AIAdminController {

    private final AIFraudService fraudService;
    private final AIExperimentService experimentService;
    private final ChurnScoreRepository churnScoreRepository;

    @GetMapping("/fraud")
    public ResponseEntity<List<FraudAlert>> getFraudAlerts() {
        return ResponseEntity.ok(fraudService.getAllAlerts());
    }

    @GetMapping("/experiments")
    public ResponseEntity<List<RevenueExperiment>> getExperiments() {
        return ResponseEntity.ok(experimentService.getAllExperiments());
    }

    @GetMapping("/churn")
    public ResponseEntity<List<ChurnScore>> getChurnScores() {
        List<ChurnScore> scores = churnScoreRepository.findAll();
        if (scores.isEmpty()) {
            // Seed a few mock churn scores for layout verification
            churnScoreRepository.save(ChurnScore.builder().userId(1L).churnProbability(BigDecimal.valueOf(0.85)).riskLevel("HIGH").build());
            churnScoreRepository.save(ChurnScore.builder().userId(2L).churnProbability(BigDecimal.valueOf(0.45)).riskLevel("MEDIUM").build());
            churnScoreRepository.save(ChurnScore.builder().userId(3L).churnProbability(BigDecimal.valueOf(0.12)).riskLevel("LOW").build());
            scores = churnScoreRepository.findAll();
        }
        return ResponseEntity.ok(scores);
    }

    @PostMapping("/experiments/record")
    public ResponseEntity<?> recordMetric(@RequestBody Map<String, String> request) {
        String experimentName = request.get("experimentName");
        String variant = request.get("variant");
        String metricType = request.get("metricType"); // IMPRESSION or CONVERSION

        if ("IMPRESSION".equalsIgnoreCase(metricType)) {
            experimentService.recordImpression(experimentName, variant);
        } else if ("CONVERSION".equalsIgnoreCase(metricType)) {
            experimentService.recordConversion(experimentName, variant);
        }

        return ResponseEntity.ok(Map.of("message", "Metric recorded successfully"));
    }
}
