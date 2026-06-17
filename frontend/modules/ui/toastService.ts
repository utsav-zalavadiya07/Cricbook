export type ToastType = "success" | "error" | "warning" | "info";

export type ToastRequest = {
  type?: ToastType;
  title?: string;
  message?: string;
  duration?: number;
  dedupeKey?: string;
};

type ToastDispatcher = (toast: ToastRequest) => void;

let dispatcher: ToastDispatcher | null = null;
let clearDispatcher: (() => void) | null = null;
const queuedToasts: ToastRequest[] = [];

function publish(toast: ToastRequest) {
  if (!dispatcher) {
    queuedToasts.push(toast);
    return;
  }

  dispatcher(toast);
}

export function registerToastDispatcher(nextDispatcher: ToastDispatcher, nextClearDispatcher?: () => void) {
  dispatcher = nextDispatcher;
  clearDispatcher = nextClearDispatcher ?? null;
  while (queuedToasts.length > 0) {
    const toast = queuedToasts.shift();
    if (toast) {
      nextDispatcher(toast);
    }
  }

  return () => {
    if (dispatcher === nextDispatcher) {
      dispatcher = null;
      clearDispatcher = null;
    }
  };
}

export function calculateToastDuration(title?: string, message?: string) {
  const textLength = `${title ?? ""} ${message ?? ""}`.trim().length;
  const extraSeconds = textLength <= 40 ? 0 : Math.ceil(textLength / 40);
  return Math.min(10000, 5000 + extraSeconds * 1000);
}

export function apiErrorToast(status?: number, fallback?: string): ToastRequest {
  switch (status) {
    case 400:
      return {
        type: "error",
        title: "Validation Failed",
        message: fallback || "Please check the entered details and try again.",
      };
    case 401:
      return {
        type: "error",
        title: "Session Expired",
        message: "Your session has expired. Please login again.",
        duration: 10000,
        dedupeKey: "session-expired",
      };
    case 403:
      return {
        type: "error",
        title: "Access Denied",
        message: "You do not have permission to perform this action.",
        duration: 8000,
      };
    case 404:
      return {
        type: "error",
        title: "Not Found",
        message: "Requested resource was not found.",
      };
    case 500:
    case 502:
    case 503:
    case 504:
      return {
        type: "error",
        title: "Server Error",
        message: "Something went wrong on our side. Please try again later.",
      };
    case 409:
      return {
        type: "warning",
        title: "Action Blocked",
        message: fallback || "This action conflicts with existing data.",
      };
    case 429:
      return {
        type: "warning",
        title: "Too Many Requests",
        message: fallback || "Please wait before trying again.",
      };
    default:
      return {
        type: "error",
        title: "Connection Lost",
        message: fallback || "Please check your internet connection and try again.",
        duration: 10000,
      };
  }
}

export const toastService = {
  show(toast: ToastRequest) {
    publish(toast);
  },
  showSuccess(title: string, message?: string) {
    publish({ type: "success", title, message });
  },
  showError(title: string, message?: string) {
    publish({ type: "error", title, message });
  },
  showWarning(title: string, message?: string) {
    publish({ type: "warning", title, message });
  },
  showInfo(title: string, message?: string) {
    publish({ type: "info", title, message });
  },
  clearAll() {
    queuedToasts.length = 0;
    clearDispatcher?.();
  },
};
