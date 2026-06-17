package com.example.Cricbook.service;

import com.example.Cricbook.dto.OwnerDashboardResponse;
import com.example.Cricbook.dto.TurfResponse;
import com.example.Cricbook.entity.Turf;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.enums.TurfStatus;
import com.example.Cricbook.mapper.TurfMapper;
import com.example.Cricbook.repository.BookingRepository;
import com.example.Cricbook.repository.TurfRepository;
import com.example.Cricbook.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerDashboardServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TurfRepository turfRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ReviewService reviewService;
    @Mock
    private BookingService bookingService;
    @Mock
    private TurfMapper turfMapper;

    @InjectMocks
    private OwnerDashboardService ownerDashboardService;

    @Test
    void getDashboardIncludesInactiveTurfOperationsCount() {
        String username = "owner@example.com";
        User owner = User.builder().id(9L).email(username).build();
        Turf activeTurf = turf(1L, "Active Turf", TurfStatus.ACTIVE, 9);
        Turf inactiveTurf = turf(2L, "Inactive Turf", TurfStatus.INACTIVE, 3);
        Turf maintenanceTurf = turf(3L, "Maintenance Turf", TurfStatus.MAINTENANCE, 1);

        when(userRepository.findByEmailOrPhone(username)).thenReturn(Optional.of(owner));
        when(turfRepository.findByOwnerId(owner.getId())).thenReturn(List.of(activeTurf, inactiveTurf, maintenanceTurf));
        when(bookingRepository.findByTurfOwnerId(owner.getId())).thenReturn(List.of());
        when(turfMapper.toTurfResponse(activeTurf)).thenReturn(TurfResponse.builder().id(activeTurf.getId()).name(activeTurf.getName()).build());
        when(bookingService.getOwnerBookings(BookingStatus.CONFIRMED)).thenReturn(List.of());
        when(reviewService.getOwnerReviews(username)).thenReturn(List.of());

        OwnerDashboardResponse response = ownerDashboardService.getDashboard(username);

        assertEquals(1L, response.getActiveTurfs());
        assertEquals(1L, response.getInactiveTurfs());
        assertEquals(1L, response.getMaintenanceTurfs());
    }

    private Turf turf(Long id, String name, TurfStatus status, int totalReviews) {
        return Turf.builder()
                .id(id)
                .name(name)
                .status(status)
                .totalReviews(totalReviews)
                .build();
    }
}
