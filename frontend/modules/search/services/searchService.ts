import api from "@/services/api";
import { SearchFilters, SearchSport } from "../types/search.types";

const serializeSearchParams = (params: SearchFilters) => {
  const searchParams = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    if (Array.isArray(value)) {
      value.forEach((item) => searchParams.append(key, String(item)));
      return;
    }
    searchParams.set(key, String(value));
  });

  return searchParams.toString();
};

export const searchTurfs = (params: SearchFilters) =>
  api.get("/search", { params, paramsSerializer: { serialize: serializeSearchParams } });

export const searchSuggestions = (query: string, limit = 8) =>
  api.get<string[]>("/search/suggestions", { params: { query, limit } });

export const getActiveSports = () =>
  api.get<SearchSport[]>("/sports/active");

export const getAvailability = (turfId: number, date: string) =>
  api.get(`/turfs/${turfId}/availability`, { params: { date } });

export const getTurfDetail = (turfId: number) =>
  api.get(`/turfs/${turfId}`);
