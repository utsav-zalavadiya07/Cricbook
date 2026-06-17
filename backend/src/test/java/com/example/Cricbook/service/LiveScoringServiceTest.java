package com.example.Cricbook.service;

import com.example.Cricbook.dto.*;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.FixtureStatus;
import com.example.Cricbook.enums.LiveMatchStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveScoringServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TournamentFixtureRepository fixtureRepository;
    @Mock private PublicMatchRepository publicMatchRepository;
    @Mock private MatchParticipantRepository matchParticipantRepository;
    @Mock private LiveMatchScoreRepository liveMatchScoreRepository;
    @Mock private ScoreEventRepository scoreEventRepository;
    @Mock private InningsRepository inningsRepository;
    @Mock private PlayerMatchStatsRepository playerMatchStatsRepository;
    @Mock private MatchAnalyticsRepository matchAnalyticsRepository;
    @Mock private NotificationService notificationService;
    @Mock private PlayerProfileRepository playerProfileRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private TournamentService tournamentService;
    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks
    private LiveScoringService liveScoringService;

    private User scorer;
    private LiveMatchScore score;

    @BeforeEach
    void setUp() {
        scorer = User.builder().id(1L).name("Scorer").email("s@test.com").build();
        score = LiveMatchScore.builder()
                .id(10L)
                .scorer(scorer)
                .title("A vs B")
                .teamAName("A")
                .teamBName("B")
                .battingTeam("A")
                .bowlingTeam("B")
                .innings(1)
                .runs(0)
                .wickets(0)
                .ballsBowled(0)
                .extras(0)
                .maxOvers(2)
                .status(LiveMatchStatus.LIVE)
                .locked(false)
                .isTie(false)
                .isSuperOver(false)
                .build();
    }

    @Test
    void startMatchTransitionsState() {
        score.setStatus(LiveMatchStatus.NOT_STARTED);
        when(userRepository.findByEmailOrPhone("s@test.com")).thenReturn(Optional.of(scorer));
        when(liveMatchScoreRepository.findById(10L)).thenReturn(Optional.of(score));
        when(liveMatchScoreRepository.save(any(LiveMatchScore.class))).thenAnswer(i -> i.getArgument(0));

        LiveScoreResponse response = liveScoringService.startMatch("s@test.com", 10L);

        assertEquals(LiveMatchStatus.LIVE, response.getStatus());
        verify(notificationService, times(1)).pushNotification(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void recordBallWideRunsDoesNotIncrementOvers() {
        ScoreEventRequest request = new ScoreEventRequest();
        request.setRuns(0);
        request.setExtraRuns(1);
        request.setExtraType("WIDE");
        request.setBatter("Virat");
        request.setBowler("Bumrah");

        when(userRepository.findByEmailOrPhone("s@test.com")).thenReturn(Optional.of(scorer));
        when(liveMatchScoreRepository.findById(10L)).thenReturn(Optional.of(score));
        when(scoreEventRepository.countByLiveScoreId(10L)).thenReturn(0L);
        when(scoreEventRepository.save(any(ScoreEvent.class))).thenAnswer(i -> i.getArgument(0));
        when(liveMatchScoreRepository.save(score)).thenReturn(score);

        LiveScoreDetailResponse response = liveScoringService.recordBall("s@test.com", 10L, request);

        assertEquals(1, response.getScore().getRuns());
        assertEquals(1, response.getScore().getExtras());
        assertEquals("0.0", response.getScore().getOvers());
    }

    @Test
    void recordBallWicketIncrementsWickets() {
        ScoreEventRequest request = new ScoreEventRequest();
        request.setRuns(0);
        request.setWicket(true);
        request.setWicketType("BOWLED");
        request.setBatter("Virat");
        request.setBowler("Bumrah");

        when(userRepository.findByEmailOrPhone("s@test.com")).thenReturn(Optional.of(scorer));
        when(liveMatchScoreRepository.findById(10L)).thenReturn(Optional.of(score));
        when(scoreEventRepository.countByLiveScoreId(10L)).thenReturn(0L);
        when(scoreEventRepository.save(any(ScoreEvent.class))).thenAnswer(i -> i.getArgument(0));
        when(liveMatchScoreRepository.save(score)).thenReturn(score);

        LiveScoreDetailResponse response = liveScoringService.recordBall("s@test.com", 10L, request);

        assertEquals(1, response.getScore().getWickets());
        assertEquals("0.1", response.getScore().getOvers());
    }

    @Test
    void undoBallRemovesLastEventAndRecalculates() {
        ScoreEvent event1 = ScoreEvent.builder().id(1L).liveScore(score).eventSequence(1).runs(4).extraRuns(0).wicket(false).batter("A").bowler("B").build();
        List<ScoreEvent> events = new ArrayList<>(List.of(event1));

        when(userRepository.findByEmailOrPhone("s@test.com")).thenReturn(Optional.of(scorer));
        when(liveMatchScoreRepository.findById(10L)).thenReturn(Optional.of(score));
        when(scoreEventRepository.findByLiveScoreIdOrderByEventSequenceAsc(10L)).thenReturn(events);

        LiveScoreDetailResponse response = liveScoringService.undoBall("s@test.com", 10L);

        verify(scoreEventRepository, times(1)).delete(event1);
    }

    @Test
    void pauseAndResumeMatchCorrectlyTransitions() {
        when(userRepository.findByEmailOrPhone("s@test.com")).thenReturn(Optional.of(scorer));
        when(liveMatchScoreRepository.findById(10L)).thenReturn(Optional.of(score));
        when(liveMatchScoreRepository.save(any(LiveMatchScore.class))).thenAnswer(i -> i.getArgument(0));

        LiveScoreResponse pauseResp = liveScoringService.pauseMatch("s@test.com", 10L);
        assertEquals(LiveMatchStatus.PAUSED, pauseResp.getStatus());

        score.setStatus(LiveMatchStatus.PAUSED);
        LiveScoreResponse resumeResp = liveScoringService.resumeMatch("s@test.com", 10L);
        assertEquals(LiveMatchStatus.LIVE, resumeResp.getStatus());
    }

    @Test
    void getAnalyticsCalculatesWormAndPartnerships() {
        ScoreEvent event1 = ScoreEvent.builder().id(1L).eventSequence(1).runs(4).extraRuns(0).wicket(false).batter("A").bowler("B").build();
        ScoreEvent event2 = ScoreEvent.builder().id(2L).eventSequence(2).runs(0).extraRuns(0).wicket(true).batter("A").bowler("B").build();

        when(liveMatchScoreRepository.findById(10L)).thenReturn(Optional.of(score));
        when(scoreEventRepository.findByLiveScoreIdOrderByEventSequenceAsc(10L)).thenReturn(List.of(event1, event2));

        MatchAnalyticsResponse analytics = liveScoringService.getAnalytics(10L);

        assertNotNull(analytics);
        assertEquals(1, analytics.getPartnerships().size());
        assertEquals(4, analytics.getPartnerships().get(0).getRuns());
    }

    @Test
    void completeScoreUpdatesExistingPlayerStatsWithoutInflatingProfile() {
        User player = User.builder().id(2L).name("Virat").email("virat@test.com").phone("222").build();
        PlayerMatchStats existingStats = PlayerMatchStats.builder()
                .id(44L)
                .user(player)
                .liveScore(score)
                .runsScored(1)
                .ballsFaced(1)
                .fours(0)
                .sixes(0)
                .wicketsTaken(0)
                .oversBowled(0.0)
                .runsConceded(0)
                .catches(0)
                .runouts(0)
                .build();
        PlayerProfile profile = PlayerProfile.builder()
                .user(player)
                .matchesPlayed(3)
                .wins(1)
                .mvpCount(0)
                .rating(4.1)
                .build();
        ScoreEvent event = ScoreEvent.builder()
                .id(1L)
                .liveScore(score)
                .eventSequence(1)
                .runs(4)
                .extraRuns(0)
                .wicket(false)
                .batter("Virat")
                .bowler("Bumrah")
                .build();

        when(userRepository.findByEmailOrPhone(anyString())).thenAnswer(invocation -> {
            String login = invocation.getArgument(0);
            if ("s@test.com".equals(login)) return Optional.of(scorer);
            return Optional.empty();
        });
        when(liveMatchScoreRepository.findById(10L)).thenReturn(Optional.of(score));
        when(scoreEventRepository.findByLiveScoreIdOrderByEventSequenceAsc(10L)).thenReturn(List.of(event));
        when(userRepository.findAll()).thenReturn(List.of(player));
        when(playerMatchStatsRepository.findByLiveScoreIdAndUserId(10L, 2L)).thenReturn(Optional.of(existingStats));
        when(playerMatchStatsRepository.save(any(PlayerMatchStats.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(playerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
        when(inningsRepository.findByLiveScoreIdOrderByInningsNumberAsc(10L)).thenReturn(List.of());
        when(liveMatchScoreRepository.save(score)).thenReturn(score);

        liveScoringService.completeScore("s@test.com", 10L);

        assertEquals(4, existingStats.getRunsScored());
        assertEquals(1, existingStats.getBallsFaced());
        assertEquals(3, profile.getMatchesPlayed());
        assertEquals(4.1, profile.getRating());
    }

    @Test
    void getScoreBuildsDigitalScorecardAndFallOfWickets() {
        ScoreEvent boundary = ScoreEvent.builder()
                .id(1L)
                .liveScore(score)
                .eventSequence(1)
                .runs(4)
                .extraRuns(0)
                .wicket(false)
                .batter("Virat")
                .bowler("Bumrah")
                .note("0.1 FOUR")
                .build();
        ScoreEvent wicket = ScoreEvent.builder()
                .id(2L)
                .liveScore(score)
                .eventSequence(2)
                .runs(0)
                .extraRuns(0)
                .wicket(true)
                .wicketType("BOWLED")
                .batter("Virat")
                .bowler("Bumrah")
                .note("0.2 OUT")
                .build();

        when(liveMatchScoreRepository.findById(10L)).thenReturn(Optional.of(score));
        when(scoreEventRepository.findByLiveScoreIdOrderByEventSequenceAsc(10L)).thenReturn(List.of(boundary, wicket));

        LiveScoreDetailResponse response = liveScoringService.getScore(10L);

        assertEquals(1, response.getInnings().size());
        assertEquals(4, response.getBattingScorecard().get(0).getRuns());
        assertTrue(response.getBattingScorecard().get(0).getOut());
        assertEquals(1, response.getBowlingScorecard().get(0).getWickets());
        assertEquals(1, response.getFallOfWickets().size());
        assertEquals("0.2", response.getFallOfWickets().get(0).getOver());
    }

    @Test
    void completeScoreSyncsTournamentFixtureWhenBothInningsExist() {
        Team teamA = Team.builder().id(100L).name("A").captain(scorer).sport("Cricket").city("Surat").maxMembers(11).build();
        Team teamB = Team.builder().id(101L).name("B").captain(scorer).sport("Cricket").city("Surat").maxMembers(11).build();
        Tournament tournament = Tournament.builder().id(5L).organizer(scorer).name("Cup").sport("Cricket").city("Surat").venue("Arena").build();
        TournamentFixture fixture = TournamentFixture.builder()
                .id(55L)
                .tournament(tournament)
                .teamA(teamA)
                .teamB(teamB)
                .round(1)
                .status(FixtureStatus.SCHEDULED)
                .build();
        score.setFixture(fixture);
        score.setInnings(2);
        score.setBattingTeam("B");
        score.setBowlingTeam("A");
        score.setRuns(8);
        score.setWickets(2);
        score.setBallsBowled(12);
        score.setExtras(1);

        Innings firstInnings = Innings.builder()
                .liveScore(score)
                .inningsNumber(1)
                .battingTeamName("A")
                .runs(10)
                .wickets(2)
                .ballsBowled(12)
                .extras(0)
                .build();
        Innings secondInnings = Innings.builder()
                .liveScore(score)
                .inningsNumber(2)
                .battingTeamName("B")
                .runs(8)
                .wickets(2)
                .ballsBowled(12)
                .extras(1)
                .build();

        when(userRepository.findByEmailOrPhone("s@test.com")).thenReturn(Optional.of(scorer));
        when(liveMatchScoreRepository.findById(10L)).thenReturn(Optional.of(score));
        when(scoreEventRepository.findByLiveScoreIdOrderByEventSequenceAsc(10L)).thenReturn(List.of());
        when(inningsRepository.findByLiveScoreIdOrderByInningsNumberAsc(10L))
                .thenReturn(List.of(firstInnings))
                .thenReturn(List.of(firstInnings, secondInnings));
        when(liveMatchScoreRepository.save(score)).thenReturn(score);

        liveScoringService.completeScore("s@test.com", 10L);

        verify(tournamentService).recordFixtureResult(eq("s@test.com"), eq(55L), argThat(request ->
                request.getTeamAScore().equals(10) && request.getTeamBScore().equals(8)));
    }

    @Test
    void recordBallRejectsInvalidExtraType() {
        ScoreEventRequest request = new ScoreEventRequest();
        request.setRuns(0);
        request.setExtraRuns(1);
        request.setExtraType("PENALTY");

        when(userRepository.findByEmailOrPhone("s@test.com")).thenReturn(Optional.of(scorer));
        when(liveMatchScoreRepository.findById(10L)).thenReturn(Optional.of(score));

        ApiException exception = assertThrows(ApiException.class, () -> liveScoringService.recordBall("s@test.com", 10L, request));

        assertEquals("Invalid extra type", exception.getMessage());
    }
}
