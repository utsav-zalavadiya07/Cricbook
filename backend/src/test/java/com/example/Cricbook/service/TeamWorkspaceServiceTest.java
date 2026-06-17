package com.example.Cricbook.service;

import com.example.Cricbook.dto.*;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.*;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamWorkspaceServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private PublicMatchRepository publicMatchRepository;
    @Mock private TeamWorkspaceRepository workspaceRepository;
    @Mock private TeamAvailabilityRepository availabilityRepository;
    @Mock private TeamLineupRepository lineupRepository;
    @Mock private TeamPollRepository pollRepository;
    @Mock private TeamPollVoteRepository voteRepository;
    @Mock private TeamAnnouncementRepository announcementRepository;
    @Mock private TeamFinanceRepository financeRepository;
    @Mock private TeamAttendanceRepository attendanceRepository;
    @Mock private NotificationService notificationService;
    @InjectMocks private TeamWorkspaceService service;

    @Test
    void setAvailabilityUpsertsMemberResponse() {
        User player = user(1L, "Player");
        Team team = team(player);
        TeamAvailabilityRequest request = new TeamAvailabilityRequest();
        request.setDate(LocalDate.now().plusDays(1));
        request.setStatus(TeamAvailabilityStatus.AVAILABLE);
        request.setNote("Ready");

        commonAccess(player, team, TeamMemberRole.PLAYER);
        when(availabilityRepository.findByTeamIdAndUserIdAndDate(10L, 1L, request.getDate())).thenReturn(Optional.empty());
        when(availabilityRepository.save(any(TeamAvailability.class))).thenAnswer(invocation -> {
            TeamAvailability item = invocation.getArgument(0);
            item.setId(50L);
            return item;
        });

        TeamAvailabilityResponse result = service.setAvailability("player@test.com", 10L, request);

        assertEquals(50L, result.getId());
        assertEquals(TeamAvailabilityStatus.AVAILABLE, result.getStatus());
        assertEquals("Ready", result.getNote());
    }

    @Test
    void lineupRequiresLeadershipRole() {
        User player = user(1L, "Player");
        Team team = team(player);
        TeamLineupRequest request = new TeamLineupRequest();
        request.setUserId(1L);
        request.setPosition(1);

        commonAccess(player, team, TeamMemberRole.PLAYER);

        ApiException error = assertThrows(ApiException.class, () -> service.saveLineup("player@test.com", 10L, request));

        assertEquals("Only team leadership can manage this workspace.", error.getMessage());
    }

    @Test
    void captainCanCreatePollAndMemberCanVote() {
        User captain = user(1L, "Captain");
        Team team = team(captain);
        TeamPollRequest pollRequest = new TeamPollRequest();
        pollRequest.setQuestion("Practice day?");
        pollRequest.setOptions(List.of("Friday", "Saturday"));

        commonAccess(captain, team, TeamMemberRole.CAPTAIN);
        when(pollRepository.save(any(TeamPoll.class))).thenAnswer(invocation -> {
            TeamPoll poll = invocation.getArgument(0);
            poll.setId(60L);
            return poll;
        });
        when(voteRepository.findByPollId(60L)).thenReturn(List.of());

        TeamPollResponse poll = service.createPoll("captain@test.com", 10L, pollRequest);

        assertEquals(60L, poll.getId());
        assertEquals(List.of("Friday", "Saturday"), poll.getOptions());

        TeamVoteRequest voteRequest = new TeamVoteRequest();
        voteRequest.setSelectedOption("Friday");
        TeamPoll pollEntity = TeamPoll.builder().id(60L).team(team).createdBy(captain).question("Practice day?").optionsCsv("Friday|Saturday").build();
        when(pollRepository.findById(60L)).thenReturn(Optional.of(pollEntity));
        when(voteRepository.findByPollId(60L)).thenReturn(List.of(TeamPollVote.builder().poll(pollEntity).user(captain).selectedOption("Friday").build()));
        when(voteRepository.findByPollIdAndUserId(60L, 1L)).thenReturn(Optional.empty());
        when(voteRepository.save(any(TeamPollVote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeamPollResponse voted = service.vote("captain@test.com", 10L, 60L, voteRequest);

        assertEquals(1L, voted.getVoteCounts().get("Friday"));
    }

    @Test
    void workspaceAnalyticsCalculatesBalanceAndAttendanceRate() {
        User captain = user(1L, "Captain");
        Team team = team(captain);
        commonAccess(captain, team, TeamMemberRole.CAPTAIN);
        when(teamMemberRepository.findByUserIdAndStatusOrderByCreatedAtDesc(1L, TeamMemberStatus.ACTIVE)).thenReturn(List.of(member(captain, team, TeamMemberRole.CAPTAIN)));
        when(financeRepository.sumByTeamIdAndType(10L, TeamFinanceType.CONTRIBUTION)).thenReturn(BigDecimal.valueOf(1000));
        when(financeRepository.sumByTeamIdAndType(10L, TeamFinanceType.REFUND)).thenReturn(BigDecimal.ZERO);
        when(financeRepository.sumByTeamIdAndType(10L, TeamFinanceType.EXPENSE)).thenReturn(BigDecimal.valueOf(250));
        when(teamMemberRepository.countByTeamIdAndStatus(10L, TeamMemberStatus.ACTIVE)).thenReturn(3L);
        when(availabilityRepository.findByTeamIdAndDateBetweenOrderByDateAscCreatedAtAsc(eq(10L), any(), any())).thenReturn(List.of());
        when(pollRepository.findByTeamIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());
        when(attendanceRepository.countByTeamId(10L)).thenReturn(4L);
        when(attendanceRepository.countByTeamIdAndStatus(10L, TeamAttendanceStatus.PRESENT)).thenReturn(3L);
        when(attendanceRepository.countByTeamIdAndStatus(10L, TeamAttendanceStatus.LATE)).thenReturn(1L);
        stubWorkspaceMapping(team);

        TeamAnalyticsResponse result = service.analytics("captain@test.com");

        assertEquals(1L, result.getTotalTeams());
        assertEquals(0, BigDecimal.valueOf(750).compareTo(result.getTeamWalletBalance()));
        assertEquals(100, result.getAverageAttendanceRate());
    }

    @Test
    void workspaceAnalyticsDoesNotCreateMissingWorkspaceDuringReadOnlySummary() {
        User captain = user(1L, "Captain");
        Team team = team(captain);
        commonAccess(captain, team, TeamMemberRole.CAPTAIN);
        when(teamMemberRepository.findByUserIdAndStatusOrderByCreatedAtDesc(1L, TeamMemberStatus.ACTIVE)).thenReturn(List.of(member(captain, team, TeamMemberRole.CAPTAIN)));
        when(financeRepository.sumByTeamIdAndType(eq(10L), any())).thenReturn(BigDecimal.ZERO);
        when(teamMemberRepository.countByTeamIdAndStatus(10L, TeamMemberStatus.ACTIVE)).thenReturn(1L);
        when(availabilityRepository.findByTeamIdAndDateBetweenOrderByDateAscCreatedAtAsc(eq(10L), any(), any())).thenReturn(List.of());
        when(pollRepository.findByTeamIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());
        when(workspaceRepository.findByTeamId(10L)).thenReturn(Optional.empty());
        when(availabilityRepository.countByTeamIdAndDateAndStatus(eq(10L), any(), any())).thenReturn(0L);

        TeamAnalyticsResponse result = service.analytics("captain@test.com");

        assertEquals(1, result.getMostActiveTeams().size());
        assertEquals("PUBLIC", result.getMostActiveTeams().get(0).getVisibility());
        verify(workspaceRepository, never()).save(any(TeamWorkspace.class));
    }

    private void stubWorkspaceMapping(Team team) {
        when(workspaceRepository.findByTeamId(10L)).thenReturn(Optional.of(TeamWorkspace.builder().team(team).visibility("PUBLIC").teamType("Casual").build()));
        when(availabilityRepository.countByTeamIdAndDateAndStatus(eq(10L), any(), any())).thenReturn(0L);
    }

    private void commonAccess(User user, Team team, TeamMemberRole role) {
        lenient().when(userRepository.findByEmailOrPhone(user.getEmail())).thenReturn(Optional.of(user));
        lenient().when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        lenient().when(teamMemberRepository.existsByTeamIdAndUserIdAndStatus(10L, user.getId(), TeamMemberStatus.ACTIVE)).thenReturn(true);
        lenient().when(teamMemberRepository.findByTeamIdAndUserIdAndStatus(10L, user.getId(), TeamMemberStatus.ACTIVE)).thenReturn(Optional.of(member(user, team, role)));
    }

    private TeamMember member(User user, Team team, TeamMemberRole role) {
        return TeamMember.builder().id(20L).team(team).user(user).role(role).status(TeamMemberStatus.ACTIVE).build();
    }

    private Team team(User captain) {
        Team team = new Team();
        team.setId(10L);
        team.setCaptain(captain);
        team.setName("Storm Riders");
        team.setSport("Cricket");
        team.setCity("Ahmedabad");
        team.setVisibility(TeamVisibility.PUBLIC);
        team.setMaxMembers(11);
        return team;
    }

    private User user(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(name.toLowerCase() + "@test.com");
        user.setPhone("900000000" + id);
        user.setRoles(new java.util.HashSet<>());
        return user;
    }
}
