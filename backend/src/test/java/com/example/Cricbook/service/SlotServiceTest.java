package com.example.Cricbook.service;

import com.example.Cricbook.dto.CreateSlotRequest;
import com.example.Cricbook.dto.SlotResponse;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.enums.RoleName;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.mapper.TurfMapper;
import com.example.Cricbook.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SlotServiceTest {

    @Mock
    private SlotRepository slotRepository;
    @Mock
    private SlotAvailabilityRepository slotAvailabilityRepository;
    @Mock
    private SlotPriceOverrideRepository slotPriceOverrideRepository;
    @Mock
    private TurfRepository turfRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TurfMapper turfMapper;
    @Mock
    private CacheService cacheService;

    @InjectMocks
    private SlotService slotService;

    @Test
    void testCreateSlots_Success() {
        Long turfId = 1L;
        String email = "owner@example.com";
        CreateSlotRequest request = new CreateSlotRequest();
        request.setStartTime("06:00");
        request.setEndTime("08:00");
        request.setSlotDuration(60);

        Role ownerRole = new Role(1L, RoleName.ROLE_OWNER);
        User owner = User.builder().id(1L).email(email).build();
        owner.getRoles().add(ownerRole);
        Turf turf = Turf.builder().id(turfId).owner(owner).build();
        turf.setPricePerHour(java.math.BigDecimal.valueOf(1500));

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(turf));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(owner));
        when(slotRepository.saveAll(anyList())).thenAnswer(i -> i.getArguments()[0]);

        List<SlotResponse> result = slotService.createSlots(turfId, email, request);

        assertFalse(result.isEmpty());
        org.mockito.ArgumentCaptor<List<Slot>> slotsCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(slotRepository).saveAll(slotsCaptor.capture());
        assertTrue(slotsCaptor.getValue().stream().allMatch(slot -> java.math.BigDecimal.valueOf(1500).equals(slot.getPrice())));
        verify(cacheService).evictByPrefix("search:");
    }

    @Test
    void testGetSlotsReturnsActiveAndDisabledRegularSlotsForOwnerFilters() {
        Long turfId = 1L;
        String email = "owner@example.com";
        Role ownerRole = new Role(1L, RoleName.ROLE_OWNER);
        User owner = User.builder().id(1L).email(email).build();
        owner.getRoles().add(ownerRole);
        Turf turf = Turf.builder().id(turfId).owner(owner).pricePerHour(java.math.BigDecimal.valueOf(1500)).build();
        Slot activeSlot = Slot.builder().id(10L).turf(turf).isActive(true).archived(false).build();
        Slot disabledSlot = Slot.builder().id(11L).turf(turf).isActive(false).archived(false).build();
        SlotResponse activeResponse = SlotResponse.builder().id(10L).isActive(true).build();
        SlotResponse disabledResponse = SlotResponse.builder().id(11L).isActive(false).build();

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(turf));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(owner));
        when(slotRepository.findByTurfIdAndArchivedFalse(turfId)).thenReturn(List.of(activeSlot, disabledSlot));
        when(turfMapper.toSlotResponse(activeSlot)).thenReturn(activeResponse);
        when(turfMapper.toSlotResponse(disabledSlot)).thenReturn(disabledResponse);

        List<SlotResponse> result = slotService.getSlots(turfId, email);

        assertEquals(2, result.size());
        assertTrue(result.get(0).getIsActive());
        assertFalse(result.get(1).getIsActive());
    }

    @Test
    void testCreateSlots_ReusesExactActiveScheduleWithoutDuplicates() {
        Long turfId = 1L;
        String email = "owner@example.com";
        CreateSlotRequest request = new CreateSlotRequest();
        request.setStartTime("06:00");
        request.setEndTime("08:00");
        request.setSlotDuration(60);
        request.setPrice(java.math.BigDecimal.valueOf(700));

        Role ownerRole = new Role(1L, RoleName.ROLE_OWNER);
        User owner = User.builder().id(1L).email(email).build();
        owner.getRoles().add(ownerRole);
        Turf turf = Turf.builder()
                .id(turfId)
                .owner(owner)
                .pricePerHour(java.math.BigDecimal.valueOf(1500))
                .build();
        Slot firstSlot = Slot.builder()
                .id(10L)
                .turf(turf)
                .startTime(java.time.LocalTime.of(6, 0))
                .endTime(java.time.LocalTime.of(7, 0))
                .price(java.math.BigDecimal.valueOf(500))
                .isActive(true)
                .archived(false)
                .build();
        Slot secondSlot = Slot.builder()
                .id(11L)
                .turf(turf)
                .startTime(java.time.LocalTime.of(7, 0))
                .endTime(java.time.LocalTime.of(8, 0))
                .price(java.math.BigDecimal.valueOf(500))
                .isActive(true)
                .archived(false)
                .build();

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(turf));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(owner));
        when(slotRepository.findByTurfIdAndArchivedFalse(turfId)).thenReturn(List.of(firstSlot, secondSlot));
        when(turfMapper.toSlotResponse(firstSlot)).thenReturn(SlotResponse.builder().id(10L).build());
        when(turfMapper.toSlotResponse(secondSlot)).thenReturn(SlotResponse.builder().id(11L).build());

        List<SlotResponse> result = slotService.createSlots(turfId, email, request);

        assertEquals(List.of(10L, 11L), result.stream().map(SlotResponse::getId).toList());
        assertEquals(java.math.BigDecimal.valueOf(500), firstSlot.getPrice());
        verify(slotRepository, never()).saveAll(anyList());
        verify(slotAvailabilityRepository).saveAll(argThat(rows -> {
            List<SlotAvailability> generatedRows = new java.util.ArrayList<>();
            rows.forEach(generatedRows::add);
            return generatedRows.size() == 180;
        }));
    }

    @Test
    void testCreateSlots_ReactivatesMatchingDisabledSlotsWithoutDuplicates() {
        Long turfId = 1L;
        String email = "owner@example.com";
        CreateSlotRequest request = new CreateSlotRequest();
        request.setStartTime("06:00");
        request.setEndTime("07:00");
        request.setSlotDuration(60);

        Role ownerRole = new Role(1L, RoleName.ROLE_OWNER);
        User owner = User.builder().id(1L).email(email).build();
        owner.getRoles().add(ownerRole);
        Turf turf = Turf.builder().id(turfId).owner(owner).build();
        Slot disabledSlot = Slot.builder()
                .id(10L)
                .turf(turf)
                .startTime(java.time.LocalTime.of(6, 0))
                .endTime(java.time.LocalTime.of(7, 0))
                .price(java.math.BigDecimal.valueOf(500))
                .isActive(false)
                .archived(false)
                .build();

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(turf));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(owner));
        when(slotRepository.findByTurfIdAndArchivedFalse(turfId)).thenReturn(List.of(disabledSlot));
        when(slotRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(turfMapper.toSlotResponse(disabledSlot)).thenReturn(SlotResponse.builder().id(10L).isActive(true).build());

        List<SlotResponse> result = slotService.createSlots(turfId, email, request);

        assertEquals(1, result.size());
        assertTrue(disabledSlot.getIsActive());
        verify(slotRepository).saveAll(argThat(slots -> {
            List<Slot> savedSlots = new java.util.ArrayList<>();
            slots.forEach(savedSlots::add);
            return savedSlots.size() == 1 && savedSlots.get(0).getId().equals(10L);
        }));
    }

    @Test
    void testCreateSlots_AllowsFullDaySchedule() {
        Long turfId = 1L;
        String email = "owner@example.com";
        CreateSlotRequest request = new CreateSlotRequest();
        request.setStartTime("00:00");
        request.setEndTime("00:00");
        request.setSlotDuration(60);
        request.setPrice(java.math.BigDecimal.valueOf(500));

        Role ownerRole = new Role(1L, RoleName.ROLE_OWNER);
        User owner = User.builder().id(1L).email(email).build();
        owner.getRoles().add(ownerRole);
        Turf turf = Turf.builder().id(turfId).owner(owner).build();

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(turf));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(owner));
        when(slotRepository.findByTurfIdAndArchivedFalse(turfId)).thenReturn(List.of());
        when(slotRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<SlotResponse> result = slotService.createSlots(turfId, email, request);

        assertEquals(24, result.size());
        verify(slotRepository).saveAll(argThat(slots -> {
            List<Slot> savedSlots = new java.util.ArrayList<>();
            slots.forEach(savedSlots::add);
            return savedSlots.size() == 24
                    && savedSlots.get(0).getStartTime().equals(java.time.LocalTime.MIDNIGHT)
                    && savedSlots.get(23).getStartTime().equals(java.time.LocalTime.of(23, 0))
                    && savedSlots.get(23).getEndTime().equals(java.time.LocalTime.MIDNIGHT);
        }));
    }

    @Test
    void testCreateSlots_FullDayFlagUsesMidnightToMidnight() {
        Long turfId = 1L;
        String email = "owner@example.com";
        CreateSlotRequest request = new CreateSlotRequest();
        request.setStartTime("06:00");
        request.setEndTime("22:00");
        request.setSlotDuration(120);
        request.setPrice(java.math.BigDecimal.valueOf(500));
        request.setFullDay(true);

        Role ownerRole = new Role(1L, RoleName.ROLE_OWNER);
        User owner = User.builder().id(1L).email(email).build();
        owner.getRoles().add(ownerRole);
        Turf turf = Turf.builder().id(turfId).owner(owner).build();

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(turf));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(owner));
        when(slotRepository.findByTurfIdAndArchivedFalse(turfId)).thenReturn(List.of());
        when(slotRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<SlotResponse> result = slotService.createSlots(turfId, email, request);

        assertEquals(12, result.size());
        verify(slotRepository).saveAll(argThat(slots -> {
            List<Slot> savedSlots = new java.util.ArrayList<>();
            slots.forEach(savedSlots::add);
            return savedSlots.size() == 12
                    && savedSlots.get(0).getStartTime().equals(java.time.LocalTime.MIDNIGHT)
                    && savedSlots.get(11).getStartTime().equals(java.time.LocalTime.of(22, 0))
                    && savedSlots.get(11).getEndTime().equals(java.time.LocalTime.MIDNIGHT);
        }));
    }

    @Test
    void testCreateSlots_ReplacesExistingConfigurationWhenRequested() {
        Long turfId = 1L;
        String email = "owner@example.com";
        CreateSlotRequest request = new CreateSlotRequest();
        request.setStartTime("09:00");
        request.setEndTime("11:00");
        request.setSlotDuration(60);
        request.setReplaceExisting(true);

        Role ownerRole = new Role(1L, RoleName.ROLE_OWNER);
        User owner = User.builder().id(1L).email(email).build();
        owner.getRoles().add(ownerRole);
        Turf turf = Turf.builder().id(turfId).owner(owner).build();
        Slot existingSlot = Slot.builder()
                .id(22L)
                .turf(turf)
                .startTime(java.time.LocalTime.of(6, 0))
                .endTime(java.time.LocalTime.of(7, 0))
                .price(java.math.BigDecimal.valueOf(500))
                .isActive(true)
                .archived(false)
                .build();

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(turf));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(owner));
        when(bookingRepository.existsByTurfIdAndBookingDateGreaterThanEqualAndStatusIn(eq(turfId), any(), anyList()))
                .thenReturn(false);
        when(slotRepository.findByTurfIdAndArchivedFalse(turfId)).thenReturn(List.of(existingSlot));
        when(slotRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<SlotResponse> result = slotService.createSlots(turfId, email, request);

        assertFalse(result.isEmpty());
        assertFalse(existingSlot.getIsActive());
        assertTrue(existingSlot.getArchived());
        verify(slotRepository, times(2)).saveAll(anyList());
    }

    @Test
    void testCreateSlots_RejectsReplaceWhenFutureBookingsExist() {
        Long turfId = 1L;
        String email = "owner@example.com";
        CreateSlotRequest request = new CreateSlotRequest();
        request.setStartTime("09:00");
        request.setEndTime("11:00");
        request.setSlotDuration(60);
        request.setReplaceExisting(true);

        Role ownerRole = new Role(1L, RoleName.ROLE_OWNER);
        User owner = User.builder().id(1L).email(email).build();
        owner.getRoles().add(ownerRole);
        Turf turf = Turf.builder().id(turfId).owner(owner).build();

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(turf));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(owner));
        when(bookingRepository.existsByTurfIdAndBookingDateGreaterThanEqualAndStatusIn(eq(turfId), any(), anyList()))
                .thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> slotService.createSlots(turfId, email, request));

        assertEquals("Cannot replace slot configuration while this turf has active future bookings. Cancel or reschedule those bookings first.", exception.getMessage());
        verify(slotRepository, never()).saveAll(anyList());
    }

    @Test
    void testCreateSlots_InvalidTime() {
        Long turfId = 1L;
        String email = "owner@example.com";
        CreateSlotRequest request = new CreateSlotRequest();
        request.setStartTime("10:00");
        request.setEndTime("10:30");
        request.setSlotDuration(60);

        Role ownerRole = new Role(1L, RoleName.ROLE_OWNER);
        User owner = User.builder().id(1L).email(email).build();
        owner.getRoles().add(ownerRole);
        Turf turf = Turf.builder().id(turfId).owner(owner).build();

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(turf));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(owner));

        ApiException exception = assertThrows(ApiException.class, () -> slotService.createSlots(turfId, email, request));

        assertEquals("Selected time range must be divisible by slot duration.", exception.getMessage());
    }

    @Test
    void testBlockSlots_Success() {
        Long turfId = 1L;
        String email = "owner@example.com";
        com.example.Cricbook.dto.BlockSlotRequest request = new com.example.Cricbook.dto.BlockSlotRequest();
        request.setSlotIds(java.util.List.of(1L));
        request.setDate(java.time.LocalDate.now());

        User owner = User.builder().id(1L).email(email).build();
        Turf turf = Turf.builder().id(turfId).owner(owner).build();
        Slot slot = Slot.builder().id(1L).turf(turf).build();
        SlotAvailability sa = SlotAvailability.builder().slot(slot).date(request.getDate()).build();

        when(turfRepository.findById(turfId)).thenReturn(java.util.Optional.of(turf));
        when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(owner));
        when(slotAvailabilityRepository.findBySlotIdAndDate(1L, request.getDate())).thenReturn(java.util.Optional.of(sa));

        slotService.blockSlots(turfId, email, request);

        assertTrue(sa.getIsBlocked());
        verify(slotAvailabilityRepository).saveAll(anyList());
    }

    @Test
    void testBlockSlots_RejectsActiveBookingOnSlot() {
        Long turfId = 1L;
        String email = "owner@example.com";
        com.example.Cricbook.dto.BlockSlotRequest request = new com.example.Cricbook.dto.BlockSlotRequest();
        request.setSlotIds(java.util.List.of(1L));
        request.setDate(java.time.LocalDate.now().plusDays(1));

        User owner = User.builder().id(1L).email(email).build();
        Turf turf = Turf.builder().id(turfId).owner(owner).build();
        Slot slot = Slot.builder().id(1L).turf(turf).build();
        SlotAvailability availability = SlotAvailability.builder()
                .slot(slot)
                .date(request.getDate())
                .isAvailable(true)
                .isBlocked(false)
                .build();
        Booking booking = Booking.builder()
                .id(9L)
                .slot(slot)
                .bookingDate(request.getDate())
                .status(BookingStatus.CONFIRMED)
                .build();

        when(turfRepository.findById(turfId)).thenReturn(java.util.Optional.of(turf));
        when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(owner));
        when(slotAvailabilityRepository.findBySlotIdAndDate(1L, request.getDate())).thenReturn(java.util.Optional.of(availability));
        when(bookingRepository.findByBookingDateAndSlotIdAndStatusIn(eq(request.getDate()), eq(1L), anyList()))
                .thenReturn(java.util.List.of(booking));

        ApiException exception = assertThrows(ApiException.class, () -> slotService.blockSlots(turfId, email, request));

        assertEquals("Cannot block a slot that already has an active booking.", exception.getMessage());
        verify(slotAvailabilityRepository, never()).saveAll(anyList());
    }

    @Test
    void testUpdateSlot_RejectsScheduleChangeWithFutureBookings() {
        Long turfId = 1L;
        Long slotId = 11L;
        String email = "owner@example.com";
        User owner = User.builder().id(1L).email(email).build();
        Turf turf = Turf.builder().id(turfId).owner(owner).build();
        Slot slot = Slot.builder()
                .id(slotId)
                .turf(turf)
                .startTime(java.time.LocalTime.of(7, 0))
                .endTime(java.time.LocalTime.of(8, 0))
                .price(java.math.BigDecimal.valueOf(1000))
                .isActive(true)
                .build();
        Booking booking = Booking.builder()
                .id(99L)
                .slot(slot)
                .bookingDate(java.time.LocalDate.now().plusDays(2))
                .status(BookingStatus.CONFIRMED)
                .build();
        com.example.Cricbook.dto.UpdateSlotRequest request = new com.example.Cricbook.dto.UpdateSlotRequest();
        request.setStartTime("08:00");
        request.setEndTime("09:00");

        when(turfRepository.findById(turfId)).thenReturn(java.util.Optional.of(turf));
        when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(owner));
        when(slotRepository.findById(slotId)).thenReturn(java.util.Optional.of(slot));
        when(slotRepository.findByTurfIdAndArchivedFalse(turfId)).thenReturn(java.util.List.of(slot));
        when(bookingRepository.findBySlotIdAndBookingDateGreaterThanEqualAndStatusIn(eq(slotId), any(), anyList()))
                .thenReturn(java.util.List.of(booking));

        ApiException exception = assertThrows(ApiException.class, () -> slotService.updateSlot(turfId, slotId, email, request));

        assertEquals("This slot has active bookings. Cancel or reschedule those bookings before changing timing or disabling it.", exception.getMessage());
        verify(slotRepository, never()).save(any(Slot.class));
    }

    @Test
    void testUpdateSlot_AllowsReactivationWhenNoActiveOverlapExists() {
        Long turfId = 1L;
        Long slotId = 46L;
        String email = "owner@example.com";
        User owner = User.builder().id(1L).email(email).build();
        Turf turf = Turf.builder().id(turfId).owner(owner).build();
        Slot slot = Slot.builder()
                .id(slotId)
                .turf(turf)
                .startTime(java.time.LocalTime.of(6, 0))
                .endTime(java.time.LocalTime.of(7, 0))
                .price(java.math.BigDecimal.valueOf(500))
                .isActive(false)
                .archived(false)
                .build();
        com.example.Cricbook.dto.UpdateSlotRequest request = new com.example.Cricbook.dto.UpdateSlotRequest();
        request.setIsActive(true);

        when(turfRepository.findById(turfId)).thenReturn(java.util.Optional.of(turf));
        when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(owner));
        when(slotRepository.findById(slotId)).thenReturn(java.util.Optional.of(slot));
        when(slotRepository.findByTurfIdAndArchivedFalse(turfId)).thenReturn(java.util.List.of(slot));
        when(slotRepository.save(slot)).thenReturn(slot);

        slotService.updateSlot(turfId, slotId, email, request);

        assertTrue(slot.getIsActive());
        verify(slotRepository).save(slot);
        verify(cacheService).evictByPrefix("search:");
    }

    @Test
    void testUpdateSlot_RejectsReactivationWhenActiveOverlapExistsWithoutSaving() {
        Long turfId = 1L;
        Long slotId = 46L;
        String email = "owner@example.com";
        User owner = User.builder().id(1L).email(email).build();
        Turf turf = Turf.builder().id(turfId).owner(owner).build();
        Slot slot = Slot.builder()
                .id(slotId)
                .turf(turf)
                .startTime(java.time.LocalTime.of(6, 0))
                .endTime(java.time.LocalTime.of(7, 0))
                .price(java.math.BigDecimal.valueOf(500))
                .isActive(false)
                .archived(false)
                .build();
        Slot conflictingSlot = Slot.builder()
                .id(47L)
                .turf(turf)
                .startTime(java.time.LocalTime.of(6, 30))
                .endTime(java.time.LocalTime.of(7, 30))
                .price(java.math.BigDecimal.valueOf(500))
                .isActive(true)
                .archived(false)
                .build();
        com.example.Cricbook.dto.UpdateSlotRequest request = new com.example.Cricbook.dto.UpdateSlotRequest();
        request.setIsActive(true);

        when(turfRepository.findById(turfId)).thenReturn(java.util.Optional.of(turf));
        when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(owner));
        when(slotRepository.findById(slotId)).thenReturn(java.util.Optional.of(slot));
        when(slotRepository.findByTurfIdAndArchivedFalse(turfId)).thenReturn(java.util.List.of(slot, conflictingSlot));

        ApiException exception = assertThrows(ApiException.class, () -> slotService.updateSlot(turfId, slotId, email, request));

        assertEquals("This slot overlaps another active slot. Disable the conflicting slot first or adjust the start/end time.", exception.getMessage());
        assertFalse(slot.getIsActive());
        verify(slotRepository, never()).save(any(Slot.class));
    }

    @Test
    void testApplyPriceOverride_RejectsPastDateAndInvalidPrice() {
        com.example.Cricbook.dto.SlotPriceOverrideRequest request = new com.example.Cricbook.dto.SlotPriceOverrideRequest();
        request.setSlotIds(java.util.List.of(1L));
        request.setDate(java.time.LocalDate.now().minusDays(1));
        request.setPrice(java.math.BigDecimal.valueOf(500));

        ApiException exception = assertThrows(ApiException.class, () -> slotService.applyPriceOverride(1L, "owner@example.com", request));

        assertEquals("Cannot apply pricing to past dates.", exception.getMessage());
        verify(turfRepository, never()).findById(anyLong());
    }

    @Test
    void testGetAvailability() {
        Long turfId = 1L;
        java.time.LocalDate date = java.time.LocalDate.now();
        when(turfRepository.findById(turfId)).thenReturn(java.util.Optional.of(Turf.builder().id(turfId).build()));
        when(slotAvailabilityRepository.findBySlotTurfIdAndDate(turfId, date)).thenReturn(java.util.Collections.emptyList());
        when(bookingRepository.findByTurfIdAndBookingDateAndStatusIn(eq(turfId), eq(date), anyList())).thenReturn(java.util.Collections.emptyList());
        when(slotPriceOverrideRepository.findBySlotTurfIdAndDate(turfId, date)).thenReturn(java.util.Collections.emptyList());

        java.util.List<com.example.Cricbook.dto.SlotAvailabilityResponse> result = slotService.getAvailability(turfId, date);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAvailabilityAutomaticallyGeneratesMissingFutureDate() {
        Long turfId = 1L;
        java.time.LocalDate date = java.time.LocalDate.now().plusDays(120);
        Turf turf = Turf.builder().id(turfId).pricePerHour(java.math.BigDecimal.valueOf(1500)).build();
        Slot slot = Slot.builder()
                .id(10L)
                .turf(turf)
                .startTime(java.time.LocalTime.of(7, 0))
                .endTime(java.time.LocalTime.of(8, 0))
                .price(java.math.BigDecimal.valueOf(500))
                .isActive(true)
                .archived(false)
                .build();
        SlotAvailability generated = SlotAvailability.builder()
                .id(20L)
                .slot(slot)
                .date(date)
                .isAvailable(true)
                .isBlocked(false)
                .build();

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(turf));
        when(slotRepository.findByTurfIdAndArchivedFalse(turfId)).thenReturn(List.of(slot));
        when(slotAvailabilityRepository.findBySlotTurfIdAndDateBetween(turfId, date, date))
                .thenReturn(Collections.emptyList());
        when(slotAvailabilityRepository.findBySlotTurfIdAndDate(turfId, date)).thenReturn(List.of(generated));
        when(bookingRepository.findByTurfIdAndBookingDateAndStatusIn(eq(turfId), eq(date), anyList()))
                .thenReturn(Collections.emptyList());
        when(slotPriceOverrideRepository.findBySlotTurfIdAndDate(turfId, date)).thenReturn(Collections.emptyList());
        when(turfMapper.toSlotAvailabilityResponse(generated)).thenReturn(
                com.example.Cricbook.dto.SlotAvailabilityResponse.builder()
                        .id(20L)
                        .slotId(10L)
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .date(date)
                        .available(true)
                        .isAvailable(true)
                        .blocked(false)
                        .isBlocked(false)
                        .price(slot.getPrice())
                        .build()
        );

        java.util.List<com.example.Cricbook.dto.SlotAvailabilityResponse> result = slotService.getAvailability(turfId, date);

        assertEquals(1, result.size());
        assertEquals("AVAILABLE", result.get(0).getStatus());
        assertEquals(java.math.BigDecimal.valueOf(1500), result.get(0).getListingPrice());
        verify(slotAvailabilityRepository).saveAll(argThat(rows -> {
            List<SlotAvailability> savedRows = new java.util.ArrayList<>();
            rows.forEach(savedRows::add);
            return savedRows.size() == 1
                    && savedRows.get(0).getSlot().getId().equals(slot.getId())
                    && savedRows.get(0).getDate().equals(date)
                    && savedRows.get(0).getIsAvailable()
                    && !savedRows.get(0).getIsBlocked();
        }));
    }

    @Test
    void testGetAvailabilityKeepsExistingFutureAvailabilityUnchanged() {
        Long turfId = 1L;
        java.time.LocalDate date = java.time.LocalDate.now().plusDays(120);
        Turf turf = Turf.builder().id(turfId).build();
        Slot slot = Slot.builder()
                .id(10L)
                .turf(turf)
                .startTime(java.time.LocalTime.of(7, 0))
                .endTime(java.time.LocalTime.of(8, 0))
                .isActive(true)
                .archived(false)
                .build();
        SlotAvailability blocked = SlotAvailability.builder()
                .id(20L)
                .slot(slot)
                .date(date)
                .isAvailable(false)
                .isBlocked(true)
                .build();

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(turf));
        when(slotRepository.findByTurfIdAndArchivedFalse(turfId)).thenReturn(List.of(slot));
        when(slotAvailabilityRepository.findBySlotTurfIdAndDateBetween(turfId, date, date)).thenReturn(List.of(blocked));
        when(slotAvailabilityRepository.findBySlotTurfIdAndDate(turfId, date)).thenReturn(List.of(blocked));
        when(bookingRepository.findByTurfIdAndBookingDateAndStatusIn(eq(turfId), eq(date), anyList()))
                .thenReturn(Collections.emptyList());
        when(slotPriceOverrideRepository.findBySlotTurfIdAndDate(turfId, date)).thenReturn(Collections.emptyList());
        when(turfMapper.toSlotAvailabilityResponse(blocked)).thenReturn(
                com.example.Cricbook.dto.SlotAvailabilityResponse.builder()
                        .id(20L)
                        .slotId(10L)
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .date(date)
                        .available(false)
                        .isAvailable(false)
                        .blocked(true)
                        .isBlocked(true)
                        .build()
        );

        java.util.List<com.example.Cricbook.dto.SlotAvailabilityResponse> result = slotService.getAvailability(turfId, date);

        assertEquals("BLOCKED", result.get(0).getStatus());
        verify(slotAvailabilityRepository, never()).saveAll(anyList());
        assertTrue(blocked.getIsBlocked());
        assertFalse(blocked.getIsAvailable());
    }

    @Test
    void testGetAvailabilityMarksPendingBookingsAsLocked() {
        Long turfId = 1L;
        java.time.LocalDate date = java.time.LocalDate.now().plusDays(1);
        Turf turf = Turf.builder().id(turfId).build();
        Slot slot = Slot.builder()
                .id(10L)
                .turf(turf)
                .startTime(java.time.LocalTime.of(7, 0))
                .endTime(java.time.LocalTime.of(8, 0))
                .isActive(true)
                .build();
        SlotAvailability availability = SlotAvailability.builder()
                .id(20L)
                .slot(slot)
                .date(date)
                .isAvailable(true)
                .isBlocked(false)
                .build();
        Booking booking = Booking.builder()
                .id(30L)
                .slot(slot)
                .bookingDate(date)
                .status(BookingStatus.PENDING)
                .build();

        when(turfRepository.findById(turfId)).thenReturn(java.util.Optional.of(turf));
        when(slotAvailabilityRepository.findBySlotTurfIdAndDate(turfId, date)).thenReturn(java.util.List.of(availability));
        when(bookingRepository.findByTurfIdAndBookingDateAndStatusIn(eq(turfId), eq(date), anyList())).thenReturn(java.util.List.of(booking));
        when(slotPriceOverrideRepository.findBySlotTurfIdAndDate(turfId, date)).thenReturn(java.util.Collections.emptyList());
        when(turfMapper.toSlotAvailabilityResponse(availability)).thenReturn(
                com.example.Cricbook.dto.SlotAvailabilityResponse.builder()
                        .id(20L)
                        .slotId(10L)
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .date(date)
                        .available(true)
                        .isAvailable(true)
                        .blocked(false)
                        .isBlocked(false)
                        .build()
        );

        java.util.List<com.example.Cricbook.dto.SlotAvailabilityResponse> result = slotService.getAvailability(turfId, date);

        assertEquals("LOCKED", result.get(0).getStatus());
        assertTrue(result.get(0).getLocked());
        assertFalse(result.get(0).getAvailable());
    }

    @Test
    void testGetAvailabilityIgnoresExpiredPendingHolds() {
        Long turfId = 1L;
        java.time.LocalDate date = java.time.LocalDate.now().plusDays(1);
        Turf turf = Turf.builder().id(turfId).build();
        Slot slot = Slot.builder()
                .id(10L)
                .turf(turf)
                .startTime(java.time.LocalTime.of(7, 0))
                .endTime(java.time.LocalTime.of(8, 0))
                .isActive(true)
                .build();
        SlotAvailability availability = SlotAvailability.builder()
                .id(20L)
                .slot(slot)
                .date(date)
                .isAvailable(false)
                .isBlocked(false)
                .build();
        Booking booking = Booking.builder()
                .id(30L)
                .slot(slot)
                .bookingDate(date)
                .status(BookingStatus.PENDING)
                .expiryTime(java.time.LocalDateTime.now().minusMinutes(1))
                .build();

        when(turfRepository.findById(turfId)).thenReturn(java.util.Optional.of(turf));
        when(slotAvailabilityRepository.findBySlotTurfIdAndDate(turfId, date)).thenReturn(java.util.List.of(availability));
        when(bookingRepository.findByTurfIdAndBookingDateAndStatusIn(eq(turfId), eq(date), anyList())).thenReturn(java.util.List.of(booking));
        when(slotPriceOverrideRepository.findBySlotTurfIdAndDate(turfId, date)).thenReturn(java.util.Collections.emptyList());
        when(turfMapper.toSlotAvailabilityResponse(availability)).thenReturn(
                com.example.Cricbook.dto.SlotAvailabilityResponse.builder()
                        .id(20L)
                        .slotId(10L)
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .date(date)
                        .available(false)
                        .isAvailable(false)
                        .blocked(false)
                        .isBlocked(false)
                        .build()
        );

        java.util.List<com.example.Cricbook.dto.SlotAvailabilityResponse> result = slotService.getAvailability(turfId, date);

        assertEquals("AVAILABLE", result.get(0).getStatus());
        assertFalse(result.get(0).getLocked());
        assertTrue(result.get(0).getAvailable());
    }

    @Test
    void testGetAvailabilityMarksPastUnbookedSlotExpired() {
        Long turfId = 1L;
        java.time.LocalDate date = java.time.LocalDate.now().minusDays(1);
        Turf turf = Turf.builder().id(turfId).build();
        Slot slot = Slot.builder()
                .id(10L)
                .turf(turf)
                .startTime(java.time.LocalTime.of(7, 0))
                .endTime(java.time.LocalTime.of(8, 0))
                .isActive(true)
                .build();
        SlotAvailability availability = SlotAvailability.builder()
                .id(20L)
                .slot(slot)
                .date(date)
                .isAvailable(true)
                .isBlocked(false)
                .build();

        when(turfRepository.findById(turfId)).thenReturn(java.util.Optional.of(turf));
        when(slotAvailabilityRepository.findBySlotTurfIdAndDate(turfId, date)).thenReturn(java.util.List.of(availability));
        when(bookingRepository.findByTurfIdAndBookingDateAndStatusIn(eq(turfId), eq(date), anyList())).thenReturn(java.util.Collections.emptyList());
        when(slotPriceOverrideRepository.findBySlotTurfIdAndDate(turfId, date)).thenReturn(java.util.Collections.emptyList());
        when(turfMapper.toSlotAvailabilityResponse(availability)).thenReturn(
                com.example.Cricbook.dto.SlotAvailabilityResponse.builder()
                        .id(20L)
                        .slotId(10L)
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .date(date)
                        .available(true)
                        .isAvailable(true)
                        .blocked(false)
                        .isBlocked(false)
                        .build()
        );

        java.util.List<com.example.Cricbook.dto.SlotAvailabilityResponse> result = slotService.getAvailability(turfId, date);

        assertEquals("EXPIRED", result.get(0).getStatus());
        assertFalse(result.get(0).getAvailable());
        assertFalse(result.get(0).getBlocked());
    }

    @Test
    void testGetAvailabilityMarksCompletedPastBookingAsBooked() {
        Long turfId = 1L;
        java.time.LocalDate date = java.time.LocalDate.now().minusDays(1);
        Turf turf = Turf.builder().id(turfId).build();
        Slot slot = Slot.builder()
                .id(10L)
                .turf(turf)
                .startTime(java.time.LocalTime.of(7, 0))
                .endTime(java.time.LocalTime.of(8, 0))
                .isActive(true)
                .build();
        SlotAvailability availability = SlotAvailability.builder()
                .id(20L)
                .slot(slot)
                .date(date)
                .isAvailable(false)
                .isBlocked(false)
                .build();
        Booking booking = Booking.builder()
                .id(30L)
                .slot(slot)
                .bookingDate(date)
                .status(BookingStatus.COMPLETED)
                .build();

        when(turfRepository.findById(turfId)).thenReturn(java.util.Optional.of(turf));
        when(slotAvailabilityRepository.findBySlotTurfIdAndDate(turfId, date)).thenReturn(java.util.List.of(availability));
        when(bookingRepository.findByTurfIdAndBookingDateAndStatusIn(eq(turfId), eq(date), anyList())).thenReturn(java.util.List.of(booking));
        when(slotPriceOverrideRepository.findBySlotTurfIdAndDate(turfId, date)).thenReturn(java.util.Collections.emptyList());
        when(turfMapper.toSlotAvailabilityResponse(availability)).thenReturn(
                com.example.Cricbook.dto.SlotAvailabilityResponse.builder()
                        .id(20L)
                        .slotId(10L)
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .date(date)
                        .available(false)
                        .isAvailable(false)
                        .blocked(false)
                        .isBlocked(false)
                        .build()
        );

        java.util.List<com.example.Cricbook.dto.SlotAvailabilityResponse> result = slotService.getAvailability(turfId, date);

        assertEquals("BOOKED", result.get(0).getStatus());
        assertFalse(result.get(0).getAvailable());
    }

    @Test
    void testGetAvailabilityMarksMaintenanceTurfUnavailable() {
        Long turfId = 1L;
        java.time.LocalDate date = java.time.LocalDate.now().plusDays(1);
        Turf turf = Turf.builder()
                .id(turfId)
                .status(com.example.Cricbook.enums.TurfStatus.MAINTENANCE)
                .build();
        Slot slot = Slot.builder()
                .id(10L)
                .turf(turf)
                .startTime(java.time.LocalTime.of(7, 0))
                .endTime(java.time.LocalTime.of(8, 0))
                .isActive(true)
                .build();
        SlotAvailability availability = SlotAvailability.builder()
                .id(20L)
                .slot(slot)
                .date(date)
                .isAvailable(true)
                .isBlocked(false)
                .build();

        when(turfRepository.findById(turfId)).thenReturn(java.util.Optional.of(turf));
        when(slotAvailabilityRepository.findBySlotTurfIdAndDate(turfId, date)).thenReturn(java.util.List.of(availability));
        when(bookingRepository.findByTurfIdAndBookingDateAndStatusIn(eq(turfId), eq(date), anyList())).thenReturn(java.util.Collections.emptyList());
        when(slotPriceOverrideRepository.findBySlotTurfIdAndDate(turfId, date)).thenReturn(java.util.Collections.emptyList());
        when(turfMapper.toSlotAvailabilityResponse(availability)).thenReturn(
                com.example.Cricbook.dto.SlotAvailabilityResponse.builder()
                        .id(20L)
                        .slotId(10L)
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .date(date)
                        .available(true)
                        .isAvailable(true)
                        .blocked(false)
                        .isBlocked(false)
                        .build()
        );

        java.util.List<com.example.Cricbook.dto.SlotAvailabilityResponse> result = slotService.getAvailability(turfId, date);

        assertEquals("MAINTENANCE", result.get(0).getStatus());
        assertFalse(result.get(0).getAvailable());
    }

    @Test
    void testGetCalendarMarksMaintenanceTurfClosed() {
        Long turfId = 1L;
        java.time.LocalDate date = java.time.LocalDate.now().plusDays(1);
        Turf turf = Turf.builder()
                .id(turfId)
                .status(com.example.Cricbook.enums.TurfStatus.MAINTENANCE)
                .build();
        Slot slot = Slot.builder()
                .id(10L)
                .turf(turf)
                .startTime(java.time.LocalTime.of(7, 0))
                .endTime(java.time.LocalTime.of(8, 0))
                .isActive(true)
                .build();
        SlotAvailability availability = SlotAvailability.builder()
                .id(20L)
                .slot(slot)
                .date(date)
                .isAvailable(true)
                .isBlocked(false)
                .build();

        when(turfRepository.findById(turfId)).thenReturn(java.util.Optional.of(turf));
        when(slotAvailabilityRepository.findBySlotTurfIdAndDateBetween(eq(turfId), eq(date), eq(date))).thenReturn(java.util.List.of(availability));
        when(bookingRepository.findByTurfIdAndBookingDateAndStatusIn(eq(turfId), eq(date), anyList())).thenReturn(java.util.Collections.emptyList());

        java.util.List<com.example.Cricbook.dto.SlotCalendarDateResponse> result = slotService.getCalendar(turfId, date, date);

        assertEquals("MAINTENANCE", result.get(0).getStatus());
        assertEquals(0, result.get(0).getAvailableSlots());
    }

    @Test
    void testGetCalendarReturnsEveryDateInRequestedWeek() {
        Long turfId = 1L;
        java.time.LocalDate start = java.time.LocalDate.of(2026, 10, 5);
        java.time.LocalDate end = start.plusDays(6);
        Turf turf = Turf.builder().id(turfId).build();

        when(turfRepository.findById(turfId)).thenReturn(java.util.Optional.of(turf));
        when(slotAvailabilityRepository.findBySlotTurfIdAndDateBetween(turfId, start, end))
                .thenReturn(java.util.Collections.emptyList());

        java.util.List<com.example.Cricbook.dto.SlotCalendarDateResponse> result = slotService.getCalendar(turfId, start, end);

        assertEquals(7, result.size());
        assertEquals(start, result.get(0).getDate());
        assertEquals(end, result.get(6).getDate());
    }
}
