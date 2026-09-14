"use client";

import Link from "next/link";
import { TileInfo } from "@/components/ui";

export default function CareerInsightsPage() {
  const statistics = {
    matchesPlayed: 48,
    winRate: "72.5%",
    runsScored: 942,
    wicketsTaken: 31,
    favoritePitch: "Artificial Turf (Fast pace)",
    preferredHour: "6:00 PM - 8:00 PM",
    staminaScore: "88/100",
    battingStrikeRate: "164.2",
  };

  return (
    <main className="theme-page-player min-h-screen px-4 py-8 sm:px-8">
      <div className="mx-auto max-w-5xl space-y-8">
        <div className="flex flex-col gap-4 border-b border-white/10 pb-5 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h1 className="theme-title text-3xl font-black tracking-tight">Career Insights and Analytics
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Career Insights and Analytics information" focusable={false}>Player profile signals, performance trends, and slot scheduling recommendations.</TileInfo>
              </span></h1>
          </div>
          <Link href="/personalized" className="theme-card w-fit rounded-xl px-4 py-2 text-xs font-semibold theme-muted transition hover:border-emerald-400/40 hover:text-current">
            Back to Personalized Feed
          </Link>
        </div>

        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          <Metric label="Total Matches Played" value={statistics.matchesPlayed} body="15% booking growth this month" tone="emerald" />
          <Metric label="Match Win Ratio" value={statistics.winRate} body="Ranked top 5% in your local sector" tone="sky" />
          <Metric label="AI Calculated Stamina Rating" value={statistics.staminaScore} body="Optimal slot duration: 90 mins" tone="emerald" />
        </div>

        <div className="grid gap-6 md:grid-cols-2">
          <section className="theme-panel rounded-[24px] p-6">
            <h3 className="theme-title border-b border-white/10 pb-3 text-lg font-bold">Playing Vector Profiles</h3>
            <div className="mt-5 space-y-5">
              <div>
                <span className="theme-muted text-xs">Batting Strike Rate</span>
                <div className="mt-2 flex items-center justify-between gap-4">
                  <div className="h-2 w-full overflow-hidden rounded-full bg-slate-200 dark:bg-slate-800">
                    <div className="h-full rounded-full bg-sky-500" style={{ width: "82%" }} />
                  </div>
                  <span className="theme-title text-sm font-bold">{statistics.battingStrikeRate}</span>
                </div>
              </div>

              <Detail label="Favorite Pitch Condition" value={statistics.favoritePitch} accent />
              <Detail label="Preferred Slot Hours" value={statistics.preferredHour} />
              <Detail label="Runs Scored and Wickets" value={`${statistics.runsScored} Runs / ${statistics.wicketsTaken} Wickets`} />
            </div>
          </section>

          <section className="theme-panel rounded-[24px] p-6">
            <h3 className="theme-title border-b border-white/10 pb-3 text-lg font-bold">AI Training Suggestions</h3>
            <ul className="mt-5 space-y-4 text-sm">
              <Suggestion title="Focus on off-peak hours" body="Your win rate drops slightly in early morning matches. Schedule evening matches to stay at peak performance." />
              <Suggestion title="Try faster pitch type" body="Your batting strike rate improves when playing on artificial pace turf instead of slower clay composite." />
              <Suggestion title="Recommended slot spacing" body="Maintain 3 days rest between games to preserve your calculated stamina coefficient." />
            </ul>
          </section>
        </div>
      </div>
    </main>
  );
}

function Metric({ label, value, body, tone }: { label: string; value: string | number; body: string; tone: "emerald" | "sky" }) {
  return (
    <div className="theme-panel rounded-[20px] p-6">
      <div className="flex min-w-0 items-start justify-between gap-2">
        <span className="theme-muted min-w-0 break-words text-xs font-semibold uppercase leading-5 tracking-wider">{label}</span>
        <TileInfo label={`${label} information`}>{body}</TileInfo>
      </div>
      <p className={`mt-2 text-4xl font-black ${tone === "emerald" ? "text-emerald-700 dark:text-emerald-300" : "text-sky-700 dark:text-sky-300"}`}>{value}</p>
    </div>
  );
}

function Detail({ label, value, accent = false }: { label: string; value: string; accent?: boolean }) {
  return (
    <div>
      <span className="theme-muted text-xs">{label}</span>
      <p className={`mt-1 text-sm font-semibold ${accent ? "text-emerald-700 dark:text-emerald-300" : "theme-title"}`}>{value}</p>
    </div>
  );
}

function Suggestion({ title, body }: { title: string; body: string }) {
  return (
    <li className="flex items-start gap-3">
      <span className="mt-1 h-2 w-2 rounded-full bg-emerald-500" />
      <div>
        <p className="theme-title font-semibold">{title}</p>
        <p className="theme-muted mt-0.5 text-xs leading-5">{body}</p>
      </div>
    </li>
  );
}
