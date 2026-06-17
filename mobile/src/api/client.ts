import AsyncStorage from "@react-native-async-storage/async-storage";

export const API_BASE_URL = "http://192.168.29.175:8080/api/v1";
const SESSION_KEYS = ["token", "refreshToken", "role", "user"] as const;

type RequestOptions = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  query?: Record<string, string | number | boolean | Array<string | number | boolean> | undefined | null>;
  skipAuthRefresh?: boolean;
};

function buildUrl(path: string, query?: RequestOptions["query"]) {
  const url = new URL(`${API_BASE_URL}${path}`);
  if (query) {
    Object.entries(query).forEach(([key, value]) => {
      if (Array.isArray(value)) {
        value.forEach((item) => url.searchParams.append(key, String(item)));
      } else if (value !== undefined && value !== null && value !== "") {
        url.searchParams.set(key, String(value));
      }
    });
  }
  return url.toString();
}

function parseResponseBody(text: string): unknown {
  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function responseErrorMessage(data: unknown): string {
  if (data && typeof data === "object") {
    const payload = data as { error?: string; message?: string };
    return payload.error || payload.message || "Request failed";
  }

  return typeof data === "string" && data ? data : "Request failed";
}

function jwtExpiresAt(token: string | null) {
  if (!token) {
    return null;
  }

  try {
    const encodedPayload = token.split(".")[1] || "";
    const normalizedPayload = encodedPayload.replace(/-/g, "+").replace(/_/g, "/");
    const paddedPayload = normalizedPayload.padEnd(Math.ceil(normalizedPayload.length / 4) * 4, "=");
    const payload = JSON.parse(globalThis.atob(paddedPayload)) as { exp?: number };
    return payload.exp ? payload.exp * 1000 : null;
  } catch {
    return null;
  }
}

export function isJwtExpired(token: string | null) {
  const expiresAt = jwtExpiresAt(token);
  return !expiresAt || expiresAt <= Date.now() + 5000;
}

export async function clearStoredSession() {
  await AsyncStorage.multiRemove(SESSION_KEYS);
}

let refreshRequest: Promise<string | null> | null = null;

export async function refreshMobileSession() {
  refreshRequest ??= (async () => {
    const refreshToken = await AsyncStorage.getItem("refreshToken");
    if (!refreshToken) {
      await clearStoredSession();
      return null;
    }

    const response = await fetch(buildUrl("/auth/refresh"), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
    const text = await response.text();
    const data = parseResponseBody(text) as { accessToken?: string; refreshToken?: string; role?: string; user?: unknown } | null;

    if (!response.ok || !data?.accessToken) {
      await clearStoredSession();
      return null;
    }

    await AsyncStorage.setItem("token", data.accessToken);
    await AsyncStorage.setItem("refreshToken", data.refreshToken || refreshToken);
    if (data.role) {
      await AsyncStorage.setItem("role", data.role);
    }
    if (data.user) {
      await AsyncStorage.setItem("user", JSON.stringify(data.user));
    }

    return data.accessToken;
  })().finally(() => {
    refreshRequest = null;
  });

  return refreshRequest;
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const [[, token], [, tenantId]] = await AsyncStorage.multiGet(["token", "tenantId"]);
  const url = buildUrl(path, options.query);
  const isFormData = typeof FormData !== "undefined" && options.body instanceof FormData;
  const activeToken = token && !isJwtExpired(token) ? token : token && !options.skipAuthRefresh ? await refreshMobileSession() : token;

  const response = await fetch(url, {
    method: options.method || "GET",
    headers: {
      ...(isFormData ? {} : { "Content-Type": "application/json" }),
      ...(activeToken ? { Authorization: `Bearer ${activeToken}` } : {}),
      ...(tenantId ? { "X-Tenant-ID": tenantId } : {}),
    },
    body: options.body ? (isFormData ? (options.body as FormData) : JSON.stringify(options.body)) : undefined,
  });

  const text = await response.text();
  const data = parseResponseBody(text);
  if (!response.ok && !options.skipAuthRefresh && (response.status === 401 || response.status === 403) && !path.includes("/auth/refresh")) {
    const refreshedToken = await refreshMobileSession();
    if (refreshedToken) {
      return apiRequest<T>(path, { ...options, skipAuthRefresh: true });
    }
  }

  if (!response.ok) {
    throw new Error(responseErrorMessage(data));
  }
  return data as T;
}
