package com.example.Cricbook.service;

import com.example.Cricbook.entity.HolidayPricingRule;
import com.example.Cricbook.entity.Role;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {

    @Mock
    private HolidayMasterRepository holidayMasterRepository;
    @Mock
    private HolidayPricingRuleRepository holidayPricingRuleRepository;
    @Mock
    private TurfRepository turfRepository;
    @Mock
    private SlotRepository slotRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private HolidayService holidayService;

    @Test
    void getUpcomingHolidaysUsesTurfLocationAndFiltersFromToday() {
        Long turfId = 10L;
        User owner = owner(1L, "owner@example.com", RoleName.ROLE_OWNER);
        Turf turf = Turf.builder()
                .id(turfId)
                .owner(owner)
                .country("India")
                .state("Gujarat")
                .city("Ahmedabad")
                .build();

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(turf));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(holidayMasterRepository.count()).thenReturn(1L);
        when(holidayMasterRepository.findUpcomingHolidays(eq(LocalDate.now()), eq("India"), eq("Gujarat"), eq("Ahmedabad")))
                .thenReturn(List.of());

        holidayService.getUpcomingHolidays(turfId, "owner@example.com");

        verify(holidayMasterRepository).findUpcomingHolidays(LocalDate.now(), "India", "Gujarat", "Ahmedabad");
        verify(holidayMasterRepository, never()).saveAll(anyList());
    }

    @Test
    void saveRuleRejectsAnotherOwnerTurf() {
        User owner = owner(1L, "owner@example.com", RoleName.ROLE_OWNER);
        User otherOwner = owner(2L, "other@example.com", RoleName.ROLE_OWNER);
        Turf turf = Turf.builder().id(10L).owner(owner).build();
        HolidayPricingRule request = HolidayPricingRule.builder()
                .autoApply(true)
                .pricingMode("PERCENTAGE")
                .value(BigDecimal.valueOf(50))
                .applyTo("ALL")
                .build();

        when(turfRepository.findById(10L)).thenReturn(Optional.of(turf));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherOwner));

        ApiException exception = assertThrows(ApiException.class,
                () -> holidayService.saveRule(10L, "other@example.com", request));

        assertEquals("You do not have permission to manage holiday pricing for this turf.", exception.getMessage());
        verify(holidayPricingRuleRepository, never()).save(any(HolidayPricingRule.class));
    }

    @Test
    void saveRuleNormalizesSelectedSlotRule() {
        User owner = owner(1L, "owner@example.com", RoleName.ROLE_OWNER);
        Turf turf = Turf.builder().id(10L).owner(owner).build();
        Slot slot = Slot.builder().id(99L).turf(turf).build();
        HolidayPricingRule request = HolidayPricingRule.builder()
                .autoApply(true)
                .pricingMode("multiplier")
                .value(BigDecimal.valueOf(1.5))
                .applyTo("selected")
                .slotIds(new HashSet<>(Set.of(99L)))
                .excludedHolidays(new HashSet<>(Set.of("Christmas")))
                .build();

        when(turfRepository.findById(10L)).thenReturn(Optional.of(turf));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(slotRepository.findById(99L)).thenReturn(Optional.of(slot));
        when(holidayPricingRuleRepository.findByTurfId(10L)).thenReturn(Optional.empty());
        when(holidayPricingRuleRepository.save(any(HolidayPricingRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HolidayPricingRule saved = holidayService.saveRule(10L, "owner@example.com", request);

        assertTrue(saved.getAutoApply());
        assertEquals("MULTIPLIER", saved.getPricingMode());
        assertEquals("SELECTED", saved.getApplyTo());
        assertEquals(Set.of(99L), saved.getSlotIds());
        assertEquals(Set.of("Christmas"), saved.getExcludedHolidays());
    }

    @Test
    void saveRuleRejectsSelectedModeWithoutSlots() {
        User owner = owner(1L, "owner@example.com", RoleName.ROLE_OWNER);
        Turf turf = Turf.builder().id(10L).owner(owner).build();
        HolidayPricingRule request = HolidayPricingRule.builder()
                .autoApply(true)
                .pricingMode("PERCENTAGE")
                .value(BigDecimal.valueOf(25))
                .applyTo("SELECTED")
                .slotIds(Set.of())
                .build();

        when(turfRepository.findById(10L)).thenReturn(Optional.of(turf));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));

        ApiException exception = assertThrows(ApiException.class,
                () -> holidayService.saveRule(10L, "owner@example.com", request));

        assertEquals("Select at least one slot for selected-slot holiday pricing.", exception.getMessage());
        verify(holidayPricingRuleRepository, never()).save(any(HolidayPricingRule.class));
    }

    @Test
    void saveRuleRejectsUnsafePercentage() {
        User owner = owner(1L, "owner@example.com", RoleName.ROLE_OWNER);
        Turf turf = Turf.builder().id(10L).owner(owner).build();
        HolidayPricingRule request = HolidayPricingRule.builder()
                .autoApply(true)
                .pricingMode("PERCENTAGE")
                .value(BigDecimal.valueOf(250))
                .applyTo("ALL")
                .build();

        when(turfRepository.findById(10L)).thenReturn(Optional.of(turf));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));

        ApiException exception = assertThrows(ApiException.class,
                () -> holidayService.saveRule(10L, "owner@example.com", request));

        assertEquals("Holiday percentage increase cannot exceed 200.", exception.getMessage());
        verify(holidayPricingRuleRepository, never()).save(any(HolidayPricingRule.class));
    }

    @Test
    void saveRuleRejectsSlotFromAnotherTurf() {
        User owner = owner(1L, "owner@example.com", RoleName.ROLE_OWNER);
        Turf turf = Turf.builder().id(10L).owner(owner).build();
        Turf otherTurf = Turf.builder().id(20L).owner(owner).build();
        Slot otherSlot = Slot.builder().id(99L).turf(otherTurf).build();
        HolidayPricingRule request = HolidayPricingRule.builder()
                .autoApply(true)
                .pricingMode("PERCENTAGE")
                .value(BigDecimal.valueOf(50))
                .applyTo("SELECTED")
                .slotIds(new HashSet<>(Set.of(99L)))
                .build();

        when(turfRepository.findById(10L)).thenReturn(Optional.of(turf));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(slotRepository.findById(99L)).thenReturn(Optional.of(otherSlot));

        ApiException exception = assertThrows(ApiException.class,
                () -> holidayService.saveRule(10L, "owner@example.com", request));

        assertEquals("Selected slot does not belong to this turf.", exception.getMessage());
        verify(holidayPricingRuleRepository, never()).save(any(HolidayPricingRule.class));
    }

    private User owner(Long id, String email, RoleName roleName) {
        User user = User.builder().id(id).email(email).build();
        user.getRoles().add(new Role(id, roleName));
        return user;
    }
}
