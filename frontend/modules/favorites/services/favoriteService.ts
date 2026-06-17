import api from "@/services/api";

export const getFavorites = () => api.get("/favorites");

export const saveFavorite = (turfId: number) =>
  api.post(`/favorites/${turfId}`);

export const removeFavorite = (turfId: number) =>
  api.delete(`/favorites/${turfId}`);

export const getFavoriteStatus = (turfId: number) =>
  api.get(`/favorites/${turfId}`);
