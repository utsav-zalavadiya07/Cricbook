import api from "@/services/api";

export type TrustRiskLevel = "LOW" | "MEDIUM" | "HIGH";
export type AttendanceStatus = "ARRIVED" | "LATE" | "NO_SHOW" | "CANCELLED";
export type TrustReportStatus = "OPEN" | "REVIEWED" | "DISMISSED" | "ACTIONED";
export type TrustReportReason =
  | "TOXIC_BEHAVIOR"
  | "AGGRESSIVE_BEHAVIOR"
  | "CHEATING"
  | "NO_PAYMENT"
  | "PROPERTY_DAMAGE"
  | "MISCONDUCT"
  | "FAKE_BOOKING"
  | "OTHER";

export type TrustScore = {
  userId: number;
  playerName: string;
  score: number;
  tier: string;
  attendanceScore: number;
  punctualityScore: number;
  cancellationScore: number;
  reportScore: number;
  paymentScore: number;
  matchCompletionScore: number;
  riskLevel: TrustRiskLevel;
  riskReason: string;
  badges: string[];
  recommendations: string[];
  updatedAt?: string;
};

export type TrustAttendance = {
  id: number;
  bookingId: number;
  userId: number;
  playerName: string;
  status: AttendanceStatus;
  delayMinutes: number;
  impact: string;
  checkinTime?: string;
  createdAt?: string;
};

export type TrustReport = {
  id: number;
  reporterId: number;
  reporterName: string;
  reportedUserId: number;
  reportedUserName: string;
  bookingId?: number;
  reason: TrustReportReason;
  description?: string;
  status: TrustReportStatus;
  createdAt?: string;
};

export type TrustAnalytics = {
  totalProfiles: number;
  elitePlayers: number;
  trustedPlayers: number;
  riskyPlayers: number;
  openReports: number;
  riskiestPlayers: TrustScore[];
  mostReliablePlayers: TrustScore[];
};

export const getMyTrustProfile = () => api.get<TrustScore>("/trust/me");
export const getTrustProfile = (userId: number) => api.get<TrustScore>(`/trust/profile/${userId}`);
export const getTrustAttendance = (userId: number) => api.get<TrustAttendance[]>(`/trust/profile/${userId}/attendance`);
export const submitTrustReport = (payload: { reportedUserId: number; bookingId?: number; reason: TrustReportReason; description?: string }) =>
  api.post<TrustReport>("/trust/reports", payload);
export const updateBookingAttendance = (bookingId: number, status: AttendanceStatus) =>
  api.post<TrustAttendance>(`/trust/owner/bookings/${bookingId}/attendance`, { status });
export const getOwnerRiskBoard = () => api.get<TrustScore[]>("/trust/owner/risk-board");
export const getTrustAnalytics = () => api.get<TrustAnalytics>("/trust/admin/analytics");
export const getOpenTrustReports = () => api.get<TrustReport[]>("/trust/admin/reports");
export const moderateTrustReport = (reportId: number, payload: { status: TrustReportStatus; scoreAdjustment?: number; reason?: string }) =>
  api.post<TrustReport>(`/trust/admin/reports/${reportId}/moderate`, payload);
export const recalculateTrustProfile = (userId: number) => api.post<TrustScore>(`/trust/admin/profiles/${userId}/recalculate`);
