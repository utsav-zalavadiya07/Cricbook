"use client";

import { useEffect, useState } from "react";
import { getAdminCommunityModeration } from "@/modules/social/services/socialService";
import { CommunityAnalytics } from "@/modules/social/types/social.types";

export default function AdminCommunityPage() {
  const [analytics, setAnalytics] = useState<CommunityAnalytics | null>(null);
  const [status, setStatus] = useState("");

  useEffect(() => {
    let active = true;
    getAdminCommunityModeration()
      .then((res) => {
        if (active) setAnalytics(res.data as CommunityAnalytics);
      })
      .catch((error) => {
        if (active) setStatus(errorMessage(error, "Unable to load community moderation metrics."));
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <main className="theme-page-owner min-h-screen p-4 sm:p-6 lg:p-8">
      <div className="mx-auto max-w-6xl space-y-6">
        <section className="theme-panel rounded-[2rem] p-6 sm:p-8">
          <p className="theme-accent-owner text-xs font-black uppercase tracking-[0.34em]">EPIC 29 / Community Moderation</p>
          <h1 className="theme-title mt-3 text-3xl font-black sm:text-5xl">Player finder and community network health.</h1>
          <p className="theme-muted mt-3 max-w-3xl text-sm leading-6">
            Monitor connection growth, recruitment activity, pending applications, and feed volume from one admin surface.
          </p>
        </section>

        {status && <div className="theme-panel rounded-2xl p-4 text-sm font-semibold text-amber-600 dark:text-amber-200">{status}</div>}

        <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Metric label="Discoverable players" value={analytics?.discoverablePlayers ?? 0} />
          <Metric label="Accepted connections" value={analytics?.acceptedConnections ?? 0} />
          <Metric label="Pending connections" value={analytics?.pendingConnections ?? 0} />
          <Metric label="Open recruitments" value={analytics?.openRecruitments ?? 0} />
          <Metric label="Pending applications" value={analytics?.pendingApplications ?? 0} />
          <Metric label="Feed events" value={analytics?.feedEvents ?? 0} />
        </section>

        <section className="theme-panel rounded-3xl p-6">
          <h2 className="theme-title text-xl font-black">Moderation Signal</h2>
          <p className="theme-muted mt-2 text-sm">{analytics?.insight || "No community telemetry available yet."}</p>
        </section>
      </div>
    </main>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="theme-panel rounded-3xl p-5">
      <p className="theme-soft text-xs font-bold uppercase tracking-[0.24em]">{label}</p>
      <p className="theme-title mt-4 text-3xl font-black">{value}</p>
    </div>
  );
}

function errorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { message?: string; error?: string } } };
  return apiError.response?.data?.message || apiError.response?.data?.error || fallback;
}
