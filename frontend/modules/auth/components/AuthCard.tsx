"use client";

import React from "react";
import { useI18n } from "@/modules/i18n/I18nProvider";
import type { LoginRole } from "./LoginForm";

interface AuthCardProps {
  children: React.ReactNode;
  title: string;
  subtitle?: string;
  portalRole?: LoginRole;
}

export default function AuthCard({ children, title, subtitle, portalRole = "USER" }: AuthCardProps) {
  const { t, translateText } = useI18n();
  const isOwner = portalRole === "OWNER";
  const accentText = isOwner ? "text-sky-400" : "text-emerald-300";
  const accentSoftText = isOwner ? "text-sky-300" : "text-emerald-300";
  const badgeClass = isOwner
    ? "bg-gradient-to-br from-sky-300 via-sky-500 to-blue-700 shadow-sky-500/25"
    : "bg-gradient-to-br from-emerald-300 via-emerald-500 to-teal-700 shadow-emerald-500/25";
  const portalLabel = isOwner ? t("portal.owner") : t("portal.player");
  const formSubtitle = isOwner ? "Manage turfs, slots, pricing, and bookings." : t("auth.appTagline");

  const showcaseFeatures = isOwner
    ? [
        ["Live Slot Control", "Open, block, and monitor booking slots in real time."],
        ["Revenue Operations", "Track bookings, payouts, invoices, reviews, and cancellations."],
        ["Pricing Intelligence", "Adjust peak rates, offers, and occupancy from one dashboard."],
      ]
    : [
        ["Live Slot Availability", "Check real-time availability and book instantly."],
        ["Create and Invite Teams", "Invite players, manage matches, and tournaments."],
        ["Track and Personalize", "Track history, rewards, and smart recommendations."],
      ];

  return (
    <div className={`auth-page-backdrop ${isOwner ? "auth-page-owner" : "auth-page-player"} relative flex min-h-[calc(100vh-84px)] items-center justify-center overflow-hidden px-4 py-4 sm:px-6 lg:px-8`}>
      <div className="pointer-events-none absolute inset-0 overflow-hidden">
        <div className={`absolute inset-0 ${isOwner ? "bg-[radial-gradient(circle_at_16%_18%,rgba(30,136,229,0.2),transparent_28%),radial-gradient(circle_at_82%_22%,rgba(14,165,233,0.18),transparent_26%),linear-gradient(135deg,rgba(2,6,23,0.72),rgba(12,23,39,0.34)_45%,rgba(10,43,77,0.22))]" : "bg-[radial-gradient(circle_at_16%_18%,rgba(0,200,83,0.18),transparent_28%),radial-gradient(circle_at_82%_22%,rgba(30,136,229,0.14),transparent_26%),linear-gradient(135deg,rgba(2,6,23,0.72),rgba(3,31,39,0.34)_45%,rgba(0,92,70,0.18))]"}`} />
        <div className={`absolute left-1/2 top-10 h-[34rem] w-[34rem] -translate-x-1/2 rounded-full border ${isOwner ? "border-sky-300/10" : "border-emerald-300/10"}`} />
        <div className={`absolute bottom-[-18rem] left-[-10rem] h-[34rem] w-[34rem] rounded-full ${isOwner ? "bg-sky-500/10" : "bg-emerald-500/10"} blur-3xl`} />
        <div className="absolute right-[-12rem] top-24 h-[30rem] w-[30rem] rounded-full bg-sky-500/10 blur-3xl" />
      </div>

      <div className="relative grid w-full max-w-[88rem] items-center gap-5 lg:grid-cols-[1.36fr_0.86fr]">
        <aside className="auth-showcase-card hidden min-h-[500px] overflow-hidden rounded-[1.75rem] p-6 backdrop-blur-2xl xl:p-7 lg:block">
          <div className="mb-6 flex items-center gap-3.5" data-i18n-skip="true">
            <div className={`inline-flex h-14 w-14 items-center justify-center rounded-2xl shadow-xl ${badgeClass}`}>
              <span className="text-xl font-black text-[#f8fafc]">CB</span>
            </div>
            <div>
              <h1 className="auth-copy-primary text-2xl font-black tracking-tight">
                Cric<span className={accentText}>book</span>
              </h1>
              <p className="auth-copy-soft mt-1 text-xs font-bold uppercase tracking-[0.34em]">{portalLabel}</p>
            </div>
          </div>

          <p className={`flex items-center gap-2 text-[11px] font-black uppercase tracking-[0.28em] ${accentSoftText}`}>
            <span className="inline-flex h-5 w-5 items-center justify-center rounded-md border border-current/40 text-[10px]">SC</span>
            {t("auth.secureAccess")}
          </p>
          <h2 className="auth-copy-primary mt-3 max-w-2xl text-4xl font-black leading-[1.02] tracking-[-0.055em] xl:text-5xl">
            {isOwner ? (
              <>
                Your turf business <span className={accentText}>control room.</span>
              </>
            ) : (
              <>
                Your cricket turf <span className={accentText}>command center.</span>
              </>
            )}
          </h2>
          <p className="auth-copy-muted mt-4 max-w-xl text-base leading-7">
            {isOwner
              ? "Control availability, bookings, payouts, and reviews from one premium turf operations hub."
              : "Book, manage and play on your favorite turfs with ease. Fast booking, live availability and seamless experience."}
          </p>

          <div className="mt-4 grid gap-3 xl:grid-cols-[1.18fr_0.82fr]">
            <div className="auth-visual-frame">
              <div className="auth-stadium-scene auth-stadium-scene-compact">
                <div className="auth-light auth-light-left">
                  <span />
                  <span />
                  <span />
                  <span />
                  <span />
                  <span />
                </div>
                <div className="auth-light auth-light-center">
                  <span />
                  <span />
                  <span />
                  <span />
                  <span />
                  <span />
                </div>
                <div className="auth-light auth-light-right">
                  <span />
                  <span />
                  <span />
                  <span />
                  <span />
                  <span />
                </div>
                <div className="auth-turf-field">
                  <div className="auth-turf-stripes" />
                  <div className="auth-pitch-strip">
                    <span className="auth-stumps auth-stumps-top" />
                    <span className="auth-stumps auth-stumps-bottom" />
                  </div>
                  <span className="auth-cricket-ball" />
                  <span className="auth-boundary auth-boundary-one" />
                  <span className="auth-boundary auth-boundary-two" />
                </div>
              </div>
            </div>

            <div className="grid gap-2.5">
              {showcaseFeatures.map(([featureTitle, body], index) => (
                <div key={featureTitle} className="auth-feature-row">
                  <span className={`auth-feature-icon ${isOwner ? "auth-feature-icon-owner" : ""}`}>{["CAL", "PAY", "BI"][index]}</span>
                  <div>
                    <p className="auth-copy-primary text-sm font-black leading-5">{featureTitle}</p>
                    <p className="auth-copy-muted mt-0.5 text-xs font-semibold leading-5">{body}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </aside>

        <section className="flex w-full items-center">
          <div className="w-full">
            <div className="mb-5 text-center lg:hidden">
              <div className={`mb-3 inline-flex h-14 w-14 items-center justify-center rounded-2xl shadow-lg ${isOwner ? "bg-gradient-to-br from-sky-400 to-blue-700" : "bg-gradient-to-br from-emerald-400 to-teal-600"}`} data-i18n-skip="true">
                <span className="text-xl font-bold text-[#f8fafc]">CB</span>
              </div>
              <h1 className="theme-title text-3xl font-bold tracking-tight" data-i18n-skip="true">
                Cric<span className={isOwner ? "text-sky-400" : "text-emerald-400"}>book</span>
              </h1>
              <p className="theme-muted mt-1 text-sm">{formSubtitle}</p>
            </div>

            <div className="auth-form-card mx-auto max-w-[27rem] overflow-hidden rounded-[1.5rem] backdrop-blur-2xl">
              <div className="auth-card-section border-x-0 border-t-0 px-5 py-3.5 sm:px-6">
                <p className={`text-[11px] font-black uppercase tracking-[0.24em] ${accentSoftText}`}>{t("auth.secureAccess")}</p>
                <h2 className="auth-copy-primary mt-1.5 text-[1.65rem] font-black tracking-tight">{translateText(title)}</h2>
                <p className="auth-copy-muted mt-0.5 text-sm leading-5">{subtitle ? translateText(subtitle) : formSubtitle}</p>
              </div>
              <div className="p-5 sm:p-6">{children}</div>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}
