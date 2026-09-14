"use client";
import { TileInfo } from "@/components/ui";

import { useEffect, useState } from "react";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import { getTeamAnalytics, TeamAnalytics } from "@/modules/team/services/teamWorkspaceService";
import { useToast } from "@/modules/ui/ToastProvider";

export default function OwnerTeamsPage() {
  const { toast } = useToast();
  const [analytics, setAnalytics] = useState<TeamAnalytics | null>(null);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      getTeamAnalytics()
        .then((res) => setAnalytics(res.data))
        .catch((error: unknown) => toast(apiMessage(error, "Could not load team analytics."), "error"));
    }, 0);
    return () => window.clearTimeout(timer);
  }, [toast]);

  return (
    <DashboardLayout>
      <main className="space-y-6">
        <section className="theme-panel-strong rounded-[2rem] p-6">
          <p className="theme-accent-owner text-xs font-black uppercase tracking-[0.34em]">Top Teams</p>
          <div className="mt-3">
            <h1 className="theme-title text-3xl font-black sm:text-5xl">Community teams driving repeat bookings.
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Teams information" focusable={false}>Track team activity, availability responses, finance movement, and attendance reliability.</TileInfo>
              </span></h1>
          </div>
        </section>
        <section className="grid gap-4 sm:grid-cols-5">
          <Metric label="Teams" value={analytics?.totalTeams ?? 0} />
          <Metric label="Members" value={analytics?.activeMembers ?? 0} />
          <Metric label="Availability" value={analytics?.availabilityResponses ?? 0} />
          <Metric label="Polls" value={analytics?.polls ?? 0} />
          <Metric label="Attendance" value={`${analytics?.averageAttendanceRate ?? 0}%`} />
        </section>
        <section className="theme-panel rounded-3xl p-6">
          <h2 className="theme-title text-xl font-black">Most Active Teams</h2>
          <div className="mt-5 grid gap-4 lg:grid-cols-3">
            {(analytics?.mostActiveTeams || []).map((team) => (
              <article key={team.teamId} className="theme-panel-soft rounded-2xl p-4">
                <p className="theme-title font-black">{team.teamName}</p>
                <p className="theme-muted mt-1 text-sm">{team.city} · {team.memberCount} players · {team.teamType}</p>
                <p className="theme-muted mt-3 text-xs">Available today {team.availableToday} · Balance ₹{team.teamBalance || 0}</p>
              </article>
            ))}
            {(analytics?.mostActiveTeams || []).length === 0 && <p className="theme-card theme-muted col-span-full rounded-2xl border-dashed p-8 text-center">No team activity yet.</p>}
          </div>
        </section>
      </main>
    </DashboardLayout>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return <div className="theme-panel rounded-2xl p-5"><p className="theme-soft text-xs font-black uppercase tracking-[0.2em]">{label}</p><p className="theme-title mt-3 text-3xl font-black">{value}</p></div>;
}

function apiMessage(error: unknown, fallback: string) {
  const response = (error as { response?: { data?: { message?: string } } } | null)?.response;
  return response?.data?.message || fallback;
}
