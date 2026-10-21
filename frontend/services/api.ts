import axios from "axios";
import { clearAuthStorageValues, getActiveAuthStorage, readAuthStorageValue } from "@/modules/auth/authStorage";
import { clearSessionCookies, persistSessionCookies } from "@/modules/auth/sessionCookies";
import { notifyApiError, notifyApiSuccess } from "@/modules/ui/apiNotificationService";
import { apiErrorToast, toastService } from "@/modules/ui/toastService";
import { useAuthStore, withSessionRefreshLock } from "@/store/authStore";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    Accept: "application/json",
  },
});

function clearAuthStorage() {
  clearAuthStorageValues();
  void clearSessionCookies();
}

let sessionExpiryHandled = false;

function redirectToLoginAfterSessionExpiry() {
  if (typeof window === "undefined") {
    return;
  }

  if (window.location.pathname.startsWith("/auth/login")) {
    return;
  }

  window.setTimeout(() => {
    window.location.assign("/auth/login");
  }, 900);
}

function notifySessionExpired() {
  if (sessionExpiryHandled) {
    return;
  }

  sessionExpiryHandled = true;
  toastService.show(apiErrorToast(401));
  redirectToLoginAfterSessionExpiry();
  if (typeof window === "undefined") {
    sessionExpiryHandled = false;
    return;
  }

  window.setTimeout(() => {
    sessionExpiryHandled = false;
  }, 10000);
}

api.interceptors.request.use((config) => {
  const token = readAuthStorageValue("token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

let refreshRequest: Promise<string> | null = null;

api.interceptors.response.use(
  (response) => {
    notifyApiSuccess(response);
    return response;
  },
  async (error) => {
    const originalRequest = error.config;
    const status = error.response?.status;
    const refreshToken = readAuthStorageValue("refreshToken");
    const failedAccessToken = readAuthStorageValue("token");

    if ((status === 401 || status === 403) && refreshToken && !originalRequest?._retry && !originalRequest?.url?.includes("/auth/refresh")) {
      originalRequest._retry = true;
      try {
        refreshRequest ??= withSessionRefreshLock(async () => {
          const currentAccessToken = readAuthStorageValue("token");
          const currentRefreshToken = readAuthStorageValue("refreshToken");

          if (currentAccessToken && currentAccessToken !== failedAccessToken) {
            useAuthStore.setState({
              token: currentAccessToken,
              refreshToken: currentRefreshToken,
              role: readAuthStorageValue("role"),
              hydrated: true,
            });
            return currentAccessToken;
          }

          if (!currentRefreshToken) {
            throw new Error("Refresh token is unavailable.");
          }

          const res = await axios.post(`${API_BASE_URL}/auth/refresh`, { refreshToken: currentRefreshToken }, { timeout: 15000 });
          if (!res.data.accessToken || !res.data.role) {
            throw new Error("Refresh response is incomplete.");
          }

          const storage = getActiveAuthStorage();
          if (!storage) {
            throw new Error("Authentication storage is unavailable.");
          }

          const nextRefreshToken = res.data.refreshToken ?? currentRefreshToken;
          storage.setItem("token", res.data.accessToken);
          storage.setItem("refreshToken", nextRefreshToken);
          storage.setItem("role", res.data.role);
          await persistSessionCookies({
            accessToken: res.data.accessToken,
            refreshToken: nextRefreshToken,
            role: res.data.role,
            remember: typeof window !== "undefined" && storage === window.localStorage,
          });
          useAuthStore.setState({
            token: res.data.accessToken,
            refreshToken: nextRefreshToken,
            role: res.data.role,
            hydrated: true,
          });
          return res.data.accessToken as string;
        })
          .finally(() => {
            refreshRequest = null;
          });

        const newToken = await refreshRequest;
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return api(originalRequest);
      } catch {
        clearAuthStorage();
        notifySessionExpired();
      }
    }

    if (status === 401 && !originalRequest?.url?.includes("/auth/")) {
      clearAuthStorage();
      notifySessionExpired();
      return Promise.reject(error);
    }

    notifyApiError(error);
    return Promise.reject(error);
  }
);

export default api;
