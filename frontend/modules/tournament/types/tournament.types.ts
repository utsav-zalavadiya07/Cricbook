export type TournamentFormat = "LEAGUE" | "KNOCKOUT" | "GROUP_STAGE" | "HYBRID";
export type TournamentStatus = "DRAFT" | "UPCOMING" | "REGISTRATION_OPEN" | "REGISTRATION_CLOSED" | "ONGOING" | "COMPLETED" | "CANCELLED";
export type FixtureStatus = "SCHEDULED" | "LIVE" | "COMPLETED" | "CANCELLED";

export type Tournament = {
  id: number;
  organizerId: number;
  organizerName: string;
  turfId?: number;
  turfName?: string;
  name: string;
  sport: string;
  city: string;
  venue: string;
  description?: string;
  bannerUrl?: string;
  rules?: string;
  requiresApproval?: boolean;
  format: TournamentFormat;
  status: TournamentStatus;
  maxTeams: number;
  registeredTeams: number;
  entryFee: number;
  prizePool?: number;
  startDate: string;
  endDate?: string;
  createdAt?: string;
};

export type TournamentRegistration = {
  id: number;
  tournamentId: number;
  teamId: number;
  teamName: string;
  registeredById: number;
  registeredByName: string;
  status: "PENDING" | "REGISTERED" | "REJECTED" | "WAITLISTED" | "CANCELLED";
  paid?: boolean;
  note?: string;
  createdAt?: string;
};

export type TournamentFixture = {
  id: number;
  tournamentId: number;
  teamAId: number;
  teamAName: string;
  teamBId: number;
  teamBName: string;
  winnerTeamId?: number;
  winnerTeamName?: string;
  round: number;
  matchDate?: string;
  turfId?: number;
  turfName?: string;
  slotId?: number;
  startTime?: string;
  scheduleNote?: string;
  teamAScore?: number;
  teamBScore?: number;
  stageLabel?: string;
  knockoutStage?: boolean;
  status: FixtureStatus;
};

export type TournamentStanding = {
  teamId: number;
  teamName: string;
  played: number;
  wins: number;
  losses: number;
  draws: number;
  points: number;
  runsFor: number;
  runsAgainst: number;
  netRuns: number;
  netRunRate: number;
};

export type CreateTournamentPayload = {
  turfId?: number;
  name: string;
  sport: string;
  city: string;
  venue: string;
  description?: string;
  bannerUrl?: string;
  rules?: string;
  requiresApproval?: boolean;
  format: TournamentFormat;
  status?: TournamentStatus;
  maxTeams: number;
  entryFee: number;
  prizePool?: number;
  startDate: string;
  endDate?: string;
};

export type TournamentReward = {
  id: number;
  tournamentId: number;
  rewardType: "PRIZE" | "MEDAL" | "CERTIFICATE";
  title: string;
  description?: string;
  position: number;
  cashAmount?: number;
  winnerTeamId?: number;
  winnerTeamName?: string;
  createdAt?: string;
};

export type TournamentDashboard = {
  tournament: Tournament;
  registrations: TournamentRegistration[];
  fixtures: TournamentFixture[];
  standings: TournamentStanding[];
  rewards: TournamentReward[];
  feed: Array<{
    id: number;
    eventType: string;
    title: string;
    summary: string;
    createdAt?: string;
  }>;
  approvedTeams: number;
  pendingTeams: number;
  waitlistTeams: number;
  completedFixtures: number;
  upcomingFixtures: number;
  registrationRevenue: number;
};
