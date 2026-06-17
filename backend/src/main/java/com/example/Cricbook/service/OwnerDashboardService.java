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
                        || booking.getStatus() == BookingStatus.COMPLETED)
                .toList();

        BigDecimal totalRevenue = sumRevenue(revenueBookings);
        BigDecimal todayRevenue = sumRevenue(revenueBookings.stream()
                .filter(booking -> today.equals(booking.getBookingDate()))
                .toList());
        BigDecimal monthlyRevenue = sumRevenue(revenueBookings.stream()
                .filter(booking -> !booking.getBookingDate().isBefore(monthStart))
                .toList());

        long totalBookings = bookings.size();
        long cancelled = bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED)
                .count();

        double cancellationPercent = totalBookings == 0 ? 0 : round((cancelled * 100.0) / totalBookings);
        double occupancyPercent = turfs.isEmpty() ? 0 : round((revenueBookings.size() * 100.0) / Math.max(turfs.size() * 30.0, 1.0));

        TurfResponse popularTurf = turfs.stream()
                .max(Comparator.comparing(turf -> turf.getTotalReviews() == null ? 0 : turf.getTotalReviews()))
                .map(turfMapper::toTurfResponse)
                .orElse(null);

        return OwnerDashboardResponse.builder()
                .todayBookings(bookings.stream().filter(booking -> today.equals(booking.getBookingDate())).count())
                .totalBookings(totalBookings)
                .todayRevenue(todayRevenue)
                .monthlyRevenue(monthlyRevenue)
                .totalRevenue(totalRevenue)
                .occupancyPercent(occupancyPercent)
                .cancellationPercent(cancellationPercent)
                .activeTurfs(turfs.stream().filter(turf -> turf.getStatus() == TurfStatus.ACTIVE).count())
                .inactiveTurfs(turfs.stream().filter(turf -> turf.getStatus() == TurfStatus.INACTIVE).count())
                .maintenanceTurfs(turfs.stream().filter(turf -> turf.getStatus() == TurfStatus.MAINTENANCE).count())
                .popularTurf(popularTurf)
                .revenueSeries(buildRevenueSeries(revenueBookings, today.minusDays(13), today))
                .upcomingBookings(bookingService.getOwnerBookings(BookingStatus.CONFIRMED).stream().limit(8).toList())
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
