import { apiRequest } from "../api/client";

export const searchTurfs = (city: string) => apiRequest<unknown[]>(`/search?city=${encodeURIComponent(city)}`);
export const getBookings = () => apiRequest<unknown[]>("/bookings/my");
export const getWallet = () => apiRequest<unknown>("/wallet");
export const getFavorites = () => apiRequest<unknown[]>("/favorites");
export const getLiveScores = () => apiRequest<unknown[]>("/live-scores");
export const getTournaments = () => apiRequest<unknown[]>("/tournaments");
