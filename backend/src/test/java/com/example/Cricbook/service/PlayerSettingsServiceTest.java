package com.example.Cricbook.service;

import com.example.Cricbook.dto.PlayerSettingsRequest;
import com.example.Cricbook.dto.PlayerSettingsResponse;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.NotificationChannel;
import com.example.Cricbook.enums.UserStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerSettingsServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PlayerSettingsRepository playerSettingsRepository;
    @Mock
    private PlayerProfileRepository playerProfileRepository;
    @Mock
    private BookingPreferenceRepository bookingPreferenceRepository;
    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @InjectMocks
    private PlayerSettingsService playerSettingsService;

    @Test
    void getSettingsReturnsSafeDefaultsFromExistingUser() {
        User user = user();
        when(userRepository.findByEmailOrPhone(user.getEmail())).thenReturn(Optional.of(user));
        when(playerSettingsRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(playerProfileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(bookingPreferenceRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(notificationPreferenceRepository.findByUserId(user.getId())).thenReturn(List.of());

        PlayerSettingsResponse response = playerSettingsService.getSettings(user.getEmail());

        assertEquals("Player One", response.getProfile().getDisplayName());
        assertEquals("dark", response.getAppearance().getTheme());
        assertEquals("en", response.getLocalization().getLanguage());
        assertTrue(response.getPrivacy().getShowTrustScore());
        assertTrue(response.getCommunity().getAllowTeamInvites());
    }

    @Test
    void updateSettingsPersistsAllMajorPreferenceSections() {
        User user = user();
        PlayerSettingsRequest request = request();
        when(userRepository.findByEmailOrPhone(user.getEmail())).thenReturn(Optional.of(user));
        when(playerSettingsRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(playerProfileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(bookingPreferenceRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(playerSettingsRepository.save(any(PlayerSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(playerProfileRepository.save(any(PlayerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingPreferenceRepository.save(any(BookingPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationPreferenceRepository.findByUserIdAndChannel(eq(user.getId()), any(NotificationChannel.class))).thenReturn(Optional.empty());
        when(notificationPreferenceRepository.save(any(NotificationPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationPreferenceRepository.findByUserId(user.getId())).thenReturn(List.of(
                NotificationPreference.builder().userId(user.getId()).channel(NotificationChannel.PUSH).enabled(true).frequency("DAILY").build(),
                NotificationPreference.builder().userId(user.getId()).channel(NotificationChannel.EMAIL).enabled(false).frequency("DAILY").build()
        ));

        PlayerSettingsResponse response = playerSettingsService.updateSettings(user.getEmail(), request);

        assertEquals("Utsav Player", response.getProfile().getDisplayName());
        assertEquals("light", response.getAppearance().getTheme());
        assertEquals("gu", response.getLocalization().getLanguage());
        assertEquals("FRIDAY", response.getBooking().getPreferredDay());
        assertEquals("20:00", response.getBooking().getPreferredTime());
        assertEquals("INTERMEDIATE", response.getMatchmaking().getPreferredSkillLevel());
        assertFalse(response.getCommunity().getAllowMessages());
        verify(playerSettingsRepository).save(any(PlayerSettings.class));
        verify(bookingPreferenceRepository).save(argThat(preference -> preference.getPreferredDay() == DayOfWeek.FRIDAY));
        verify(notificationPreferenceRepository, times(4)).save(any(NotificationPreference.class));
    }

    @Test
    void updateSettingsRejectsUnsupportedLanguage() {
        User user = user();
        PlayerSettingsRequest request = new PlayerSettingsRequest();
        PlayerSettingsRequest.LocalizationSection localization = new PlayerSettingsRequest.LocalizationSection();
        localization.setLanguage("xx");
        request.setLocalization(localization);
        when(userRepository.findByEmailOrPhone(user.getEmail())).thenReturn(Optional.of(user));
        when(playerSettingsRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(playerProfileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(bookingPreferenceRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> playerSettingsService.updateSettings(user.getEmail(), request));

        assertEquals("Language is not supported yet.", exception.getMessage());
        verify(playerSettingsRepository, never()).save(any(PlayerSettings.class));
    }

    @Test
    void requestDataExportMarksExportRequest() {
        User user = user();
        when(userRepository.findByEmailOrPhone(user.getEmail())).thenReturn(Optional.of(user));
        when(playerSettingsRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(playerSettingsRepository.save(any(PlayerSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(playerProfileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(bookingPreferenceRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(notificationPreferenceRepository.findByUserId(user.getId())).thenReturn(List.of());

        PlayerSettingsResponse response = playerSettingsService.requestDataExport(user.getEmail());

        assertTrue(response.getData().getDownloadDataRequested());
        assertNotNull(response.getData().getLastExportRequestedAt());
    }

    private User user() {
        return User.builder()
                .id(12L)
                .name("Player One")
                .email("player@test.com")
                .phone("9876543210")
                .preferredLanguage("en")
                .preferredTheme("dark")
                .status(UserStatus.ACTIVE)
                .build();
    }

    private PlayerSettingsRequest request() {
        PlayerSettingsRequest request = new PlayerSettingsRequest();
        PlayerSettingsRequest.ProfileSection profile = new PlayerSettingsRequest.ProfileSection();
        profile.setDisplayName("Utsav Player");
        profile.setPhone("9999999999");
        profile.setBio("Opening batter");
        profile.setCity("Surat");
        profile.setPosition("Batsman");
        profile.setSkillLevel("Intermediate");
        profile.setVisibility("friends");
        request.setProfile(profile);

        PlayerSettingsRequest.AppearanceSection appearance = new PlayerSettingsRequest.AppearanceSection();
        appearance.setTheme("light");
        request.setAppearance(appearance);

        PlayerSettingsRequest.LocalizationSection localization = new PlayerSettingsRequest.LocalizationSection();
        localization.setLanguage("gu");
        localization.setRegionCity("Surat");
        localization.setCurrency("INR");
        localization.setTimezone("Asia/Kolkata");
        request.setLocalization(localization);

        PlayerSettingsRequest.BookingSection booking = new PlayerSettingsRequest.BookingSection();
        booking.setPreferredDay("friday");
        booking.setPreferredTime("20:00");
        booking.setPreferredDurationMinutes(90);
        booking.setPreferredCity("Surat");
        booking.setAutoWaitlistEnabled(true);
        booking.setQuickRebookEnabled(true);
        request.setBooking(booking);

        PlayerSettingsRequest.MatchmakingSection matchmaking = new PlayerSettingsRequest.MatchmakingSection();
        matchmaking.setCompetitionLevel("competitive");
        matchmaking.setPreferredSkillLevel("intermediate");
        matchmaking.setTrustedOnly(true);
        matchmaking.setNearbyOnly(true);
        request.setMatchmaking(matchmaking);

        PlayerSettingsRequest.CommunitySection community = new PlayerSettingsRequest.CommunitySection();
        community.setAllowTeamInvites(true);
        community.setAllowFriendRequests(true);
        community.setAllowMessages(false);
        request.setCommunity(community);

        PlayerSettingsRequest.PaymentSection payments = new PlayerSettingsRequest.PaymentSection();
        payments.setDefaultPaymentMethod("UPI");
        payments.setSavedUpiId("player@upi");
        payments.setAutoPaySplits(false);
        request.setPayments(payments);

        PlayerSettingsRequest.NotificationSection notifications = new PlayerSettingsRequest.NotificationSection();
        notifications.setPush(true);
        notifications.setEmail(false);
        notifications.setSms(false);
        notifications.setWhatsapp(true);
        notifications.setFrequency("daily");
        request.setNotifications(notifications);

        return request;
    }
}
