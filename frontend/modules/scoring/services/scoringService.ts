import api from "@/services/api";
import { CreateLiveScorePayload, LiveScore, LiveScoreDetail, ScoreEventPayload } from "../types/scoring.types";

export const SCORING_API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";

export const getLiveScores = () => api.get<LiveScore[]>("/live-scores");

export const getScoreArchive = () => api.get<LiveScore[]>("/live-scores/archive");

export const createLiveScore = (payload: CreateLiveScorePayload) => api.post<LiveScore>("/live-scores", payload);

export const getLiveScore = (scoreId: number) => api.get<LiveScoreDetail>(`/live-scores/${scoreId}`);

export const startLiveScore = (scoreId: number) => api.post<LiveScore>(`/live-scores/${scoreId}/start`);

export const pauseLiveScore = (scoreId: number) => api.post<LiveScore>(`/live-scores/${scoreId}/pause`);

export const resumeLiveScore = (scoreId: number) => api.post<LiveScore>(`/live-scores/${scoreId}/resume`);

export const undoScoreEvent = (scoreId: number) => api.post<LiveScoreDetail>(`/live-scores/${scoreId}/undo`);

export const recordScoreEvent = (scoreId: number, payload: ScoreEventPayload) =>
  api.post<LiveScoreDetail>(`/live-scores/${scoreId}/events`, payload);

export const completeLiveScore = (scoreId: number) => api.post<LiveScore>(`/live-scores/${scoreId}/complete`);
