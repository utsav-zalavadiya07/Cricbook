package com.example.Cricbook.service;

import com.example.Cricbook.dto.PlayerPerformanceLeaderboardResponse;
import com.example.Cricbook.dto.PlayerPerformanceResponse;
import com.example.Cricbook.entity.LiveMatchScore;
import com.example.Cricbook.entity.PlayerMatchStats;
import com.example.Cricbook.entity.PlayerProfile;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.LiveMatchStatus;
import com.example.Cricbook.repository.PlayerMatchStatsRepository;
import com.example.Cricbook.repository.PlayerProfileRepository;
import com.example.Cricbook.repository.TeamMemberRepository;
import com.example.Cricbook.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlayerPerformanceServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PlayerProfileRepository playerProfileRepository;
    @Mock private PlayerMatchStatsRepository playerMatchStatsRepository;
    @Mock private TeamMemberRepository teamMemberRepository;

    private PlayerPerformanceService service;
    private User player;
    private User secondPlayer;
    private PlayerProfile profile;

    @BeforeEach
    void setUp() {
        service = new PlayerPerformanceService(userRepository, playerProfileRepository, playerMatchStatsRepository, teamMemberRepository);
        player = User.builder().id(7L).name("Virat Player").email("virat@test.com").phone("777").build();
        secondPlayer = User.builder().id(8L).name("Rohit Player").email("rohit@test.com").phone("888").build();
        profile = PlayerProfile.builder()
                .user(player)
                .city("Ahmedabad")
                .bio("Opening batter")
                .photoUrl("/uploads/virat.png")
                .matchesPlayed(2)
                .wins(1)
                .mvpCount(1)
                .rating(4.2)
                .build();
    }

    @Test
    void playerPerformanceAggregatesCareerStatsAndAchievements() {
        List<PlayerMatchStats> stats = List.of(
                stat(player, 100L, "Falcons vs Strikers", 66, 36, 5, 4, 2, 2.0, 18),
                stat(player, 101L, "Falcons vs Royals", 34, 24, 3, 1, 1, 1.0, 10)
        );
        when(userRepository.findById(7L)).thenReturn(Optional.of(player));
        when(playerProfileRepository.findByUserId(7L)).thenReturn(Optional.of(profile));
        when(playerMatchStatsRepository.findByUserIdWithScoreOrderByRecent(7L)).thenReturn(stats);

        PlayerPerformanceResponse response = service.playerPerformance(7L);

        assertEquals(2, response.getCareer().getMatches());
        assertEquals(100, response.getBatting().getRuns());
        assertEquals(66, response.getBatting().getHighestScore());
        assertEquals(3, response.getBowling().getWickets());
        assertEquals("2/18", response.getBowling().getBestFigures());
        assertTrue(response.getAchievements().stream().anyMatch(item -> item.getCode().equals("FIRST_FIFTY") && item.getUnlocked()));
        assertFalse(response.getTimeline().isEmpty());
    }

    @Test
    void leaderboardRanksPlayersByRunsAndFiltersCity() {
        PlayerProfile secondProfile = PlayerProfile.builder()
                .user(secondPlayer)
                .city("Ahmedabad")
                .matchesPlayed(1)
                .wins(0)
                .mvpCount(0)
                .rating(4.0)
                .build();
        List<PlayerMatchStats> allStats = List.of(
                stat(player, 100L, "Falcons vs Strikers", 80, 40, 6, 5, 1, 1.0, 12),
                stat(secondPlayer, 102L, "Royals vs Titans", 120, 54, 10, 8, 0, 0.0, 0)
        );
        when(playerMatchStatsRepository.findAllWithUserAndScoreOrderByRecent()).thenReturn(allStats);
        when(playerProfileRepository.findByUserId(7L)).thenReturn(Optional.of(profile));
        when(playerProfileRepository.findByUserId(8L)).thenReturn(Optional.of(secondProfile));

        List<PlayerPerformanceLeaderboardResponse> leaderboard = service.leaderboard("RUNS", "Ahmedabad", 10);

        assertEquals(2, leaderboard.size());
        assertEquals(1, leaderboard.get(0).getRank());
        assertEquals(secondPlayer.getId(), leaderboard.get(0).getUserId());
        assertEquals(120, leaderboard.get(0).getRuns());
    }

    @Test
    void playerWithNoCompletedScorecardsReturnsStableEmptyProfile() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(player));
        when(playerProfileRepository.findByUserId(7L)).thenReturn(Optional.of(profile));
        when(playerMatchStatsRepository.findByUserIdWithScoreOrderByRecent(7L)).thenReturn(List.of());

        PlayerPerformanceResponse response = service.playerPerformance(7L);

        assertEquals(0, response.getCareer().getMatches());
        assertEquals(0, response.getBatting().getRuns());
        assertEquals(0, response.getBowling().getWickets());
        assertTrue(response.getInsight().contains("No completed scored matches"));
    }

    private PlayerMatchStats stat(User user,
                                  Long scoreId,
                                  String title,
                                  int runs,
                                  int balls,
                                  int fours,
                                  int sixes,
                                  int wickets,
                                  double overs,
                                  int conceded) {
        LiveMatchScore score = LiveMatchScore.builder()
                .id(scoreId)
                .scorer(user)
                .title(title)
                .teamAName("A")
                .teamBName("B")
                .battingTeam("A")
                .bowlingTeam("B")
                .innings(2)
                .runs(0)
                .wickets(0)
                .ballsBowled(0)
                .extras(0)
                .maxOvers(20)
                .status(LiveMatchStatus.COMPLETED)
                .locked(false)
                .isTie(false)
                .isSuperOver(false)
                .createdAt(LocalDateTime.now().minusDays(scoreId % 10))
                .updatedAt(LocalDateTime.now().minusDays(scoreId % 10))
                .build();
        return PlayerMatchStats.builder()
                .id(scoreId)
                .user(user)
                .liveScore(score)
                .runsScored(runs)
                .ballsFaced(balls)
                .fours(fours)
                .sixes(sixes)
                .wicketsTaken(wickets)
                .oversBowled(overs)
                .runsConceded(conceded)
                .catches(1)
                .runouts(0)
                .build();
    }
}
