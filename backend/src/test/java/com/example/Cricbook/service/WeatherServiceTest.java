package com.example.Cricbook.service;

import com.example.Cricbook.dto.WeatherDtos.*;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.*;
import com.example.Cricbook.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {
    @Mock private WeatherCacheRepository weatherCacheRepository;
    @Mock private PlayabilityScoreRepository playabilityScoreRepository;
    @Mock private WeatherPolicyRepository weatherPolicyRepository;
    @Mock private TurfEnvironmentRepository turfEnvironmentRepository;
    @Mock private WeatherRescheduleRepository weatherRescheduleRepository;
    @Mock private WeatherRefundRepository weatherRefundRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private TurfRepository turfRepository;
    @Mock private SlotRepository slotRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;

    private WeatherService weatherService;
    private User user;
    private User owner;
    private Turf turf;
    private Slot slot;
    private Booking booking;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService(
                weatherCacheRepository,
                playabilityScoreRepository,
                weatherPolicyRepository,
                turfEnvironmentRepository,
                weatherRescheduleRepository,
                weatherRefundRepository,
                bookingRepository,
                turfRepository,
                slotRepository,
                userRepository,
                notificationService
        );
        user = User.builder().id(1L).email("player@test.com").phone("111").name("Player").build();
        owner = User.builder().id(2L).email("owner@test.com").phone("222").name("Owner").build();
        turf = Turf.builder().id(10L).name("Rain Arena").city("Ahmedabad").location("SG Road").owner(owner).pricePerHour(BigDecimal.valueOf(1200)).build();
        slot = Slot.builder().id(20L).turf(turf).startTime(LocalTime.of(19, 0)).endTime(LocalTime.of(21, 0)).price(BigDecimal.valueOf(1200)).isActive(true).build();
        booking = Booking.builder().id(30L).user(user).turf(turf).slot(slot).bookingDate(LocalDate.now().plusDays(1)).status(BookingStatus.CONFIRMED).amount(BigDecimal.valueOf(1200)).build();
    }

    @Test
    void forecastGeneratesAndCachesFreeForecast() {
        when(weatherCacheRepository.findByCityIgnoreCaseAndForecastDateAndForecastHour("Ahmedabad", LocalDate.now(), 18)).thenReturn(Optional.empty());
        when(weatherCacheRepository.save(any(WeatherCache.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ForecastResponse response = weatherService.forecast("Ahmedabad", LocalDate.now(), 18);

        assertEquals("Ahmedabad", response.getCity());
        assertNotNull(response.getRainProbability());
        assertNotNull(response.getRiskLevel());
        verify(weatherCacheRepository).save(any(WeatherCache.class));
    }

    @Test
    void indoorPlayabilityImprovesWeatherScore() {
        WeatherCache rainy = WeatherCache.builder()
                .city("Ahmedabad")
                .forecastDate(LocalDate.now())
                .forecastHour(19)
                .temperature(32.0)
                .rainProbability(80.0)
                .humidity(72.0)
                .windSpeed(8.0)
                .uvIndex(2.0)
                .summary("Heavy rain risk")
                .build();
        when(turfRepository.findById(10L)).thenReturn(Optional.of(turf));
        when(slotRepository.findById(20L)).thenReturn(Optional.of(slot));
        when(weatherCacheRepository.findByCityIgnoreCaseAndForecastDateAndForecastHour(anyString(), any(), eq(19))).thenReturn(Optional.of(rainy));
        when(turfEnvironmentRepository.findByTurfId(10L)).thenReturn(Optional.of(TurfEnvironment.builder().turf(turf).type(TurfEnvironmentType.INDOOR).rainProtection(true).heatProtection(true).build()));
        when(playabilityScoreRepository.findFirstBySlotIdAndPlayDateOrderByUpdatedAtDesc(eq(20L), any())).thenReturn(Optional.empty());
        when(playabilityScoreRepository.save(any(PlayabilityScore.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlayabilityResponse response = weatherService.playability(10L, 20L, LocalDate.now());

        assertTrue(response.getScore() >= 70);
        assertEquals(WeatherRiskLevel.LOW, response.getRiskLevel());
    }

    @Test
    void ownerPolicyCanBeUpdated() {
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        when(weatherPolicyRepository.findByOwnerId(2L)).thenReturn(Optional.empty());
        when(weatherPolicyRepository.save(any(WeatherPolicy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WeatherPolicyResponse response = weatherService.updateOwnerPolicy("owner@test.com", WeatherPolicyRequest.builder()
                .rainRefundAllowed(true)
                .flexibleReschedule(true)
                .rainThreshold(65)
                .heatThresholdCelsius(37)
                .refundType(WeatherRefundType.WALLET_CREDIT)
                .build());

        assertEquals(65, response.getRainThreshold());
        assertEquals(WeatherRefundType.WALLET_CREDIT, response.getRefundType());
    }

    @Test
    void rescheduleRequiresHighWeatherRiskAndNotifiesPlayer() {
        WeatherCache rainy = WeatherCache.builder().city("Ahmedabad").forecastDate(booking.getBookingDate()).forecastHour(19).temperature(34.0).rainProbability(95.0).humidity(90.0).windSpeed(12.0).uvIndex(2.0).summary("Heavy rain risk").build();
        when(userRepository.findByEmail("player@test.com")).thenReturn(Optional.of(user));
        when(bookingRepository.findById(30L)).thenReturn(Optional.of(booking));
        when(turfRepository.findById(10L)).thenReturn(Optional.of(turf));
        when(slotRepository.findById(20L)).thenReturn(Optional.of(slot));
        when(weatherCacheRepository.findByCityIgnoreCaseAndForecastDateAndForecastHour(anyString(), any(), eq(19))).thenReturn(Optional.of(rainy));
        when(turfEnvironmentRepository.findByTurfId(10L)).thenReturn(Optional.empty());
        when(playabilityScoreRepository.findFirstBySlotIdAndPlayDateOrderByUpdatedAtDesc(anyLong(), any())).thenReturn(Optional.empty());
        when(playabilityScoreRepository.save(any(PlayabilityScore.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(weatherRescheduleRepository.save(any(WeatherReschedule.class))).thenAnswer(invocation -> {
            WeatherReschedule item = invocation.getArgument(0);
            item.setId(99L);
            return item;
        });

        WeatherRescheduleResponse response = weatherService.suggestReschedule("player@test.com", 30L);

        assertEquals(99L, response.getId());
        assertEquals("SUGGESTED", response.getStatus());
        verify(notificationService).pushNotification(eq(1L), eq("player@test.com"), anyString(), anyString());
    }

    @Test
    void playerDashboardSummarizesForecastAndBookings() {
        when(userRepository.findByEmail("player@test.com")).thenReturn(Optional.of(user));
        when(weatherCacheRepository.findByCityIgnoreCaseAndForecastDateAndForecastHour(anyString(), any(), anyInt())).thenReturn(Optional.empty());
        when(weatherCacheRepository.save(any(WeatherCache.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(weatherCacheRepository.findByCityIgnoreCaseAndForecastDateOrderByForecastHourAsc(anyString(), any())).thenReturn(List.of());
        when(bookingRepository.findByUserIdOrderByBookingDateDescCreatedAtDesc(1L)).thenReturn(List.of());
        when(weatherRefundRepository.findByBookingUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        WeatherDashboardResponse response = weatherService.playerDashboard("player@test.com", "Ahmedabad");

        assertEquals("Ahmedabad", response.getCity());
        assertEquals(4, response.getForecast().size());
        assertNotNull(response.getInsight());
    }
}
