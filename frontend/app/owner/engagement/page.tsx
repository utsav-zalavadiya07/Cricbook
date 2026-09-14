"use client";
import { TileInfo } from "@/components/ui";

import { useEffect, useState } from "react";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import { useI18n } from "@/modules/i18n/I18nProvider";
import { getOwnerGamificationAnalytics } from "@/modules/gamification/services/gamificationService";
import { GamificationAnalytics } from "@/modules/gamification/types/gamification.types";

export default function OwnerEngagementPage() {
  const { t } = useI18n();
  const [analytics, setAnalytics] = useState<GamificationAnalytics | null>(null);
  const [status, setStatus] = useState("");

  useEffect(() => {
    const timer = window.setTimeout(async () => {
      try {
        const res = await getOwnerGamificationAnalytics();
        setAnalytics(res.data);
      } catch (error: unknown) {
        setStatus(errorMessage(error, "Unable to load engagement analytics."));
      }
    }, 0);
    return () => window.clearTimeout(timer);
  }, []);

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <section className="theme-panel rounded-[2rem] p-6 sm:p-8">
          <p className="theme-accent-owner text-xs font-black uppercase tracking-[0.34em]">{t("gamification.ownerEpic")}</p>
          <h1 className="theme-title mt-3 text-3xl font-black sm:text-5xl">{t("gamification.ownerTitle")}
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Page information" focusable={false}>{t("gamification.ownerSubtitle")}</TileInfo>
              </span></h1>
        </section>
        {status && <div className="theme-panel-soft rounded-2xl p-4 text-sm font-semibold text-sky-600 dark:text-sky-200">{status}</div>}
        <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <Metric label={t("gamification.metric.activePlayers")} value={analytics?.activePlayers ?? 0} />
          <Metric label={t("gamification.metric.completedChallenges")} value={analytics?.completedChallenges ?? 0} />
          <Metric label={t("gamification.metric.availableRewards")} value={analytics?.availableRewards ?? 0} />
          <Metric label={t("gamification.metric.completion")} value={`${Math.round(analytics?.challengeCompletionRate || 0)}%`} />
        </section>
        <section className="grid gap-6 xl:grid-cols-2">
          <Panel title={t("gamification.topPlayers")} body={analytics?.insight || t("common.loading")}>
            <div className="space-y-3">
              {analytics?.topPlayers.map((entry) => (
                <div key={entry.userId} className="theme-panel-soft flex items-center justify-between rounded-2xl p-4">
                  <p className="theme-title font-black">#{entry.rank} {entry.playerName}</p>
                  <p className="theme-muted text-sm">Level {entry.level} - {entry.score} XP</p>
                </div>
              ))}
            </div>
          </Panel>
          <Panel title={t("gamification.teamCompetitions")} body={t("gamification.teamCompetitionsBody")}>
            <div className="space-y-3">
              {analytics?.teamCompetitions.map((team) => (
                <div key={team.teamId} className="theme-panel-soft flex items-center justify-between rounded-2xl p-4">
                  <p className="theme-title font-black">#{team.rank} {team.teamName}</p>
                  <p className="theme-muted text-sm">{team.points} pts</p>
                </div>
              ))}
            </div>
          </Panel>
        </section>
      </div>
    </DashboardLayout>
  );
}

function Panel({ title, body, children }: { title: string; body: string; children: React.ReactNode }) {
  return <section className="theme-panel rounded-3xl p-6"><h2 className="theme-title text-xl font-black">{title}</h2><p className="theme-muted mt-1 text-sm">{body}</p><div className="mt-5">{children}</div></section>;
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return <div className="theme-panel rounded-3xl p-5"><p className="theme-soft text-xs font-bold uppercase tracking-[0.24em]">{label}</p><p className="theme-title mt-4 text-3xl font-black">{value}</p></div>;
}

function errorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { message?: string; error?: string } } };
  return apiError.response?.data?.message || apiError.response?.data?.error || fallback;
}
