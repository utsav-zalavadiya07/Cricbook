package com.example.Cricbook.dto;

import com.example.Cricbook.enums.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class TravelExperienceRequests {

    private TravelExperienceRequests() {
    }

    @Data
    public static class PartnerRequest {
        private String partnerName;
        private String partnerType;
        private String email;
        private String phone;
        private String city;
        private BigDecimal commissionPercent;
        private String notes;
    }

    @Data
    public static class PartnerStatusRequest {
        private TravelPartnerStatus status;
        private String notes;
    }

    @Data
    public static class PackageRequest {
        private String packageCode;
        private String title;
        private String sport;
        private String originCity;
        private String destination;
        private String packageType;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDate cancellationDeadline;
        private BigDecimal pricePerPerson;
        private Integer capacity;
        private Boolean includesAccommodation;
        private Boolean includesTransport;
        private Long linkedTournamentId;
        private Long partnerId;
        private TravelPackageStatus status;
        private String itinerary;
    }

    @Data
    public static class PackageBookingRequest {
        private String bookingType;
        private Integer participantCount;
        private String travelerNames;
        private String couponCode;
        private String paymentReference;
    }

    @Data
    public static class EventRequest {
        private String title;
        private TravelEventType eventType;
        private String sport;
        private String city;
        private String venue;
        private LocalDateTime startsAt;
        private LocalDateTime endsAt;
        private Integer capacity;
        private BigDecimal basePrice;
        private TravelEventStatus status;
        private String description;
    }

    @Data
    public static class TicketTypeRequest {
        private String name;
        private BigDecimal price;
        private Integer totalInventory;
        private Boolean active;
    }

    @Data
    public static class TicketPurchaseRequest {
        private Long ticketTypeId;
        private Integer quantity;
    }

    @Data
    public static class AccommodationRequest {
        private Long partnerId;
        private String name;
        private String accommodationType;
        private String city;
        private String roomType;
        private BigDecimal pricePerNight;
        private Integer totalRooms;
        private String status;
    }

    @Data
    public static class AccommodationBookingRequest {
        private LocalDate checkIn;
        private LocalDate checkOut;
        private Integer rooms;
    }
}
