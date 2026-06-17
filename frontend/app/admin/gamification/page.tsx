"use client";

import { useEffect, useState } from "react";
import { useI18n } from "@/modules/i18n/I18nProvider";
import { getAdminGamificationAnalytics } from "@/modules/gamification/services/gamificationService";
import { GamificationAnalytics } from "@/modules/gamification/types/gamification.types";

export default function AdminGamificationPage() {
  const { t } = useI18n();
  const [analytics, setAnalytics] = useState<GamificationAnalytics | null>(null);
  const [status, setStatus] = useState("");

  useEffect(() => {
    const timer = window.setTimeout(async () => {
      try {
        const res = await getAdminGamificationAnalytics();
        setAnalytics(res.data);
      } catch (error: unknown) {
        setStatus(errorMessage(error, "Unable to load gamification control."));
      }
    }, 0);
    return () => window.clearTimeout(timer);
  }, []);

  return (
    <main className="theme-page-owner min-h-screen p-4 sm:p-6 lg:p-8">
      <div className="mx-auto max-w-6xl space-y-6">
        <section className="theme-panel rounded-[2rem] p-6 sm:p-8">
          <p className="theme-accent-owner text-xs font-black uppercase tracking-[0.34em]">{t("gamification.adminEpic")}</p>
          <h1 className="theme-title mt-3 text-3xl font-black sm:text-5xl">{t("gamification.adminTitle")}</h1>
          <p className="theme-muted mt-3 max-w-3xl text-sm leading-6">{t("gamification.adminSubtitle")}</p>
        </section>
        {status && <div className="theme-panel-soft rounded-2xl p-4 text-sm font-semibold text-amber-600 dark:text-amber-200">{status}</div>}
        <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Metric label={t("gamification.metric.activePlayers")} value={analytics?.activePlayers ?? 0} />
          <Metric label={t("gamification.metric.completedChallenges")} value={analytics?.completedChallenges ?? 0} />
          <Metric label={t("gamification.metric.availableRewards")} value={analytics?.availableRewards ?? 0} />
          <Metric label={t("gamification.metric.completion")} value={`${Math.round(analytics?.challengeCompletionRate || 0)}%`} />
        </section>
        <section className="theme-panel rounded-3xl p-6">
          <h2 className="theme-title text-xl font-black">{t("gamification.platformLeaderboard")}</h2>
          <p className="theme-muted mt-2 text-sm">{analytics?.insight}</p>
          <div className="mt-5 grid gap-3 sm:grid-cols-2">
            {analytics?.topPlayers.map((entry) => (
              <div key={entry.userId} className="theme-panel-soft rounded-2xl p-4">
                <p className="theme-title font-black">#{entry.rank} {entry.playerName}</p>
                <p className="theme-muted mt-2 text-sm">Level {entry.level} - {entry.score} XP</p>
              </div>
            ))}
          </div>
        </section>
      </div>
    </main>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return <div className="theme-panel rounded-3xl p-5"><p className="theme-soft text-xs font-bold uppercase tracking-[0.24em]">{label}</p><p className="theme-title mt-4 text-3xl font-black">{value}</p></div>;
}

function errorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { message?: string; error?: string } } };
  return apiError.response?.data?.message || apiError.response?.data?.error || fallback;
}
