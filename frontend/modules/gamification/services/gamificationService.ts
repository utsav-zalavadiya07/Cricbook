import api from "@/services/api";
import { GamificationActivityType, GamificationAnalytics, GamificationProfile, GamificationReward, LeaderboardEntry, LeaderboardType } from "../types/gamification.types";

export const getGamificationProfile = () =>
  api.get<GamificationProfile>("/gamification/profile");

export const awardGamificationXp = (payload: { activityType: GamificationActivityType; points?: number; reason?: string }) =>
  api.post<GamificationProfile>("/gamification/xp", payload);

export const claimGamificationReward = (rewardId: number) =>
  api.post<GamificationReward>(`/gamification/rewards/${rewardId}/claim`);

export const getGamificationLeaderboard = (params?: { city?: string; type?: LeaderboardType }) =>
  api.get<LeaderboardEntry[]>("/gamification/leaderboard", { params });

export const getOwnerGamificationAnalytics = () =>
  api.get<GamificationAnalytics>("/gamification/owner/analytics");

export const getAdminGamificationAnalytics = () =>
  api.get<GamificationAnalytics>("/gamification/admin/analytics");
