"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import type { ReactNode } from "react";
import { calculateToastDuration, registerToastDispatcher, type ToastRequest, type ToastType } from "@/modules/ui/toastService";

type Toast = Required<Pick<ToastRequest, "type" | "title" | "message" | "duration">> & {
  id: string;
  dedupeKey?: string;
  exiting?: boolean;
};

const ToastContext = createContext<{
  toast: (message: string, type?: ToastType) => void;
  showSuccess: (title: string, message?: string) => void;
  showError: (title: string, message?: string) => void;
  showWarning: (title: string, message?: string) => void;
  showInfo: (title: string, message?: string) => void;
  clearAll: () => void;
} | null>(null);

const toastConfig: Record<
  ToastType,
  {
    label: string;
    accentClassName: string;
    iconClassName: string;
    icon: ReactNode;
  }
> = {
  success: {
    label: "Success",
    accentClassName: "from-emerald-500/25 via-emerald-500/10 to-transparent",
    iconClassName: "border-emerald-500/30 bg-emerald-500/15 text-emerald-400",
    icon: <path d="m6 12.5 3.2 3.2L18 7.8" stroke="currentColor" strokeWidth="2.3" strokeLinecap="round" strokeLinejoin="round" />,
  },
  error: {
    label: "Error",
    accentClassName: "from-red-500/25 via-red-500/10 to-transparent",
    iconClassName: "border-red-500/30 bg-red-500/15 text-red-400",
    icon: (
      <>
        <path d="M12 7.5v5" stroke="currentColor" strokeWidth="2.3" strokeLinecap="round" />
        <path d="M12 16.5h.01" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
      </>
    ),
  },
  warning: {
    label: "Warning",
    accentClassName: "from-amber-500/25 via-amber-500/10 to-transparent",
    iconClassName: "border-amber-500/30 bg-amber-500/15 text-amber-400",
    icon: (
      <>
        <path d="m12 4 8 14H4L12 4Z" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" />
        <path d="M12 9v4" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" />
        <path d="M12 16h.01" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" />
      </>
    ),
  },
  info: {
    label: "Info",
    accentClassName: "from-sky-500/25 via-sky-500/10 to-transparent",
    iconClassName: "border-sky-500/30 bg-sky-500/15 text-sky-400",
    icon: (
      <>
        <path d="M12 11v5" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" />
        <path d="M12 8h.01" stroke="currentColor" strokeWidth="2.8" strokeLinecap="round" />
        <circle cx="12" cy="12" r="8" stroke="currentColor" strokeWidth="2" />
      </>
    ),
  },
};

function defaultTitle(type: ToastType) {
  return toastConfig[type].label;
}

function createToast(request: ToastRequest): Toast {
  const type = request.type ?? "info";
  const title = request.title?.trim() || defaultTitle(type);
  const message = request.message?.trim() || "";

  return {
    id: `${Date.now()}-${Math.random().toString(36).slice(2)}`,
    type,
    title,
    message,
    duration: request.duration ?? calculateToastDuration(title, message),
    dedupeKey: request.dedupeKey,
  };
}

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const visibleLimitRef = useRef(5);

  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.map((item) => (item.id === id ? { ...item, exiting: true } : item)));
    window.setTimeout(() => {
      setToasts((prev) => prev.filter((item) => item.id !== id));
    }, 200);
  }, []);

  const pushToast = useCallback((request: ToastRequest) => {
    const nextToast = createToast(request);

    setToasts((prev) => {
      const withoutDuplicate = prev.filter((item) => {
        if (nextToast.dedupeKey && item.dedupeKey === nextToast.dedupeKey) {
          return false;
        }
        return !(item.type === nextToast.type && item.title === nextToast.title && item.message === nextToast.message);
      });
      return [nextToast, ...withoutDuplicate].slice(0, visibleLimitRef.current);
    });
  }, []);

  useEffect(() => {
    const mediaQuery = window.matchMedia("(max-width: 640px)");
    const updateLimit = () => {
      const nextLimit = mediaQuery.matches ? 3 : 5;
      visibleLimitRef.current = nextLimit;
      setToasts((prev) => prev.slice(0, nextLimit));
    };

    updateLimit();
    mediaQuery.addEventListener("change", updateLimit);
    return () => mediaQuery.removeEventListener("change", updateLimit);
  }, []);

  const clearAll = useCallback(() => {
    setToasts((prev) => prev.map((item) => ({ ...item, exiting: true })));
    window.setTimeout(() => setToasts([]), 200);
  }, []);

  useEffect(() => registerToastDispatcher(pushToast, clearAll), [clearAll, pushToast]);

  const toast = useCallback(
    (message: string, type: ToastType = "info") => {
      pushToast({ type, title: defaultTitle(type), message });
    },
    [pushToast],
  );

  const showSuccess = useCallback((title: string, message?: string) => pushToast({ type: "success", title, message }), [pushToast]);
  const showError = useCallback((title: string, message?: string) => pushToast({ type: "error", title, message }), [pushToast]);
  const showWarning = useCallback((title: string, message?: string) => pushToast({ type: "warning", title, message }), [pushToast]);
  const showInfo = useCallback((title: string, message?: string) => pushToast({ type: "info", title, message }), [pushToast]);

  const value = useMemo(
    () => ({ toast, showSuccess, showError, showWarning, showInfo, clearAll }),
    [clearAll, showError, showInfo, showSuccess, showWarning, toast],
  );

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div
        aria-live="polite"
        aria-relevant="additions text"
        className="pointer-events-none fixed inset-x-4 bottom-4 z-[110] flex flex-col gap-3 sm:inset-x-auto sm:bottom-6 sm:right-6 sm:w-[420px]"
      >
        {toasts.map((item) => (
          <ToastCard key={item.id} toast={item} onRemove={removeToast} />
        ))}
      </div>
    </ToastContext.Provider>
  );
}

