"use client";

import { useThemeMode } from "./ThemeProvider";
import { themeOptions } from "./themeTokens";

type ThemeToggleProps = {
  compact?: boolean;
  className?: string;
};

export default function ThemeToggle({ compact = false, className = "" }: ThemeToggleProps) {
  const { mode, setMode } = useThemeMode();

  if (compact) {
    return (
      <div className={`theme-toggle-shell flex items-center gap-1 rounded-2xl p-1 ${className}`.trim()}>
        {themeOptions.map((option) => {
          const active = mode === option.value;
          return (
            <button
              key={option.value}
              type="button"
              suppressHydrationWarning
              onClick={() => setMode(option.value)}
              title={option.label}
              className={`rounded-xl px-2.5 py-2 text-xs font-semibold transition ${
                active ? "theme-toggle-active text-white" : "theme-toggle-inactive"
              }`}
            >
              {compactIcon(option.icon)}
            </button>
          );
        })}
      </div>
    );
  }

  return (
    <div className={`grid gap-2 sm:grid-cols-3 ${className}`.trim()}>
      {themeOptions.map((option) => {
        const active = mode === option.value;
        return (
          <button
            key={option.value}
            type="button"
            suppressHydrationWarning
            onClick={() => setMode(option.value)}
            className={`rounded-2xl border px-4 py-3 text-left transition ${
              active ? "theme-option-active" : "theme-option"
            }`}
          >
            <div className="flex items-center gap-2">
              <span className="text-sm">{compactIcon(option.icon)}</span>
              <span className="theme-title text-sm font-semibold">{option.label}</span>
            </div>
            <p className="theme-muted mt-1 text-xs leading-5">{option.description}</p>
          </button>
        );
      })}
    </div>
  );
}

function compactIcon(icon: string) {
  switch (icon) {
    case "Sun":
      return "Light";
    case "Moon":
      return "Dark";
    default:
      return "Auto";
  }
}
