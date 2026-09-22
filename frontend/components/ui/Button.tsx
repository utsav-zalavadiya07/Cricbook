"use client";

import { forwardRef } from "react";
import type { ButtonHTMLAttributes, ReactNode } from "react";
import { usePortal } from "@/hooks/usePortal";

type ButtonTone = "player" | "owner" | "secondary" | "danger";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  tone?: ButtonTone;
  children: ReactNode;
}

const toneClasses: Record<ButtonTone, string> = {
  player:
    "bg-gradient-to-r from-emerald-500 to-teal-600 text-white shadow-emerald-500/20 hover:from-emerald-400 hover:to-teal-500",
  owner:
    "bg-gradient-to-r from-sky-500 to-blue-600 text-white shadow-sky-500/20 hover:from-sky-400 hover:to-blue-500",
  secondary:
    "theme-card theme-text hover:bg-[var(--theme-panel-soft)]",
  danger:
    "border border-red-500/30 bg-red-500/10 text-red-300 hover:bg-red-500/15",
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { tone, className = "", children, type = "button", ...props },
  ref,
) {
  const { accent } = usePortal();
  const activeTone = tone ?? accent;

  return (
    <button
      ref={ref}
      type={type}
      className={`inline-flex min-h-11 items-center justify-center rounded-xl px-4 py-2.5 text-sm font-bold shadow-lg transition-all duration-200 hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-50 ${toneClasses[activeTone]} ${className}`}
      {...props}
    >
      {children}
    </button>
  );
});
