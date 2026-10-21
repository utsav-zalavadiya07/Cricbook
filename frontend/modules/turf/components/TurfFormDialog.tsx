"use client";

import { KeyboardEvent, useEffect, useRef } from "react";
import { Button } from "@/components/ui";
import { usePortal } from "@/hooks/usePortal";
import type { Turf } from "../types/turf.types";
import TurfForm from "./TurfForm";

interface TurfFormDialogProps {
  open: boolean;
  mode: "create" | "edit";
  turf?: Turf | null;
  onClose: () => void;
  onSaved: () => void;
}

const focusableSelector =
  'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])';

function dialogCopy(mode: "create" | "edit", turf?: Turf | null) {
  if (mode === "edit") {
    return {
      eyebrow: "Owner turf update",
      title: "Edit turf",
      description: turf?.name
        ? `Update ${turf.name} details, sports, amenities, images, pricing, and map coordinates.`
        : "Update venue details, sports, amenities, images, pricing, and map coordinates.",
    };
  }

  return {
    eyebrow: "Owner turf setup",
    title: "Add a new turf",
    description: "Create the venue here and return to your turf list without switching tabs.",
  };
}

export default function TurfFormDialog({ open, mode, turf, onClose, onSaved }: TurfFormDialogProps) {
  const { isOwner } = usePortal();
  const dialogRef = useRef<HTMLDivElement>(null);
  const closeButtonRef = useRef<HTMLButtonElement>(null);
  const copy = dialogCopy(mode, turf);
  const titleId = `turf-form-dialog-${mode}-title`;

  useEffect(() => {
    if (!open) return;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    window.setTimeout(() => closeButtonRef.current?.focus(), 0);

    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [open]);

  if (!open) return null;

  const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (event.key === "Escape") {
      event.preventDefault();
      onClose();
      return;
    }

    if (event.key !== "Tab" || !dialogRef.current) return;

    const focusable = Array.from(dialogRef.current.querySelectorAll<HTMLElement>(focusableSelector)).filter(
      (element) => element.offsetParent !== null,
    );

    if (!focusable.length) return;

    const first = focusable[0];
    const last = focusable[focusable.length - 1];

    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  };

  return (
    <div className="theme-overlay fixed inset-0 z-50 flex items-center justify-center p-3 backdrop-blur-xl sm:p-4">
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        onKeyDown={handleKeyDown}
        className="theme-panel-strong relative flex max-h-[calc(100vh-2rem)] w-full max-w-3xl flex-col overflow-hidden rounded-2xl shadow-2xl sm:max-h-[calc(100vh-3rem)] sm:rounded-3xl"
      >
        <div className="theme-panel-soft sticky top-0 z-20 flex items-start justify-between gap-4 border-x-0 border-t-0 p-4 sm:p-5">
          <div className="max-w-4xl">
            <p className={`${isOwner ? "theme-accent-owner" : "theme-accent-player"} text-xs font-black uppercase tracking-[0.28em]`}>
              {copy.eyebrow}
            </p>
            <h2 id={titleId} className="theme-title mt-1 text-xl font-black sm:text-2xl">
              {copy.title}
            </h2>
            <p className="theme-muted mt-0.5 text-xs sm:text-sm">{copy.description}</p>
          </div>
          <Button ref={closeButtonRef} tone="secondary" onClick={onClose} className="shrink-0 rounded-full px-3 py-1.5 text-xs font-bold" aria-label="Close turf popup">
            X
          </Button>
        </div>

        <div className={`min-h-0 overflow-y-auto p-4 sm:p-5 ${isOwner ? "owner-scrollbar" : ""}`}>
          <TurfForm
            key={mode === "edit" ? `${turf?.id ?? "edit"}-${(turf?.sports ?? [turf?.sport ?? ""]).join("|")}` : "create"}
            initialData={mode === "edit" ? turf || undefined : undefined}
            turfId={mode === "edit" ? turf?.id : undefined}
            onCancel={onClose}
            onSuccess={onSaved}
          />
        </div>
      </div>
    </div>
  );
}
