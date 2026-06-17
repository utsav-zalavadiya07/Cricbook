package com.example.Cricbook.service;

import com.example.Cricbook.dto.*;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.ChatRoomType;
import com.example.Cricbook.enums.FeedEventType;
import com.example.Cricbook.enums.InviteStatus;
import com.example.Cricbook.enums.MatchFormat;
import com.example.Cricbook.enums.MatchVisibility;
import com.example.Cricbook.enums.PlayerConnectionStatus;
import com.example.Cricbook.enums.PlayerConnectionType;
import com.example.Cricbook.enums.PublicMatchStatus;
import com.example.Cricbook.enums.RecruitmentApplicationStatus;
import com.example.Cricbook.enums.TeamMemberRole;
import com.example.Cricbook.enums.TeamMemberStatus;
import com.example.Cricbook.enums.TeamRecruitmentStatus;
import com.example.Cricbook.enums.TeamVisibility;
import com.example.Cricbook.enums.NotificationChannel;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SocialService {

    private final UserRepository userRepository;
    private final TurfRepository turfRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamInviteRepository teamInviteRepository;
    private final TeamJoinRequestRepository teamJoinRequestRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final PlayerAvailabilityRepository playerAvailabilityRepository;
    private final PlayerTrustScoreRepository playerTrustScoreRepository;
    private final PlayerConnectionRepository playerConnectionRepository;
    private final TeamRecruitmentRepository teamRecruitmentRepository;
    private final RecruitmentApplicationRepository recruitmentApplicationRepository;
    private final FeedEventRepository feedEventRepository;
    private final FeedReactionRepository feedReactionRepository;
    private final FeedCommentRepository feedCommentRepository;
    private final CommunityModerationRepository communityModerationRepository;
    private final PublicMatchRepository publicMatchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final NotificationService notificationService;

    @Transactional
    public TeamResponse createTeam(String username, CreateTeamRequest request) {
        if (request == null) {
            throw new ApiException("Team details are required");
        }
        User user = findUser(username);
        String teamName = cleanRequired(request.getName(), "Team name is required", 3, 80);
        if (teamRepository.existsByNameIgnoreCase(teamName)) {
            throw ApiException.conflict("A team with this name already exists.");
        }
        String city = cleanRequired(request.getCity(), "City is required", 2, 80);
        Team team = teamRepository.save(Team.builder()
                .captain(user)
                .name(teamName)
                .sport(cleanOptional(request.getSport(), 40, "Sport").orElse("Cricket"))
                .city(city)
                .description(cleanOptional(request.getDescription(), 600, "Description").orElse(null))
                .logoUrl(cleanOptional(request.getLogoUrl(), 500, "Logo URL").orElse(null))
                .visibility(request.getVisibility() == null ? TeamVisibility.PUBLIC : request.getVisibility())
                .maxMembers(resolveTeamSize(request.getMaxMembers()))
                .build());

        teamMemberRepository.save(TeamMember.builder()
                .team(team)
                .user(user)
                .role(TeamMemberRole.CAPTAIN)
                .status(TeamMemberStatus.ACTIVE)
                .build());

        publishFeed(user, FeedEventType.TEAM_CREATED, "created a new team", team.getName(), team.getId(), "TEAM", "/teams");
        return mapTeam(team, true);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getMyTeams(String username) {
        User user = findUser(username);
        return teamMemberRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), TeamMemberStatus.ACTIVE)
                .stream()
                .map(member -> mapTeam(member.getTeam(), false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> discoverTeams(String username, String query, String city, TeamVisibility visibility, boolean openOnly) {
        findUser(username);
        List<TeamVisibility> visibilities = visibility == null
                ? List.of(TeamVisibility.PUBLIC, TeamVisibility.INVITE_ONLY)
                : visibility == TeamVisibility.PRIVATE ? List.of() : List.of(visibility);
        if (visibilities.isEmpty()) {
            return List.of();
        }
        String cleanedQuery = cleanSearch(query);
        String cleanedCity = cleanSearch(city);
        return teamRepository.discoverTeams(cleanedQuery, cleanedCity, visibilities)
                .stream()
                .filter(team -> !openOnly || teamMemberRepository.countByTeamIdAndStatus(team.getId(), TeamMemberStatus.ACTIVE) < team.getMaxMembers())
                .map(team -> mapTeam(team, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamInviteResponse> getMyInvites(String username) {
        User user = findUser(username);
        return teamInviteRepository.findByReceiverIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapInvite)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamJoinRequestResponse> getJoinRequestsForManagedTeams(String username) {
        User user = findUser(username);
        List<TeamJoinRequest> requests = new ArrayList<>();
        for (TeamMember membership : teamMemberRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), TeamMemberStatus.ACTIVE)) {
            if (isTeamManagerRole(membership.getRole())) {
                requests.addAll(teamJoinRequestRepository.findByTeamIdAndStatusOrderByCreatedAtDesc(membership.getTeam().getId(), InviteStatus.PENDING));
            }
        }
        return requests.stream().map(this::mapJoinRequest).toList();
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeam(String username, Long teamId) {
        User user = findUser(username);
        Team team = findTeam(teamId);
        if (team.getVisibility() == TeamVisibility.PRIVATE) {
            assertActiveTeamMember(teamId, user.getId());
        }
        return mapTeam(team, true);
    }

    @Transactional
    public TeamMemberResponse inviteTeamMember(String username, Long teamId, InviteTeamMemberRequest request) {
        if (request == null) {
            throw new ApiException("Member details are required");
        }
        User captain = findUser(username);
        Team team = findTeam(teamId);
        assertTeamManager(team, captain);

        User invitedUser = findUser(required(request.getLoginId(), "User email or phone is required"));
        if (teamMemberRepository.existsByTeamIdAndUserIdAndStatus(teamId, invitedUser.getId(), TeamMemberStatus.ACTIVE)) {
            throw new ApiException("User already belongs to this team");
        }
        if (teamInviteRepository.existsByTeamIdAndReceiverIdAndStatus(teamId, invitedUser.getId(), InviteStatus.PENDING)) {
            throw new ApiException("Invite already pending for this user");
        }
        if (teamMemberRepository.countByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE) >= team.getMaxMembers()) {
            throw new ApiException("Team is full");
        }

        TeamInvite invite = teamInviteRepository.save(TeamInvite.builder()
                .team(team)
                .sender(captain)
                .receiver(invitedUser)
                .status(InviteStatus.PENDING)
                .build());

        publishFeed(captain, FeedEventType.TEAM_INVITE_SENT, "invited a player to", team.getName(), team.getId(), "TEAM", "/teams");
        notificationService.pushNotification(
                invitedUser.getId(),
                invitedUser.getEmail(),
                "Team invite from " + captain.getName(),
                "You have been invited to join " + team.getName(),
                com.example.Cricbook.enums.NotificationChannel.PUSH,
                null,
                "/teams"
        );

        return TeamMemberResponse.builder()
                .id(invite.getId())
                .userId(invitedUser.getId())
                .name(invitedUser.getName())
                .email(invitedUser.getEmail())
                .role(TeamMemberRole.PLAYER)
                .status(TeamMemberStatus.INVITED)
                .createdAt(invite.getCreatedAt())
                .build();
    }

    @Transactional
    public TeamJoinRequestResponse requestToJoinTeam(String username, Long teamId, RequestJoinTeamRequest request) {
        User requester = findUser(username);
        Team team = findTeam(teamId);
        if (teamMemberRepository.existsByTeamIdAndUserIdAndStatus(teamId, requester.getId(), TeamMemberStatus.ACTIVE)) {
            throw ApiException.conflict("You are already a member of this team.");
        }
        if (teamMemberRepository.countByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE) >= team.getMaxMembers()) {
            throw ApiException.conflict("Team is full.");
        }
        if (teamJoinRequestRepository.existsByTeamIdAndRequesterIdAndStatus(teamId, requester.getId(), InviteStatus.PENDING)) {
            throw ApiException.conflict("Join request already pending for this team.");
        }
        TeamJoinRequest saved = teamJoinRequestRepository.save(TeamJoinRequest.builder()
                .team(team)
                .requester(requester)
                .message(cleanOptional(request == null ? null : request.getMessage(), 600, "Message").orElse(null))
                .status(InviteStatus.PENDING)
                .build());
        notificationService.pushNotification(
                team.getCaptain().getId(),
                team.getCaptain().getEmail(),
                requester.getName() + " requested to join " + team.getName(),
                "Review the pending team join request.",
                com.example.Cricbook.enums.NotificationChannel.PUSH,
                null,
                "/teams"
        );
        return mapJoinRequest(saved);
    }

    @Transactional
    public TeamJoinRequestResponse respondToJoinRequest(String username, Long requestId, RespondInviteRequest request) {
        User reviewer = findUser(username);
        TeamJoinRequest joinRequest = teamJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> ApiException.notFound("Join request not found."));
        assertTeamManager(joinRequest.getTeam(), reviewer);
        if (joinRequest.getStatus() != InviteStatus.PENDING) {
            throw ApiException.conflict("Join request is already processed.");
        }
        joinRequest.setReviewer(reviewer);
        if (request != null && request.isAccept()) {
            if (teamMemberRepository.countByTeamIdAndStatus(joinRequest.getTeam().getId(), TeamMemberStatus.ACTIVE) >= joinRequest.getTeam().getMaxMembers()) {
                throw ApiException.conflict("Team is full.");
            }
            joinRequest.setStatus(InviteStatus.ACCEPTED);
            upsertActiveMembership(joinRequest.getTeam(), joinRequest.getRequester(), TeamMemberRole.PLAYER);
            publishFeed(joinRequest.getRequester(), FeedEventType.TEAM_JOINED, "joined team", joinRequest.getTeam().getName(), joinRequest.getTeam().getId(), "TEAM", "/teams");
        } else {
            joinRequest.setStatus(InviteStatus.DECLINED);
        }
        TeamJoinRequest saved = teamJoinRequestRepository.save(joinRequest);
        notificationService.pushNotification(
                joinRequest.getRequester().getId(),
                joinRequest.getRequester().getEmail(),
                "Team request " + saved.getStatus().name().toLowerCase(Locale.ROOT),
                "Your request for " + joinRequest.getTeam().getName() + " was " + saved.getStatus().name().toLowerCase(Locale.ROOT) + ".",
                com.example.Cricbook.enums.NotificationChannel.PUSH,
                null,
                "/teams"
        );
        return mapJoinRequest(saved);
    }

    @Transactional
    public TeamInviteResponse respondToInvite(String username, Long inviteId, RespondInviteRequest request) {
        User receiver = findUser(username);
        TeamInvite invite = teamInviteRepository.findByIdAndReceiverId(inviteId, receiver.getId())
                .orElseThrow(() -> new ApiException("Invite not found"));
        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new ApiException("Invite is already processed");
        }

        if (request != null && request.isAccept()) {
            if (teamMemberRepository.countByTeamIdAndStatus(invite.getTeam().getId(), TeamMemberStatus.ACTIVE) >= invite.getTeam().getMaxMembers()) {
                throw new ApiException("Team is full");
            }
            invite.setStatus(InviteStatus.ACCEPTED);
            upsertActiveMembership(invite.getTeam(), receiver, TeamMemberRole.PLAYER);
            publishFeed(receiver, FeedEventType.TEAM_JOINED, "joined team", invite.getTeam().getName(), invite.getTeam().getId(), "TEAM", "/teams");
            notificationService.pushNotification(
                    invite.getSender().getId(),
                    invite.getSender().getEmail(),
                    receiver.getName() + " accepted your team invite",
                    receiver.getName() + " joined " + invite.getTeam().getName(),
                    com.example.Cricbook.enums.NotificationChannel.PUSH,
                    null,
                    "/teams"
            );
        } else {
            invite.setStatus(InviteStatus.DECLINED);
        }

        return mapInvite(teamInviteRepository.save(invite));
    }

    @Transactional
    public TeamResponse leaveTeam(String username, Long teamId) {
        User user = findUser(username);
        TeamMember membership = teamMemberRepository.findByTeamIdAndUserIdAndStatus(teamId, user.getId(), TeamMemberStatus.ACTIVE)
                .orElseThrow(() -> new ApiException("You are not an active member of this team"));
        if (membership.getRole() == TeamMemberRole.CAPTAIN) {
            throw new ApiException("Transfer captain role before leaving the team");
        }
        membership.setStatus(TeamMemberStatus.LEFT);
        teamMemberRepository.save(membership);
        return mapTeam(membership.getTeam(), true);
    }

    @Transactional
    public TeamResponse removeMember(String username, Long teamId, Long userId) {
        User actingUser = findUser(username);
        Team team = findTeam(teamId);
        assertCaptain(team, actingUser);
        TeamMember membership = teamMemberRepository.findByTeamIdAndUserIdAndStatus(teamId, userId, TeamMemberStatus.ACTIVE)
                .orElseThrow(() -> new ApiException("Member not found"));
        if (membership.getRole() == TeamMemberRole.CAPTAIN) {
            throw new ApiException("Cannot remove the captain");
        }
        membership.setStatus(TeamMemberStatus.REMOVED);
        teamMemberRepository.save(membership);
        return mapTeam(team, true);
    }

    @Transactional
    public TeamResponse transferCaptain(String username, Long teamId, TeamActionRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new ApiException("Target user is required");
        }
        User actingUser = findUser(username);
        Team team = findTeam(teamId);
        assertCaptain(team, actingUser);

        TeamMember currentCaptain = teamMemberRepository.findByTeamIdAndUserIdAndStatus(teamId, actingUser.getId(), TeamMemberStatus.ACTIVE)
                .orElseThrow(() -> new ApiException("Captain membership not found"));
        TeamMember nextCaptain = teamMemberRepository.findByTeamIdAndUserIdAndStatus(teamId, request.getUserId(), TeamMemberStatus.ACTIVE)
                .orElseThrow(() -> new ApiException("Target member not found"));

        currentCaptain.setRole(TeamMemberRole.VICE_CAPTAIN);
        nextCaptain.setRole(TeamMemberRole.CAPTAIN);
        team.setCaptain(nextCaptain.getUser());
        teamMemberRepository.save(currentCaptain);
        teamMemberRepository.save(nextCaptain);
        teamRepository.save(team);
        return mapTeam(team, true);
    }

    @Transactional
    public TeamResponse updateMemberRole(String username, Long teamId, TeamActionRequest request) {
        if (request == null || request.getUserId() == null || request.getRole() == null) {
            throw new ApiException("User and role are required");
        }
        if (request.getRole() == TeamMemberRole.CAPTAIN) {
            throw new ApiException("Use transfer captain for captain changes");
        }
        User actingUser = findUser(username);
        Team team = findTeam(teamId);
        assertCaptain(team, actingUser);

        TeamMember membership = teamMemberRepository.findByTeamIdAndUserIdAndStatus(teamId, request.getUserId(), TeamMemberStatus.ACTIVE)
                .orElseThrow(() -> new ApiException("Member not found"));
        if (membership.getRole() == TeamMemberRole.CAPTAIN) {
            throw new ApiException("Use transfer captain for captain changes");
        }
        membership.setRole(request.getRole());
        teamMemberRepository.save(membership);
        return mapTeam(team, true);
    }

    @Transactional
    public void deleteTeam(String username, Long teamId) {
        User actingUser = findUser(username);
        Team team = findTeam(teamId);
        assertCaptain(team, actingUser);
        List<TeamInvite> invites = teamInviteRepository.findByTeamIdOrderByCreatedAtDesc(teamId);
        invites.forEach(invite -> invite.setStatus(InviteStatus.CANCELLED));
        teamInviteRepository.saveAll(invites);
        teamRepository.delete(team);
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getTeamMembers(String username, Long teamId) {
        User user = findUser(username);
        Team team = findTeam(teamId);
        if (team.getVisibility() == TeamVisibility.PRIVATE) {
            assertActiveTeamMember(teamId, user.getId());
        }
        return teamMemberRepository.findByTeamIdAndStatusOrderByCreatedAtAsc(teamId, TeamMemberStatus.ACTIVE)
                .stream()
                .map(this::mapMember)
                .toList();
    }

    @Transactional
    public PublicMatchResponse createPublicMatch(String username, CreatePublicMatchRequest request) {
        if (request == null) {
            throw new ApiException("Match details are required");
        }
        User user = findUser(username);
        Turf turf = request.getTurfId() == null ? null : turfRepository.findById(request.getTurfId())
                .orElseThrow(() -> new ApiException("Turf not found"));
        Team homeTeam = request.getHomeTeamId() == null ? null : findTeam(request.getHomeTeamId());
        Team awayTeam = request.getAwayTeamId() == null ? null : findTeam(request.getAwayTeamId());
        if (awayTeam != null && homeTeam == null) {
            throw ApiException.badRequest("Home team is required when challenging another team.");
        }
        if (homeTeam != null) {
            assertTeamManager(homeTeam, user);
        }
        if (homeTeam != null && awayTeam != null) {
            if (homeTeam.getId().equals(awayTeam.getId())) {
                throw ApiException.badRequest("A team cannot challenge itself.");
            }
            List<PublicMatchStatus> activeChallengeStates = List.of(PublicMatchStatus.PENDING, PublicMatchStatus.ACCEPTED, PublicMatchStatus.SCHEDULED, PublicMatchStatus.LIVE);
            boolean duplicateChallenge = publicMatchRepository.existsByHomeTeamIdAndAwayTeamIdAndStatusIn(homeTeam.getId(), awayTeam.getId(), activeChallengeStates)
                    || publicMatchRepository.existsByHomeTeamIdAndAwayTeamIdAndStatusIn(awayTeam.getId(), homeTeam.getId(), activeChallengeStates);
            if (duplicateChallenge) {
                throw ApiException.badRequest("An active challenge already exists between these teams.");
            }
        }
        if (request.getMatchDate() != null && request.getStartTime() != null && LocalDateTime.of(request.getMatchDate(), request.getStartTime()).isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest("Match cannot be scheduled in the past.");
        }

        PublicMatch match = publicMatchRepository.save(PublicMatch.builder()
                .creator(user)
                .turf(turf)
                .title(cleanRequired(request.getTitle(), "Match title is required", 3, 120))
                .sport(defaultValue(request.getSport(), "Cricket"))
                .city(cleanRequired(request.getCity(), "City is required", 2, 80))
                .location(cleanOptional(defaultValue(request.getLocation(), turf == null ? request.getCity() : turf.getLocation()), 180, "Location").orElse(request.getCity()))
                .matchDate(requiredObject(request.getMatchDate(), "Match date is required"))
                .startTime(requiredObject(request.getStartTime(), "Start time is required"))
                .endTime(request.getEndTime())
                .maxPlayers(resolveLimit(request.getMaxPlayers(), 22, "Max players must be positive"))
                .visibility(request.getVisibility() == null ? MatchVisibility.PUBLIC : request.getVisibility())
                .format(request.getFormat() == null ? MatchFormat.FRIENDLY : request.getFormat())
                .overs(request.getOvers())
                .entryFee(request.getEntryFee())
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .challengeMessage(cleanOptional(request.getChallengeMessage(), 500, "Challenge message").orElse(null))
                .notes(cleanOptional(request.getNotes(), 1000, "Match notes").orElse(null))
                .scheduledAt(homeTeam != null && awayTeam != null ? null : LocalDateTime.now())
                .status(homeTeam != null && awayTeam != null ? PublicMatchStatus.PENDING : PublicMatchStatus.OPEN)
                .build());

        matchParticipantRepository.save(MatchParticipant.builder()
                .match(match)
                .user(user)
                .build());

        publishFeed(user, FeedEventType.MATCH_CREATED, homeTeam != null && awayTeam != null ? "sent a team challenge" : "created a match", match.getTitle(), match.getId(), "MATCH", "/social");
        if (awayTeam != null && awayTeam.getCaptain() != null) {
            notificationService.pushNotification(
                    awayTeam.getCaptain().getId(),
                    awayTeam.getCaptain().getEmail(),
                    "New team challenge",
                    homeTeam.getName() + " challenged " + awayTeam.getName() + " to a match.",
                    com.example.Cricbook.enums.NotificationChannel.PUSH,
                    null,
                    "/social"
            );
        }
        return mapMatch(match, true);
    }

    @Transactional(readOnly = true)
    public List<PublicMatchResponse> getOpenMatches() {
        return publicMatchRepository.findByStatusOrderByMatchDateAscStartTimeAsc(PublicMatchStatus.OPEN)
                .stream()
                .map(match -> mapMatch(match, false))
                .toList();
    }

    @Transactional
    public PublicMatchResponse joinMatch(String username, Long matchId) {
        User user = findUser(username);
        PublicMatch match = findMatch(matchId);
        if (match.getStatus() != PublicMatchStatus.OPEN) {
            throw new ApiException("Match is not open");
        }
        if (!matchParticipantRepository.existsByMatchIdAndUserId(matchId, user.getId())) {
            if (matchParticipantRepository.countByMatchId(matchId) >= match.getMaxPlayers()) {
                throw new ApiException("Match is full");
            }
            matchParticipantRepository.save(MatchParticipant.builder()
                    .match(match)
                    .user(user)
                    .build());
            publishFeed(user, FeedEventType.MATCH_JOINED, "joined match", match.getTitle(), match.getId(), "MATCH", "/social");
            notificationService.pushNotification(
                    match.getCreator().getId(),
                    match.getCreator().getEmail(),
                    user.getName() + " joined your match",
                    user.getName() + " joined " + match.getTitle(),
                    com.example.Cricbook.enums.NotificationChannel.PUSH,
                    null,
                    "/social"
            );
        }
        if (matchParticipantRepository.countByMatchId(matchId) >= match.getMaxPlayers()) {
            match.setStatus(PublicMatchStatus.FULL);
            publicMatchRepository.save(match);
        }
        return mapMatch(match, true);
    }

    @Transactional
    public PublicMatchResponse acceptMatchChallenge(String username, Long matchId) {
        User actor = findUser(username);
        PublicMatch match = findMatch(matchId);
        if (match.getStatus() != PublicMatchStatus.PENDING) {
            throw ApiException.badRequest("Only pending challenges can be accepted.");
        }
        if (match.getAwayTeam() == null) {
            throw ApiException.badRequest("This match is not a team challenge.");
        }
        assertTeamManager(match.getAwayTeam(), actor);
        match.setStatus(PublicMatchStatus.SCHEDULED);
        match.setAcceptedAt(LocalDateTime.now());
        match.setScheduledAt(LocalDateTime.now());
        PublicMatch saved = publicMatchRepository.save(match);
        publishFeed(actor, FeedEventType.MATCH_CREATED, "accepted a team challenge", saved.getTitle(), saved.getId(), "MATCH", "/social");
        notifyMatchCaptain(saved.getHomeTeam(), "Challenge accepted", saved.getAwayTeam().getName() + " accepted " + saved.getTitle());
        return mapMatch(saved, true);
    }

    @Transactional
    public PublicMatchResponse rejectMatchChallenge(String username, Long matchId) {
        User actor = findUser(username);
        PublicMatch match = findMatch(matchId);
        if (match.getStatus() != PublicMatchStatus.PENDING) {
            throw ApiException.badRequest("Only pending challenges can be rejected.");
        }
        if (match.getAwayTeam() == null) {
            throw ApiException.badRequest("This match is not a team challenge.");
        }
        assertTeamManager(match.getAwayTeam(), actor);
        match.setStatus(PublicMatchStatus.REJECTED);
        PublicMatch saved = publicMatchRepository.save(match);
        publishFeed(actor, FeedEventType.MATCH_CREATED, "rejected a team challenge", saved.getTitle(), saved.getId(), "MATCH", "/social");
        notifyMatchCaptain(saved.getHomeTeam(), "Challenge rejected", saved.getAwayTeam().getName() + " rejected " + saved.getTitle());
        return mapMatch(saved, true);
    }

    @Transactional
    public PublicMatchResponse scheduleMatch(String username, Long matchId, MatchScheduleRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Match schedule details are required.");
        }
        User actor = findUser(username);
        PublicMatch match = findMatch(matchId);
        assertMatchManager(match, actor);
        if (match.getStatus() == PublicMatchStatus.COMPLETED || match.getStatus() == PublicMatchStatus.CANCELLED || match.getStatus() == PublicMatchStatus.REJECTED) {
            throw ApiException.badRequest("This match can no longer be scheduled.");
        }
        if (request.getMatchDate() == null || request.getStartTime() == null) {
            throw ApiException.badRequest("Match date and start time are required.");
        }
        if (LocalDateTime.of(request.getMatchDate(), request.getStartTime()).isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest("Match cannot be scheduled in the past.");
        }
        if (request.getEndTime() != null && !request.getEndTime().isAfter(request.getStartTime())) {
            throw ApiException.badRequest("End time must be after start time.");
        }
        Turf turf = request.getTurfId() == null ? match.getTurf() : turfRepository.findById(request.getTurfId()).orElseThrow(() -> new ApiException("Turf not found"));
        match.setTurf(turf);
        match.setMatchDate(request.getMatchDate());
        match.setStartTime(request.getStartTime());
        match.setEndTime(request.getEndTime());
        if (request.getLocation() != null && !request.getLocation().isBlank()) {
            match.setLocation(cleanOptional(request.getLocation(), 180, "Location").orElse(match.getLocation()));
        } else if (turf != null) {
            match.setLocation(turf.getLocation());
        }
        match.setScheduledAt(LocalDateTime.now());
        if (match.getStatus() == PublicMatchStatus.PENDING) {
            match.setStatus(PublicMatchStatus.ACCEPTED);
        } else if (match.getStatus() == PublicMatchStatus.ACCEPTED || match.getStatus() == PublicMatchStatus.OPEN || match.getStatus() == PublicMatchStatus.FULL) {
            match.setStatus(PublicMatchStatus.SCHEDULED);
        }
        if (request.getNote() != null && !request.getNote().isBlank()) {
            match.setNotes(cleanOptional(request.getNote(), 1000, "Schedule note").orElse(match.getNotes()));
        }
        PublicMatch saved = publicMatchRepository.save(match);
        publishFeed(actor, FeedEventType.MATCH_CREATED, "scheduled a match", saved.getTitle(), saved.getId(), "MATCH", "/social");
        notifyMatchTeams(saved, "Match scheduled", saved.getTitle() + " is scheduled for " + saved.getMatchDate() + " at " + saved.getStartTime());
        return mapMatch(saved, true);
    }

    @Transactional
    public PublicMatchResponse startPublicMatch(String username, Long matchId) {
        User actor = findUser(username);
        PublicMatch match = findMatch(matchId);
        assertMatchManager(match, actor);
        if (!(match.getStatus() == PublicMatchStatus.SCHEDULED || match.getStatus() == PublicMatchStatus.OPEN || match.getStatus() == PublicMatchStatus.FULL)) {
            throw ApiException.badRequest("Only scheduled or open matches can be started.");
        }
        match.setStatus(PublicMatchStatus.LIVE);
        match.setStartedAt(LocalDateTime.now());
        PublicMatch saved = publicMatchRepository.save(match);
        publishFeed(actor, FeedEventType.MATCH_CREATED, "started a match", saved.getTitle(), saved.getId(), "MATCH", "/social");
        notifyMatchTeams(saved, "Match started", saved.getTitle() + " is now live.");
        return mapMatch(saved, true);
    }

    @Transactional
    public PublicMatchResponse cancelPublicMatch(String username, Long matchId) {
        User actor = findUser(username);
        PublicMatch match = findMatch(matchId);
        assertMatchManager(match, actor);
        if (match.getStatus() == PublicMatchStatus.COMPLETED || match.getStatus() == PublicMatchStatus.CANCELLED) {
            throw ApiException.badRequest("This match cannot be cancelled.");
        }
        match.setStatus(PublicMatchStatus.CANCELLED);
        PublicMatch saved = publicMatchRepository.save(match);
        publishFeed(actor, FeedEventType.MATCH_CREATED, "cancelled a match", saved.getTitle(), saved.getId(), "MATCH", "/social");
        notifyMatchTeams(saved, "Match cancelled", saved.getTitle() + " was cancelled.");
        return mapMatch(saved, true);
    }

    @Transactional
    public PublicMatchResponse submitMatchResult(String username, Long matchId, MatchResultRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Match result is required.");
        }
        User actor = findUser(username);
        PublicMatch match = findMatch(matchId);
        assertMatchManager(match, actor);
        if (match.getStatus() == PublicMatchStatus.CANCELLED || match.getStatus() == PublicMatchStatus.REJECTED) {
            throw ApiException.badRequest("Result cannot be submitted for this match.");
        }
        if (request.getHomeScore() == null || request.getAwayScore() == null || request.getHomeScore() < 0 || request.getAwayScore() < 0) {
            throw ApiException.badRequest("Both team scores are required and cannot be negative.");
        }
        Team winner = resolveWinnerTeam(match, request.getWinnerTeamId(), request.getHomeScore(), request.getAwayScore());
        match.setWinnerTeam(winner);
        match.setHomeScore(request.getHomeScore());
        match.setAwayScore(request.getAwayScore());
        match.setResultMargin(cleanOptional(request.getResultMargin(), 120, "Result margin").orElse(defaultResultMargin(match, request.getHomeScore(), request.getAwayScore())));
        match.setResultSummary(cleanOptional(request.getResultSummary(), 1000, "Result summary").orElse(null));
        match.setStatus(PublicMatchStatus.COMPLETED);
        match.setCompletedAt(LocalDateTime.now());
        PublicMatch saved = publicMatchRepository.save(match);
        publishFeed(actor, FeedEventType.MATCH_CREATED, "submitted a match result", saved.getTitle(), saved.getId(), "MATCH", "/social");
        notifyMatchTeams(saved, "Match result submitted", saved.getTitle() + " result is now available.");
        return mapMatch(saved, true);
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getMatchParticipants(Long matchId) {
        findMatch(matchId);
        return matchParticipantRepository.findByMatchIdOrderByJoinedAtAsc(matchId)
                .stream()
                .map(participant -> TeamMemberResponse.builder()
                        .id(participant.getId())
                        .userId(participant.getUser().getId())
                        .name(participant.getUser().getName())
                        .email(participant.getUser().getEmail())
                        .role(TeamMemberRole.PLAYER)
                        .status(TeamMemberStatus.ACTIVE)
                        .createdAt(participant.getJoinedAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicMatchResponse> getMyMatchHistory(String username) {
        User user = findUser(username);
        return matchParticipantRepository.findByUserIdOrderByJoinedAtDesc(user.getId())
                .stream()
                .map(MatchParticipant::getMatch)
                .distinct()
                .map(match -> mapMatch(match, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamMatchStatsResponse getTeamMatchStats(String username, Long teamId) {
        User viewer = findUser(username);
        Team team = findTeam(teamId);
        if (team.getVisibility() == TeamVisibility.PRIVATE) {
            assertActiveTeamMember(teamId, viewer.getId());
        }
        List<PublicMatch> matches = publicMatchRepository.findByHomeTeamIdOrAwayTeamIdOrderByMatchDateDescStartTimeDesc(teamId, teamId);
        List<PublicMatch> completed = matches.stream()
                .filter(match -> match.getStatus() == PublicMatchStatus.COMPLETED)
                .toList();
        long wins = completed.stream()
                .filter(match -> match.getWinnerTeam() != null && match.getWinnerTeam().getId().equals(teamId))
                .count();
        long draws = completed.stream()
                .filter(match -> match.getWinnerTeam() == null)
                .count();
        long losses = Math.max(0, completed.size() - wins - draws);
        long pendingChallenges = matches.stream()
                .filter(match -> match.getAwayTeam() != null && match.getAwayTeam().getId().equals(teamId) && match.getStatus() == PublicMatchStatus.PENDING)
                .count();
        long upcomingMatches = matches.stream()
                .filter(match -> match.getStatus() == PublicMatchStatus.PENDING
                        || match.getStatus() == PublicMatchStatus.ACCEPTED
                        || match.getStatus() == PublicMatchStatus.SCHEDULED
                        || match.getStatus() == PublicMatchStatus.LIVE)
                .count();
        int winPercentage = completed.isEmpty() ? 0 : (int) Math.round((wins * 100.0) / completed.size());
        return TeamMatchStatsResponse.builder()
                .teamId(team.getId())
                .teamName(team.getName())
                .matchesPlayed((long) completed.size())
                .wins(wins)
                .losses(losses)
                .draws(draws)
                .winPercentage(winPercentage)
                .pendingChallenges(pendingChallenges)
                .upcomingMatches(upcomingMatches)
                .recentMatches(matches.stream().limit(10).map(match -> mapMatch(match, false)).toList())
                .rivalries(buildRivalries(teamId, completed))
                .build();
    }

    @Transactional(readOnly = true)
    public List<PlayerDiscoverResponse> discoverPlayers(String username, String city, String position, String availability) {
        User actor = findUser(username);
        return userRepository.findAll()
                .stream()
                .filter(user -> !user.getId().equals(actor.getId()))
                .map(user -> mapPlayerDiscover(user, actor))
                .filter(item -> city == null || city.isBlank() || containsIgnoreCase(item.getCity(), city) || containsIgnoreCase(item.getAvailability() == null ? null : item.getAvailability().getHomeCity(), city))
                .filter(item -> position == null || position.isBlank() || containsIgnoreCase(item.getPosition(), position))
                .filter(item -> availability == null || availability.isBlank() || containsIgnoreCase(item.getPreferredTime(), availability) || containsIgnoreCase(item.getAvailability() == null ? null : item.getAvailability().getPreferredWindow(), availability))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlayerDiscoverResponse> nearbyPlayers(String username, String city, String availability) {
        User actor = findUser(username);
        String resolvedCity = city;
        if (resolvedCity == null || resolvedCity.isBlank()) {
            PlayerAvailability actorAvailability = playerAvailabilityRepository.findByUserId(actor.getId()).orElse(null);
            PlayerProfile actorProfile = playerProfileRepository.findByUserId(actor.getId()).orElse(null);
            resolvedCity = actorAvailability != null && actorAvailability.getHomeCity() != null
                    ? actorAvailability.getHomeCity()
                    : actorProfile == null ? null : actorProfile.getCity();
        }
        return discoverPlayers(username, resolvedCity, null, availability).stream()
                .sorted((left, right) -> Integer.compare(nullSafe(right.getMatchScore()), nullSafe(left.getMatchScore())))
                .limit(25)
                .toList();
    }

    @Transactional
    public CommunityDtos.PlayerConnectionResponse connectPlayer(String username, CommunityDtos.ConnectionRequest request) {
        User from = findUser(username);
        if (request == null || request.getUserId() == null) {
            throw ApiException.badRequest("Select a player to connect with.");
        }
        if (from.getId().equals(request.getUserId())) {
            throw ApiException.badRequest("You cannot connect with yourself.");
        }
        User to = userRepository.findById(request.getUserId()).orElseThrow(() -> ApiException.notFound("Player was not found."));
        PlayerConnectionType type = request.getType() == null ? PlayerConnectionType.FRIEND : request.getType();
        PlayerConnection connection = playerConnectionRepository.findByFromUserIdAndToUserIdAndType(from.getId(), to.getId(), type)
                .orElse(PlayerConnection.builder()
                        .fromUser(from)
                        .toUser(to)
                        .type(type)
                        .status(PlayerConnectionStatus.PENDING)
                        .build());
        connection.setMessage(request.getMessage());
        connection.setStatus(type == PlayerConnectionType.FOLLOW ? PlayerConnectionStatus.ACCEPTED : PlayerConnectionStatus.PENDING);
        PlayerConnection saved = playerConnectionRepository.save(connection);
        publishFeed(from, FeedEventType.PROFILE_UPDATED, type == PlayerConnectionType.FOLLOW ? "followed" : "sent a connection request to", to.getName(), to.getId(), "PLAYER", "/social");
        notificationService.pushNotification(
                to.getId(),
                to.getEmail(),
                from.getName() + " wants to connect",
                from.getName() + " sent you a Cricbook network request.",
                com.example.Cricbook.enums.NotificationChannel.PUSH,
                null,
                "/social"
        );
        return mapConnection(saved);
    }

    @Transactional
    public CommunityDtos.PlayerConnectionResponse respondConnection(String username, Long connectionId, CommunityDtos.ConnectionDecisionRequest request) {
        User user = findUser(username);
        PlayerConnection connection = playerConnectionRepository.findById(connectionId)
                .orElseThrow(() -> ApiException.notFound("Connection request was not found."));
        if (!connection.getToUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("Only the requested player can respond.");
        }
        connection.setStatus(request != null && request.isAccept() ? PlayerConnectionStatus.ACCEPTED : PlayerConnectionStatus.DECLINED);
        PlayerConnection saved = playerConnectionRepository.save(connection);
        if (saved.getStatus() == PlayerConnectionStatus.ACCEPTED) {
            publishFeed(user, FeedEventType.PROFILE_UPDATED, "connected with", saved.getFromUser().getName(), saved.getFromUser().getId(), "PLAYER", "/social");
        }
        return mapConnection(saved);
    }

    @Transactional(readOnly = true)
    public List<CommunityDtos.PlayerConnectionResponse> myNetwork(String username) {
        User user = findUser(username);
        return playerConnectionRepository.findByFromUserIdOrToUserIdOrderByCreatedAtDesc(user.getId(), user.getId())
                .stream()
                .map(this::mapConnection)
                .toList();
    }

    @Transactional
    public CommunityDtos.RecruitmentResponse createRecruitment(String username, CommunityDtos.RecruitmentRequest request) {
        User captain = findUser(username);
        if (request == null || request.getTeamId() == null) {
            throw ApiException.badRequest("Team is required for recruitment.");
        }
        Team team = findTeam(request.getTeamId());
        assertCaptain(team, captain);
        TeamRecruitment saved = teamRecruitmentRepository.save(TeamRecruitment.builder()
                .team(team)
                .captain(captain)
                .roleNeeded(required(request.getRoleNeeded(), "Role needed is required."))
                .skillLevel(defaultValue(request.getSkillLevel(), "Intermediate"))
                .city(defaultValue(request.getCity(), team.getCity()))
                .availability(request.getAvailability())
                .description(request.getDescription())
                .matchDate(request.getMatchDate())
                .status(TeamRecruitmentStatus.OPEN)
                .build());
        publishFeed(captain, FeedEventType.TEAM_INVITE_SENT, "opened recruitment for", team.getName(), team.getId(), "TEAM", "/social");
        return mapRecruitment(saved, true);
    }

    @Transactional(readOnly = true)
    public List<CommunityDtos.RecruitmentResponse> openRecruitments(String city, String role) {
        return teamRecruitmentRepository.findByStatusOrderByCreatedAtDesc(TeamRecruitmentStatus.OPEN)
                .stream()
                .filter(item -> city == null || city.isBlank() || containsIgnoreCase(item.getCity(), city))
                .filter(item -> role == null || role.isBlank() || containsIgnoreCase(item.getRoleNeeded(), role))
                .map(item -> mapRecruitment(item, false))
                .toList();
    }

    @Transactional
    public CommunityDtos.RecruitmentApplicationResponse applyRecruitment(String username, Long recruitmentId, CommunityDtos.RecruitmentApplyRequest request) {
        User user = findUser(username);
        TeamRecruitment recruitment = teamRecruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> ApiException.notFound("Recruitment post was not found."));
        if (recruitment.getStatus() != TeamRecruitmentStatus.OPEN) {
            throw ApiException.badRequest("This recruitment post is not open.");
        }
        if (recruitment.getCaptain().getId().equals(user.getId())) {
            throw ApiException.badRequest("Captain cannot apply to their own recruitment post.");
        }
        RecruitmentApplication application = recruitmentApplicationRepository.findByRecruitmentIdAndUserId(recruitmentId, user.getId())
                .orElse(RecruitmentApplication.builder()
                        .recruitment(recruitment)
                        .user(user)
                        .status(RecruitmentApplicationStatus.PENDING)
                        .build());
        application.setMessage(request == null ? null : request.getMessage());
        application.setStatus(RecruitmentApplicationStatus.PENDING);
        RecruitmentApplication saved = recruitmentApplicationRepository.save(application);
        notificationService.pushNotification(
                recruitment.getCaptain().getId(),
                recruitment.getCaptain().getEmail(),
                user.getName() + " applied for " + recruitment.getRoleNeeded(),
                user.getName() + " wants to join " + recruitment.getTeam().getName() + ".",
                com.example.Cricbook.enums.NotificationChannel.PUSH,
                null,
                "/social"
        );
        return mapApplication(saved);
    }

    @Transactional
    public CommunityDtos.RecruitmentApplicationResponse respondRecruitmentApplication(String username, Long applicationId, CommunityDtos.ConnectionDecisionRequest request) {
        User captain = findUser(username);
        RecruitmentApplication application = recruitmentApplicationRepository.findById(applicationId)
                .orElseThrow(() -> ApiException.notFound("Recruitment application was not found."));
        if (!application.getRecruitment().getCaptain().getId().equals(captain.getId())) {
            throw ApiException.forbidden("Only the recruitment captain can respond.");
        }
        application.setStatus(request != null && request.isAccept() ? RecruitmentApplicationStatus.ACCEPTED : RecruitmentApplicationStatus.DECLINED);
        RecruitmentApplication saved = recruitmentApplicationRepository.save(application);
        return mapApplication(saved);
    }

    @Transactional(readOnly = true)
    public CommunityDtos.CommunityAnalyticsResponse communityAnalytics() {
        long discoverablePlayers = Math.max(0, userRepository.count() - 1);
        long acceptedConnections = playerConnectionRepository.countByStatus(PlayerConnectionStatus.ACCEPTED);
        long pendingConnections = playerConnectionRepository.countByStatus(PlayerConnectionStatus.PENDING);
        long openRecruitments = teamRecruitmentRepository.countByStatus(TeamRecruitmentStatus.OPEN);
        long pendingApplications = recruitmentApplicationRepository.countByStatus(RecruitmentApplicationStatus.PENDING);
        long feedEvents = feedEventRepository.count();
        long activePosts = feedEventRepository.countByStatus("ACTIVE");
        long moderationItems = communityModerationRepository.count();
        long comments = activePosts == 0 ? 0 : feedEventRepository.findTop50ByStatusOrderByCreatedAtDesc("ACTIVE").stream()
                .mapToLong(feed -> feedCommentRepository.countByFeedEventIdAndStatus(feed.getId(), "ACTIVE"))
                .sum();
        int engagementRate = activePosts == 0 ? 0 : (int) Math.min(100, Math.round(((double) (comments + acceptedConnections) / activePosts) * 10));
        int networkActivity = (int) (acceptedConnections + openRecruitments + feedEvents + comments);
        return CommunityDtos.CommunityAnalyticsResponse.builder()
                .discoverablePlayers(discoverablePlayers)
                .acceptedConnections(acceptedConnections)
                .pendingConnections(pendingConnections)
                .openRecruitments(openRecruitments)
                .pendingApplications(pendingApplications)
                .feedEvents(feedEvents)
                .activePosts(activePosts)
                .comments(comments)
                .moderationItems(moderationItems)
                .engagementRate(engagementRate)
                .insight(networkActivity > 20 ? "Community network activity is strong." : "Grow engagement by opening recruitments and inviting nearby players.")
                .build();
    }

    @Transactional(readOnly = true)
    public List<FeedEventResponse> getFeed(String username, String scope, String query, String sort, String entityType, Long entityId) {
        User viewer = username == null ? null : findUser(username);
        String cleanQuery = cleanSearch(query);
        String cleanScope = normalizeFeedVisibility(scope);
        List<FeedEvent> feed;
        if (entityType != null && entityId != null) {
            feed = feedEventRepository.findTop25ByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType.toUpperCase(Locale.ROOT), entityId);
        } else if ("TRENDING".equalsIgnoreCase(sort)) {
            feed = feedEventRepository.findTop50ByStatusOrderByEngagementScoreDescCreatedAtDesc("ACTIVE");
        } else if (cleanQuery != null || cleanScope != null) {
            feed = feedEventRepository.searchActiveFeed(cleanScope, cleanQuery);
        } else {
            feed = feedEventRepository.findTop50ByStatusOrderByCreatedAtDesc("ACTIVE");
        }
        return feed.stream()
                .filter(event -> canViewFeedEvent(viewer, event))
                .limit(50)
                .map(event -> mapFeedEvent(event, viewer))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeedEventResponse> getFeed() {
        return getFeed(null, null, null, null, null, null);
    }

    @Transactional
    public FeedEventResponse createFeedPost(String username, CommunityDtos.FeedPostRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Post details are required.");
        }
        User actor = findUser(username);
        String content = cleanRequired(request.getContent(), "Post content is required.", 1, 1000);
        String title = cleanOptional(request.getTitle(), 120, "Post title").orElse("Community update");
        String visibility = normalizeFeedVisibility(defaultValue(request.getVisibility(), "PUBLIC"));
        Long entityId = resolveFeedEntityId(actor, visibility, request);
        String entityType = resolveFeedEntityType(visibility, request);
        FeedEvent post = feedEventRepository.save(FeedEvent.builder()
                .actor(actor)
                .eventType(FeedEventType.SOCIAL_POST_CREATED)
                .title(title)
                .summary(content)
                .visibility(visibility)
                .status("ACTIVE")
                .mediaUrls(joinMediaUrls(request.getMediaUrls()))
                .entityId(entityId)
                .entityType(entityType)
                .deepLink("/feed")
                .build());
        return mapFeedEvent(post, actor);
    }

    @Transactional
    public FeedEventResponse editFeedPost(String username, Long postId, CommunityDtos.FeedPostRequest request) {
        User actor = findUser(username);
        FeedEvent post = findFeedPost(postId);
        assertFeedOwner(post, actor);
        if (!"ACTIVE".equalsIgnoreCase(post.getStatus())) {
            throw ApiException.badRequest("Deleted or hidden posts cannot be edited.");
        }
        if (request != null && request.getTitle() != null) {
            post.setTitle(cleanRequired(request.getTitle(), "Post title is required.", 1, 120));
        }
        if (request != null && request.getContent() != null) {
            post.setSummary(cleanRequired(request.getContent(), "Post content is required.", 1, 1000));
        }
        if (request != null && request.getVisibility() != null) {
            post.setVisibility(normalizeFeedVisibility(request.getVisibility()));
        }
        if (request != null && request.getMediaUrls() != null) {
            post.setMediaUrls(joinMediaUrls(request.getMediaUrls()));
        }
        post.setEventType(FeedEventType.SOCIAL_POST_EDITED);
        post.setEditedAt(LocalDateTime.now());
        updateFeedEngagement(post);
        return mapFeedEvent(feedEventRepository.save(post), actor);
    }

    @Transactional
    public void deleteFeedPost(String username, Long postId) {
        User actor = findUser(username);
        FeedEvent post = findFeedPost(postId);
        assertFeedOwner(post, actor);
        if ("DELETED".equalsIgnoreCase(post.getStatus())) {
            throw ApiException.badRequest("Post is already deleted.");
        }
        post.setStatus("DELETED");
        post.setEditedAt(LocalDateTime.now());
        feedEventRepository.save(post);
    }

    @Transactional
    public FeedEventResponse reactToFeedPost(String username, Long postId, CommunityDtos.FeedReactionRequest request) {
        User actor = findUser(username);
        FeedEvent post = findActiveFeedPost(postId);
        if (!canViewFeedEvent(actor, post)) {
            throw ApiException.notFound("Post was not found.");
        }
        String reactionType = normalizeReaction(request == null ? null : request.getReactionType());
        FeedReaction reaction = feedReactionRepository.findByFeedEventIdAndUserId(postId, actor.getId())
                .orElseGet(() -> FeedReaction.builder().feedEvent(post).user(actor).build());
        reaction.setReactionType(reactionType);
        feedReactionRepository.save(reaction);
        post.setReactionCount((int) feedReactionRepository.countByFeedEventId(postId));
        post.setEventType(FeedEventType.SOCIAL_POST_REACTED);
        updateFeedEngagement(post);
        FeedEvent saved = feedEventRepository.save(post);
        notifyPostOwner(saved, actor, "New reaction", actor.getName() + " reacted to your post.");
        return mapFeedEvent(saved, actor);
    }

    @Transactional
    public FeedEventResponse removeFeedReaction(String username, Long postId) {
        User actor = findUser(username);
        FeedEvent post = findActiveFeedPost(postId);
        feedReactionRepository.deleteByFeedEventIdAndUserId(postId, actor.getId());
        post.setReactionCount((int) feedReactionRepository.countByFeedEventId(postId));
        updateFeedEngagement(post);
        return mapFeedEvent(feedEventRepository.save(post), actor);
    }

    @Transactional(readOnly = true)
    public List<CommunityDtos.FeedCommentResponse> getFeedComments(String username, Long postId) {
        User actor = findUser(username);
        FeedEvent post = findActiveFeedPost(postId);
        if (!canViewFeedEvent(actor, post)) {
            throw ApiException.notFound("Post was not found.");
        }
        return feedCommentRepository.findTop50ByFeedEventIdAndStatusOrderByCreatedAtAsc(postId, "ACTIVE")
                .stream()
                .map(this::mapFeedComment)
                .toList();
    }

    @Transactional
    public CommunityDtos.FeedCommentResponse addFeedComment(String username, Long postId, CommunityDtos.FeedCommentRequest request) {
        User actor = findUser(username);
        FeedEvent post = findActiveFeedPost(postId);
        if (!canViewFeedEvent(actor, post)) {
            throw ApiException.notFound("Post was not found.");
        }
        String message = cleanRequired(request == null ? null : request.getMessage(), "Comment is required.", 1, 1000);
        FeedComment comment = feedCommentRepository.save(FeedComment.builder()
                .feedEvent(post)
                .author(actor)
                .parentCommentId(request == null ? null : request.getParentCommentId())
                .message(message)
                .status("ACTIVE")
                .build());
        post.setCommentCount((int) feedCommentRepository.countByFeedEventIdAndStatus(postId, "ACTIVE"));
        post.setEventType(FeedEventType.SOCIAL_COMMENTED);
        updateFeedEngagement(post);
        feedEventRepository.save(post);
        notifyPostOwner(post, actor, "New comment", actor.getName() + " commented on your post.");
        return mapFeedComment(comment);
    }

    @Transactional
    public CommunityDtos.FeedCommentResponse editFeedComment(String username, Long commentId, CommunityDtos.FeedCommentRequest request) {
        User actor = findUser(username);
        FeedComment comment = feedCommentRepository.findById(commentId)
                .orElseThrow(() -> ApiException.notFound("Comment was not found."));
        if (!comment.getAuthor().getId().equals(actor.getId())) {
            throw ApiException.forbidden("Only the comment author can edit this comment.");
        }
        if (!"ACTIVE".equalsIgnoreCase(comment.getStatus())) {
            throw ApiException.badRequest("Deleted comments cannot be edited.");
        }
        comment.setMessage(cleanRequired(request == null ? null : request.getMessage(), "Comment is required.", 1, 1000));
        comment.setEditedAt(LocalDateTime.now());
        return mapFeedComment(feedCommentRepository.save(comment));
    }

    @Transactional
    public void deleteFeedComment(String username, Long commentId) {
        User actor = findUser(username);
        FeedComment comment = feedCommentRepository.findById(commentId)
                .orElseThrow(() -> ApiException.notFound("Comment was not found."));
        if (!comment.getAuthor().getId().equals(actor.getId())) {
            throw ApiException.forbidden("Only the comment author can delete this comment.");
        }
        comment.setStatus("DELETED");
        comment.setEditedAt(LocalDateTime.now());
        feedCommentRepository.save(comment);
        FeedEvent post = comment.getFeedEvent();
        post.setCommentCount((int) feedCommentRepository.countByFeedEventIdAndStatus(post.getId(), "ACTIVE"));
        updateFeedEngagement(post);
        feedEventRepository.save(post);
    }

    @Transactional
    public void reportContent(String username, CommunityDtos.ContentReportRequest request) {
        User actor = findUser(username);
        String contentType = cleanRequired(request == null ? null : request.getContentType(), "Content type is required.", 2, 60).toUpperCase(Locale.ROOT);
        Long contentId = requiredObject(request == null ? null : request.getContentId(), "Content ID is required.");
        String reason = cleanRequired(request == null ? null : request.getReason(), "Report reason is required.", 2, 800);
        communityModerationRepository.save(CommunityModeration.builder()
                .contentType(contentType)
                .contentId(contentId)
                .action("REPORT")
                .moderatorId(actor.getEmail())
                .reason(reason + cleanOptional(request == null ? null : request.getDetails(), 300, "Report details").map(details -> " - " + details).orElse(""))
                .build());
        if ("POST".equals(contentType)) {
            FeedEvent post = findActiveFeedPost(contentId);
            post.setEventType(FeedEventType.SOCIAL_POST_REPORTED);
            feedEventRepository.save(post);
        }
    }

    @Transactional
    public void moderateContent(String username, CommunityDtos.ContentReportRequest request) {
        User admin = findUser(username);
        String contentType = cleanRequired(request == null ? null : request.getContentType(), "Content type is required.", 2, 60).toUpperCase(Locale.ROOT);
        Long contentId = requiredObject(request == null ? null : request.getContentId(), "Content ID is required.");
        String action = cleanRequired(request == null ? null : request.getReason(), "Moderation action is required.", 2, 60).toUpperCase(Locale.ROOT);
        String details = cleanOptional(request == null ? null : request.getDetails(), 800, "Moderation details").orElse(action);
        if ("POST".equals(contentType)) {
            FeedEvent post = findFeedPost(contentId);
            post.setStatus("DELETE".equals(action) || "DELETED".equals(action) ? "DELETED" : "HIDDEN");
            post.setEditedAt(LocalDateTime.now());
            feedEventRepository.save(post);
        } else if ("COMMENT".equals(contentType)) {
            FeedComment comment = feedCommentRepository.findById(contentId)
                    .orElseThrow(() -> ApiException.notFound("Comment was not found."));
            comment.setStatus("DELETED");
            comment.setEditedAt(LocalDateTime.now());
            feedCommentRepository.save(comment);
        }
        communityModerationRepository.save(CommunityModeration.builder()
                .contentType(contentType)
                .contentId(contentId)
                .action(action)
                .moderatorId(admin.getEmail())
                .reason(details)
                .build());
    }

    @Transactional
    public ChatMessageResponse sendTeamMessage(String username, Long teamId, ChatMessageRequest request) {
        User user = findUser(username);
        assertActiveTeamMember(teamId, user.getId());
        findTeam(teamId);
        return saveMessage(user, ChatRoomType.TEAM, teamId, request);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getTeamMessages(String username, Long teamId) {
        User user = findUser(username);
        assertActiveTeamMember(teamId, user.getId());
        return getMessages(ChatRoomType.TEAM, teamId);
    }

    @Transactional
    public ChatMessageResponse sendMatchMessage(String username, Long matchId, ChatMessageRequest request) {
        User user = findUser(username);
        assertMatchParticipant(matchId, user.getId());
        return saveMessage(user, ChatRoomType.MATCH, matchId, request);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMatchMessages(String username, Long matchId) {
        User user = findUser(username);
        assertMatchParticipant(matchId, user.getId());
        return getMessages(ChatRoomType.MATCH, matchId);
    }

    @Transactional
    public ChatMessageResponse sendDirectMessage(String username, Long recipientId, ChatMessageRequest request) {
        User sender = findUser(username);
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> ApiException.notFound("Recipient was not found."));
        if (sender.getId().equals(recipient.getId())) {
            throw ApiException.badRequest("You cannot send a direct message to yourself.");
        }
        ChatMessageResponse response = saveMessage(sender, ChatRoomType.DIRECT, conversationId(sender.getId(), recipient.getId()), request);
        notificationService.pushNotification(
                recipient.getId(),
                recipient.getEmail(),
                "New direct message",
                sender.getName() + " sent you a message on Cricbook.",
                NotificationChannel.PUSH,
                null,
                "/social",
                "SOCIAL",
                "NORMAL"
        );
        return response;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getDirectMessages(String username, Long recipientId) {
        User viewer = findUser(username);
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> ApiException.notFound("Recipient was not found."));
        return getMessages(ChatRoomType.DIRECT, conversationId(viewer.getId(), recipient.getId()));
    }

    private ChatMessageResponse saveMessage(User user, ChatRoomType roomType, Long roomId, ChatMessageRequest request) {
        if (request == null) {
            throw new ApiException("Message is required");
        }
        String messageType = normalizeMessageType(request.getMessageType(), request.getMediaUrl());
        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                .roomType(roomType)
                .roomId(roomId)
                .sender(user)
                .message(cleanRequired(request.getMessage(), "Message is required", 1, 1000))
                .messageType(messageType)
                .mediaUrl(cleanOptional(request.getMediaUrl(), 500, "Media URL").orElse(null))
                .deliveredAt(LocalDateTime.now())
                .build());
        return mapMessage(message);
    }

    private List<ChatMessageResponse> getMessages(ChatRoomType roomType, Long roomId) {
        return chatMessageRepository.findTop50ByRoomTypeAndRoomIdOrderByCreatedAtAsc(roomType, roomId)
                .stream()
                .map(this::mapMessage)
                .toList();
    }

    private TeamResponse mapTeam(Team team, boolean includeMembers) {
        int activeMembers = (int) teamMemberRepository.countByTeamIdAndStatus(team.getId(), TeamMemberStatus.ACTIVE);
        List<TeamMemberResponse> members = includeMembers
                ? teamMemberRepository.findByTeamIdAndStatusOrderByCreatedAtAsc(team.getId(), TeamMemberStatus.ACTIVE).stream().map(this::mapMember).toList()
                : List.of();
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .sport(team.getSport())
                .city(team.getCity())
                .description(team.getDescription())
                .logoUrl(team.getLogoUrl())
                .visibility(team.getVisibility())
                .maxMembers(team.getMaxMembers())
                .memberCount(activeMembers)
                .openSpots(Math.max(team.getMaxMembers() - activeMembers, 0))
                .reputationScore(teamReputationScore(team, activeMembers))
                .captainId(team.getCaptain().getId())
                .captainName(team.getCaptain().getName())
                .members(members)
                .createdAt(team.getCreatedAt())
                .build();
    }

    private TeamMemberResponse mapMember(TeamMember member) {
        return TeamMemberResponse.builder()
                .id(member.getId())
                .userId(member.getUser().getId())
                .name(member.getUser().getName())
                .email(member.getUser().getEmail())
                .role(normalizeRole(member.getRole()))
                .status(member.getStatus())
                .createdAt(member.getCreatedAt())
                .build();
    }

    private TeamInviteResponse mapInvite(TeamInvite invite) {
        return TeamInviteResponse.builder()
                .id(invite.getId())
                .teamId(invite.getTeam().getId())
                .teamName(invite.getTeam().getName())
                .senderId(invite.getSender().getId())
                .senderName(invite.getSender().getName())
                .receiverId(invite.getReceiver().getId())
                .receiverName(invite.getReceiver().getName())
                .status(invite.getStatus())
                .createdAt(invite.getCreatedAt())
                .build();
    }

    private TeamJoinRequestResponse mapJoinRequest(TeamJoinRequest request) {
        return TeamJoinRequestResponse.builder()
                .id(request.getId())
                .teamId(request.getTeam().getId())
                .teamName(request.getTeam().getName())
                .requesterId(request.getRequester().getId())
                .requesterName(request.getRequester().getName())
                .reviewerId(request.getReviewer() == null ? null : request.getReviewer().getId())
                .reviewerName(request.getReviewer() == null ? null : request.getReviewer().getName())
                .message(request.getMessage())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .build();
    }

    private TeamMemberRole normalizeRole(TeamMemberRole role) {
        if (role == TeamMemberRole.MEMBER) {
            return TeamMemberRole.PLAYER;
        }
        return role;
    }

    private PublicMatchResponse mapMatch(PublicMatch match, boolean includeParticipants) {
        return PublicMatchResponse.builder()
                .id(match.getId())
                .creatorId(match.getCreator().getId())
                .creatorName(match.getCreator().getName())
                .turfId(match.getTurf() == null ? null : match.getTurf().getId())
                .turfName(match.getTurf() == null ? null : match.getTurf().getName())
                .title(match.getTitle())
                .sport(match.getSport())
                .city(match.getCity())
                .location(match.getLocation())
                .matchDate(match.getMatchDate())
                .startTime(match.getStartTime())
                .endTime(match.getEndTime())
                .maxPlayers(match.getMaxPlayers())
                .joinedPlayers((int) matchParticipantRepository.countByMatchId(match.getId()))
                .visibility(match.getVisibility())
                .format(match.getFormat())
                .overs(match.getOvers())
                .entryFee(match.getEntryFee())
                .homeTeamId(match.getHomeTeam() == null ? null : match.getHomeTeam().getId())
                .homeTeamName(match.getHomeTeam() == null ? null : match.getHomeTeam().getName())
                .awayTeamId(match.getAwayTeam() == null ? null : match.getAwayTeam().getId())
                .awayTeamName(match.getAwayTeam() == null ? null : match.getAwayTeam().getName())
                .winnerTeamId(match.getWinnerTeam() == null ? null : match.getWinnerTeam().getId())
                .winnerTeamName(match.getWinnerTeam() == null ? null : match.getWinnerTeam().getName())
                .homeScore(match.getHomeScore())
                .awayScore(match.getAwayScore())
                .resultMargin(match.getResultMargin())
                .challengeMessage(match.getChallengeMessage())
                .notes(match.getNotes())
                .resultSummary(match.getResultSummary())
                .status(match.getStatus())
                .participants(includeParticipants ? getMatchParticipants(match.getId()) : List.of())
                .acceptedAt(match.getAcceptedAt())
                .scheduledAt(match.getScheduledAt())
                .startedAt(match.getStartedAt())
                .completedAt(match.getCompletedAt())
                .createdAt(match.getCreatedAt())
                .build();
    }

    private ChatMessageResponse mapMessage(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .roomType(message.getRoomType())
                .roomId(message.getRoomId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getName())
                .message(message.getMessage())
                .messageType(message.getMessageType())
                .mediaUrl(message.getMediaUrl())
                .deliveredAt(message.getDeliveredAt())
                .readAt(message.getReadAt())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private PlayerDiscoverResponse mapPlayerDiscover(User user, User actor) {
        PlayerProfile profile = playerProfileRepository.findByUserId(user.getId()).orElse(null);
        PlayerAvailability availability = playerAvailabilityRepository.findByUserId(user.getId()).orElse(null);
        Integer trustScore = playerTrustScoreRepository.findByUserId(user.getId()).map(PlayerTrustScore::getScore).orElse(70);
        Integer networkScore = (int) Math.min(100, 40 + playerConnectionRepository.countByFromUserIdOrToUserId(user.getId(), user.getId()) * 10);
        Integer matchScore = calculateMatchScore(profile, availability, trustScore, networkScore, actor);
        return PlayerDiscoverResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .city(profile == null ? null : profile.getCity())
                .preferredTime(profile == null ? null : profile.getPreferredTime())
                .battingStyle(profile == null || profile.getBattingStyle() == null ? null : profile.getBattingStyle().name())
                .bowlingStyle(profile == null || profile.getBowlingStyle() == null ? null : profile.getBowlingStyle().name())
                .position(profile == null || profile.getPosition() == null ? null : profile.getPosition().name())
                .experience(profile == null ? null : profile.getExperience())
                .skillLevel(resolveSkillLevel(profile))
                .trustScore(trustScore)
                .networkScore(networkScore)
                .matchScore(matchScore)
                .connectionStatus(resolveConnectionStatus(actor.getId(), user.getId()))
                .recommendationReason(buildRecommendationReason(profile, availability, trustScore))
                .rating(profile == null ? 0.0 : profile.getRating())
                .matches(profile == null ? 0 : profile.getMatchesPlayed())
                .wins(profile == null ? 0 : profile.getWins())
                .availability(availability == null ? null : PlayerAvailabilityResponse.builder()
                        .id(availability.getId())
                        .userId(user.getId())
                        .userName(user.getName())
                        .availableDays(availability.getAvailableDays())
                        .preferredWindow(availability.getPreferredWindow())
                        .homeCity(availability.getHomeCity())
                        .canTravel(availability.getCanTravel())
                        .notes(availability.getNotes())
                        .updatedAt(availability.getUpdatedAt())
                        .build())
                .build();
    }

    private CommunityDtos.PlayerConnectionResponse mapConnection(PlayerConnection connection) {
        return CommunityDtos.PlayerConnectionResponse.builder()
                .id(connection.getId())
                .fromUserId(connection.getFromUser().getId())
                .fromUserName(connection.getFromUser().getName())
                .toUserId(connection.getToUser().getId())
                .toUserName(connection.getToUser().getName())
                .type(connection.getType())
                .status(connection.getStatus())
                .message(connection.getMessage())
                .createdAt(connection.getCreatedAt())
                .build();
    }

    private CommunityDtos.RecruitmentResponse mapRecruitment(TeamRecruitment recruitment, boolean includeApplications) {
        List<CommunityDtos.RecruitmentApplicationResponse> applications = includeApplications
                ? recruitmentApplicationRepository.findByRecruitmentIdOrderByCreatedAtDesc(recruitment.getId()).stream().map(this::mapApplication).toList()
                : List.of();
        return CommunityDtos.RecruitmentResponse.builder()
                .id(recruitment.getId())
                .teamId(recruitment.getTeam().getId())
                .teamName(recruitment.getTeam().getName())
                .captainId(recruitment.getCaptain().getId())
                .captainName(recruitment.getCaptain().getName())
                .roleNeeded(recruitment.getRoleNeeded())
                .skillLevel(recruitment.getSkillLevel())
                .city(recruitment.getCity())
                .availability(recruitment.getAvailability())
                .description(recruitment.getDescription())
                .matchDate(recruitment.getMatchDate())
                .status(recruitment.getStatus())
                .applicationCount(recruitmentApplicationRepository.findByRecruitmentIdOrderByCreatedAtDesc(recruitment.getId()).size())
                .applications(applications)
                .createdAt(recruitment.getCreatedAt())
                .build();
    }

    private CommunityDtos.RecruitmentApplicationResponse mapApplication(RecruitmentApplication application) {
        return CommunityDtos.RecruitmentApplicationResponse.builder()
                .id(application.getId())
                .recruitmentId(application.getRecruitment().getId())
                .userId(application.getUser().getId())
                .userName(application.getUser().getName())
                .status(application.getStatus())
                .message(application.getMessage())
                .createdAt(application.getCreatedAt())
                .build();
    }

    private Integer calculateMatchScore(PlayerProfile profile, PlayerAvailability availability, Integer trustScore, Integer networkScore, User actor) {
        int score = 30;
        score += Math.min(25, trustScore / 4);
        score += Math.min(15, networkScore / 8);
        if (profile != null && profile.getRating() != null) score += Math.min(15, (int) Math.round(profile.getRating() * 3));
        PlayerAvailability actorAvailability = playerAvailabilityRepository.findByUserId(actor.getId()).orElse(null);
        if (actorAvailability != null && availability != null && containsIgnoreCase(availability.getHomeCity(), actorAvailability.getHomeCity())) score += 15;
        return Math.min(100, score);
    }

    private String resolveSkillLevel(PlayerProfile profile) {
        if (profile == null || profile.getRating() == null) return "Beginner";
        if (profile.getRating() >= 4.5) return "Professional";
        if (profile.getRating() >= 3.8) return "Advanced";
        if (profile.getRating() >= 2.5) return "Intermediate";
        return "Beginner";
    }

    private String resolveConnectionStatus(Long actorId, Long userId) {
        return playerConnectionRepository.findByFromUserIdOrToUserIdOrderByCreatedAtDesc(actorId, actorId).stream()
                .filter(connection -> connection.getFromUser().getId().equals(userId) || connection.getToUser().getId().equals(userId))
                .findFirst()
                .map(connection -> connection.getStatus().name())
                .orElse("NONE");
    }

    private String buildRecommendationReason(PlayerProfile profile, PlayerAvailability availability, Integer trustScore) {
        if (trustScore >= 90) return "High trust player with strong reliability.";
        if (availability != null && availability.getPreferredWindow() != null) return "Availability matches " + availability.getPreferredWindow() + " cricket.";
        if (profile != null && profile.getPosition() != null) return "Useful " + profile.getPosition().name().replace("_", " ").toLowerCase() + " option for team balance.";
        return "Flexible cricket player for community matches.";
    }

    private FeedEventResponse mapFeedEvent(FeedEvent event) {
        return mapFeedEvent(event, null);
    }

    private FeedEventResponse mapFeedEvent(FeedEvent event, User viewer) {
        return FeedEventResponse.builder()
                .id(event.getId())
                .actorId(event.getActor().getId())
                .actorName(event.getActor().getName())
                .eventType(event.getEventType())
                .title(event.getTitle())
                .summary(event.getSummary())
                .entityId(event.getEntityId())
                .entityType(event.getEntityType())
                .deepLink(event.getDeepLink())
                .visibility(event.getVisibility())
                .status(event.getStatus())
                .mediaUrls(splitMediaUrls(event.getMediaUrls()))
                .reactionCount(event.getReactionCount())
                .commentCount(event.getCommentCount())
                .shareCount(event.getShareCount())
                .viewCount(event.getViewCount())
                .engagementScore(event.getEngagementScore())
                .viewerReaction(viewer == null ? null : feedReactionRepository.findByFeedEventIdAndUserId(event.getId(), viewer.getId()).map(FeedReaction::getReactionType).orElse(null))
                .ownPost(viewer != null && event.getActor().getId().equals(viewer.getId()))
                .editedAt(event.getEditedAt())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private void publishFeed(User actor, FeedEventType type, String action, String subject, Long entityId, String entityType, String deepLink) {
        feedEventRepository.save(FeedEvent.builder()
                .actor(actor)
                .eventType(type)
                .title(actor.getName() + " " + action)
                .summary(actor.getName() + " " + action + " " + subject)
                .entityId(entityId)
                .entityType(entityType)
                .deepLink(deepLink)
                .visibility("PUBLIC")
                .status("ACTIVE")
                .build());
    }

    private CommunityDtos.FeedCommentResponse mapFeedComment(FeedComment comment) {
        return CommunityDtos.FeedCommentResponse.builder()
                .id(comment.getId())
                .feedEventId(comment.getFeedEvent().getId())
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getName())
                .parentCommentId(comment.getParentCommentId())
                .message(comment.getMessage())
                .status(comment.getStatus())
                .editedAt(comment.getEditedAt())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    private FeedEvent findFeedPost(Long postId) {
        return feedEventRepository.findById(postId)
                .orElseThrow(() -> ApiException.notFound("Post was not found."));
    }

    private FeedEvent findActiveFeedPost(Long postId) {
        FeedEvent post = findFeedPost(postId);
        if (!"ACTIVE".equalsIgnoreCase(post.getStatus())) {
            throw ApiException.notFound("Post was not found.");
        }
        return post;
    }

    private void assertFeedOwner(FeedEvent post, User actor) {
        if (!post.getActor().getId().equals(actor.getId())) {
            throw ApiException.forbidden("Only the post author can change this post.");
        }
    }

    private boolean canViewFeedEvent(User viewer, FeedEvent event) {
        String visibility = defaultValue(event.getVisibility(), "PUBLIC").toUpperCase(Locale.ROOT);
        if ("PUBLIC".equals(visibility)) {
            return true;
        }
        if (viewer == null) {
            return false;
        }
        if (event.getActor().getId().equals(viewer.getId())) {
            return true;
        }
        if ("TEAM".equals(visibility)) {
            return event.getEntityId() != null && teamMemberRepository.existsByTeamIdAndUserIdAndStatus(event.getEntityId(), viewer.getId(), TeamMemberStatus.ACTIVE);
        }
        if ("FOLLOWERS".equals(visibility)) {
            return playerConnectionRepository.findByFromUserIdAndToUserIdAndType(viewer.getId(), event.getActor().getId(), PlayerConnectionType.FOLLOW)
                    .filter(connection -> connection.getStatus() == PlayerConnectionStatus.ACCEPTED || connection.getStatus() == PlayerConnectionStatus.PENDING)
                    .isPresent();
        }
        return false;
    }

    private Long resolveFeedEntityId(User actor, String visibility, CommunityDtos.FeedPostRequest request) {
        if ("TEAM".equals(visibility)) {
            Long teamId = requiredObject(request.getTeamId(), "Team ID is required for team-only posts.");
            assertActiveTeamMember(teamId, actor.getId());
            return teamId;
        }
        if (request.getMatchId() != null) {
            assertMatchParticipant(request.getMatchId(), actor.getId());
            return request.getMatchId();
        }
        return request.getTournamentId();
    }

    private String resolveFeedEntityType(String visibility, CommunityDtos.FeedPostRequest request) {
        if ("TEAM".equals(visibility)) return "TEAM";
        if (request.getMatchId() != null) return "MATCH";
        if (request.getTournamentId() != null) return "TOURNAMENT";
        return "POST";
    }

    private String normalizeFeedVisibility(String visibility) {
        if (visibility == null || visibility.isBlank()) {
            return null;
        }
        String normalized = visibility.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        if (!Set.of("PUBLIC", "FOLLOWERS", "TEAM", "PRIVATE").contains(normalized)) {
            throw ApiException.badRequest("Post visibility must be PUBLIC, FOLLOWERS, TEAM, or PRIVATE.");
        }
        return normalized;
    }

    private String normalizeReaction(String reactionType) {
        String normalized = defaultValue(reactionType, "LIKE").trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        if (!Set.of("LIKE", "AMAZING", "GREAT_MATCH", "WELL_PLAYED").contains(normalized)) {
            throw ApiException.badRequest("Reaction type must be LIKE, AMAZING, GREAT_MATCH, or WELL_PLAYED.");
        }
        return normalized;
    }

    private String normalizeMessageType(String messageType, String mediaUrl) {
        String normalized = defaultValue(messageType, mediaUrl == null || mediaUrl.isBlank() ? "TEXT" : "IMAGE")
                .trim()
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
        if (!Set.of("TEXT", "IMAGE", "VIDEO", "MATCH_INVITE").contains(normalized)) {
            throw ApiException.badRequest("Message type must be TEXT, IMAGE, VIDEO, or MATCH_INVITE.");
        }
        return normalized;
    }

    private String joinMediaUrls(List<String> mediaUrls) {
        if (mediaUrls == null || mediaUrls.isEmpty()) {
            return null;
        }
        List<String> cleaned = mediaUrls.stream()
                .filter(url -> url != null && !url.isBlank())
                .map(url -> cleanOptional(url, 500, "Media URL").orElse(""))
                .filter(url -> !url.isBlank())
                .distinct()
                .limit(10)
                .toList();
        return cleaned.isEmpty() ? null : String.join("\n", cleaned);
    }

    private List<String> splitMediaUrls(String mediaUrls) {
        if (mediaUrls == null || mediaUrls.isBlank()) {
            return List.of();
        }
        return Arrays.stream(mediaUrls.split("\\R"))
                .filter(url -> !url.isBlank())
                .toList();
    }

    private void updateFeedEngagement(FeedEvent post) {
        int score = post.getReactionCount() * 3 + post.getCommentCount() * 5 + post.getShareCount() * 4 + post.getViewCount();
        post.setEngagementScore(score);
    }

    private void notifyPostOwner(FeedEvent post, User actor, String title, String message) {
        if (post.getActor() == null || post.getActor().getId().equals(actor.getId())) {
            return;
        }
        notificationService.pushNotification(
                post.getActor().getId(),
                post.getActor().getEmail(),
                title,
                message,
                NotificationChannel.PUSH,
                null,
                post.getDeepLink() == null ? "/feed" : post.getDeepLink(),
                "SOCIAL",
                "NORMAL"
        );
    }

    private Long conversationId(Long firstUserId, Long secondUserId) {
        long first = Math.min(firstUserId, secondUserId);
        long second = Math.max(firstUserId, secondUserId);
        return first * 1_000_000_000L + second;
    }

    private boolean containsIgnoreCase(String value, String probe) {
        return value != null && probe != null && value.toLowerCase().contains(probe.toLowerCase());
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private void assertActiveTeamMember(Long teamId, Long userId) {
        if (!teamMemberRepository.existsByTeamIdAndUserIdAndStatus(teamId, userId, TeamMemberStatus.ACTIVE)) {
            throw new ApiException("You are not a member of this team");
        }
    }

    private void assertMatchParticipant(Long matchId, Long userId) {
        findMatch(matchId);
        if (!matchParticipantRepository.existsByMatchIdAndUserId(matchId, userId)) {
            throw new ApiException("Join this match before using match chat");
        }
    }

    private void assertCaptain(Team team, User user) {
        if (!team.getCaptain().getId().equals(user.getId())) {
            throw new ApiException("Only captain can perform this action");
        }
    }

    private void assertTeamManager(Team team, User user) {
        TeamMember membership = teamMemberRepository.findByTeamIdAndUserIdAndStatus(team.getId(), user.getId(), TeamMemberStatus.ACTIVE)
                .orElseThrow(() -> ApiException.forbidden("Only team managers can perform this action."));
        if (!isTeamManagerRole(membership.getRole())) {
            throw ApiException.forbidden("Only captain, vice captain, or manager can perform this action.");
        }
    }

    private boolean isTeamManagerRole(TeamMemberRole role) {
        TeamMemberRole normalized = normalizeRole(role);
        return normalized == TeamMemberRole.CAPTAIN || normalized == TeamMemberRole.VICE_CAPTAIN || normalized == TeamMemberRole.MANAGER || normalized == TeamMemberRole.OWNER;
    }

    private void upsertActiveMembership(Team team, User user, TeamMemberRole role) {
        TeamMember membership = teamMemberRepository.findByTeamIdAndUserId(team.getId(), user.getId())
                .orElseGet(() -> TeamMember.builder().team(team).user(user).build());
        membership.setRole(role);
        membership.setStatus(TeamMemberStatus.ACTIVE);
        teamMemberRepository.save(membership);
    }

    private int teamReputationScore(Team team, int activeMembers) {
        int fillScore = team.getMaxMembers() == null || team.getMaxMembers() == 0 ? 0 : (int) Math.round((activeMembers * 50.0) / team.getMaxMembers());
        int captainScore = team.getCaptain() == null ? 0 : 20;
        int profileScore = 0;
        if (team.getDescription() != null && !team.getDescription().isBlank()) profileScore += 10;
        if (team.getLogoUrl() != null && !team.getLogoUrl().isBlank()) profileScore += 10;
        if (team.getVisibility() == TeamVisibility.PUBLIC || team.getVisibility() == TeamVisibility.INVITE_ONLY) profileScore += 10;
        return Math.min(100, fillScore + captainScore + profileScore);
    }

    private void assertMatchManager(PublicMatch match, User user) {
        if (match.getCreator() != null && match.getCreator().getId().equals(user.getId())) {
            return;
        }
        if (isActiveTeamManager(match.getHomeTeam(), user) || isActiveTeamManager(match.getAwayTeam(), user)) {
            return;
        }
        throw ApiException.forbidden("Only match creators or team leadership can manage this match.");
    }

    private boolean isActiveTeamManager(Team team, User user) {
        if (team == null || user == null || user.getId() == null) {
            return false;
        }
        return teamMemberRepository.findByTeamIdAndUserIdAndStatus(team.getId(), user.getId(), TeamMemberStatus.ACTIVE)
                .map(TeamMember::getRole)
                .map(this::isTeamManagerRole)
                .orElse(false);
    }

    private Team resolveWinnerTeam(PublicMatch match, Long winnerTeamId, Integer homeScore, Integer awayScore) {
        if (winnerTeamId == null) {
            if (homeScore.equals(awayScore)) {
                return null;
            }
            return homeScore > awayScore ? match.getHomeTeam() : match.getAwayTeam();
        }
        boolean homeWinner = match.getHomeTeam() != null && match.getHomeTeam().getId().equals(winnerTeamId);
        boolean awayWinner = match.getAwayTeam() != null && match.getAwayTeam().getId().equals(winnerTeamId);
        if (!homeWinner && !awayWinner) {
            throw ApiException.badRequest("Winner must be one of the match teams.");
        }
        if (homeScore.equals(awayScore)) {
            throw ApiException.badRequest("Winner cannot be selected for a tied score.");
        }
        if (homeWinner && homeScore < awayScore) {
            throw ApiException.badRequest("Home team winner score must be greater than away score.");
        }
        if (awayWinner && awayScore < homeScore) {
            throw ApiException.badRequest("Away team winner score must be greater than home score.");
        }
        return homeWinner ? match.getHomeTeam() : match.getAwayTeam();
    }

    private String defaultResultMargin(PublicMatch match, Integer homeScore, Integer awayScore) {
        if (homeScore.equals(awayScore)) {
            return "Draw";
        }
        Team winner = homeScore > awayScore ? match.getHomeTeam() : match.getAwayTeam();
        int margin = Math.abs(homeScore - awayScore);
        String winnerName = winner == null ? "Winner" : winner.getName();
        return winnerName + " won by " + margin + " runs";
    }

    private void notifyMatchTeams(PublicMatch match, String title, String message) {
        notifyMatchCaptain(match.getHomeTeam(), title, message);
        notifyMatchCaptain(match.getAwayTeam(), title, message);
    }

    private void notifyMatchCaptain(Team team, String title, String message) {
        if (team == null || team.getCaptain() == null) {
            return;
        }
        notificationService.pushNotification(
                team.getCaptain().getId(),
                team.getCaptain().getEmail(),
                title,
                message,
                com.example.Cricbook.enums.NotificationChannel.PUSH,
                null,
                "/social"
        );
    }

    private List<TeamMatchStatsResponse.RivalryResponse> buildRivalries(Long teamId, List<PublicMatch> completedMatches) {
        Map<Long, RivalryAccumulator> rivals = new LinkedHashMap<>();
        for (PublicMatch match : completedMatches) {
            Team opponent = null;
            if (match.getHomeTeam() != null && match.getHomeTeam().getId().equals(teamId)) {
                opponent = match.getAwayTeam();
            } else if (match.getAwayTeam() != null && match.getAwayTeam().getId().equals(teamId)) {
                opponent = match.getHomeTeam();
            }
            if (opponent == null) {
                continue;
            }
            Team rivalryOpponent = opponent;
            RivalryAccumulator accumulator = rivals.computeIfAbsent(rivalryOpponent.getId(), id -> new RivalryAccumulator(rivalryOpponent));
            accumulator.matches++;
            if (match.getWinnerTeam() != null && match.getWinnerTeam().getId().equals(teamId)) {
                accumulator.wins++;
            } else if (match.getWinnerTeam() != null && match.getWinnerTeam().getId().equals(opponent.getId())) {
                accumulator.losses++;
            }
        }
        return rivals.values().stream()
                .sorted(Comparator.comparingLong((RivalryAccumulator item) -> item.matches).reversed())
                .limit(5)
                .map(item -> TeamMatchStatsResponse.RivalryResponse.builder()
                        .opponentTeamId(item.opponent.getId())
                        .opponentTeamName(item.opponent.getName())
                        .matches(item.matches)
                        .wins(item.wins)
                        .losses(item.losses)
                        .build())
                .toList();
    }

    private Team findTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ApiException("Team not found"));
    }

    private PublicMatch findMatch(Long matchId) {
        return publicMatchRepository.findById(matchId)
                .orElseThrow(() -> new ApiException("Match not found"));
    }

    private User findUser(String loginId) {
        return userRepository.findByEmailOrPhone(loginId)
                .orElseThrow(() -> new ApiException("User not found"));
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(message);
        }
        return value.trim();
    }

    private String cleanRequired(String value, String message, int min, int max) {
        String cleaned = cleanOptional(value, max, message).orElseThrow(() -> new ApiException(message));
        if (cleaned.length() < min) {
            throw new ApiException(message);
        }
        return cleaned;
    }

    private Optional<String> cleanOptional(String value, int max, String field) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String cleaned = value.trim()
                .replaceAll("<[^>]*>", "")
                .replace("<", "")
                .replace(">", "")
                .replaceAll("\\s+", " ");
        if (cleaned.length() > max) {
            throw ApiException.badRequest(field + " cannot exceed " + max + " characters.");
        }
        return Optional.of(cleaned);
    }

    private String cleanSearch(String value) {
        return cleanOptional(value, 80, "Search").orElse(null);
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private <T> T requiredObject(T value, String message) {
        if (value == null) {
            throw new ApiException(message);
        }
        return value;
    }

    private Integer resolveLimit(Integer value, Integer fallback, String message) {
        Integer resolved = value == null ? fallback : value;
        if (resolved <= 0) {
            throw new ApiException(message);
        }
        return resolved;
    }

    private Integer resolveTeamSize(Integer value) {
        int resolved = value == null ? 11 : value;
        if (resolved < 2 || resolved > 30) {
            throw ApiException.badRequest("Team size must be between 2 and 30 members.");
        }
        return resolved;
    }

    private static class RivalryAccumulator {
        private final Team opponent;
        private long matches;
        private long wins;
        private long losses;

        private RivalryAccumulator(Team opponent) {
            this.opponent = opponent;
        }
    }
}
