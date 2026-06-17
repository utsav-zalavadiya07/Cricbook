"use client";

import type { ReactNode } from "react";

export type AdminRowAction = {
  label: string;
  intent?: "primary" | "neutral" | "danger";
  execute: () => Promise<void>;
};

export type AdminTableRow = {
  id: string | number;
  title: string;
  subtitle?: string;
  meta?: string[];
  amount?: string | number;
  status?: string;
  priority?: string;
  updatedAt?: string;
  actions?: AdminRowAction[];
};

export type AdminMetric = {
  label: string;
  value: string | number;
  detail?: string;
  tone?: "emerald" | "sky" | "amber" | "rose" | "violet" | "slate";
};

type AdminPageHeaderProps = {
  eyebrow: string;
  title: string;
  description: string;
  actions?: ReactNode;
};

export function AdminPageHeader({ eyebrow, title, description, actions }: AdminPageHeaderProps) {
  return (
    <section className="rounded-[2rem] border border-white/10 bg-gradient-to-br from-slate-900 via-slate-900 to-emerald-950/30 p-5 shadow-2xl sm:p-6">
      <div className="flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">
        <div>
          <p className="text-xs font-black uppercase tracking-[0.32em] text-emerald-400">{eyebrow}</p>
          <h1 className="mt-3 text-3xl font-black tracking-tight text-white sm:text-4xl">{title}</h1>
          <p className="mt-3 max-w-4xl text-sm leading-6 text-slate-300">{description}</p>
        </div>
        {actions ? <div className="flex flex-wrap gap-2">{actions}</div> : null}
      </div>
    </section>
  );
}

export function AdminMetricCard({ label, value, detail, tone = "slate" }: AdminMetric) {
  const toneClass = {
    emerald: "border-emerald-500/25 bg-emerald-500/10 text-emerald-300",
    sky: "border-sky-500/25 bg-sky-500/10 text-sky-300",
    amber: "border-amber-500/25 bg-amber-500/10 text-amber-300",
    rose: "border-rose-500/25 bg-rose-500/10 text-rose-300",
    violet: "border-violet-500/25 bg-violet-500/10 text-violet-300",
    slate: "border-white/10 bg-slate-900/80 text-slate-300",
  }[tone];

  return (
    <article className={`rounded-3xl border p-5 shadow-xl ${toneClass}`}>
      <p className="text-xs font-black uppercase tracking-[0.2em] opacity-80">{label}</p>
      <p className="mt-3 text-3xl font-black text-white">{value}</p>
      {detail ? <p className="mt-2 text-xs leading-5 text-slate-400">{detail}</p> : null}
    </article>
  );
}

export function AdminStatusBadge({ value }: { value?: string }) {
  const normalized = (value || "UNKNOWN").toUpperCase();
  const tone =
    ["ACTIVE", "APPROVED", "PAID", "SUCCESS", "RESOLVED", "LIVE", "COMPLETED", "SENT", "PUBLISHED"].includes(normalized)
      ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-300"
      : ["PENDING", "OPEN", "IN_PROGRESS", "WAITING_CUSTOMER", "PROCESSING", "DRAFT"].includes(normalized)
        ? "border-amber-500/30 bg-amber-500/10 text-amber-300"
        : ["FAILED", "REJECTED", "SUSPENDED", "BANNED", "CRITICAL", "HIGH", "CANCELLED", "BLOCKED"].includes(normalized)
          ? "border-rose-500/30 bg-rose-500/10 text-rose-300"
          : "border-slate-500/30 bg-slate-500/10 text-slate-300";

  return (
    <span className={`inline-flex rounded-full border px-3 py-1 text-[11px] font-black uppercase tracking-[0.14em] ${tone}`}>
      {normalized.replaceAll("_", " ")}
    </span>
  );
}

type AdminToolbarProps = {
  search: string;
  onSearchChange: (value: string) => void;
  status: string;
  onStatusChange: (value: string) => void;
  statuses?: string[];
  onRefresh: () => void;
  loading?: boolean;
  children?: ReactNode;
};

