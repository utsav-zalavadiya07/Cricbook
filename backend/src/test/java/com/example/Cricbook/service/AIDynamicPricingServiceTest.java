package com.example.Cricbook.service;

import com.example.Cricbook.entity.PricingRule;
import com.example.Cricbook.entity.PricingHistory;
import com.example.Cricbook.entity.Role;
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
import com.example.Cricbook.service.ai.AIDynamicPricingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AIDynamicPricingServiceTest {

    @Mock
    private PricingRuleRepository ruleRepository;
    @Mock
    private PricingHistoryRepository historyRepository;
    @Mock
    private TurfRepository turfRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private HolidayMasterRepository holidayMasterRepository;
    @Mock
    private HolidayPricingRuleRepository holidayPricingRuleRepository;

    @InjectMocks
    private AIDynamicPricingService dynamicPricingService;

    @Test
    void testCalculateDynamicPrice_WeekendAndPeakHour() {
        Long turfId = 1L;
        Long slotId = 10L;
        BigDecimal basePrice = BigDecimal.valueOf(1000);

        // Saturday at 7 PM (19:00) - Both Weekend and Peak Hour
        LocalDate weekendDate = LocalDate.of(2026, 5, 30); // Saturday
        LocalTime peakTime = LocalTime.of(19, 0);

        PricingRule weekendRule = PricingRule.builder()
                .turfId(turfId)
                .ruleName("WEEKEND")
                .adjustmentPercent(BigDecimal.valueOf(20))
                .isActive(true)
                .build();

        PricingRule peakRule = PricingRule.builder()
                .turfId(turfId)
                .ruleName("PEAK_HOUR")
                .adjustmentPercent(BigDecimal.valueOf(15))
                .isActive(true)
                .build();

        when(ruleRepository.findByTurfIdAndRuleName(turfId, "WEEKEND")).thenReturn(Optional.of(weekendRule));
        when(ruleRepository.findByTurfIdAndRuleName(turfId, "PEAK_HOUR")).thenReturn(Optional.of(peakRule));

        BigDecimal finalPrice = dynamicPricingService.calculateDynamicPrice(turfId, slotId, basePrice, weekendDate, peakTime);

        // Base + 20% (Weekend) + 15% (Peak) = 1000 + 200 + 150 = 1350
        assertEquals(0, BigDecimal.valueOf(1350).compareTo(finalPrice));
        verify(historyRepository, times(1)).save(any(PricingHistory.class));
    }

    @Test
    void testCalculateDynamicPrice_WeekdayOffPeak() {
        Long turfId = 1L;
        Long slotId = 10L;
        BigDecimal basePrice = BigDecimal.valueOf(1000);

        // Tuesday at 10 AM (10:00) - Weekday Off Peak
        LocalDate weekdayDate = LocalDate.of(2026, 5, 26); // Tuesday
        LocalTime offPeakTime = LocalTime.of(10, 0);

        BigDecimal finalPrice = dynamicPricingService.calculateDynamicPrice(turfId, slotId, basePrice, weekdayDate, offPeakTime);

        assertEquals(0, basePrice.compareTo(finalPrice));
        verify(historyRepository, never()).save(any(PricingHistory.class));
    }

    @Test
    void toggleRuleRequiresOwnerAccessAndUpdatesPercent() {
        User owner = owner(1L, "owner@example.com", RoleName.ROLE_OWNER);
        Turf turf = Turf.builder().id(10L).owner(owner).build();
        PricingRule rule = PricingRule.builder()
                .turfId(10L)
                .ruleName("PEAK_HOUR")
                .adjustmentPercent(BigDecimal.valueOf(15))
                .isActive(true)
                .build();

        when(turfRepository.findById(10L)).thenReturn(Optional.of(turf));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(ruleRepository.findByTurfIdAndRuleName(10L, "PEAK_HOUR")).thenReturn(Optional.of(rule));

        dynamicPricingService.toggleRule("owner@example.com", 10L, "peak_hour", false, BigDecimal.valueOf(25));

        assertFalse(rule.getIsActive());
        assertEquals(0, BigDecimal.valueOf(25).compareTo(rule.getAdjustmentPercent()));
        verify(ruleRepository).save(rule);
    }

    @Test
    void toggleRuleBlocksAnotherOwnerTurf() {
        User owner = owner(1L, "owner@example.com", RoleName.ROLE_OWNER);
        User otherOwner = owner(2L, "other@example.com", RoleName.ROLE_OWNER);
        Turf turf = Turf.builder().id(10L).owner(owner).build();

        when(turfRepository.findById(10L)).thenReturn(Optional.of(turf));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherOwner));

        ApiException exception = assertThrows(ApiException.class,
                () -> dynamicPricingService.toggleRule("other@example.com", 10L, "WEEKEND", true, BigDecimal.valueOf(20)));

        assertEquals("You do not have permission to manage pricing for this turf.", exception.getMessage());
        verify(ruleRepository, never()).save(any(PricingRule.class));
    }

    @Test
    void toggleRuleRejectsUnsupportedRuleAndUnsafePercent() {
        User owner = owner(1L, "owner@example.com", RoleName.ROLE_OWNER);
        Turf turf = Turf.builder().id(10L).owner(owner).build();

        when(turfRepository.findById(10L)).thenReturn(Optional.of(turf));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));

        ApiException unsupported = assertThrows(ApiException.class,
                () -> dynamicPricingService.toggleRule("owner@example.com", 10L, "HOLIDAY", true, BigDecimal.valueOf(20)));
        assertEquals("Pricing rule must be PEAK_HOUR or WEEKEND.", unsupported.getMessage());

        ApiException unsafePercent = assertThrows(ApiException.class,
                () -> dynamicPricingService.toggleRule("owner@example.com", 10L, "WEEKEND", true, BigDecimal.valueOf(300)));
        assertEquals("Adjustment percent must be between -90 and 200.", unsafePercent.getMessage());
    }

    @Test
    void testCalculateDynamicPrice_AutoHolidayPercentage() {
        Long turfId = 1L;
        Long slotId = 10L;
        BigDecimal basePrice = BigDecimal.valueOf(1000);
        LocalDate holidayDate = LocalDate.of(2026, 10, 2); // Gandhi Jayanti (Friday)
        LocalTime startTime = LocalTime.of(10, 0);

        Turf turf = Turf.builder()
                .id(turfId)
                .country("India")
                .state("Gujarat")
                .city("Ahmedabad")
                .build();

        com.example.Cricbook.entity.HolidayMaster holiday = com.example.Cricbook.entity.HolidayMaster.builder()
                .holidayName("Gandhi Jayanti")
                .holidayDate(holidayDate)
                .country("India")
                .state("Gujarat")
                .city("Ahmedabad")
                .holidayType("NATIONAL")
                .isActive(true)
                .build();

        com.example.Cricbook.entity.HolidayPricingRule rule = com.example.Cricbook.entity.HolidayPricingRule.builder()
                .turfId(turfId)
                .autoApply(true)
                .pricingMode("PERCENTAGE")
                .value(BigDecimal.valueOf(50)) // 50% increase
                .applyTo("ALL")
                .isActive(true)
                .build();

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(turf));
        when(holidayMasterRepository.findMatchingHolidays(holidayDate, "India", "Gujarat", "Ahmedabad"))
                .thenReturn(java.util.List.of(holiday));
        when(holidayPricingRuleRepository.findByTurfId(turfId)).thenReturn(Optional.of(rule));

        BigDecimal finalPrice = dynamicPricingService.calculateDynamicPrice(turfId, slotId, basePrice, holidayDate, startTime);

        // 1000 + 50% = 1500
        assertEquals(0, BigDecimal.valueOf(1500).compareTo(finalPrice));
        verify(historyRepository, times(1)).save(any(PricingHistory.class));
    }

    @Test
    void testCalculateDynamicPrice_AutoHolidayMultiplier() {
        Long turfId = 1L;
        Long slotId = 10L;
        BigDecimal basePrice = BigDecimal.valueOf(1000);
        LocalDate holidayDate = LocalDate.of(2026, 10, 2); // Gandhi Jayanti (Friday)
        LocalTime startTime = LocalTime.of(10, 0);

        Turf turf = Turf.builder().id(turfId).country("India").state("Gujarat").city("Ahmedabad").build();
        com.example.Cricbook.entity.HolidayMaster holiday = com.example.Cricbook.entity.HolidayMaster.builder()
                .holidayName("Gandhi Jayanti").holidayDate(holidayDate).country("India").state("Gujarat").city("Ahmedabad").holidayType("NATIONAL").isActive(true).build();

        com.example.Cricbook.entity.HolidayPricingRule rule = com.example.Cricbook.entity.HolidayPricingRule.builder()
                .turfId(turfId)
                .autoApply(true)
                .pricingMode("MULTIPLIER")
                .value(BigDecimal.valueOf(1.3)) // 1.3x multiplier
                .applyTo("ALL")
                .isActive(true)
                .build();

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(turf));
        when(holidayMasterRepository.findMatchingHolidays(holidayDate, "India", "Gujarat", "Ahmedabad"))
                .thenReturn(java.util.List.of(holiday));
        when(holidayPricingRuleRepository.findByTurfId(turfId)).thenReturn(Optional.of(rule));

        BigDecimal finalPrice = dynamicPricingService.calculateDynamicPrice(turfId, slotId, basePrice, holidayDate, startTime);

        // 1000 * 1.3 = 1300
        assertEquals(0, BigDecimal.valueOf(1300).compareTo(finalPrice));
    }

    @Test
    void testCalculateDynamicPrice_AutoHolidayFixed() {
        Long turfId = 1L;
        Long slotId = 10L;
        BigDecimal basePrice = BigDecimal.valueOf(1000);
        LocalDate holidayDate = LocalDate.of(2026, 10, 2); // Gandhi Jayanti (Friday)
        LocalTime startTime = LocalTime.of(10, 0);

        Turf turf = Turf.builder().id(turfId).country("India").state("Gujarat").city("Ahmedabad").build();
        com.example.Cricbook.entity.HolidayMaster holiday = com.example.Cricbook.entity.HolidayMaster.builder()
                .holidayName("Gandhi Jayanti").holidayDate(holidayDate).country("India").state("Gujarat").city("Ahmedabad").holidayType("NATIONAL").isActive(true).build();

        com.example.Cricbook.entity.HolidayPricingRule rule = com.example.Cricbook.entity.HolidayPricingRule.builder()
                .turfId(turfId)
                .autoApply(true)
                .pricingMode("FIXED")
                .value(BigDecimal.valueOf(2200)) // Rs 2200 fixed price
                .applyTo("ALL")
                .isActive(true)
                .build();

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(turf));
        when(holidayMasterRepository.findMatchingHolidays(holidayDate, "India", "Gujarat", "Ahmedabad"))
                .thenReturn(java.util.List.of(holiday));
        when(holidayPricingRuleRepository.findByTurfId(turfId)).thenReturn(Optional.of(rule));

        BigDecimal finalPrice = dynamicPricingService.calculateDynamicPrice(turfId, slotId, basePrice, holidayDate, startTime);

        // Should be exactly 2200
        assertEquals(0, BigDecimal.valueOf(2200).compareTo(finalPrice));
    }

    @Test
    void testCalculateDynamicPrice_AutoHolidayExcluded() {
        Long turfId = 1L;
        Long slotId = 10L;
        BigDecimal basePrice = BigDecimal.valueOf(1000);
        LocalDate holidayDate = LocalDate.of(2026, 10, 2); // Gandhi Jayanti (Friday)
        LocalTime startTime = LocalTime.of(10, 0);

        Turf turf = Turf.builder().id(turfId).country("India").state("Gujarat").city("Ahmedabad").build();
        com.example.Cricbook.entity.HolidayMaster holiday = com.example.Cricbook.entity.HolidayMaster.builder()
                .holidayName("Gandhi Jayanti").holidayDate(holidayDate).country("India").state("Gujarat").city("Ahmedabad").holidayType("NATIONAL").isActive(true).build();

        java.util.Set<String> exclusions = new java.util.HashSet<>();
        exclusions.add("Gandhi Jayanti");

        com.example.Cricbook.entity.HolidayPricingRule rule = com.example.Cricbook.entity.HolidayPricingRule.builder()
                .turfId(turfId)
                .autoApply(true)
                .pricingMode("PERCENTAGE")
                .value(BigDecimal.valueOf(50))
                .excludedHolidays(exclusions)
                .applyTo("ALL")
                .isActive(true)
                .build();

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(turf));
        when(holidayMasterRepository.findMatchingHolidays(holidayDate, "India", "Gujarat", "Ahmedabad"))
                .thenReturn(java.util.List.of(holiday));
        when(holidayPricingRuleRepository.findByTurfId(turfId)).thenReturn(Optional.of(rule));

        BigDecimal finalPrice = dynamicPricingService.calculateDynamicPrice(turfId, slotId, basePrice, holidayDate, startTime);

        // Price should remain basePrice because it's excluded
        assertEquals(0, basePrice.compareTo(finalPrice));
    }

    private User owner(Long id, String email, RoleName roleName) {
        User user = User.builder().id(id).email(email).build();
        user.getRoles().add(new Role(id, roleName));
        return user;
    }
}
