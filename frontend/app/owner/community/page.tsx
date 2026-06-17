"use client";

import { useEffect, useState } from "react";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import { getMatchmakingAnalytics, MatchmakingAnalytics } from "@/modules/matchmaking/services/matchmakingService";
import { useToast } from "@/modules/ui/ToastProvider";

export default function OwnerCommunityPage() {
  const { toast } = useToast();
  const [analytics, setAnalytics] = useState<MatchmakingAnalytics | null>(null);

  useEffect(() => {
    getMatchmakingAnalytics()
      .then((res) => setAnalytics(res.data))
      .catch((error) => toast(error?.response?.data?.message || "Could not load community activity.", "error"));
  }, [toast]);

  return (
    <DashboardLayout>
      <main className="space-y-6">
        <section className="theme-panel-strong rounded-[2rem] p-6">
          <p className="theme-accent-owner text-xs font-black uppercase tracking-[0.34em]">Community Activity</p>
          <h1 className="theme-title mt-3 text-3xl font-black sm:text-5xl">Open matches filling near your turfs.</h1>
          <p className="theme-muted mt-3 max-w-3xl text-sm leading-6">Track active community demand, fill rates, and match creation momentum.</p>
        </section>
        <section className="grid gap-4 sm:grid-cols-4">
          <Metric label="Open Matches" value={analytics?.openMatches ?? 0} />
          <Metric label="Full Matches" value={analytics?.fullMatches ?? 0} />
          <Metric label="Participants" value={analytics?.totalParticipants ?? 0} />
          <Metric label="Avg Fill" value={`${analytics?.averageFillRate ?? 0}%`} />
        </section>
        <section className="theme-panel rounded-3xl p-6">
          <h2 className="theme-title text-xl font-black">Most Active Matches</h2>
          <div className="mt-5 grid gap-4 lg:grid-cols-2">
            {(analytics?.mostFillableMatches || []).map((match) => (
              <article key={match.id} className="theme-panel-soft rounded-2xl p-4">
                <p className="theme-title font-black">{match.title}</p>
                <p className="theme-muted mt-1 text-sm">{match.city} · {match.matchDate} · {match.joinedPlayers}/{match.maxPlayers} joined</p>
              </article>
            ))}
          </div>
        </section>
      </main>
    </DashboardLayout>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="theme-panel rounded-2xl p-5">
      <p className="theme-soft text-xs font-black uppercase tracking-[0.2em]">{label}</p>
      <p className="theme-title mt-3 text-3xl font-black">{value}</p>
    </div>
  );
}
