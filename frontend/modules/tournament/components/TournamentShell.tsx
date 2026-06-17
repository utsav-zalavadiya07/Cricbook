"use client";

import Link from "next/link";
import { ReactNode } from "react";

export const inputClass =
  "w-full rounded-2xl border border-white/10 bg-slate-900/90 px-4 py-3 text-sm text-white outline-none placeholder:text-slate-500";

export function TournamentPageShell({ children }: { children: ReactNode }) {
  return (
    <main className="theme-page-player min-h-screen px-4 py-6 sm:px-6 lg:px-8">
      <div className="w-full space-y-6">{children}</div>
    </main>
  );
}

export function Hero({
  eyebrow,
  title,
  description,
  action,
  stats,
}: {
  eyebrow: string;
  title: string;
  description: string;
  action?: ReactNode;
  stats?: ReactNode;
}) {
  return (
    <section className="rounded-[32px] border border-white/10 bg-slate-950/70 p-6 shadow-2xl shadow-black/30">
      <p className="text-xs font-semibold uppercase tracking-[0.3em] text-sky-300/70">{eyebrow}</p>
      <div className="mt-4 flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
        <div className="max-w-4xl">
          <h1 className="text-3xl font-black tracking-tight text-white sm:text-4xl">{title}</h1>
          <p className="mt-2 text-sm text-slate-300">{description}</p>
          {action ? <div className="mt-5">{action}</div> : null}
        </div>
        {stats}
      </div>
    </section>
  );
}

export function Surface({ title, children, action }: { title: string; children: ReactNode; action?: ReactNode }) {
  return (
    <section className="rounded-[28px] border border-white/10 bg-white/[0.04] p-6">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-xl font-bold text-white">{title}</h2>
        {action}
      </div>
      <div className="mt-4 space-y-3">{children}</div>
    </section>
  );
}

export function Panel({ title, action, children }: { title: string; action?: ReactNode; children: ReactNode }) {
  return (
    <div className="rounded-3xl border border-white/10 bg-white/[0.03] p-5">
      <div className="flex items-center justify-between gap-3">
        <h3 className="text-lg font-bold text-white">{title}</h3>
        {action}
      </div>
      <div className="mt-4 space-y-3">{children}</div>
    </div>
  );
}

export function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="mb-2 block text-sm font-semibold text-slate-200">{label}</span>
      {children}
    </label>
  );
}

export function SmallButton({
  label,
  tone,
  onClick,
  type = "button",
}: {
  label: string;
  tone: "primary" | "secondary" | "danger";
  onClick?: () => void;
  type?: "button" | "submit";
}) {
  const styles =
    tone === "primary"
      ? "border-sky-400/30 bg-sky-500/15 text-sky-200"
      : tone === "danger"
        ? "border-red-500/30 bg-red-500/10 text-red-200"
        : "border-white/10 bg-white/[0.03] text-slate-200";
  return (
    <button type={type} onClick={onClick} className={`rounded-xl border px-3 py-2 text-xs font-semibold ${styles}`}>
      {label}
    </button>
  );
}

export function Stat({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/[0.03] px-4 py-3">
      <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-slate-500">{label}</p>
      <p className="mt-2 text-lg font-bold text-white">{value}</p>
    </div>
  );
}

export function Empty({ text }: { text: string }) {
  return <p className="rounded-2xl border border-dashed border-white/10 p-4 text-center text-sm text-slate-400">{text}</p>;
}

export function TournamentCard({
  href,
  title,
  meta,
  description,
}: {
  href: string;
  title: string;
  meta: string;
  description?: string;
}) {
  return (
    <Link href={href} className="block rounded-3xl border border-white/10 bg-slate-950/60 px-4 py-4 hover:border-sky-400/30">
      <p className="font-semibold text-white">{title}</p>
      <p className="mt-1 text-sm text-slate-400">{meta}</p>
      {description ? <p className="mt-2 line-clamp-2 text-sm text-slate-500">{description}</p> : null}
    </Link>
  );
}

export function PrimaryLink({ href, label }: { href: string; label: string }) {
  return (
    <Link href={href} className="inline-flex rounded-2xl bg-sky-500 px-4 py-3 text-sm font-bold text-white hover:bg-sky-400">
      {label}
    </Link>
  );
}

export function SecondaryLink({ href, label }: { href: string; label: string }) {
  return (
    <Link href={href} className="inline-flex rounded-2xl border border-white/10 bg-white/[0.03] px-4 py-3 text-sm font-semibold text-slate-200">
      {label}
    </Link>
  );
}

export function errorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { error?: string } } };
  return apiError.response?.data?.error || fallback;
}

export function formatMoney(value?: number) {
  return `Rs ${value || 0}`;
}

export function formatTournamentDate(startDate?: string, endDate?: string) {
  if (!startDate) {
    return "Schedule pending";
  }
  return endDate ? `${startDate} to ${endDate}` : startDate;
}
