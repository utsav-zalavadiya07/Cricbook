"use client";

import type { InputHTMLAttributes, TextareaHTMLAttributes } from "react";
import { usePortal } from "@/hooks/usePortal";

const focusClasses = {
  player: "focus:border-emerald-500 focus:ring-emerald-500",
  owner: "focus:border-sky-500 focus:ring-sky-500",
  neutral: "focus:border-indigo-500 focus:ring-indigo-500",
};

const baseInputClass =
  "theme-input w-full rounded-xl px-4 py-3 text-sm outline-none transition disabled:cursor-not-allowed disabled:opacity-60 focus:ring-1";

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  accent?: "player" | "owner" | "neutral";
}

export interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  accent?: "player" | "owner" | "neutral";
}

export function Input({ accent, className = "", ...props }: InputProps) {
  const portal = usePortal();
  const activeAccent = accent ?? portal.accent;
  return <input className={`${baseInputClass} ${focusClasses[activeAccent]} ${className}`} {...props} />;
}

export function Textarea({ accent, className = "", ...props }: TextareaProps) {
  const portal = usePortal();
  const activeAccent = accent ?? portal.accent;
  return <textarea className={`${baseInputClass} ${focusClasses[activeAccent]} resize-none ${className}`} {...props} />;
}