export function AdminToolbar({
  search,
  onSearchChange,
  status,
  onStatusChange,
  statuses = ["ALL", "ACTIVE", "PENDING", "OPEN", "RESOLVED", "FAILED"],
  onRefresh,
  loading,
  children,
}: AdminToolbarProps) {
  return (
    <section className="rounded-3xl border border-white/10 bg-slate-900/75 p-4 shadow-xl">
      <div className="grid gap-3 xl:grid-cols-[minmax(220px,1fr)_220px_auto] xl:items-end">
        <label className="block">
          <span className="text-xs font-black uppercase tracking-[0.18em] text-slate-500">Search</span>
          <input
            value={search}
            onChange={(event) => onSearchChange(event.target.value)}
            placeholder="Search name, ID, email, turf, booking, status..."
            className="mt-2 h-11 w-full rounded-2xl border border-white/10 bg-slate-950/80 px-4 text-sm font-semibold text-white outline-none placeholder:text-slate-600 focus:border-emerald-400/60"
          />
        </label>
        <label className="block">
          <span className="text-xs font-black uppercase tracking-[0.18em] text-slate-500">Status</span>
          <select
            value={status}
            onChange={(event) => onStatusChange(event.target.value)}
            className="mt-2 h-11 w-full rounded-2xl border border-white/10 bg-slate-950/80 px-4 pr-8 text-sm font-black text-white outline-none focus:border-emerald-400/60"
          >
            {statuses.map((item) => (
              <option key={item} value={item}>
                {item.replaceAll("_", " ")}
              </option>
            ))}
          </select>
        </label>
        <div className="flex flex-wrap gap-2">
          {children}
          <AdminActionButton disabled={loading} onClick={onRefresh} label={loading ? "Refreshing..." : "Refresh"} />
        </div>
      </div>
    </section>
  );
}

type AdminDataTableProps = {
  title: string;
  description?: string;
  rows: AdminTableRow[];
  loading?: boolean;
  emptyLabel?: string;
  actionBusyLabel?: string;
  onActionStart?: (label: string) => void;
  onActionDone?: (label: string) => void;
  onActionError?: (label: string, error: unknown) => void;
};

