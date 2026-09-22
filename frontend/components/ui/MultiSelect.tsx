"use client";

import { useEffect, useRef, useState } from "react";
import type { SelectOption } from "./Select";
import { usePortal } from "@/hooks/usePortal";

interface MultiSelectProps {
  options: SelectOption[];
  value: string[];
  onChange: (value: string[]) => void;
  placeholder?: string;
  accent?: "player" | "owner" | "neutral";
  maxSelected?: number;
  className?: string;
}

const focusClasses = {
  player: "focus:border-emerald-500 focus:ring-emerald-500",
  owner: "focus:border-sky-500 focus:ring-sky-500",
  neutral: "focus:border-indigo-500 focus:ring-indigo-500",
};

const selectedClasses = {
  player: "theme-badge",
  owner: "theme-owner-badge",
  neutral: "border-indigo-500/40 bg-indigo-500/15 text-indigo-500",
};

export function MultiSelect({
  options,
  value,
  onChange,
  placeholder = "Select options",
  accent,
  maxSelected,
  className = "",
}: MultiSelectProps) {
  const portal = usePortal();
  const activeAccent = accent ?? portal.accent;
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);
  const selectedSet = new Set(value.map(String));
  const selectedOptions = options.filter((option) => selectedSet.has(String(option.value)));
  const selectedText = selectedOptions.length
    ? selectedOptions.map((option) => option.label).join(", ")
    : placeholder;

  useEffect(() => {
    const handleClick = (event: MouseEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    const handleKeydown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClick);
    document.addEventListener("keydown", handleKeydown);
    return () => {
      document.removeEventListener("mousedown", handleClick);
      document.removeEventListener("keydown", handleKeydown);
    };
  }, []);

  const toggleValue = (optionValue: string) => {
    if (selectedSet.has(optionValue)) {
      onChange(value.filter((item) => item !== optionValue));
      return;
    }
    if (maxSelected && value.length >= maxSelected) {
      return;
    }
    onChange([...value, optionValue]);
  };

  return (
    <div ref={rootRef} className={`relative ${open ? "z-50" : ""} ${className}`}>
      <button
        type="button"
        aria-haspopup="listbox"
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
        className={`theme-input flex min-h-[48px] w-full items-center justify-between gap-3 rounded-xl px-4 py-3 text-left text-sm font-semibold outline-none transition focus:ring-1 ${focusClasses[activeAccent]}`}
      >
        <span className={selectedOptions.length ? "theme-text truncate" : "theme-soft truncate"}>{selectedText}</span>
        <svg
          aria-hidden="true"
          className={`theme-soft h-4 w-4 shrink-0 transition-transform ${open ? "rotate-180" : ""}`}
          viewBox="0 0 20 20"
          fill="none"
        >
          <path d="M5 7.5L10 12.5L15 7.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>

      {open && (
        <div className="theme-panel-strong absolute left-0 right-0 top-[calc(100%+0.5rem)] z-50 overflow-hidden rounded-2xl">
          <div role="listbox" aria-multiselectable="true" className="max-h-64 overflow-y-auto p-2">
            {options.map((option) => {
              const optionValue = String(option.value);
              const selected = selectedSet.has(optionValue);
              return (
                <button
                  key={optionValue}
                  type="button"
                  role="option"
                  aria-selected={selected}
                  disabled={option.disabled}
                  onClick={() => toggleValue(optionValue)}
                  className={`flex w-full items-center gap-3 rounded-xl border px-3 py-2.5 text-left text-sm font-bold transition disabled:cursor-not-allowed disabled:opacity-50 ${
                    selected ? selectedClasses[activeAccent] : "border-transparent theme-text hover:bg-[var(--theme-panel-soft)]"
                  }`}
                >
                  <span
                    className={`grid h-5 w-5 shrink-0 place-items-center rounded-md border text-[11px] ${
                      selected ? "border-current bg-current/10" : "border-[var(--theme-border)]"
                    }`}
                  >
                    {selected ? "✓" : ""}
                  </span>
                  <span>{option.label}</span>
                </button>
              );
            })}
          </div>
          <div className="flex items-center justify-between gap-2 border-t border-[var(--theme-border)] p-2">
            <button
              type="button"
              onClick={() => onChange([])}
              className="theme-muted rounded-xl px-3 py-2 text-xs font-black transition hover:bg-[var(--theme-panel-soft)]"
            >
              Clear
            </button>
            <p className="theme-soft text-xs">
              {value.length}/{maxSelected && maxSelected < options.length ? maxSelected : options.length} selected
            </p>
            <button
              type="button"
              onClick={() => setOpen(false)}
              className="rounded-xl bg-[var(--theme-secondary)] px-3 py-2 text-xs font-black text-white transition hover:opacity-90"
            >
              Done
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
