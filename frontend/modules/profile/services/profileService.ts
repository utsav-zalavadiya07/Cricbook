import api from "@/services/api";
import { UpdatePlayerProfilePayload, UpdateProfilePayload } from "../types/profile.types";

export const getProfile = () => api.get("/users/me");

export const updateProfile = (data: UpdateProfilePayload) =>
  api.put("/users/update", data);

export const changePassword = (data: { currentPassword: string; newPassword: string; confirmPassword: string }) =>
  api.post("/users/change-password", data);

export const deactivateAccount = (data: { password: string }) =>
  api.post("/users/deactivate", data);

export const getBookings = () =>
  api.get("/users/bookings");

export const getReviews = () =>
  api.get("/users/reviews");

export const getMyPlayerProfile = () => api.get("/users/player-profile");

export const getPlayerProfile = (userId: number) => api.get(`/users/player/${userId}`);

export const updatePlayerProfile = (data: UpdatePlayerProfilePayload) => api.put("/users/player-profile", data);
