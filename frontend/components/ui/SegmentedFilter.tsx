"use client";

import { useId } from "react";
import { usePortal } from "@/hooks/usePortal";

export type SegmentedFilterOption<Value extends string = string> = {
  value: Value;
  label: string;
  count?: number;
  disabled?: boolean;
};

type SegmentedFilterProps<Value extends string> = {
  label: string;
  value: Value;
  options: SegmentedFilterOption<Value>[];
  onChange: (value: Value) => void;
  className?: string;
};

export function SegmentedFilter<Value extends string>({
  label,
  value,
  options,
  onChange,
  className = "",
}: SegmentedFilterProps<Value>) {
  const labelId = useId();
  const { accent } = usePortal();
  const activeClass = accent === "owner"
    ? "bg-sky-500 text-white shadow-sm shadow-sky-500/20"
    : "bg-emerald-500 text-white shadow-sm shadow-emerald-500/20";
  const hoverClass = accent === "owner"
    ? "hover:bg-sky-500/10 hover:text-sky-600 dark:hover:text-sky-300"
    : "hover:bg-emerald-500/10 hover:text-emerald-600 dark:hover:text-emerald-300";
  const focusClass = accent === "owner" ? "focus-visible:ring-sky-400/70" : "focus-visible:ring-emerald-400/70";

  return (
    <div className={`min-w-0 space-y-1.5 ${className}`}>
      <span id={labelId} className="theme-muted block text-[10px] font-black uppercase tracking-[0.16em]">
        {label}
      </span>
      <div className="no-scrollbar overflow-x-auto">
        <div
          role="radiogroup"
          aria-labelledby={labelId}
          className="theme-input inline-flex min-w-max rounded-xl p-1"
        >
          {options.map((option) => {
            const selected = option.value === value;
            return (
              <button
                key={option.value}
                type="button"
                role="radio"
                aria-checked={selected}
                disabled={option.disabled}
                onClick={() => onChange(option.value)}
                className={`rounded-lg px-3 py-2 text-xs font-bold transition duration-150 focus-visible:outline-none focus-visible:ring-2 disabled:cursor-not-allowed disabled:opacity-40 ${focusClass} ${
                  selected ? activeClass : `theme-muted ${hoverClass}`
                }`}
              >
                {option.label}
                {option.count !== undefined && <span className="ml-1 opacity-75">({option.count})</span>}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
