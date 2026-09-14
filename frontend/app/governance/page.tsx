"use client";
import { TileInfo } from "@/components/ui";

import { useEffect, useMemo, useState } from "react";
import { getGovernanceFederations, getGovernanceRankings, GovernanceMap } from "@/modules/governance/services/governanceService";

export default function GovernancePage() {
  const [federations, setFederations] = useState<GovernanceMap[]>([]);
  const [rankings, setRankings] = useState<GovernanceMap[]>([]);
  const [sport, setSport] = useState("CRICKET");
  const [loading, setLoading] = useState(true);

  async function loadGovernance(selectedSport = sport) {
    try {
      setLoading(true);
      const [federationRes, rankingRes] = await Promise.all([
        getGovernanceFederations({ sport: selectedSport }),
        getGovernanceRankings({ sport: selectedSport }),
      ]);
      setFederations(federationRes.data);
      setRankings(rankingRes.data);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void loadGovernance(), 0);
    return () => window.clearTimeout(timer);
  }, []);

  const stats = useMemo(
    () => [
      { label: "Federations", value: federations.filter((item) => String(item.bodyType) === "NATIONAL_FEDERATION").length },
      { label: "Associations", value: federations.filter((item) => String(item.bodyType) !== "NATIONAL_FEDERATION").length },
      { label: "Ranked Records", value: rankings.length },
      { label: "Avg Compliance", value: average(federations.map((item) => Number(item.complianceScore || 0))) },
    ],
    [federations, rankings.length]
  );

  async function handleSportChange(value: string) {
    setSport(value);
    await loadGovernance(value);
  }

  return (
    <main className="min-h-screen theme-bg px-4 py-8 text-slate-100 sm:px-6" data-i18n-skip="true">
      <div className="mx-auto max-w-7xl space-y-6">
        <section className="theme-card rounded-[2rem] border p-6 shadow-2xl">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.26em] text-emerald-300">Federation Governance</p>
              <h1 className="mt-3 text-3xl font-black theme-title sm:text-4xl">Sports governance and ranking directory
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Sports governance and ranking directory information" focusable={false}>Track active federations, state and district associations, national rankings, player licensing readiness, and integrity visibility from one public portal.</TileInfo>
              </span></h1>
            </div>
            <select
              value={sport}
              onChange={(event) => void handleSportChange(event.target.value)}
              className="theme-input min-w-44 rounded-2xl px-4 py-3 text-sm font-bold outline-none"
            >
              {["CRICKET", "FOOTBALL", "BADMINTON", "TENNIS", "BASKETBALL"].map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </select>
          </div>
        </section>

        <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {stats.map((item) => (
            <div key={item.label} className="theme-card rounded-3xl border p-5">
              <p className="text-xs font-black uppercase tracking-[0.18em] theme-soft">{item.label}</p>
              <p className="mt-3 text-3xl font-black theme-title">{item.value}</p>
            </div>
          ))}
        </section>

        {loading ? (
          <section className="theme-card rounded-[2rem] border p-10 text-center text-sm font-bold theme-soft">Loading governance directory...</section>
        ) : (
          <section className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
            <Panel title="Federation hierarchy" subtitle="National, state, district, university, and corporate league bodies.">
              <div className="space-y-3">
                {federations.length ? (
                  federations.map((item) => (
                    <div key={String(item.id)} className="rounded-2xl border border-white/10 bg-white/[0.04] p-4">
                      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                        <div>
                          <p className="font-black theme-title">{String(item.name)}</p>
                          <p className="mt-1 text-xs font-bold uppercase tracking-[0.16em] theme-soft">
                            {String(item.bodyType).replaceAll("_", " ")} - {String(item.region || "National")}
                          </p>
                        </div>
                        <span className="rounded-full border border-emerald-400/20 bg-emerald-400/10 px-3 py-1 text-xs font-black text-emerald-200">
                          {String(item.status)}
                        </span>
                      </div>
                      <div className="mt-3 grid gap-2 text-xs font-semibold theme-soft sm:grid-cols-3">
                        <span>Sport: {String(item.sport)}</span>
                        <span>Compliance: {String(item.complianceScore || 0)}</span>
                        <span>Members: {String(item.affiliatedMembers || 0)}</span>
                      </div>
                    </div>
                  ))
                ) : (
                  <p className="text-sm font-bold theme-soft">No active federation bodies are published yet.</p>
                )}
              </div>
            </Panel>

            <Panel title="National rankings" subtitle="Published ranking records from governance calculations.">
              <div className="space-y-3">
                {rankings.length ? (
                  rankings.slice(0, 12).map((item) => (
                    <div key={String(item.id)} className="flex items-center justify-between gap-3 rounded-2xl border border-white/10 bg-slate-950/40 p-4">
                      <div>
                        <p className="font-black theme-title">#{String(item.rankPosition || "--")} {String(item.entityType)}</p>
                        <p className="mt-1 text-xs font-bold theme-soft">
                          {String(item.rankingType)} - {String(item.category || "OPEN")} - Entity {String(item.entityId)}
                        </p>
                      </div>
                      <span className="text-lg font-black text-emerald-300">{String(item.points || 0)}</span>
                    </div>
                  ))
                ) : (
                  <p className="text-sm font-bold theme-soft">Rankings will appear after federation admins publish records.</p>
                )}
              </div>
            </Panel>
          </section>
        )}
      </div>
    </main>
  );
}

function Panel({ title, subtitle, children }: { title: string; subtitle: string; children: React.ReactNode }) {
  return (
    <section className="theme-card rounded-[2rem] border p-5">
      <p className="text-lg font-black theme-title">{title}</p>
      <p className="mt-1 text-sm theme-soft">{subtitle}</p>
      <div className="mt-5">{children}</div>
    </section>
  );
}

function average(values: number[]) {
  const filtered = values.filter((value) => Number.isFinite(value) && value > 0);
  if (!filtered.length) {
    return 0;
  }
  return Math.round(filtered.reduce((sum, value) => sum + value, 0) / filtered.length);
}
