package com.example.Cricbook.service;

import com.example.Cricbook.dto.BlockSlotRequest;
import com.example.Cricbook.dto.CreateSlotRequest;
import com.example.Cricbook.dto.SlotPriceOverrideRequest;
import com.example.Cricbook.dto.SlotAvailabilityResponse;
import com.example.Cricbook.dto.SlotCalendarDateResponse;
import com.example.Cricbook.dto.SlotResponse;
import com.example.Cricbook.dto.UpdateSlotRequest;
import com.example.Cricbook.entity.Slot;
import com.example.Cricbook.entity.SlotAvailability;
import com.example.Cricbook.entity.SlotPriceOverride;
import com.example.Cricbook.entity.Turf;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.entity.Booking;
import com.example.Cricbook.enums.RoleName;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.mapper.TurfMapper;
import com.example.Cricbook.repository.SlotAvailabilityRepository;
import com.example.Cricbook.repository.SlotPriceOverrideRepository;
import com.example.Cricbook.repository.SlotRepository;
import com.example.Cricbook.repository.TurfRepository;
import com.example.Cricbook.repository.UserRepository;
import com.example.Cricbook.repository.BookingRepository;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.enums.TurfStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlotService {
    private static final int AVAILABILITY_WINDOW_DAYS = 90;
    private static final DateTimeFormatter SLOT_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_SLOT_DURATION_MINUTES = 720;
    private static final int MINUTES_PER_DAY = 24 * 60;
    private static final BigDecimal MAX_SLOT_PRICE = BigDecimal.valueOf(100000);
    private static final List<BookingStatus> ACTIVE_BOOKING_STATUSES = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN);
    private static final List<BookingStatus> AVAILABILITY_BOOKING_STATUSES = List.of(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED,
            BookingStatus.CHECKED_IN,
            BookingStatus.COMPLETED,
            BookingStatus.NO_SHOW
    );

    private final SlotRepository slotRepository;
    private final SlotAvailabilityRepository slotAvailabilityRepository;
    private final SlotPriceOverrideRepository slotPriceOverrideRepository;
    private final TurfRepository turfRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final TurfMapper turfMapper;
    private final CacheService cacheService;
    @org.springframework.context.annotation.Lazy
    private final com.example.Cricbook.service.ai.AIDynamicPricingService aiDynamicPricingService;

    @Transactional
    public List<SlotResponse> createSlots(Long turfId, String userEmail, CreateSlotRequest request) {
        validateCreateSlotRequest(request);
        Turf turf = getTurfEntity(turfId);
        validateOwnershipOrAdmin(turf, userEmail);

        boolean fullDay = Boolean.TRUE.equals(request.getFullDay());
        LocalTime startTime = fullDay ? LocalTime.MIDNIGHT : parseSlotTime(request.getStartTime(), "Start time");
        LocalTime endTime = fullDay ? LocalTime.MIDNIGHT : parseSlotTime(request.getEndTime(), "End time");
        int startMinute = toMinuteOfDay(startTime);
        int endMinute = fullDay ? MINUTES_PER_DAY : toEndMinute(startTime, endTime);

        if (endMinute <= startMinute) {
            throw ApiException.badRequest("Start time must be before end time.");
        }
        if ((endMinute - startMinute) % request.getSlotDuration() != 0) {
            throw ApiException.badRequest("Selected time range must be divisible by slot duration.");
        }

        boolean replaceExisting = Boolean.TRUE.equals(request.getReplaceExisting());
        if (replaceExisting) {
            archiveExistingSlotConfiguration(turfId);
        }

        List<Slot> existingRegularSlots = replaceExisting
                ? List.of()
                : unarchivedRegularSlots(turfId);
        List<Slot> existingActiveSlots = existingRegularSlots.stream()
                .filter(slot -> Boolean.TRUE.equals(slot.getIsActive()))
                .collect(Collectors.toList());
        List<Slot> createdSlots = new ArrayList<>();
        List<Slot> reactivatedSlots = new ArrayList<>();
        List<Slot> requestedSlots = new ArrayList<>();
        int currentMinute = startMinute;

        while (currentMinute < endMinute) {
            int nextMinute = currentMinute + request.getSlotDuration();
            LocalTime currentTime = fromMinuteOfDay(currentMinute);
            LocalTime nextTime = fromMinuteOfDay(nextMinute);

            Slot exactExistingSlot = findExactSlot(existingRegularSlots, currentTime, nextTime);
            if (exactExistingSlot != null) {
                if (!Boolean.TRUE.equals(exactExistingSlot.getIsActive())) {
                    if (hasActiveSlotOverlap(existingActiveSlots, currentMinute, nextMinute)) {
                        throw scheduleOverlapConflict(currentTime, nextTime);
                    }
                    exactExistingSlot.setIsActive(true);
                    reactivatedSlots.add(exactExistingSlot);
                    existingActiveSlots.add(exactExistingSlot);
                }
                requestedSlots.add(exactExistingSlot);
                currentMinute = nextMinute;
                continue;
            }
            if (hasActiveSlotOverlap(existingActiveSlots, currentMinute, nextMinute)) {
                throw scheduleOverlapConflict(currentTime, nextTime);
            }

            Slot slot = Slot.builder()
                    .turf(turf)
                    .startTime(currentTime)
                    .endTime(nextTime)
                    .price(request.getPrice() != null ? request.getPrice() : turf.getPricePerHour())
                    .isActive(true)
                    .archived(false)
                    .build();
            
            createdSlots.add(slot);
            existingActiveSlots.add(slot);
            requestedSlots.add(slot);
            currentMinute = nextMinute;
        }
        if (requestedSlots.isEmpty()) {
            throw ApiException.badRequest("No slots can be created from the selected time range and duration.");
        }

        List<Slot> changedSlots = new ArrayList<>(reactivatedSlots);
        changedSlots.addAll(createdSlots);
        if (!changedSlots.isEmpty()) {
            slotRepository.saveAll(changedSlots);
        }
        
        // Prime the rolling booking window. Later availability requests automatically
        // fill any missing dates from this regular slot configuration.
        generateAvailability(requestedSlots);
        invalidateSearchPricing();

        return requestedSlots.stream()
                .map(turfMapper::toSlotResponse)
                .collect(Collectors.toList());
    }

    private void archiveExistingSlotConfiguration(Long turfId) {
        if (bookingRepository != null && bookingRepository.existsByTurfIdAndBookingDateGreaterThanEqualAndStatusIn(
                turfId,
                LocalDate.now(),
                ACTIVE_BOOKING_STATUSES
        )) {
            throw ApiException.conflict("Cannot replace slot configuration while this turf has active future bookings. Cancel or reschedule those bookings first.");
        }

        List<Slot> existingSlots = slotRepository.findByTurfIdAndArchivedFalse(turfId);
        if (existingSlots.isEmpty()) {
            return;
        }

        existingSlots.forEach(slot -> {
            slot.setIsActive(false);
            slot.setArchived(true);
        });
        slotRepository.saveAll(existingSlots);
    }

    @Transactional
    public void generateAvailability(List<Slot> slots) {
        LocalDate today = LocalDate.now();
        generateMissingAvailability(slots, today, today.plusDays(AVAILABILITY_WINDOW_DAYS - 1));
    }

    @Transactional
    public void blockSlots(Long turfId, String userEmail, BlockSlotRequest request) {
        updateSlotBlocks(turfId, userEmail, request, request.getBlocked() == null || request.getBlocked());
    }

    @Transactional
    public void unblockSlots(Long turfId, String userEmail, BlockSlotRequest request) {
        updateSlotBlocks(turfId, userEmail, request, false);
    }

    private void updateSlotBlocks(Long turfId, String userEmail, BlockSlotRequest request, boolean blocked) {
        validateBlockRequest(request);
        Turf turf = getTurfEntity(turfId);
        validateOwnershipOrAdmin(turf, userEmail);
        ensureAvailability(turfId, request.getDate(), request.getDate());

        List<SlotAvailability> availabilities = new ArrayList<>();
        for (Long slotId : uniqueSlotIds(request.getSlotIds())) {
            SlotAvailability availability = slotAvailabilityRepository.findBySlotIdAndDate(slotId, request.getDate())
                    .orElseThrow(() -> ApiException.notFound("Slot availability was not found for the selected date."));
            
            // Validate slot belongs to turf
            if (!availability.getSlot().getTurf().getId().equals(turfId)) {
                throw ApiException.badRequest("Selected slot does not belong to this turf.");
            }
            if (blocked && hasActiveBookingOnDate(slotId, request.getDate())) {
                throw ApiException.conflict("Cannot block a slot that already has an active booking.");
            }

            availability.setIsBlocked(blocked);
            if (blocked) {
                availability.setIsAvailable(false);
            } else {
                boolean booked = bookingRepository.findBookedSlotIdsByTurfAndDate(
                        turfId,
                        request.getDate(),
                        ACTIVE_BOOKING_STATUSES
                ).contains(slotId);
                availability.setIsAvailable(!booked);
            }
            availabilities.add(availability);
        }

        slotAvailabilityRepository.saveAll(availabilities);
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> getSlots(Long turfId, String userEmail) {
        Turf turf = getTurfEntity(turfId);
        validateOwnershipOrAdmin(turf, userEmail);
        return slotRepository.findByTurfIdAndArchivedFalse(turfId).stream()
                .map(turfMapper::toSlotResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SlotResponse updateSlot(Long turfId, Long slotId, String userEmail, UpdateSlotRequest request) {
        Turf turf = getTurfEntity(turfId);
        validateOwnershipOrAdmin(turf, userEmail);

        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> ApiException.notFound("Slot was not found."));

        if (!slot.getTurf().getId().equals(turfId)) {
            throw ApiException.badRequest("Selected slot does not belong to this turf.");
        }

        LocalTime nextStartTime = slot.getStartTime();
        LocalTime nextEndTime = slot.getEndTime();
        BigDecimal nextPrice = slot.getPrice();
        Boolean nextActive = slot.getIsActive();

        if (request.getStartTime() != null) {
            nextStartTime = parseSlotTime(request.getStartTime(), "Start time");
        }
        if (request.getEndTime() != null) {
            nextEndTime = parseSlotTime(request.getEndTime(), "End time");
        }
        if (request.getPrice() != null) {
            validatePrice(request.getPrice(), "Slot price");
            nextPrice = request.getPrice();
        }
        if (request.getIsActive() != null) {
            nextActive = request.getIsActive();
        }

        int nextStartMinute = toMinuteOfDay(nextStartTime);
        int nextEndMinute = toEndMinute(nextStartTime, nextEndTime);
        if (nextEndMinute <= nextStartMinute) {
            throw ApiException.badRequest("Start time must be before end time.");
        }
        boolean slotWillBeActive = !Boolean.FALSE.equals(nextActive);
        List<Slot> existingSlots = activeRegularSlots(turfId).stream()
                .filter(existing -> !existing.getId().equals(slotId))
                .collect(Collectors.toList());
        if (slotWillBeActive && hasActiveSlotOverlap(existingSlots, nextStartMinute, nextEndMinute)) {
            throw ApiException.conflict("This slot overlaps another active slot. Disable the conflicting slot first or adjust the start/end time.");
        }
        boolean scheduleChanged = request.getStartTime() != null || request.getEndTime() != null || Boolean.FALSE.equals(request.getIsActive());
        if (scheduleChanged && hasActiveFutureBookings(slotId)) {
            throw ApiException.conflict("This slot has active bookings. Cancel or reschedule those bookings before changing timing or disabling it.");
        }

        slot.setStartTime(nextStartTime);
        slot.setEndTime(nextEndTime);
        slot.setPrice(nextPrice);
        slot.setIsActive(nextActive);

        SlotResponse response = turfMapper.toSlotResponse(slotRepository.save(slot));
        invalidateSearchPricing();
        return response;
    }

    @Transactional
    public SlotResponse disableSlot(Long turfId, Long slotId, String userEmail) {
        UpdateSlotRequest request = new UpdateSlotRequest();
        request.setIsActive(false);
        return updateSlot(turfId, slotId, userEmail, request);
    }

    @Transactional
    public void applyPriceOverride(Long turfId, String userEmail, SlotPriceOverrideRequest request) {
        validatePriceOverrideRequest(request);
        Turf turf = getTurfEntity(turfId);
        validateOwnershipOrAdmin(turf, userEmail);

        List<SlotPriceOverride> overrides = new ArrayList<>();
        for (Long slotId : uniqueSlotIds(request.getSlotIds())) {
            Slot slot = slotRepository.findById(slotId)
                    .orElseThrow(() -> ApiException.notFound("Slot was not found."));

            if (!slot.getTurf().getId().equals(turfId)) {
                throw ApiException.badRequest("Selected slot does not belong to this turf.");
            }

            SlotPriceOverride override = slotPriceOverrideRepository.findBySlotIdAndDate(slotId, request.getDate())
                    .orElse(SlotPriceOverride.builder().slot(slot).date(request.getDate()).build());
            override.setPrice(request.getPrice());
            override.setReason(request.getReason());
            overrides.add(override);
        }

        slotPriceOverrideRepository.saveAll(overrides);
        invalidateSearchPricing();
    }

    @Transactional
    public List<SlotAvailabilityResponse> getAvailability(Long turfId, LocalDate date) {
        if (date == null) {
            throw ApiException.badRequest("Availability date is required.");
        }
        Turf turf = getTurfEntity(turfId);
        ensureAvailability(turfId, date, date);
        List<SlotAvailability> availabilities = slotAvailabilityRepository.findBySlotTurfIdAndDate(turfId, date);
        java.util.Map<Long, BookingStatus> bookingStatuses = bookingStatusesBySlot(turfId, date);
        java.util.Map<Long, java.math.BigDecimal> priceOverrides = slotPriceOverrideRepository == null
                ? java.util.Map.of()
                : slotPriceOverrideRepository.findBySlotTurfIdAndDate(turfId, date)
                .stream()
                .collect(Collectors.toMap(override -> override.getSlot().getId(), SlotPriceOverride::getPrice));

        List<SlotAvailabilityResponse> response = availabilities.stream()
                .filter(this::isVisibleSlotAvailability)
                .map(slotAvailability -> {
                    SlotAvailabilityResponse item = turfMapper.toSlotAvailabilityResponse(slotAvailability);
                    item.setListingPrice(turf.getPricePerHour());
                    if (item.getSlotId() != null && priceOverrides.containsKey(item.getSlotId())) {
                        item.setPrice(priceOverrides.get(item.getSlotId()));
                    }
                    BookingStatus bookingStatus = item.getSlotId() == null ? null : bookingStatuses.get(item.getSlotId());
                    boolean pastSlot = isPastSlot(date, item.getStartTime());
                    if (isTurfInMaintenance(turf)) {
                        item.setStatus("MAINTENANCE");
                        item.setIsAvailable(false);
                        item.setAvailable(false);
                    } else if (isTurfClosed(turf)) {
                        item.setStatus("CLOSED");
                        item.setIsAvailable(false);
                        item.setAvailable(false);
                    } else if (Boolean.TRUE.equals(item.getBlocked())) {
                        item.setStatus("BLOCKED");
                        item.setIsAvailable(false);
                        item.setAvailable(false);
                    } else if (bookingStatus == BookingStatus.PENDING) {
                        item.setStatus("LOCKED");
                        item.setLocked(true);
                        item.setIsAvailable(false);
                        item.setAvailable(false);
                    } else if (isBookedStatus(bookingStatus)) {
                        item.setStatus("BOOKED");
                        item.setIsAvailable(false);
                        item.setAvailable(false);
                    } else if (pastSlot) {
                        item.setStatus("EXPIRED");
                        item.setIsAvailable(false);
                        item.setAvailable(false);
                    } else if (bookingStatus == null) {
                        item.setStatus("AVAILABLE");
                        item.setIsAvailable(true);
                        item.setAvailable(true);
                    } else {
                        item.setStatus("UNAVAILABLE");
                    }
                    item.setLocked(Boolean.TRUE.equals(item.getLocked()));
                    // Calculate dynamic price and high demand flags
                    if (aiDynamicPricingService != null && item.getSlotId() != null && item.getPrice() != null) {
                        java.math.BigDecimal dynamicPrice = aiDynamicPricingService.calculateDynamicPrice(
                                turfId, 
                                item.getSlotId(), 
                                item.getPrice(), 
                                date, 
                                item.getStartTime()
                        );
                        item.setPrice(dynamicPrice);

                        boolean isHighDemand = aiDynamicPricingService.isSlotHighDemand(
                                turfId, 
                                item.getSlotId(), 
                                date, 
                                item.getStartTime()
                        );
                        item.setDemandTag(isHighDemand ? "High Demand" : "Normal");
                    }
                    return item;
                })
                .collect(Collectors.toList());

        return response;
    }

    private void invalidateSearchPricing() {
        cacheService.evictByPrefix("search:");
    }

    @Transactional
    public List<SlotCalendarDateResponse> getCalendar(Long turfId, LocalDate startDate, LocalDate endDate) {
        Turf turf = getTurfEntity(turfId);
        LocalDate start = startDate == null ? LocalDate.now() : startDate;
        LocalDate end = endDate == null ? start.plusDays(30) : endDate;
        if (end.isBefore(start)) {
            throw ApiException.badRequest("Calendar end date must be on or after start date.");
        }
        if (java.time.temporal.ChronoUnit.DAYS.between(start, end) > 90) {
            throw ApiException.badRequest("Calendar range cannot exceed 90 days.");
        }

        ensureAvailability(turfId, start, end);

        List<SlotAvailability> rows = slotAvailabilityRepository.findBySlotTurfIdAndDateBetween(turfId, start, end).stream()
                .filter(this::isVisibleSlotAvailability)
                .collect(Collectors.toList());
        java.util.Map<LocalDate, List<SlotAvailability>> byDate = rows.stream()
                .collect(Collectors.groupingBy(SlotAvailability::getDate));

        List<SlotCalendarDateResponse> response = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            List<SlotAvailability> dayRows = byDate.getOrDefault(current, List.of());
            java.util.Map<Long, BookingStatus> bookingStatuses = bookingStatusesBySlot(turfId, current);
            LocalDate date = current;
            long bookedSlots = dayRows.stream()
                    .map(row -> bookingStatuses.get(row.getSlot().getId()))
                    .filter(this::isBookedStatus)
                    .count();
            long blockedSlots = dayRows.stream().filter(row -> Boolean.TRUE.equals(row.getIsBlocked())).count();
            long availableSlots = dayRows.stream()
                    .filter(row -> !Boolean.TRUE.equals(row.getIsBlocked()))
                    .filter(row -> !bookingStatuses.containsKey(row.getSlot().getId()))
                    .filter(row -> !isPastSlot(date, row.getSlot().getStartTime()))
                    .count();
            if (isTurfClosed(turf) || isTurfInMaintenance(turf)) {
                availableSlots = 0;
            }
            response.add(SlotCalendarDateResponse.builder()
                    .date(current)
                    .totalSlots(dayRows.size())
                    .availableSlots(availableSlots)
                    .bookedSlots(bookedSlots)
                    .blockedSlots(blockedSlots)
                    .status(calendarStatus(turf, availableSlots, dayRows.isEmpty()))
                    .build());
            current = current.plusDays(1);
        }
        return response;
    }

    @Transactional(readOnly = true)
    public java.math.BigDecimal resolvePrice(Long slotId, LocalDate date) {
        java.math.BigDecimal basePrice = slotPriceOverrideRepository.findBySlotIdAndDate(slotId, date)
                .map(SlotPriceOverride::getPrice)
                .orElseGet(() -> slotRepository.findById(slotId)
                        .orElseThrow(() -> new ApiException("Slot not found"))
                        .getPrice());

        if (aiDynamicPricingService != null) {
            Slot slot = slotRepository.findById(slotId)
                    .orElseThrow(() -> new ApiException("Slot not found"));
            return aiDynamicPricingService.calculateDynamicPrice(slot.getTurf().getId(), slotId, basePrice, date, slot.getStartTime());
        }
        return basePrice;
    }

    private Turf getTurfEntity(Long turfId) {
        return turfRepository.findById(turfId)
                .orElseThrow(() -> new ApiException("Turf not found"));
    }

    private void ensureAvailability(Long turfId, LocalDate startDate, LocalDate endDate) {
        generateMissingAvailability(activeRegularSlots(turfId), startDate, endDate);
    }

    private void generateMissingAvailability(List<Slot> slots, LocalDate startDate, LocalDate endDate) {
        if (slots == null || slots.isEmpty() || startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return;
        }

        List<SlotAvailability> existingRows = slotAvailabilityRepository.findBySlotTurfIdAndDateBetween(
                slots.get(0).getTurf().getId(),
                startDate,
                endDate
        );
        java.util.Set<String> existingKeys = existingRows.stream()
                .map(row -> row.getSlot().getId() + ":" + row.getDate())
                .collect(Collectors.toSet());
        List<SlotAvailability> missingRows = new ArrayList<>();

        for (Slot slot : slots) {
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                String key = slot.getId() + ":" + current;
                if (!existingKeys.contains(key)) {
                    missingRows.add(SlotAvailability.builder()
                            .slot(slot)
                            .date(current)
                            .isAvailable(true)
                            .isBlocked(false)
                            .build());
                }
                current = current.plusDays(1);
            }
        }

        if (!missingRows.isEmpty()) {
            slotAvailabilityRepository.saveAll(missingRows);
        }
    }

    private java.util.Map<Long, BookingStatus> bookingStatusesBySlot(Long turfId, LocalDate date) {
        if (bookingRepository == null) {
            return java.util.Map.of();
        }
        return bookingRepository.findByTurfIdAndBookingDateAndStatusIn(
                        turfId,
                        date,
                        AVAILABILITY_BOOKING_STATUSES
                )
                .stream()
                .filter(this::isRelevantAvailabilityBooking)
                .collect(Collectors.toMap(booking -> booking.getSlot().getId(), Booking::getStatus, (first, second) -> first));
    }

    private boolean isRelevantAvailabilityBooking(Booking booking) {
        return isBookedStatus(booking.getStatus()) || isActiveBookingHold(booking);
    }

    private boolean isBookedStatus(BookingStatus status) {
        return status == BookingStatus.CONFIRMED
                || status == BookingStatus.CHECKED_IN
                || status == BookingStatus.COMPLETED
                || status == BookingStatus.NO_SHOW;
    }

    private boolean isActiveBookingHold(Booking booking) {
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return true;
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            return false;
        }
        return booking.getExpiryTime() == null || booking.getExpiryTime().isAfter(java.time.LocalDateTime.now());
    }

    private boolean isPastSlot(LocalDate date, LocalTime startTime) {
        return date.isBefore(LocalDate.now()) || (date.equals(LocalDate.now()) && startTime != null && !startTime.isAfter(LocalTime.now()));
    }

    private String calendarStatus(Turf turf, long availableSlots, boolean noSlots) {
        if (isTurfInMaintenance(turf)) {
            return "MAINTENANCE";
        }
        if (isTurfClosed(turf)) {
            return "CLOSED";
        }
        if (availableSlots > 0) {
            return "AVAILABLE";
        }
        return noSlots ? "NO_SLOTS" : "FULL";
    }

    private boolean isTurfInMaintenance(Turf turf) {
        return turf.getStatus() == TurfStatus.MAINTENANCE;
    }

    private boolean isTurfClosed(Turf turf) {
        return turf.getStatus() != null && turf.getStatus() != TurfStatus.ACTIVE && turf.getStatus() != TurfStatus.MAINTENANCE;
    }

    private boolean isVisibleSlotAvailability(SlotAvailability slotAvailability) {
        Slot slot = slotAvailability.getSlot();
        return slot != null
                && Boolean.TRUE.equals(slot.getIsActive())
                && !Boolean.TRUE.equals(slot.getArchived());
    }

    private boolean hasActiveSlotOverlap(List<Slot> existingSlots, int startMinute, int endMinute) {
        return existingSlots.stream().anyMatch(slot -> {
            int existingStart = toMinuteOfDay(slot.getStartTime());
            int existingEnd = toEndMinute(slot.getStartTime(), slot.getEndTime());
            return existingStart < endMinute && existingEnd > startMinute;
        });
    }

    private Slot findExactSlot(List<Slot> existingSlots, LocalTime startTime, LocalTime endTime) {
        return existingSlots.stream()
                .filter(slot -> slot.getStartTime().equals(startTime) && slot.getEndTime().equals(endTime))
                .findFirst()
                .orElse(null);
    }

    private ApiException scheduleOverlapConflict(LocalTime startTime, LocalTime endTime) {
        return ApiException.conflict("The active regular schedule has a different slot overlapping "
                + startTime + " - " + endTime
                + ". Regular slots repeat automatically and do not expire. Use Replace regular schedule only when you want to change the timetable.");
    }

    private List<Slot> activeRegularSlots(Long turfId) {
        return unarchivedRegularSlots(turfId).stream()
                .filter(slot -> Boolean.TRUE.equals(slot.getIsActive()))
                .collect(Collectors.toList());
    }

    private List<Slot> unarchivedRegularSlots(Long turfId) {
        List<Slot> slots = slotRepository.findByTurfIdAndArchivedFalse(turfId);
        if (slots == null) {
            return List.of();
        }
        return slots;
    }

    private int toMinuteOfDay(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }

    private int toEndMinute(LocalTime startTime, LocalTime endTime) {
        int startMinute = toMinuteOfDay(startTime);
        int endMinute = toMinuteOfDay(endTime);
        if (endMinute == startMinute) {
            return MINUTES_PER_DAY;
        }
        if (endMinute < startMinute) {
            return endMinute + MINUTES_PER_DAY;
        }
        return endMinute;
    }

    private LocalTime fromMinuteOfDay(int minute) {
        return LocalTime.of((minute % MINUTES_PER_DAY) / 60, minute % 60);
    }

    private void validateCreateSlotRequest(CreateSlotRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Slot details are required.");
        }
        if (request.getSlotDuration() == null || request.getSlotDuration() <= 0) {
            throw ApiException.badRequest("Slot duration must be greater than zero.");
        }
        if (request.getSlotDuration() > MAX_SLOT_DURATION_MINUTES) {
            throw ApiException.badRequest("Slot duration cannot exceed 720 minutes.");
        }
        if (request.getPrice() != null) {
            validatePrice(request.getPrice(), "Slot price");
        }
    }

    private void validateBlockRequest(BlockSlotRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Slot block details are required.");
        }
        if (request.getDate() == null) {
            throw ApiException.badRequest("Block date is required.");
        }
        if (request.getDate().isBefore(LocalDate.now())) {
            throw ApiException.badRequest("Cannot update slot availability for past dates.");
        }
        if (request.getSlotIds() == null || request.getSlotIds().isEmpty()) {
            throw ApiException.badRequest("Select at least one slot.");
        }
    }

    private void validatePriceOverrideRequest(SlotPriceOverrideRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Pricing details are required.");
        }
        if (request.getDate() == null) {
            throw ApiException.badRequest("Pricing date is required.");
        }
        if (request.getDate().isBefore(LocalDate.now())) {
            throw ApiException.badRequest("Cannot apply pricing to past dates.");
        }
        if (request.getSlotIds() == null || request.getSlotIds().isEmpty()) {
            throw ApiException.badRequest("Select at least one slot.");
        }
        validatePrice(request.getPrice(), "Override price");
        if (request.getReason() != null && request.getReason().length() > 180) {
            throw ApiException.badRequest("Pricing reason must be 180 characters or fewer.");
        }
    }

    private void validatePrice(BigDecimal price, String label) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest(label + " must be greater than zero.");
        }
        if (price.compareTo(MAX_SLOT_PRICE) > 0) {
            throw ApiException.badRequest(label + " cannot exceed Rs 100000.");
        }
    }

    private LocalTime parseSlotTime(String value, String label) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest(label + " is required.");
        }
        try {
            return LocalTime.parse(value.trim(), SLOT_TIME_FORMAT);
        } catch (DateTimeParseException ex) {
            throw ApiException.badRequest(label + " must use HH:mm format.");
        }
    }

    private List<Long> uniqueSlotIds(List<Long> slotIds) {
        Set<Long> unique = slotIds.stream()
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (unique.isEmpty()) {
            throw ApiException.badRequest("Select at least one valid slot.");
        }
        return new ArrayList<>(unique);
    }

    private boolean hasActiveBookingOnDate(Long slotId, LocalDate date) {
        if (bookingRepository == null) {
            return false;
        }
        return bookingRepository.findByBookingDateAndSlotIdAndStatusIn(date, slotId, ACTIVE_BOOKING_STATUSES)
                .stream()
                .anyMatch(this::isActiveBookingHold);
    }

    private boolean hasActiveFutureBookings(Long slotId) {
        if (bookingRepository == null) {
            return false;
        }
        return bookingRepository.findBySlotIdAndBookingDateGreaterThanEqualAndStatusIn(slotId, LocalDate.now(), ACTIVE_BOOKING_STATUSES)
                .stream()
                .anyMatch(this::isActiveBookingHold);
    }

    private void validateOwnershipOrAdmin(Turf turf, String userEmail) {
        User user = findUser(userEmail)
                .orElseThrow(() -> new ApiException("User not found"));

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);

        if (!isAdmin && !turf.getOwner().getId().equals(user.getId())) {
            throw new ApiException("User does not have permission to manage this turf");
        }
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
}
