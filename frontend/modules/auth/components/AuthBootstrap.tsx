"use client";

import { useEffect } from "react";
import { AUTH_STORAGE_KEYS } from "@/modules/auth/authStorage";
import { useAuthStore } from "@/store/authStore";

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

export default function AuthBootstrap() {
  const hydrateFromStorage = useAuthStore((state) => state.hydrateFromStorage);
  const token = useAuthStore((state) => state.token);

  useEffect(() => {
    void hydrateFromStorage();
  }, [hydrateFromStorage]);

  useEffect(() => {
    let synchronizationTimer: number | null = null;

    const synchronizeChangedSession = (event: StorageEvent) => {
      if (!event.key || AUTH_STORAGE_KEYS.includes(event.key as (typeof AUTH_STORAGE_KEYS)[number])) {
        if (synchronizationTimer !== null) {
          window.clearTimeout(synchronizationTimer);
        }
        synchronizationTimer = window.setTimeout(() => {
          synchronizationTimer = null;
          void hydrateFromStorage();
        }, 25);
      }
    };

    window.addEventListener("storage", synchronizeChangedSession);
    return () => {
      window.removeEventListener("storage", synchronizeChangedSession);
      if (synchronizationTimer !== null) {
        window.clearTimeout(synchronizationTimer);
      }
    };
  }, [hydrateFromStorage]);

  useEffect(() => {
    const expiresAt = jwtExpiresAt(token);
    if (!expiresAt) {
      return;
    }

    const timeout = window.setTimeout(() => {
      void hydrateFromStorage();
    }, Math.max(0, expiresAt - Date.now() - 5000));

    return () => window.clearTimeout(timeout);
  }, [hydrateFromStorage, token]);

  return null;
}
