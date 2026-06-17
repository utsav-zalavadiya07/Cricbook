export type ThemeMode = "light" | "dark" | "system";
export type ResolvedTheme = "light" | "dark";

export const THEME_STORAGE_KEY = "preferredTheme";
export const DEFAULT_THEME_MODE: ThemeMode = "dark";

export const themeOptions: Array<{ value: ThemeMode; label: string; icon: string; description: string }> = [
  { value: "light", label: "Light", icon: "Sun", description: "Bright, clean surfaces for daytime use." },
  { value: "dark", label: "Dark", icon: "Moon", description: "Deep contrast with Cricbook's default sports look." },
  { value: "system", label: "System", icon: "System", description: "Follow your device appearance automatically." },
];

export const themePalettes = {
  dark: {
    bg: "#07111E",
    surface: "#101C2D",
    surfaceStrong: "#15263D",
    panel: "#0D192A",
    elevated: "#12233A",
    border: "rgba(167, 176, 192, 0.16)",
    text: "#F8FAFC",
    textMuted: "#A7B0C0",
    textSoft: "#7C8CA5",
    primary: "#00C853",
    primarySoft: "rgba(0, 200, 83, 0.16)",
    secondary: "#1E88E5",
    secondarySoft: "rgba(30, 136, 229, 0.16)",
    shadow: "0 24px 80px rgba(2, 6, 23, 0.42)",
    playerGlow: "radial-gradient(circle at top left, rgba(0, 200, 83, 0.18), transparent 30%)",
    ownerGlow: "radial-gradient(circle at top left, rgba(30, 136, 229, 0.18), transparent 30%)",
    adminGlow: "radial-gradient(circle at top left, rgba(59, 130, 246, 0.18), transparent 30%)",
  },
  light: {
    bg: "#F4F8FC",
    surface: "#FFFFFF",
    surfaceStrong: "#EEF4FB",
    panel: "#FFFFFF",
    elevated: "#F8FBFF",
    border: "rgba(15, 23, 42, 0.10)",
    text: "#111827",
    textMuted: "#5B6B81",
    textSoft: "#7A8799",
    primary: "#00A844",
    primarySoft: "rgba(0, 168, 68, 0.12)",
    secondary: "#1976D2",
    secondarySoft: "rgba(25, 118, 210, 0.12)",
    shadow: "0 22px 60px rgba(15, 23, 42, 0.08)",
    playerGlow: "radial-gradient(circle at top left, rgba(0, 168, 68, 0.12), transparent 30%)",
    ownerGlow: "radial-gradient(circle at top left, rgba(25, 118, 210, 0.12), transparent 30%)",
    adminGlow: "radial-gradient(circle at top left, rgba(30, 64, 175, 0.12), transparent 30%)",
  },
} as const;

export function normalizeThemeMode(input?: string | null): ThemeMode {
  if (input === "light" || input === "dark" || input === "system") {
    return input;
  }
  return DEFAULT_THEME_MODE;
}

export function resolveTheme(mode: ThemeMode, prefersDark: boolean): ResolvedTheme {
  return mode === "system" ? (prefersDark ? "dark" : "light") : mode;
}
