import api from "@/services/api";
import { TeamMember } from "@/modules/social/types/social.types";

export type TeamAvailabilityStatus = "AVAILABLE" | "MAYBE" | "UNAVAILABLE";
export type TeamFinanceType = "CONTRIBUTION" | "EXPENSE" | "REFUND";
export type TeamAttendanceStatus = "PRESENT" | "LATE" | "ABSENT" | "EXCUSED";

export type TeamAvailability = {
  id: number;
  teamId: number;
  userId: number;
  userName: string;
  date: string;
  status: TeamAvailabilityStatus;
  note?: string;
};

export type TeamLineup = {
  id: number;
  teamId: number;
  matchId?: number;
  userId: number;
  userName: string;
  position: number;
  lineupRole: string;
};

export type TeamPoll = {
  id: number;
  teamId: number;
  question: string;
  options: string[];
  voteCounts: Record<string, number>;
  expiresAt?: string;
};

export type TeamAnnouncement = {
  id: number;
  teamId: number;
  title: string;
  content: string;
  pinned: boolean;
  createdByName: string;
  expiresAt?: string;
  createdAt?: string;
};

export type TeamFinance = {
  id: number;
  teamId: number;
  userId?: number;
  userName?: string;
  type: TeamFinanceType;
  amount: number;
  description: string;
  createdAt?: string;
};

export type TeamAttendance = {
  id: number;
  teamId: number;
  matchId?: number;
  userId: number;
  userName: string;
  status: TeamAttendanceStatus;
  note?: string;
};

export type TeamWorkspace = {
  teamId: number;
  teamName: string;
  city: string;
  sport: string;
  visibility: string;
  teamType: string;
  bannerUrl?: string;
  goals?: string;
  memberCount: number;
  availableToday: number;
  maybeToday: number;
  unavailableToday: number;
  teamBalance: number;
  attendanceRate: number;
  members: TeamMember[];
  availability: TeamAvailability[];
  lineup: TeamLineup[];
  polls: TeamPoll[];
  announcements: TeamAnnouncement[];
  finances: TeamFinance[];
  attendance: TeamAttendance[];
};

export type TeamAnalytics = {
  totalTeams: number;
  activeMembers: number;
  availabilityResponses: number;
  polls: number;
  teamWalletBalance: number;
  averageAttendanceRate: number;
  mostActiveTeams: TeamWorkspace[];
};

export const getTeamWorkspace = (teamId: number) => api.get<TeamWorkspace>(`/team-workspaces/${teamId}`);
export const updateTeamWorkspace = (teamId: number, payload: Partial<TeamWorkspace>) => api.put<TeamWorkspace>(`/team-workspaces/${teamId}`, payload);
export const setTeamAvailability = (teamId: number, payload: { date: string; status: TeamAvailabilityStatus; note?: string }) =>
  api.post<TeamAvailability>(`/team-workspaces/${teamId}/availability`, payload);
export const saveTeamLineup = (teamId: number, payload: { matchId?: number; userId: number; position: number; lineupRole?: string }) =>
  api.post<TeamLineup>(`/team-workspaces/${teamId}/lineup`, payload);
export const createTeamPoll = (teamId: number, payload: { question: string; options: string[]; expiresAt?: string }) =>
  api.post<TeamPoll>(`/team-workspaces/${teamId}/polls`, payload);
export const voteTeamPoll = (teamId: number, pollId: number, selectedOption: string) =>
  api.post<TeamPoll>(`/team-workspaces/${teamId}/polls/${pollId}/vote`, { selectedOption });
export const createTeamAnnouncement = (teamId: number, payload: { title: string; content: string; pinned?: boolean; expiresAt?: string }) =>
  api.post<TeamAnnouncement>(`/team-workspaces/${teamId}/announcements`, payload);
export const addTeamFinance = (teamId: number, payload: { userId?: number; type: TeamFinanceType; amount: number; description: string }) =>
  api.post<TeamFinance>(`/team-workspaces/${teamId}/finances`, payload);
export const recordTeamAttendance = (teamId: number, payload: { matchId?: number; userId: number; status: TeamAttendanceStatus; note?: string }) =>
  api.post<TeamAttendance>(`/team-workspaces/${teamId}/attendance`, payload);
export const getTeamAnalytics = () => api.get<TeamAnalytics>("/team-workspaces/analytics");
export const getAdminTeamAnalytics = () => api.get<TeamAnalytics>("/team-workspaces/admin/analytics");
