"use client";

import { useEffect, useState } from "react";
import { getAdminTeamAnalytics, TeamAnalytics } from "@/modules/team/services/teamWorkspaceService";
import { useToast } from "@/modules/ui/ToastProvider";

export default function AdminTeamsPage() {
  const { toast } = useToast();
  const [analytics, setAnalytics] = useState<TeamAnalytics | null>(null);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      getAdminTeamAnalytics()
        .then((res) => setAnalytics(res.data))
        .catch((error: unknown) => toast(apiMessage(error, "Could not load team moderation."), "error"));
    }, 0);
    return () => window.clearTimeout(timer);
  }, [toast]);

  return (
    <main className="theme-page-admin min-h-screen px-4 py-8 sm:px-6 lg:px-10">
      <div className="mx-auto max-w-7xl space-y-6">
        <section className="theme-panel-strong rounded-[2rem] p-6">
          <p className="theme-accent-owner text-xs font-black uppercase tracking-[0.34em]">Team Moderation</p>
          <h1 className="theme-title mt-3 text-3xl font-black sm:text-5xl">Team operating system health.</h1>
          <p className="theme-muted mt-3 max-w-3xl text-sm leading-6">Monitor availability adoption, poll activity, team wallet movement, and attendance quality.</p>
        </section>
        <section className="grid gap-4 sm:grid-cols-5">
          <Metric label="Teams" value={analytics?.totalTeams ?? 0} />
          <Metric label="Members" value={analytics?.activeMembers ?? 0} />
          <Metric label="Responses" value={analytics?.availabilityResponses ?? 0} />
          <Metric label="Balance" value={`₹${analytics?.teamWalletBalance ?? 0}`} />
          <Metric label="Attendance" value={`${analytics?.averageAttendanceRate ?? 0}%`} />
        </section>
        <section className="theme-panel rounded-3xl p-6">
          <h2 className="theme-title text-xl font-black">Teams to Watch</h2>
          <div className="mt-5 grid gap-4 lg:grid-cols-3">
            {(analytics?.mostActiveTeams || []).map((team) => (
              <article key={team.teamId} className="theme-panel-soft rounded-2xl p-4">
                <p className="theme-title font-black">{team.teamName}</p>
                <p className="theme-muted mt-1 text-sm">{team.city} · {team.memberCount} players · {team.visibility}</p>
                <p className="theme-muted mt-3 text-xs">Attendance {team.attendanceRate}% · Polls {team.polls?.length || 0}</p>
              </article>
            ))}
          </div>
        </section>
      </div>
    </main>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return <div className="theme-panel rounded-2xl p-5"><p className="theme-soft text-xs font-black uppercase tracking-[0.2em]">{label}</p><p className="theme-title mt-3 text-3xl font-black">{value}</p></div>;
}

function apiMessage(error: unknown, fallback: string) {
  const response = (error as { response?: { data?: { message?: string } } } | null)?.response;
  return response?.data?.message || fallback;
}
