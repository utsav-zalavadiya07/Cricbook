export const AUTH_STORAGE_KEYS = ["token", "refreshToken", "role"] as const;

export type AuthStorageKey = (typeof AUTH_STORAGE_KEYS)[number];

function tokenIssuedAt(token: string | null) {
  if (!token) {
    return 0;
  }

  try {
    const encodedPayload = token.split(".")[1] || "";
    const normalizedPayload = encodedPayload.replace(/-/g, "+").replace(/_/g, "/");
    const paddedPayload = normalizedPayload.padEnd(Math.ceil(normalizedPayload.length / 4) * 4, "=");
    const payload = JSON.parse(atob(paddedPayload)) as { iat?: number; exp?: number };
    return payload.iat ?? payload.exp ?? 0;
  } catch {
    return 0;
  }
}

export function getActiveAuthStorage() {
  if (typeof window === "undefined") {
    return null;
  }

  const sessionToken = window.sessionStorage.getItem("token");
  const localToken = window.localStorage.getItem("token");

  if (sessionToken && localToken) {
    return tokenIssuedAt(sessionToken) >= tokenIssuedAt(localToken)
      ? window.sessionStorage
      : window.localStorage;
  }

  if (sessionToken) {
    return window.sessionStorage;
  }

  return localToken ? window.localStorage : null;
}

export function readAuthStorageValue(key: AuthStorageKey) {
  return getActiveAuthStorage()?.getItem(key) ?? null;
}

export function clearAuthStorageValues() {
  if (typeof window === "undefined") {
    return;
  }

  for (const key of AUTH_STORAGE_KEYS) {
    window.localStorage.removeItem(key);
    window.sessionStorage.removeItem(key);
  }
}
