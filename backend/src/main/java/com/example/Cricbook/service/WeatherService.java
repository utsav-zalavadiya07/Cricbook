package com.example.Cricbook.service;

import com.example.Cricbook.dto.WeatherDtos.*;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WeatherService {
    private final WeatherCacheRepository weatherCacheRepository;
    private final PlayabilityScoreRepository playabilityScoreRepository;
    private final WeatherPolicyRepository weatherPolicyRepository;
    private final TurfEnvironmentRepository turfEnvironmentRepository;
    private final WeatherRescheduleRepository weatherRescheduleRepository;
    private final WeatherRefundRepository weatherRefundRepository;
    private final BookingRepository bookingRepository;
    private final TurfRepository turfRepository;
    private final SlotRepository slotRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public ForecastResponse forecast(String city, LocalDate date, Integer hour) {
        String normalizedCity = normalizeCity(city);
        LocalDate forecastDate = Optional.ofNullable(date).orElse(LocalDate.now());
        int forecastHour = Optional.ofNullable(hour).orElse(18);
        WeatherCache cache = weatherCacheRepository.findByCityIgnoreCaseAndForecastDateAndForecastHour(normalizedCity, forecastDate, forecastHour)
                .orElseGet(() -> weatherCacheRepository.save(generateForecast(normalizedCity, forecastDate, forecastHour)));
        return mapForecast(cache);
    }

    @Transactional
    public List<ForecastResponse> dayForecast(String city, LocalDate date) {
        String normalizedCity = normalizeCity(city);
        LocalDate forecastDate = Optional.ofNullable(date).orElse(LocalDate.now());
        List<WeatherCache> cached = weatherCacheRepository.findByCityIgnoreCaseAndForecastDateOrderByForecastHourAsc(normalizedCity, forecastDate);
        if (cached.size() >= 4) {
            return cached.stream().map(this::mapForecast).toList();
        }
        return List.of(6, 12, 18, 21).stream()
                .map(hour -> forecast(normalizedCity, forecastDate, hour))
                .toList();
    }

    @Transactional
    public PlayabilityResponse playability(Long turfId, Long slotId, LocalDate date) {
        Turf turf = turfRepository.findById(required(turfId, "Select a turf."))
                .orElseThrow(() -> ApiException.notFound("Turf was not found."));
        Slot slot = slotRepository.findById(required(slotId, "Select a slot."))
                .orElseThrow(() -> ApiException.notFound("Slot was not found."));
        if (!slot.getTurf().getId().equals(turf.getId())) {
            throw ApiException.badRequest("Selected slot does not belong to this turf.");
        }
        LocalDate playDate = Optional.ofNullable(date).orElse(LocalDate.now());
        ForecastResponse forecast = forecast(turf.getCity(), playDate, slot.getStartTime().getHour());
        TurfEnvironment environment = turfEnvironmentRepository.findByTurfId(turf.getId())
                .orElseGet(() -> defaultEnvironment(turf));
        int score = calculateScore(forecast, environment);
        double effectiveRain = environment.getRainProtection() ? forecast.getRainProbability() * 0.35 : forecast.getRainProbability();
        WeatherRiskLevel risk = riskFor(score, effectiveRain, forecast.getTemperature());
        String reason = buildReason(forecast, environment, score);

        PlayabilityScore entity = playabilityScoreRepository.findFirstBySlotIdAndPlayDateOrderByUpdatedAtDesc(slot.getId(), playDate)
                .orElse(PlayabilityScore.builder().slot(slot).playDate(playDate).build());
        entity.setScore(score);
        entity.setRiskLevel(risk);
        entity.setReason(reason);
        playabilityScoreRepository.save(entity);

        return PlayabilityResponse.builder()
                .turfId(turf.getId())
                .turfName(turf.getName())
                .slotId(slot.getId())
                .date(playDate)
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .score(score)
                .riskLevel(risk)
                .rainProbability(forecast.getRainProbability())
                .temperature(forecast.getTemperature())
                .reason(reason)
                .recommendation(recommendationFor(risk, environment))
                .build();
    }

    @Transactional(readOnly = true)
    public WeatherPolicyResponse getOwnerPolicy(String loginId) {
        User owner = findUser(loginId);
        return mapPolicy(weatherPolicyRepository.findByOwnerId(owner.getId()).orElse(defaultPolicy(owner)));
    }

    @Transactional
    public WeatherPolicyResponse updateOwnerPolicy(String loginId, WeatherPolicyRequest request) {
        User owner = findUser(loginId);
        WeatherPolicy policy = weatherPolicyRepository.findByOwnerId(owner.getId()).orElse(defaultPolicy(owner));
        policy.setRainRefundAllowed(Optional.ofNullable(request.getRainRefundAllowed()).orElse(Boolean.TRUE));
        policy.setFlexibleReschedule(Optional.ofNullable(request.getFlexibleReschedule()).orElse(Boolean.TRUE));
        policy.setRainThreshold(Optional.ofNullable(request.getRainThreshold()).orElse(70));
        policy.setHeatThresholdCelsius(Optional.ofNullable(request.getHeatThresholdCelsius()).orElse(38));
        policy.setRefundType(Optional.ofNullable(request.getRefundType()).orElse(WeatherRefundType.RESCHEDULE_ONLY));
        return mapPolicy(weatherPolicyRepository.save(policy));
    }

    @Transactional
    public TurfEnvironmentResponse updateEnvironment(String loginId, TurfEnvironmentRequest request) {
        User owner = findUser(loginId);
        Turf turf = turfRepository.findById(required(request.getTurfId(), "Select a turf."))
                .orElseThrow(() -> ApiException.notFound("Turf was not found."));
        if (!turf.getOwner().getId().equals(owner.getId())) {
            throw ApiException.forbidden("You can only update weather settings for your own turf.");
        }
        TurfEnvironment environment = turfEnvironmentRepository.findByTurfId(turf.getId()).orElse(TurfEnvironment.builder().turf(turf).build());
        environment.setType(Optional.ofNullable(request.getType()).orElse(TurfEnvironmentType.OUTDOOR));
        environment.setRainProtection(Boolean.TRUE.equals(request.getRainProtection()));
        environment.setHeatProtection(Boolean.TRUE.equals(request.getHeatProtection()));
        return mapEnvironment(turfEnvironmentRepository.save(environment));
    }

    @Transactional
    public WeatherRescheduleResponse suggestReschedule(String loginId, Long bookingId) {
        User user = findUser(loginId);
        Booking booking = bookingRepository.findById(required(bookingId, "Booking is required."))
                .orElseThrow(() -> ApiException.notFound("Booking was not found."));
        if (!booking.getUser().getId().equals(user.getId()) && !booking.getTurf().getOwner().getId().equals(user.getId())) {
            throw ApiException.forbidden("You do not have permission to reschedule this booking.");
        }
        PlayabilityResponse risk = playability(booking.getTurf().getId(), booking.getSlot().getId(), booking.getBookingDate());
        if (risk.getRiskLevel() == WeatherRiskLevel.LOW) {
            throw ApiException.conflict("Weather risk is low. Reschedule is not required right now.");
        }
        WeatherReschedule reschedule = WeatherReschedule.builder()
                .booking(booking)
                .suggestedSlot(booking.getSlot())
                .suggestedDate(booking.getBookingDate().plusDays(1))
                .status(WeatherRescheduleStatus.SUGGESTED)
                .reason("Weather risk is " + risk.getRiskLevel() + ". Suggested a safer next-day slot.")
                .build();
        notificationService.pushNotification(booking.getUser().getId(), booking.getUser().getEmail(), "Weather reschedule suggested", reschedule.getReason());
        return mapReschedule(weatherRescheduleRepository.save(reschedule));
    }

    @Transactional
    public WeatherRefundResponse evaluateRefund(String loginId, Long bookingId) {
        User user = findUser(loginId);
        Booking booking = bookingRepository.findById(required(bookingId, "Booking is required."))
                .orElseThrow(() -> ApiException.notFound("Booking was not found."));
        if (!booking.getUser().getId().equals(user.getId()) && !booking.getTurf().getOwner().getId().equals(user.getId())) {
            throw ApiException.forbidden("You do not have permission to evaluate this refund.");
        }
        WeatherPolicy policy = weatherPolicyRepository.findByOwnerId(booking.getTurf().getOwner().getId()).orElse(defaultPolicy(booking.getTurf().getOwner()));
        PlayabilityResponse risk = playability(booking.getTurf().getId(), booking.getSlot().getId(), booking.getBookingDate());
        BigDecimal amount = risk.getRainProbability() >= policy.getRainThreshold() && Boolean.TRUE.equals(policy.getRainRefundAllowed())
                ? booking.getAmount().multiply(BigDecimal.valueOf(0.5))
                : BigDecimal.ZERO;
        WeatherRefund refund = WeatherRefund.builder()
                .booking(booking)
                .refundType(amount.signum() > 0 ? policy.getRefundType() : WeatherRefundType.RESCHEDULE_ONLY)
                .amount(amount)
                .status(amount.signum() > 0 ? WeatherRefundStatus.APPROVED : WeatherRefundStatus.PENDING)
                .reason(amount.signum() > 0 ? "Weather policy allows refund protection." : "Reschedule is recommended before refund.")
                .build();
        return mapRefund(weatherRefundRepository.save(refund));
    }

    @Transactional
    public WeatherDashboardResponse playerDashboard(String loginId, String city) {
        User user = findUser(loginId);
        String dashboardCity = normalizeCity(city);
        ForecastResponse current = forecast(dashboardCity, LocalDate.now(), LocalTime.now().getHour());
        List<PlayabilityResponse> recommendations = bookingRepository.findByUserIdOrderByBookingDateDescCreatedAtDesc(user.getId()).stream()
                .limit(4)
                .map(booking -> playability(booking.getTurf().getId(), booking.getSlot().getId(), booking.getBookingDate()))
                .sorted(Comparator.comparing(PlayabilityResponse::getScore).reversed())
                .toList();
        long highRisk = recommendations.stream().filter(item -> item.getRiskLevel() == WeatherRiskLevel.HIGH || item.getRiskLevel() == WeatherRiskLevel.UNSAFE).count();
        return WeatherDashboardResponse.builder()
                .city(dashboardCity)
                .current(current)
                .forecast(dayForecast(dashboardCity, LocalDate.now()))
                .recommendations(recommendations)
                .highRiskBookings(highRisk)
                .weatherRefunds(weatherRefundRepository.findByBookingUserIdOrderByCreatedAtDesc(user.getId()).size())
                .insight(highRisk > 0 ? "Some upcoming bookings have weather risk. Review reschedule options." : "Weather looks playable for your current booking pattern.")
                .build();
    }

    @Transactional
    public WeatherDashboardResponse ownerDashboard(String loginId) {
        User owner = findUser(loginId);
        String city = turfRepository.findByOwnerId(owner.getId()).stream().findFirst().map(Turf::getCity).orElse("Ahmedabad");
        return WeatherDashboardResponse.builder()
                .city(city)
                .current(forecast(city, LocalDate.now(), LocalTime.now().getHour()))
                .forecast(dayForecast(city, LocalDate.now()))
                .recommendations(List.of())
                .highRiskBookings(weatherRescheduleRepository.findByBookingTurfOwnerIdOrderByCreatedAtDesc(owner.getId()).size())
                .weatherRefunds(weatherRefundRepository.countByBookingTurfOwnerId(owner.getId()))
                .insight("Use rain thresholds and flexible rescheduling to reduce refund disputes.")
                .build();
    }

    @Transactional
    public WeatherDashboardResponse adminDashboard(String city) {
        String dashboardCity = normalizeCity(city);
        return WeatherDashboardResponse.builder()
                .city(dashboardCity)
                .current(forecast(dashboardCity, LocalDate.now(), LocalTime.now().getHour()))
                .forecast(dayForecast(dashboardCity, LocalDate.now()))
                .recommendations(List.of())
                .highRiskBookings(weatherRescheduleRepository.count())
                .weatherRefunds(weatherRefundRepository.count())
                .insight("Monitor seasonal playability and weather-driven reschedules across cities.")
                .build();
    }

    private WeatherCache generateForecast(String city, LocalDate date, int hour) {
        int seed = Math.abs((city.toLowerCase() + date + hour).hashCode());
        double rain = seed % 100;
        double temp = 24 + (seed % 17) + (hour >= 12 && hour <= 16 ? 4 : 0);
        double humidity = 45 + (seed % 50);
        double wind = 4 + (seed % 18);
        double uv = hour >= 10 && hour <= 16 ? 6 + (seed % 5) : 2 + (seed % 3);
        return WeatherCache.builder()
                .city(city)
                .forecastDate(date)
                .forecastHour(hour)
                .temperature(temp)
                .rainProbability(rain)
                .humidity(humidity)
                .windSpeed(wind)
                .uvIndex(uv)
                .summary(summary(rain, temp, humidity))
                .build();
    }

    private int calculateScore(ForecastResponse forecast, TurfEnvironment environment) {
        int rainPenalty = (int) Math.round(forecast.getRainProbability() * (environment.getRainProtection() ? 0.15 : 0.4));
        int heatPenalty = Math.max(0, (int) Math.round((forecast.getTemperature() - 32) * (environment.getHeatProtection() ? 1.2 : 2.0)));
        int humidityPenalty = Math.max(0, (int) Math.round((forecast.getHumidity() - 65) * 0.35));
        int windPenalty = Math.max(0, (int) Math.round((forecast.getWindSpeed() - 18) * 0.5));
        int environmentBonus = environment.getType() == TurfEnvironmentType.INDOOR ? 15 : environment.getType() == TurfEnvironmentType.SEMI_INDOOR ? 8 : 0;
        return Math.max(0, Math.min(100, 100 - rainPenalty - heatPenalty - humidityPenalty - windPenalty + environmentBonus));
    }

    private WeatherRiskLevel riskFor(int score, double rain, double temp) {
        if (score < 30 || rain >= 85 || temp >= 42) return WeatherRiskLevel.UNSAFE;
        if (score < 50 || rain >= 70 || temp >= 39) return WeatherRiskLevel.HIGH;
        if (score < 70 || rain >= 45) return WeatherRiskLevel.MEDIUM;
        return WeatherRiskLevel.LOW;
    }

    private String buildReason(ForecastResponse forecast, TurfEnvironment environment, int score) {
        return "Score " + score + " with " + Math.round(forecast.getRainProbability()) + "% rain, "
                + Math.round(forecast.getTemperature()) + "C heat, " + Math.round(forecast.getHumidity()) + "% humidity, "
                + environment.getType().name().toLowerCase().replace('_', ' ') + " protection.";
    }

    private String recommendationFor(WeatherRiskLevel risk, TurfEnvironment environment) {
        if (risk == WeatherRiskLevel.UNSAFE) return "Avoid this slot or move to an indoor turf.";
        if (risk == WeatherRiskLevel.HIGH) return environment.getRainProtection() ? "Proceed with caution and keep reschedule ready." : "Prefer indoor or later slot.";
        if (risk == WeatherRiskLevel.MEDIUM) return "Playable with caution. Hydrate and monitor alerts.";
        return "Good conditions for cricket.";
    }

    private String summary(double rain, double temp, double humidity) {
        if (rain >= 70) return "Heavy rain risk";
        if (temp >= 39) return "Extreme heat advisory";
        if (humidity >= 80) return "High humidity";
        if (rain >= 40) return "Light rain possible";
        return "Playable weather";
    }

    private WeatherRiskLevel forecastRisk(WeatherCache cache) {
        return riskFor(100 - (int) Math.round(cache.getRainProbability() * 0.4), cache.getRainProbability(), cache.getTemperature());
    }

    private ForecastResponse mapForecast(WeatherCache cache) {
        return ForecastResponse.builder()
                .city(cache.getCity())
                .date(cache.getForecastDate())
                .hour(cache.getForecastHour())
                .temperature(cache.getTemperature())
                .rainProbability(cache.getRainProbability())
                .humidity(cache.getHumidity())
                .windSpeed(cache.getWindSpeed())
                .uvIndex(cache.getUvIndex())
                .summary(cache.getSummary())
                .riskLevel(forecastRisk(cache))
                .build();
    }

    private WeatherPolicyResponse mapPolicy(WeatherPolicy policy) {
        return WeatherPolicyResponse.builder()
                .rainRefundAllowed(policy.getRainRefundAllowed())
                .flexibleReschedule(policy.getFlexibleReschedule())
                .rainThreshold(policy.getRainThreshold())
                .heatThresholdCelsius(policy.getHeatThresholdCelsius())
                .refundType(policy.getRefundType())
                .build();
    }

    private TurfEnvironmentResponse mapEnvironment(TurfEnvironment environment) {
        return TurfEnvironmentResponse.builder()
                .turfId(environment.getTurf().getId())
                .turfName(environment.getTurf().getName())
                .type(environment.getType())
                .rainProtection(environment.getRainProtection())
                .heatProtection(environment.getHeatProtection())
                .build();
    }

    private WeatherRescheduleResponse mapReschedule(WeatherReschedule item) {
        return WeatherRescheduleResponse.builder()
                .id(item.getId())
                .bookingId(item.getBooking().getId())
                .suggestedSlotId(item.getSuggestedSlot().getId())
                .suggestedDate(item.getSuggestedDate())
                .status(item.getStatus().name())
                .reason(item.getReason())
                .build();
    }

    private WeatherRefundResponse mapRefund(WeatherRefund item) {
        return WeatherRefundResponse.builder()
                .id(item.getId())
                .bookingId(item.getBooking().getId())
                .refundType(item.getRefundType())
                .amount(item.getAmount())
                .status(item.getStatus().name())
                .reason(item.getReason())
                .build();
    }

    private WeatherPolicy defaultPolicy(User owner) {
        return WeatherPolicy.builder()
                .owner(owner)
                .rainRefundAllowed(true)
                .flexibleReschedule(true)
                .rainThreshold(70)
                .heatThresholdCelsius(38)
                .refundType(WeatherRefundType.RESCHEDULE_ONLY)
                .build();
    }

    private TurfEnvironment defaultEnvironment(Turf turf) {
        return TurfEnvironment.builder()
                .turf(turf)
                .type(TurfEnvironmentType.OUTDOOR)
                .rainProtection(false)
                .heatProtection(false)
                .build();
    }

    private String normalizeCity(String city) {
        return city == null || city.isBlank() ? "Ahmedabad" : city.trim();
    }

    private Long required(Long id, String message) {
        if (id == null) throw ApiException.badRequest(message);
        return id;
    }

    private User findUser(String loginId) {
        if (loginId != null && loginId.contains("@")) {
            Optional<User> byEmail = userRepository.findByEmail(loginId);
            if (byEmail.isPresent()) return byEmail.get();
        }
        return userRepository.findByEmailOrPhone(loginId)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
    }
}
