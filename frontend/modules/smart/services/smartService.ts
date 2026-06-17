import api from "@/services/api";

export const getSmartRecommendations = (params?: { city?: string; turfId?: number; date?: string }) =>
  api.get("/smart/recommendations", { params });
