import { createSlice } from "./reduxShim";
import { LiveScore, LiveScoreDetail } from "../types/models";

export interface LiveState {
  liveScores: LiveScore[];
  selectedLiveScoreId: number | null;
  liveScoreDetail: LiveScoreDetail | null;
}

const initialState: LiveState = {
  liveScores: [],
  selectedLiveScoreId: null,
  liveScoreDetail: null,
};

export const liveSlice = createSlice({
  name: "live",
  initialState,
  reducers: {
    setLiveScores: (state, action) => {
      state.liveScores = action.payload;
    },
    setSelectedLiveScoreId: (state, action) => {
      state.selectedLiveScoreId = action.payload;
    },
    setLiveScoreDetail: (state, action) => {
      state.liveScoreDetail = action.payload;
    },
  },
});

export const { setLiveScores, setSelectedLiveScoreId, setLiveScoreDetail } = liveSlice.actions;
export default liveSlice.reducer;
