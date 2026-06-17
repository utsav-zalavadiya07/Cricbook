package com.example.Cricbook.service;

import com.example.Cricbook.dto.TravelExperienceRequests;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TravelExperienceService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final UserRepository userRepository;
    private final TravelPartnerRepository partnerRepository;
    private final TravelPackageRepository packageRepository;
    private final TravelBookingRepository bookingRepository;
    private final TravelEventRepository eventRepository;
    private final TravelTicketTypeRepository ticketTypeRepository;
    private final TravelTicketRepository ticketRepository;
    private final TravelAccommodationRepository accommodationRepository;
    private final TravelAccommodationBookingRepository accommodationBookingRepository;
    private final TravelAuditRepository auditRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> packageMarketplace(String query, String sport, String originCity, String destination, BigDecimal maxPrice) {
        return packageRepository.searchPublished(
                        TravelPackageStatus.PUBLISHED,
                        clean(query),
                        clean(sport),
                        clean(originCity),
                        clean(destination),
                        maxPrice)
                .stream()
                .map(this::mapPackage)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> eventMarketplace(TravelEventType eventType, String query, String sport, String city) {
        return eventRepository.searchPublished(TravelEventStatus.PUBLISHED, eventType, clean(query), clean(sport), clean(city))
                .stream()
                .map(this::mapEvent)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> accommodations(String city, String accommodationType) {
        return accommodationRepository.searchAvailable(clean(city), clean(accommodationType)).stream()
                .map(this::mapAccommodation)
                .toList();
    }

    @Transactional
    public Map<String, Object> registerPartner(TravelExperienceRequests.PartnerRequest request, String actor) {
        validatePartner(request);
        if (partnerRepository.existsByEmailIgnoreCase(request.getEmail().trim())) {
            throw ApiException.conflict("Travel partner email already exists.");
        }
        TravelPartner partner = partnerRepository.save(TravelPartner.builder()
                .partnerName(request.getPartnerName().trim())
                .partnerType(normalize(request.getPartnerType(), "HOTEL"))
                .email(request.getEmail().trim().toLowerCase(Locale.ROOT))
                .phone(clean(request.getPhone()))
                .city(clean(request.getCity()))
                .status(TravelPartnerStatus.PENDING)
                .commissionPercent(moneyOrZero(request.getCommissionPercent()))
                .notes(clean(request.getNotes()))
                .build());
        audit(actor, "TRAVEL_PARTNER_REGISTERED", "TravelPartner", partner.getId(), partner.getPartnerName());
        return mapPartner(partner);
    }

    @Transactional
    public Map<String, Object> updatePartnerStatus(Long partnerId, TravelExperienceRequests.PartnerStatusRequest request, String actor) {
        TravelPartner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> ApiException.notFound("Travel partner was not found."));
        partner.setStatus(request == null || request.getStatus() == null ? TravelPartnerStatus.APPROVED : request.getStatus());
        partner.setNotes(clean(request == null ? null : request.getNotes()));
        TravelPartner saved = partnerRepository.save(partner);
        audit(actor, "TRAVEL_PARTNER_STATUS_UPDATED", "TravelPartner", saved.getId(), saved.getStatus().name());
        return mapPartner(saved);
    }

    @Transactional
    public Map<String, Object> createPackage(TravelExperienceRequests.PackageRequest request, String actor) {
        validatePackage(request);
        if (packageRepository.existsByPackageCodeIgnoreCase(request.getPackageCode().trim())) {
            throw ApiException.conflict("Travel package code already exists.");
        }
        TravelPartner partner = request.getPartnerId() == null ? null : partnerRepository.findById(request.getPartnerId())
                .orElseThrow(() -> ApiException.notFound("Travel partner was not found."));
        TravelPackage travelPackage = packageRepository.save(TravelPackage.builder()
                .packageCode(request.getPackageCode().trim().toUpperCase(Locale.ROOT))
                .title(request.getTitle().trim())
                .sport(request.getSport().trim())
                .originCity(clean(request.getOriginCity()))
                .destination(request.getDestination().trim())
                .packageType(normalize(request.getPackageType(), "FULL_PACKAGE"))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .cancellationDeadline(request.getCancellationDeadline())
                .pricePerPerson(money(request.getPricePerPerson()))
                .capacity(Math.max(1, request.getCapacity()))
                .bookedCount(0)
                .includesAccommodation(Boolean.TRUE.equals(request.getIncludesAccommodation()))
                .includesTransport(Boolean.TRUE.equals(request.getIncludesTransport()))
                .linkedTournamentId(request.getLinkedTournamentId())
                .partner(partner)
                .status(request.getStatus() == null ? TravelPackageStatus.DRAFT : request.getStatus())
                .itinerary(clean(request.getItinerary()))
                .build());
        audit(actor, "TRAVEL_PACKAGE_CREATED", "TravelPackage", travelPackage.getId(), travelPackage.getPackageCode());
        return mapPackage(travelPackage);
    }

    @Transactional
    public Map<String, Object> bookPackage(Long packageId, TravelExperienceRequests.PackageBookingRequest request, String loginId) {
        User user = findUser(loginId);
        TravelPackage travelPackage = packageRepository.findById(packageId)
                .orElseThrow(() -> ApiException.notFound("Travel package was not found."));
        if (travelPackage.getStatus() != TravelPackageStatus.PUBLISHED) {
            throw ApiException.conflict("Travel package is not open for booking.");
        }
        int participants = request == null || request.getParticipantCount() == null ? 1 : Math.max(1, request.getParticipantCount());
        int available = availablePackageCapacity(travelPackage);
        if (available < participants) {
            throw ApiException.conflict("Travel package capacity exceeded.");
        }
        BigDecimal amount = travelPackage.getPricePerPerson().multiply(BigDecimal.valueOf(participants)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = travelDiscount(clean(request == null ? null : request.getCouponCode()), amount);
        TravelBooking booking = bookingRepository.save(TravelBooking.builder()
                .user(user)
                .travelPackage(travelPackage)
                .bookingReference("TRV-" + System.currentTimeMillis() + "-" + user.getId())
                .bookingType(normalize(request == null ? null : request.getBookingType(), participants > 1 ? "GROUP" : "INDIVIDUAL"))
                .participantCount(participants)
                .amount(amount)
                .discount(discount)
                .total(amount.subtract(discount).max(ZERO).setScale(2, RoundingMode.HALF_UP))
                .refundAmount(ZERO)
                .paymentStatus(hasText(request == null ? null : request.getPaymentReference()) ? "PAID" : "PENDING")
                .couponCode(clean(request == null ? null : request.getCouponCode()))
                .status(TravelBookingStatus.CONFIRMED)
                .travelerNames(clean(request == null ? null : request.getTravelerNames()))
                .build());
        travelPackage.setBookedCount(travelPackage.getBookedCount() + participants);
        if (travelPackage.getBookedCount() >= travelPackage.getCapacity()) {
            travelPackage.setStatus(TravelPackageStatus.SOLD_OUT);
        }
        packageRepository.save(travelPackage);
        audit(loginId, "TRAVEL_PACKAGE_BOOKED", "TravelBooking", booking.getId(), booking.getBookingReference());
        return mapBooking(booking);
    }

    @Transactional
    public Map<String, Object> cancelPackageBooking(Long bookingId, String loginId) {
        User user = findUser(loginId);
        TravelBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Travel booking was not found."));
        if (!booking.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("You can only cancel your own travel booking.");
        }
        if (booking.getStatus() != TravelBookingStatus.CONFIRMED) {
            throw ApiException.conflict("Travel booking is not cancellable.");
        }
        TravelPackage travelPackage = booking.getTravelPackage();
        boolean beforeDeadline = travelPackage.getCancellationDeadline() == null || !LocalDate.now().isAfter(travelPackage.getCancellationDeadline());
        booking.setStatus(TravelBookingStatus.CANCELLED);
        booking.setRefundAmount(beforeDeadline ? booking.getTotal() : ZERO);
        travelPackage.setBookedCount(Math.max(0, travelPackage.getBookedCount() - booking.getParticipantCount()));
        if (travelPackage.getStatus() == TravelPackageStatus.SOLD_OUT) {
            travelPackage.setStatus(TravelPackageStatus.PUBLISHED);
        }
        packageRepository.save(travelPackage);
        TravelBooking saved = bookingRepository.save(booking);
        audit(loginId, "TRAVEL_PACKAGE_CANCELLED", "TravelBooking", saved.getId(), saved.getRefundAmount().toPlainString());
        return mapBooking(saved);
    }

    @Transactional
    public Map<String, Object> createEvent(TravelExperienceRequests.EventRequest request, String actor) {
        validateEvent(request);
        TravelEvent event = eventRepository.save(TravelEvent.builder()
                .title(request.getTitle().trim())
                .eventType(request.getEventType() == null ? TravelEventType.EVENT : request.getEventType())
                .sport(request.getSport().trim())
                .city(request.getCity().trim())
                .venue(clean(request.getVenue()))
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .capacity(Math.max(1, request.getCapacity()))
                .registeredCount(0)
                .basePrice(moneyOrZero(request.getBasePrice()))
                .status(request.getStatus() == null ? TravelEventStatus.DRAFT : request.getStatus())
                .description(clean(request.getDescription()))
                .build());
        audit(actor, "TRAVEL_EVENT_CREATED", "TravelEvent", event.getId(), event.getTitle());
        return mapEvent(event);
    }

    @Transactional
    public Map<String, Object> createTicketType(Long eventId, TravelExperienceRequests.TicketTypeRequest request, String actor) {
        if (request == null || !hasText(request.getName()) || request.getPrice() == null || request.getTotalInventory() == null) {
            throw ApiException.badRequest("Ticket name, price, and inventory are required.");
        }
        TravelEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> ApiException.notFound("Travel event was not found."));
        if (request.getTotalInventory() <= 0) {
            throw ApiException.badRequest("Ticket inventory must be greater than zero.");
        }
        TravelTicketType ticketType = ticketTypeRepository.save(TravelTicketType.builder()
                .event(event)
                .name(request.getName().trim())
                .price(money(request.getPrice()))
                .totalInventory(request.getTotalInventory())
                .soldInventory(0)
                .active(request.getActive() == null || request.getActive())
                .build());
        audit(actor, "TRAVEL_TICKET_TYPE_CREATED", "TravelTicketType", ticketType.getId(), ticketType.getName());
        return mapTicketType(ticketType);
    }

    @Transactional
    public Map<String, Object> buyTicket(Long eventId, TravelExperienceRequests.TicketPurchaseRequest request, String loginId) {
        User user = findUser(loginId);
        TravelEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> ApiException.notFound("Travel event was not found."));
        if (event.getStatus() != TravelEventStatus.PUBLISHED) {
            throw ApiException.conflict("Event is not open for ticket purchase.");
        }
        TravelTicketType ticketType = ticketTypeRepository.findById(requiredId(request == null ? null : request.getTicketTypeId(), "Ticket type is required."))
                .orElseThrow(() -> ApiException.notFound("Ticket type was not found."));
        if (!ticketType.getEvent().getId().equals(event.getId())) {
            throw ApiException.badRequest("Ticket type does not belong to this event.");
        }
        int quantity = request.getQuantity() == null ? 1 : Math.max(1, request.getQuantity());
        if (!Boolean.TRUE.equals(ticketType.getActive()) || availableTicketInventory(ticketType) < quantity || availableEventCapacity(event) < quantity) {
            throw ApiException.conflict("Ticket inventory is not available.");
        }
        TravelTicket ticket = ticketRepository.save(TravelTicket.builder()
                .event(event)
                .ticketType(ticketType)
                .user(user)
                .qrCode("TRV-QR-" + System.currentTimeMillis() + "-" + user.getId())
                .quantity(quantity)
                .amount(ticketType.getPrice().multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP))
                .status(TravelTicketStatus.ISSUED)
                .build());
        ticketType.setSoldInventory(ticketType.getSoldInventory() + quantity);
        event.setRegisteredCount(event.getRegisteredCount() + quantity);
        ticketTypeRepository.save(ticketType);
        eventRepository.save(event);
        audit(loginId, "TRAVEL_TICKET_PURCHASED", "TravelTicket", ticket.getId(), ticket.getQrCode());
        return mapTicket(ticket);
    }

    @Transactional
    public Map<String, Object> validateTicketQr(String qrCode, String actor) {
        TravelTicket ticket = ticketRepository.findByQrCode(required(qrCode, "QR code is required."))
                .orElseThrow(() -> ApiException.notFound("Travel ticket was not found."));
        if (ticket.getStatus() != TravelTicketStatus.ISSUED) {
            throw ApiException.conflict("Ticket QR is not valid for entry.");
        }
        ticket.setStatus(TravelTicketStatus.USED);
        ticket.setUsedAt(LocalDateTime.now());
        TravelTicket saved = ticketRepository.save(ticket);
        audit(actor, "TRAVEL_TICKET_VALIDATED", "TravelTicket", saved.getId(), saved.getQrCode());
        return mapTicket(saved);
    }

    @Transactional
    public Map<String, Object> createAccommodation(TravelExperienceRequests.AccommodationRequest request, String actor) {
        validateAccommodation(request);
        TravelPartner partner = request.getPartnerId() == null ? null : partnerRepository.findById(request.getPartnerId())
                .orElseThrow(() -> ApiException.notFound("Travel partner was not found."));
        TravelAccommodation accommodation = accommodationRepository.save(TravelAccommodation.builder()
                .partner(partner)
                .name(request.getName().trim())
                .accommodationType(normalize(request.getAccommodationType(), "HOTEL"))
                .city(request.getCity().trim())
                .roomType(normalize(request.getRoomType(), "STANDARD"))
                .pricePerNight(money(request.getPricePerNight()))
                .totalRooms(Math.max(1, request.getTotalRooms()))
                .bookedRooms(0)
                .status(normalize(request.getStatus(), "ACTIVE"))
                .build());
        audit(actor, "TRAVEL_ACCOMMODATION_CREATED", "TravelAccommodation", accommodation.getId(), accommodation.getName());
        return mapAccommodation(accommodation);
    }

    @Transactional
    public Map<String, Object> bookAccommodation(Long accommodationId, TravelExperienceRequests.AccommodationBookingRequest request, String loginId) {
        User user = findUser(loginId);
        validateAccommodationBooking(request);
        TravelAccommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> ApiException.notFound("Accommodation was not found."));
        if (!"ACTIVE".equalsIgnoreCase(accommodation.getStatus())) {
            throw ApiException.conflict("Accommodation is not active.");
        }
        int rooms = Math.max(1, request.getRooms());
        if (availableRooms(accommodation) < rooms) {
            throw ApiException.conflict("Room inventory is not available.");
        }
        long nights = Math.max(1, ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut()));
        TravelAccommodationBooking booking = accommodationBookingRepository.save(TravelAccommodationBooking.builder()
                .accommodation(accommodation)
                .user(user)
                .checkIn(request.getCheckIn())
                .checkOut(request.getCheckOut())
                .rooms(rooms)
                .total(accommodation.getPricePerNight().multiply(BigDecimal.valueOf(rooms)).multiply(BigDecimal.valueOf(nights)).setScale(2, RoundingMode.HALF_UP))
                .status("CONFIRMED")
                .build());
        accommodation.setBookedRooms(accommodation.getBookedRooms() + rooms);
        accommodationRepository.save(accommodation);
        audit(loginId, "TRAVEL_ACCOMMODATION_BOOKED", "TravelAccommodationBooking", booking.getId(), accommodation.getName());
        return mapAccommodationBooking(booking);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> myTravel(String loginId) {
        User user = findUser(loginId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("packageBookings", bookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::mapBooking).toList());
        response.put("tickets", ticketRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::mapTicket).toList());
        response.put("accommodations", accommodationBookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::mapAccommodationBooking).toList());
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        BigDecimal travelRevenue = bookingRepository.totalRevenueExcluding(TravelBookingStatus.CANCELLED);
        BigDecimal ticketRevenue = ticketRepository.ticketRevenueExcluding(TravelTicketStatus.CANCELLED);
        BigDecimal accommodationRevenue = accommodationBookingRepository.accommodationRevenue();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("travelRevenue", travelRevenue);
        response.put("ticketRevenue", ticketRevenue);
        response.put("accommodationRevenue", accommodationRevenue);
        response.put("totalRevenue", travelRevenue.add(ticketRevenue).add(accommodationRevenue).setScale(2, RoundingMode.HALF_UP));
        response.put("confirmedBookings", bookingRepository.countByStatus(TravelBookingStatus.CONFIRMED));
        response.put("issuedTickets", ticketRepository.countByStatus(TravelTicketStatus.ISSUED));
        response.put("usedTickets", ticketRepository.countByStatus(TravelTicketStatus.USED));
        response.put("packages", packageRepository.findTop8ByStatusOrderByStartDateAsc(TravelPackageStatus.PUBLISHED).stream().map(this::mapPackage).toList());
        response.put("events", eventRepository.findTop8ByStatusOrderByStartsAtAsc(TravelEventStatus.PUBLISHED).stream().map(this::mapEvent).toList());
        response.put("recentBookings", bookingRepository.findTop20ByOrderByCreatedAtDesc().stream().map(this::mapBooking).toList());
        response.put("partners", partnerRepository.findAllByOrderByCreatedAtDesc().stream().limit(8).map(this::mapPartner).toList());
        response.put("audit", auditRepository.findTop25ByOrderByCreatedAtDesc().stream().map(this::mapAudit).toList());
        return response;
    }

    private Map<String, Object> mapPartner(TravelPartner partner) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", partner.getId());
        map.put("partnerName", partner.getPartnerName());
        map.put("partnerType", partner.getPartnerType());
        map.put("email", partner.getEmail());
        map.put("phone", partner.getPhone());
        map.put("city", partner.getCity());
        map.put("status", partner.getStatus());
        map.put("commissionPercent", partner.getCommissionPercent());
        map.put("notes", partner.getNotes());
        return map;
    }

    private Map<String, Object> mapPackage(TravelPackage travelPackage) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", travelPackage.getId());
        map.put("packageCode", travelPackage.getPackageCode());
        map.put("title", travelPackage.getTitle());
        map.put("sport", travelPackage.getSport());
        map.put("originCity", travelPackage.getOriginCity());
        map.put("destination", travelPackage.getDestination());
        map.put("packageType", travelPackage.getPackageType());
        map.put("startDate", travelPackage.getStartDate());
        map.put("endDate", travelPackage.getEndDate());
        map.put("cancellationDeadline", travelPackage.getCancellationDeadline());
        map.put("pricePerPerson", travelPackage.getPricePerPerson());
        map.put("capacity", travelPackage.getCapacity());
        map.put("bookedCount", travelPackage.getBookedCount());
        map.put("availableCapacity", availablePackageCapacity(travelPackage));
        map.put("includesAccommodation", travelPackage.getIncludesAccommodation());
        map.put("includesTransport", travelPackage.getIncludesTransport());
        map.put("linkedTournamentId", travelPackage.getLinkedTournamentId());
        map.put("partnerName", travelPackage.getPartner() == null ? null : travelPackage.getPartner().getPartnerName());
        map.put("status", travelPackage.getStatus());
        map.put("itinerary", travelPackage.getItinerary());
        return map;
    }

    private Map<String, Object> mapBooking(TravelBooking booking) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", booking.getId());
        map.put("bookingReference", booking.getBookingReference());
        map.put("packageId", booking.getTravelPackage().getId());
        map.put("packageTitle", booking.getTravelPackage().getTitle());
        map.put("destination", booking.getTravelPackage().getDestination());
        map.put("bookingType", booking.getBookingType());
        map.put("participantCount", booking.getParticipantCount());
        map.put("amount", booking.getAmount());
        map.put("discount", booking.getDiscount());
        map.put("total", booking.getTotal());
        map.put("refundAmount", booking.getRefundAmount());
        map.put("paymentStatus", booking.getPaymentStatus());
        map.put("couponCode", booking.getCouponCode());
        map.put("status", booking.getStatus());
        map.put("travelerNames", booking.getTravelerNames());
        return map;
    }

    private Map<String, Object> mapEvent(TravelEvent event) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", event.getId());
        map.put("title", event.getTitle());
        map.put("eventType", event.getEventType());
        map.put("sport", event.getSport());
        map.put("city", event.getCity());
        map.put("venue", event.getVenue());
        map.put("startsAt", event.getStartsAt());
        map.put("endsAt", event.getEndsAt());
        map.put("capacity", event.getCapacity());
        map.put("registeredCount", event.getRegisteredCount());
        map.put("availableCapacity", availableEventCapacity(event));
        map.put("basePrice", event.getBasePrice());
        map.put("status", event.getStatus());
        map.put("description", event.getDescription());
        map.put("ticketTypes", ticketTypeRepository.findByEventIdOrderByPriceAsc(event.getId()).stream().map(this::mapTicketType).toList());
        return map;
    }

    private Map<String, Object> mapTicketType(TravelTicketType ticketType) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", ticketType.getId());
        map.put("eventId", ticketType.getEvent().getId());
        map.put("name", ticketType.getName());
        map.put("price", ticketType.getPrice());
        map.put("totalInventory", ticketType.getTotalInventory());
        map.put("soldInventory", ticketType.getSoldInventory());
        map.put("availableInventory", availableTicketInventory(ticketType));
        map.put("active", ticketType.getActive());
        return map;
    }

    private Map<String, Object> mapTicket(TravelTicket ticket) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", ticket.getId());
        map.put("eventId", ticket.getEvent().getId());
        map.put("eventTitle", ticket.getEvent().getTitle());
        map.put("ticketType", ticket.getTicketType().getName());
        map.put("qrCode", ticket.getQrCode());
        map.put("quantity", ticket.getQuantity());
        map.put("amount", ticket.getAmount());
        map.put("status", ticket.getStatus());
        map.put("usedAt", ticket.getUsedAt());
        return map;
    }

    private Map<String, Object> mapAccommodation(TravelAccommodation accommodation) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", accommodation.getId());
        map.put("partnerName", accommodation.getPartner() == null ? null : accommodation.getPartner().getPartnerName());
        map.put("name", accommodation.getName());
        map.put("accommodationType", accommodation.getAccommodationType());
        map.put("city", accommodation.getCity());
        map.put("roomType", accommodation.getRoomType());
        map.put("pricePerNight", accommodation.getPricePerNight());
        map.put("totalRooms", accommodation.getTotalRooms());
        map.put("bookedRooms", accommodation.getBookedRooms());
        map.put("availableRooms", availableRooms(accommodation));
        map.put("status", accommodation.getStatus());
        return map;
    }

    private Map<String, Object> mapAccommodationBooking(TravelAccommodationBooking booking) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", booking.getId());
        map.put("accommodationId", booking.getAccommodation().getId());
        map.put("accommodationName", booking.getAccommodation().getName());
        map.put("checkIn", booking.getCheckIn());
        map.put("checkOut", booking.getCheckOut());
        map.put("rooms", booking.getRooms());
        map.put("total", booking.getTotal());
        map.put("status", booking.getStatus());
        return map;
    }

    private Map<String, Object> mapAudit(TravelAudit audit) {
        return Map.of(
                "id", audit.getId(),
                "actor", audit.getActor() == null ? "system" : audit.getActor(),
                "action", audit.getAction(),
                "entityType", audit.getEntityType(),
                "entityId", audit.getEntityId() == null ? 0 : audit.getEntityId(),
                "summary", audit.getSummary() == null ? "" : audit.getSummary()
        );
    }

    private void validatePartner(TravelExperienceRequests.PartnerRequest request) {
        if (request == null || !hasText(request.getPartnerName()) || !hasText(request.getPartnerType()) || !hasText(request.getEmail())) {
            throw ApiException.badRequest("Partner name, type, and email are required.");
        }
        if (!request.getEmail().contains("@")) {
            throw ApiException.badRequest("Partner email is invalid.");
        }
    }

    private void validatePackage(TravelExperienceRequests.PackageRequest request) {
        if (request == null || !hasText(request.getPackageCode()) || !hasText(request.getTitle()) || !hasText(request.getSport()) || !hasText(request.getDestination()) || request.getStartDate() == null || request.getEndDate() == null || request.getPricePerPerson() == null || request.getCapacity() == null) {
            throw ApiException.badRequest("Package code, title, sport, destination, dates, price, and capacity are required.");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw ApiException.badRequest("Package end date cannot be before start date.");
        }
        if (request.getCapacity() <= 0) {
            throw ApiException.badRequest("Package capacity must be greater than zero.");
        }
    }

    private void validateEvent(TravelExperienceRequests.EventRequest request) {
        if (request == null || !hasText(request.getTitle()) || !hasText(request.getSport()) || !hasText(request.getCity()) || request.getStartsAt() == null || request.getEndsAt() == null || request.getCapacity() == null) {
            throw ApiException.badRequest("Event title, sport, city, schedule, and capacity are required.");
        }
        if (request.getEndsAt().isBefore(request.getStartsAt())) {
            throw ApiException.badRequest("Event end time cannot be before start time.");
        }
        if (request.getCapacity() <= 0) {
            throw ApiException.badRequest("Event capacity must be greater than zero.");
        }
    }

    private void validateAccommodation(TravelExperienceRequests.AccommodationRequest request) {
        if (request == null || !hasText(request.getName()) || !hasText(request.getCity()) || !hasText(request.getRoomType()) || request.getPricePerNight() == null || request.getTotalRooms() == null) {
            throw ApiException.badRequest("Accommodation name, city, room type, price, and inventory are required.");
        }
        if (request.getTotalRooms() <= 0) {
            throw ApiException.badRequest("Room inventory must be greater than zero.");
        }
    }

    private void validateAccommodationBooking(TravelExperienceRequests.AccommodationBookingRequest request) {
        if (request == null || request.getCheckIn() == null || request.getCheckOut() == null || request.getRooms() == null) {
            throw ApiException.badRequest("Check-in, check-out, and room count are required.");
        }
        if (!request.getCheckOut().isAfter(request.getCheckIn())) {
            throw ApiException.badRequest("Check-out must be after check-in.");
        }
    }

    private int availablePackageCapacity(TravelPackage travelPackage) {
        return Math.max(0, travelPackage.getCapacity() - travelPackage.getBookedCount());
    }

    private int availableEventCapacity(TravelEvent event) {
        return Math.max(0, event.getCapacity() - event.getRegisteredCount());
    }

    private int availableTicketInventory(TravelTicketType ticketType) {
        return Math.max(0, ticketType.getTotalInventory() - ticketType.getSoldInventory());
    }

    private int availableRooms(TravelAccommodation accommodation) {
        return Math.max(0, accommodation.getTotalRooms() - accommodation.getBookedRooms());
    }

    private BigDecimal travelDiscount(String couponCode, BigDecimal amount) {
        if (couponCode == null) {
            return ZERO;
        }
        if ("TRAVEL10".equalsIgnoreCase(couponCode)) {
            return amount.multiply(BigDecimal.valueOf(0.10)).min(BigDecimal.valueOf(1000)).setScale(2, RoundingMode.HALF_UP);
        }
        if ("CAMP5".equalsIgnoreCase(couponCode)) {
            return amount.multiply(BigDecimal.valueOf(0.05)).setScale(2, RoundingMode.HALF_UP);
        }
        return ZERO;
    }

    private Long requiredId(Long value, String message) {
        if (value == null) {
            throw ApiException.badRequest(message);
        }
        return value;
    }

    private String required(String value, String message) {
        if (!hasText(value)) {
            throw ApiException.badRequest(message);
        }
        return value.trim();
    }

    private User findUser(String loginId) {
        if (!hasText(loginId)) {
            throw ApiException.unauthorized("User account is required.");
        }
        return userRepository.findByEmailOrPhone(loginId)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
    }

    private void audit(String actor, String action, String entityType, Long entityId, String summary) {
        auditRepository.save(TravelAudit.builder()
                .actor(clean(actor))
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .summary(clean(summary))
                .build());
    }

    private BigDecimal money(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw ApiException.badRequest("Amount cannot be negative.");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal moneyOrZero(BigDecimal amount) {
        if (amount == null) {
            return ZERO;
        }
        return money(amount);
    }

    private String normalize(String value, String fallback) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
    }

    private String clean(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
