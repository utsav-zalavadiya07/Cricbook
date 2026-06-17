"use client";

import { SportsOsMap } from "@/modules/sportsos/services/sportsOsService";

export function MetricCard({ label, value, detail }: { label: string; value: string; detail?: string }) {
  return (
    <div className="theme-card rounded-3xl border p-5 shadow-xl">
      <p className="text-xs font-black uppercase tracking-[0.22em] text-emerald-300">{label}</p>
      <p className="mt-3 text-3xl font-black theme-title">{value}</p>
      {detail ? <p className="mt-2 text-xs font-bold theme-soft">{detail}</p> : null}
    </div>
  );
}

export function Panel({ title, subtitle, children }: { title: string; subtitle?: string; children: React.ReactNode }) {
  return (
    <section className="theme-card rounded-[2rem] border p-5 shadow-xl">
      <div className="mb-4">
        <h2 className="text-xl font-black theme-title">{title}</h2>
        {subtitle ? <p className="mt-1 text-sm theme-soft">{subtitle}</p> : null}
      </div>
      {children}
    </section>
  );
}

export function Field({
  label,
  value,
  onChange,
  placeholder,
  type = "text",
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  type?: string;
}) {
  return (
    <label className="block">
      <span className="text-xs font-black theme-title">{label}</span>
      <input
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        className="mt-2 w-full rounded-2xl border border-white/10 bg-white/[0.05] px-4 py-3 text-sm font-bold theme-title outline-none focus:border-emerald-400"
      />
    </label>
  );
}

export function SelectField({
  label,
  value,
  onChange,
  options,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  options: string[];
}) {
  return (
    <label className="block">
      <span className="text-xs font-black theme-title">{label}</span>
      <select
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-2 w-full rounded-2xl border border-white/10 bg-white/[0.05] px-4 py-3 text-sm font-bold theme-title outline-none focus:border-emerald-400"
      >
        {options.map((option) => (
          <option key={option} value={option}>
            {option.replaceAll("_", " ")}
          </option>
        ))}
      </select>
    </label>
  );
}

export function DataTable({ rows, columns, empty }: { rows: SportsOsMap[]; columns: Array<[string, string]>; empty: string }) {
  return (
    <div className="overflow-hidden rounded-3xl border border-white/10">
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-white/10 text-left text-sm">
          <thead className="bg-white/[0.04] text-xs uppercase tracking-[0.18em] theme-soft">
            <tr>
              {columns.map(([, label]) => (
                <th key={label} className="px-4 py-3 font-black">
                  {label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-white/10">
            {rows.length ? (
              rows.map((row, index) => (
                <tr key={`${String(row.id || row.code || row.name || "row")}-${index}`}>
                  {columns.map(([key]) => (
                    <td key={key} className="max-w-[220px] truncate px-4 py-3 font-bold theme-soft">
                      {formatCell(row[key])}
                    </td>
                  ))}
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={columns.length} className="px-4 py-6 text-center text-sm font-bold theme-soft">
                  {empty}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export function CompactRow({ title, meta, status }: { title: string; meta?: string; status?: string }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/[0.04] p-4">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <p className="font-black theme-title">{title}</p>
        {status ? <span className="w-fit rounded-full bg-emerald-400/15 px-3 py-1 text-xs font-black uppercase tracking-[0.16em] text-emerald-200">{status}</span> : null}
      </div>
      {meta ? <p className="mt-2 text-sm leading-6 theme-soft">{meta}</p> : null}
    </div>
  );
}

export function formatNumber(value: unknown) {
  const number = Number(value || 0);
  if (!Number.isFinite(number)) {
    return "0";
  }
  return new Intl.NumberFormat("en-IN", { maximumFractionDigits: 2 }).format(number);
}

export function errorMessage(error: unknown, fallback: string) {
  if (typeof error === "object" && error && "response" in error) {
    const response = (error as { response?: { data?: { message?: string } } }).response;
    return response?.data?.message || fallback;
  }
  return fallback;
}

function formatCell(value: unknown) {
  if (value === null || value === undefined || value === "") {
    return "--";
  }
  if (typeof value === "number") {
    return formatNumber(value);
  }
  if (typeof value === "boolean") {
    return value ? "Yes" : "No";
  }
  return String(value);
}
