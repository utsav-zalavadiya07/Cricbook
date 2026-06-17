package com.example.Cricbook.service;

import com.example.Cricbook.entity.HolidayMaster;
import com.example.Cricbook.entity.HolidayPricingRule;
import com.example.Cricbook.entity.Slot;
import com.example.Cricbook.entity.Turf;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.RoleName;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.HolidayMasterRepository;
import com.example.Cricbook.repository.HolidayPricingRuleRepository;
import com.example.Cricbook.repository.SlotRepository;
import com.example.Cricbook.repository.TurfRepository;
import com.example.Cricbook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class HolidayService {
    private static final Set<String> SUPPORTED_PRICING_MODES = Set.of("PERCENTAGE", "FIXED", "MULTIPLIER");
    private static final Set<String> SUPPORTED_APPLY_TO = Set.of("ALL", "SELECTED");
    private static final BigDecimal MAX_RULE_VALUE = BigDecimal.valueOf(100000);

    private final HolidayMasterRepository holidayMasterRepository;
    private final HolidayPricingRuleRepository holidayPricingRuleRepository;
    private final TurfRepository turfRepository;
    private final SlotRepository slotRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public HolidayPricingRule getRule(Long turfId, String username) {
        requireTurfAccess(turfId, username);
        return holidayPricingRuleRepository.findByTurfId(turfId)
                .orElseGet(() -> HolidayPricingRule.builder()
                        .turfId(turfId)
                        .autoApply(false)
                        .pricingMode("PERCENTAGE")
                        .value(java.math.BigDecimal.ZERO)
                        .applyTo("ALL")
                        .isActive(true)
                        .build());
    }

    @Transactional
    public HolidayPricingRule saveRule(Long turfId, String username, HolidayPricingRule ruleRequest) {
        Turf turf = requireTurfAccess(turfId, username);
        validateRuleRequest(ruleRequest);

        String pricingMode = normalize(ruleRequest.getPricingMode(), "PERCENTAGE");
        String applyTo = normalize(ruleRequest.getApplyTo(), "ALL");
        Set<Long> slotIds = applyTo.equals("SELECTED") ? validateSelectedSlots(turf, ruleRequest.getSlotIds()) : Set.of();

        HolidayPricingRule rule = holidayPricingRuleRepository.findByTurfId(turfId)
                .orElse(HolidayPricingRule.builder().turfId(turfId).build());

        rule.setAutoApply(Boolean.TRUE.equals(ruleRequest.getAutoApply()));
        rule.setPricingMode(pricingMode);
        rule.setValue(ruleRequest.getValue());
        rule.setApplyTo(applyTo);
        rule.setIsActive(ruleRequest.getIsActive() == null || Boolean.TRUE.equals(ruleRequest.getIsActive()));
        rule.setExcludedHolidays(ruleRequest.getExcludedHolidays() == null ? new HashSet<>() : new HashSet<>(ruleRequest.getExcludedHolidays()));
        rule.setSlotIds(new HashSet<>(slotIds));

        return holidayPricingRuleRepository.save(rule);
    }

    @Transactional
    public List<HolidayMaster> getUpcomingHolidays(Long turfId, String username) {
        Turf turf = requireTurfAccess(turfId, username);

        String country = turf.getCountry() != null && !turf.getCountry().isBlank() ? turf.getCountry() : "India";
        String state = turf.getState() != null && !turf.getState().isBlank() ? turf.getState() : "Gujarat";
        String city = turf.getCity() != null && !turf.getCity().isBlank() ? turf.getCity() : "Ahmedabad";

        // Seed if empty
        if (holidayMasterRepository.count() == 0) {
            seedDefaultHolidays();
        }

        return holidayMasterRepository.findUpcomingHolidays(LocalDate.now(), country, state, city);
    }

    @Transactional
    public void seedDefaultHolidays() {
        if (holidayMasterRepository.count() > 0) {
            return;
        }

        log.info("Seeding default holidays into HolidayMaster table...");

        // India 2026 standard holidays
        List<HolidayMaster> defaults = List.of(
            // National Holidays
            HolidayMaster.builder().country("India").holidayName("Republic Day").holidayDate(LocalDate.of(2026, 1, 26)).holidayType("NATIONAL").isActive(true).build(),
            HolidayMaster.builder().country("India").holidayName("Independence Day").holidayDate(LocalDate.of(2026, 8, 15)).holidayType("NATIONAL").isActive(true).build(),
            HolidayMaster.builder().country("India").holidayName("Gandhi Jayanti").holidayDate(LocalDate.of(2026, 10, 2)).holidayType("NATIONAL").isActive(true).build(),

            // Festivals
            HolidayMaster.builder().country("India").holidayName("Diwali").holidayDate(LocalDate.of(2026, 11, 8)).holidayType("FESTIVAL").isActive(true).build(),
            HolidayMaster.builder().country("India").holidayName("Holi").holidayDate(LocalDate.of(2026, 3, 4)).holidayType("FESTIVAL").isActive(true).build(),
            HolidayMaster.builder().country("India").holidayName("Janmashtami").holidayDate(LocalDate.of(2026, 9, 3)).holidayType("FESTIVAL").isActive(true).build(),
            HolidayMaster.builder().country("India").holidayName("Christmas").holidayDate(LocalDate.of(2026, 12, 25)).holidayType("FESTIVAL").isActive(true).build(),
            HolidayMaster.builder().country("India").holidayName("New Year").holidayDate(LocalDate.of(2026, 1, 1)).holidayType("FESTIVAL").isActive(true).build(),

            // State Holidays - Gujarat
            HolidayMaster.builder().country("India").state("Gujarat").holidayName("Gujarat Statehood Day").holidayDate(LocalDate.of(2026, 5, 1)).holidayType("STATE").isActive(true).build(),
            HolidayMaster.builder().country("India").state("Gujarat").holidayName("Uttarayan").holidayDate(LocalDate.of(2026, 1, 14)).holidayType("STATE").isActive(true).build()
        );

        holidayMasterRepository.saveAll(defaults);
    }

    private void validateRuleRequest(HolidayPricingRule ruleRequest) {
        if (ruleRequest == null) {
            throw ApiException.badRequest("Holiday pricing rule details are required.");
        }
        String pricingMode = normalize(ruleRequest.getPricingMode(), "PERCENTAGE");
        if (!SUPPORTED_PRICING_MODES.contains(pricingMode)) {
            throw ApiException.badRequest("Pricing mode must be PERCENTAGE, FIXED, or MULTIPLIER.");
        }
        String applyTo = normalize(ruleRequest.getApplyTo(), "ALL");
        if (!SUPPORTED_APPLY_TO.contains(applyTo)) {
            throw ApiException.badRequest("Apply-to mode must be ALL or SELECTED.");
        }
        BigDecimal value = ruleRequest.getValue();
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("Holiday pricing value must be greater than zero.");
        }
        if (value.compareTo(MAX_RULE_VALUE) > 0) {
            throw ApiException.badRequest("Holiday pricing value cannot exceed Rs 100000.");
        }
        if ("PERCENTAGE".equals(pricingMode) && value.compareTo(BigDecimal.valueOf(200)) > 0) {
            throw ApiException.badRequest("Holiday percentage increase cannot exceed 200.");
        }
        if ("MULTIPLIER".equals(pricingMode) && value.compareTo(BigDecimal.valueOf(10)) > 0) {
            throw ApiException.badRequest("Holiday multiplier cannot exceed 10.");
        }
    }

    private Set<Long> validateSelectedSlots(Turf turf, Set<Long> requestedSlotIds) {
        if (requestedSlotIds == null || requestedSlotIds.isEmpty()) {
            throw ApiException.badRequest("Select at least one slot for selected-slot holiday pricing.");
        }
        Set<Long> slotIds = new HashSet<>();
        for (Long slotId : requestedSlotIds) {
            if (slotId == null) {
                continue;
            }
            Slot slot = slotRepository.findById(slotId)
                    .orElseThrow(() -> ApiException.notFound("Slot was not found."));
            if (slot.getTurf() == null || !turf.getId().equals(slot.getTurf().getId())) {
                throw ApiException.badRequest("Selected slot does not belong to this turf.");
            }
            slotIds.add(slotId);
        }
        if (slotIds.isEmpty()) {
            throw ApiException.badRequest("Select at least one valid slot.");
        }
        return slotIds;
    }

    private Turf requireTurfAccess(Long turfId, String username) {
        Turf turf = turfRepository.findById(turfId)
                .orElseThrow(() -> ApiException.notFound("Turf not found."));
        User user = findUser(username);
        boolean admin = user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);
        if (!admin && (turf.getOwner() == null || !turf.getOwner().getId().equals(user.getId()))) {
            throw ApiException.forbidden("You do not have permission to manage holiday pricing for this turf.");
        }
        return turf;
    }

    private User findUser(String username) {
        if (username != null && username.contains("@")) {
            return userRepository.findByEmail(username)
                    .orElseGet(() -> userRepository.findByEmailOrPhone(username)
                            .orElseThrow(() -> ApiException.notFound("User account was not found.")));
        }
        return userRepository.findByEmailOrPhone(username)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
