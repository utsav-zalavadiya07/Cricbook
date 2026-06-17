"use client";

import { useEffect, useState } from "react";
import { getOpenTrustReports, getTrustAnalytics, moderateTrustReport, TrustAnalytics, TrustReport, TrustReportStatus } from "@/modules/trust/services/trustService";
import { useToast } from "@/modules/ui/ToastProvider";

const reportStatuses: TrustReportStatus[] = ["REVIEWED", "DISMISSED", "ACTIONED"];

export default function AdminTrustPage() {
  const { toast } = useToast();
  const [analytics, setAnalytics] = useState<TrustAnalytics | null>(null);
  const [reports, setReports] = useState<TrustReport[]>([]);
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    try {
      const [analyticsRes, reportsRes] = await Promise.all([getTrustAnalytics(), getOpenTrustReports()]);
      setAnalytics(analyticsRes.data);
      setReports(reportsRes.data || []);
    } catch (error: unknown) {
      toast(apiMessage(error, "Could not load trust moderation."), "error");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const moderate = async (reportId: number, status: TrustReportStatus, adjustment?: number) => {
    try {
      await moderateTrustReport(reportId, { status, scoreAdjustment: adjustment, reason: "Admin trust moderation review." });
      toast("Trust report updated.", "success");
      await load();
    } catch (error: unknown) {
      toast(apiMessage(error, "Could not moderate report."), "error");
    }
  };

  return (
    <main className="theme-page-admin min-h-screen px-4 py-8 sm:px-6 lg:px-10">
      <div className="mx-auto max-w-7xl space-y-8">
        <section className="theme-panel-strong rounded-[2rem] p-6">
          <p className="theme-accent-owner text-xs font-black uppercase tracking-[0.34em]">Admin Moderation</p>
          <h1 className="theme-title mt-3 text-4xl font-black tracking-tight sm:text-6xl">Trust command center</h1>
          <p className="theme-muted mt-3 max-w-3xl text-sm leading-6">
            Review sportsmanship reports, monitor risk segments, and keep the marketplace safe without relying on generic 500 errors or silent penalties.
          </p>
        </section>

        <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          <Metric label="Profiles" value={analytics?.totalProfiles ?? 0} />
          <Metric label="Elite" value={analytics?.elitePlayers ?? 0} />
          <Metric label="Trusted" value={analytics?.trustedPlayers ?? 0} />
          <Metric label="Risky" value={analytics?.riskyPlayers ?? 0} />
          <Metric label="Open Reports" value={analytics?.openReports ?? 0} />
        </section>

        <section className="grid gap-6 xl:grid-cols-2">
          <div className="theme-panel rounded-3xl p-6">
            <h2 className="theme-title text-xl font-black">Open Reports</h2>
            <p className="theme-muted mt-1 text-sm">Action reports only after review; score adjustments are explicit.</p>
            <div className="mt-5 space-y-3">
              {loading && <p className="theme-muted py-10 text-center">Loading reports...</p>}
              {!loading && reports.length === 0 && <p className="theme-card theme-muted rounded-2xl border-dashed p-8 text-center">No open trust reports.</p>}
              {reports.map((report) => (
                <div key={report.id} className="theme-panel-soft rounded-2xl p-4">
                  <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                    <div>
                      <p className="theme-title font-bold">{report.reportedUserName || `User #${report.reportedUserId}`}</p>
                      <p className="theme-muted mt-1 text-sm">Reported by {report.reporterName || `User #${report.reporterId}`} · {report.reason.replaceAll("_", " ")}</p>
                      {report.description && <p className="theme-muted mt-2 text-sm">{report.description}</p>}
                    </div>
                    <span className="theme-card rounded-full px-3 py-1 text-xs font-bold">{report.status}</span>
                  </div>
                  <div className="mt-4 flex flex-wrap gap-2">
                    {reportStatuses.map((status) => (
                      <button key={status} onClick={() => moderate(report.id, status, status === "ACTIONED" ? -8 : undefined)} className="theme-card rounded-xl px-3 py-2 text-xs font-black transition hover:border-sky-400/50 hover:text-sky-500">
                        {status}
                      </button>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="space-y-6">
            <Leaderboard title="Most Reliable Players" data={analytics?.mostReliablePlayers || []} />
            <Leaderboard title="Highest Risk Players" data={analytics?.riskiestPlayers || []} />
          </div>
        </section>
      </div>
    </main>
  );
}

function apiMessage(error: unknown, fallback: string) {
  const response = (error as { response?: { data?: { message?: string } } } | null)?.response;
  return response?.data?.message || fallback;
}

function Metric({ label, value }: { label: string; value: number }) {
  return (
    <div className="theme-panel rounded-2xl p-5">
      <p className="theme-soft text-xs font-black uppercase tracking-[0.22em]">{label}</p>
      <p className="theme-title mt-3 text-3xl font-black">{value}</p>
    </div>
  );
}

function Leaderboard({ title, data }: { title: string; data: TrustAnalytics["mostReliablePlayers"] }) {
  return (
    <div className="theme-panel rounded-3xl p-6">
      <h2 className="theme-title text-xl font-black">{title}</h2>
      <div className="mt-5 space-y-3">
        {data.length === 0 && <p className="theme-card theme-muted rounded-2xl border-dashed p-8 text-center">No players yet.</p>}
        {data.map((profile) => (
          <div key={profile.userId} className="theme-panel-soft flex items-center justify-between gap-3 rounded-2xl p-4">
            <div>
              <p className="theme-title font-bold">{profile.playerName || `User #${profile.userId}`}</p>
              <p className="theme-muted text-sm">{profile.tier} · {profile.riskLevel}</p>
            </div>
            <p className="theme-title text-2xl font-black">{profile.score}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
