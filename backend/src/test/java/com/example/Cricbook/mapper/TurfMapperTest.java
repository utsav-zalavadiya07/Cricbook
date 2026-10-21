package com.example.Cricbook.mapper;

import com.example.Cricbook.dto.TurfResponse;
import com.example.Cricbook.entity.Turf;
import com.example.Cricbook.entity.TurfAmenity;
import com.example.Cricbook.entity.TurfImage;
import com.example.Cricbook.enums.BoxType;
import com.example.Cricbook.enums.PitchType;
import com.example.Cricbook.enums.TurfStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TurfMapperTest {

    private final TurfMapper turfMapper = new TurfMapper();

    @Test
    void mapsOwnerCardDisplayFields() {
        Turf turf = Turf.builder()
                .id(7L)
                .name("Riverfront Arena")
                .description("Premium indoor cricket venue")
                .location("River Road")
                .city("Ahmedabad")
                .capacity(12)
                .pricePerHour(BigDecimal.valueOf(1500))
                .pitchType(PitchType.TURF)
                .boxType(BoxType.SIX_V_SIX)
                .status(TurfStatus.ACTIVE)
                .rating(4.7)
                .totalReviews(32)
                .amenities(List.of(TurfAmenity.builder().amenity("Parking").build()))
                .images(List.of(TurfImage.builder().imagePath("/uploads/turf/7/cover.webp").build()))
                .build();

        TurfResponse response = turfMapper.toTurfResponse(turf);

        assertEquals(7L, response.getId());
        assertEquals("Riverfront Arena", response.getName());
        assertEquals("Ahmedabad", response.getCity());
        assertEquals("River Road", response.getLocation());
        assertEquals(12, response.getCapacity());
        assertEquals(BigDecimal.valueOf(1500), response.getPricePerHour());
        assertEquals(PitchType.TURF, response.getPitchType());
        assertEquals(BoxType.SIX_V_SIX, response.getBoxType());
        assertEquals(TurfStatus.ACTIVE, response.getStatus());
        assertEquals(4.7, response.getRating());
        assertEquals(32, response.getTotalReviews());
        assertEquals(List.of("Parking"), response.getAmenities());
        assertEquals(List.of("/uploads/turf/7/cover.webp"), response.getImages());
    }
}
