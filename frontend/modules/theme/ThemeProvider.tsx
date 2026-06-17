"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { useAuthStore } from "@/store/authStore";
import { getThemePreference, updateThemePreference } from "./services/themeService";
import { DEFAULT_THEME_MODE, normalizeThemeMode, ResolvedTheme, resolveTheme, THEME_STORAGE_KEY, ThemeMode } from "./themeTokens";

type ThemeContextValue = {
  mode: ThemeMode;
  resolvedTheme: ResolvedTheme;
  setMode: (mode: ThemeMode) => void;
  hydrated: boolean;
};

const ThemeContext = createContext<ThemeContextValue | null>(null);

function getSystemPrefersDark() {
  if (typeof window === "undefined" || !window.matchMedia) {
    return true;
  }
  return window.matchMedia("(prefers-color-scheme: dark)").matches;
}

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const token = useAuthStore((state) => state.token);
  const authHydrated = useAuthStore((state) => state.hydrated);
  const [mode, setModeState] = useState<ThemeMode>(DEFAULT_THEME_MODE);
  const [prefersDark, setPrefersDark] = useState(true);
  const [hydrated, setHydrated] = useState(false);
  const hasExplicitLocalPreference = useRef(false);
  const syncedRemotePreference = useRef(false);

  useEffect(() => {
    syncedRemotePreference.current = false;
  }, [token]);

  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }

    const timer = window.setTimeout(() => {
      const storedMode = normalizeThemeMode(localStorage.getItem(THEME_STORAGE_KEY));
      hasExplicitLocalPreference.current = localStorage.getItem(THEME_STORAGE_KEY) !== null;
      setModeState(storedMode);
      setPrefersDark(getSystemPrefersDark());
      setHydrated(true);
    }, 0);

    return () => window.clearTimeout(timer);
  }, []);

  useEffect(() => {
    if (typeof window === "undefined" || !window.matchMedia) {
      return;
    }

    const media = window.matchMedia("(prefers-color-scheme: dark)");
    const listener = (event: MediaQueryListEvent) => setPrefersDark(event.matches);
    media.addEventListener("change", listener);
    return () => media.removeEventListener("change", listener);
  }, []);

  const resolvedTheme = useMemo(() => resolveTheme(mode, prefersDark), [mode, prefersDark]);

  useEffect(() => {
    if (!hydrated || typeof document === "undefined") {
      return;
    }

    document.documentElement.dataset.themeMode = mode;
    document.documentElement.dataset.theme = resolvedTheme;
    document.documentElement.style.colorScheme = resolvedTheme;
    localStorage.setItem(THEME_STORAGE_KEY, mode);
  }, [hydrated, mode, resolvedTheme]);

  useEffect(() => {
    if (!token || !authHydrated || !hydrated || syncedRemotePreference.current) {
      return;
    }

    let active = true;

    getThemePreference()
      .then((response) => {
        if (!active) {
          return;
        }

        const remoteMode = normalizeThemeMode(response.data?.preferredTheme);
        syncedRemotePreference.current = true;

        if (!hasExplicitLocalPreference.current) {
          setModeState(remoteMode);
        }
      })
      .catch(() => {
        syncedRemotePreference.current = true;
      });

    return () => {
      active = false;
    };
  }, [authHydrated, hydrated, token]);

  const setMode = useCallback(
    (nextMode: ThemeMode) => {
      const normalized = normalizeThemeMode(nextMode);
      hasExplicitLocalPreference.current = true;
      setModeState(normalized);
      if (token) {
        updateThemePreference(normalized).catch(() => undefined);
      }
    },
    [token]
  );

  const value = useMemo<ThemeContextValue>(
    () => ({
      mode,
      resolvedTheme,
      setMode,
      hydrated,
    }),
    [hydrated, mode, resolvedTheme, setMode]
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useThemeMode() {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error("useThemeMode must be used within ThemeProvider");
  }
  return context;
}
