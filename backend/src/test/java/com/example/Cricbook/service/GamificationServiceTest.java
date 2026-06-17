package com.example.Cricbook.service;

import com.example.Cricbook.dto.GamificationDtos.*;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.*;
import com.example.Cricbook.repository.*;
import org.springframework.dao.DataAccessResourceFailureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GamificationServiceTest {
    @Mock private PlayerXpRepository playerXpRepository;
    @Mock private PlayerStreakRepository playerStreakRepository;
    @Mock private AchievementRepository achievementRepository;
    @Mock private PlayerAchievementRepository playerAchievementRepository;
    @Mock private PlayerBadgeRepository playerBadgeRepository;
    @Mock private ChallengeRepository challengeRepository;
    @Mock private PlayerChallengeRepository playerChallengeRepository;
    @Mock private PlayerRewardRepository playerRewardRepository;
    @Mock private LeaderboardCacheRepository leaderboardCacheRepository;
    @Mock private SeasonalEventRepository seasonalEventRepository;
    @Mock private TeamCompetitionRepository teamCompetitionRepository;
    @Mock private UserRepository userRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private LoyaltyService loyaltyService;
    @Mock private NotificationService notificationService;

    private GamificationService service;
    private User user;
    private final List<PlayerXp> xpStore = new ArrayList<>();
    private final List<PlayerStreak> streakStore = new ArrayList<>();
    private final List<PlayerReward> rewardStore = new ArrayList<>();
    private final List<PlayerChallenge> playerChallengeStore = new ArrayList<>();
    private final List<Achievement> achievementStore = new ArrayList<>();
    private final List<Challenge> challengeStore = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new GamificationService(
                playerXpRepository,
                playerStreakRepository,
                achievementRepository,
                playerAchievementRepository,
                playerBadgeRepository,
                challengeRepository,
                playerChallengeRepository,
                playerRewardRepository,
                leaderboardCacheRepository,
                seasonalEventRepository,
                teamCompetitionRepository,
                userRepository,
                bookingRepository,
                teamRepository,
                loyaltyService,
                notificationService
        );
        user = User.builder().id(1L).name("Utsav Player").email("player@test.com").phone("111").build();
        Achievement firstBooking = Achievement.builder().id(10L).name("First Booking").category(AchievementCategory.ATTENDANCE).triggerType(GamificationActivityType.BOOKING).threshold(1).xpReward(50).active(true).build();
        Challenge bookChallenge = Challenge.builder().id(20L).title("Book With Friends").description("Book once.").activityType(GamificationActivityType.BOOKING).target(1).xpReward(75).rewardLabel("Booking reward").active(true).build();
        achievementStore.add(firstBooking);
        challengeStore.add(bookChallenge);

        when(userRepository.findByEmail("player@test.com")).thenReturn(Optional.of(user));
        when(playerXpRepository.findByUserId(1L)).thenAnswer(invocation -> xpStore.stream().findFirst());
        when(playerXpRepository.save(any(PlayerXp.class))).thenAnswer(invocation -> {
            PlayerXp xp = invocation.getArgument(0);
            if (xp.getId() == null) xp.setId(1L);
            xpStore.removeIf(item -> item.getId().equals(xp.getId()));
            xpStore.add(xp);
            return xp;
        });
        when(playerStreakRepository.findByUserIdAndStreakType(eq(1L), any())).thenAnswer(invocation -> {
            StreakType type = invocation.getArgument(1);
            return streakStore.stream().filter(item -> item.getStreakType() == type).findFirst();
        });
        when(playerStreakRepository.save(any(PlayerStreak.class))).thenAnswer(invocation -> {
            PlayerStreak streak = invocation.getArgument(0);
            if (streak.getId() == null) streak.setId((long) (streakStore.size() + 1));
            streakStore.removeIf(item -> item.getId().equals(streak.getId()));
            streakStore.add(streak);
            return streak;
        });
        when(playerStreakRepository.findByUserIdOrderByStreakTypeAsc(1L)).thenReturn(streakStore);
        when(playerStreakRepository.findTop10ByOrderByBestStreakDescUpdatedAtDesc()).thenReturn(streakStore);

        when(achievementRepository.count()).thenReturn(1L);
        when(achievementRepository.findByActiveTrueOrderByThresholdAsc()).thenReturn(achievementStore);
        when(achievementRepository.findByTriggerTypeAndActiveTrueOrderByThresholdAsc(any())).thenAnswer(invocation -> {
            GamificationActivityType type = invocation.getArgument(0);
            return achievementStore.stream().filter(item -> item.getTriggerType() == type).toList();
        });
        when(playerAchievementRepository.findByUserIdOrderByEarnedAtDesc(1L)).thenReturn(List.of());
        when(playerAchievementRepository.findByUserIdAndAchievementId(eq(1L), anyLong())).thenReturn(Optional.empty());
        when(playerAchievementRepository.save(any(PlayerAchievement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(challengeRepository.count()).thenReturn(1L);
        when(challengeRepository.findByActiveTrueOrderByExpiresAtAsc()).thenReturn(challengeStore);
        when(playerChallengeRepository.findByUserIdAndChallengeId(eq(1L), anyLong())).thenAnswer(invocation -> {
            Long challengeId = invocation.getArgument(1);
            return playerChallengeStore.stream().filter(item -> item.getChallenge().getId().equals(challengeId)).findFirst();
        });
        when(playerChallengeRepository.save(any(PlayerChallenge.class))).thenAnswer(invocation -> {
            PlayerChallenge challenge = invocation.getArgument(0);
            if (challenge.getId() == null) challenge.setId((long) (playerChallengeStore.size() + 1));
            playerChallengeStore.removeIf(item -> item.getId().equals(challenge.getId()));
            playerChallengeStore.add(challenge);
            return challenge;
        });
        when(playerChallengeRepository.findByUserIdOrderByUpdatedAtDesc(1L)).thenReturn(playerChallengeStore);

        when(playerRewardRepository.findByUserIdOrderByUpdatedAtDesc(1L)).thenReturn(rewardStore);
        when(playerRewardRepository.save(any(PlayerReward.class))).thenAnswer(invocation -> {
            PlayerReward reward = invocation.getArgument(0);
            if (reward.getId() == null) reward.setId((long) (rewardStore.size() + 1));
            rewardStore.removeIf(item -> item.getId().equals(reward.getId()));
            rewardStore.add(reward);
            return reward;
        });
        when(playerBadgeRepository.findByUserIdOrderByEarnedAtDesc(1L)).thenReturn(List.of());
        when(playerBadgeRepository.findByUserIdAndBadge(eq(1L), anyString())).thenReturn(Optional.empty());
        when(playerBadgeRepository.save(any(PlayerBadge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(leaderboardCacheRepository.findTop10ByLeaderboardTypeAndCityIgnoreCaseOrderByRankAsc(any(), anyString())).thenReturn(List.of());
        when(seasonalEventRepository.count()).thenReturn(1L);
        when(seasonalEventRepository.findByActiveTrueAndEndDateGreaterThanEqualOrderByStartDateAsc(any(LocalDate.class))).thenReturn(List.of());
        when(teamRepository.findByCaptainIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(teamCompetitionRepository.findTop10ByOrderByPointsDescUpdatedAtDesc()).thenReturn(List.of());
        when(teamRepository.findAll()).thenReturn(List.of());
    }

    @Test
    void profileCreatesDefaultXpAndChallenges() {
        GamificationProfileResponse response = service.profile("player@test.com");

        assertEquals(1, response.getXp().getLevel());
        assertEquals(1, response.getChallenges().size());
        assertEquals("Book With Friends", response.getChallenges().get(0).getTitle());
    }

    @Test
    void awardingBookingXpUpdatesLevelStreakChallengeAndReward() {
        when(bookingRepository.findByUserId(1L)).thenReturn(List.of(Booking.builder().id(99L).user(user).build()));

        GamificationProfileResponse response = service.awardXp("player@test.com", AwardXpRequest.builder()
                .activityType(GamificationActivityType.BOOKING)
                .points(260)
                .reason("Test booking")
                .build());

        assertTrue(response.getXp().getLevel() >= 2);
        assertEquals(1, response.getStreaks().get(0).getCurrentStreak());
        assertTrue(response.getRewards().stream().anyMatch(reward -> reward.getStatus() == GamificationRewardStatus.AVAILABLE));
        verify(notificationService, atLeastOnce()).pushNotification(eq(1L), eq("player@test.com"), anyString(), anyString());
    }

    @Test
    void claimRewardMarksRewardClaimedAndCreditsLoyalty() {
        PlayerReward reward = PlayerReward.builder().id(5L).user(user).rewardType("COUPON").label("XP coupon").status(GamificationRewardStatus.AVAILABLE).build();
        when(playerRewardRepository.findById(5L)).thenReturn(Optional.of(reward));
        when(playerRewardRepository.save(any(PlayerReward.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RewardResponse response = service.claimReward("player@test.com", 5L);

        assertEquals(GamificationRewardStatus.CLAIMED, response.getStatus());
        verify(loyaltyService).earnPoints(eq(user), eq(25), contains("Claimed gamification reward"));
    }

    @Test
    void leaderboardReturnsRankedXpPlayers() {
        xpStore.add(PlayerXp.builder().id(1L).user(user).xp(500).level(3).build());
        User second = User.builder().id(2L).name("Second Player").email("second@test.com").phone("222").build();
        xpStore.add(PlayerXp.builder().id(2L).user(second).xp(300).level(2).build());
        when(playerXpRepository.findTop10ByOrderByXpDescUpdatedAtDesc()).thenReturn(xpStore);

        List<LeaderboardEntryResponse> response = service.leaderboard("Global", LeaderboardType.HIGHEST_XP);

        assertEquals(2, response.size());
        assertEquals(1, response.get(0).getRank());
        assertEquals(500, response.get(0).getScore());
    }

    @Test
    void leaderboardFallsBackToLiveDataWhenCacheTableIsUnavailable() {
        xpStore.add(PlayerXp.builder().id(1L).user(user).xp(500).level(3).build());
        when(playerXpRepository.findTop10ByOrderByXpDescUpdatedAtDesc()).thenReturn(xpStore);
        when(leaderboardCacheRepository.findTop10ByLeaderboardTypeAndCityIgnoreCaseOrderByRankAsc(any(), anyString()))
                .thenThrow(new DataAccessResourceFailureException("missing cache table"));

        List<LeaderboardEntryResponse> response = service.leaderboard("Global", LeaderboardType.HIGHEST_XP);

        assertEquals(1, response.size());
        assertEquals("Utsav Player", response.get(0).getPlayerName());
    }

    @Test
    void ownerAnalyticsFallsBackWhenTeamCompetitionCacheIsUnavailable() {
        xpStore.add(PlayerXp.builder().id(1L).user(user).xp(500).level(3).build());
        when(playerXpRepository.findTop10ByOrderByXpDescUpdatedAtDesc()).thenReturn(xpStore);
        when(teamCompetitionRepository.findTop10ByOrderByPointsDescUpdatedAtDesc())
                .thenThrow(new DataAccessResourceFailureException("missing team competition table"));
        when(userRepository.findByEmailOrPhone("player@test.com")).thenReturn(Optional.of(user));

        GamificationAnalyticsResponse response = service.ownerAnalytics("player@test.com");

        assertEquals(0, response.getTeamCompetitions().size());
        assertEquals(1, response.getTopPlayers().size());
    }
}
