import { createSlice } from "./reduxShim";
import { Booking, PersonalizationPayload, SearchFilters, SlotAvailability, Turf } from "../types/models";

export interface BookingState {
  searchFilters: SearchFilters;
  searchResults: Turf[];
  selectedTurf: Turf | null;
  selectedAvailability: SlotAvailability[];
  selectedSlotId: number | null;
  bookings: Booking[];
  favorites: Turf[];
  personalization: PersonalizationPayload | null;
}

const initialState: BookingState = {
  searchFilters: {
    city: "",
    date: new Date().toISOString().split("T")[0],
    boxType: "",
    pitchType: "",
  },
  searchResults: [],
  selectedTurf: null,
  selectedAvailability: [],
  selectedSlotId: null,
  bookings: [],
  favorites: [],
  personalization: null,
};

export const bookingSlice = createSlice({
  name: "booking",
  initialState,
  reducers: {
    setSearchFilters: (state, action) => {
      state.searchFilters = action.payload;
    },
    setSearchResults: (state, action) => {
      state.searchResults = action.payload;
    },
    setSelectedTurf: (state, action) => {
      state.selectedTurf = action.payload;
    },
    setSelectedAvailability: (state, action) => {
      state.selectedAvailability = action.payload;
    },
    setSelectedSlotId: (state, action) => {
      state.selectedSlotId = action.payload;
    },
    setBookings: (state, action) => {
      state.bookings = action.payload;
    },
    setFavorites: (state, action) => {
      state.favorites = action.payload;
    },
    setPersonalization: (state, action) => {
      state.personalization = action.payload;
    },
  },
});

export const {
  setSearchFilters,
  setSearchResults,
  setSelectedTurf,
  setSelectedAvailability,
  setSelectedSlotId,
  setBookings,
  setFavorites,
  setPersonalization,
} = bookingSlice.actions;

export default bookingSlice.reducer;