function ToastCard({ toast, onRemove }: { toast: Toast; onRemove: (id: string) => void }) {
  const [entered, setEntered] = useState(false);
  const remainingRef = useRef(toast.duration);
  const startedAtRef = useRef(0);
  const timerRef = useRef<number | null>(null);
  const config = toastConfig[toast.type];

  const clearTimer = useCallback(() => {
    if (timerRef.current) {
      window.clearTimeout(timerRef.current);
      timerRef.current = null;
    }
  }, []);

  const startTimer = useCallback(() => {
    clearTimer();
    startedAtRef.current = Date.now();
    timerRef.current = window.setTimeout(() => onRemove(toast.id), remainingRef.current);
  }, [clearTimer, onRemove, toast.id]);

  const pauseTimer = useCallback(() => {
    if (!timerRef.current) return;
    remainingRef.current = Math.max(0, remainingRef.current - (Date.now() - startedAtRef.current));
    clearTimer();
  }, [clearTimer]);

  const resumeTimer = useCallback(() => {
    if (remainingRef.current <= 0) {
      onRemove(toast.id);
      return;
    }
    startTimer();
  }, [onRemove, startTimer, toast.id]);

  useEffect(() => {
    const animationFrame = window.requestAnimationFrame(() => setEntered(true));
    startTimer();
    return () => {
      window.cancelAnimationFrame(animationFrame);
      clearTimer();
    };
  }, [clearTimer, startTimer]);

  return (
    <div
      role={toast.type === "error" ? "alert" : "status"}
      aria-atomic="true"
      onMouseEnter={pauseTimer}
      onMouseLeave={resumeTimer}
      className={`pointer-events-auto relative overflow-hidden rounded-2xl border border-[var(--theme-border)] bg-[var(--theme-panel)] shadow-[0_18px_50px_rgba(2,8,23,0.32)] transition-all duration-[250ms] ease-out ${
        entered && !toast.exiting ? "translate-y-0 opacity-100" : "translate-y-4 opacity-0"
      }`}
    >
      <div className={`absolute inset-y-0 left-0 w-1.5 bg-gradient-to-b ${config.accentClassName}`} />
      <div className="flex gap-3 p-4 pr-3">
        <div className={`mt-0.5 grid h-9 w-9 shrink-0 place-items-center rounded-xl border ${config.iconClassName}`}>
          <svg aria-hidden="true" className="h-5 w-5" viewBox="0 0 24 24" fill="none">
            {config.icon}
          </svg>
        </div>
        <div className="min-w-0 flex-1">
          <p className="theme-title text-sm font-black leading-5">{toast.title}</p>
          {toast.message && <p className="theme-muted mt-1 whitespace-pre-line text-sm leading-5">{toast.message}</p>}
        </div>
        <button
          type="button"
          aria-label={`Dismiss ${config.label.toLowerCase()} notification`}
          onClick={() => onRemove(toast.id)}
          className="theme-muted -mr-1 grid h-8 w-8 shrink-0 place-items-center rounded-xl transition hover:bg-[var(--theme-panel-soft)] hover:text-[var(--theme-text)] focus:outline-none focus:ring-2 focus:ring-sky-500/40"
        >
          <svg aria-hidden="true" className="h-4 w-4" viewBox="0 0 24 24" fill="none">
            <path d="m7 7 10 10M17 7 7 17" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" />
          </svg>
        </button>
      </div>
    </div>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) {
    return {
      toast: () => undefined,
      showSuccess: () => undefined,
      showError: () => undefined,
      showWarning: () => undefined,
      showInfo: () => undefined,
      clearAll: () => undefined,
    };
  }
  return ctx;
}
