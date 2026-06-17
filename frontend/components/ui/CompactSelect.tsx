"use client";

import { useEffect, useId, useRef, useState } from "react";

export type CompactSelectOption = {
  label: string;
  value: string;
};

type CompactSelectProps = {
  value: string;
  options: CompactSelectOption[];
  onChange: (value: string) => void;
  ariaLabel: string;
  className?: string;
};

export default function CompactSelect({ value, options, onChange, ariaLabel, className = "" }: CompactSelectProps) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);
  const listboxId = useId();
  const selected = options.find((option) => option.value === value) ?? options[0];

  useEffect(() => {
    const handlePointerDown = (event: PointerEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpen(false);
      }
    };

    document.addEventListener("pointerdown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, []);

  return (
    <div ref={rootRef} className={`relative ${className}`}>
      <button
        type="button"
        aria-label={ariaLabel}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={listboxId}
        onClick={() => setOpen((current) => !current)}
        className="theme-input flex min-h-12 w-full items-center justify-between gap-3 rounded-xl px-4 py-2 text-left text-sm font-semibold outline-none transition focus:border-emerald-400/50 focus:ring-1 focus:ring-emerald-400/40"
      >
        <span className="truncate">{selected?.label ?? ariaLabel}</span>
        <svg aria-hidden="true" className={`h-4 w-4 shrink-0 text-[var(--theme-soft)] transition ${open ? "rotate-180" : ""}`} viewBox="0 0 20 20" fill="none">
          <path d="M5 7.5 10 12.5 15 7.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>

      {open && (
        <div className="absolute left-0 right-0 top-full z-30 mt-2 max-h-64 overflow-hidden rounded-2xl border border-[var(--theme-border)] bg-[var(--theme-surface)] shadow-2xl">
          <ul id={listboxId} role="listbox" aria-label={ariaLabel} className="max-h-64 overflow-y-auto p-1">
            {options.map((option) => {
              const active = option.value === value;
              return (
                <li key={option.value} role="option" aria-selected={active}>
                  <button
                    type="button"
                    onClick={() => {
                      onChange(option.value);
                      setOpen(false);
                    }}
                    className={`flex min-h-11 w-full items-center justify-between rounded-xl px-3 py-2 text-left text-sm font-semibold transition ${
                      active
                        ? "bg-emerald-500 text-white"
                        : "theme-text hover:bg-emerald-500/10 hover:text-emerald-300"
                    }`}
                  >
                    <span>{option.label}</span>
                    {active && (
                      <svg aria-hidden="true" className="h-4 w-4" viewBox="0 0 16 16" fill="none">
                        <path d="m3.25 8.25 3 3 6.5-6.75" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                      </svg>
                    )}
                  </button>
                </li>
              );
            })}
          </ul>
        </div>
      )}
    </div>
  );
}
