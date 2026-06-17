export type TeamMember = {
  id: number;
  userId: number;
  name: string;
  email: string;
  role: TeamRole;
  status: "INVITED" | "ACTIVE" | "DECLINED" | "LEFT" | "REMOVED";
  createdAt?: string;
};

export type Team = {
  id: number;
  name: string;
  sport: string;
  city: string;
  description?: string;
  logoUrl?: string;
  visibility?: TeamVisibility;
  maxMembers: number;
  memberCount: number;
  openSpots?: number;
  reputationScore?: number;
  captainId: number;
  captainName: string;
  members?: TeamMember[];
  createdAt?: string;
};

export type TeamInvite = {
  id: number;
  teamId: number;
  teamName: string;
  senderId: number;
  senderName: string;
  receiverId: number;
  receiverName: string;
  status: "PENDING" | "ACCEPTED" | "DECLINED" | "CANCELLED";
  createdAt?: string;
};

export type TeamJoinRequest = {
  id: number;
  teamId: number;
  teamName: string;
  requesterId: number;
  requesterName: string;
  reviewerId?: number;
  reviewerName?: string;
  message?: string;
  status: "PENDING" | "ACCEPTED" | "DECLINED" | string;
  createdAt?: string;
  updatedAt?: string;
};

export type PublicMatch = {
  id: number;
  creatorId: number;
  creatorName: string;
  turfId?: number;
  turfName?: string;
  title: string;
  sport: string;
  city: string;
  location: string;
  matchDate: string;
  startTime: string;
  endTime?: string;
  maxPlayers: number;
  joinedPlayers: number;
  visibility?: MatchVisibility;
  format?: MatchFormat;
  overs?: number;
  entryFee?: number;
  homeTeamId?: number;
  homeTeamName?: string;
  awayTeamId?: number;
  awayTeamName?: string;
  winnerTeamId?: number;
  winnerTeamName?: string;
  homeScore?: number;
  awayScore?: number;
  resultMargin?: string;
  challengeMessage?: string;
  resultSummary?: string;
  acceptedAt?: string;
  scheduledAt?: string;
  startedAt?: string;
  completedAt?: string;
  notes?: string;
  status: "PENDING" | "ACCEPTED" | "SCHEDULED" | "OPEN" | "FULL" | "LIVE" | "REJECTED" | "CANCELLED" | "COMPLETED" | string;
  participants?: TeamMember[];
  createdAt?: string;
};

export type ChatMessage = {
  id: number;
  roomType: "TEAM" | "MATCH" | "DIRECT";
  roomId: number;
  senderId: number;
  senderName: string;
  message: string;
  messageType?: "TEXT" | "IMAGE" | "VIDEO" | "MATCH_INVITE" | string;
  mediaUrl?: string;
  deliveredAt?: string;
  readAt?: string;
  createdAt?: string;
};

export type TeamVisibility = "PUBLIC" | "PRIVATE" | "INVITE_ONLY";
export type TeamRole = "CAPTAIN" | "VICE_CAPTAIN" | "MANAGER" | "OWNER" | "PLAYER" | "MEMBER";
export type MatchVisibility = "PUBLIC" | "PRIVATE" | "PAID";
export type MatchFormat = "FRIENDLY" | "PRACTICE" | "T20" | "ONE_DAY" | "BOX_CRICKET";

export type CreateTeamPayload = {
  name: string;
  sport: string;
  city: string;
  description?: string;
  logoUrl?: string;
  maxMembers: number;
  visibility?: TeamVisibility;
};

export type CreateMatchPayload = {
  turfId?: number;
  title: string;
  sport: string;
  city: string;
  location: string;
  matchDate: string;
  startTime: string;
  endTime?: string;
  maxPlayers: number;
  visibility?: MatchVisibility;
  format?: MatchFormat;
  overs?: number;
  entryFee?: number;
  homeTeamId?: number;
  awayTeamId?: number;
  challengeMessage?: string;
  notes?: string;
};

export type MatchSchedulePayload = {
  turfId?: number;
  matchDate: string;
  startTime: string;
  endTime?: string;
  location?: string;
  note?: string;
};

export type MatchResultPayload = {
  winnerTeamId?: number;
  homeScore: number;
  awayScore: number;
  resultMargin?: string;
  resultSummary?: string;
};

