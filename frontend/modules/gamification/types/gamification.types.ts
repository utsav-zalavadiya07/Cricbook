export type GamificationActivityType = "BOOKING" | "MATCH_PLAYED" | "ATTENDANCE" | "WIN" | "REVIEW" | "REFERRAL" | "MVP" | "CHALLENGE";
export type StreakType = "WEEKLY_MATCH" | "ATTENDANCE" | "BOOKING" | "FITNESS";
export type ChallengeStatus = "ACTIVE" | "COMPLETED" | "EXPIRED";
export type RewardStatus = "LOCKED" | "AVAILABLE" | "CLAIMED";
export type LeaderboardType = "HIGHEST_XP" | "LONGEST_STREAK" | "MOST_MATCHES" | "MOST_ACTIVE";

export interface PlayerXp {
  userId: number;
  playerName: string;
  xp: number;
  level: number;
  nextLevelXp: number;
  progressPercent: number;
  title: string;
  lastActivity?: string;
}

export interface Streak {
  streakType: StreakType;
  currentStreak: number;
  bestStreak: number;
  lastActivityDate?: string;
  label: string;
}

export interface Achievement {
  id: number;
  name: string;
  category: string;
  triggerType: GamificationActivityType;
  threshold: number;
  xpReward: number;
  earned: boolean;
  earnedAt?: string;
}

export interface Badge {
  id: number;
  badge: string;
  earnedAt?: string;
}

export interface Challenge {
  id: number;
  title: string;
  description?: string;
  activityType: GamificationActivityType;
  target: number;
  progress: number;
  xpReward: number;
  rewardLabel?: string;
  status: ChallengeStatus;
  expiresAt?: string;
  progressPercent: number;
}

export interface GamificationReward {
  id: number;
  rewardType: string;
  label: string;
  status: RewardStatus;
  unlockedAt?: string;
  claimedAt?: string;
}

export interface LeaderboardEntry {
  userId: number;
  playerName: string;
  rank: number;
  score: number;
  level: number;
  city: string;
  leaderboardType: LeaderboardType;
}

export interface SeasonalEvent {
  id: number;
  name: string;
  description?: string;
  startDate: string;
  endDate: string;
}

export interface TeamCompetition {
  teamId: number;
  teamName: string;
  points: number;
  rank: number;
  seasonName: string;
}

export interface GamificationProfile {
  xp: PlayerXp;
  streaks: Streak[];
  achievements: Achievement[];
  badges: Badge[];
  challenges: Challenge[];
  rewards: GamificationReward[];
  leaderboard: LeaderboardEntry[];
  seasonalEvents: SeasonalEvent[];
  insight: string;
}

export interface GamificationAnalytics {
  activePlayers: number;
  completedChallenges: number;
  availableRewards: number;
  challengeCompletionRate: number;
  topPlayers: LeaderboardEntry[];
  teamCompetitions: TeamCompetition[];
  insight: string;
}
