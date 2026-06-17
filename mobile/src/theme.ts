import React, { createContext, ReactNode, useContext } from "react";

export type ThemeMode = "light" | "dark" | "system";
export type ResolvedTheme = "light" | "dark";
export type ThemeColors = {
  bg: string;
  bgElevated: string;
  panel: string;
  panelSoft: string;
  border: string;
  text: string;
  muted: string;
  secondary: string;
  success: string;
  player: string;
  playerSoft: string;
  owner: string;
  ownerSoft: string;
  danger: string;
  error: string;
  warning: string;
  card: string;
  input: string;
};

export type AppTheme = {
  colors: ThemeColors;
  radius: {
    sm: number;
    md: number;
    lg: number;
    pill: number;
  };
  spacing: {
    xs: number;
    sm: number;
    md: number;
    lg: number;
    xl: number;
  };
};

export const themePalettes = {
  dark: {
    colors: {
      bg: "#07111E",
      bgElevated: "#101C2D",
      panel: "#15263D",
      panelSoft: "#12233A",
      border: "#25364D",
      text: "#FFFFFF",
      muted: "#A7B0C0",
      secondary: "#38BDF8",
      success: "#00C853",
      player: "#00C853",
      playerSoft: "#0D4B3A",
      owner: "#1E88E5",
      ownerSoft: "#14395F",
      danger: "#EF4444",
      error: "#EF4444",
      warning: "#F59E0B",
      card: "#0D1828",
      input: "#10233A",
    },
    radius: {
      sm: 10,
      md: 16,
      lg: 24,
      pill: 999,
    },
    spacing: {
      xs: 8,
      sm: 12,
      md: 16,
      lg: 20,
      xl: 24,
    },
  },
  light: {
    colors: {
      bg: "#F4F8FC",
      bgElevated: "#FFFFFF",
      panel: "#FFFFFF",
      panelSoft: "#EEF4FB",
      border: "#D7E1EC",
      text: "#111827",
      muted: "#5B6B81",
      secondary: "#1976D2",
      success: "#00A844",
      player: "#00A844",
      playerSoft: "#DFF5E8",
      owner: "#1976D2",
      ownerSoft: "#DCEBFB",
      danger: "#DC2626",
      error: "#DC2626",
      warning: "#D97706",
      card: "#FFFFFF",
      input: "#F8FBFF",
    },
    radius: {
      sm: 10,
      md: 16,
      lg: 24,
      pill: 999,
    },
    spacing: {
      xs: 8,
      sm: 12,
      md: 16,
      lg: 20,
      xl: 24,
    },
  },
} as const;

export const theme = themePalettes.dark;

const ThemeContext = createContext<AppTheme>(theme);

export function ThemeProvider({ value, children }: { value: AppTheme; children: ReactNode }) {
  return React.createElement(ThemeContext.Provider, { value }, children);
}

export function useTheme() {
  return useContext(ThemeContext);
}

export function resolveThemeMode(mode: ThemeMode, prefersDark: boolean): ResolvedTheme {
  return mode === "system" ? (prefersDark ? "dark" : "light") : mode;
}
