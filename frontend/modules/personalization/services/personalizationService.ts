import api from "@/services/api";
import { SearchFilters } from "@/modules/search/types/search.types";

export const getPersonalization = () => api.get("/personalization");

export const getRecentSearches = () => api.get("/personalization/recent-searches");

export const saveRecentSearch = (filters: SearchFilters) =>
  api.post("/personalization/recent-searches", {
    city: filters.city,
    date: filters.date,
    boxType: filters.boxType,
    pitchType: filters.pitchType,
  });
