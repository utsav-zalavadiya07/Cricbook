import { create } from "zustand";
import { clearAuthStorageValues, getActiveAuthStorage, readAuthStorageValue } from "@/modules/auth/authStorage";
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

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

type AuthResponse = {
  accessToken?: string;
  refreshToken?: string;
  role?: string;
  sessionId?: string;
};

type JwtPayload = {
  exp?: number;
  role?: string;
};

let hydrationRequest: Promise<void> | null = null;

function clearStoredAuth() {
  clearAuthStorageValues();
}

function readStoredAuth(key: "token" | "refreshToken" | "role") {
  return readAuthStorageValue(key);
}

function writeStoredAuth(token: string, refreshToken: string | null | undefined, role: string) {
  const storage = getActiveAuthStorage();
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

function decodeJwtPayload(token: string | null): JwtPayload | null {
  if (!token) {
    return null;
  }

  try {
    const encodedPayload = token.split(".")[1] || "";
    const normalizedPayload = encodedPayload.replace(/-/g, "+").replace(/_/g, "/");
    const paddedPayload = normalizedPayload.padEnd(Math.ceil(normalizedPayload.length / 4) * 4, "=");
    return JSON.parse(atob(paddedPayload)) as JwtPayload;
  } catch {
    return null;
  }
}

function isExpired(token: string | null) {
  const exp = decodeJwtPayload(token)?.exp;
  const expiresAt = exp ? exp * 1000 : null;
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

export async function withSessionRefreshLock<T>(refresh: () => Promise<T>) {
  if (typeof navigator !== "undefined" && navigator.locks) {
    return navigator.locks.request("cricbook-session-refresh", refresh);
  }

  return refresh();
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
  hydrateFromStorage: () => {
    if (hydrationRequest) {
      return hydrationRequest;
    }

    hydrationRequest = (async () => {
      if (typeof window === "undefined") {
        return;
      }

      const token = readStoredAuth("token");
      const refreshToken = readStoredAuth("refreshToken");
      const storedRole = readStoredAuth("role");
      const tokenRole = decodeJwtPayload(token)?.role;
      const role = tokenRole || storedRole;

      if (!token || !role) {
        clearStoredAuth();
        await clearSessionCookies();
        set({ user: null, token: null, refreshToken: null, role: null, hydrated: true });
        return;
      }

      if (isExpired(token)) {
        try {
          const refreshed = await withSessionRefreshLock(async () => {
            const currentToken = readStoredAuth("token");
            const currentRefreshToken = readStoredAuth("refreshToken");
            const currentRole = decodeJwtPayload(currentToken)?.role || readStoredAuth("role");

            if (currentToken && currentRole && !isExpired(currentToken)) {
              return {
                accessToken: currentToken,
                refreshToken: currentRefreshToken ?? undefined,
                role: currentRole,
              } satisfies AuthResponse;
            }

            if (!currentRefreshToken) {
              return null;
            }

            const nextSession = await refreshStoredSession(currentRefreshToken);
            if (!nextSession?.accessToken || !nextSession.role) {
              return null;
            }

            const remember = getActiveAuthStorage() === window.localStorage;
            const nextRefreshToken = nextSession.refreshToken ?? currentRefreshToken;
            writeStoredAuth(nextSession.accessToken, nextRefreshToken, nextSession.role);
            await persistSessionCookies({
              accessToken: nextSession.accessToken,
              refreshToken: nextRefreshToken,
              role: nextSession.role,
              remember,
            });

            return {
              ...nextSession,
              refreshToken: nextRefreshToken,
            };
          });

          if (!refreshed?.accessToken || !refreshed.role) {
            throw new Error("Refresh failed");
          }

          set({
            token: refreshed.accessToken,
            refreshToken: refreshed.refreshToken ?? refreshToken,
            role: refreshed.role,
            hydrated: true,
          });
          return;
        } catch {
          clearStoredAuth();
          await clearSessionCookies();
          set({ user: null, token: null, refreshToken: null, role: null, hydrated: true });
          return;
        }
      }

      try {
        await persistSessionCookies({
          accessToken: token,
          refreshToken,
          role,
          remember: getActiveAuthStorage() === window.localStorage,
        });
        set({ token, refreshToken, role, hydrated: true });
      } catch {
        clearStoredAuth();
        await clearSessionCookies();
        set({ user: null, token: null, refreshToken: null, role: null, hydrated: true });
      }
    })().finally(() => {
      hydrationRequest = null;
    });

    return hydrationRequest;
  },
}));
