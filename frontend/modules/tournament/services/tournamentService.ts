import api from "@/services/api";
import { CreateTournamentPayload, TournamentFormat, TournamentStatus } from "../types/tournament.types";

export const getOpenTournaments = (params?: {
  query?: string;
  city?: string;
  format?: TournamentFormat | "";
  status?: TournamentStatus | "";
  maxEntryFee?: number;
  fromDate?: string;
  toDate?: string;
}) => api.get("/tournaments", { params });
export const getMyTournaments = () => api.get("/tournaments/mine");
export const getTournament = (tournamentId: number) => api.get(`/tournaments/${tournamentId}`);

export const createTournament = (payload: CreateTournamentPayload) => api.post("/tournaments", payload);

export const updateTournamentStatus = (tournamentId: number, status: TournamentStatus, note?: string) =>
  api.post(`/tournaments/${tournamentId}/status`, { status, note });

export const registerTeamForTournament = (tournamentId: number, teamId: number) =>
  api.post(`/tournaments/${tournamentId}/registrations`, { teamId });

export const getTournamentRegistrations = (tournamentId: number) =>
  api.get(`/tournaments/${tournamentId}/registrations`);

export const approveTournamentRegistration = (tournamentId: number, registrationId: number, note?: string) =>
  api.post(`/tournaments/${tournamentId}/registrations/${registrationId}/approve`, { note });

export const rejectTournamentRegistration = (tournamentId: number, registrationId: number, note?: string) =>
  api.post(`/tournaments/${tournamentId}/registrations/${registrationId}/reject`, { note });

export const cancelTournamentRegistration = (tournamentId: number, registrationId: number) =>
  api.post(`/tournaments/${tournamentId}/registrations/${registrationId}/cancel`);

export const generateTournamentFixtures = (tournamentId: number) =>
  api.post(`/tournaments/${tournamentId}/fixtures/generate`);

export const regenerateTournamentFixtures = (tournamentId: number) =>
  api.post(`/tournaments/${tournamentId}/fixtures/regenerate`);

export const getTournamentFixtures = (tournamentId: number) =>
  api.get(`/tournaments/${tournamentId}/fixtures`);

export const scheduleTournamentFixture = (
  fixtureId: number,
  payload: { turfId?: number; slotId?: number; matchDate?: string; startTime?: string; scheduleNote?: string }
) => api.post(`/tournaments/fixtures/${fixtureId}/schedule`, payload);

export const recordFixtureResult = (fixtureId: number, teamAScore: number, teamBScore: number) =>
  api.post(`/tournaments/fixtures/${fixtureId}/result`, { teamAScore, teamBScore });

export const getTournamentStandings = (tournamentId: number) =>
  api.get(`/tournaments/${tournamentId}/standings`);

export const addTournamentReward = (
  tournamentId: number,
  payload: { rewardType: "PRIZE" | "MEDAL" | "CERTIFICATE"; title: string; description?: string; position: number; cashAmount?: number; winnerTeamId?: number }
) => api.post(`/tournaments/${tournamentId}/rewards`, payload);

export const getTournamentRewards = (tournamentId: number) =>
  api.get(`/tournaments/${tournamentId}/rewards`);

export const getTournamentDashboard = (tournamentId: number) =>
  api.get(`/tournaments/${tournamentId}/dashboard`);