export type TeamMatchStats = {
  teamId: number;
  teamName: string;
  matchesPlayed: number;
  wins: number;
  losses: number;
  draws: number;
  winPercentage: number;
  pendingChallenges: number;
  upcomingMatches: number;
  rivalries: {
    opponentTeamId: number;
    opponentTeamName: string;
    matchesPlayed: number;
    wins: number;
    losses: number;
  }[];
};

export type PlayerAvailability = {
  id?: number;
  userId: number;
  userName: string;
  availableDays?: string;
  preferredWindow?: string;
  homeCity?: string;
  canTravel?: boolean;
  notes?: string;
  updatedAt?: string;
};

export type PlayerDiscover = {
  userId: number;
  name: string;
  email: string;
  city?: string;
  preferredTime?: string;
  battingStyle?: string;
  bowlingStyle?: string;
  position?: string;
  experience?: string;
  skillLevel?: string;
  trustScore?: number;
  networkScore?: number;
  matchScore?: number;
  connectionStatus?: string;
  recommendationReason?: string;
  rating: number;
  matches: number;
  wins: number;
  availability?: PlayerAvailability | null;
};

export type PlayerConnection = {
  id: number;
  fromUserId: number;
  fromUserName: string;
  toUserId: number;
  toUserName: string;
  type: "FRIEND" | "FOLLOW" | "TEAMMATE" | "FAVORITE_CAPTAIN" | string;
  status: "PENDING" | "ACCEPTED" | "DECLINED" | "BLOCKED" | string;
  message?: string;
  createdAt?: string;
};

export type RecruitmentApplication = {
  id: number;
  recruitmentId: number;
  userId: number;
  userName: string;
  status: "PENDING" | "ACCEPTED" | "DECLINED" | "WITHDRAWN" | string;
  message?: string;
  createdAt?: string;
};

export type TeamRecruitment = {
  id: number;
  teamId: number;
  teamName: string;
  captainId: number;
  captainName: string;
  roleNeeded: string;
  skillLevel?: string;
  city?: string;
  availability?: string;
  description?: string;
  matchDate?: string;
  status: "OPEN" | "PAUSED" | "FILLED" | "CLOSED" | string;
  applicationCount: number;
  applications?: RecruitmentApplication[];
  createdAt?: string;
};

export type CommunityAnalytics = {
  discoverablePlayers: number;
  acceptedConnections: number;
  pendingConnections: number;
  openRecruitments: number;
  pendingApplications: number;
  feedEvents: number;
  activePosts?: number;
  comments?: number;
  moderationItems?: number;
  engagementRate?: number;
  insight: string;
};

export type FeedEvent = {
  id: number;
  actorId: number;
  actorName: string;
  eventType:
    | "TEAM_CREATED"
    | "TEAM_INVITE_SENT"
    | "TEAM_JOINED"
    | "MATCH_CREATED"
    | "MATCH_JOINED"
    | "PROFILE_UPDATED"
    | "SOCIAL_POST_CREATED"
    | "SOCIAL_POST_EDITED"
    | "SOCIAL_POST_REACTED"
    | "SOCIAL_COMMENTED"
    | "SOCIAL_POST_REPORTED"
    | string;
  title: string;
  summary: string;
  entityId?: number;
  entityType?: string;
  deepLink?: string;
  visibility?: "PUBLIC" | "FOLLOWERS" | "TEAM" | "PRIVATE" | string;
  status?: "ACTIVE" | "HIDDEN" | "DELETED" | string;
  mediaUrls?: string[];
  reactionCount?: number;
  commentCount?: number;
  shareCount?: number;
  viewCount?: number;
  engagementScore?: number;
  viewerReaction?: string | null;
  ownPost?: boolean;
  editedAt?: string;
  createdAt?: string;
};

export type FeedPostPayload = {
  title?: string;
  content: string;
  visibility?: "PUBLIC" | "FOLLOWERS" | "TEAM" | "PRIVATE" | string;
  teamId?: number;
  tournamentId?: number;
  matchId?: number;
  mediaUrls?: string[];
};

export type FeedComment = {
  id: number;
  feedEventId: number;
  authorId: number;
  authorName: string;
  parentCommentId?: number;
  message: string;
  status: string;
  editedAt?: string;
  createdAt?: string;
};

export type PlayerProfile = {
  userId: number;
  name: string;
  email: string;
  phone: string;
  photoUrl?: string;
  bio?: string;
  city?: string;
  preferredTime?: string;
  battingStyle?: string;
  bowlingStyle?: string;
  position?: string;
  experience?: string;
  matches: number;
  wins: number;
  mvp: number;
  rating: number;
};
