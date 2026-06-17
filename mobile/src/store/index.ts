import { configureStore } from "./reduxShim";
import authReducer from "./authSlice";
import bookingReducer from "./bookingSlice";
import liveReducer from "./liveSlice";
import tournamentReducer from "./tournamentSlice";

export const store = configureStore({
  reducer: {
    auth: authReducer,
    booking: bookingReducer,
    live: liveReducer,
    tournament: tournamentReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;

export { Provider, useSelector, useDispatch } from "./reduxShim";

export * from "./authSlice";
export * from "./bookingSlice";
export * from "./liveSlice";
export * from "./tournamentSlice";
