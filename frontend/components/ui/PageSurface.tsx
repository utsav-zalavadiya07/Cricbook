import type { ReactNode } from "react";
import { TileInfo } from "./TileInfo";

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
        <div className="mt-2">
          <h1 className="theme-title text-2xl font-black leading-tight sm:text-[2rem]">
            {title}
            {description ? (
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label={`${title} information`} focusable={false}>{description}</TileInfo>
              </span>
            ) : null}
          </h1>
        </div>
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
  helperFocusable = true,
  helperAlign = "right",
  helperTooltipClassName = "",
  supportingText,
  action,
  navigationHint,
  className = "",
  valueClassName = "",
}: {
  label: string;
  value: string | number;
  helper?: ReactNode;
  helperFocusable?: boolean;
  helperAlign?: "left" | "right";
  helperTooltipClassName?: string;
  supportingText?: string;
  action?: ReactNode;
  navigationHint?: string;
  className?: string;
  valueClassName?: string;
}) {
  return (
    <div className={`theme-card rounded-xl p-3.5 sm:p-4 ${className}`}>
      <div className="flex min-w-0 items-center gap-2">
        <p className="theme-soft min-w-0 break-words text-[10px] font-black uppercase leading-5 tracking-[0.22em]">{label}</p>
        {helper ? (
          <TileInfo
            variant="standard"
            label={`${label} information`}
            focusable={helperFocusable}
            align={helperAlign}
            tooltipClassName={helperTooltipClassName}
          >
            {helper}
          </TileInfo>
        ) : null}
      </div>
      <div className="mt-2 flex items-end justify-between gap-3">
        <div className="min-w-0">
          <p className={`theme-title min-w-0 text-xl font-black leading-tight ${valueClassName}`}>{value}</p>
          {supportingText ? <p className="theme-muted mt-1 text-[11px] leading-4">{supportingText}</p> : null}
        </div>
        {action ? action : navigationHint ? (
          <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-sky-500 text-white shadow-sm transition-all duration-300 group-hover:bg-sky-600 dark:bg-sky-500/10 dark:text-sky-400 dark:group-hover:bg-sky-500/20 dark:group-hover:text-sky-300 dark:shadow-none">
            <span className="sr-only">{navigationHint}</span>
            <svg
              aria-hidden="true"
              width="15"
              height="15"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="3"
              strokeLinecap="round"
              strokeLinejoin="round"
              className="transition-transform duration-300 group-hover:translate-x-0.5"
            >
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </span>
        ) : null}
      </div>
    </div>
  );
}
