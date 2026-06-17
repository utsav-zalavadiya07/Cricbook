package com.example.Cricbook.service;

import com.example.Cricbook.entity.Sport;
import com.example.Cricbook.entity.SportTurf;
import com.example.Cricbook.repository.AuditLogRepository;
import com.example.Cricbook.repository.BookingRepository;
import com.example.Cricbook.repository.PublicMatchRepository;
import com.example.Cricbook.repository.SportConfigurationAuditRepository;
import com.example.Cricbook.repository.SportRepository;
import com.example.Cricbook.repository.SportTurfRepository;
import com.example.Cricbook.repository.TeamRepository;
import com.example.Cricbook.repository.TournamentRepository;
import com.example.Cricbook.repository.TurfRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultiSportServiceTest {

    @Mock private SportRepository sportRepository;
    @Mock private SportTurfRepository sportTurfRepository;
    @Mock private SportConfigurationAuditRepository sportConfigurationAuditRepository;
    @Mock private TurfRepository turfRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private TournamentRepository tournamentRepository;
    @Mock private PublicMatchRepository publicMatchRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks
    private MultiSportService multiSportService;

    @Test
    void turfSupportByTurfIdUsesOneMappingLoadForManyTurfs() {
        Sport football = sport(7L, "Football", "FOOTBALL", true, 2);
        when(sportRepository.findByCodeIgnoreCase("FOOTBALL")).thenReturn(Optional.of(football));
        when(sportTurfRepository.findByTurfIdIn(List.of(1L, 2L, 3L))).thenReturn(List.of(
                SportTurf.builder().sportId(7L).turfId(1L).build(),
                SportTurf.builder().sportId(9L).turfId(2L).build()
        ));

        Map<Long, Boolean> support = multiSportService.turfSupportByTurfId(List.of(1L, 2L, 3L), "football");

        assertEquals(true, support.get(1L));
        assertEquals(false, support.get(2L));
        assertEquals(false, support.get(3L));
    }

    @Test
    void activeSportCodesByTurfIdReturnsMappedCodesAndLegacyCricketFallback() {
        Sport cricket = sport(1L, "Cricket", "CRICKET", true, 1);
        Sport football = sport(7L, "Football", "FOOTBALL", true, 2);
        when(sportRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc()).thenReturn(List.of(cricket, football));
        when(sportTurfRepository.findByTurfIdIn(List.of(10L, 11L))).thenReturn(List.of(
                SportTurf.builder().sportId(7L).turfId(10L).build()
        ));

        Map<Long, List<String>> codes = multiSportService.activeSportCodesByTurfId(List.of(10L, 11L));

        assertEquals(List.of("FOOTBALL"), codes.get(10L));
        assertEquals(List.of("CRICKET"), codes.get(11L));
    }

    private Sport sport(Long id, String name, String code, boolean active, int displayOrder) {
        return Sport.builder()
                .id(id)
                .name(name)
                .code(code)
                .icon(code.substring(0, 2))
                .isActive(active)
                .displayOrder(displayOrder)
                .build();
    }
}
