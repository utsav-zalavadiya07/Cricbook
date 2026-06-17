package com.example.Cricbook.service;

import com.example.Cricbook.dto.CommunityDtos;
import com.example.Cricbook.dto.CreateTeamRequest;
import com.example.Cricbook.dto.CreatePublicMatchRequest;
import com.example.Cricbook.dto.FeedEventResponse;
import com.example.Cricbook.dto.MatchResultRequest;
import com.example.Cricbook.dto.PlayerDiscoverResponse;
import com.example.Cricbook.dto.PublicMatchResponse;
import com.example.Cricbook.dto.RespondInviteRequest;
import com.example.Cricbook.dto.TeamMatchStatsResponse;
import com.example.Cricbook.dto.TeamInviteResponse;
import com.example.Cricbook.dto.TeamResponse;
import com.example.Cricbook.entity.MatchParticipant;
import com.example.Cricbook.entity.FeedComment;
import com.example.Cricbook.entity.FeedEvent;
import com.example.Cricbook.entity.FeedReaction;
import com.example.Cricbook.entity.PlayerConnection;
import com.example.Cricbook.entity.PlayerProfile;
import com.example.Cricbook.entity.PublicMatch;
import com.example.Cricbook.entity.TeamRecruitment;
import com.example.Cricbook.entity.Team;
import com.example.Cricbook.entity.TeamInvite;
import com.example.Cricbook.entity.TeamJoinRequest;
import com.example.Cricbook.entity.TeamMember;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.InviteStatus;
import com.example.Cricbook.enums.PlayerConnectionStatus;
import com.example.Cricbook.enums.PlayerConnectionType;
import com.example.Cricbook.enums.PlayerPosition;
import com.example.Cricbook.enums.PublicMatchStatus;
import com.example.Cricbook.enums.TeamMemberRole;
import com.example.Cricbook.enums.TeamMemberStatus;
import com.example.Cricbook.enums.TeamRecruitmentStatus;
import com.example.Cricbook.enums.TeamVisibility;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.ChatMessageRepository;
import com.example.Cricbook.repository.CommunityModerationRepository;
import com.example.Cricbook.repository.FeedCommentRepository;
import com.example.Cricbook.repository.FeedEventRepository;
import com.example.Cricbook.repository.FeedReactionRepository;
import com.example.Cricbook.repository.MatchParticipantRepository;
import com.example.Cricbook.repository.PlayerAvailabilityRepository;
import com.example.Cricbook.repository.PlayerConnectionRepository;
import com.example.Cricbook.repository.PlayerProfileRepository;
import com.example.Cricbook.repository.PlayerTrustScoreRepository;
import com.example.Cricbook.repository.PublicMatchRepository;
import com.example.Cricbook.repository.RecruitmentApplicationRepository;
import com.example.Cricbook.repository.TeamInviteRepository;
import com.example.Cricbook.repository.TeamJoinRequestRepository;
import com.example.Cricbook.repository.TeamMemberRepository;
import com.example.Cricbook.repository.TeamRecruitmentRepository;
import com.example.Cricbook.repository.TeamRepository;
import com.example.Cricbook.repository.TurfRepository;
import com.example.Cricbook.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TurfRepository turfRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private TeamInviteRepository teamInviteRepository;
    @Mock private TeamJoinRequestRepository teamJoinRequestRepository;
    @Mock private PlayerProfileRepository playerProfileRepository;
    @Mock private PlayerAvailabilityRepository playerAvailabilityRepository;
    @Mock private PlayerTrustScoreRepository playerTrustScoreRepository;
    @Mock private PlayerConnectionRepository playerConnectionRepository;
    @Mock private TeamRecruitmentRepository teamRecruitmentRepository;
    @Mock private RecruitmentApplicationRepository recruitmentApplicationRepository;
    @Mock private FeedEventRepository feedEventRepository;
    @Mock private FeedReactionRepository feedReactionRepository;
    @Mock private FeedCommentRepository feedCommentRepository;
    @Mock private CommunityModerationRepository communityModerationRepository;
    @Mock private PublicMatchRepository publicMatchRepository;
    @Mock private MatchParticipantRepository matchParticipantRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private SocialService socialService;

    @Test
    void createTeamAddsCaptainMembership() {
        User user = User.builder().id(1L).name("Virat").email("v@test.com").phone("999").build();
        Team savedTeam = Team.builder()
                .id(10L)
                .captain(user)
                .name("Strikers")
                .sport("Cricket")
                .city("Ahmedabad")
                .visibility(TeamVisibility.PRIVATE)
                .maxMembers(11)
                .build();
        TeamMember captain = TeamMember.builder()
                .id(20L)
                .team(savedTeam)
                .user(user)
                .role(TeamMemberRole.CAPTAIN)
                .status(TeamMemberStatus.ACTIVE)
                .build();
        CreateTeamRequest request = new CreateTeamRequest();
        request.setName("Strikers");
        request.setCity("Ahmedabad");
        request.setVisibility(TeamVisibility.PRIVATE);

        when(userRepository.findByEmailOrPhone("v@test.com")).thenReturn(Optional.of(user));
        when(teamRepository.save(any(Team.class))).thenReturn(savedTeam);
        when(teamMemberRepository.save(any(TeamMember.class))).thenReturn(captain);
        when(teamMemberRepository.countByTeamIdAndStatus(10L, TeamMemberStatus.ACTIVE)).thenReturn(1L);
        when(teamMemberRepository.findByTeamIdAndStatusOrderByCreatedAtAsc(10L, TeamMemberStatus.ACTIVE)).thenReturn(List.of(captain));

        TeamResponse response = socialService.createTeam("v@test.com", request);

        assertEquals("Strikers", response.getName());
        assertEquals(1, response.getMemberCount());
        assertEquals(TeamMemberRole.CAPTAIN, response.getMembers().get(0).getRole());
        assertEquals(TeamVisibility.PRIVATE, response.getVisibility());

        ArgumentCaptor<TeamMember> memberCaptor = ArgumentCaptor.forClass(TeamMember.class);
        verify(teamMemberRepository).save(memberCaptor.capture());
        assertEquals(TeamMemberRole.CAPTAIN, memberCaptor.getValue().getRole());
        assertEquals(TeamMemberStatus.ACTIVE, memberCaptor.getValue().getStatus());
    }

    @Test
    void createTeamRejectsMissingName() {
        User user = User.builder().id(1L).email("v@test.com").build();
        CreateTeamRequest request = new CreateTeamRequest();
        request.setCity("Ahmedabad");

        when(userRepository.findByEmailOrPhone("v@test.com")).thenReturn(Optional.of(user));

        assertThrows(ApiException.class, () -> socialService.createTeam("v@test.com", request));
    }

    @Test
    void createTeamRejectsDuplicateName() {
        User user = User.builder().id(1L).name("Virat").email("v@test.com").build();
        CreateTeamRequest request = new CreateTeamRequest();
        request.setName("Strikers");
        request.setCity("Ahmedabad");

        when(userRepository.findByEmailOrPhone("v@test.com")).thenReturn(Optional.of(user));
        when(teamRepository.existsByNameIgnoreCase("Strikers")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> socialService.createTeam("v@test.com", request));

        assertEquals("A team with this name already exists.", exception.getMessage());
    }

    @Test
    void createTeamSanitizesDescriptionAndSupportsInviteOnly() {
        User user = User.builder().id(1L).name("Virat").email("v@test.com").phone("999").build();
        CreateTeamRequest request = new CreateTeamRequest();
        request.setName("Clean Strikers");
        request.setCity("Ahmedabad");
        request.setDescription("<script>alert(1)</script> Weekend practice squad");
        request.setVisibility(TeamVisibility.INVITE_ONLY);
        request.setMaxMembers(15);

        when(userRepository.findByEmailOrPhone("v@test.com")).thenReturn(Optional.of(user));
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team team = invocation.getArgument(0);
            team.setId(12L);
            return team;
        });
        when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(invocation -> {
            TeamMember member = invocation.getArgument(0);
            member.setId(22L);
            return member;
        });
        when(teamMemberRepository.countByTeamIdAndStatus(12L, TeamMemberStatus.ACTIVE)).thenReturn(1L);
        when(teamMemberRepository.findByTeamIdAndStatusOrderByCreatedAtAsc(12L, TeamMemberStatus.ACTIVE)).thenReturn(List.of());

        TeamResponse response = socialService.createTeam("v@test.com", request);

        assertEquals(TeamVisibility.INVITE_ONLY, response.getVisibility());
        assertEquals(14, response.getOpenSpots());
        ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
        verify(teamRepository).save(teamCaptor.capture());
        assertEquals("alert(1) Weekend practice squad", teamCaptor.getValue().getDescription());
    }

    @Test
    void respondToInviteAcceptsAndCreatesActiveMembership() {
        User captain = User.builder().id(1L).name("Captain").email("captain@test.com").build();
        User receiver = User.builder().id(2L).name("Player").email("player@test.com").build();
        Team team = Team.builder()
                .id(10L)
                .name("Strikers")
                .captain(captain)
                .sport("Cricket")
                .city("Surat")
                .visibility(TeamVisibility.PUBLIC)
                .maxMembers(11)
                .build();
        TeamInvite invite = TeamInvite.builder()
                .id(30L)
                .team(team)
                .sender(captain)
                .receiver(receiver)
                .status(InviteStatus.PENDING)
                .build();
        RespondInviteRequest request = new RespondInviteRequest();
        request.setAccept(true);

        when(userRepository.findByEmailOrPhone("player@test.com")).thenReturn(Optional.of(receiver));
        when(teamInviteRepository.findByIdAndReceiverId(30L, 2L)).thenReturn(Optional.of(invite));
        when(teamMemberRepository.countByTeamIdAndStatus(10L, TeamMemberStatus.ACTIVE)).thenReturn(1L);
        when(teamInviteRepository.save(any(TeamInvite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeamInviteResponse response = socialService.respondToInvite("player@test.com", 30L, request);

        assertEquals(InviteStatus.ACCEPTED, response.getStatus());

        ArgumentCaptor<TeamMember> memberCaptor = ArgumentCaptor.forClass(TeamMember.class);
        verify(teamMemberRepository).save(memberCaptor.capture());
        assertEquals(receiver.getId(), memberCaptor.getValue().getUser().getId());
        assertEquals(TeamMemberRole.PLAYER, memberCaptor.getValue().getRole());
        assertEquals(TeamMemberStatus.ACTIVE, memberCaptor.getValue().getStatus());
    }

    @Test
    void inviteTeamMemberAllowsViceCaptainManagerRole() {
        User captain = User.builder().id(1L).name("Captain").email("captain@test.com").build();
        User viceCaptain = User.builder().id(3L).name("Vice").email("vice@test.com").build();
        User invited = User.builder().id(2L).name("Player").email("player@test.com").build();
        Team team = Team.builder()
                .id(10L)
                .name("Strikers")
                .captain(captain)
                .sport("Cricket")
                .city("Surat")
                .visibility(TeamVisibility.PUBLIC)
                .maxMembers(11)
                .build();
        com.example.Cricbook.dto.InviteTeamMemberRequest request = new com.example.Cricbook.dto.InviteTeamMemberRequest();
        request.setLoginId("player@test.com");

        when(userRepository.findByEmailOrPhone("vice@test.com")).thenReturn(Optional.of(viceCaptain));
        when(userRepository.findByEmailOrPhone("player@test.com")).thenReturn(Optional.of(invited));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserIdAndStatus(10L, 3L, TeamMemberStatus.ACTIVE))
                .thenReturn(Optional.of(TeamMember.builder().team(team).user(viceCaptain).role(TeamMemberRole.VICE_CAPTAIN).status(TeamMemberStatus.ACTIVE).build()));
        when(teamInviteRepository.save(any(TeamInvite.class))).thenAnswer(invocation -> {
            TeamInvite invite = invocation.getArgument(0);
            invite.setId(45L);
            return invite;
        });

        com.example.Cricbook.dto.TeamMemberResponse response = socialService.inviteTeamMember("vice@test.com", 10L, request);

        assertEquals(45L, response.getId());
        assertEquals(TeamMemberStatus.INVITED, response.getStatus());
    }

    @Test
    void requestToJoinTeamCreatesPendingRequestAndBlocksDuplicate() {
        User captain = User.builder().id(1L).name("Captain").email("captain@test.com").build();
        User requester = User.builder().id(2L).name("Player").email("player@test.com").build();
        Team team = Team.builder()
                .id(10L)
                .name("Strikers")
                .captain(captain)
                .sport("Cricket")
                .city("Surat")
                .visibility(TeamVisibility.INVITE_ONLY)
                .maxMembers(11)
                .build();
        com.example.Cricbook.dto.RequestJoinTeamRequest request = new com.example.Cricbook.dto.RequestJoinTeamRequest();
        request.setMessage("I can bowl at death overs.");

        when(userRepository.findByEmailOrPhone("player@test.com")).thenReturn(Optional.of(requester));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(teamJoinRequestRepository.existsByTeamIdAndRequesterIdAndStatus(10L, 2L, InviteStatus.PENDING)).thenReturn(false);
        when(teamJoinRequestRepository.save(any(TeamJoinRequest.class))).thenAnswer(invocation -> {
            TeamJoinRequest saved = invocation.getArgument(0);
            saved.setId(66L);
            return saved;
        });

        com.example.Cricbook.dto.TeamJoinRequestResponse response = socialService.requestToJoinTeam("player@test.com", 10L, request);

        assertEquals(66L, response.getId());
        assertEquals(InviteStatus.PENDING, response.getStatus());
        assertEquals("I can bowl at death overs.", response.getMessage());
    }

    @Test
    void respondToJoinRequestAcceptsAndReactivatesExistingMembership() {
        User captain = User.builder().id(1L).name("Captain").email("captain@test.com").build();
        User requester = User.builder().id(2L).name("Player").email("player@test.com").build();
        Team team = Team.builder()
                .id(10L)
                .name("Strikers")
                .captain(captain)
                .sport("Cricket")
                .city("Surat")
                .visibility(TeamVisibility.PUBLIC)
                .maxMembers(11)
                .build();
        TeamJoinRequest joinRequest = TeamJoinRequest.builder()
                .id(70L)
                .team(team)
                .requester(requester)
                .status(InviteStatus.PENDING)
                .build();
        TeamMember oldMembership = TeamMember.builder()
                .id(90L)
                .team(team)
                .user(requester)
                .role(TeamMemberRole.PLAYER)
                .status(TeamMemberStatus.LEFT)
                .build();
        RespondInviteRequest decision = new RespondInviteRequest();
        decision.setAccept(true);

        when(userRepository.findByEmailOrPhone("captain@test.com")).thenReturn(Optional.of(captain));
        when(teamJoinRequestRepository.findById(70L)).thenReturn(Optional.of(joinRequest));
        when(teamMemberRepository.findByTeamIdAndUserIdAndStatus(10L, 1L, TeamMemberStatus.ACTIVE))
                .thenReturn(Optional.of(TeamMember.builder().team(team).user(captain).role(TeamMemberRole.CAPTAIN).status(TeamMemberStatus.ACTIVE).build()));
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.of(oldMembership));
        when(teamJoinRequestRepository.save(joinRequest)).thenReturn(joinRequest);

        com.example.Cricbook.dto.TeamJoinRequestResponse response = socialService.respondToJoinRequest("captain@test.com", 70L, decision);

        assertEquals(InviteStatus.ACCEPTED, response.getStatus());
        assertEquals(TeamMemberStatus.ACTIVE, oldMembership.getStatus());
        verify(teamMemberRepository).save(oldMembership);
    }

    @Test
    void updateMemberRoleRejectsCaptainRoleAssignment() {
        User captain = User.builder().id(1L).name("Captain").email("captain@test.com").build();
        com.example.Cricbook.dto.TeamActionRequest request = new com.example.Cricbook.dto.TeamActionRequest();
        request.setUserId(2L);
        request.setRole(TeamMemberRole.CAPTAIN);

        ApiException exception = assertThrows(ApiException.class, () -> socialService.updateMemberRole("captain@test.com", 10L, request));

        assertEquals("Use transfer captain for captain changes", exception.getMessage());
    }

    @Test
    void discoverTeamsReturnsVisibleOpenTeams() {
        User actor = User.builder().id(1L).name("Player").email("player@test.com").build();
        User captain = User.builder().id(2L).name("Captain").email("captain@test.com").build();
        Team openTeam = Team.builder()
                .id(10L)
                .name("Open Strikers")
                .captain(captain)
                .sport("Cricket")
                .city("Surat")
                .visibility(TeamVisibility.PUBLIC)
                .maxMembers(11)
                .build();
        Team fullTeam = Team.builder()
                .id(11L)
                .name("Full Squad")
                .captain(captain)
                .sport("Cricket")
                .city("Surat")
                .visibility(TeamVisibility.INVITE_ONLY)
                .maxMembers(1)
                .build();

        when(userRepository.findByEmailOrPhone("player@test.com")).thenReturn(Optional.of(actor));
        when(teamRepository.discoverTeams("strikers", "surat", List.of(TeamVisibility.PUBLIC, TeamVisibility.INVITE_ONLY)))
                .thenReturn(List.of(openTeam, fullTeam));
        when(teamMemberRepository.countByTeamIdAndStatus(10L, TeamMemberStatus.ACTIVE)).thenReturn(3L);
        when(teamMemberRepository.countByTeamIdAndStatus(11L, TeamMemberStatus.ACTIVE)).thenReturn(1L);

        List<TeamResponse> result = socialService.discoverTeams("player@test.com", "strikers", "surat", null, true);

        assertEquals(1, result.size());
        assertEquals("Open Strikers", result.get(0).getName());
        assertEquals(8, result.get(0).getOpenSpots());
    }

    @Test
    void discoverPlayersAddsTrustNetworkAndMatchSignals() {
        User actor = User.builder().id(1L).name("Captain").email("captain@test.com").build();
        User player = User.builder().id(2L).name("Bowler").email("bowler@test.com").build();
        PlayerProfile profile = PlayerProfile.builder()
                .user(player)
                .city("Surat")
                .position(PlayerPosition.BOWLER)
                .experience("Club")
                .rating(4.2)
                .matchesPlayed(12)
                .wins(8)
                .mvpCount(2)
                .build();

        when(userRepository.findByEmailOrPhone("captain@test.com")).thenReturn(Optional.of(actor));
        when(userRepository.findAll()).thenReturn(List.of(actor, player));
        when(playerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
        when(playerAvailabilityRepository.findByUserId(2L)).thenReturn(Optional.empty());
        when(playerAvailabilityRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(playerTrustScoreRepository.findByUserId(2L)).thenReturn(Optional.empty());
        when(playerConnectionRepository.countByFromUserIdOrToUserId(2L, 2L)).thenReturn(1L);
        when(playerConnectionRepository.findByFromUserIdOrToUserIdOrderByCreatedAtDesc(1L, 1L)).thenReturn(List.of());

        List<PlayerDiscoverResponse> result = socialService.discoverPlayers("captain@test.com", "Surat", "bowler", null);

        assertEquals(1, result.size());
        assertEquals("Advanced", result.get(0).getSkillLevel());
        assertEquals(70, result.get(0).getTrustScore());
        assertEquals("NONE", result.get(0).getConnectionStatus());
    }

    @Test
    void connectPlayerCreatesPendingNetworkRequest() {
        User from = User.builder().id(1L).name("Captain").email("captain@test.com").build();
        User to = User.builder().id(2L).name("Player").email("player@test.com").build();
        CommunityDtos.ConnectionRequest request = new CommunityDtos.ConnectionRequest(2L, PlayerConnectionType.FRIEND, "Join my network");

        when(userRepository.findByEmailOrPhone("captain@test.com")).thenReturn(Optional.of(from));
        when(userRepository.findById(2L)).thenReturn(Optional.of(to));
        when(playerConnectionRepository.findByFromUserIdAndToUserIdAndType(1L, 2L, PlayerConnectionType.FRIEND)).thenReturn(Optional.empty());
        when(playerConnectionRepository.save(any(PlayerConnection.class))).thenAnswer(invocation -> {
            PlayerConnection saved = invocation.getArgument(0);
            saved.setId(44L);
            return saved;
        });

        CommunityDtos.PlayerConnectionResponse response = socialService.connectPlayer("captain@test.com", request);

        assertEquals(44L, response.getId());
        assertEquals(PlayerConnectionStatus.PENDING, response.getStatus());
        assertEquals(2L, response.getToUserId());
    }

    @Test
    void createRecruitmentRequiresTeamCaptainAndSavesOpenPost() {
        User captain = User.builder().id(1L).name("Captain").email("captain@test.com").build();
        Team team = Team.builder()
                .id(10L)
                .name("Strikers")
                .captain(captain)
                .city("Ahmedabad")
                .visibility(TeamVisibility.PUBLIC)
                .maxMembers(11)
                .build();
        CommunityDtos.RecruitmentRequest request = new CommunityDtos.RecruitmentRequest();
        request.setTeamId(10L);
        request.setRoleNeeded("Fast bowler");

        when(userRepository.findByEmailOrPhone("captain@test.com")).thenReturn(Optional.of(captain));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(teamRecruitmentRepository.save(any(TeamRecruitment.class))).thenAnswer(invocation -> {
            TeamRecruitment saved = invocation.getArgument(0);
            saved.setId(55L);
            return saved;
        });
        when(recruitmentApplicationRepository.findByRecruitmentIdOrderByCreatedAtDesc(55L)).thenReturn(List.of());

        CommunityDtos.RecruitmentResponse response = socialService.createRecruitment("captain@test.com", request);

        assertEquals(55L, response.getId());
        assertEquals("Fast bowler", response.getRoleNeeded());
        assertEquals(TeamRecruitmentStatus.OPEN, response.getStatus());
    }

    @Test
    void createPublicMatchCreatesPendingTeamChallenge() {
        User captain = User.builder().id(1L).name("Captain").email("captain@test.com").build();
        User rivalCaptain = User.builder().id(2L).name("Rival").email("rival@test.com").build();
        Team home = team(10L, "Strikers", captain);
        Team away = team(11L, "Riders", rivalCaptain);
        CreatePublicMatchRequest request = new CreatePublicMatchRequest();
        request.setTitle("Friday rivalry");
        request.setCity("Surat");
        request.setMatchDate(LocalDate.now().plusDays(3));
        request.setStartTime(LocalTime.of(19, 0));
        request.setHomeTeamId(10L);
        request.setAwayTeamId(11L);
        request.setChallengeMessage("<b>Bring your best XI</b>");

        when(userRepository.findByEmailOrPhone("captain@test.com")).thenReturn(Optional.of(captain));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(home));
        when(teamRepository.findById(11L)).thenReturn(Optional.of(away));
        when(teamMemberRepository.findByTeamIdAndUserIdAndStatus(10L, 1L, TeamMemberStatus.ACTIVE))
                .thenReturn(Optional.of(TeamMember.builder().team(home).user(captain).role(TeamMemberRole.CAPTAIN).status(TeamMemberStatus.ACTIVE).build()));
        when(publicMatchRepository.existsByHomeTeamIdAndAwayTeamIdAndStatusIn(eq(10L), eq(11L), any())).thenReturn(false);
        when(publicMatchRepository.existsByHomeTeamIdAndAwayTeamIdAndStatusIn(eq(11L), eq(10L), any())).thenReturn(false);
        when(publicMatchRepository.save(any(PublicMatch.class))).thenAnswer(invocation -> {
            PublicMatch match = invocation.getArgument(0);
            match.setId(99L);
            return match;
        });
        when(publicMatchRepository.findById(99L)).thenReturn(Optional.of(PublicMatch.builder()
                .id(99L)
                .creator(captain)
                .title("Friday rivalry")
                .city("Surat")
                .location("Surat")
                .matchDate(request.getMatchDate())
                .startTime(request.getStartTime())
                .maxPlayers(22)
                .homeTeam(home)
                .awayTeam(away)
                .challengeMessage("Bring your best XI")
                .status(PublicMatchStatus.PENDING)
                .build()));
        when(matchParticipantRepository.save(any(MatchParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchParticipantRepository.countByMatchId(99L)).thenReturn(1L);
        when(matchParticipantRepository.findByMatchIdOrderByJoinedAtAsc(99L)).thenReturn(List.of());

        PublicMatchResponse response = socialService.createPublicMatch("captain@test.com", request);

        assertEquals(PublicMatchStatus.PENDING, response.getStatus());
        assertEquals(10L, response.getHomeTeamId());
        assertEquals(11L, response.getAwayTeamId());
        assertEquals("Bring your best XI", response.getChallengeMessage());
    }

    @Test
    void acceptMatchChallengeSchedulesChallengeForAwayLeadership() {
        User homeCaptain = User.builder().id(1L).name("Captain").email("captain@test.com").build();
        User awayCaptain = User.builder().id(2L).name("Rival").email("rival@test.com").build();
        Team home = team(10L, "Strikers", homeCaptain);
        Team away = team(11L, "Riders", awayCaptain);
        PublicMatch match = PublicMatch.builder()
                .id(99L)
                .creator(homeCaptain)
                .title("Friday rivalry")
                .city("Surat")
                .location("Surat")
                .matchDate(LocalDate.now().plusDays(3))
                .startTime(LocalTime.of(19, 0))
                .maxPlayers(22)
                .homeTeam(home)
                .awayTeam(away)
                .status(PublicMatchStatus.PENDING)
                .build();

        when(userRepository.findByEmailOrPhone("rival@test.com")).thenReturn(Optional.of(awayCaptain));
        when(publicMatchRepository.findById(99L)).thenReturn(Optional.of(match));
        when(teamMemberRepository.findByTeamIdAndUserIdAndStatus(11L, 2L, TeamMemberStatus.ACTIVE))
                .thenReturn(Optional.of(TeamMember.builder().team(away).user(awayCaptain).role(TeamMemberRole.CAPTAIN).status(TeamMemberStatus.ACTIVE).build()));
        when(publicMatchRepository.save(match)).thenReturn(match);
        when(matchParticipantRepository.countByMatchId(99L)).thenReturn(0L);
        when(matchParticipantRepository.findByMatchIdOrderByJoinedAtAsc(99L)).thenReturn(List.of());

        PublicMatchResponse response = socialService.acceptMatchChallenge("rival@test.com", 99L);

        assertEquals(PublicMatchStatus.SCHEDULED, response.getStatus());
        assertEquals(11L, response.getAwayTeamId());
    }

    @Test
    void submitMatchResultCompletesAndStoresWinner() {
        User homeCaptain = User.builder().id(1L).name("Captain").email("captain@test.com").build();
        User awayCaptain = User.builder().id(2L).name("Rival").email("rival@test.com").build();
        Team home = team(10L, "Strikers", homeCaptain);
        Team away = team(11L, "Riders", awayCaptain);
        PublicMatch match = PublicMatch.builder()
                .id(99L)
                .creator(homeCaptain)
                .title("Friday rivalry")
                .city("Surat")
                .location("Surat")
                .matchDate(LocalDate.now())
                .startTime(LocalTime.of(19, 0))
                .maxPlayers(22)
                .homeTeam(home)
                .awayTeam(away)
                .status(PublicMatchStatus.LIVE)
                .build();
        MatchResultRequest request = new MatchResultRequest();
        request.setWinnerTeamId(10L);
        request.setHomeScore(151);
        request.setAwayScore(132);

        when(userRepository.findByEmailOrPhone("captain@test.com")).thenReturn(Optional.of(homeCaptain));
        when(publicMatchRepository.findById(99L)).thenReturn(Optional.of(match));
        when(publicMatchRepository.save(match)).thenReturn(match);
        when(matchParticipantRepository.countByMatchId(99L)).thenReturn(0L);
        when(matchParticipantRepository.findByMatchIdOrderByJoinedAtAsc(99L)).thenReturn(List.of());

        PublicMatchResponse response = socialService.submitMatchResult("captain@test.com", 99L, request);

        assertEquals(PublicMatchStatus.COMPLETED, response.getStatus());
        assertEquals(10L, response.getWinnerTeamId());
        assertEquals(151, response.getHomeScore());
        assertEquals("Strikers won by 19 runs", response.getResultMargin());
    }

    @Test
    void teamMatchStatsCalculatesRecordAndRivalries() {
        User homeCaptain = User.builder().id(1L).name("Captain").email("captain@test.com").build();
        User awayCaptain = User.builder().id(2L).name("Rival").email("rival@test.com").build();
        Team home = team(10L, "Strikers", homeCaptain);
        Team away = team(11L, "Riders", awayCaptain);
        PublicMatch completedWin = PublicMatch.builder()
                .id(99L)
                .creator(homeCaptain)
                .title("Final")
                .city("Surat")
                .location("Surat")
                .matchDate(LocalDate.now().minusDays(1))
                .startTime(LocalTime.of(19, 0))
                .maxPlayers(22)
                .homeTeam(home)
                .awayTeam(away)
                .winnerTeam(home)
                .status(PublicMatchStatus.COMPLETED)
                .build();
        PublicMatch pending = PublicMatch.builder()
                .id(100L)
                .creator(awayCaptain)
                .title("Rematch")
                .city("Surat")
                .location("Surat")
                .matchDate(LocalDate.now().plusDays(4))
                .startTime(LocalTime.of(19, 0))
                .maxPlayers(22)
                .homeTeam(away)
                .awayTeam(home)
                .status(PublicMatchStatus.PENDING)
                .build();

        when(userRepository.findByEmailOrPhone("captain@test.com")).thenReturn(Optional.of(homeCaptain));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(home));
        when(publicMatchRepository.findByHomeTeamIdOrAwayTeamIdOrderByMatchDateDescStartTimeDesc(10L, 10L)).thenReturn(List.of(pending, completedWin));
        when(matchParticipantRepository.countByMatchId(anyLong())).thenReturn(0L);

        TeamMatchStatsResponse response = socialService.getTeamMatchStats("captain@test.com", 10L);

        assertEquals(1L, response.getMatchesPlayed());
        assertEquals(1L, response.getWins());
        assertEquals(100, response.getWinPercentage());
        assertEquals(1L, response.getPendingChallenges());
        assertEquals(1, response.getRivalries().size());
        assertEquals(11L, response.getRivalries().get(0).getOpponentTeamId());
    }

    @Test
    void createFeedPostSavesActivePublicPost() {
        User actor = User.builder().id(1L).name("Captain").email("captain@test.com").build();
        CommunityDtos.FeedPostRequest request = new CommunityDtos.FeedPostRequest();
        request.setTitle("Net session recap");
        request.setContent("Great practice tonight.");
        request.setVisibility("PUBLIC");
        request.setMediaUrls(List.of("https://cdn.example.com/photo.jpg"));

        when(userRepository.findByEmailOrPhone("captain@test.com")).thenReturn(Optional.of(actor));
        when(feedEventRepository.save(any(FeedEvent.class))).thenAnswer(invocation -> {
            FeedEvent saved = invocation.getArgument(0);
            saved.setId(200L);
            return saved;
        });
        when(feedReactionRepository.findByFeedEventIdAndUserId(200L, 1L)).thenReturn(Optional.empty());

        FeedEventResponse response = socialService.createFeedPost("captain@test.com", request);

        assertEquals(200L, response.getId());
        assertEquals("PUBLIC", response.getVisibility());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals(1, response.getMediaUrls().size());
    }

    @Test
    void reactToFeedPostCreatesSingleViewerReactionAndUpdatesCount() {
        User actor = User.builder().id(1L).name("Captain").email("captain@test.com").build();
        User author = User.builder().id(2L).name("Author").email("author@test.com").build();
        FeedEvent post = FeedEvent.builder()
                .id(200L)
                .actor(author)
                .title("Match win")
                .summary("Won by 20 runs")
                .visibility("PUBLIC")
                .status("ACTIVE")
                .build();
        CommunityDtos.FeedReactionRequest request = new CommunityDtos.FeedReactionRequest("WELL_PLAYED");

        when(userRepository.findByEmailOrPhone("captain@test.com")).thenReturn(Optional.of(actor));
        when(feedEventRepository.findById(200L)).thenReturn(Optional.of(post));
        when(feedReactionRepository.findByFeedEventIdAndUserId(200L, 1L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(FeedReaction.builder().feedEvent(post).user(actor).reactionType("WELL_PLAYED").build()));
        when(feedReactionRepository.save(any(FeedReaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(feedReactionRepository.countByFeedEventId(200L)).thenReturn(1L);
        when(feedEventRepository.save(post)).thenReturn(post);

        FeedEventResponse response = socialService.reactToFeedPost("captain@test.com", 200L, request);

        assertEquals(1, response.getReactionCount());
        assertEquals("WELL_PLAYED", response.getViewerReaction());
        assertEquals(3, response.getEngagementScore());
    }

    @Test
    void addFeedCommentUpdatesCounterAndReturnsComment() {
        User actor = User.builder().id(1L).name("Captain").email("captain@test.com").build();
        FeedEvent post = FeedEvent.builder()
                .id(200L)
                .actor(actor)
                .title("Practice")
                .summary("Who is in?")
                .visibility("PUBLIC")
                .status("ACTIVE")
                .build();
        CommunityDtos.FeedCommentRequest request = new CommunityDtos.FeedCommentRequest("I am joining.", null);

        when(userRepository.findByEmailOrPhone("captain@test.com")).thenReturn(Optional.of(actor));
        when(feedEventRepository.findById(200L)).thenReturn(Optional.of(post));
        when(feedCommentRepository.save(any(FeedComment.class))).thenAnswer(invocation -> {
            FeedComment saved = invocation.getArgument(0);
            saved.setId(300L);
            return saved;
        });
        when(feedCommentRepository.countByFeedEventIdAndStatus(200L, "ACTIVE")).thenReturn(1L);
        when(feedEventRepository.save(post)).thenReturn(post);

        CommunityDtos.FeedCommentResponse response = socialService.addFeedComment("captain@test.com", 200L, request);

        assertEquals(300L, response.getId());
        assertEquals("I am joining.", response.getMessage());
        assertEquals(1, post.getCommentCount());
        assertEquals(5, post.getEngagementScore());
    }

    private Team team(Long id, String name, User captain) {
        Team team = new Team();
        team.setId(id);
        team.setCaptain(captain);
        team.setName(name);
        team.setSport("Cricket");
        team.setCity("Surat");
        team.setVisibility(TeamVisibility.PUBLIC);
        team.setMaxMembers(11);
        return team;
    }
}
