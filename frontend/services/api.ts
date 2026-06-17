import axios from "axios";
import { clearSessionCookies, persistSessionCookies } from "@/modules/auth/sessionCookies";
import { notifyApiError, notifyApiSuccess } from "@/modules/ui/apiNotificationService";
import { apiErrorToast, toastService } from "@/modules/ui/toastService";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    Accept: "application/json",
  },
});

const authKeys = ["token", "refreshToken", "role"] as const;

function getBrowserStorage() {
  if (typeof window === "undefined") {
    return null;
  }

  return {
    local: window.localStorage,
    session: window.sessionStorage,
  };
}

function readAuthValue(key: (typeof authKeys)[number]) {
  const storage = getBrowserStorage();
  if (!storage) {
    return null;
  }

  return storage.local.getItem(key) ?? storage.session.getItem(key);
}

function activeAuthStorage() {
  const storage = getBrowserStorage();
  if (!storage) {
    return null;
  }

  return storage.local.getItem("token") ? storage.local : storage.session;
}

function clearAuthStorage() {
  const storage = getBrowserStorage();
  if (!storage) {
    return;
  }

  for (const key of authKeys) {
    storage.local.removeItem(key);
    storage.session.removeItem(key);
  }
  void clearSessionCookies();
}

let sessionExpiryHandled = false;

function redirectToLoginAfterSessionExpiry() {
  if (typeof window === "undefined") {
    return;
  }

  const currentPath = `${window.location.pathname}${window.location.search}`;
  if (window.location.pathname.startsWith("/auth/login")) {
    return;
  }

  window.setTimeout(() => {
    window.location.assign(`/auth/login?next=${encodeURIComponent(currentPath)}`);
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
  const token = readAuthValue("token");
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
    const refreshToken = readAuthValue("refreshToken");

    if ((status === 401 || status === 403) && refreshToken && !originalRequest?._retry && !originalRequest?.url?.includes("/auth/refresh")) {
      originalRequest._retry = true;
      try {
        refreshRequest ??= axios
          .post(`${API_BASE_URL}/auth/refresh`, { refreshToken }, { timeout: 15000 })
          .then((res) => {
            const storage = activeAuthStorage();
            storage?.setItem("token", res.data.accessToken);
            storage?.setItem("refreshToken", res.data.refreshToken);
            storage?.setItem("role", res.data.role);
            void persistSessionCookies({
              accessToken: res.data.accessToken,
              refreshToken: res.data.refreshToken,
              role: res.data.role,
              remember: storage === getBrowserStorage()?.local,
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
