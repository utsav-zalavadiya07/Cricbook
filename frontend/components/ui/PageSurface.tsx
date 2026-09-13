import type { ReactNode } from "react";

export function PageSection({
  children,
  className = "",
}: {
  children: ReactNode;
  className?: string;
}) {
  return <section className={`theme-panel rounded-2xl p-4 sm:p-5 ${className}`}>{children}</section>;
}

export function PageSectionHeader({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: ReactNode;
}) {
  return (
    <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
      <div className="min-w-0 max-w-3xl">
        {eyebrow ? <p className="theme-accent-owner text-[11px] font-black uppercase tracking-[0.24em]">{eyebrow}</p> : null}
        <h1 className="theme-title mt-2 text-2xl font-black leading-tight sm:text-[2rem]">{title}</h1>
        {description ? <p className="theme-muted mt-2 max-w-2xl text-sm sm:text-[15px]">{description}</p> : null}
      </div>
      {actions ? <div className="flex shrink-0 items-start">{actions}</div> : null}
    </div>
  );
}

export function PageMetricGrid({ children }: { children: ReactNode }) {
  return <div className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-5">{children}</div>;
}

export function PageMetricCard({
  label,
  value,
  helper,
}: {
  label: string;
  value: string | number;
  helper: string;
}) {
  return (
    <div className="theme-card rounded-xl p-3.5 sm:p-4">
      <p className="theme-soft text-[10px] font-black uppercase tracking-[0.22em]">{label}</p>
      <p className="theme-title mt-2 text-xl font-black leading-tight">{value}</p>
      <p className="theme-muted mt-1 text-xs leading-5">{helper}</p>
    </div>
  );
}