export function AdminDataTable({
  title,
  description,
  rows,
  loading,
  emptyLabel = "No records found for this view.",
  actionBusyLabel,
  onActionStart,
  onActionDone,
  onActionError,
}: AdminDataTableProps) {
  return (
    <section className="overflow-hidden rounded-3xl border border-white/10 bg-slate-900/75 shadow-2xl">
      <div className="flex flex-col gap-2 border-b border-white/10 px-5 py-4 md:flex-row md:items-end md:justify-between">
        <div>
          <h2 className="text-xl font-black text-white">{title}</h2>
          {description ? <p className="mt-1 text-sm text-slate-400">{description}</p> : null}
        </div>
        <p className="text-xs font-black uppercase tracking-[0.18em] text-slate-500">
          {loading ? "Loading" : `${rows.length} records`}
        </p>
      </div>
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-white/10">
          <thead className="bg-slate-950/60">
            <tr>
              <AdminTh>Record</AdminTh>
              <AdminTh>Context</AdminTh>
              <AdminTh>Amount</AdminTh>
              <AdminTh>Status</AdminTh>
              <AdminTh>Updated</AdminTh>
              <AdminTh>Actions</AdminTh>
            </tr>
          </thead>
          <tbody className="divide-y divide-white/10">
            {loading ? (
              <tr>
                <td colSpan={6} className="px-5 py-10 text-center text-sm font-semibold text-slate-400">
                  Loading admin records...
                </td>
              </tr>
            ) : rows.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-5 py-10">
                  <AdminEmptyState title={emptyLabel} detail="Try clearing filters or refresh the module data." />
                </td>
              </tr>
            ) : (
              rows.map((row) => (
                <tr key={`${row.id}-${row.title}`} className="bg-slate-900/35 align-top transition hover:bg-emerald-500/[0.04]">
                  <td className="min-w-[280px] px-5 py-4">
                    <p className="font-black text-white">{row.title}</p>
                    {row.subtitle ? <p className="mt-1 text-sm text-slate-400">{row.subtitle}</p> : null}
                    <p className="mt-2 text-xs font-black uppercase tracking-[0.16em] text-slate-600">ID {row.id}</p>
                  </td>
                  <td className="min-w-[260px] px-5 py-4">
                    <div className="flex flex-wrap gap-2">
                      {(row.meta || []).map((item) => (
                        <span key={item} className="rounded-full border border-white/10 bg-slate-950/70 px-3 py-1 text-xs font-semibold text-slate-300">
                          {item}
                        </span>
                      ))}
                    </div>
                    {row.priority ? <div className="mt-2"><AdminStatusBadge value={row.priority} /></div> : null}
                  </td>
                  <td className="whitespace-nowrap px-5 py-4 text-sm font-black text-emerald-300">{row.amount ?? "-"}</td>
                  <td className="whitespace-nowrap px-5 py-4"><AdminStatusBadge value={row.status} /></td>
                  <td className="whitespace-nowrap px-5 py-4 text-sm text-slate-400">{formatAdminDate(row.updatedAt)}</td>
                  <td className="min-w-[220px] px-5 py-4">
                    <div className="flex flex-wrap gap-2">
                      {(row.actions || []).map((action) => (
                        <AdminActionButton
                          key={`${row.id}-${action.label}`}
                          label={action.label}
                          intent={action.intent}
                          disabled={actionBusyLabel === `${row.id}:${action.label}`}
                          onClick={async () => {
                            const key = `${row.id}:${action.label}`;
                            onActionStart?.(key);
                            try {
                              await action.execute();
                              onActionDone?.(key);
                            } catch (error) {
                              onActionError?.(key, error);
                            }
                          }}
                        />
                      ))}
                      {(!row.actions || row.actions.length === 0) ? <span className="text-xs font-semibold text-slate-500">No quick actions</span> : null}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function AdminTh({ children }: { children: ReactNode }) {
  return <th className="px-5 py-4 text-left text-[11px] font-black uppercase tracking-[0.2em] text-slate-500">{children}</th>;
}

export function AdminActionButton({
  label,
  onClick,
  intent = "neutral",
  disabled,
}: {
  label: string;
  onClick: () => void | Promise<void>;
  intent?: "primary" | "neutral" | "danger";
  disabled?: boolean;
}) {
  const className = {
    primary: "border-emerald-500/30 bg-emerald-500/15 text-emerald-200 hover:bg-emerald-500/25",
    neutral: "border-white/10 bg-slate-950/70 text-slate-200 hover:bg-white/10",
    danger: "border-rose-500/30 bg-rose-500/10 text-rose-200 hover:bg-rose-500/20",
  }[intent];

  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      className={`rounded-xl border px-3 py-2 text-xs font-black transition disabled:cursor-not-allowed disabled:opacity-50 ${className}`}
    >
      {label}
    </button>
  );
}

export function AdminEmptyState({ title, detail }: { title: string; detail?: string }) {
  return (
    <div className="rounded-3xl border border-dashed border-white/10 bg-slate-950/40 px-5 py-10 text-center">
      <p className="text-base font-black text-white">{title}</p>
      {detail ? <p className="mt-2 text-sm text-slate-500">{detail}</p> : null}
    </div>
  );
}

export function AdminErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="rounded-3xl border border-rose-500/20 bg-rose-500/10 p-5 text-rose-100">
      <p className="font-black">Admin module could not load</p>
      <p className="mt-2 text-sm leading-6 text-rose-200/80">{message}</p>
      {onRetry ? <AdminActionButton label="Retry" intent="danger" onClick={onRetry} /> : null}
    </div>
  );
}

export function formatMoney(value?: number | null) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "-";
  }
  return new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 }).format(value);
}

export function formatAdminDate(value?: string) {
  if (!value) {
    return "-";
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("en-IN", { dateStyle: "medium", timeStyle: "short" }).format(parsed);
}
