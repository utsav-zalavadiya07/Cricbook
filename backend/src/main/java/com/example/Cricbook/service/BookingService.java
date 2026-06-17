package com.example.Cricbook.service;

import com.example.Cricbook.dto.*;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.AttendanceStatus;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.enums.MembershipStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import com.example.Cricbook.utils.NotificationTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private static final int BOOKING_HOLD_MINUTES = 5;
    private static final int CANCELLATION_CUTOFF_HOURS = 24;
    private static final int CHECK_IN_OPEN_HOURS = 2;
    private static final int CHECK_IN_GRACE_MINUTES = 15;
    private static final List<BookingStatus> ACTIVE_BOOKING_STATUSES = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN);
    private static final List<BookingStatus> REVENUE_BOOKING_STATUSES = List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN, BookingStatus.COMPLETED);

    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final SlotAvailabilityRepository slotAvailabilityRepository;
    private final TurfRepository turfRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SlotPriceOverrideRepository slotPriceOverrideRepository;
    private final UserMembershipRepository userMembershipRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final TrustAttendanceRepository trustAttendanceRepository;
    private final BookingTimelineRepository bookingTimelineRepository;
    private final NoShowCaseRepository noShowCaseRepository;
    private final BookingCancellationRequestRepository bookingCancellationRequestRepository;
    @org.springframework.context.annotation.Lazy
    private final MonetizationGateService monetizationGateService;
    @org.springframework.context.annotation.Lazy
    private final LoyaltyService loyaltyService;
    @org.springframework.context.annotation.Lazy
    private final com.example.Cricbook.service.ai.AIDynamicPricingService aiDynamicPricingService;
    @org.springframework.context.annotation.Lazy
    private final TrustScoreService trustScoreService;
    private final MultiSportService multiSportService;

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        validateCreateBookingRequest(request);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = findUser(username)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
        return createBookingForUser(user, request.getTurfId(), request.getSlotId(), request.getBookingDate(), requestedSport(request.getSportCode(), request.getSport()));
    }

    @Transactional
    public List<BookingResponse> createBatchBooking(CreateBatchBookingRequest request) {
        validateBatchBookingRequest(request);

        return new LinkedHashSet<>(request.getSlotIds()).stream()
                .map(slotId -> {
                    String username = SecurityContextHolder.getContext().getAuthentication().getName();
                    User user = findUser(username)
                            .orElseThrow(() -> ApiException.notFound("User account was not found."));
                    return createBookingForUser(user, request.getTurfId(), slotId, request.getBookingDate(), requestedSport(request.getSportCode(), request.getSport()));
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public BookingResponse createBookingForUser(User user, Long turfId, Long slotId, java.time.LocalDate bookingDate) {
        return createBookingForUser(user, turfId, slotId, bookingDate, "CRICKET");
    }

    @Transactional
    public BookingResponse createBookingForUser(User user, Long turfId, Long slotId, java.time.LocalDate bookingDate, String sportCode) {
        validateCreateBookingValues(turfId, slotId, bookingDate);
        if (monetizationGateService != null) {
            monetizationGateService.trackAndCheckUsage(user.getId(), "BOOKINGS");
        }

        Turf turf = turfRepository.findById(turfId)
                .orElseThrow(() -> ApiException.notFound("Turf was not found."));
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> ApiException.notFound("Slot was not found."));
        validateSlotForBooking(turf, slot, bookingDate);
        validateSportForBooking(turf.getId(), sportCode);
        expireInactivePendingHolds(bookingDate, slotId);

        // 1. Lock slot
        SlotAvailability sa = slotAvailabilityRepository
            .lockSlot(slotId, bookingDate)
            .orElseThrow(() -> ApiException.notFound("Slot was not found for the selected date."));

        // 2. Check availability
        if (!sa.getIsAvailable() || sa.getIsBlocked()) {
            throw ApiException.conflict("Selected slot is no longer available.");
        }

        if (hasActiveBooking(bookingDate, slotId)) {
            sa.setIsAvailable(false);
            slotAvailabilityRepository.save(sa);
            throw ApiException.conflict("Selected slot is already held or booked.");
        }

        // 3. Create Booking
        Booking booking = Booking.builder()
                .user(user)
                .turf(turf)
                .slot(slot)
                .sport(normalizeSportCode(sportCode))
                .bookingDate(bookingDate)
                .status(BookingStatus.PENDING)
                .amount(resolveBookingAmount(slot, bookingDate, user))
                .expiryTime(LocalDateTime.now().plusMinutes(BOOKING_HOLD_MINUTES))
                .build();

        Booking savedBooking;
        try {
            savedBooking = bookingRepository.save(booking);
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("Selected slot is already held or booked.");
        }

        // 4. Mark slot unavailable
        sa.setIsAvailable(false);
        slotAvailabilityRepository.save(sa);

        return mapToResponse(savedBooking);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public BookingResponse confirmBooking(Long bookingId, ConfirmBookingRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking was not found."));
        User currentUser = currentUser();
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw ApiException.forbidden("You do not have permission to confirm this booking.");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw ApiException.conflict("Booking cannot be confirmed because it is no longer pending.");
        }

        if (booking.getExpiryTime() != null && booking.getExpiryTime().isBefore(LocalDateTime.now())) {
            expireBookingAndReleaseSlot(booking);
            throw ApiException.conflict("Booking session has expired. Please create a new booking.");
        }

        if ("SUCCESS".equalsIgnoreCase(request.getPaymentStatus())) {
            booking.setStatus(BookingStatus.CONFIRMED);
            
            User user = booking.getUser();
            notificationService.pushNotification(
                user.getId(),
                user.getEmail(),
                "Booking Confirmed",
                NotificationTemplates.bookingConfirmed(booking.getBookingDate())
            );

            if (loyaltyService != null) {
                loyaltyService.earnPoints(user, 10, "Booking turf on " + booking.getBookingDate());
                loyaltyService.rewardReferralIfPending(user);
            }
        } else {
            // If payment failed, we cancel the booking and release slots
            return cancelBooking(bookingId);
        }

        return mapToResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking was not found."));
        User currentUser = currentUser();
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw ApiException.forbidden("You do not have permission to cancel this booking.");
        }

        return cancelBookingInternal(booking);
    }

    private BookingResponse cancelBookingInternal(Booking booking) {
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED || booking.getStatus() == BookingStatus.REFUNDED) {
            throw ApiException.conflict("Booking is already cancelled, expired, or refunded.");
        }
        if (booking.getStatus() == BookingStatus.CHECKED_IN || booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.NO_SHOW) {
            throw ApiException.conflict("Booking has already entered operations and cannot be cancelled.");
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED
                && bookingStartsAt(booking).isBefore(LocalDateTime.now().plusHours(CANCELLATION_CUTOFF_HOURS))) {
            throw ApiException.conflict("Confirmed bookings can only be cancelled at least 24 hours before the slot starts.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        releaseSlot(booking);
        if (trustScoreService != null) {
            trustScoreService.recordCancellation(booking);
        }

        User user = booking.getUser();
        notificationService.pushNotification(
            user.getId(),
            user.getEmail(),
            "Booking Cancelled",
            NotificationTemplates.bookingCancelled()
        );

        return mapToResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse cancelOwnerBooking(Long bookingId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User owner = findUser(username)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking was not found."));

        if (!booking.getTurf().getOwner().getId().equals(owner.getId())) {
            throw ApiException.forbidden("You do not have permission to cancel this booking.");
        }

        return cancelBookingInternal(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking was not found."));
        User currentUser = currentUser();
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw ApiException.forbidden("You do not have permission to view this booking.");
        }
        return mapToResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = findUser(username)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
        Map<Long, String> paymentStatuses = paymentStatusesForUser(user.getId());
        return bookingRepository.findByUserIdOrderByBookingDateDescCreatedAtDesc(user.getId())
                .stream()
                .map(booking -> mapToResponse(booking, paymentStatuses.get(booking.getId())))
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getOwnerBookings(BookingStatus status) {
        return getOwnerBookings(status, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getOwnerBookings(BookingStatus status, LocalDate date, Long turfId, String query) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User owner = findUser(username)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));

        String normalizedQuery = safe(query).trim().toLowerCase();
        Map<Long, String> paymentStatuses = paymentStatusesForOwner(owner.getId());
        Map<Long, TrustAttendance> attendanceByBooking = attendanceForOwner(owner.getId());

        return bookingsForOwner(owner.getId()).stream()
                .filter(booking -> status == null || booking.getStatus() == status)
                .filter(booking -> date == null || date.equals(booking.getBookingDate()))
                .filter(booking -> turfId == null || booking.getTurf().getId().equals(turfId))
                .filter(booking -> matchesOwnerBookingQuery(booking, normalizedQuery))
                .sorted(ownerBookingOrder())
                .map(booking -> mapToResponse(booking, paymentStatuses.get(booking.getId()), attendanceByBooking.get(booking.getId())))
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BookingResponse getOwnerBooking(Long bookingId) {
        Booking booking = requireOwnerBooking(bookingId);
        return mapToResponse(
                booking,
                paymentRepository.findByBookingId(bookingId).map(payment -> normalizePaymentStatus(payment.getStatus())).orElse(null),
                trustAttendanceRepository.findByBookingId(bookingId).orElse(null)
        );
    }

    @Transactional(readOnly = true)
    public OwnerBookingDetailsResponse getOwnerBookingDetails(Long bookingId) {
        Booking booking = requireOwnerBooking(bookingId);
        String latestPaymentStatus = paymentRepository.findByBookingId(bookingId)
                .map(payment -> normalizePaymentStatus(payment.getStatus()))
                .orElse(inferPaymentStatus(booking.getStatus()));
        String latestCancellationStatus = bookingCancellationRequestRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .findFirst()
                .map(BookingCancellationRequest::getStatus)
                .orElse(null);

        return OwnerBookingDetailsResponse.builder()
                .booking(mapToResponse(booking, latestPaymentStatus, trustAttendanceRepository.findByBookingId(bookingId).orElse(null)))
                .latestPaymentStatus(latestPaymentStatus)
                .latestCancellationStatus(latestCancellationStatus)
                .timeline(bookingTimelineRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream()
                        .map(this::mapTimeline)
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public OwnerBookingOperationsSummary getOwnerOperationsSummary() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User owner = findUser(username)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));

        LocalDate today = LocalDate.now();
        List<Booking> bookings = bookingsForOwner(owner.getId());
        Map<Long, TrustAttendance> attendanceByBooking = attendanceForOwner(owner.getId());
        long totalBookings = bookings.size();
        long todayBookings = bookings.stream().filter(booking -> today.equals(booking.getBookingDate())).count();
        long upcomingBookings = bookings.stream().filter(booking -> !booking.getBookingDate().isBefore(today) && booking.getStatus() == BookingStatus.CONFIRMED).count();
        long activeMatches = bookings.stream().filter(booking -> today.equals(booking.getBookingDate()) && booking.getStatus() == BookingStatus.CHECKED_IN).count();
        long pendingArrivals = bookings.stream().filter(booking -> today.equals(booking.getBookingDate()) && booking.getStatus() == BookingStatus.CONFIRMED).count();
        long completedBookings = bookings.stream().filter(booking -> booking.getStatus() == BookingStatus.COMPLETED).count();
        long cancelledBookings = bookings.stream().filter(booking -> booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED || booking.getStatus() == BookingStatus.REFUNDED).count();
        long noShowBookings = bookings.stream().filter(booking -> booking.getStatus() == BookingStatus.NO_SHOW).count();
        long checkedInToday = attendanceByBooking.values().stream()
                .filter(attendance -> attendance.getCheckinTime() != null && attendance.getCheckinTime().toLocalDate().equals(today))
                .filter(attendance -> attendance.getStatus() == AttendanceStatus.ARRIVED || attendance.getStatus() == AttendanceStatus.LATE)
                .count();
        long attendanceEligible = bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CHECKED_IN || booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.NO_SHOW)
                .count();

        return OwnerBookingOperationsSummary.builder()
                .totalBookings(totalBookings)
                .todayBookings(todayBookings)
                .upcomingBookings(upcomingBookings)
                .activeMatches(activeMatches)
                .pendingArrivals(pendingArrivals)
                .checkedInToday(checkedInToday)
                .completedBookings(completedBookings)
                .cancelledBookings(cancelledBookings)
                .noShowBookings(noShowBookings)
                .todayRevenue(sumRevenue(bookings.stream()
                        .filter(booking -> today.equals(booking.getBookingDate()))
                        .filter(booking -> REVENUE_BOOKING_STATUSES.contains(booking.getStatus()))
                        .toList()))
                .attendanceRate(percent(checkedInToday + completedBookings, attendanceEligible))
                .noShowRate(percent(noShowBookings, attendanceEligible))
                .recentActivity(bookings.stream()
                        .flatMap(booking -> bookingTimelineRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()).stream())
                        .sorted(Comparator.comparing(BookingTimeline::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(12)
                        .map(this::mapTimeline)
                        .toList())
                .build();
    }

    @Transactional
    public BookingResponse checkInOwnerBooking(Long bookingId, OwnerBookingActionRequest request) {
        Booking booking = requireOwnerBooking(bookingId);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw ApiException.conflict("Only confirmed bookings can be checked in.");
        }
        if (trustAttendanceRepository.findByBookingId(bookingId).isPresent()) {
            throw ApiException.conflict("This booking already has an attendance record.");
        }
        validateCheckInWindow(booking);

        AttendanceStatus attendanceStatus = LocalDateTime.now().isAfter(bookingStartsAt(booking).plusMinutes(CHECK_IN_GRACE_MINUTES))
                ? AttendanceStatus.LATE
                : AttendanceStatus.ARRIVED;
        TrustAttendance attendance = trustAttendanceRepository.save(TrustAttendance.builder()
                .booking(booking)
                .user(booking.getUser())
                .status(attendanceStatus)
                .checkinTime(LocalDateTime.now())
                .build());
        booking.setStatus(BookingStatus.CHECKED_IN);
        Booking saved = bookingRepository.save(booking);
        writeTimeline(saved, "CHECKED_IN", safe(request == null ? null : request.getNote(), "Player checked in by owner operations."));
        return mapToResponse(saved, paymentRepository.findByBookingId(bookingId).map(payment -> normalizePaymentStatus(payment.getStatus())).orElse(null), attendance);
    }

    @Transactional
    public BookingResponse completeOwnerBooking(Long bookingId, OwnerBookingActionRequest request) {
        Booking booking = requireOwnerBooking(bookingId);
        if (booking.getStatus() != BookingStatus.CHECKED_IN && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw ApiException.conflict("Only confirmed or checked-in bookings can be completed.");
        }
        if (bookingEndsAt(booking).isAfter(LocalDateTime.now())) {
            throw ApiException.conflict("Booking can be completed only after the slot end time.");
        }
        booking.setStatus(BookingStatus.COMPLETED);
        Booking saved = bookingRepository.save(booking);
        writeTimeline(saved, "COMPLETED", safe(request == null ? null : request.getNote(), "Booking completed by owner operations."));
        return mapToResponse(saved, paymentRepository.findByBookingId(bookingId).map(payment -> normalizePaymentStatus(payment.getStatus())).orElse(null), trustAttendanceRepository.findByBookingId(bookingId).orElse(null));
    }

    @Transactional
    public BookingResponse markOwnerNoShow(Long bookingId, OwnerBookingActionRequest request) {
        Booking booking = requireOwnerBooking(bookingId);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw ApiException.conflict("Only confirmed bookings without check-in can be marked no-show.");
        }
        if (bookingEndsAt(booking).isAfter(LocalDateTime.now())) {
            throw ApiException.conflict("No-show can be marked only after the slot end time.");
        }
        if (trustAttendanceRepository.findByBookingId(bookingId).isPresent()) {
            throw ApiException.conflict("This booking already has attendance and cannot be marked no-show.");
        }
        TrustAttendance attendance = trustAttendanceRepository.save(TrustAttendance.builder()
                .booking(booking)
                .user(booking.getUser())
                .status(AttendanceStatus.NO_SHOW)
                .build());
        noShowCaseRepository.findByBookingId(bookingId).orElseGet(() -> noShowCaseRepository.save(NoShowCase.builder()
                .booking(booking)
                .user(booking.getUser())
                .noShowType("PLAYER")
                .status("OPEN")
                .penalty(BigDecimal.ZERO)
                .notes(safe(request == null ? null : request.getReason(), "Owner marked player as no-show."))
                .build()));
        booking.setStatus(BookingStatus.NO_SHOW);
        Booking saved = bookingRepository.save(booking);
        writeTimeline(saved, "NO_SHOW", safe(request == null ? null : request.getReason(), "Owner marked player as no-show."));
        return mapToResponse(saved, paymentRepository.findByBookingId(bookingId).map(payment -> normalizePaymentStatus(payment.getStatus())).orElse(null), attendance);
    }

    @Transactional
    public OwnerBookingTimelineResponse addOwnerBookingNote(Long bookingId, OwnerBookingActionRequest request) {
        Booking booking = requireOwnerBooking(bookingId);
        String note = safe(request == null ? null : request.getNote(), "");
        if (note.isBlank()) {
            throw ApiException.badRequest("Note is required.");
        }
        if (note.length() > 700) {
            throw ApiException.badRequest("Note cannot exceed 700 characters.");
        }
        return mapTimeline(writeTimeline(booking, "OWNER_NOTE", note));
    }

    private void releaseSlot(Booking booking) {
        SlotAvailability sa = slotAvailabilityRepository
            .findBySlotIdAndDate(booking.getSlot().getId(), booking.getBookingDate())
            .orElseThrow(() -> ApiException.notFound("Slot availability record was not found."));

        boolean activeBookingExists = hasActiveBooking(booking.getBookingDate(), booking.getSlot().getId());
        sa.setIsAvailable(!Boolean.TRUE.equals(sa.getIsBlocked()) && !activeBookingExists);
        slotAvailabilityRepository.save(sa);
    }

    private Booking requireOwnerBooking(Long bookingId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User owner = findUser(username)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking was not found."));

        if (booking.getTurf() == null || booking.getTurf().getOwner() == null || !booking.getTurf().getOwner().getId().equals(owner.getId())) {
            throw ApiException.forbidden("You do not have permission to manage this booking.");
        }
        return booking;
    }

    private List<Booking> bookingsForOwner(Long ownerId) {
        List<Booking> bookings = bookingRepository.findByTurfOwnerId(ownerId);
        return bookings == null ? List.of() : bookings;
    }

    private Comparator<Booking> ownerBookingOrder() {
        return Comparator
                .comparing(Booking::getBookingDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Booking::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private boolean matchesOwnerBookingQuery(Booking booking, String normalizedQuery) {
        if (normalizedQuery == null || normalizedQuery.isBlank()) {
            return true;
        }
        return String.valueOf(booking.getId()).equals(normalizedQuery)
                || safe(booking.getUser().getName()).toLowerCase().contains(normalizedQuery)
                || safe(booking.getUser().getPhone()).toLowerCase().contains(normalizedQuery)
                || safe(booking.getUser().getEmail()).toLowerCase().contains(normalizedQuery)
                || safe(booking.getTurf().getName()).toLowerCase().contains(normalizedQuery);
    }

    private Map<Long, String> paymentStatusesForOwner(Long ownerId) {
        Map<Long, String> statuses = new HashMap<>();
        for (Booking booking : bookingsForOwner(ownerId)) {
            paymentRepository.findByBookingId(booking.getId())
                    .ifPresent(payment -> statuses.put(booking.getId(), normalizePaymentStatus(payment.getStatus())));
        }
        return statuses;
    }

    private Map<Long, TrustAttendance> attendanceForOwner(Long ownerId) {
        Map<Long, TrustAttendance> attendance = new HashMap<>();
        for (Booking booking : bookingsForOwner(ownerId)) {
            trustAttendanceRepository.findByBookingId(booking.getId())
                    .ifPresent(item -> attendance.put(booking.getId(), item));
        }
        return attendance;
    }

    private void validateCheckInWindow(Booking booking) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startsAt = bookingStartsAt(booking);
        LocalDateTime endsAt = bookingEndsAt(booking);
        if (now.isBefore(startsAt.minusHours(CHECK_IN_OPEN_HOURS))) {
            throw ApiException.conflict("Check-in opens two hours before the slot starts.");
        }
        if (now.isAfter(endsAt.plusMinutes(30))) {
            throw ApiException.conflict("Check-in window is closed. Mark this booking as no-show or complete it.");
        }
    }

    private BookingTimeline writeTimeline(Booking booking, String eventType, String details) {
        return bookingTimelineRepository.save(BookingTimeline.builder()
                .booking(booking)
                .eventType(eventType)
                .performedBy(actor())
                .details(safe(details, eventType))
                .build());
    }

    private OwnerBookingTimelineResponse mapTimeline(BookingTimeline item) {
        return OwnerBookingTimelineResponse.builder()
                .id(item.getId())
                .bookingId(item.getBooking().getId())
                .eventType(item.getEventType())
                .performedBy(item.getPerformedBy())
                .details(item.getDetails())
                .createdAt(item.getCreatedAt())
                .build();
    }

    private BigDecimal sumRevenue(List<Booking> bookings) {
        return bookings.stream()
                .map(Booking::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private double percent(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String actor() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(authentication -> safe(authentication.getName(), "owner"))
                .orElse("owner");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    // Runs every 1 minute to expire pending bookings older than 5 minutes
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expirePendingBookings() {
        List<Booking> expiredBookings = bookingRepository.findByStatusAndExpiryTimeBefore(BookingStatus.PENDING, LocalDateTime.now());
        
        for (Booking booking : expiredBookings) {
            expireBookingAndReleaseSlot(booking);
            
            User user = booking.getUser();
            notificationService.pushNotification(
                user.getId(),
                user.getEmail(),
                "Booking Expired",
                NotificationTemplates.bookingExpired()
            );
            
            log.info("Expired booking: {}", booking.getId());
        }
    }

    private void expireBookingAndReleaseSlot(Booking booking) {
        booking.setStatus(BookingStatus.EXPIRED);
        bookingRepository.save(booking);
        releaseSlot(booking);
    }

    private void expireInactivePendingHolds(java.time.LocalDate bookingDate, Long slotId) {
        bookingsForSlot(bookingDate, slotId, List.of(BookingStatus.PENDING))
                .filter(booking -> !isActiveBookingHold(booking))
                .forEach(this::expireBookingAndReleaseSlot);
    }

    private boolean hasActiveBooking(java.time.LocalDate bookingDate, Long slotId) {
        return bookingsForSlot(bookingDate, slotId, ACTIVE_BOOKING_STATUSES)
                .anyMatch(this::isActiveBookingHold);
    }

    private java.util.stream.Stream<Booking> bookingsForSlot(
            java.time.LocalDate bookingDate,
            Long slotId,
            List<BookingStatus> statuses
    ) {
        List<Booking> bookings = bookingRepository.findByBookingDateAndSlotIdAndStatusIn(bookingDate, slotId, statuses);
        return bookings == null ? java.util.stream.Stream.empty() : bookings.stream();
    }

    private boolean isActiveBookingHold(Booking booking) {
        if (booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.CHECKED_IN) {
            return true;
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            return false;
        }
        return booking.getExpiryTime() == null || booking.getExpiryTime().isAfter(LocalDateTime.now());
    }

    private BookingResponse mapToResponse(Booking booking) {
        return mapToResponse(booking, null);
    }

    private BookingResponse mapToResponse(Booking booking, String paymentStatus) {
        return mapToResponse(booking, paymentStatus, null);
    }

    private BookingResponse mapToResponse(Booking booking, String paymentStatus, TrustAttendance attendance) {
        return BookingResponse.builder()
                .id(booking.getId())
                .playerId(booking.getUser().getId())
                .playerName(booking.getUser().getName())
                .playerEmail(booking.getUser().getEmail())
                .playerPhone(booking.getUser().getPhone())
                .turfId(booking.getTurf().getId())
                .sport(booking.getSport())
                .bookingDate(booking.getBookingDate())
                .status(booking.getStatus())
                .amount(booking.getAmount())
                .totalAmount(booking.getAmount())
                .slotId(booking.getSlot().getId())
                .slotIds(List.of(booking.getSlot().getId()))
                .slotCount(1)
                .turfName(booking.getTurf().getName())
                .turfCity(booking.getTurf().getCity())
                .turfLocation(booking.getTurf().getLocation())
                .startTime(booking.getSlot().getStartTime())
                .endTime(booking.getSlot().getEndTime())
                .paymentStatus(paymentStatus == null ? inferPaymentStatus(booking.getStatus()) : paymentStatus)
                .attendanceStatus(attendance == null ? null : attendance.getStatus().name())
                .checkedInAt(attendance == null ? null : attendance.getCheckinTime())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private Map<Long, String> paymentStatusesForUser(Long userId) {
        Map<Long, String> statuses = new HashMap<>();
        for (Payment payment : paymentRepository.findByBookingUserIdOrderByCreatedAtDesc(userId)) {
            for (Long bookingId : resolvePaymentBookingIds(payment)) {
                statuses.putIfAbsent(bookingId, normalizePaymentStatus(payment.getStatus()));
            }
        }
        return statuses;
    }

    private List<Long> resolvePaymentBookingIds(Payment payment) {
        if (payment.getBatchBookingIds() == null || payment.getBatchBookingIds().isBlank()) {
            return List.of(payment.getBooking().getId());
        }
        return java.util.Arrays.stream(payment.getBatchBookingIds().split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Long::valueOf)
                .collect(java.util.stream.Collectors.toList());
    }

    private String normalizePaymentStatus(String status) {
        if ("SUCCESS".equalsIgnoreCase(status)) return "PAID";
        if ("REFUNDED".equalsIgnoreCase(status)) return "REFUNDED";
        if ("FAILED".equalsIgnoreCase(status)) return "FAILED";
        return "PENDING";
    }

    private String inferPaymentStatus(BookingStatus status) {
        if (status == BookingStatus.CONFIRMED || status == BookingStatus.CHECKED_IN || status == BookingStatus.COMPLETED || status == BookingStatus.NO_SHOW) return "PAID";
        if (status == BookingStatus.REFUNDED) return "REFUNDED";
        if (status == BookingStatus.CANCELLED) return "FAILED";
        return "PENDING";
    }

    private BigDecimal resolveBookingAmount(Slot slot, java.time.LocalDate date, User user) {
        BigDecimal initialAmount = slotPriceOverrideRepository.findBySlotIdAndDate(slot.getId(), date)
                .map(SlotPriceOverride::getPrice)
                .orElse(slot.getPrice());

        final BigDecimal baseAmount;
        if (aiDynamicPricingService != null) {
            baseAmount = aiDynamicPricingService.calculateDynamicPrice(slot.getTurf().getId(), slot.getId(), initialAmount, date, slot.getStartTime());
        } else {
            baseAmount = initialAmount;
        }

        // Check new Subscription first
        java.util.Optional<Subscription> activeSub = subscriptionRepository.findFirstActiveSubscription(user.getId(), LocalDateTime.now());
        if (activeSub.isPresent() && activeSub.get().getPlan().getDiscountPercent() != null && activeSub.get().getPlan().getDiscountPercent() > 0) {
            BigDecimal discount = BigDecimal.valueOf(activeSub.get().getPlan().getDiscountPercent()).divide(BigDecimal.valueOf(100));
            return baseAmount.subtract(baseAmount.multiply(discount));
        }

        return userMembershipRepository.findFirstByUserIdAndStatusAndEndsAtAfterOrderByEndsAtDesc(user.getId(), MembershipStatus.ACTIVE, LocalDateTime.now())
                .filter(membership -> membership.getPlan().getDiscountPercent() != null && membership.getPlan().getDiscountPercent() > 0)
                .map(membership -> {
                    BigDecimal discount = BigDecimal.valueOf(membership.getPlan().getDiscountPercent()).divide(BigDecimal.valueOf(100));
                    return baseAmount.subtract(baseAmount.multiply(discount));
                })
                .orElse(baseAmount);
    }

    private void validateCreateBookingRequest(CreateBookingRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Booking details are required.");
        }
        validateCreateBookingValues(request.getTurfId(), request.getSlotId(), request.getBookingDate());
    }

    private String requestedSport(String sportCode, String sport) {
        String value = sportCode != null && !sportCode.isBlank() ? sportCode : sport;
        return value == null || value.isBlank() ? "CRICKET" : value;
    }

    private String normalizeSportCode(String sport) {
        return sport == null || sport.isBlank()
                ? "CRICKET"
                : sport.trim().toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private void validateSportForBooking(Long turfId, String sportCode) {
        if (!multiSportService.turfSupportsSport(turfId, sportCode)) {
            throw ApiException.conflict("Selected turf does not support the requested sport.");
        }
    }

    private void validateBatchBookingRequest(CreateBatchBookingRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Booking details are required.");
        }
        if (request.getTurfId() == null) {
            throw ApiException.badRequest("Turf is required.");
        }
        if (request.getBookingDate() == null) {
            throw ApiException.badRequest("Booking date is required.");
        }
        if (request.getBookingDate().isBefore(java.time.LocalDate.now())) {
            throw ApiException.badRequest("Booking date cannot be in the past.");
        }
        if (request.getSlotIds() == null || request.getSlotIds().isEmpty()) {
            throw ApiException.badRequest("Select at least one slot.");
        }

        LinkedHashSet<Long> uniqueSlotIds = new LinkedHashSet<>(request.getSlotIds());
        List<Slot> sortedSlots = slotRepository.findAllById(uniqueSlotIds).stream()
                .sorted(java.util.Comparator.comparing(Slot::getStartTime))
                .collect(java.util.stream.Collectors.toList());
        if (sortedSlots.size() != uniqueSlotIds.size()) {
            throw ApiException.notFound("One or more selected slots were not found.");
        }

        Turf turf = turfRepository.findById(request.getTurfId())
                .orElseThrow(() -> ApiException.notFound("Turf was not found."));
        for (Slot slot : sortedSlots) {
            validateSlotForBooking(turf, slot, request.getBookingDate());
        }
        for (int index = 1; index < sortedSlots.size(); index++) {
            Slot previous = sortedSlots.get(index - 1);
            Slot current = sortedSlots.get(index);
            if (!previous.getEndTime().equals(current.getStartTime())) {
                throw ApiException.badRequest("Selected slots must be consecutive.");
            }
        }
        long durationMinutes = java.time.Duration.between(
                sortedSlots.get(0).getStartTime(),
                sortedSlots.get(sortedSlots.size() - 1).getEndTime()
        ).toMinutes();
        if (durationMinutes > 360) {
            throw ApiException.badRequest("Booking duration cannot exceed 6 hours.");
        }
    }

    private void validateCreateBookingValues(Long turfId, Long slotId, java.time.LocalDate bookingDate) {
        if (turfId == null) {
            throw ApiException.badRequest("Turf is required.");
        }
        if (slotId == null) {
            throw ApiException.badRequest("Slot is required.");
        }
        if (bookingDate == null) {
            throw ApiException.badRequest("Booking date is required.");
        }
        if (bookingDate.isBefore(java.time.LocalDate.now())) {
            throw ApiException.badRequest("Booking date cannot be in the past.");
        }
    }

    private void validateSlotForBooking(Turf turf, Slot slot, java.time.LocalDate bookingDate) {
        if (!slot.getTurf().getId().equals(turf.getId())) {
            throw ApiException.badRequest("Selected slot does not belong to the requested turf.");
        }
        if (!Boolean.TRUE.equals(slot.getIsActive())) {
            throw ApiException.conflict("Selected slot is not active.");
        }
        if (bookingDate.equals(java.time.LocalDate.now()) && !slot.getStartTime().isAfter(java.time.LocalTime.now())) {
            throw ApiException.conflict("Selected slot has already started.");
        }
    }

    private LocalDateTime bookingStartsAt(Booking booking) {
        return LocalDateTime.of(booking.getBookingDate(), booking.getSlot().getStartTime());
    }

    private LocalDateTime bookingEndsAt(Booking booking) {
        return LocalDateTime.of(booking.getBookingDate(), booking.getSlot().getEndTime());
    }

    private java.util.Optional<User> findUser(String loginId) {
        if (loginId != null && loginId.contains("@")) {
            java.util.Optional<User> byEmail = userRepository.findByEmail(loginId);
            if (byEmail.isPresent()) {
                return byEmail;
            }
        }
        return userRepository.findByEmailOrPhone(loginId);
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return findUser(username)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
    }
}
