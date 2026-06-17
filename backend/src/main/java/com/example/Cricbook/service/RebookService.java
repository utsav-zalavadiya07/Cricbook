package com.example.Cricbook.service;

import com.example.Cricbook.dto.BookingResponse;
import com.example.Cricbook.dto.RebookDtos.*;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RebookService {
    private static final List<BookingStatus> ACTIVE_BOOKING_STATUSES = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN);

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final RebookHistoryRepository rebookHistoryRepository;
    private final RecurringBookingRepository recurringBookingRepository;
    private final BookingTemplateRepository bookingTemplateRepository;
    private final BookingPredictionRepository bookingPredictionRepository;
    private final AutoWaitlistRepository autoWaitlistRepository;
    private final BookingConflictRepository bookingConflictRepository;
    private final UserRepository userRepository;
    private final TurfRepository turfRepository;
    private final SlotRepository slotRepository;
    private final NotificationService notificationService;

    @Transactional
    public BookingResponse rebook(String loginId, RebookRequest request) {
        User user = findUser(loginId);
        if (request.getBookingId() == null) {
            throw ApiException.badRequest("Select a previous booking to rebook.");
        }
        Booking source = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> ApiException.notFound("Previous booking was not found."));
        if (!source.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("You can only rebook your own bookings.");
        }

        LocalDate targetDate = Optional.ofNullable(request.getTargetDate())
                .orElseGet(() -> nextOccurrence(source.getBookingDate().getDayOfWeek(), LocalDate.now()));
        if (!targetDate.isAfter(LocalDate.now().minusDays(1))) {
            throw ApiException.badRequest("Choose today or a future date for rebooking.");
        }
        detectConflict(user, source.getSlot(), targetDate);

        BookingResponse response = bookingService.createBookingForUser(user, source.getTurf().getId(), source.getSlot().getId(), targetDate);
        RebookHistory history = rebookHistoryRepository.findByUserIdAndSourceBookingId(user.getId(), source.getId())
                .orElse(RebookHistory.builder().user(user).sourceBooking(source).rebookCount(0).build());
        history.setRebookCount(history.getRebookCount() + 1);
        history.setLastRebookedAt(LocalDateTime.now());
        rebookHistoryRepository.save(history);

        notificationService.pushNotification(
                user.getId(),
                user.getEmail(),
                "Rebook ready",
                "Your repeat booking is reserved. Complete payment to confirm the slot."
        );
        return response;
    }

    @Transactional(readOnly = true)
    public List<FrequentBookingResponse> frequentBookings(String loginId) {
        User user = findUser(loginId);
        return frequentBookingsForUser(user);
    }

    @Transactional(readOnly = true)
    public List<BookingPredictionResponse> predictions(String loginId) {
        User user = findUser(loginId);
        List<BookingPredictionResponse> persisted = bookingPredictionRepository.findTop5ByUserIdOrderByConfidenceDescCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapPrediction)
                .collect(Collectors.toList());
        if (!persisted.isEmpty()) {
            return persisted;
        }
        return frequentBookingsForUser(user).stream()
                .limit(3)
                .map(item -> BookingPredictionResponse.builder()
                        .turfId(item.getTurfId())
                        .turfName(item.getTurfName())
                        .slotId(item.getSlotId())
                        .predictedDate(item.getNextSuggestedDate())
                        .startTime(item.getStartTime())
                        .endTime(item.getEndTime())
                        .confidence(Math.min(0.95, 0.55 + (item.getTimesBooked() * 0.1)))
                        .reason(item.getRecommendationReason())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public RecurringBookingResponse createRecurring(String loginId, RecurringBookingRequest request) {
        User user = findUser(loginId);
        Turf turf = turfRepository.findById(requireId(request.getTurfId(), "Select a turf for recurring booking."))
                .orElseThrow(() -> ApiException.notFound("Turf was not found."));
        Slot slot = slotRepository.findById(requireId(request.getSlotId(), "Select a slot for recurring booking."))
                .orElseThrow(() -> ApiException.notFound("Slot was not found."));
        validateSlotBelongsToTurf(turf, slot);

        DayOfWeek day = Optional.ofNullable(request.getPreferredDay())
                .orElseGet(() -> Optional.ofNullable(request.getNextBookingDate()).orElse(LocalDate.now().plusWeeks(1)).getDayOfWeek());
        LocalDate nextDate = Optional.ofNullable(request.getNextBookingDate())
                .orElseGet(() -> nextOccurrence(day, LocalDate.now()));
        if (bookingRepository.existsByUserIdAndBookingDateAndSlotIdAndStatusIn(user.getId(), nextDate, slot.getId(), ACTIVE_BOOKING_STATUSES)) {
            saveConflict(user, "RECURRING_DUPLICATE", "A booking already exists for this recurring slot.");
            throw ApiException.conflict("You already have a booking for this recurring slot.");
        }

        RecurringBooking recurring = RecurringBooking.builder()
                .user(user)
                .teamId(request.getTeamId())
                .turf(turf)
                .slot(slot)
                .frequency(Optional.ofNullable(request.getFrequency()).orElse(RecurringBookingFrequency.WEEKLY))
                .preferredDay(day)
                .nextBookingDate(nextDate)
                .autoConfirm(Boolean.TRUE.equals(request.getAutoConfirm()))
                .status(RecurringBookingStatus.ACTIVE)
                .build();
        return mapRecurring(recurringBookingRepository.save(recurring));
    }

    @Transactional(readOnly = true)
    public List<RecurringBookingResponse> myRecurring(String loginId) {
        User user = findUser(loginId);
        return recurringBookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapRecurring)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingTemplateResponse createTemplate(String loginId, BookingTemplateRequest request) {
        User user = findUser(loginId);
        if (request.getTemplateName() == null || request.getTemplateName().isBlank()) {
            throw ApiException.badRequest("Template name is required.");
        }
        Turf turf = turfRepository.findById(requireId(request.getTurfId(), "Select a turf for this template."))
                .orElseThrow(() -> ApiException.notFound("Turf was not found."));

        BookingTemplate template = BookingTemplate.builder()
                .user(user)
                .templateName(request.getTemplateName().trim())
                .turf(turf)
                .teamId(request.getTeamId())
                .preferredDay(request.getPreferredDay())
                .preferredTime(request.getPreferredTime())
                .durationMinutes(Optional.ofNullable(request.getDurationMinutes()).orElse(60))
                .players(Optional.ofNullable(request.getPlayers()).orElse(12))
                .status(BookingTemplateStatus.ACTIVE)
                .build();
        return mapTemplate(bookingTemplateRepository.save(template));
    }

    @Transactional(readOnly = true)
    public List<BookingTemplateResponse> myTemplates(String loginId) {
        User user = findUser(loginId);
        return bookingTemplateRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), BookingTemplateStatus.ACTIVE).stream()
                .map(this::mapTemplate)
                .collect(Collectors.toList());
    }

    @Transactional
    public AutoWaitlistResponse createAutoWaitlist(String loginId, AutoWaitlistRequest request) {
        User user = findUser(loginId);
        Slot slot = slotRepository.findById(requireId(request.getSlotId(), "Select a slot for auto waitlist."))
                .orElseThrow(() -> ApiException.notFound("Slot was not found."));
        LocalDate preferredDate = Optional.ofNullable(request.getPreferredDate())
                .orElseThrow(() -> ApiException.badRequest("Preferred waitlist date is required."));
        if (preferredDate.isBefore(LocalDate.now())) {
            throw ApiException.badRequest("Preferred waitlist date cannot be in the past.");
        }

        AutoWaitlist waitlist = AutoWaitlist.builder()
                .user(user)
                .slot(slot)
                .preferredDate(preferredDate)
                .autoBookEnabled(Boolean.TRUE.equals(request.getAutoBookEnabled()))
                .status(AutoWaitlistStatus.ACTIVE)
                .build();
        return mapAutoWaitlist(autoWaitlistRepository.save(waitlist));
    }

    @Transactional(readOnly = true)
    public List<AutoWaitlistResponse> myAutoWaitlists(String loginId) {
        User user = findUser(loginId);
        return autoWaitlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapAutoWaitlist)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BookingIntelligenceResponse myAnalytics(String loginId) {
        User user = findUser(loginId);
        List<FrequentBookingResponse> frequent = frequentBookingsForUser(user);
        List<BookingPredictionResponse> predicted = predictions(loginId);
        long recurringCount = recurringBookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).size();
        long templateCount = bookingTemplateRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), BookingTemplateStatus.ACTIVE).size();
        long waitlistCount = autoWaitlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).size();
        long conflictCount = bookingConflictRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), BookingConflictStatus.OPEN).size();
        double readiness = Math.min(100, (frequent.size() * 25) + (recurringCount * 20) + (templateCount * 10) + (waitlistCount * 10));
        return BookingIntelligenceResponse.builder()
                .frequentBookings(frequent.size())
                .recurringBookings(recurringCount)
                .templates(templateCount)
                .autoWaitlists(waitlistCount)
                .conflicts(conflictCount)
                .rebookReadiness(readiness)
                .topFrequentBookings(frequent.stream().limit(5).collect(Collectors.toList()))
                .predictions(predicted)
                .build();
    }

    @Transactional(readOnly = true)
    public BookingIntelligenceResponse ownerAnalytics(String loginId) {
        User owner = findUser(loginId);
        List<RecurringBookingResponse> recurring = recurringBookingRepository.findByTurfOwnerIdOrderByCreatedAtDesc(owner.getId()).stream()
                .map(this::mapRecurring)
                .collect(Collectors.toList());
        return BookingIntelligenceResponse.builder()
                .frequentBookings(recurring.size())
                .recurringBookings(recurring.size())
                .templates(0)
                .autoWaitlists(0)
                .conflicts(0)
                .rebookReadiness(Math.min(100, recurring.size() * 12.5))
                .topFrequentBookings(List.of())
                .predictions(List.of())
                .build();
    }

    @Transactional(readOnly = true)
    public BookingIntelligenceResponse adminAnalytics() {
        long recurring = recurringBookingRepository.count();
        long templates = bookingTemplateRepository.count();
        long waitlists = autoWaitlistRepository.count();
        long conflicts = bookingConflictRepository.count();
        return BookingIntelligenceResponse.builder()
                .frequentBookings(rebookHistoryRepository.count())
                .recurringBookings(recurring)
                .templates(templates)
                .autoWaitlists(waitlists)
                .conflicts(conflicts)
                .rebookReadiness(Math.min(100, recurring + templates + waitlists))
                .topFrequentBookings(List.of())
                .predictions(List.of())
                .build();
    }

    private List<FrequentBookingResponse> frequentBookingsForUser(User user) {
        Map<String, List<Booking>> grouped = bookingRepository.findByUserIdOrderByBookingDateDescCreatedAtDesc(user.getId()).stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.PENDING)
                .collect(Collectors.groupingBy(booking -> booking.getTurf().getId() + ":" + booking.getSlot().getId()));

        return grouped.values().stream()
                .map(bookings -> bookings.stream().max(Comparator.comparing(Booking::getBookingDate)).orElse(bookings.get(0)))
                .map(booking -> {
                    int count = grouped.get(booking.getTurf().getId() + ":" + booking.getSlot().getId()).size();
                    return FrequentBookingResponse.builder()
                            .sourceBookingId(booking.getId())
                            .turfId(booking.getTurf().getId())
                            .turfName(booking.getTurf().getName())
                            .city(booking.getTurf().getCity())
                            .slotId(booking.getSlot().getId())
                            .startTime(booking.getSlot().getStartTime())
                            .endTime(booking.getSlot().getEndTime())
                            .amount(booking.getAmount())
                            .timesBooked(count)
                            .lastBookedDate(booking.getBookingDate())
                            .nextSuggestedDate(nextOccurrence(booking.getBookingDate().getDayOfWeek(), LocalDate.now()))
                            .recommendationReason(count > 1 ? "Repeated turf and time pattern detected." : "Fast rebook from your latest booking.")
                            .build();
                })
                .sorted(Comparator.comparing(FrequentBookingResponse::getTimesBooked).reversed()
                        .thenComparing(FrequentBookingResponse::getLastBookedDate, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    private void detectConflict(User user, Slot slot, LocalDate date) {
        if (bookingRepository.existsByUserIdAndBookingDateAndSlotIdAndStatusIn(user.getId(), date, slot.getId(), ACTIVE_BOOKING_STATUSES)) {
            saveConflict(user, "DUPLICATE_SLOT", "You already have this slot reserved for the selected date.");
            throw ApiException.conflict("You already have this slot reserved for the selected date.");
        }
    }

    private void saveConflict(User user, String type, String message) {
        bookingConflictRepository.save(BookingConflict.builder()
                .user(user)
                .conflictType(type)
                .message(message)
                .status(BookingConflictStatus.OPEN)
                .build());
    }

    private BookingPredictionResponse mapPrediction(BookingPrediction prediction) {
        Slot slot = prediction.getPredictedSlot();
        Turf turf = prediction.getPredictedTurf();
        return BookingPredictionResponse.builder()
                .turfId(turf != null ? turf.getId() : null)
                .turfName(turf != null ? turf.getName() : null)
                .slotId(slot != null ? slot.getId() : null)
                .predictedDate(prediction.getPredictedDate())
                .startTime(slot != null ? slot.getStartTime() : null)
                .endTime(slot != null ? slot.getEndTime() : null)
                .confidence(prediction.getConfidence())
                .reason(prediction.getReason())
                .build();
    }

    private RecurringBookingResponse mapRecurring(RecurringBooking recurring) {
        return RecurringBookingResponse.builder()
                .id(recurring.getId())
                .turfId(recurring.getTurf().getId())
                .turfName(recurring.getTurf().getName())
                .slotId(recurring.getSlot().getId())
                .startTime(recurring.getSlot().getStartTime())
                .endTime(recurring.getSlot().getEndTime())
                .teamId(recurring.getTeamId())
                .frequency(recurring.getFrequency())
                .preferredDay(recurring.getPreferredDay())
                .nextBookingDate(recurring.getNextBookingDate())
                .autoConfirm(recurring.getAutoConfirm())
                .status(recurring.getStatus())
                .build();
    }

    private BookingTemplateResponse mapTemplate(BookingTemplate template) {
        return BookingTemplateResponse.builder()
                .id(template.getId())
                .templateName(template.getTemplateName())
                .turfId(template.getTurf().getId())
                .turfName(template.getTurf().getName())
                .teamId(template.getTeamId())
                .preferredDay(template.getPreferredDay())
                .preferredTime(template.getPreferredTime())
                .durationMinutes(template.getDurationMinutes())
                .players(template.getPlayers())
                .status(template.getStatus().name())
                .build();
    }

    private AutoWaitlistResponse mapAutoWaitlist(AutoWaitlist waitlist) {
        return AutoWaitlistResponse.builder()
                .id(waitlist.getId())
                .slotId(waitlist.getSlot().getId())
                .turfName(waitlist.getSlot().getTurf().getName())
                .preferredDate(waitlist.getPreferredDate())
                .autoBookEnabled(waitlist.getAutoBookEnabled())
                .status(waitlist.getStatus())
                .build();
    }

    private LocalDate nextOccurrence(DayOfWeek day, LocalDate from) {
        LocalDate next = from.with(java.time.temporal.TemporalAdjusters.nextOrSame(day));
        return next.isAfter(from) ? next : next.plusWeeks(1);
    }

    private void validateSlotBelongsToTurf(Turf turf, Slot slot) {
        if (!slot.getTurf().getId().equals(turf.getId())) {
            throw ApiException.badRequest("Selected slot does not belong to this turf.");
        }
    }

    private Long requireId(Long id, String message) {
        if (id == null) {
            throw ApiException.badRequest(message);
        }
        return id;
    }

    private User findUser(String loginId) {
        String effectiveLogin = loginId != null ? loginId : SecurityContextHolder.getContext().getAuthentication().getName();
        if (effectiveLogin != null && effectiveLogin.contains("@")) {
            Optional<User> byEmail = userRepository.findByEmail(effectiveLogin);
            if (byEmail.isPresent()) {
                return byEmail.get();
            }
        }
        return userRepository.findByEmailOrPhone(effectiveLogin)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
    }
}
