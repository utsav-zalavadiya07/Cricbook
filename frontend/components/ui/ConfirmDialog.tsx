"use client";

import { createContext, useCallback, useContext, useEffect, useId, useMemo, useRef, useState } from "react";
import type { KeyboardEvent, ReactNode } from "react";

type ConfirmTone = "player" | "owner" | "danger";

export type ConfirmDialogOptions = {
  title: string;
  description?: ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  tone?: ConfirmTone;
  initialFocus?: "confirm" | "cancel";
};

type ConfirmDialogState = Required<Pick<ConfirmDialogOptions, "title" | "confirmLabel" | "cancelLabel" | "tone" | "initialFocus">> & {
  description?: ReactNode;
  resolve: (confirmed: boolean) => void;
};

const toneConfig: Record<
  ConfirmTone,
  {
    icon: ReactNode;
    badgeClassName: string;
    confirmClassName: string;
    eyebrow: string;
  }
> = {
  player: {
    icon: <path d="M12 5v14m-7-7h14" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" />,
    badgeClassName: "bg-emerald-500/15 text-emerald-500 ring-emerald-500/20",
    confirmClassName:
      "bg-gradient-to-r from-emerald-500 to-teal-600 text-white shadow-emerald-500/20 hover:from-emerald-400 hover:to-teal-500 focus:ring-emerald-500/25",
    eyebrow: "Confirmation",
  },
  owner: {
    icon: <path d="m6.5 12 3.2 3.2 7-7" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />,
    badgeClassName: "bg-sky-500/15 text-sky-500 ring-sky-500/20",
    confirmClassName:
      "bg-gradient-to-r from-sky-500 to-blue-600 text-white shadow-sky-500/20 hover:from-sky-400 hover:to-blue-500 focus:ring-sky-500/25",
    eyebrow: "Owner action",
  },
  danger: {
    icon: (
      <>
        <path d="M12 8v4.4" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" />
        <path d="M12 16h.01" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
      </>
    ),
    badgeClassName: "bg-red-500/15 text-red-500 ring-red-500/20",
    confirmClassName:
      "bg-gradient-to-r from-red-500 to-rose-600 text-white shadow-red-500/20 hover:from-red-400 hover:to-rose-500 focus:ring-red-500/25",
    eyebrow: "Needs confirmation",
  },
};

const ConfirmDialogContext = createContext<((options: ConfirmDialogOptions) => Promise<boolean>) | null>(null);

export function ConfirmDialogProvider({ children }: { children: ReactNode }) {
  const [dialog, setDialog] = useState<ConfirmDialogState | null>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);
  const cancelButtonRef = useRef<HTMLButtonElement | null>(null);
  const confirmButtonRef = useRef<HTMLButtonElement | null>(null);
  const titleId = useId();
  const descriptionId = useId();

  const closeDialog = useCallback(
    (confirmed: boolean) => {
      setDialog((current) => {
        current?.resolve(confirmed);
        return null;
      });
      window.setTimeout(() => previousFocusRef.current?.focus(), 0);
    },
    [],
  );

  const confirm = useCallback((options: ConfirmDialogOptions) => {
    previousFocusRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;

    return new Promise<boolean>((resolve) => {
      const tone = options.tone ?? "player";
      setDialog({
        title: options.title,
        description: options.description,
        confirmLabel: options.confirmLabel ?? "Confirm",
        cancelLabel: options.cancelLabel ?? "Cancel",
        tone,
        initialFocus: options.initialFocus ?? (tone === "danger" ? "cancel" : "confirm"),
        resolve,
      });
    });
  }, []);

  useEffect(() => {
    if (!dialog) return;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    window.setTimeout(() => {
      const target = dialog.initialFocus === "cancel" ? cancelButtonRef.current : confirmButtonRef.current;
      target?.focus();
    }, 0);

    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [dialog]);

  const value = useMemo(() => confirm, [confirm]);

  const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (!dialog) return;

    if (event.key === "Escape") {
      event.preventDefault();
      closeDialog(false);
      return;
    }

    if (event.key !== "Tab") return;

    const focusableElements = [cancelButtonRef.current, confirmButtonRef.current].filter(
      (element): element is HTMLButtonElement => !!element && !element.disabled,
    );
    if (focusableElements.length === 0) return;

    const firstElement = focusableElements[0];
    const lastElement = focusableElements[focusableElements.length - 1];

    if (event.shiftKey && document.activeElement === firstElement) {
      event.preventDefault();
      lastElement.focus();
    } else if (!event.shiftKey && document.activeElement === lastElement) {
      event.preventDefault();
      firstElement.focus();
    }
  };

  const activeTone = dialog ? toneConfig[dialog.tone] : null;

  return (
    <ConfirmDialogContext.Provider value={value}>
      {children}
      {dialog && activeTone && (
        <div
          className="fixed inset-0 z-[120] flex items-center justify-center bg-[rgba(5,20,48,0.76)] px-4 py-6 backdrop-blur-[4px]"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) closeDialog(false);
          }}
        >
          <div
            role="alertdialog"
            aria-modal="true"
            aria-labelledby={titleId}
            aria-describedby={dialog.description ? descriptionId : undefined}
            onKeyDown={handleKeyDown}
            className="relative w-full max-w-lg overflow-hidden rounded-[2rem] border border-[var(--theme-border)] bg-[var(--theme-panel)] p-0 shadow-[0_30px_90px_rgba(2,8,23,0.52)] outline-none"
          >
            <div className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-transparent via-current to-transparent opacity-50" />
            <div className="p-6 sm:p-7">
              <div className="flex items-start gap-4">
                <div className={`grid h-12 w-12 shrink-0 place-items-center rounded-2xl ring-1 ${activeTone.badgeClassName}`}>
                  <svg aria-hidden="true" className="h-6 w-6" viewBox="0 0 24 24" fill="none">
                    {activeTone.icon}
                  </svg>
                </div>
                <div className="min-w-0">
                  <p className="theme-soft text-[11px] font-black uppercase tracking-[0.24em]">{activeTone.eyebrow}</p>
                  <h2 id={titleId} className="theme-title mt-1 text-xl font-black leading-tight sm:text-2xl">
                    {dialog.title}
                  </h2>
                  {dialog.description && (
                    <div id={descriptionId} className="theme-muted mt-2 text-sm leading-6">
                      {dialog.description}
                    </div>
                  )}
                </div>
              </div>

              <div className="mt-7 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
                <button
                  ref={cancelButtonRef}
                  type="button"
                  onClick={() => closeDialog(false)}
                  className="theme-card theme-text inline-flex min-h-11 items-center justify-center rounded-xl px-5 py-2.5 text-sm font-black transition hover:bg-[var(--theme-panel-soft)] focus:outline-none focus:ring-4 focus:ring-slate-500/15"
                >
                  {dialog.cancelLabel}
                </button>
                <button
                  ref={confirmButtonRef}
                  type="button"
                  onClick={() => closeDialog(true)}
                  className={`inline-flex min-h-11 items-center justify-center rounded-xl px-5 py-2.5 text-sm font-black shadow-lg transition hover:-translate-y-0.5 focus:outline-none focus:ring-4 ${activeTone.confirmClassName}`}
                >
                  {dialog.confirmLabel}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </ConfirmDialogContext.Provider>
  );
}

export function useConfirmDialog(): (options: ConfirmDialogOptions) => Promise<boolean> {
  const confirm = useContext(ConfirmDialogContext);
  if (!confirm) {
    return async () => false;
  }
  return confirm;
}
