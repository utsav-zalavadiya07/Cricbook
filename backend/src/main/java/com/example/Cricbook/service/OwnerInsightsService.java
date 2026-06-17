package com.example.Cricbook.service;

import com.example.Cricbook.dto.AnalyticsPoint;
import com.example.Cricbook.dto.OwnerExpenseRequest;
import com.example.Cricbook.dto.OwnerExpenseResponse;
import com.example.Cricbook.dto.OwnerInsightsResponse;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnerInsightsService {
    private static final int INSIGHT_WINDOW_DAYS = 30;

    private final UserRepository userRepository;
    private final TurfRepository turfRepository;
    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final OwnerExpenseRepository expenseRepository;
    private final PlayerTrustScoreRepository trustScoreRepository;
    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public OwnerInsightsResponse getOwnerInsights(String username) {
        User owner = findUser(username, "Owner not found.");
        return buildInsights(owner);
    }

    @Transactional(readOnly = true)
    public OwnerInsightsResponse getPlatformInsights() {
        return buildInsights(null);
    }

    @Transactional
    public OwnerExpenseResponse addExpense(String username, OwnerExpenseRequest request) {
        User owner = findUser(username, "Owner not found.");
        if (request == null || request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("Expense amount must be greater than zero.");
        }
        if (request.getType() == null || request.getType().isBlank()) {
            throw ApiException.badRequest("Expense type is required.");
        }

        OwnerExpense saved = expenseRepository.save(OwnerExpense.builder()
                .owner(owner)
                .type(request.getType().trim())
                .amount(request.getAmount())
                .expenseDate(request.getExpenseDate() == null ? LocalDate.now() : request.getExpenseDate())
                .note(request.getNote())
                .build());
        return toExpenseResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OwnerExpenseResponse> getExpenses(String username, LocalDate start, LocalDate end) {
        User owner = findUser(username, "Owner not found.");
        LocalDate today = LocalDate.now();
        LocalDate rangeStart = start == null ? today.minusDays(INSIGHT_WINDOW_DAYS - 1L) : start;
        LocalDate rangeEnd = end == null ? today : end;
        return expenseRepository.findByOwnerIdAndExpenseDateBetweenOrderByExpenseDateDesc(owner.getId(), rangeStart, rangeEnd)
                .stream()
                .map(this::toExpenseResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OwnerInsightsResponse.BusinessCoachResponse askCoach(String username, OwnerInsightsResponse.BusinessCoachRequest request) {
        OwnerInsightsResponse insights = getOwnerInsights(username);
        String question = request == null || request.getQuestion() == null ? "" : request.getQuestion().toLowerCase(Locale.ROOT);
        return buildCoachResponse(question, insights);
    }

    private OwnerInsightsResponse buildInsights(User owner) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate start = today.minusDays(INSIGHT_WINDOW_DAYS - 1L);
        List<Turf> turfs = owner == null ? turfRepository.findAll() : turfRepository.findByOwnerId(owner.getId());
        List<Booking> bookings = owner == null ? bookingRepository.findAll() : bookingRepository.findByTurfOwnerId(owner.getId());
        List<Booking> confirmed = bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
                .toList();
        List<Booking> recentConfirmed = confirmed.stream()
                .filter(booking -> betweenInclusive(booking.getBookingDate(), start, today))
                .toList();

        BigDecimal weekRevenue = revenueBetween(confirmed, today.minusDays(6), today);
        BigDecimal lastWeekRevenue = revenueBetween(confirmed, today.minusDays(13), today.minusDays(7));
        BigDecimal monthRevenue = revenueBetween(confirmed, monthStart, today);
        BigDecimal recentRevenue = sumRevenue(recentConfirmed);
        BigDecimal expenses = owner == null ? BigDecimal.ZERO : expenseRepository.sumByOwnerAndDateRange(owner.getId(), start, today);

        OwnerInsightsResponse.RevenueIntelligence revenue = OwnerInsightsResponse.RevenueIntelligence.builder()
                .todayRevenue(revenueBetween(confirmed, today, today))
                .weekRevenue(weekRevenue)
                .lastWeekRevenue(lastWeekRevenue)
                .monthRevenue(monthRevenue)
                .averageBookingValue(averageBookingValue(recentConfirmed))
                .weeklyGrowthPercent(percentChange(weekRevenue, lastWeekRevenue))
                .dailyTrend(buildRevenueSeries(recentConfirmed, today.minusDays(13), today))
                .build();

        OwnerInsightsResponse.OccupancyIntelligence occupancy = buildOccupancy(turfs, recentConfirmed);
        List<OwnerInsightsResponse.PeakHourCell> heatmap = buildHeatmap(recentConfirmed, turfs);
        List<OwnerInsightsResponse.PricingSuggestion> pricingSuggestions = buildPricingSuggestions(turfs, recentConfirmed);
        OwnerInsightsResponse.ProfitSummary profit = OwnerInsightsResponse.ProfitSummary.builder()
                .revenue(recentRevenue)
                .expenses(expenses)
                .profit(recentRevenue.subtract(expenses))
                .marginPercent(recentRevenue.compareTo(BigDecimal.ZERO) == 0 ? 0 : round(recentRevenue.subtract(expenses).multiply(BigDecimal.valueOf(100)).divide(recentRevenue, 2, RoundingMode.HALF_UP).doubleValue()))
                .build();

        OwnerInsightsResponse response = OwnerInsightsResponse.builder()
                .revenue(revenue)
                .occupancy(occupancy)
                .heatmap(heatmap)
                .customers(buildCustomers(confirmed))
                .pricingSuggestions(pricingSuggestions)
                .bookingPatterns(buildPatterns(bookings, heatmap, occupancy))
                .profit(profit)
                .forecast(buildForecast(recentRevenue, occupancy))
                .promotions(buildPromotions(occupancy, heatmap))
                .community(buildCommunity(confirmed, owner))
                .recentExpenses(owner == null ? List.of() : getExpenses(owner.getEmail(), start, today).stream().limit(8).toList())
                .build();
        response.setCoach(buildCoachResponse("", response));
        return response;
    }

    private OwnerInsightsResponse.OccupancyIntelligence buildOccupancy(List<Turf> turfs, List<Booking> recentConfirmed) {
        long activeSlots = turfs.stream()
                .flatMap(turf -> slotRepository.findByTurfId(turf.getId()).stream())
                .filter(slot -> Boolean.TRUE.equals(slot.getIsActive()))
                .count();
        long capacity = Math.max(activeSlots * INSIGHT_WINDOW_DAYS, 1);
        long booked = recentConfirmed.size();
        double occupancyPercent = round((booked * 100.0) / capacity);
        Map<Long, Long> bySlot = recentConfirmed.stream()
                .filter(booking -> booking.getSlot() != null)
                .collect(Collectors.groupingBy(booking -> booking.getSlot().getId(), Collectors.counting()));
        long peakBookings = bySlot.values().stream().mapToLong(Long::longValue).max().orElse(0);
        return OwnerInsightsResponse.OccupancyIntelligence.builder()
                .occupancyPercent(occupancyPercent)
                .bookedSlots(booked)
                .availableSlotCapacity(capacity)
                .unusedSlotCapacity(Math.max(capacity - booked, 0))
                .peakUtilizationPercent(round((peakBookings * 100.0) / INSIGHT_WINDOW_DAYS))
                .build();
    }

    private List<OwnerInsightsResponse.PeakHourCell> buildHeatmap(List<Booking> bookings, List<Turf> turfs) {
        long turfCount = Math.max(turfs.size(), 1);
        Map<String, Long> grouped = bookings.stream()
                .filter(booking -> booking.getBookingDate() != null && booking.getSlot() != null && booking.getSlot().getStartTime() != null)
                .collect(Collectors.groupingBy(
                        booking -> booking.getBookingDate().getDayOfWeek().name() + "|" + booking.getSlot().getStartTime().getHour(),
                        Collectors.counting()));

        return grouped.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|");
                    DayOfWeek day = DayOfWeek.valueOf(parts[0]);
                    int hour = Integer.parseInt(parts[1]);
                    long count = entry.getValue();
                    double utilization = round((count * 100.0) / Math.max(turfCount * 4.0, 1.0));
                    return OwnerInsightsResponse.PeakHourCell.builder()
                            .dayOfWeek(day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                            .hour(hour)
                            .hourLabel(formatHour(hour))
                            .bookingCount(count)
                            .utilizationPercent(utilization)
                            .intensity(utilization >= 75 ? "HIGH" : utilization >= 35 ? "MEDIUM" : "LOW")
                            .build();
                })
                .sorted(Comparator.comparing(OwnerInsightsResponse.PeakHourCell::getBookingCount).reversed())
                .limit(12)
                .toList();
    }

    private List<OwnerInsightsResponse.CustomerInsight> buildCustomers(List<Booking> bookings) {
        return bookings.stream()
                .filter(booking -> booking.getUser() != null)
                .collect(Collectors.groupingBy(booking -> booking.getUser().getId()))
                .values()
                .stream()
                .map(customerBookings -> {
                    Booking latest = customerBookings.stream()
                            .max(Comparator.comparing(Booking::getBookingDate))
                            .orElse(customerBookings.get(0));
                    User user = latest.getUser();
                    long count = customerBookings.size();
                    return OwnerInsightsResponse.CustomerInsight.builder()
                            .customerId(user.getId())
                            .customerName(user.getName())
                            .bookings(count)
                            .lifetimeValue(sumRevenue(customerBookings))
                            .lastVisit(latest.getBookingDate())
                            .segment(count >= 8 ? "VIP Player" : count >= 3 ? "Repeat Captain" : "New Customer")
                            .trustScore(trustScoreRepository.findByUserId(user.getId()).map(PlayerTrustScore::getScore).orElse(75))
                            .build();
                })
                .sorted(Comparator.comparing(OwnerInsightsResponse.CustomerInsight::getLifetimeValue).reversed())
                .limit(8)
                .toList();
    }

    private List<OwnerInsightsResponse.PricingSuggestion> buildPricingSuggestions(List<Turf> turfs, List<Booking> bookings) {
        Map<Long, Long> demandBySlot = bookings.stream()
                .filter(booking -> booking.getSlot() != null)
                .collect(Collectors.groupingBy(booking -> booking.getSlot().getId(), Collectors.counting()));

        return turfs.stream()
                .flatMap(turf -> slotRepository.findByTurfId(turf.getId()).stream())
                .filter(slot -> Boolean.TRUE.equals(slot.getIsActive()))
                .sorted(Comparator.comparing(slot -> demandBySlot.getOrDefault(slot.getId(), 0L), Comparator.reverseOrder()))
                .limit(6)
                .map(slot -> {
                    long demand = demandBySlot.getOrDefault(slot.getId(), 0L);
                    boolean highDemand = demand >= 8;
                    boolean lowDemand = demand <= 1;
                    BigDecimal multiplier = highDemand ? BigDecimal.valueOf(1.10) : lowDemand ? BigDecimal.valueOf(0.90) : BigDecimal.ONE;
                    String reason = highDemand
                            ? "High demand slot. Increase price slightly to capture peak-hour value."
                            : lowDemand
                            ? "Low utilization slot. Offer a weekday discount to recover unused capacity."
                            : "Demand is balanced. Keep pricing stable and monitor trend next week.";
                    return OwnerInsightsResponse.PricingSuggestion.builder()
                            .turfId(slot.getTurf().getId())
                            .turfName(slot.getTurf().getName())
                            .slotId(slot.getId())
                            .currentPrice(slot.getPrice())
                            .recommendedPrice(slot.getPrice().multiply(multiplier).setScale(0, RoundingMode.HALF_UP))
                            .reason(reason)
                            .confidence(highDemand || lowDemand ? 82 : 64)
                            .build();
                })
                .toList();
    }

    private List<OwnerInsightsResponse.BusinessPattern> buildPatterns(
            List<Booking> bookings,
            List<OwnerInsightsResponse.PeakHourCell> heatmap,
            OwnerInsightsResponse.OccupancyIntelligence occupancy) {
        long cancelled = bookings.stream().filter(booking -> booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED).count();
        double cancellationRate = bookings.isEmpty() ? 0 : round((cancelled * 100.0) / bookings.size());
        String peak = heatmap.isEmpty() ? "No clear peak hour yet" : heatmap.get(0).getDayOfWeek() + " " + heatmap.get(0).getHourLabel();
        return List.of(
                OwnerInsightsResponse.BusinessPattern.builder()
                        .label("Peak hour")
                        .metric(peak)
                        .insight("Highest demand appears in this window. Review pricing and staffing for it.")
                        .direction("UP")
                        .build(),
                OwnerInsightsResponse.BusinessPattern.builder()
                        .label("Cancellation trend")
                        .metric(cancellationRate + "%")
                        .insight(cancellationRate > 15 ? "Cancellation pressure is high. Use deposits, resale, or waitlist nudges." : "Cancellation behavior is within a manageable range.")
                        .direction(cancellationRate > 15 ? "RISK" : "STABLE")
                        .build(),
                OwnerInsightsResponse.BusinessPattern.builder()
                        .label("Unused capacity")
                        .metric(String.valueOf(occupancy.getUnusedSlotCapacity()))
                        .insight("Unused slots are the best source for promotions and corporate packages.")
                        .direction(occupancy.getOccupancyPercent() < 50 ? "OPPORTUNITY" : "STABLE")
                        .build()
        );
    }

    private OwnerInsightsResponse.ForecastInsight buildForecast(BigDecimal recentRevenue, OwnerInsightsResponse.OccupancyIntelligence occupancy) {
        BigDecimal expected = recentRevenue.divide(BigDecimal.valueOf(INSIGHT_WINDOW_DAYS), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(7))
                .setScale(0, RoundingMode.HALF_UP);
        String demand = occupancy.getOccupancyPercent() >= 70 ? "HIGH" : occupancy.getOccupancyPercent() >= 35 ? "MEDIUM" : "LOW";
        return OwnerInsightsResponse.ForecastInsight.builder()
                .period("Next 7 days")
                .expectedRevenue(expected)
                .expectedOccupancy(Math.min(round(occupancy.getOccupancyPercent() * 1.05), 100))
                .demandLevel(demand)
                .insight(demand.equals("HIGH")
                        ? "Demand is strong. Protect peak slots and avoid broad discounts."
                        : "Use off-peak campaigns and resale/waitlist notifications to lift occupancy.")
                .build();
    }

    private List<OwnerInsightsResponse.PromotionSuggestion> buildPromotions(
            OwnerInsightsResponse.OccupancyIntelligence occupancy,
            List<OwnerInsightsResponse.PeakHourCell> heatmap) {
        List<OwnerInsightsResponse.PromotionSuggestion> suggestions = new ArrayList<>();
        if (occupancy.getOccupancyPercent() < 55) {
            suggestions.add(OwnerInsightsResponse.PromotionSuggestion.builder()
                    .title("Weekday Happy Hour")
                    .description("Offer a limited discount for afternoon slots with weak demand.")
                    .suggestedDiscountPercent(12)
                    .expectedImpact("Recover unused capacity without touching peak-hour pricing.")
                    .build());
        }
        if (!heatmap.isEmpty()) {
            suggestions.add(OwnerInsightsResponse.PromotionSuggestion.builder()
                    .title("Peak Slot Protection")
                    .description("Keep " + heatmap.get(0).getDayOfWeek() + " " + heatmap.get(0).getHourLabel() + " slots premium and move discounts to adjacent hours.")
                    .suggestedDiscountPercent(0)
                    .expectedImpact("Preserve margin where players already show high intent.")
                    .build());
        }
        suggestions.add(OwnerInsightsResponse.PromotionSuggestion.builder()
                .title("Repeat Captain Reward")
                .description("Send a small credit to repeat captains after their third booking.")
                .suggestedDiscountPercent(8)
                .expectedImpact("Improve retention without offering public discounts.")
                .build());
        return suggestions;
    }

    private OwnerInsightsResponse.CommunityInsight buildCommunity(List<Booking> confirmed, User owner) {
        Map<Long, Long> counts = confirmed.stream()
                .filter(booking -> booking.getUser() != null)
                .collect(Collectors.groupingBy(booking -> booking.getUser().getId(), Collectors.counting()));
        long repeat = counts.values().stream().filter(count -> count >= 2).count();
        long frequent = counts.values().stream().filter(count -> count >= 5).count();
        long activeTeams = owner == null ? teamRepository.count() : teamRepository.findByCaptainIdOrderByCreatedAtDesc(owner.getId()).size();
        String score = repeat >= 10 ? "Strong" : repeat >= 3 ? "Growing" : "Early";
        return OwnerInsightsResponse.CommunityInsight.builder()
                .repeatCustomers(repeat)
                .frequentCaptains(frequent)
                .activeTeams(activeTeams)
                .communityScore(score)
                .build();
    }

    private OwnerInsightsResponse.BusinessCoachResponse buildCoachResponse(String question, OwnerInsightsResponse insights) {
        List<String> actions = new ArrayList<>();
        if (question.contains("price") || question.contains("pricing")) {
            actions.add("Apply only slot-level pricing changes; avoid changing all turf prices at once.");
            actions.add("Increase peak slots by 5-10% only when occupancy stays above 70%.");
        } else if (question.contains("revenue") || question.contains("profit")) {
            actions.add("Protect high-demand slots and move discounts to low-demand slots.");
            actions.add("Track electricity, staff, rent, and maintenance expenses weekly.");
        } else {
            actions.add("Review peak-hour heatmap before launching discounts.");
            actions.add("Use repeat-customer rewards before public coupons.");
        }
        String answer = "Your current occupancy is " + insights.getOccupancy().getOccupancyPercent()
                + "% and the next-week forecast is Rs " + insights.getForecast().getExpectedRevenue()
                + ". Focus on off-peak recovery first, then tune premium slots.";
        return OwnerInsightsResponse.BusinessCoachResponse.builder()
                .answer(answer)
                .recommendedActions(actions)
                .confidence(84)
                .build();
    }

    private List<AnalyticsPoint> buildRevenueSeries(List<Booking> bookings, LocalDate start, LocalDate end) {
        Map<LocalDate, List<Booking>> byDate = bookings.stream()
                .filter(booking -> betweenInclusive(booking.getBookingDate(), start, end))
                .collect(Collectors.groupingBy(Booking::getBookingDate));
        return start.datesUntil(end.plusDays(1))
                .map(date -> AnalyticsPoint.builder()
                        .date(date)
                        .bookings((long) byDate.getOrDefault(date, List.of()).size())
                        .revenue(sumRevenue(byDate.getOrDefault(date, List.of())))
                        .build())
                .toList();
    }

    private User findUser(String username, String message) {
        return userRepository.findByEmailOrPhone(username).orElseThrow(() -> ApiException.notFound(message));
    }

    private BigDecimal revenueBetween(List<Booking> bookings, LocalDate start, LocalDate end) {
        return sumRevenue(bookings.stream().filter(booking -> betweenInclusive(booking.getBookingDate(), start, end)).toList());
    }

    private BigDecimal sumRevenue(List<Booking> bookings) {
        return bookings.stream().map(booking -> booking.getAmount() == null ? BigDecimal.ZERO : booking.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal averageBookingValue(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return sumRevenue(bookings).divide(BigDecimal.valueOf(bookings.size()), 2, RoundingMode.HALF_UP);
    }

    private boolean betweenInclusive(LocalDate date, LocalDate start, LocalDate end) {
        return date != null && !date.isBefore(start) && !date.isAfter(end);
    }

    private Double percentChange(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) == 0 ? 0 : 100.0;
        }
        return round(current.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous, 2, RoundingMode.HALF_UP).doubleValue());
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String formatHour(int hour) {
        LocalTime time = LocalTime.of(hour, 0);
        int displayHour = time.getHour() % 12 == 0 ? 12 : time.getHour() % 12;
        return displayHour + (time.getHour() < 12 ? " AM" : " PM");
    }

    private OwnerExpenseResponse toExpenseResponse(OwnerExpense expense) {
        return OwnerExpenseResponse.builder()
                .id(expense.getId())
                .type(expense.getType())
                .amount(expense.getAmount())
                .expenseDate(expense.getExpenseDate())
                .note(expense.getNote())
                .build();
    }
}
