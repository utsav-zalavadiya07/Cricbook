import type { AxiosError, AxiosResponse, InternalAxiosRequestConfig } from "axios";
import { apiErrorToast, toastService, type ToastRequest, type ToastType } from "@/modules/ui/toastService";

export type ApiNotificationCategory =
  | "booking"
  | "payments"
  | "wallet"
  | "tournament"
  | "team"
  | "academy"
  | "community"
  | "career"
  | "governance"
  | "sports-os"
  | "system"
  | "security"
  | "fan-economy"
  | "turf"
  | "slots";

export type ApiNotificationOverride = {
  title?: string;
  message?: string;
  type?: ToastType;
};

export type ApiNotificationConfig = {
  silent?: boolean;
  success?: boolean | ApiNotificationOverride;
  error?: boolean | ApiNotificationOverride;
  category?: ApiNotificationCategory;
  operation?: string;
  background?: boolean;
};

declare module "axios" {
  export interface AxiosRequestConfig {
    notification?: ApiNotificationConfig;
  }

  export interface InternalAxiosRequestConfig {
    notification?: ApiNotificationConfig;
  }
}

const serverErrorStatuses = new Set([500, 502, 503, 504]);

function methodOf(config?: InternalAxiosRequestConfig | null) {
  return (config?.method ?? "get").toLowerCase();
}

function shouldNotifySuccess(response: AxiosResponse) {
  const notification = response.config.notification;
  if (notification?.silent || notification?.success === false) {
    return false;
  }

  if (notification?.success === true || typeof notification?.success === "object") {
    return true;
  }

  if (notification?.background) {
    return true;
  }

  return false;
}

function payloadMessage(data: unknown) {
  if (!data || typeof data !== "object") {
    return "";
  }

  const payload = data as { message?: unknown; statusMessage?: unknown; detail?: unknown };
  if (typeof payload.message === "string" && payload.message.trim()) {
    return payload.message.trim();
  }
  if (typeof payload.statusMessage === "string" && payload.statusMessage.trim()) {
    return payload.statusMessage.trim();
  }
  if (typeof payload.detail === "string" && payload.detail.trim()) {
    return payload.detail.trim();
  }
  return "";
}

function titleCase(value: string) {
  return value
    .replace(/[-_]+/g, " ")
    .replace(/\s+/g, " ")
    .trim()
    .replace(/\b\w/g, (char) => char.toUpperCase());
}

function inferDomain(url?: string) {
  const normalizedUrl = (url ?? "").toLowerCase();
  if (normalizedUrl.includes("/bookings")) return "Booking";
  if (normalizedUrl.includes("/payments") || normalizedUrl.includes("/refund")) return "Payment";
  if (normalizedUrl.includes("/wallet")) return "Wallet";
  if (normalizedUrl.includes("/tournaments")) return "Tournament";
  if (normalizedUrl.includes("/teams")) return "Team";
  if (normalizedUrl.includes("/academy") || normalizedUrl.includes("/coach")) return "Academy";
  if (normalizedUrl.includes("/career")) return "Career";
  if (normalizedUrl.includes("/governance")) return "Governance";
  if (normalizedUrl.includes("/sports-os")) return "Sports OS";
  if (normalizedUrl.includes("/fan")) return "Fan Economy";
  if (normalizedUrl.includes("/slots")) return "Slot";
  if (normalizedUrl.includes("/turfs")) return "Turf";
  if (normalizedUrl.includes("/auth")) return "Account";
  return "Request";
}

function normalizeUrl(url?: string) {
  return (url ?? "").split("?")[0];
}

function inferSuccessMessage(response: AxiosResponse) {
  const notification = response.config.notification;
  const override = typeof notification?.success === "object" ? notification.success : undefined;
  const method = methodOf(response.config);
  const domain = notification?.operation ? titleCase(notification.operation) : inferDomain(response.config.url);
  const message = override?.message || payloadMessage(response.data);

  if (message) {
    return {
      title: override?.title || (notification?.background ? "Info" : "Success"),
      message,
      type: override?.type ?? (notification?.background ? "info" : "success"),
    };
  }

  const verb = method === "post" ? "created" : method === "put" || method === "patch" ? "updated" : method === "delete" ? "deleted" : "completed";
  return {
    title: override?.title || (notification?.background ? "Info" : "Success"),
    message: `${domain} ${verb} successfully.`,
    type: override?.type ?? (notification?.background ? "info" : "success"),
  };
}

export function notifyApiSuccess(response: AxiosResponse) {
  if (!shouldNotifySuccess(response)) {
    return;
  }

  const toast = inferSuccessMessage(response);
  toastService.show({
    type: toast.type,
    title: toast.title,
    message: toast.message,
    dedupeKey: `api-success:${methodOf(response.config)}:${normalizeUrl(response.config.url)}:${toast.message}`,
  });
}

export function notifyApiError(error: AxiosError) {
  const notification = error.config?.notification;
  if (notification?.silent || notification?.error === false) {
    return;
  }

  const override = typeof notification?.error === "object" ? notification.error : undefined;
  const status = error.response?.status;
  const apiMessage = extractApiErrorMessage(error, undefined);
  const mappedToast = apiErrorToast(status, serverErrorStatuses.has(status ?? 0) ? undefined : apiMessage);
  const toast: ToastRequest = {
    ...mappedToast,
    ...override,
    type: override?.type ?? mappedToast.type,
    title: override?.title ?? mappedToast.title,
    message: override?.message ?? mappedToast.message,
    dedupeKey: mappedToast.dedupeKey ?? `api-error:${status ?? "network"}:${normalizeUrl(error.config?.url)}:${mappedToast.message}`,
  };

  toastService.show(toast);
}

export function extractApiErrorMessage(error: unknown, fallback?: string) {
  const axiosError = error as AxiosError | undefined;
  const data = axiosError?.response?.data;

  if (data && typeof data === "object") {
    const payload = data as {
      message?: unknown;
      error?: unknown;
      details?: unknown;
      errors?: unknown;
    };
    const details = normalizeValidationDetails(payload.details) || normalizeValidationDetails(payload.errors);
    if (details) {
      return details;
    }
    if (typeof payload.message === "string" && payload.message.trim()) {
      return payload.message.trim();
    }
    if (typeof payload.error === "string" && payload.error.trim() && payload.error.toLowerCase() !== "internal server error") {
      return payload.error.trim();
    }
  }

  if (typeof data === "string" && data.trim()) {
    return data.trim();
  }

  return fallback;
}

function normalizeValidationDetails(value: unknown) {
  if (!value) {
    return "";
  }

  if (typeof value === "string") {
    return value.trim();
  }

  if (Array.isArray(value)) {
    return value
      .map((item) => (typeof item === "string" ? item : payloadMessage(item)))
      .filter(Boolean)
      .join("\n");
  }

  if (typeof value === "object") {
    return Object.entries(value as Record<string, unknown>)
      .flatMap(([field, detail]) => {
        if (Array.isArray(detail)) {
          return detail.map((item) => `${titleCase(field)}: ${String(item)}`);
        }
        return [`${titleCase(field)}: ${String(detail)}`];
      })
      .filter(Boolean)
      .join("\n");
  }

  return "";
}
