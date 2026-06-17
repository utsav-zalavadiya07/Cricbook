package com.example.Cricbook.service;

import com.example.Cricbook.dto.OwnerSettingsRequest;
import com.example.Cricbook.dto.OwnerSettingsResponse;
import com.example.Cricbook.entity.OwnerSettings;
import com.example.Cricbook.entity.Role;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.RoleName;
import com.example.Cricbook.enums.UserStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.OwnerSettingsRepository;
import com.example.Cricbook.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerSettingsServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private OwnerSettingsRepository ownerSettingsRepository;
    @InjectMocks
    private OwnerSettingsService ownerSettingsService;

    @Test
    void getSettingsReturnsOwnerDefaultsWhenNotConfigured() {
        User owner = owner();
        when(userRepository.findByEmailOrPhone(owner.getEmail())).thenReturn(Optional.of(owner));
        when(ownerSettingsRepository.findByOwnerId(owner.getId())).thenReturn(Optional.empty());

        OwnerSettingsResponse response = ownerSettingsService.getSettings(owner.getEmail());

        assertEquals("Owner One", response.getBusinessName());
        assertEquals("WEEKLY", response.getSettlementSchedule());
        assertTrue(response.getBookingAlerts());
    }

    @Test
    void updateSettingsPersistsBusinessPayoutAndNotifications() {
        User owner = owner();
        OwnerSettingsRequest request = request();
        when(userRepository.findByEmailOrPhone(owner.getEmail())).thenReturn(Optional.of(owner));
        when(ownerSettingsRepository.findByOwnerId(owner.getId())).thenReturn(Optional.empty());
        when(ownerSettingsRepository.save(any(OwnerSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OwnerSettingsResponse response = ownerSettingsService.updateSettings(owner.getEmail(), request);

        assertEquals("Blue Box Sports", response.getBusinessName());
        assertEquals("HDFC0001234", response.getBankIfsc());
        assertEquals("owner@upi", response.getUpiId());
        assertEquals("DAILY", response.getSettlementSchedule());
        assertEquals("AI_SUGGESTED", response.getPricingMode());
        assertEquals(21, response.getAdvanceBookingDays());
        assertEquals(70, response.getRainThresholdPercent());
        assertEquals(10, response.getMaxResaleMarkupPercent());
        assertTrue(response.getAiCoachEnabled());
        assertEquals(false, response.getMarketingAlerts());
        verify(ownerSettingsRepository).save(any(OwnerSettings.class));
    }

    @Test
    void updateSettingsRejectsInvalidIfsc() {
        User owner = owner();
        OwnerSettingsRequest request = request();
        request.setBankIfsc("BAD");
        when(userRepository.findByEmailOrPhone(owner.getEmail())).thenReturn(Optional.of(owner));
        when(ownerSettingsRepository.findByOwnerId(owner.getId())).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> ownerSettingsService.updateSettings(owner.getEmail(), request));

        assertEquals("IFSC code must be valid, for example HDFC0001234.", exception.getMessage());
        verify(ownerSettingsRepository, never()).save(any(OwnerSettings.class));
    }

    @Test
    void updateSettingsRejectsNonOwner() {
        User player = User.builder().id(2L).name("Player").email("player@test.com").phone("9999999999").status(UserStatus.ACTIVE).build();
        player.getRoles().add(new Role(2L, RoleName.ROLE_USER));
        when(userRepository.findByEmailOrPhone(player.getEmail())).thenReturn(Optional.of(player));

        assertThrows(ApiException.class, () -> ownerSettingsService.updateSettings(player.getEmail(), request()));
    }

    @Test
    void updateSettingsRejectsInvalidBookingRange() {
        User owner = owner();
        OwnerSettingsRequest request = request();
        request.setAdvanceBookingDays(0);
        when(userRepository.findByEmailOrPhone(owner.getEmail())).thenReturn(Optional.of(owner));
        when(ownerSettingsRepository.findByOwnerId(owner.getId())).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> ownerSettingsService.updateSettings(owner.getEmail(), request));

        assertEquals("Advance booking limit must be between 1 and 365 days.", exception.getMessage());
        verify(ownerSettingsRepository, never()).save(any(OwnerSettings.class));
    }

    @Test
    void requestExportMarksOwnerExportRequested() {
        User owner = owner();
        when(userRepository.findByEmailOrPhone(owner.getEmail())).thenReturn(Optional.of(owner));
        when(ownerSettingsRepository.findByOwnerId(owner.getId())).thenReturn(Optional.empty());
        when(ownerSettingsRepository.save(any(OwnerSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OwnerSettingsResponse response = ownerSettingsService.requestExport(owner.getEmail());

        assertTrue(response.getDataExportRequested());
        assertTrue(response.getLastExportRequestedAt() != null && !response.getLastExportRequestedAt().isBlank());
        verify(ownerSettingsRepository).save(any(OwnerSettings.class));
    }

    private User owner() {
        User owner = User.builder()
                .id(1L)
                .name("Owner One")
                .email("owner@test.com")
                .phone("9876543210")
                .preferredLanguage("en")
                .preferredTheme("dark")
                .status(UserStatus.ACTIVE)
                .build();
        owner.getRoles().add(new Role(1L, RoleName.ROLE_OWNER));
        return owner;
    }

    private OwnerSettingsRequest request() {
        OwnerSettingsRequest request = new OwnerSettingsRequest();
        request.setBusinessName("Blue Box Sports");
        request.setSupportPhone("+91 9876543210");
        request.setBusinessAddress("SG Highway, Ahmedabad");
        request.setGstNumber("24ABCDE1234F1Z5");
        request.setBusinessLogoUrl("https://cdn.cricbook.test/logo.png");
        request.setSupportEmail("SUPPORT@CRICBOOK.COM");
        request.setBusinessType("multi_turf");
        request.setDefaultSlotDurationMinutes(60);
        request.setOpeningTime("06:00");
        request.setClosingTime("23:00");
        request.setIndoorOutdoor("semi_indoor");
        request.setAdvanceBookingDays(21);
        request.setMinimumBookingMinutes(60);
        request.setAllowInstantBooking(true);
        request.setApprovalRequired(false);
        request.setTrustedPlayersOnly(true);
        request.setCancellationPolicy("moderate");
        request.setDynamicPricingEnabled(true);
        request.setPricingMode("ai_suggested");
        request.setPeakMultiplier(1.3d);
        request.setWeekendMultiplier(1.2d);
        request.setStaffInviteEmail("manager@cricbook.com");
        request.setStaffRole("receptionist");
        request.setBankAccountName("Blue Box Sports");
        request.setBankAccountNumber("1234567890");
        request.setBankIfsc("hdfc0001234");
        request.setUpiId("owner@upi");
        request.setSettlementSchedule("daily");
        request.setBookingAlerts(true);
        request.setPaymentAlerts(true);
        request.setReviewAlerts(false);
        request.setMarketingAlerts(false);
        request.setReminderLeadMinutes(120);
        request.setPartialPaymentEnabled(true);
        request.setMinimumPaymentPercent(50);
        request.setRefundMode("wallet_credit");
        request.setRainRefundAllowed(true);
        request.setRainThresholdPercent(70);
        request.setResaleEnabled(true);
        request.setTransferEnabled(true);
        request.setMaxResaleMarkupPercent(10);
        request.setAllowPublicTournaments(true);
        request.setCommunityVisibility("public");
        request.setTeamRegistrationRule("approval_required");
        request.setBrandPrimaryColor("#0EA5E9");
        request.setBrandTheme("system");
        request.setSessionTimeoutMinutes(60);
        request.setAiCoachEnabled(true);
        request.setExportFormat("excel");
        return request;
    }
}
