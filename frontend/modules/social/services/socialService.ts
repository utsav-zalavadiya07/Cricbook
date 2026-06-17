import api from "@/services/api";
import {
  CreateMatchPayload,
  CreateTeamPayload,
  FeedPostPayload,
  MatchResultPayload,
  MatchSchedulePayload,
  PlayerAvailability,
  PublicMatch,
  Team,
  TeamJoinRequest,
  TeamMatchStats,
  TeamRole,
  TeamVisibility,
} from "../types/social.types";

export const getMyTeams = () => api.get<Team[]>("/social/teams");

export const createTeam = (payload: CreateTeamPayload) => api.post<Team>("/social/teams", payload);

export const getTeam = (teamId: number) => api.get<Team>(`/social/teams/${teamId}`);

export const discoverTeams = (params?: {
  query?: string;
  city?: string;
  visibility?: TeamVisibility;
  openOnly?: boolean;
}) => api.get<Team[]>("/social/teams/discover", { params });

export const inviteTeamMember = (teamId: number, loginId: string) =>
  api.post(`/social/teams/${teamId}/members`, { loginId });

export const getTeamInvites = () => api.get("/social/teams/invites");

export const getTeamJoinRequests = () => api.get<TeamJoinRequest[]>("/social/teams/join-requests");

export const requestToJoinTeam = (teamId: number, message?: string) =>
  api.post<TeamJoinRequest>(`/social/teams/${teamId}/join-requests`, { message });

export const respondTeamInvite = (inviteId: number, accept: boolean) =>
  api.post(`/social/teams/invites/${inviteId}/respond`, { accept });

export const respondTeamJoinRequest = (requestId: number, accept: boolean) =>
  api.post<TeamJoinRequest>(`/social/teams/join-requests/${requestId}/respond`, { accept });

export const leaveTeam = (teamId: number) => api.post(`/social/teams/${teamId}/leave`);

export const transferCaptain = (teamId: number, userId: number) =>
  api.post(`/social/teams/${teamId}/transfer-captain`, { userId });

export const updateTeamRole = (teamId: number, userId: number, role: TeamRole) =>
  api.post(`/social/teams/${teamId}/roles`, { userId, role });

export const removeTeamMember = (teamId: number, userId: number) =>
  api.delete(`/social/teams/${teamId}/members/${userId}`);

export const deleteTeam = (teamId: number) => api.delete(`/social/teams/${teamId}`);

export const getOpenMatches = () => api.get<PublicMatch[]>("/social/matches");

export const getMyMatchHistory = () => api.get<PublicMatch[]>("/social/matches/history");

export const createPublicMatch = (payload: CreateMatchPayload) => api.post<PublicMatch>("/social/matches", payload);

export const joinPublicMatch = (matchId: number) => api.post<PublicMatch>(`/social/matches/${matchId}/join`);

export const acceptMatchChallenge = (matchId: number) =>
  api.post<PublicMatch>(`/social/matches/${matchId}/accept`);

export const rejectMatchChallenge = (matchId: number) =>
  api.post<PublicMatch>(`/social/matches/${matchId}/reject`);

export const schedulePublicMatch = (matchId: number, payload: MatchSchedulePayload) =>
  api.post<PublicMatch>(`/social/matches/${matchId}/schedule`, payload);

export const startPublicMatch = (matchId: number) =>
  api.post<PublicMatch>(`/social/matches/${matchId}/start`);

export const cancelPublicMatch = (matchId: number) =>
  api.post<PublicMatch>(`/social/matches/${matchId}/cancel`);

export const submitMatchResult = (matchId: number, payload: MatchResultPayload) =>
  api.post<PublicMatch>(`/social/matches/${matchId}/result`, payload);

export const getTeamMatchStats = (teamId: number) =>
  api.get<TeamMatchStats>(`/social/teams/${teamId}/match-stats`);

export const discoverPlayers = (params?: { city?: string; position?: string; availability?: string }) =>
  api.get("/social/players/discover", { params });

export const getNearbyPlayers = (params?: { city?: string; availability?: string }) =>
  api.get("/social/players/nearby", { params });

export const getMyNetwork = () => api.get("/social/network");

export const connectPlayer = (payload: { userId: number; type?: string; message?: string }) =>
  api.post("/social/network/connect", payload);

export const respondConnection = (connectionId: number, accept: boolean) =>
  api.post(`/social/network/${connectionId}/respond`, { accept });

export const createRecruitment = (payload: {
  teamId: number;
  roleNeeded: string;
  skillLevel?: string;
  city?: string;
  availability?: string;
  description?: string;
  matchDate?: string;
}) => api.post("/social/recruitment", payload);

export const getOpenRecruitments = (params?: { city?: string; role?: string }) =>
  api.get("/social/recruitment/open", { params });

export const applyRecruitment = (recruitmentId: number, message?: string) =>
  api.post(`/social/recruitment/${recruitmentId}/apply`, { message });

export const respondRecruitmentApplication = (applicationId: number, accept: boolean) =>
  api.post(`/social/recruitment/applications/${applicationId}/respond`, { accept });

export const getCommunityAnalytics = () => api.get("/social/analytics");

export const getAdminCommunityModeration = () => api.get("/social/admin/moderation");

export const getSocialFeed = (params?: { scope?: string; query?: string; sort?: string; entityType?: string; entityId?: number }) =>
  api.get("/social/feed", { params });

export const createFeedPost = (payload: FeedPostPayload) =>
  api.post("/social/feed", payload);

export const editFeedPost = (postId: number, payload: Partial<FeedPostPayload>) =>
  api.patch(`/social/feed/${postId}`, payload);

export const deleteFeedPost = (postId: number) =>
  api.delete(`/social/feed/${postId}`);

export const reactToFeedPost = (postId: number, reactionType: string = "LIKE") =>
  api.post(`/social/feed/${postId}/reactions`, { reactionType });

export const removeFeedReaction = (postId: number) =>
  api.delete(`/social/feed/${postId}/reactions`);

export const getFeedComments = (postId: number) =>
  api.get(`/social/feed/${postId}/comments`);

export const addFeedComment = (postId: number, message: string, parentCommentId?: number) =>
  api.post(`/social/feed/${postId}/comments`, { message, parentCommentId });

export const reportContent = (contentType: string, contentId: number, reason: string, details?: string) =>
  api.post("/social/reports", { contentType, contentId, reason, details });

export const getTeamMessages = (teamId: number) => api.get(`/social/chat/team/${teamId}`);

export const sendTeamMessage = (teamId: number, message: string, messageType = "TEXT", mediaUrl?: string) =>
  api.post(`/social/chat/team/${teamId}`, { message, messageType, mediaUrl });

export const getMatchMessages = (matchId: number) => api.get(`/social/chat/match/${matchId}`);

export const sendMatchMessage = (matchId: number, message: string, messageType = "TEXT", mediaUrl?: string) =>
  api.post(`/social/chat/match/${matchId}`, { message, messageType, mediaUrl });

export const getDirectMessages = (recipientId: number) =>
  api.get(`/social/chat/direct/${recipientId}`);

export const sendDirectMessage = (recipientId: number, message: string, messageType = "TEXT", mediaUrl?: string) =>
  api.post(`/social/chat/direct/${recipientId}`, { message, messageType, mediaUrl });

export const getMyAvailability = () => api.get("/users/player-profile/availability");

export const updateMyAvailability = (payload: Partial<PlayerAvailability>) =>
  api.put("/users/player-profile/availability", payload);
