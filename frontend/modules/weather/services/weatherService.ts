import api from "@/services/api";
import { Forecast, Playability, TurfEnvironment, WeatherDashboard, WeatherPolicy } from "../types/weather.types";

export const getForecast = (params?: { city?: string; date?: string; hour?: number }) =>
  api.get<Forecast>("/weather/forecast", { params });

export const getDayForecast = (params?: { city?: string; date?: string }) =>
  api.get<Forecast[]>("/weather/forecast/day", { params });

export const getPlayability = (params: { turfId: number; slotId: number; date?: string }) =>
  api.get<Playability>("/weather/playability", { params });

export const getWeatherDashboard = (city?: string) =>
  api.get<WeatherDashboard>("/weather/dashboard", { params: { city } });

export const suggestWeatherReschedule = (bookingId: number) =>
  api.post(`/weather/reschedule/${bookingId}`);

export const evaluateWeatherRefund = (bookingId: number) =>
  api.post(`/weather/refund/${bookingId}/evaluate`);

export const getOwnerWeatherDashboard = () =>
  api.get<WeatherDashboard>("/weather/owner/dashboard");

export const getOwnerWeatherPolicy = () =>
  api.get<WeatherPolicy>("/weather/owner/policy");

export const updateOwnerWeatherPolicy = (payload: Partial<WeatherPolicy>) =>
  api.put<WeatherPolicy>("/weather/owner/policy", payload);

export const updateTurfEnvironment = (payload: Partial<TurfEnvironment> & { turfId: number }) =>
  api.put<TurfEnvironment>("/weather/owner/environment", payload);

export const getAdminWeatherDashboard = (city?: string) =>
  api.get<WeatherDashboard>("/weather/admin/dashboard", { params: { city } });
