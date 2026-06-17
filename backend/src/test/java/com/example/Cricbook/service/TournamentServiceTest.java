package com.example.Cricbook.service;

import com.example.Cricbook.dto.CreateTournamentRequest;
import com.example.Cricbook.dto.FixtureResultRequest;
import com.example.Cricbook.dto.TournamentFixtureResponse;
import com.example.Cricbook.dto.TournamentResponse;
import com.example.Cricbook.dto.TournamentStandingResponse;
import com.example.Cricbook.dto.UpdateTournamentStatusRequest;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.FixtureStatus;
import com.example.Cricbook.enums.TournamentFormat;
import com.example.Cricbook.enums.TournamentRegistrationStatus;
import com.example.Cricbook.enums.TournamentStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TurfRepository turfRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private TournamentRepository tournamentRepository;
    @Mock private TournamentRegistrationRepository registrationRepository;
    @Mock private TournamentFixtureRepository fixtureRepository;
    @Mock private TournamentStandingRepository standingRepository;
    @Mock private TournamentRewardRepository rewardRepository;
    @Mock private FeedEventRepository feedEventRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private TournamentService tournamentService;

    @Test
    void createTournamentRejectsEndDateBeforeStartDate() {
        User organizer = User.builder().id(1L).email("admin@test.com").name("Owner").build();
        CreateTournamentRequest request = createTournamentRequest();
        request.setEndDate(request.getStartDate().minusDays(1));

        when(userRepository.findByEmailOrPhone("admin@test.com")).thenReturn(Optional.of(organizer));

        ApiException exception = assertThrows(ApiException.class, () -> tournamentService.createTournament("admin@test.com", request));

        assertEquals("End date cannot be before start date", exception.getMessage());
    }

    @Test
    void createTournamentRejectsDuplicateNameInCity() {
        User organizer = User.builder().id(1L).email("admin@test.com").name("Owner").build();
        CreateTournamentRequest request = createTournamentRequest();

        when(userRepository.findByEmailOrPhone("admin@test.com")).thenReturn(Optional.of(organizer));
        when(tournamentRepository.existsByNameIgnoreCaseAndCityIgnoreCase("City Cup", "Surat")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> tournamentService.createTournament("admin@test.com", request));

        assertEquals("A tournament with this name already exists in this city", exception.getMessage());
    }

    @Test
    void updateTournamentStatusClosesRegistration() {
        User organizer = User.builder().id(1L).email("admin@test.com").name("Owner").build();
        Tournament tournament = Tournament.builder()
                .id(5L)
                .organizer(organizer)
                .name("City Cup")
                .sport("Cricket")
                .city("Surat")
                .venue("Central Turf")
                .format(TournamentFormat.LEAGUE)
                .status(TournamentStatus.REGISTRATION_OPEN)
                .maxTeams(4)
                .entryFee(BigDecimal.ZERO)
                .startDate(LocalDate.now())
                .build();
        UpdateTournamentStatusRequest request = new UpdateTournamentStatusRequest();
        request.setStatus(TournamentStatus.REGISTRATION_CLOSED);

        when(userRepository.findByEmailOrPhone("admin@test.com")).thenReturn(Optional.of(organizer));
        when(tournamentRepository.findById(5L)).thenReturn(Optional.of(tournament));
        when(tournamentRepository.save(tournament)).thenReturn(tournament);
        when(registrationRepository.countByTournamentIdAndStatus(5L, TournamentRegistrationStatus.REGISTERED)).thenReturn(0L);

        TournamentResponse response = tournamentService.updateTournamentStatus("admin@test.com", 5L, request);

        assertEquals(TournamentStatus.REGISTRATION_CLOSED, response.getStatus());
    }

    @Test
    void generateLeagueFixturesCreatesRoundRobinMatches() {
        User organizer = User.builder().id(1L).email("admin@test.com").build();
        Tournament tournament = Tournament.builder()
                .id(5L)
                .organizer(organizer)
                .name("City Cup")
                .sport("Cricket")
                .city("Surat")
                .venue("Central Turf")
                .format(TournamentFormat.LEAGUE)
                .status(TournamentStatus.REGISTRATION_OPEN)
                .maxTeams(4)
                .entryFee(BigDecimal.ZERO)
                .startDate(LocalDate.now())
                .build();
        Team a = team(1L, "A", organizer);
        Team b = team(2L, "B", organizer);
        Team c = team(3L, "C", organizer);

        when(userRepository.findByEmailOrPhone("admin@test.com")).thenReturn(Optional.of(organizer));
        when(tournamentRepository.findById(5L)).thenReturn(Optional.of(tournament));
        when(fixtureRepository.existsByTournamentId(5L)).thenReturn(false);
        when(registrationRepository.findByTournamentIdOrderByCreatedAtAsc(5L)).thenReturn(List.of(
                registration(tournament, a),
                registration(tournament, b),
                registration(tournament, c)
        ));
        when(tournamentRepository.save(tournament)).thenReturn(tournament);
        when(fixtureRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<TournamentFixtureResponse> fixtures = tournamentService.generateFixtures("admin@test.com", 5L);

        assertEquals(3, fixtures.size());
        assertEquals("A", fixtures.get(0).getTeamAName());
        assertEquals("B", fixtures.get(0).getTeamBName());
    }

    @Test
    void recordFixtureResultAdvancesKnockoutBracket() {
        User organizer = User.builder().id(1L).email("admin@test.com").name("Owner").build();
        Tournament tournament = Tournament.builder()
                .id(8L)
                .organizer(organizer)
                .name("Knockout Cup")
                .sport("Cricket")
                .city("Surat")
                .venue("Arena")
                .format(TournamentFormat.KNOCKOUT)
                .status(TournamentStatus.ONGOING)
                .maxTeams(4)
                .entryFee(BigDecimal.ZERO)
                .startDate(LocalDate.now())
                .build();
        Team a = team(1L, "A", organizer);
        Team b = team(2L, "B", organizer);
        Team c = team(3L, "C", organizer);
        Team d = team(4L, "D", organizer);

        TournamentFixture firstSemi = TournamentFixture.builder()
                .id(10L)
                .tournament(tournament)
                .teamA(a)
                .teamB(b)
                .round(1)
                .matchDate(LocalDate.now())
                .status(FixtureStatus.SCHEDULED)
                .build();
        TournamentFixture secondSemi = TournamentFixture.builder()
                .id(11L)
                .tournament(tournament)
                .teamA(c)
                .teamB(d)
                .round(1)
                .matchDate(LocalDate.now().plusDays(1))
                .winner(c)
                .status(FixtureStatus.COMPLETED)
                .build();

        when(userRepository.findByEmailOrPhone("admin@test.com")).thenReturn(Optional.of(organizer));
        when(fixtureRepository.findById(10L)).thenReturn(Optional.of(firstSemi));
        when(standingRepository.findByTournamentIdAndTeamId(tournament.getId(), a.getId())).thenReturn(Optional.empty());
        when(standingRepository.findByTournamentIdAndTeamId(tournament.getId(), b.getId())).thenReturn(Optional.empty());
        when(standingRepository.save(any(TournamentStanding.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fixtureRepository.save(firstSemi)).thenAnswer(invocation -> invocation.getArgument(0));
        when(fixtureRepository.findByTournamentIdAndRoundOrderByMatchDateAscStartTimeAscIdAsc(8L, 1)).thenReturn(List.of(firstSemi, secondSemi));
        when(fixtureRepository.countByTournamentIdAndRound(8L, 2)).thenReturn(0L);
        when(fixtureRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        TournamentFixtureResponse response = tournamentService.recordFixtureResult(
                "admin@test.com",
                10L,
                fixtureResultRequest(120, 100)
        );

        assertEquals("A", response.getWinnerTeamName());
        verify(fixtureRepository).saveAll(anyList());
        verify(tournamentRepository, times(0)).save(any(Tournament.class));
    }

    @Test
    void recordLeagueDrawAwardsBothTeamsOnePoint() {
        User organizer = User.builder().id(1L).email("admin@test.com").name("Owner").build();
        Tournament tournament = Tournament.builder()
                .id(12L)
                .organizer(organizer)
                .name("League Cup")
                .sport("Cricket")
                .city("Surat")
                .venue("Arena")
                .format(TournamentFormat.LEAGUE)
                .status(TournamentStatus.ONGOING)
                .maxTeams(4)
                .entryFee(BigDecimal.ZERO)
                .startDate(LocalDate.now())
                .build();
        Team a = team(1L, "A", organizer);
        Team b = team(2L, "B", organizer);
        TournamentFixture fixture = TournamentFixture.builder()
                .id(20L)
                .tournament(tournament)
                .teamA(a)
                .teamB(b)
                .round(1)
                .matchDate(LocalDate.now())
                .status(FixtureStatus.SCHEDULED)
                .build();
        TournamentStanding standingA = standing(tournament, a);
        TournamentStanding standingB = standing(tournament, b);

        when(userRepository.findByEmailOrPhone("admin@test.com")).thenReturn(Optional.of(organizer));
        when(fixtureRepository.findById(20L)).thenReturn(Optional.of(fixture));
        when(standingRepository.findByTournamentIdAndTeamId(12L, 1L)).thenReturn(Optional.of(standingA));
        when(standingRepository.findByTournamentIdAndTeamId(12L, 2L)).thenReturn(Optional.of(standingB));
        when(fixtureRepository.save(fixture)).thenAnswer(invocation -> invocation.getArgument(0));

        TournamentFixtureResponse response = tournamentService.recordFixtureResult("admin@test.com", 20L, fixtureResultRequest(140, 140));

        assertEquals(null, response.getWinnerTeamName());
        assertEquals(1, standingA.getDraws());
        assertEquals(1, standingB.getDraws());
        assertEquals(1, standingA.getPoints());
        assertEquals(1, standingB.getPoints());
    }

    @Test
    void getStandingsReturnsDrawsAndNetRunRateTieBreakerData() {
        Tournament tournament = Tournament.builder()
                .id(15L)
                .organizer(User.builder().id(1L).email("admin@test.com").build())
                .name("League Cup")
                .sport("Cricket")
                .city("Surat")
                .venue("Arena")
                .format(TournamentFormat.LEAGUE)
                .status(TournamentStatus.ONGOING)
                .maxTeams(4)
                .entryFee(BigDecimal.ZERO)
                .startDate(LocalDate.now())
                .build();
        Team a = team(1L, "A", tournament.getOrganizer());
        Team b = team(2L, "B", tournament.getOrganizer());
        TournamentStanding standingA = TournamentStanding.builder()
                .tournament(tournament)
                .team(a)
                .played(2)
                .wins(1)
                .losses(0)
                .draws(1)
                .points(3)
                .runsFor(260)
                .runsAgainst(220)
                .build();
        TournamentStanding standingB = TournamentStanding.builder()
                .tournament(tournament)
                .team(b)
                .played(2)
                .wins(1)
                .losses(1)
                .draws(0)
                .points(2)
                .runsFor(230)
                .runsAgainst(240)
                .build();

        when(tournamentRepository.findById(15L)).thenReturn(Optional.of(tournament));
        when(standingRepository.findByTournamentIdOrderByPointsDescWinsDescRunsForDesc(15L)).thenReturn(List.of(standingB, standingA));

        List<TournamentStandingResponse> standings = tournamentService.getStandings(15L);

        assertEquals("A", standings.get(0).getTeamName());
        assertEquals(1, standings.get(0).getDraws());
        assertEquals(20D, standings.get(0).getNetRunRate());
    }

    private FixtureResultRequest fixtureResultRequest(int teamAScore, int teamBScore) {
        FixtureResultRequest request = new FixtureResultRequest();
        request.setTeamAScore(teamAScore);
        request.setTeamBScore(teamBScore);
        return request;
    }

    private CreateTournamentRequest createTournamentRequest() {
        CreateTournamentRequest request = new CreateTournamentRequest();
        request.setName("City Cup");
        request.setCity("Surat");
        request.setVenue("Central Turf");
        request.setFormat(TournamentFormat.LEAGUE);
        request.setMaxTeams(4);
        request.setEntryFee(BigDecimal.ZERO);
        request.setStartDate(LocalDate.now().plusDays(7));
        request.setEndDate(LocalDate.now().plusDays(10));
        return request;
    }

    private Team team(Long id, String name, User captain) {
        return Team.builder().id(id).name(name).sport("Cricket").city("Surat").captain(captain).maxMembers(11).build();
    }

    private TournamentRegistration registration(Tournament tournament, Team team) {
        return TournamentRegistration.builder()
                .tournament(tournament)
                .team(team)
                .registeredBy(team.getCaptain())
                .status(TournamentRegistrationStatus.REGISTERED)
                .build();
    }

    private TournamentStanding standing(Tournament tournament, Team team) {
        return TournamentStanding.builder()
                .tournament(tournament)
                .team(team)
                .played(0)
                .wins(0)
                .losses(0)
                .draws(0)
                .points(0)
                .runsFor(0)
                .runsAgainst(0)
                .build();
    }
}
