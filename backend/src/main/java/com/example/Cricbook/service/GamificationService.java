package com.example.Cricbook.service;

import com.example.Cricbook.dto.GamificationDtos.*;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationService {
    private static final int XP_PER_LEVEL = 250;

    private final PlayerXpRepository playerXpRepository;
    private final PlayerStreakRepository playerStreakRepository;
    private final AchievementRepository achievementRepository;
    private final PlayerAchievementRepository playerAchievementRepository;
    private final PlayerBadgeRepository playerBadgeRepository;
    private final ChallengeRepository challengeRepository;
    private final PlayerChallengeRepository playerChallengeRepository;
    private final PlayerRewardRepository playerRewardRepository;
    private final LeaderboardCacheRepository leaderboardCacheRepository;
    private final SeasonalEventRepository seasonalEventRepository;
    private final TeamCompetitionRepository teamCompetitionRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final TeamRepository teamRepository;
    private final LoyaltyService loyaltyService;
    private final NotificationService notificationService;

    @Transactional
    public GamificationProfileResponse profile(String loginId) {
        seedDefaults();
        User user = findUser(loginId);
        PlayerXp xp = getOrCreateXp(user);
        ensurePlayerChallenges(user);
        ensureBaselineBadgesAndRewards(user, xp);
        return buildProfile(user, xp);
    }

    @Transactional
    public GamificationProfileResponse awardXp(String loginId, AwardXpRequest request) {
        if (request == null || request.getActivityType() == null) {
            throw ApiException.badRequest("Select the activity that should receive XP.");
        }
        seedDefaults();
        User user = findUser(loginId);
        int points = Optional.ofNullable(request.getPoints()).filter(value -> value > 0)
                .orElse(defaultPoints(request.getActivityType()));
        PlayerXp xp = getOrCreateXp(user);
        int previousLevel = xp.getLevel();
        xp.setXp(Math.max(0, xp.getXp() + points));
        xp.setLevel(levelForXp(xp.getXp()));
        xp.setLastActivity(Optional.ofNullable(request.getReason()).filter(reason -> !reason.isBlank())
                .orElse(labelForActivity(request.getActivityType())));
        playerXpRepository.save(xp);

        updateStreak(user, streakForActivity(request.getActivityType()));
        progressChallenges(user, request.getActivityType());
        unlockAchievements(user, request.getActivityType(), xp);
        ensureBaselineBadgesAndRewards(user, xp);
        rebuildLeaderboard();

        if (xp.getLevel() > previousLevel) {
            notificationService.pushNotification(user.getId(), user.getEmail(), "Level up", "You reached Level " + xp.getLevel() + " on Cricbook.");
        }
        return buildProfile(user, xp);
    }

    @Transactional
    public RewardResponse claimReward(String loginId, Long rewardId) {
        User user = findUser(loginId);
        PlayerReward reward = playerRewardRepository.findById(rewardId)
                .orElseThrow(() -> ApiException.notFound("Reward was not found."));
        if (!reward.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("You can only claim your own rewards.");
        }
        if (reward.getStatus() == GamificationRewardStatus.CLAIMED) {
            throw ApiException.conflict("This reward has already been claimed.");
        }
        if (reward.getStatus() == GamificationRewardStatus.LOCKED) {
            throw ApiException.badRequest("This reward is still locked.");
        }
        reward.setStatus(GamificationRewardStatus.CLAIMED);
        reward.setClaimedAt(LocalDateTime.now());
        playerRewardRepository.save(reward);
        if (loyaltyService != null) {
            loyaltyService.earnPoints(user, 25, "Claimed gamification reward: " + reward.getLabel());
        }
        return mapReward(reward);
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> leaderboard(String city, LeaderboardType type) {
        LeaderboardType resolvedType = Optional.ofNullable(type).orElse(LeaderboardType.HIGHEST_XP);
        String resolvedCity = Optional.ofNullable(city).filter(value -> !value.isBlank()).orElse("Global");
        try {
            List<LeaderboardEntryResponse> cached = leaderboardCacheRepository.findTop10ByLeaderboardTypeAndCityIgnoreCaseOrderByRankAsc(resolvedType, resolvedCity)
                    .stream()
                    .map(this::mapLeaderboard)
                    .toList();
            if (!cached.isEmpty()) {
                return cached;
            }
        } catch (DataAccessException ex) {
            log.warn("Leaderboard cache is unavailable; falling back to live leaderboard.", ex);
        }
        return buildLiveLeaderboard(resolvedType, resolvedCity);
    }

    @Transactional(readOnly = true)
    public GamificationAnalyticsResponse ownerAnalytics(String loginId) {
        User owner = findUser(loginId);
        long ownedTeams = teamRepository.findByCaptainIdOrderByCreatedAtDesc(owner.getId()).size();
        List<LeaderboardEntryResponse> topPlayers = buildLiveLeaderboard(LeaderboardType.HIGHEST_XP, "Global").stream().limit(5).toList();
        return GamificationAnalyticsResponse.builder()
                .activePlayers(playerXpRepository.count())
                .completedChallenges(playerChallengeRepository.countByStatus(ChallengeStatus.COMPLETED))
                .availableRewards(playerRewardRepository.countByStatus(GamificationRewardStatus.AVAILABLE))
                .challengeCompletionRate(completionRate())
                .topPlayers(topPlayers)
                .teamCompetitions(teamCompetitions())
                .insight(ownedTeams > 0
                        ? "Your cricket community has active team engagement. Use rewards for repeat captains and high-streak players."
                        : "No owner-led teams are connected yet. Promote challenges to build recurring community activity.")
                .build();
    }

    @Transactional(readOnly = true)
    public GamificationAnalyticsResponse adminAnalytics() {
        return GamificationAnalyticsResponse.builder()
                .activePlayers(playerXpRepository.count())
                .completedChallenges(playerChallengeRepository.countByStatus(ChallengeStatus.COMPLETED))
                .availableRewards(playerRewardRepository.countByStatus(GamificationRewardStatus.AVAILABLE))
                .challengeCompletionRate(completionRate())
                .topPlayers(buildLiveLeaderboard(LeaderboardType.HIGHEST_XP, "Global"))
                .teamCompetitions(teamCompetitions())
                .insight("Gamification is tracking XP, levels, streaks, achievements, rewards, and leaderboard engagement across the platform.")
                .build();
    }

    private GamificationProfileResponse buildProfile(User user, PlayerXp xp) {
        List<Achievement> achievements = achievementRepository.findByActiveTrueOrderByThresholdAsc();
        Map<Long, PlayerAchievement> earned = playerAchievementRepository.findByUserIdOrderByEarnedAtDesc(user.getId()).stream()
                .collect(Collectors.toMap(item -> item.getAchievement().getId(), item -> item, (a, b) -> a));

        return GamificationProfileResponse.builder()
                .xp(mapXp(user, xp))
                .streaks(playerStreakRepository.findByUserIdOrderByStreakTypeAsc(user.getId()).stream().map(this::mapStreak).toList())
                .achievements(achievements.stream().map(item -> mapAchievement(item, earned.get(item.getId()))).toList())
                .badges(playerBadgeRepository.findByUserIdOrderByEarnedAtDesc(user.getId()).stream().map(this::mapBadge).toList())
                .challenges(playerChallengeRepository.findByUserIdOrderByUpdatedAtDesc(user.getId()).stream().map(this::mapChallenge).toList())
                .rewards(playerRewardRepository.findByUserIdOrderByUpdatedAtDesc(user.getId()).stream().map(this::mapReward).toList())
                .leaderboard(buildLiveLeaderboard(LeaderboardType.HIGHEST_XP, "Global").stream().limit(5).toList())
                .seasonalEvents(seasonalEventRepository.findByActiveTrueAndEndDateGreaterThanEqualOrderByStartDateAsc(LocalDate.now()).stream().map(this::mapEvent).toList())
                .insight(xp.getLevel() >= 10 ? "You are building strong cricket habits. Keep your streak alive this week." : "Book, play, review, and invite friends to unlock higher levels and rewards.")
                .build();
    }

    private void updateStreak(User user, StreakType streakType) {
        PlayerStreak streak = playerStreakRepository.findByUserIdAndStreakType(user.getId(), streakType)
                .orElse(PlayerStreak.builder().user(user).streakType(streakType).currentStreak(0).bestStreak(0).build());
        LocalDate today = LocalDate.now();
        LocalDate last = streak.getLastActivityDate();
        if (last == null) {
            streak.setCurrentStreak(1);
        } else if (last.equals(today)) {
            streak.setCurrentStreak(Math.max(1, streak.getCurrentStreak()));
        } else if (!last.isBefore(today.minusDays(7))) {
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
        } else {
            streak.setCurrentStreak(1);
        }
        streak.setBestStreak(Math.max(streak.getBestStreak(), streak.getCurrentStreak()));
        streak.setLastActivityDate(today);
        playerStreakRepository.save(streak);
    }

    private void progressChallenges(User user, GamificationActivityType activityType) {
        ensurePlayerChallenges(user);
        List<PlayerChallenge> challenges = playerChallengeRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());
        for (PlayerChallenge playerChallenge : challenges) {
            Challenge challenge = playerChallenge.getChallenge();
            if (playerChallenge.getStatus() != ChallengeStatus.ACTIVE || challenge.getActivityType() != activityType) {
                continue;
            }
            if (challenge.getExpiresAt() != null && challenge.getExpiresAt().isBefore(LocalDateTime.now())) {
                playerChallenge.setStatus(ChallengeStatus.EXPIRED);
                playerChallengeRepository.save(playerChallenge);
                continue;
            }
            playerChallenge.setProgress(Math.min(challenge.getTarget(), playerChallenge.getProgress() + 1));
            if (playerChallenge.getProgress() >= challenge.getTarget()) {
                playerChallenge.setStatus(ChallengeStatus.COMPLETED);
                playerChallenge.setCompletedAt(LocalDateTime.now());
                PlayerXp xp = getOrCreateXp(user);
                xp.setXp(xp.getXp() + challenge.getXpReward());
                xp.setLevel(levelForXp(xp.getXp()));
                playerXpRepository.save(xp);
                unlockReward(user, "CHALLENGE", challenge.getRewardLabel() == null ? "Challenge reward" : challenge.getRewardLabel());
            }
            playerChallengeRepository.save(playerChallenge);
        }
    }

    private void unlockAchievements(User user, GamificationActivityType activityType, PlayerXp xp) {
        List<Achievement> achievements = achievementRepository.findByTriggerTypeAndActiveTrueOrderByThresholdAsc(activityType);
        int activityScore = activityType == GamificationActivityType.BOOKING
                ? bookingRepository.findByUserId(user.getId()).size()
                : xp.getXp();
        for (Achievement achievement : achievements) {
            if (activityScore < achievement.getThreshold()) {
                continue;
            }
            if (playerAchievementRepository.findByUserIdAndAchievementId(user.getId(), achievement.getId()).isPresent()) {
                continue;
            }
            playerAchievementRepository.save(PlayerAchievement.builder().user(user).achievement(achievement).build());
            xp.setXp(xp.getXp() + achievement.getXpReward());
            xp.setLevel(levelForXp(xp.getXp()));
            playerXpRepository.save(xp);
            unlockBadge(user, achievement.getName());
            notificationService.pushNotification(user.getId(), user.getEmail(), "Achievement unlocked", achievement.getName() + " is now on your profile.");
        }
    }

    private void ensureBaselineBadgesAndRewards(User user, PlayerXp xp) {
        if (xp.getLevel() >= 5) {
            unlockBadge(user, "Active Player");
        }
        if (xp.getLevel() >= 10) {
            unlockBadge(user, "Cricket Pro");
            unlockReward(user, "PRIORITY_BOOKING", "Priority booking access");
        }
        if (xp.getXp() >= 1000) {
            unlockReward(user, "COUPON", "XP milestone coupon");
        }
    }

    private void unlockBadge(User user, String badge) {
        playerBadgeRepository.findByUserIdAndBadge(user.getId(), badge)
                .orElseGet(() -> playerBadgeRepository.save(PlayerBadge.builder().user(user).badge(badge).build()));
    }

    private void unlockReward(User user, String rewardType, String label) {
        boolean exists = playerRewardRepository.findByUserIdOrderByUpdatedAtDesc(user.getId()).stream()
                .anyMatch(reward -> reward.getRewardType().equals(rewardType) && reward.getLabel().equals(label));
        if (!exists) {
            playerRewardRepository.save(PlayerReward.builder()
                    .user(user)
                    .rewardType(rewardType)
                    .label(label)
                    .status(GamificationRewardStatus.AVAILABLE)
                    .unlockedAt(LocalDateTime.now())
                    .build());
        }
    }

    private void ensurePlayerChallenges(User user) {
        for (Challenge challenge : challengeRepository.findByActiveTrueOrderByExpiresAtAsc()) {
            playerChallengeRepository.findByUserIdAndChallengeId(user.getId(), challenge.getId())
                    .orElseGet(() -> playerChallengeRepository.save(PlayerChallenge.builder()
                            .user(user)
                            .challenge(challenge)
                            .progress(0)
                            .status(ChallengeStatus.ACTIVE)
                            .build()));
        }
    }

    private void seedDefaults() {
        if (achievementRepository.count() == 0) {
            achievementRepository.save(Achievement.builder().name("First Booking").category(AchievementCategory.ATTENDANCE).triggerType(GamificationActivityType.BOOKING).threshold(1).xpReward(50).build());
            achievementRepository.save(Achievement.builder().name("Weekend Warrior").category(AchievementCategory.PERFORMANCE).triggerType(GamificationActivityType.MATCH_PLAYED).threshold(500).xpReward(100).build());
            achievementRepository.save(Achievement.builder().name("Community Builder").category(AchievementCategory.COMMUNITY).triggerType(GamificationActivityType.REFERRAL).threshold(200).xpReward(150).build());
        }
        if (challengeRepository.count() == 0) {
            LocalDateTime expiry = LocalDateTime.now().plusDays(7);
            challengeRepository.save(Challenge.builder().title("Book With Friends").description("Create a booking this week and start a team habit.").activityType(GamificationActivityType.BOOKING).target(1).xpReward(75).rewardLabel("Booking streak booster").expiresAt(expiry).build());
            challengeRepository.save(Challenge.builder().title("Play Two Matches").description("Complete two cricket sessions this week.").activityType(GamificationActivityType.MATCH_PLAYED).target(2).xpReward(150).rewardLabel("Match day reward").expiresAt(expiry).build());
            challengeRepository.save(Challenge.builder().title("Invite A Player").description("Bring one player into your Cricbook network.").activityType(GamificationActivityType.REFERRAL).target(1).xpReward(100).rewardLabel("Community builder reward").expiresAt(expiry).build());
        }
        if (seasonalEventRepository.count() == 0) {
            seasonalEventRepository.save(SeasonalEvent.builder()
                    .name("Weekend Sprint")
                    .description("Earn bonus XP by booking, playing, and keeping your weekly cricket streak active.")
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now().plusDays(30))
                    .active(true)
                    .build());
        }
    }

    private void rebuildLeaderboard() {
        // Live leaderboard is used for freshness; cached table remains available for future scheduled materialization.
    }

    private List<LeaderboardEntryResponse> buildLiveLeaderboard(LeaderboardType type, String city) {
        if (type == LeaderboardType.LONGEST_STREAK) {
            return playerStreakRepository.findTop10ByOrderByBestStreakDescUpdatedAtDesc().stream()
                    .map(streak -> LeaderboardEntryResponse.builder()
                            .userId(streak.getUser().getId())
                            .playerName(streak.getUser().getName())
                            .rank(0)
                            .score(streak.getBestStreak())
                            .level(playerXpRepository.findByUserId(streak.getUser().getId()).map(PlayerXp::getLevel).orElse(1))
                            .city(city)
                            .leaderboardType(type)
                            .build())
                    .collect(collectRanked());
        }
        return playerXpRepository.findTop10ByOrderByXpDescUpdatedAtDesc().stream()
                .map(xp -> LeaderboardEntryResponse.builder()
                        .userId(xp.getUser().getId())
                        .playerName(xp.getUser().getName())
                        .rank(0)
                        .score(xp.getXp())
                        .level(xp.getLevel())
                        .city(city)
                        .leaderboardType(type)
                        .build())
                .collect(collectRanked());
    }

    private java.util.stream.Collector<LeaderboardEntryResponse, ?, List<LeaderboardEntryResponse>> collectRanked() {
        return Collectors.collectingAndThen(Collectors.toList(), list -> {
            for (int i = 0; i < list.size(); i++) {
                list.get(i).setRank(i + 1);
            }
            return list;
        });
    }

    private double completionRate() {
        long completed = playerChallengeRepository.countByStatus(ChallengeStatus.COMPLETED);
        long total = playerChallengeRepository.count();
        return total == 0 ? 0 : Math.round((completed * 10000.0) / total) / 100.0;
    }

    private List<TeamCompetitionResponse> teamCompetitions() {
        List<TeamCompetition> competitions;
        try {
            competitions = teamCompetitionRepository.findTop10ByOrderByPointsDescUpdatedAtDesc();
        } catch (DataAccessException ex) {
            log.warn("Team competition cache is unavailable; falling back to live team list.", ex);
            competitions = List.of();
        }
        if (competitions.isEmpty()) {
            return teamRepository.findAll().stream().limit(5).map(team -> TeamCompetitionResponse.builder()
                    .teamId(team.getId())
                    .teamName(team.getName())
                    .points(0)
                    .rank(0)
                    .seasonName("Current Season")
                    .build()).collect(collectTeamRanked());
        }
        return competitions.stream().map(item -> TeamCompetitionResponse.builder()
                .teamId(item.getTeamId())
                .teamName(item.getTeamName())
                .points(item.getPoints())
                .rank(item.getRank())
                .seasonName(item.getSeasonName())
                .build()).collect(collectTeamRanked());
    }

    private java.util.stream.Collector<TeamCompetitionResponse, ?, List<TeamCompetitionResponse>> collectTeamRanked() {
        return Collectors.collectingAndThen(Collectors.toList(), list -> {
            for (int i = 0; i < list.size(); i++) {
                list.get(i).setRank(i + 1);
            }
            return list;
        });
    }

    private PlayerXp getOrCreateXp(User user) {
        return playerXpRepository.findByUserId(user.getId())
                .orElseGet(() -> playerXpRepository.save(PlayerXp.builder().user(user).xp(0).level(1).build()));
    }

    private PlayerXpResponse mapXp(User user, PlayerXp xp) {
        int nextLevelXp = xp.getLevel() * XP_PER_LEVEL;
        int previousLevelXp = (xp.getLevel() - 1) * XP_PER_LEVEL;
        int progress = Math.min(100, Math.max(0, ((xp.getXp() - previousLevelXp) * 100) / XP_PER_LEVEL));
        return PlayerXpResponse.builder()
                .userId(user.getId())
                .playerName(user.getName())
                .xp(xp.getXp())
                .level(xp.getLevel())
                .nextLevelXp(nextLevelXp)
                .progressPercent(progress)
                .title(titleForLevel(xp.getLevel()))
                .lastActivity(xp.getLastActivity())
                .build();
    }

    private StreakResponse mapStreak(PlayerStreak streak) {
        return StreakResponse.builder()
                .streakType(streak.getStreakType())
                .currentStreak(streak.getCurrentStreak())
                .bestStreak(streak.getBestStreak())
                .lastActivityDate(streak.getLastActivityDate())
                .label(labelForStreak(streak.getStreakType()))
                .build();
    }

    private AchievementResponse mapAchievement(Achievement achievement, PlayerAchievement earned) {
        return AchievementResponse.builder()
                .id(achievement.getId())
                .name(achievement.getName())
                .category(achievement.getCategory())
                .triggerType(achievement.getTriggerType())
                .threshold(achievement.getThreshold())
                .xpReward(achievement.getXpReward())
                .earned(earned != null)
                .earnedAt(earned != null ? earned.getEarnedAt() : null)
                .build();
    }

    private BadgeResponse mapBadge(PlayerBadge badge) {
        return BadgeResponse.builder().id(badge.getId()).badge(badge.getBadge()).earnedAt(badge.getEarnedAt()).build();
    }

    private ChallengeResponse mapChallenge(PlayerChallenge playerChallenge) {
        Challenge challenge = playerChallenge.getChallenge();
        int target = Math.max(1, challenge.getTarget());
        return ChallengeResponse.builder()
                .id(playerChallenge.getId())
                .title(challenge.getTitle())
                .description(challenge.getDescription())
                .activityType(challenge.getActivityType())
                .target(challenge.getTarget())
                .progress(playerChallenge.getProgress())
                .xpReward(challenge.getXpReward())
                .rewardLabel(challenge.getRewardLabel())
                .status(playerChallenge.getStatus())
                .expiresAt(challenge.getExpiresAt())
                .progressPercent(Math.min(100, (playerChallenge.getProgress() * 100) / target))
                .build();
    }

    private RewardResponse mapReward(PlayerReward reward) {
        return RewardResponse.builder()
                .id(reward.getId())
                .rewardType(reward.getRewardType())
                .label(reward.getLabel())
                .status(reward.getStatus())
                .unlockedAt(reward.getUnlockedAt())
                .claimedAt(reward.getClaimedAt())
                .build();
    }

    private LeaderboardEntryResponse mapLeaderboard(LeaderboardCache cache) {
        return LeaderboardEntryResponse.builder()
                .userId(cache.getUser().getId())
                .playerName(cache.getUser().getName())
                .rank(cache.getRank())
                .score(cache.getScore())
                .level(playerXpRepository.findByUserId(cache.getUser().getId()).map(PlayerXp::getLevel).orElse(1))
                .city(cache.getCity())
                .leaderboardType(cache.getLeaderboardType())
                .build();
    }

    private SeasonalEventResponse mapEvent(SeasonalEvent event) {
        return SeasonalEventResponse.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getDescription())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .build();
    }

    private int defaultPoints(GamificationActivityType type) {
        return switch (type) {
            case BOOKING -> 20;
            case MATCH_PLAYED -> 50;
            case ATTENDANCE -> 30;
            case WIN -> 75;
            case REVIEW -> 25;
            case REFERRAL -> 200;
            case MVP -> 100;
            case CHALLENGE -> 40;
        };
    }

    private int levelForXp(int xp) {
        return Math.max(1, (xp / XP_PER_LEVEL) + 1);
    }

    private StreakType streakForActivity(GamificationActivityType type) {
        return switch (type) {
            case BOOKING -> StreakType.BOOKING;
            case ATTENDANCE -> StreakType.ATTENDANCE;
            case MATCH_PLAYED, WIN, MVP, CHALLENGE -> StreakType.WEEKLY_MATCH;
            case REVIEW, REFERRAL -> StreakType.FITNESS;
        };
    }

    private String labelForStreak(StreakType type) {
        return switch (type) {
            case WEEKLY_MATCH -> "Weekly Match Streak";
            case ATTENDANCE -> "Attendance Streak";
            case BOOKING -> "Booking Streak";
            case FITNESS -> "Activity Streak";
        };
    }

    private String labelForActivity(GamificationActivityType type) {
        return type.name().replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private String titleForLevel(int level) {
        if (level >= 25) return "Cricket Pro";
        if (level >= 10) return "Active Player";
        if (level >= 5) return "Rising Cricketer";
        return "Beginner";
    }

    private User findUser(String loginId) {
        if (loginId != null && loginId.contains("@")) {
            Optional<User> byEmail = userRepository.findByEmail(loginId);
            if (byEmail.isPresent()) {
                return byEmail.get();
            }
        }
        return userRepository.findByEmailOrPhone(loginId)
                .orElseThrow(() -> ApiException.unauthorized("Your account could not be loaded. Please sign in again."));
    }
}
