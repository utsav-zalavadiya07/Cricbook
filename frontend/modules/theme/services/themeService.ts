import api from "@/services/api";
import { ThemeMode } from "../themeTokens";

export const getThemePreference = () => api.get<{ preferredTheme: ThemeMode }>("/users/theme");

export const updateThemePreference = (preferredTheme: ThemeMode) =>
  api.put<{ preferredTheme: ThemeMode }>("/users/theme", { preferredTheme }, { notification: { silent: true } });
