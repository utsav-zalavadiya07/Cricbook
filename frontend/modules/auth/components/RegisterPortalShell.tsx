"use client";

import React from "react";
import type { LoginRole } from "./LoginForm";

type RegisterPortalShellProps = {
  children: React.ReactNode;
  portalRole: LoginRole;
};

const ownerStats = [
  { label: "Bookings", value: "24/7", tone: "bg-sky-500/20 text-sky-300", icon: "CAL" },
  { label: "Analytics", value: "Real-time", tone: "bg-emerald-500/20 text-emerald-300", icon: "UP" },
  { label: "Payouts", value: "Faster", tone: "bg-violet-500/20 text-violet-300", icon: "PAY" },
  { label: "Secure", value: "100%", tone: "bg-amber-500/20 text-amber-300", icon: "OK" },
];

const playerStats = [
  { label: "Search", value: "Instant", tone: "bg-emerald-500/20 text-emerald-300", icon: "SR" },
  { label: "Booking", value: "3-step", tone: "bg-sky-500/20 text-sky-300", icon: "BK" },
  { label: "Teams", value: "Invite", tone: "bg-violet-500/20 text-violet-300", icon: "TM" },
  { label: "Rewards", value: "Earn", tone: "bg-amber-500/20 text-amber-300", icon: "RW" },
];

