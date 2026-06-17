import { createSlice } from "./reduxShim";
import {
  FeedEvent,
  NotificationItem,
  PlayerDiscover,
  PublicMatch,
  Team,
  Tournament,
  TournamentFixture,
  TournamentStanding,
} from "../types/models";

export interface TournamentState {
  tournaments: Tournament[];
  selectedTournamentId: number | null;
  tournamentFixtures: TournamentFixture[];
  tournamentStandings: TournamentStanding[];
  teams: Team[];
  matches: PublicMatch[];
  matchHistory: PublicMatch[];
  notifications: NotificationItem[];
  socialFeed: FeedEvent[];
  discoverablePlayers: PlayerDiscover[];
}

const initialState: TournamentState = {
  tournaments: [],
  selectedTournamentId: null,
  tournamentFixtures: [],
  tournamentStandings: [],
  teams: [],
  matches: [],
  matchHistory: [],
  notifications: [],
  socialFeed: [],
  discoverablePlayers: [],
};

export const tournamentSlice = createSlice({
  name: "tournament",
  initialState,
  reducers: {
    setTournaments: (state, action) => {
      state.tournaments = action.payload;
    },
    setSelectedTournamentId: (state, action) => {
      state.selectedTournamentId = action.payload;
    },
    setTournamentFixtures: (state, action) => {
      state.tournamentFixtures = action.payload;
    },
    setTournamentStandings: (state, action) => {
      state.tournamentStandings = action.payload;
    },
    setTeams: (state, action) => {
      state.teams = action.payload;
    },
    setMatches: (state, action) => {
      state.matches = action.payload;
    },
    setMatchHistory: (state, action) => {
      state.matchHistory = action.payload;
    },
    setNotifications: (state, action) => {
      state.notifications = action.payload;
    },
    setSocialFeed: (state, action) => {
      state.socialFeed = action.payload;
    },
    setDiscoverablePlayers: (state, action) => {
      state.discoverablePlayers = action.payload;
    },
  },
});

export const {
  setTournaments,
  setSelectedTournamentId,
  setTournamentFixtures,
  setTournamentStandings,
  setTeams,
  setMatches,
  setMatchHistory,
  setNotifications,
  setSocialFeed,
  setDiscoverablePlayers,
} = tournamentSlice.actions;

export default tournamentSlice.reducer;
