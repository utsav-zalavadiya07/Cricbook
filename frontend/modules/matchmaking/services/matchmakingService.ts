import api from "@/services/api";
import { CreateMatchPayload, PublicMatch } from "@/modules/social/types/social.types";

export type MatchmakingCandidate = {
  userId: number;
  name: string;
  city?: string;
  position?: string;
  preferredTime?: string;
  skillLevel?: string;
  trustScore: number;
  rating?: number;
  matches?: number;
  recommendationScore: number;
  reason: string;
};

export type MatchmakingMatch = {
  id: number;
  title: string;
  city: string;
  location: string;
  matchDate: string;
  startTime: string;
  endTime?: string;
  maxPlayers: number;
  joinedPlayers: number;
  slotsLeft: number;
  skillLevel?: string;
  autoApprove: boolean;
  trustedOnly: boolean;
  status: "OPEN" | "FULL" | "CANCELLED" | "COMPLETED";
  matchScore: number;
  recommendationReason: string;
  match: PublicMatch;
  candidates: MatchmakingCandidate[];
};

export type MatchmakingAnalytics = {
  openMatches: number;
  fullMatches: number;
  totalParticipants: number;
  averageFillRate: number;
  mostFillableMatches: MatchmakingMatch[];
};

export type MatchmakingCreatePayload = CreateMatchPayload & {
  requiredPlayers?: number;
  skillLevel?: string;
  autoApprove?: boolean;
  trustedOnly?: boolean;
};

export const createMatchmakingRequest = (payload: MatchmakingCreatePayload) => api.post<MatchmakingMatch>("/matchmaking/create", payload);
export const getOpenMatchmaking = (params?: { city?: string; skillLevel?: string; trustedOnly?: boolean }) => api.get<MatchmakingMatch[]>("/matchmaking/open", { params });
export const getNearbyMatches = (city?: string) => api.get<MatchmakingMatch[]>("/matchmaking/nearby", { params: city ? { city } : undefined });
export const getMatchRecommendations = () => api.get<MatchmakingMatch[]>("/matchmaking/recommendations");
export const getMatchCandidates = (matchId: number, params?: { city?: string; skillLevel?: string; trustedOnly?: boolean }) =>
  api.get<MatchmakingCandidate[]>(`/matchmaking/matches/${matchId}/candidates`, { params });
export const joinMatchmakingMatch = (matchId: number) => api.post<MatchmakingMatch>(`/matchmaking/matches/${matchId}/join`);
export const getMatchmakingAnalytics = () => api.get<MatchmakingAnalytics>("/matchmaking/analytics");
