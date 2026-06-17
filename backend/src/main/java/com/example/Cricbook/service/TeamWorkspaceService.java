package com.example.Cricbook.service;

import com.example.Cricbook.dto.*;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamWorkspaceService {
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final PublicMatchRepository publicMatchRepository;
    private final TeamWorkspaceRepository workspaceRepository;
    private final TeamAvailabilityRepository availabilityRepository;
    private final TeamLineupRepository lineupRepository;
    private final TeamPollRepository pollRepository;
    private final TeamPollVoteRepository voteRepository;
    private final TeamAnnouncementRepository announcementRepository;
    private final TeamFinanceRepository financeRepository;
    private final TeamAttendanceRepository attendanceRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public TeamWorkspaceResponse getWorkspace(String loginId, Long teamId) {
        User user = findUser(loginId);
        Team team = findTeam(teamId);
        assertTeamAccess(user, team);
        return mapWorkspace(team, user);
    }

    @Transactional
    public TeamWorkspaceResponse updateWorkspace(String loginId, Long teamId, TeamWorkspaceRequest request) {
        User user = findUser(loginId);
        Team team = findTeam(teamId);
        assertTeamManager(user, team);
        TeamWorkspace workspace = ensureWorkspace(team);
        if (request != null) {
            if (hasText(request.getVisibility())) workspace.setVisibility(request.getVisibility().trim());
            if (hasText(request.getTeamType())) workspace.setTeamType(request.getTeamType().trim());
            if (hasText(request.getBannerUrl())) workspace.setBannerUrl(request.getBannerUrl().trim());
            if (request.getGoals() != null) workspace.setGoals(request.getGoals().trim());
        }
        workspaceRepository.save(workspace);
        return mapWorkspace(team, user);
    }

    @Transactional
    public TeamAvailabilityResponse setAvailability(String loginId, Long teamId, TeamAvailabilityRequest request) {
        if (request == null || request.getDate() == null || request.getStatus() == null) {
            throw ApiException.badRequest("Availability date and status are required.");
        }
        User user = findUser(loginId);
        Team team = findTeam(teamId);
        assertTeamAccess(user, team);
        TeamAvailability availability = availabilityRepository.findByTeamIdAndUserIdAndDate(teamId, user.getId(), request.getDate())
                .orElse(TeamAvailability.builder().team(team).user(user).date(request.getDate()).build());
        availability.setStatus(request.getStatus());
        availability.setNote(clean(request.getNote()));
        return mapAvailability(availabilityRepository.save(availability));
    }

    @Transactional
    public TeamLineupResponse saveLineup(String loginId, Long teamId, TeamLineupRequest request) {
        if (request == null || request.getUserId() == null || request.getPosition() == null) {
            throw ApiException.badRequest("Lineup player and batting position are required.");
        }
        User actor = findUser(loginId);
        Team team = findTeam(teamId);
        assertTeamManager(actor, team);
        User player = userRepository.findById(request.getUserId()).orElseThrow(() -> ApiException.notFound("Lineup player was not found."));
        assertActiveMember(teamId, player.getId());
        PublicMatch match = request.getMatchId() == null ? null : publicMatchRepository.findById(request.getMatchId())
                .orElseThrow(() -> ApiException.notFound("Match was not found."));
        TeamLineup lineup = lineupRepository.findByTeamIdAndMatchIdAndUserId(teamId, request.getMatchId(), request.getUserId())
                .orElse(TeamLineup.builder().team(team).match(match).user(player).build());
        lineup.setPosition(Math.max(1, request.getPosition()));
        lineup.setLineupRole(hasText(request.getLineupRole()) ? request.getLineupRole().trim() : "PLAYER");
        TeamLineup saved = lineupRepository.save(lineup);
        notificationService.pushNotification(player.getId(), player.getEmail(), "Lineup Selected", "Your captain selected you in the Cricbook team lineup.");
        return mapLineup(saved);
    }

    @Transactional
    public TeamPollResponse createPoll(String loginId, Long teamId, TeamPollRequest request) {
        if (request == null || !hasText(request.getQuestion()) || request.getOptions() == null || request.getOptions().size() < 2) {
            throw ApiException.badRequest("Poll question and at least two options are required.");
        }
        User user = findUser(loginId);
        Team team = findTeam(teamId);
        assertTeamManager(user, team);
        String options = request.getOptions().stream().filter(this::hasText).map(String::trim).distinct().collect(Collectors.joining("|"));
        TeamPoll poll = pollRepository.save(TeamPoll.builder()
                .team(team)
                .createdBy(user)
                .question(request.getQuestion().trim())
                .optionsCsv(options)
                .expiresAt(request.getExpiresAt())
                .build());
        return mapPoll(poll);
    }

    @Transactional
    public TeamPollResponse vote(String loginId, Long teamId, Long pollId, TeamVoteRequest request) {
        if (request == null || !hasText(request.getSelectedOption())) {
            throw ApiException.badRequest("Poll option is required.");
        }
        User user = findUser(loginId);
        Team team = findTeam(teamId);
        assertTeamAccess(user, team);
        TeamPoll poll = pollRepository.findById(pollId).orElseThrow(() -> ApiException.notFound("Team poll was not found."));
        if (!poll.getTeam().getId().equals(team.getId())) {
            throw ApiException.badRequest("Poll does not belong to this team.");
        }
        if (!pollOptions(poll).contains(request.getSelectedOption())) {
            throw ApiException.badRequest("Selected option is not valid for this poll.");
        }
        TeamPollVote vote = voteRepository.findByPollIdAndUserId(pollId, user.getId())
                .orElse(TeamPollVote.builder().poll(poll).user(user).build());
        vote.setSelectedOption(request.getSelectedOption());
        voteRepository.save(vote);
        return mapPoll(poll);
    }

    @Transactional
    public TeamAnnouncementResponse createAnnouncement(String loginId, Long teamId, TeamAnnouncementRequest request) {
        if (request == null || !hasText(request.getTitle()) || !hasText(request.getContent())) {
            throw ApiException.badRequest("Announcement title and content are required.");
        }
        User user = findUser(loginId);
        Team team = findTeam(teamId);
        assertTeamManager(user, team);
        TeamAnnouncement announcement = announcementRepository.save(TeamAnnouncement.builder()
                .team(team)
                .createdBy(user)
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .pinned(Boolean.TRUE.equals(request.getPinned()))
                .expiresAt(request.getExpiresAt())
                .build());
        teamMemberRepository.findByTeamIdAndStatusOrderByCreatedAtAsc(teamId, TeamMemberStatus.ACTIVE)
                .forEach(member -> notificationService.pushNotification(member.getUser().getId(), member.getUser().getEmail(), "Team Announcement", announcement.getTitle()));
        return mapAnnouncement(announcement);
    }

    @Transactional
    public TeamFinanceResponse addFinance(String loginId, Long teamId, TeamFinanceRequest request) {
        if (request == null || request.getType() == null || request.getAmount() == null || !hasText(request.getDescription())) {
            throw ApiException.badRequest("Finance type, amount, and description are required.");
        }
        User actor = findUser(loginId);
        Team team = findTeam(teamId);
        assertTeamManager(actor, team);
        User target = request.getUserId() == null ? actor : userRepository.findById(request.getUserId()).orElseThrow(() -> ApiException.notFound("Finance member was not found."));
        TeamFinance finance = financeRepository.save(TeamFinance.builder()
                .team(team)
                .user(target)
                .type(request.getType())
                .amount(request.getAmount())
                .description(request.getDescription().trim())
                .build());
        return mapFinance(finance);
    }

    @Transactional
    public TeamAttendanceResponse recordAttendance(String loginId, Long teamId, TeamAttendanceRequest request) {
        if (request == null || request.getUserId() == null || request.getStatus() == null) {
            throw ApiException.badRequest("Attendance player and status are required.");
        }
        User actor = findUser(loginId);
        Team team = findTeam(teamId);
        assertTeamManager(actor, team);
        User player = userRepository.findById(request.getUserId()).orElseThrow(() -> ApiException.notFound("Attendance player was not found."));
        assertActiveMember(teamId, player.getId());
        PublicMatch match = request.getMatchId() == null ? null : publicMatchRepository.findById(request.getMatchId())
                .orElseThrow(() -> ApiException.notFound("Match was not found."));
        TeamAttendance attendance = attendanceRepository.findByTeamIdAndMatchIdAndUserId(teamId, request.getMatchId(), request.getUserId())
                .orElse(TeamAttendance.builder().team(team).match(match).user(player).build());
        attendance.setStatus(request.getStatus());
        attendance.setNote(clean(request.getNote()));
        return mapAttendance(attendanceRepository.save(attendance));
    }

    @Transactional(readOnly = true)
    public TeamAnalyticsResponse analytics(String loginId) {
        User user = findUser(loginId);
        boolean admin = hasRole(user, RoleName.ROLE_ADMIN);
        List<Team> teams = admin ? teamRepository.findAll() : teamMemberRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), TeamMemberStatus.ACTIVE)
                .stream().map(TeamMember::getTeam).toList();
        BigDecimal balance = teams.stream().map(team -> teamBalance(team.getId())).reduce(BigDecimal.ZERO, BigDecimal::add);
        long activeMembers = teams.stream().mapToLong(team -> teamMemberRepository.countByTeamIdAndStatus(team.getId(), TeamMemberStatus.ACTIVE)).sum();
        long availability = teams.stream().flatMap(team -> availabilityRepository.findByTeamIdAndDateBetweenOrderByDateAscCreatedAtAsc(team.getId(), LocalDate.now().minusDays(7), LocalDate.now().plusDays(14)).stream()).count();
        long polls = teams.stream().mapToLong(team -> pollRepository.findByTeamIdOrderByCreatedAtDesc(team.getId()).size()).sum();
        int attendanceRate = teams.isEmpty() ? 0 : (int) Math.round(teams.stream().mapToInt(team -> attendanceRate(team.getId())).average().orElse(0));
        return TeamAnalyticsResponse.builder()
                .totalTeams((long) teams.size())
                .activeMembers(activeMembers)
                .availabilityResponses(availability)
                .polls(polls)
                .teamWalletBalance(balance)
                .averageAttendanceRate(attendanceRate)
                .mostActiveTeams(teams.stream().limit(8).map(this::mapWorkspaceSnapshot).toList())
                .build();
    }

    private TeamWorkspaceResponse mapWorkspace(Team team, User viewer) {
        TeamWorkspace workspace = ensureWorkspace(team);
        LocalDate today = LocalDate.now();
        return TeamWorkspaceResponse.builder()
                .teamId(team.getId())
                .teamName(team.getName())
                .city(team.getCity())
                .sport(team.getSport())
                .visibility(workspace.getVisibility())
                .teamType(workspace.getTeamType())
                .bannerUrl(workspace.getBannerUrl())
                .goals(workspace.getGoals())
                .memberCount((int) teamMemberRepository.countByTeamIdAndStatus(team.getId(), TeamMemberStatus.ACTIVE))
                .availableToday((int) availabilityRepository.countByTeamIdAndDateAndStatus(team.getId(), today, TeamAvailabilityStatus.AVAILABLE))
                .maybeToday((int) availabilityRepository.countByTeamIdAndDateAndStatus(team.getId(), today, TeamAvailabilityStatus.MAYBE))
                .unavailableToday((int) availabilityRepository.countByTeamIdAndDateAndStatus(team.getId(), today, TeamAvailabilityStatus.UNAVAILABLE))
                .teamBalance(teamBalance(team.getId()))
                .attendanceRate(attendanceRate(team.getId()))
                .members(teamMemberRepository.findByTeamIdAndStatusOrderByCreatedAtAsc(team.getId(), TeamMemberStatus.ACTIVE).stream().map(this::mapMember).toList())
                .availability(availabilityRepository.findByTeamIdAndDateBetweenOrderByDateAscCreatedAtAsc(team.getId(), today.minusDays(3), today.plusDays(14)).stream().map(this::mapAvailability).toList())
                .lineup(lineupRepository.findByTeamIdOrderByCreatedAtDesc(team.getId()).stream().limit(20).map(this::mapLineup).toList())
                .polls(pollRepository.findByTeamIdOrderByCreatedAtDesc(team.getId()).stream().limit(10).map(this::mapPoll).toList())
                .announcements(announcementRepository.findByTeamIdOrderByPinnedDescCreatedAtDesc(team.getId()).stream().limit(10).map(this::mapAnnouncement).toList())
                .finances(financeRepository.findByTeamIdOrderByCreatedAtDesc(team.getId()).stream().limit(20).map(this::mapFinance).toList())
                .attendance(attendanceRepository.findByTeamIdOrderByCreatedAtDesc(team.getId()).stream().limit(20).map(this::mapAttendance).toList())
                .build();
    }

    private TeamWorkspaceResponse mapWorkspaceSnapshot(Team team) {
        Optional<TeamWorkspace> workspace = workspaceRepository.findByTeamId(team.getId());
        LocalDate today = LocalDate.now();
        return TeamWorkspaceResponse.builder()
                .teamId(team.getId())
                .teamName(team.getName())
                .city(team.getCity())
                .sport(team.getSport())
                .visibility(workspace.map(TeamWorkspace::getVisibility).orElse(team.getVisibility().name()))
                .teamType(workspace.map(TeamWorkspace::getTeamType).orElse("Casual"))
                .bannerUrl(workspace.map(TeamWorkspace::getBannerUrl).orElse(null))
                .goals(workspace.map(TeamWorkspace::getGoals).orElse(team.getDescription()))
                .memberCount((int) teamMemberRepository.countByTeamIdAndStatus(team.getId(), TeamMemberStatus.ACTIVE))
                .availableToday((int) availabilityRepository.countByTeamIdAndDateAndStatus(team.getId(), today, TeamAvailabilityStatus.AVAILABLE))
                .maybeToday((int) availabilityRepository.countByTeamIdAndDateAndStatus(team.getId(), today, TeamAvailabilityStatus.MAYBE))
                .unavailableToday((int) availabilityRepository.countByTeamIdAndDateAndStatus(team.getId(), today, TeamAvailabilityStatus.UNAVAILABLE))
                .teamBalance(teamBalance(team.getId()))
                .attendanceRate(attendanceRate(team.getId()))
                .members(List.of())
                .availability(List.of())
                .lineup(List.of())
                .polls(List.of())
                .announcements(List.of())
                .finances(List.of())
                .attendance(List.of())
                .build();
    }

    private TeamMemberResponse mapMember(TeamMember member) {
        return TeamMemberResponse.builder()
                .id(member.getId())
                .userId(member.getUser().getId())
                .name(member.getUser().getName())
                .email(member.getUser().getEmail())
                .role(member.getRole())
                .status(member.getStatus())
                .build();
    }

    private TeamAvailabilityResponse mapAvailability(TeamAvailability item) {
        return TeamAvailabilityResponse.builder().id(item.getId()).teamId(item.getTeam().getId()).userId(item.getUser().getId()).userName(item.getUser().getName()).date(item.getDate()).status(item.getStatus()).note(item.getNote()).build();
    }

    private TeamLineupResponse mapLineup(TeamLineup item) {
        return TeamLineupResponse.builder().id(item.getId()).teamId(item.getTeam().getId()).matchId(item.getMatch() == null ? null : item.getMatch().getId()).userId(item.getUser().getId()).userName(item.getUser().getName()).position(item.getPosition()).lineupRole(item.getLineupRole()).build();
    }

    private TeamPollResponse mapPoll(TeamPoll poll) {
        Map<String, Long> counts = voteRepository.findByPollId(poll.getId()).stream().collect(Collectors.groupingBy(TeamPollVote::getSelectedOption, Collectors.counting()));
        return TeamPollResponse.builder().id(poll.getId()).teamId(poll.getTeam().getId()).question(poll.getQuestion()).options(pollOptions(poll)).voteCounts(counts).expiresAt(poll.getExpiresAt()).build();
    }

    private TeamAnnouncementResponse mapAnnouncement(TeamAnnouncement item) {
        return TeamAnnouncementResponse.builder().id(item.getId()).teamId(item.getTeam().getId()).title(item.getTitle()).content(item.getContent()).pinned(item.getPinned()).createdByName(item.getCreatedBy().getName()).expiresAt(item.getExpiresAt()).createdAt(item.getCreatedAt()).build();
    }

    private TeamFinanceResponse mapFinance(TeamFinance item) {
        return TeamFinanceResponse.builder().id(item.getId()).teamId(item.getTeam().getId()).userId(item.getUser() == null ? null : item.getUser().getId()).userName(item.getUser() == null ? null : item.getUser().getName()).type(item.getType()).amount(item.getAmount()).description(item.getDescription()).createdAt(item.getCreatedAt()).build();
    }

    private TeamAttendanceResponse mapAttendance(TeamAttendance item) {
        return TeamAttendanceResponse.builder().id(item.getId()).teamId(item.getTeam().getId()).matchId(item.getMatch() == null ? null : item.getMatch().getId()).userId(item.getUser().getId()).userName(item.getUser().getName()).status(item.getStatus()).note(item.getNote()).build();
    }

    private TeamWorkspace ensureWorkspace(Team team) {
        return workspaceRepository.findByTeamId(team.getId()).orElseGet(() -> workspaceRepository.save(TeamWorkspace.builder().team(team).visibility(team.getVisibility().name()).teamType("Casual").goals(team.getDescription()).build()));
    }

    private BigDecimal teamBalance(Long teamId) {
        BigDecimal contributions = financeRepository.sumByTeamIdAndType(teamId, TeamFinanceType.CONTRIBUTION);
        BigDecimal refunds = financeRepository.sumByTeamIdAndType(teamId, TeamFinanceType.REFUND);
        BigDecimal expenses = financeRepository.sumByTeamIdAndType(teamId, TeamFinanceType.EXPENSE);
        return contributions.add(refunds).subtract(expenses);
    }

    private int attendanceRate(Long teamId) {
        long total = attendanceRepository.countByTeamId(teamId);
        if (total == 0) return 0;
        long present = attendanceRepository.countByTeamIdAndStatus(teamId, TeamAttendanceStatus.PRESENT) + attendanceRepository.countByTeamIdAndStatus(teamId, TeamAttendanceStatus.LATE);
        return (int) Math.round((present * 100.0) / total);
    }

    private List<String> pollOptions(TeamPoll poll) {
        return Arrays.stream(poll.getOptionsCsv().split("\\|")).filter(this::hasText).toList();
    }

    private void assertTeamAccess(User user, Team team) {
        if (hasRole(user, RoleName.ROLE_ADMIN)) return;
        if (!teamMemberRepository.existsByTeamIdAndUserIdAndStatus(team.getId(), user.getId(), TeamMemberStatus.ACTIVE)) {
            throw ApiException.forbidden("You must be an active team member to access this workspace.");
        }
    }

    private void assertTeamManager(User user, Team team) {
        if (hasRole(user, RoleName.ROLE_ADMIN)) return;
        TeamMember member = teamMemberRepository.findByTeamIdAndUserIdAndStatus(team.getId(), user.getId(), TeamMemberStatus.ACTIVE)
                .orElseThrow(() -> ApiException.forbidden("You must be an active team member to manage this workspace."));
        if (!(member.getRole() == TeamMemberRole.OWNER || member.getRole() == TeamMemberRole.CAPTAIN || member.getRole() == TeamMemberRole.VICE_CAPTAIN || member.getRole() == TeamMemberRole.MANAGER)) {
            throw ApiException.forbidden("Only team leadership can manage this workspace.");
        }
    }

    private void assertActiveMember(Long teamId, Long userId) {
        if (!teamMemberRepository.existsByTeamIdAndUserIdAndStatus(teamId, userId, TeamMemberStatus.ACTIVE)) {
            throw ApiException.badRequest("Player must be an active team member.");
        }
    }

    private Team findTeam(Long teamId) {
        return teamRepository.findById(teamId).orElseThrow(() -> ApiException.notFound("Team was not found."));
    }

    private User findUser(String loginId) {
        return userRepository.findByEmailOrPhone(loginId).orElseThrow(() -> ApiException.notFound("User account was not found."));
    }

    private boolean hasRole(User user, RoleName role) {
        return user.getRoles() != null && user.getRoles().stream().anyMatch(item -> item.getName() == role);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String clean(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
