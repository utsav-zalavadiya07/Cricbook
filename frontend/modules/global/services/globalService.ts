import api from "@/services/api";

export const getExpansionConfigs = () => api.get("/global/configs");

export const getGlobalCities = () => getExpansionConfigs();

export const createGlobalCity = (payload: {
  city: string;
  country: string;
  currency: string;
  locale: string;
  timeZone: string;
  active: boolean;
}) => api.post("/global/cities", payload);
