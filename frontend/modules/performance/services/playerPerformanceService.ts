import api from "@/services/api";
import { PlayerPerformance, PlayerPerformanceLeaderboardEntry } from "../types/playerPerformance.types";

export const getPlayerPerformance = () =>
  api.get<PlayerPerformance>("/player-performance/me");

export const getPlayerPerformanceLeaderboard = (params?: { type?: "RUNS" | "WICKETS" | "MATCHES" | "AVERAGE" | "IMPACT"; city?: string; limit?: number }) =>
  api.get<PlayerPerformanceLeaderboardEntry[]>("/player-performance/leaderboard", { params });
