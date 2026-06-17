export type LiveMatchStatus = "NOT_STARTED" | "LIVE" | "INNINGS_BREAK" | "COMPLETED" | "PAUSED" | "CANCELLED";

export type LiveScore = {
  id: number;
  fixtureId?: number;
  publicMatchId?: number;
  scorerId: number;
  scorerName: string;
  title: string;
  teamAName: string;
  teamBName: string;
  battingTeam: string;
  bowlingTeam: string;
  innings: number;
  runs: number;
  wickets: number;
  ballsBowled: number;
  overs: string;
  extras: number;
  targetRuns?: number;
  maxOvers: number;
  runRate: number;
  projectedScore: number;
  status: LiveMatchStatus;
  locked?: boolean;
  tie?: boolean;
  superOver?: boolean;
  winnerTeamName?: string;
  resultSummary?: string;
  shareUrl?: string;
  updatedAt?: string;
};

export type ScoreEvent = {
  id: number;
  eventSequence: number;
  runs: number;
  extraRuns: number;
  extraType?: string;
  wicket: boolean;
  wicketType?: string;
  batter?: string;
  bowler?: string;
  note?: string;
  createdAt?: string;
};

export type LiveScoreDetail = {
  score: LiveScore;
  events: ScoreEvent[];
  innings?: LiveScoreInnings[];
  battingScorecard?: LiveScoreBattingRow[];
  bowlingScorecard?: LiveScoreBowlingRow[];
  recentOvers?: LiveScoreOverSummary[];
  fallOfWickets?: LiveScoreFallOfWicket[];
  resultSummary?: string;
  winnerTeamName?: string;
  verificationStatus?: string;
  streamUrl?: string;
  shareUrl?: string;
  auditTrail?: string[];
  strikerName?: string;
  strikerRuns?: number;
  strikerBalls?: number;
  strikerFours?: number;
  strikerSixes?: number;
  nonStrikerName?: string;
  nonStrikerRuns?: number;
  nonStrikerBalls?: number;
  nonStrikerFours?: number;
  nonStrikerSixes?: number;
  bowlerName?: string;
  bowlerOvers?: number;
  bowlerRuns?: number;
  bowlerWickets?: number;
  partnershipRuns?: number;
  partnershipBalls?: number;
  requiredRunRate?: number;
};

export type LiveScoreInnings = {
  inningsNumber: number;
  battingTeamName: string;
  runs: number;
  wickets: number;
  ballsBowled: number;
  overs: string;
  extras: number;
};

export type LiveScoreBattingRow = {
  playerName: string;
  runs: number;
  balls: number;
  fours: number;
  sixes: number;
  strikeRate: number;
  out: boolean;
  dismissal?: string;
};

export type LiveScoreBowlingRow = {
  playerName: string;
  overs: string;
  balls: number;
  runsConceded: number;
  wickets: number;
  economy: number;
};

export type LiveScoreOverSummary = {
  overNumber: number;
  label: string;
  runs: number;
  wickets: number;
  events: ScoreEvent[];
};

export type LiveScoreFallOfWicket = {
  wicketNumber: number;
  score: number;
  over: string;
  batter: string;
  wicketType?: string;
};

export type CreateLiveScorePayload = {
  fixtureId?: number;
  publicMatchId?: number;
  title?: string;
  teamAName: string;
  teamBName: string;
  battingTeam?: string;
  maxOvers: number;
};

export type ScoreEventPayload = {
  runs: number;
  extraRuns: number;
  extraType?: string;
  wicket: boolean;
  wicketType?: string;
  batter?: string;
  bowler?: string;
  note?: string;
};
