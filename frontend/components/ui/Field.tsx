import type { ReactNode } from "react";

interface FieldProps {
  label: ReactNode;
  children: ReactNode;
  hint?: ReactNode;
  error?: ReactNode;
  className?: string;
}

export function Field({ label, children, hint, error, className = "" }: FieldProps) {
  return (
    <label className={`block ${className}`}>
      <span className="theme-muted mb-1.5 block text-sm font-semibold">{label}</span>
      {children}
      {hint ? <span className="theme-soft mt-1 block text-xs">{hint}</span> : null}
      {error ? <span className="mt-1 block text-xs text-red-400">{error}</span> : null}
    </label>
  );
}
