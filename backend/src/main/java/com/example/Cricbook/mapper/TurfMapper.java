package com.example.Cricbook.mapper;

import com.example.Cricbook.dto.SlotAvailabilityResponse;
import com.example.Cricbook.dto.SlotResponse;
import com.example.Cricbook.dto.TurfResponse;
import com.example.Cricbook.entity.Slot;
import com.example.Cricbook.entity.SlotAvailability;
import com.example.Cricbook.entity.Turf;
import com.example.Cricbook.entity.TurfAmenity;
import com.example.Cricbook.entity.TurfImage;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class TurfMapper {

    public TurfResponse toTurfResponse(Turf turf) {
        if (turf == null) {
            return null;
        }

        TurfResponse response = new TurfResponse();
        response.setId(turf.getId());
        response.setName(turf.getName());
        response.setDescription(turf.getDescription());
        response.setLocation(turf.getLocation());
        response.setCity(turf.getCity());
        response.setCountry(turf.getCountry());
        response.setState(turf.getState());
        response.setTimezone(turf.getTimezone());
        response.setHolidaySource(turf.getHolidaySource());
        response.setCapacity(turf.getCapacity());
        response.setLatitude(turf.getLatitude());
        response.setLongitude(turf.getLongitude());
        response.setPricePerHour(turf.getPricePerHour());
        response.setPitchType(turf.getPitchType());
        response.setBoxType(turf.getBoxType());
        response.setStatus(turf.getStatus());
        response.setRating(turf.getRating());
        response.setTotalReviews(turf.getTotalReviews());
        response.setAmenities(turf.getAmenities() != null ? turf.getAmenities().stream().map(TurfAmenity::getAmenity).collect(Collectors.toList()) : null);
        response.setImages(turf.getImages() != null ? turf.getImages().stream().map(TurfImage::getImagePath).collect(Collectors.toList()) : null);
        return response;
    }

    public SlotResponse toSlotResponse(Slot slot) {
        if (slot == null) {
            return null;
        }

        return SlotResponse.builder()
                .id(slot.getId())
                .turfId(slot.getTurf() != null ? slot.getTurf().getId() : null)
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .price(slot.getPrice())
                .isActive(slot.getIsActive())
                .build();
    }

    public SlotAvailabilityResponse toSlotAvailabilityResponse(SlotAvailability slotAvailability) {
        if (slotAvailability == null) {
            return null;
        }

        return SlotAvailabilityResponse.builder()
                .id(slotAvailability.getId())
                .slot(toSlotResponse(slotAvailability.getSlot()))
                .slotId(slotAvailability.getSlot() != null ? slotAvailability.getSlot().getId() : null)
                .startTime(slotAvailability.getSlot() != null ? slotAvailability.getSlot().getStartTime() : null)
                .endTime(slotAvailability.getSlot() != null ? slotAvailability.getSlot().getEndTime() : null)
                .price(slotAvailability.getSlot() != null ? slotAvailability.getSlot().getPrice() : null)
                .date(slotAvailability.getDate())
                .isAvailable(slotAvailability.getIsAvailable())
                .isBlocked(slotAvailability.getIsBlocked())
                .available(slotAvailability.getIsAvailable())
                .blocked(slotAvailability.getIsBlocked())
                .build();
    }
}
