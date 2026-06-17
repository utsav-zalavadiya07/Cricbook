package com.example.Cricbook.entity;

import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceIndexTest {

    @Test
    void turfHasCompositeSearchIndexes() {
        Set<String> indexes = indexNames(Turf.class);

        assertTrue(indexes.contains("idx_turf_status_city"));
        assertTrue(indexes.contains("idx_turf_status_price"));
        assertTrue(indexes.contains("idx_turf_status_rating"));
    }

    @Test
    void bookingHasOperationalIndexes() {
        Set<String> indexes = indexNames(Booking.class);

        assertTrue(indexes.contains("idx_booking_user_date"));
        assertTrue(indexes.contains("idx_booking_turf_date_status"));
        assertTrue(indexes.contains("idx_booking_status_expiry"));
    }

    @Test
    void slotAvailabilityHasBlockedAvailabilityIndex() {
        Set<String> indexes = indexNames(SlotAvailability.class);

        assertTrue(indexes.contains("idx_sa_date_avail_blocked"));
    }

    private Set<String> indexNames(Class<?> entityClass) {
        return Arrays.stream(entityClass.getAnnotation(Table.class).indexes())
                .map(Index::name)
                .collect(Collectors.toSet());
    }
}
