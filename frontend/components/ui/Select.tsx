"use client";

import type { SelectHTMLAttributes } from "react";

export interface SelectOption {
  label: string;
  value: string | number;
  disabled?: boolean;
}

interface SelectProps extends Omit<SelectHTMLAttributes<HTMLSelectElement>, "children"> {
  options: SelectOption[];
  placeholder?: string;
  accent?: "player" | "owner" | "neutral";
}

export function Select({ options, placeholder, accent = "player", className = "", suppressHydrationWarning = true, ...props }: SelectProps) {
  const focusClass =
    accent === "owner"
      ? "focus:border-sky-500 focus:ring-sky-500"
      : accent === "neutral"
        ? "focus:border-indigo-500 focus:ring-indigo-500"
        : "focus:border-emerald-500 focus:ring-emerald-500";

  return (
    <select
      suppressHydrationWarning={suppressHydrationWarning}
      className={`theme-select theme-input w-full rounded-xl px-4 py-3 text-sm font-semibold outline-none transition focus:ring-1 ${focusClass} ${className}`}
      {...props}
    >
      {placeholder ? (
        <option value="" disabled>
          {placeholder}
        </option>
      ) : null}
      {options.map((option) => (
        <option key={String(option.value)} value={option.value} disabled={option.disabled}>
          {option.label}
        </option>
      ))}
    </select>
  );
}
