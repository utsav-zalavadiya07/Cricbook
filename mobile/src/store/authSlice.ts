import { createSlice } from "./reduxShim";
import { Session, UserProfile } from "../types/models";

export interface AuthState {
  session: Session | null;
  biometricEnabled: boolean;
  portal: "player" | "owner";
  profile: UserProfile | null;
}

const initialState: AuthState = {
  session: null,
  biometricEnabled: false,
  portal: "player",
  profile: null,
};

export const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    setSession: (state, action) => {
      state.session = action.payload;
    },
    setBiometricEnabled: (state, action) => {
      state.biometricEnabled = action.payload;
    },
    setPortal: (state, action) => {
      state.portal = action.payload;
    },
    setProfile: (state, action) => {
      state.profile = action.payload;
    },
  },
});

export const { setSession, setBiometricEnabled, setPortal, setProfile } = authSlice.actions;
export default authSlice.reducer;
