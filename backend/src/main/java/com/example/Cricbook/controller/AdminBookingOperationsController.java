package com.example.Cricbook.controller;

import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.enums.WeatherRefundStatus;
import com.example.Cricbook.repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('BOOKING_VIEW')")
@Transactional
public class AdminBookingOperationsController {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final SlotRepository slotRepository;
    private final SlotAvailabilityRepository slotAvailabilityRepository;
    private final BookingModificationLogRepository modificationLogRepository;
    private final BookingCancellationRequestRepository cancellationRequestRepository;
    private final BookingDisputeRepository bookingDisputeRepository;
    private final SlotConflictRepository slotConflictRepository;
    private final NoShowCaseRepository noShowCaseRepository;
    private final BookingTimelineRepository bookingTimelineRepository;
    private final BookingEscalationCaseRepository escalationCaseRepository;
    private final WeatherRefundRepository weatherRefundRepository;
    private final PlayerTrustScoreRepository playerTrustScoreRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        LocalDate today = LocalDate.now();
        List<Booking> bookings = bookingRepository.findAll();
        long todaysBookings = bookings.stream().filter(booking -> today.equals(booking.getBookingDate())).count();
        long activeMatches = bookings.stream().filter(booking -> today.equals(booking.getBookingDate()) && booking.getStatus() == BookingStatus.CONFIRMED).count();
        long upcomingSlots = bookings.stream().filter(booking -> !booking.getBookingDate().isBefore(today) && booking.getStatus() == BookingStatus.CONFIRMED).count();
        long delayedCheckIns = noShowCaseRepository.findByStatusOrderByCreatedAtDesc("OPEN").size();
        long pendingCancellations = cancellationRequestRepository.findByStatusOrderByCreatedAtDesc("PENDING").size();
        long activeDisputes = bookingDisputeRepository.findByStatusOrderByCreatedAtDesc("OPEN").size();
        long criticalEscalations = escalationCaseRepository.findByStatusOrderByCreatedAtDesc("OPEN").stream()
                .filter(item -> "CRITICAL".equalsIgnoreCase(item.getPriority()) || "LEVEL_3".equalsIgnoreCase(item.getPriority()))
                .count();
        long cancelled = bookings.stream().filter(booking -> booking.getStatus() == BookingStatus.CANCELLED).count();
        long noShows = noShowCaseRepository.count();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("todaysBookings", todaysBookings);
        response.put("activeMatches", activeMatches);
        response.put("upcomingSlots", upcomingSlots);
        response.put("delayedCheckIns", delayedCheckIns);
        response.put("pendingCancellations", pendingCancellations);
        response.put("activeDisputes", activeDisputes);
        response.put("noShowCases", noShows);
        response.put("criticalEscalations", criticalEscalations);
        response.put("cancellationRate", percent(cancelled, bookings.size()));
        response.put("disputeRate", percent(bookingDisputeRepository.count(), bookings.size()));
        response.put("noShowRate", percent(noShows, bookings.size()));
        response.put("liveFeed", bookingTimelineRepository.findTop100ByOrderByCreatedAtDesc().stream().limit(12).map(this::mapTimeline).toList());
        response.put("insight", operationalInsight(activeDisputes, pendingCancellations, criticalEscalations));
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> bookings(@RequestParam(required = false) String status, @RequestParam(required = false) String query) {
        String normalizedStatus = safe(status).trim();
        String normalizedQuery = safe(query).trim().toLowerCase(Locale.ROOT);
        return ResponseEntity.ok(bookingRepository.findAll().stream()
                .filter(booking -> normalizedStatus.isBlank() || booking.getStatus().name().equalsIgnoreCase(normalizedStatus))
                .filter(booking -> normalizedQuery.isBlank()
                        || safe(booking.getUser().getName()).toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || safe(booking.getTurf().getName()).toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || String.valueOf(booking.getId()).equals(normalizedQuery))
                .sorted(Comparator.comparing(Booking::getBookingDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Booking::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapBooking)
                .toList());
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<Map<String, Object>> bookingDetails(@PathVariable Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NoSuchElementException("Booking not found"));
        Map<String, Object> details = new LinkedHashMap<>(mapBooking(booking));
        details.put("payment", paymentRepository.findByBookingId(bookingId).map(this::mapPayment).orElse(null));
        details.put("refunds", paymentRepository.findByBookingId(bookingId).stream().flatMap(payment -> refundRepository.findAll().stream().filter(refund -> refund.getPayment().getId().equals(payment.getId()))).map(this::mapRefund).toList());
        details.put("timeline", bookingTimelineRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream().map(this::mapTimeline).toList());
        details.put("modifications", modificationLogRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream().map(this::mapModification).toList());
        details.put("cancellations", cancellationRequestRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream().map(this::mapCancellation).toList());
        details.put("disputes", bookingDisputeRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream().map(this::mapDispute).toList());
        return ResponseEntity.ok(details);
    }

    @PostMapping("/{bookingId}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('BOOKING_EDIT')")
    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable Long bookingId, @RequestBody StatusRequest request) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NoSuchElementException("Booking not found"));
        BookingStatus oldStatus = booking.getStatus();
        BookingStatus newStatus = BookingStatus.valueOf(safe(request.getStatus()).trim().toUpperCase(Locale.ROOT));
        booking.setStatus(newStatus);
        Booking saved = bookingRepository.save(booking);
        writeModification(saved, "status", oldStatus.name(), newStatus.name(), request.getReason());
        writeTimeline(saved, "STATUS_CHANGED", "Status changed from " + oldStatus + " to " + newStatus + ". Reason: " + safe(request.getReason()));
        return ResponseEntity.ok(mapBooking(saved));
    }

    @PostMapping("/{bookingId}/modify")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('BOOKING_EDIT')")
    public ResponseEntity<Map<String, Object>> modifyBooking(@PathVariable Long bookingId, @RequestBody BookingModifyRequest request) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NoSuchElementException("Booking not found"));
        String reason = safe(request.getReason()).isBlank() ? "Admin booking modification" : request.getReason();
        if (request.getBookingDate() != null && !request.getBookingDate().equals(booking.getBookingDate())) {
            LocalDate oldDate = booking.getBookingDate();
            booking.setBookingDate(request.getBookingDate());
            writeModification(booking, "bookingDate", String.valueOf(oldDate), String.valueOf(request.getBookingDate()), reason);
        }
        if (request.getSlotId() != null && !request.getSlotId().equals(booking.getSlot().getId())) {
            Slot oldSlot = booking.getSlot();
            Slot newSlot = slotRepository.findById(request.getSlotId()).orElseThrow(() -> new NoSuchElementException("Slot not found"));
            booking.setSlot(newSlot);
            booking.setTurf(newSlot.getTurf());
            writeModification(booking, "slot", String.valueOf(oldSlot.getId()), String.valueOf(newSlot.getId()), reason);
        }
        Booking saved = bookingRepository.save(booking);
        writeTimeline(saved, "BOOKING_MODIFIED", reason);
        return ResponseEntity.ok(mapBooking(saved));
    }

    @PostMapping("/cancellations")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('BOOKING_CANCEL')")
    public ResponseEntity<Map<String, Object>> createCancellation(@RequestBody CancellationRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId()).orElseThrow(() -> new NoSuchElementException("Booking not found"));
        BookingCancellationRequest cancellation = cancellationRequestRepository.save(BookingCancellationRequest.builder()
                .booking(booking)
                .requestedBy(safe(request.getRequestedBy()).isBlank() ? actor() : request.getRequestedBy())
                .cancellationType(safe(request.getCancellationType()).isBlank() ? "SYSTEM_CANCELLATION" : request.getCancellationType().trim().toUpperCase(Locale.ROOT))
                .reason(safe(request.getReason()).isBlank() ? "Admin cancellation review" : request.getReason())
                .status("PENDING")
                .build());
        writeTimeline(booking, "CANCELLATION_REQUESTED", cancellation.getReason());
        return ResponseEntity.ok(mapCancellation(cancellation));
    }

    @GetMapping("/cancellations")
    public ResponseEntity<List<Map<String, Object>>> cancellations() {
        return ResponseEntity.ok(cancellationRequestRepository.findAll().stream()
                .sorted(Comparator.comparing(BookingCancellationRequest::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapCancellation)
                .toList());
    }

    @PostMapping("/cancellations/{id}/decision")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('BOOKING_CANCEL')")
    public ResponseEntity<Map<String, Object>> decideCancellation(@PathVariable Long id, @RequestBody DecisionRequest request) {
        BookingCancellationRequest cancellation = cancellationRequestRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Cancellation request not found"));
        String decision = safe(request.getDecision()).isBlank() ? "APPROVED" : request.getDecision().trim().toUpperCase(Locale.ROOT);
        cancellation.setStatus(decision);
        cancellation.setDecisionNotes(safe(request.getReason()).isBlank() ? "Admin decision" : request.getReason());
        if ("APPROVED".equals(decision)) {
            cancellation.getBooking().setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(cancellation.getBooking());
        }
        BookingCancellationRequest saved = cancellationRequestRepository.save(cancellation);
        writeTimeline(saved.getBooking(), "CANCELLATION_" + decision, saved.getDecisionNotes());
        return ResponseEntity.ok(mapCancellation(saved));
    }

    @GetMapping("/disputes")
    public ResponseEntity<List<Map<String, Object>>> disputes() {
        return ResponseEntity.ok(bookingDisputeRepository.findAll().stream()
                .sorted(Comparator.comparing(BookingDispute::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapDispute)
                .toList());
    }

    @PostMapping("/disputes")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('DISPUTE_RESOLVE')")
    public ResponseEntity<Map<String, Object>> createDispute(@RequestBody DisputeRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId()).orElseThrow(() -> new NoSuchElementException("Booking not found"));
        BookingDispute dispute = bookingDisputeRepository.save(BookingDispute.builder()
                .booking(booking)
                .disputeType(safe(request.getDisputeType()).isBlank() ? "OWNER_VS_PLAYER" : request.getDisputeType().trim().toUpperCase(Locale.ROOT))
                .status("OPEN")
                .summary(safe(request.getSummary()).isBlank() ? "Admin opened booking dispute" : request.getSummary())
                .assignedTo(safe(request.getAssignedTo()).isBlank() ? actor() : request.getAssignedTo())
                .build());
        writeTimeline(booking, "DISPUTE_OPENED", dispute.getDisputeType() + ": " + dispute.getSummary());
        return ResponseEntity.ok(mapDispute(dispute));
    }

    @PostMapping("/disputes/{id}/resolve")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('DISPUTE_RESOLVE')")
    public ResponseEntity<Map<String, Object>> resolveDispute(@PathVariable Long id, @RequestBody DecisionRequest request) {
        BookingDispute dispute = bookingDisputeRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Dispute not found"));
        dispute.setStatus("RESOLVED");
        dispute.setResolution(safe(request.getReason()).isBlank() ? "Resolved by admin" : request.getReason());
        BookingDispute saved = bookingDisputeRepository.save(dispute);
        writeTimeline(saved.getBooking(), "DISPUTE_RESOLVED", saved.getResolution());
        return ResponseEntity.ok(mapDispute(saved));
    }

    @GetMapping("/conflicts")
    public ResponseEntity<List<Map<String, Object>>> conflicts() {
        return ResponseEntity.ok(slotConflictRepository.findAll().stream()
                .sorted(Comparator.comparing(SlotConflict::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapSlotConflict)
                .toList());
    }

    @PostMapping("/conflicts/scan")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('BOOKING_EDIT')")
    public ResponseEntity<Map<String, Object>> scanConflicts() {
        Map<String, List<Booking>> grouped = bookingRepository.findAll().stream()
                .filter(booking -> booking.getStatus() == BookingStatus.PENDING || booking.getStatus() == BookingStatus.CONFIRMED)
                .collect(Collectors.groupingBy(booking -> booking.getSlot().getId() + "::" + booking.getBookingDate()));
        List<SlotConflict> created = new ArrayList<>();
        grouped.values().stream().filter(items -> items.size() > 1).forEach(items -> {
            Booking first = items.get(0);
            Booking second = items.get(1);
            created.add(slotConflictRepository.save(SlotConflict.builder()
                    .slotId(first.getSlot().getId())
                    .bookingA(first.getId())
                    .bookingB(second.getId())
                    .status("OPEN")
                    .notes("Duplicate active booking detected for slot/date.")
                    .build()));
        });
        return ResponseEntity.ok(Map.of("created", created.size(), "conflicts", created.stream().map(this::mapSlotConflict).toList()));
    }

    @PostMapping("/conflicts/{id}/resolve")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('BOOKING_EDIT')")
    public ResponseEntity<Map<String, Object>> resolveConflict(@PathVariable Long id, @RequestBody DecisionRequest request) {
        SlotConflict conflict = slotConflictRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Conflict not found"));
        conflict.setStatus("RESOLVED");
        conflict.setNotes(safe(request.getReason()).isBlank() ? "Resolved by booking operations" : request.getReason());
        return ResponseEntity.ok(mapSlotConflict(slotConflictRepository.save(conflict)));
    }

    @GetMapping("/no-shows")
    public ResponseEntity<List<Map<String, Object>>> noShows() {
        return ResponseEntity.ok(noShowCaseRepository.findAll().stream().map(this::mapNoShow).toList());
    }

    @PostMapping("/no-shows")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('BOOKING_EDIT')")
    public ResponseEntity<Map<String, Object>> createNoShow(@RequestBody NoShowRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId()).orElseThrow(() -> new NoSuchElementException("Booking not found"));
        User user = request.getUserId() == null ? booking.getUser() : booking.getUser().getId().equals(request.getUserId()) ? booking.getUser() : booking.getTurf().getOwner();
        NoShowCase noShow = noShowCaseRepository.save(NoShowCase.builder()
                .booking(booking)
                .user(user)
                .noShowType(safe(request.getNoShowType()).isBlank() ? "PLAYER_NO_SHOW" : request.getNoShowType().trim().toUpperCase(Locale.ROOT))
                .status("OPEN")
                .penalty(request.getPenalty() == null ? BigDecimal.ZERO : request.getPenalty())
                .notes(safe(request.getNotes()).isBlank() ? "Admin no-show case" : request.getNotes())
                .build());
        adjustTrustForNoShow(user);
        writeTimeline(booking, "NO_SHOW_RECORDED", noShow.getNoShowType() + " penalty=" + noShow.getPenalty());
        return ResponseEntity.ok(mapNoShow(noShow));
    }

    @PostMapping("/no-shows/{id}/resolve")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('BOOKING_EDIT')")
    public ResponseEntity<Map<String, Object>> resolveNoShow(@PathVariable Long id, @RequestBody DecisionRequest request) {
        NoShowCase noShow = noShowCaseRepository.findById(id).orElseThrow(() -> new NoSuchElementException("No-show case not found"));
        noShow.setStatus("RESOLVED");
        noShow.setNotes(safe(request.getReason()).isBlank() ? noShow.getNotes() : request.getReason());
        NoShowCase saved = noShowCaseRepository.save(noShow);
        writeTimeline(saved.getBooking(), "NO_SHOW_RESOLVED", saved.getNotes());
        return ResponseEntity.ok(mapNoShow(saved));
    }

    @GetMapping("/weather-cases")
    public ResponseEntity<List<Map<String, Object>>> weatherCases() {
        return ResponseEntity.ok(weatherRefundRepository.findAll().stream()
                .sorted(Comparator.comparing(WeatherRefund::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapWeatherRefund)
                .toList());
    }

    @PostMapping("/weather-cases/{id}/decision")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('BOOKING_CANCEL') or hasAuthority('REFUND_OVERRIDE')")
    public ResponseEntity<Map<String, Object>> decideWeatherCase(@PathVariable Long id, @RequestBody DecisionRequest request) {
        WeatherRefund refund = weatherRefundRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Weather case not found"));
        WeatherRefundStatus status = WeatherRefundStatus.valueOf((safe(request.getDecision()).isBlank() ? "APPROVED" : request.getDecision()).trim().toUpperCase(Locale.ROOT));
        refund.setStatus(status);
        refund.setReason(safe(request.getReason()).isBlank() ? refund.getReason() : request.getReason());
        WeatherRefund saved = weatherRefundRepository.save(refund);
        writeTimeline(saved.getBooking(), "WEATHER_CASE_" + status, saved.getReason());
        return ResponseEntity.ok(mapWeatherRefund(saved));
    }

    @GetMapping("/timeline")
    public ResponseEntity<List<Map<String, Object>>> timeline() {
        return ResponseEntity.ok(bookingTimelineRepository.findTop100ByOrderByCreatedAtDesc().stream().map(this::mapTimeline).toList());
    }

    @GetMapping("/{bookingId}/timeline")
    public ResponseEntity<List<Map<String, Object>>> bookingTimeline(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingTimelineRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream().map(this::mapTimeline).toList());
    }

    @GetMapping("/escalations")
    public ResponseEntity<List<Map<String, Object>>> escalations() {
        return ResponseEntity.ok(escalationCaseRepository.findAll().stream().map(this::mapEscalation).toList());
    }

    @PostMapping("/escalations")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ESCALATION_MANAGE')")
    public ResponseEntity<Map<String, Object>> createEscalation(@RequestBody EscalationRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId()).orElseThrow(() -> new NoSuchElementException("Booking not found"));
        BookingEscalationCase escalation = escalationCaseRepository.save(BookingEscalationCase.builder()
                .booking(booking)
                .ticketId(request.getTicketId())
                .priority(safe(request.getPriority()).isBlank() ? "LEVEL_1" : request.getPriority().trim().toUpperCase(Locale.ROOT))
                .assignedTo(safe(request.getAssignedTo()).isBlank() ? actor() : request.getAssignedTo())
                .status("OPEN")
                .summary(safe(request.getSummary()).isBlank() ? "Booking escalation" : request.getSummary())
                .build());
        writeTimeline(booking, "ESCALATION_OPENED", escalation.getPriority() + ": " + escalation.getSummary());
        return ResponseEntity.ok(mapEscalation(escalation));
    }

    @PostMapping("/escalations/{id}/resolve")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ESCALATION_MANAGE')")
    public ResponseEntity<Map<String, Object>> resolveEscalation(@PathVariable Long id, @RequestBody DecisionRequest request) {
        BookingEscalationCase escalation = escalationCaseRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Escalation not found"));
        escalation.setStatus("RESOLVED");
        escalation.setSummary(escalation.getSummary() + " | Resolution: " + safe(request.getReason()));
        BookingEscalationCase saved = escalationCaseRepository.save(escalation);
        writeTimeline(saved.getBooking(), "ESCALATION_RESOLVED", safe(request.getReason()));
        return ResponseEntity.ok(mapEscalation(saved));
    }

    private Map<String, Object> mapBooking(Booking booking) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", booking.getId());
        item.put("playerId", booking.getUser().getId());
        item.put("player", booking.getUser().getName());
        item.put("playerEmail", booking.getUser().getEmail());
        item.put("ownerId", booking.getTurf().getOwner().getId());
        item.put("owner", booking.getTurf().getOwner().getName());
        item.put("turfId", booking.getTurf().getId());
        item.put("turf", booking.getTurf().getName());
        item.put("slotId", booking.getSlot().getId());
        item.put("slot", booking.getSlot().getStartTime() + " - " + booking.getSlot().getEndTime());
        item.put("bookingDate", booking.getBookingDate());
        item.put("paymentStatus", paymentRepository.findByBookingId(booking.getId()).map(Payment::getStatus).orElse("PENDING"));
        item.put("amount", booking.getAmount());
        item.put("status", booking.getStatus());
        item.put("createdAt", booking.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapPayment(Payment payment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", payment.getId());
        map.put("amount", payment.getAmount());
        map.put("status", payment.getStatus());
        map.put("createdAt", payment.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapRefund(Refund refund) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", refund.getId());
        map.put("amount", refund.getAmount());
        map.put("status", refund.getStatus());
        map.put("reason", refund.getReason());
        map.put("createdAt", refund.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapCancellation(BookingCancellationRequest item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("bookingId", item.getBooking().getId());
        map.put("player", item.getBooking().getUser().getName());
        map.put("turf", item.getBooking().getTurf().getName());
        map.put("requestedBy", item.getRequestedBy());
        map.put("cancellationType", item.getCancellationType());
        map.put("reason", item.getReason());
        map.put("status", item.getStatus());
        map.put("decisionNotes", item.getDecisionNotes());
        map.put("createdAt", item.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapDispute(BookingDispute item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("bookingId", item.getBooking().getId());
        map.put("disputeType", item.getDisputeType());
        map.put("status", item.getStatus());
        map.put("summary", item.getSummary());
        map.put("resolution", item.getResolution());
        map.put("assignedTo", item.getAssignedTo());
        map.put("createdAt", item.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapSlotConflict(SlotConflict item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("slotId", item.getSlotId());
        map.put("bookingA", item.getBookingA());
        map.put("bookingB", item.getBookingB());
        map.put("status", item.getStatus());
        map.put("notes", item.getNotes());
        map.put("createdAt", item.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapNoShow(NoShowCase item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("bookingId", item.getBooking().getId());
        map.put("userId", item.getUser().getId());
        map.put("userName", item.getUser().getName());
        map.put("noShowType", item.getNoShowType());
        map.put("status", item.getStatus());
        map.put("penalty", item.getPenalty());
        map.put("notes", item.getNotes());
        map.put("createdAt", item.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapWeatherRefund(WeatherRefund item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("bookingId", item.getBooking().getId());
        map.put("refundType", item.getRefundType());
        map.put("amount", item.getAmount());
        map.put("status", item.getStatus());
        map.put("reason", item.getReason());
        map.put("createdAt", item.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapTimeline(BookingTimeline item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("bookingId", item.getBooking().getId());
        map.put("eventType", item.getEventType());
        map.put("performedBy", item.getPerformedBy());
        map.put("details", item.getDetails());
        map.put("createdAt", item.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapModification(BookingModificationLog item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("fieldName", item.getFieldName());
        map.put("oldValue", item.getOldValue());
        map.put("newValue", item.getNewValue());
        map.put("modifiedBy", item.getModifiedBy());
        map.put("reason", item.getReason());
        map.put("createdAt", item.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapEscalation(BookingEscalationCase item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("bookingId", item.getBooking().getId());
        map.put("ticketId", item.getTicketId());
        map.put("priority", item.getPriority());
        map.put("assignedTo", item.getAssignedTo());
        map.put("status", item.getStatus());
        map.put("summary", item.getSummary());
        map.put("createdAt", item.getCreatedAt());
        return map;
    }

    private void writeModification(Booking booking, String field, String oldValue, String newValue, String reason) {
        modificationLogRepository.save(BookingModificationLog.builder()
                .booking(booking)
                .fieldName(field)
                .oldValue(oldValue)
                .newValue(newValue)
                .modifiedBy(actor())
                .reason(safe(reason).isBlank() ? "Admin intervention" : reason)
                .build());
    }

    private void writeTimeline(Booking booking, String eventType, String details) {
        bookingTimelineRepository.save(BookingTimeline.builder()
                .booking(booking)
                .eventType(eventType)
                .performedBy(actor())
                .details(safe(details).isBlank() ? eventType : details)
                .build());
    }

    private void adjustTrustForNoShow(User user) {
        playerTrustScoreRepository.findByUserId(user.getId()).ifPresent(score -> {
            int current = score.getScore() == null ? 100 : score.getScore();
            score.setScore(Math.max(0, current - 5));
            playerTrustScoreRepository.save(score);
        });
    }

    private double percent(long numerator, long denominator) {
        if (denominator <= 0) return 0;
        return Math.round((numerator * 1000.0 / denominator)) / 10.0;
    }

    private String operationalInsight(long disputes, long cancellations, long escalations) {
        if (escalations > 0) return "Critical escalations require immediate senior admin review.";
        if (disputes > cancellations) return "Dispute load is higher than cancellation queue; prioritize evidence review.";
        if (cancellations > 0) return "Cancellation queue is active; resolve before slot time to reduce refund disputes.";
        return "Booking operations are currently stable.";
    }

    private String actor() {
        return SecurityContextHolder.getContext().getAuthentication() == null ? "system_admin" : SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @Data
    public static class StatusRequest {
        private String status;
        private String reason;
    }

    @Data
    public static class BookingModifyRequest {
        private LocalDate bookingDate;
        private Long slotId;
        private String reason;
    }

    @Data
    public static class CancellationRequest {
        private Long bookingId;
        private String requestedBy;
        private String cancellationType;
        private String reason;
    }

    @Data
    public static class DecisionRequest {
        private String decision;
        private String reason;
    }

    @Data
    public static class DisputeRequest {
        private Long bookingId;
        private String disputeType;
        private String summary;
        private String assignedTo;
    }

    @Data
    public static class NoShowRequest {
        private Long bookingId;
        private Long userId;
        private String noShowType;
        private BigDecimal penalty;
        private String notes;
    }

    @Data
    public static class EscalationRequest {
        private Long bookingId;
        private Long ticketId;
        private String priority;
        private String assignedTo;
        private String summary;
    }
}
