export interface PlayerPerformance {
  profile: {
    userId: number;
    name: string;
    email?: string;
    phone?: string;
    photoUrl?: string;
    bio?: string;
    city?: string;
    battingStyle?: string;
    bowlingStyle?: string;
    position?: string;
    experience?: string;
    matches: number;
    wins: number;
    mvp: number;
    rating: number;
    profileCompletionPercentage: number;
  };
  career: {
    matches: number;
    wins: number;
    losses: number;
    draws: number;
    mvpAwards: number;
    reputationScore: number;
    impactScore: number;
    consistencyScore: number;
  };
  batting: {
    innings: number;
    runs: number;
    balls: number;
    fours: number;
    sixes: number;
    highestScore: number;
    average: number;
    strikeRate: number;
    fifties: number;
    hundreds: number;
  };
  bowling: {
    innings: number;
    wickets: number;
    balls: number;
    overs: string;
    runsConceded: number;
    average: number;
    economyRate: number;
    strikeRate: number;
    bestFigures: string;
  };
  fielding: {
    catches: number;
    runouts: number;
    dismissals: number;
  };
  recentMatches: PlayerPerformanceMatch[];
  achievements: PlayerPerformanceAchievement[];
  timeline: PlayerPerformanceTimeline[];
  insight: string;
}

export interface PlayerPerformanceMatch {
  liveScoreId?: number;
  title: string;
  status?: string;
  result: string;
  runs: number;
  balls: number;
  fours: number;
  sixes: number;
  wickets: number;
  overs: string;
  runsConceded: number;
  catches: number;
  runouts: number;
  playedAt?: string;
}

export interface PlayerPerformanceAchievement {
  code: string;
  name: string;
  description: string;
  unlocked: boolean;
  progress: number;
  target: number;
}

export interface PlayerPerformanceTimeline {
  type: string;
  title: string;
  description: string;
  liveScoreId?: number;
  occurredAt?: string;
}

export interface PlayerPerformanceLeaderboardEntry {
  userId: number;
  playerName: string;
  city?: string;
  rank: number;
  leaderboardType: string;
  score: number;
  statLabel: string;
  matches: number;
  runs: number;
  wickets: number;
  impactScore: number;
}