export default function RegisterPortalShell({ children, portalRole }: RegisterPortalShellProps) {
  const isOwner = portalRole === "OWNER";
  const stats = isOwner ? ownerStats : playerStats;

  return (
    <main className={`auth-page-backdrop ${isOwner ? "auth-page-owner" : "auth-page-player"} relative min-h-[calc(100vh-84px)] overflow-hidden px-4 py-4 sm:px-6 lg:px-8`}>
      <div className="pointer-events-none absolute inset-0 overflow-hidden">
        <div className={`absolute inset-0 ${isOwner ? "bg-[radial-gradient(circle_at_18%_14%,rgba(14,165,233,0.18),transparent_28%),radial-gradient(circle_at_80%_18%,rgba(37,99,235,0.2),transparent_26%)]" : "bg-[radial-gradient(circle_at_18%_14%,rgba(16,185,129,0.16),transparent_28%),radial-gradient(circle_at_80%_18%,rgba(45,212,191,0.13),transparent_26%)]"}`} />
        <div className="absolute left-1/2 top-20 h-[34rem] w-[34rem] -translate-x-1/2 rounded-full border border-white/5" />
        <div className={`absolute bottom-[-14rem] left-[-7rem] h-[30rem] w-[30rem] rounded-full ${isOwner ? "bg-sky-500/10" : "bg-emerald-500/10"} blur-3xl`} />
      </div>

      <div className="relative mx-auto grid w-full max-w-[84rem] items-center gap-5 lg:grid-cols-[1.04fr_0.96fr]">
        <section className="hidden lg:block">
          <div className={`inline-flex items-center gap-2 rounded-xl border px-3 py-1.5 text-[11px] font-black uppercase tracking-[0.2em] ${isOwner ? "border-sky-400/25 bg-sky-500/10 text-sky-300" : "border-emerald-400/25 bg-emerald-500/10 text-emerald-300"}`}>
            <span className={`inline-flex h-6 w-6 items-center justify-center rounded-lg text-[10px] ${isOwner ? "bg-sky-500/20" : "bg-emerald-500/20"}`}>SEC</span>
            {isOwner ? "Secure • Reliable • Built for turf owners" : "Secure • Fast • Built for players"}
          </div>

          <h1 className="auth-copy-primary mt-4 max-w-3xl text-4xl font-black leading-[1.02] tracking-[-0.055em] xl:text-5xl">
            {isOwner ? (
              <>
                Your turf business. <br />
                Fully <span className="text-sky-400">in control.</span>
              </>
            ) : (
              <>
                Your cricket plans. <br />
                Ready <span className="text-emerald-300">to play.</span>
              </>
            )}
          </h1>
          <p className="auth-copy-muted mt-3 max-w-2xl text-base leading-7">
            {isOwner
              ? "Manage availability, bookings, payouts, pricing, and reviews — all from one powerful dashboard."
              : "Find turfs, book slots, create teams, join tournaments, and keep your cricket activity in one place."}
          </p>

          <div className="mt-5 grid max-w-3xl grid-cols-4 gap-3">
            {stats.map((item, index) => (
              <div key={item.label} className={`flex items-center gap-2.5 ${index > 0 ? "border-l border-white/15 pl-3" : ""}`}>
                <span className={`inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl text-[10px] font-black ${item.tone}`}>
                  {item.icon}
                </span>
                <span>
                  <span className="auth-copy-primary block text-sm font-black">{item.value}</span>
                  <span className="auth-copy-muted block text-xs">{item.label}</span>
                </span>
              </div>
            ))}
          </div>

          <div className="auth-register-dashboard mt-5 overflow-hidden rounded-[1.5rem] border border-sky-400/20 bg-slate-950/60 shadow-2xl shadow-sky-950/40">
            <div className="grid grid-cols-[3.75rem_1fr]">
              <div className="border-r border-white/10 bg-slate-950/55 p-3">
                <div className={`mb-4 flex h-8 w-8 items-center justify-center rounded-xl text-xs font-black text-white ${isOwner ? "bg-sky-500" : "bg-emerald-500"}`}>CB</div>
                {["👤", "✉", "💳", "📊", "⚙"].map((item) => (
                  <div key={item} className="mb-2.5 flex h-8 w-8 items-center justify-center rounded-xl bg-white/5 text-xs text-slate-300">{item}</div>
                ))}
              </div>
              <div className="p-4">
                <div className="mb-3 flex items-center justify-between">
                  <h2 className="auth-copy-primary text-base font-black">{isOwner ? "Dashboard" : "Player Hub"}</h2>
                  <span className="auth-copy-soft text-xs">LIVE</span>
                </div>
                <div className="grid grid-cols-4 gap-2.5">
                  {[
                    ["Today's Bookings", "32", "↗ 12%"],
                    ["Revenue", "₹48,250", "↗ 18%"],
                    ["Occupancy", "78%", "↗ 8%"],
                    ["Rating", "4.8 ★", "Trusted"],
                  ].map(([label, value, meta]) => (
                    <div key={label} className="rounded-xl border border-white/10 bg-white/[0.04] p-2.5">
                      <p className="auth-copy-soft text-[10px]">{label}</p>
                      <p className="auth-copy-primary mt-1.5 text-base font-black">{value}</p>
                      <p className="mt-1 text-[10px] text-emerald-300">{meta}</p>
                    </div>
                  ))}
                </div>
                <div className="mt-3 rounded-2xl border border-white/10 bg-white/[0.04] p-3">
                  <div className="mb-2 flex items-center justify-between">
                    <p className="auth-copy-primary text-sm font-black">Bookings Overview</p>
                    <span className="rounded-lg bg-white/5 px-2 py-1 text-[10px] text-slate-300">This Week</span>
                  </div>
                  <div className="auth-register-chart" />
                </div>
                <div className="mt-3 grid grid-cols-[0.85fr_1.15fr] gap-3">
                  <div className="auth-register-turf" aria-hidden="true" />
                  <div className="rounded-2xl border border-white/10 bg-white/[0.04] p-3">
                    <p className="auth-copy-primary text-sm font-black">Recent Bookings</p>
                    {["Aryan Sharma", "Rahul Verma", "Sneha Kapoor"].map((name, index) => (
                      <div key={name} className="mt-2 flex items-center justify-between gap-2">
                        <span className="flex items-center gap-2">
                          <span className="h-7 w-7 rounded-full bg-sky-400/25" />
                          <span>
                            <span className="auth-copy-primary block text-xs font-bold">{name}</span>
                            <span className="auth-copy-soft block text-[10px]">Box {index + 1} • 7:00 PM</span>
                          </span>
                        </span>
                        <span className={`rounded-lg px-2 py-1 text-[10px] font-bold ${index === 1 ? "bg-amber-500/20 text-amber-300" : "bg-emerald-500/20 text-emerald-300"}`}>
                          {index === 1 ? "Pending" : "Confirmed"}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div className="mt-4 flex items-center justify-between rounded-2xl border border-white/10 bg-white/[0.04] p-4">
            <div className="flex items-center gap-4">
              <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-emerald-500/15 text-xs font-black text-emerald-300">SEC</span>
              <span>
                <span className="auth-copy-primary block font-black">Enterprise-grade security</span>
                <span className="auth-copy-muted text-sm">Your data is protected with industry-leading encryption and compliance standards.</span>
              </span>
            </div>
            <div className="hidden items-center gap-4 xl:flex">
              {["ISO 27001", "GDPR", "SOC 2"].map((badge) => (
                <span key={badge} className="rounded-2xl border border-white/10 px-4 py-2 text-center text-xs font-black text-slate-200">{badge}</span>
              ))}
            </div>
          </div>
        </section>

        <section className="flex w-full items-center justify-center">
          {children}
        </section>
      </div>
    </main>
  );
}
