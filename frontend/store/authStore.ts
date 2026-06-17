import { create } from "zustand";
import { clearSessionCookies, persistSessionCookies } from "@/modules/auth/sessionCookies";

interface User {
  id?: number;
  name?: string;
  email?: string;
}

interface AuthState {
  user: User | null;
  token: string | null;
  refreshToken: string | null;
  role: string | null;
  hydrated: boolean;
  setAuth: (user: User | null, token: string, role: string, refreshToken?: string, options?: { remember?: boolean }) => Promise<void>;
  logout: () => Promise<void>;
  hydrateFromStorage: () => Promise<void>;
}

const AUTH_KEYS = ["token", "refreshToken", "role"] as const;
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

type AuthResponse = {
  accessToken?: string;
  refreshToken?: string;
  role?: string;
  sessionId?: string;
};

function clearStoredAuth() {
  if (typeof window === "undefined") {
    return;
  }

  for (const key of AUTH_KEYS) {
    window.localStorage.removeItem(key);
    window.sessionStorage.removeItem(key);
  }
}

function readStoredAuth(key: (typeof AUTH_KEYS)[number]) {
  if (typeof window === "undefined") {
    return null;
  }

  return window.localStorage.getItem(key) ?? window.sessionStorage.getItem(key);
}

function preferredStorage() {
  if (typeof window === "undefined") {
    return null;
  }

  return window.sessionStorage.getItem("token") ? window.sessionStorage : window.localStorage;
}

function writeStoredAuth(token: string, refreshToken: string | null | undefined, role: string) {
  const storage = preferredStorage();
  if (!storage) {
    return;
  }

  clearStoredAuth();
  storage.setItem("token", token);
  storage.setItem("role", role);
  if (refreshToken) {
    storage.setItem("refreshToken", refreshToken);
  }
}

function jwtExpiresAt(token: string | null) {
  if (!token) {
    return null;
  }

  try {
    const encodedPayload = token.split(".")[1] || "";
    const normalizedPayload = encodedPayload.replace(/-/g, "+").replace(/_/g, "/");
    const paddedPayload = normalizedPayload.padEnd(Math.ceil(normalizedPayload.length / 4) * 4, "=");
    const payload = JSON.parse(atob(paddedPayload)) as { exp?: number };
    return payload.exp ? payload.exp * 1000 : null;
  } catch {
    return null;
  }
}

function isExpired(token: string | null) {
  const expiresAt = jwtExpiresAt(token);
  return !expiresAt || expiresAt <= Date.now() + 5000;
}

async function refreshStoredSession(refreshToken: string) {
  const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  });

  if (!response.ok) {
    return null;
  }

  return await response.json() as AuthResponse;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  token: null,
  refreshToken: null,
  role: null,
  hydrated: false,
  setAuth: async (user, token, role, refreshToken, options) => {
    await persistSessionCookies({ accessToken: token, refreshToken, role, remember: options?.remember });

    if (typeof window !== "undefined") {
      const storage = options?.remember === false ? window.sessionStorage : window.localStorage;
      clearStoredAuth();
      storage.setItem("token", token);
      storage.setItem("role", role);
      if (refreshToken) storage.setItem("refreshToken", refreshToken);
    }

    set({ user, token, role, refreshToken: refreshToken ?? readStoredAuth("refreshToken"), hydrated: true });
  },
  logout: async () => {
    const token = readStoredAuth("token");
    const refreshToken = readStoredAuth("refreshToken");

    try {
      await fetch(`${API_BASE_URL}/auth/logout`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: refreshToken ? JSON.stringify({ refreshToken }) : undefined,
      });
    } catch {
      // Client cleanup must still happen even if the backend session is already expired.
    } finally {
      clearStoredAuth();
      await clearSessionCookies();
      set({ user: null, token: null, refreshToken: null, role: null, hydrated: true });
    }
  },
  hydrateFromStorage: async () => {
    if (typeof window === "undefined") {
      return;
    }

    const token = readStoredAuth("token");
    const refreshToken = readStoredAuth("refreshToken");
    const role = readStoredAuth("role");

    if (!token || !role) {
      clearStoredAuth();
      void clearSessionCookies();
      set({ user: null, token: null, refreshToken: null, role: null, hydrated: true });
      return;
    }

    if (isExpired(token)) {
      if (!refreshToken) {
        clearStoredAuth();
        void clearSessionCookies();
        set({ user: null, token: null, refreshToken: null, role: null, hydrated: true });
        return;
      }

      try {
        const refreshed = await refreshStoredSession(refreshToken);
        if (!refreshed?.accessToken || !refreshed.role) {
          throw new Error("Refresh failed");
        }

        const nextRefreshToken = refreshed.refreshToken ?? refreshToken;
        writeStoredAuth(refreshed.accessToken, nextRefreshToken, refreshed.role);
        await persistSessionCookies({
          accessToken: refreshed.accessToken,
          refreshToken: nextRefreshToken,
          role: refreshed.role,
        });
        set({ token: refreshed.accessToken, refreshToken: nextRefreshToken, role: refreshed.role, hydrated: true });
        return;
      } catch {
        clearStoredAuth();
        void clearSessionCookies();
        set({ user: null, token: null, refreshToken: null, role: null, hydrated: true });
        return;
      }
    }

    set({
      token,
      refreshToken,
      role,
      hydrated: true,
    });
  },
}));
