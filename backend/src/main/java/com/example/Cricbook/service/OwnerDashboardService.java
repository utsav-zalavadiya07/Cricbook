package com.example.Cricbook.service;

import com.example.Cricbook.dto.AnalyticsPoint;
import com.example.Cricbook.dto.BookingResponse;
import com.example.Cricbook.dto.OwnerDashboardResponse;
import com.example.Cricbook.dto.ReviewResponse;
import com.example.Cricbook.dto.TurfResponse;
import com.example.Cricbook.entity.Booking;
import com.example.Cricbook.entity.Turf;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.enums.TurfStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.mapper.TurfMapper;
import com.example.Cricbook.repository.BookingRepository;
import com.example.Cricbook.repository.TurfRepository;
import com.example.Cricbook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnerDashboardService {

    private final UserRepository userRepository;
    private final TurfRepository turfRepository;
    private final BookingRepository bookingRepository;
    private final ReviewService reviewService;
    private final BookingService bookingService;
    private final TurfMapper turfMapper;

    @Transactional(readOnly = true)
    public OwnerDashboardResponse getDashboard(String username) {
        User owner = userRepository.findByEmailOrPhone(username)
                .orElseThrow(() -> new ApiException("Owner not found"));

        List<Turf> turfs = turfRepository.findByOwnerId(owner.getId());
        List<Booking> bookings = bookingRepository.findByTurfOwnerId(owner.getId());
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        List<Booking> revenueBookings = bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED
                        || booking.getStatus() == BookingStatus.CHECKED_IN
                        || booking.getStatus() == BookingStatus.COMPLETED
                        || booking.getStatus() == BookingStatus.NO_SHOW)
                .toList();

        BigDecimal totalRevenue = sumRevenue(revenueBookings);
        BigDecimal todayRevenue = sumRevenue(revenueBookings.stream()
                .filter(booking -> today.equals(booking.getBookingDate()))
                .toList());
        BigDecimal monthlyRevenue = sumRevenue(revenueBookings.stream()
                .filter(booking -> !booking.getBookingDate().isBefore(monthStart) && !booking.getBookingDate().isAfter(today.withDayOfMonth(today.lengthOfMonth())))
                .toList());

        LocalDate current14Start = today.minusDays(13);
        LocalDate previous14Start = today.minusDays(27);
        LocalDate previous14End = today.minusDays(14);

        BigDecimal last14DaysRevenue = sumRevenue(revenueBookings.stream()
                .filter(booking -> !booking.getBookingDate().isBefore(current14Start))
                .toList());

        BigDecimal previous14DaysRevenue = sumRevenue(revenueBookings.stream()
                .filter(booking -> !booking.getBookingDate().isBefore(previous14Start) && !booking.getBookingDate().isAfter(previous14End))
                .toList());

        double revenueGrowthPercent = 0.0;
        if (previous14DaysRevenue.compareTo(BigDecimal.ZERO) > 0) {
            revenueGrowthPercent = round((last14DaysRevenue.subtract(previous14DaysRevenue).doubleValue() / previous14DaysRevenue.doubleValue()) * 100.0);
        } else if (last14DaysRevenue.compareTo(BigDecimal.ZERO) > 0) {
            revenueGrowthPercent = 100.0;
        }

        long totalBookings = bookings.size();
        long cancelled = bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED || booking.getStatus() == BookingStatus.REFUNDED)
                .count();

        double cancellationPercent = totalBookings == 0 ? 0 : round((cancelled * 100.0) / totalBookings);

        List<Booking> currentMonthBookings = revenueBookings.stream()
                .filter(booking -> !booking.getBookingDate().isBefore(monthStart) && !booking.getBookingDate().isAfter(today.withDayOfMonth(today.lengthOfMonth())))
                .toList();
        
        // Use an estimated max capacity of 30 days * 10 slots = 300 per turf
        double maxMonthlyCapacity = Math.max(turfs.size() * 300.0, 1.0);
        double occupancyPercent = turfs.isEmpty() ? 0 : round((currentMonthBookings.size() * 100.0) / maxMonthlyCapacity);

        TurfResponse popularTurf = turfs.stream()
                .max(Comparator.comparing(turf -> turf.getTotalReviews() == null ? 0 : turf.getTotalReviews()))
                .map(turfMapper::toTurfResponse)
                .orElse(null);

        return OwnerDashboardResponse.builder()
                .todayBookings(bookings.stream().filter(booking -> today.equals(booking.getBookingDate()) && booking.getStatus() != BookingStatus.CANCELLED && booking.getStatus() != BookingStatus.EXPIRED && booking.getStatus() != BookingStatus.REFUNDED).count())
                .totalBookings(totalBookings)
                .todayRevenue(todayRevenue)
                .monthlyRevenue(monthlyRevenue)
                .totalRevenue(totalRevenue)
                .last14DaysRevenue(last14DaysRevenue)
                .revenueGrowthPercent(revenueGrowthPercent)
                .occupancyPercent(occupancyPercent)
                .cancellationPercent(cancellationPercent)
                .activeTurfs(turfs.stream().filter(turf -> turf.getStatus() == TurfStatus.ACTIVE).count())
                .inactiveTurfs(turfs.stream().filter(turf -> turf.getStatus() == TurfStatus.INACTIVE).count())
                .maintenanceTurfs(turfs.stream().filter(turf -> turf.getStatus() == TurfStatus.MAINTENANCE).count())
                .popularTurf(popularTurf)
                .revenueSeries(buildRevenueSeries(revenueBookings, today.minusDays(13), today))
                .upcomingBookings(bookingService.getUpcomingOwnerBookings(username).stream().limit(8).toList())
                .recentReviews(reviewService.getOwnerReviews(username).stream().limit(6).toList())
                .build();
    }

    private BigDecimal sumRevenue(List<Booking> bookings) {
        return bookings.stream()
                .map(Booking::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<AnalyticsPoint> buildRevenueSeries(List<Booking> bookings, LocalDate start, LocalDate end) {
        Map<LocalDate, List<Booking>> byDate = bookings.stream()
                .filter(booking -> !booking.getBookingDate().isBefore(start) && !booking.getBookingDate().isAfter(end))
                .collect(Collectors.groupingBy(Booking::getBookingDate));

        return start.datesUntil(end.plusDays(1))
                .map(date -> {
                    List<Booking> dayBookings = byDate.getOrDefault(date, List.of());
                    return AnalyticsPoint.builder()
                            .date(date)
                            .bookings((long) dayBookings.size())
                            .revenue(sumRevenue(dayBookings))
                            .build();
                })
                .toList();
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
