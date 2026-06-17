package com.example.Cricbook.service.ai;

import com.example.Cricbook.entity.PricingRule;
import com.example.Cricbook.entity.PricingHistory;
import com.example.Cricbook.entity.Turf;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.RoleName;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.PricingRuleRepository;
import com.example.Cricbook.repository.PricingHistoryRepository;
import com.example.Cricbook.repository.TurfRepository;
import com.example.Cricbook.repository.UserRepository;
import com.example.Cricbook.repository.HolidayMasterRepository;
import com.example.Cricbook.repository.HolidayPricingRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIDynamicPricingService {
    private static final List<String> SUPPORTED_RULES = List.of("PEAK_HOUR", "WEEKEND");
    private static final BigDecimal MIN_ADJUSTMENT_PERCENT = BigDecimal.valueOf(-90);
    private static final BigDecimal MAX_ADJUSTMENT_PERCENT = BigDecimal.valueOf(200);

    private final PricingRuleRepository ruleRepository;
    private final PricingHistoryRepository historyRepository;
    private final TurfRepository turfRepository;
    private final UserRepository userRepository;
    private final HolidayMasterRepository holidayMasterRepository;
    private final HolidayPricingRuleRepository holidayPricingRuleRepository;

    @Transactional
    public BigDecimal calculateDynamicPrice(Long turfId, Long slotId, BigDecimal basePrice, LocalDate date, LocalTime startTime) {
        if (basePrice == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal adjustedPrice = basePrice;
        StringBuilder adjustmentReason = new StringBuilder();
        boolean readOnlyTransaction = TransactionSynchronizationManager.isCurrentTransactionReadOnly();

        // 0. Check Holiday Pricing Automation Rule
        Optional<Turf> turfOpt = turfRepository.findById(turfId);
        if (turfOpt.isPresent()) {
            Turf turf = turfOpt.get();
            String country = turf.getCountry() != null && !turf.getCountry().isBlank() ? turf.getCountry() : "India";
            String state = turf.getState() != null && !turf.getState().isBlank() ? turf.getState() : "Gujarat";
            String city = turf.getCity() != null && !turf.getCity().isBlank() ? turf.getCity() : "Ahmedabad";

            List<com.example.Cricbook.entity.HolidayMaster> matchingHolidays = 
                    holidayMasterRepository.findMatchingHolidays(date, country, state, city);
            
            if (!matchingHolidays.isEmpty()) {
                com.example.Cricbook.entity.HolidayMaster holiday = matchingHolidays.get(0);
                Optional<com.example.Cricbook.entity.HolidayPricingRule> holidayRuleOpt = 
                        holidayPricingRuleRepository.findByTurfId(turfId);
                
                if (holidayRuleOpt.isPresent()) {
                    com.example.Cricbook.entity.HolidayPricingRule rule = holidayRuleOpt.get();
                    if (Boolean.TRUE.equals(rule.getIsActive()) && Boolean.TRUE.equals(rule.getAutoApply()) 
                            && !rule.getExcludedHolidays().contains(holiday.getHolidayName())) {
                        
                        boolean appliesToSlot = "ALL".equalsIgnoreCase(rule.getApplyTo()) 
                                || ("SELECTED".equalsIgnoreCase(rule.getApplyTo()) && rule.getSlotIds().contains(slotId));
                        
                        if (appliesToSlot) {
                            BigDecimal ruleVal = rule.getValue();
                            if ("PERCENTAGE".equalsIgnoreCase(rule.getPricingMode())) {
                                BigDecimal multiplier = ruleVal.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                                BigDecimal increment = basePrice.multiply(multiplier);
                                adjustedPrice = adjustedPrice.add(increment);
                                adjustmentReason.append("Holiday Markup: ").append(holiday.getHolidayName())
                                        .append(" (+").append(ruleVal).append("%). ");
                            } else if ("MULTIPLIER".equalsIgnoreCase(rule.getPricingMode())) {
                                BigDecimal newPrice = basePrice.multiply(ruleVal);
                                BigDecimal increment = newPrice.subtract(basePrice);
                                adjustedPrice = adjustedPrice.add(increment);
                                adjustmentReason.append("Holiday Multiplier: ").append(holiday.getHolidayName())
                                        .append(" (").append(ruleVal).append("x). ");
                            } else if ("FIXED".equalsIgnoreCase(rule.getPricingMode())) {
                                BigDecimal increment = ruleVal.subtract(basePrice);
                                adjustedPrice = adjustedPrice.add(increment);
                                adjustmentReason.append("Holiday Fixed Price: ").append(holiday.getHolidayName())
                                        .append(" (Rs ").append(ruleVal).append("). ");
                            }
                        }
                    }
                }
            }
        }

        // 1. Check Weekend Adjustment (+20% default)
        boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        if (isWeekend) {
            Optional<PricingRule> weekendRuleOpt = ruleRepository.findByTurfIdAndRuleName(turfId, "WEEKEND");
            PricingRule rule = weekendRuleOpt.orElseGet(() -> defaultPricingRule(turfId, "WEEKEND", BigDecimal.valueOf(20), readOnlyTransaction));

            if (Boolean.TRUE.equals(rule.getIsActive())) {
                BigDecimal multiplier = rule.getAdjustmentPercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                BigDecimal increment = basePrice.multiply(multiplier);
                adjustedPrice = adjustedPrice.add(increment);
                adjustmentReason.append("Weekend Markup (+").append(rule.getAdjustmentPercent()).append("%). ");
            }
        }

        // 2. Check Peak Hour Adjustment (+15% default, 6:00 PM - 10:00 PM)
        boolean isPeakHour = !startTime.isBefore(LocalTime.of(18, 0)) && !startTime.isAfter(LocalTime.of(22, 0));
        if (isPeakHour) {
            Optional<PricingRule> peakRuleOpt = ruleRepository.findByTurfIdAndRuleName(turfId, "PEAK_HOUR");
            PricingRule rule = peakRuleOpt.orElseGet(() -> defaultPricingRule(turfId, "PEAK_HOUR", BigDecimal.valueOf(15), readOnlyTransaction));

            if (Boolean.TRUE.equals(rule.getIsActive())) {
                BigDecimal multiplier = rule.getAdjustmentPercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                BigDecimal increment = basePrice.multiply(multiplier);
                adjustedPrice = adjustedPrice.add(increment);
                adjustmentReason.append("Peak Hour Markup (+").append(rule.getAdjustmentPercent()).append("%). ");
            }
        }

        // Round final price to nearest integer or 2 decimal places
        adjustedPrice = adjustedPrice.setScale(2, RoundingMode.HALF_UP);

        // 3. Log audit history if there is any adjustment
        if (adjustedPrice.compareTo(basePrice) != 0 && !readOnlyTransaction) {
            log.info("Dynamic pricing triggered for turf {}, slot {}: {} -> {} due to: {}", 
                    turfId, slotId, basePrice, adjustedPrice, adjustmentReason.toString().trim());
            
            historyRepository.save(PricingHistory.builder()
                    .turfId(turfId)
                    .slotId(slotId)
                    .originalPrice(basePrice)
                    .dynamicPrice(adjustedPrice)
                    .reason(adjustmentReason.toString().trim())
                    .build());
        }

        return adjustedPrice;
    }

    private PricingRule defaultPricingRule(Long turfId, String ruleName, BigDecimal adjustmentPercent, boolean readOnlyTransaction) {
        PricingRule rule = PricingRule.builder()
                .turfId(turfId)
                .ruleName(ruleName)
                .adjustmentPercent(adjustmentPercent)
                .isActive(true)
                .build();
        if (readOnlyTransaction) {
            return rule;
        }
        PricingRule saved = ruleRepository.save(rule);
        return saved != null ? saved : rule;
    }

    @Transactional
    public void toggleRule(String username, Long turfId, String ruleName, boolean active, BigDecimal adjustmentPercent) {
        requireTurfAccess(username, turfId);
        String normalizedRuleName = normalizeRuleName(ruleName);
        validateAdjustmentPercent(adjustmentPercent);
        log.info("Toggling pricing rule '{}' for turf {} to active={}", normalizedRuleName, turfId, active);
        Optional<PricingRule> ruleOpt = ruleRepository.findByTurfIdAndRuleName(turfId, normalizedRuleName);
        if (ruleOpt.isPresent()) {
            PricingRule rule = ruleOpt.get();
            rule.setIsActive(active);
            if (adjustmentPercent != null) {
                rule.setAdjustmentPercent(adjustmentPercent);
            }
            ruleRepository.save(rule);
        } else {
            BigDecimal percent = adjustmentPercent == null ? defaultPercent(normalizedRuleName) : adjustmentPercent;
            ruleRepository.save(PricingRule.builder()
                    .turfId(turfId)
                    .ruleName(normalizedRuleName)
                    .adjustmentPercent(percent)
                    .isActive(active)
                    .build());
        }
    }

    @Transactional
    public List<PricingRule> getRules(String username, Long turfId) {
        requireTurfAccess(username, turfId);
        return getRules(turfId);
    }

    @Transactional
    public List<PricingRule> getRules(Long turfId) {
        List<PricingRule> rules = ruleRepository.findByTurfId(turfId);
        if (rules.isEmpty()) {
            // Seed rules
            rules = List.of(
                    ruleRepository.save(PricingRule.builder().turfId(turfId).ruleName("PEAK_HOUR").adjustmentPercent(BigDecimal.valueOf(15)).isActive(true).build()),
                    ruleRepository.save(PricingRule.builder().turfId(turfId).ruleName("WEEKEND").adjustmentPercent(BigDecimal.valueOf(20)).isActive(true).build())
            );
        }
        return rules;
    }

    public boolean isSlotHighDemand(Long turfId, Long slotId, LocalDate date, LocalTime startTime) {
        // High demand is weekend evening slot
        boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        boolean isPeakHour = !startTime.isBefore(LocalTime.of(18, 0)) && !startTime.isAfter(LocalTime.of(22, 0));
        return isWeekend && isPeakHour;
    }

    @Transactional(readOnly = true)
    public List<PricingHistory> getPricingHistory(String username, Long turfId) {
        requireTurfAccess(username, turfId);
        return getPricingHistory(turfId);
    }

    @Transactional(readOnly = true)
    public List<PricingHistory> getPricingHistory(Long turfId) {
        return historyRepository.findByTurfIdOrderByCreatedAtDesc(turfId);
    }

    private void requireTurfAccess(String username, Long turfId) {
        if (turfId == null) {
            throw ApiException.badRequest("Turf ID is required.");
        }
        Turf turf = turfRepository.findById(turfId)
                .orElseThrow(() -> ApiException.notFound("Turf was not found."));
        User user = findUser(username);
        boolean admin = user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);
        if (!admin && !turf.getOwner().getId().equals(user.getId())) {
            throw ApiException.forbidden("You do not have permission to manage pricing for this turf.");
        }
    }

    private User findUser(String username) {
        if (username != null && username.contains("@")) {
            Optional<User> byEmail = userRepository.findByEmail(username);
            if (byEmail.isPresent()) {
                return byEmail.get();
            }
        }
        return userRepository.findByEmailOrPhone(username)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
    }

    private String normalizeRuleName(String ruleName) {
        if (ruleName == null || ruleName.isBlank()) {
            throw ApiException.badRequest("Pricing rule name is required.");
        }
        String normalized = ruleName.trim().toUpperCase();
        if (!SUPPORTED_RULES.contains(normalized)) {
            throw ApiException.badRequest("Pricing rule must be PEAK_HOUR or WEEKEND.");
        }
        return normalized;
    }

    private void validateAdjustmentPercent(BigDecimal adjustmentPercent) {
        if (adjustmentPercent == null) {
            return;
        }
        if (adjustmentPercent.compareTo(MIN_ADJUSTMENT_PERCENT) < 0 || adjustmentPercent.compareTo(MAX_ADJUSTMENT_PERCENT) > 0) {
            throw ApiException.badRequest("Adjustment percent must be between -90 and 200.");
        }
    }

    private BigDecimal defaultPercent(String ruleName) {
        return "WEEKEND".equals(ruleName) ? BigDecimal.valueOf(20) : BigDecimal.valueOf(15);
    }
}
