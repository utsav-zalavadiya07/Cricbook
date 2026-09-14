"use client";

import { useEffect, useMemo, useState } from "react";
import { TileInfo } from "@/components/ui";
import {
  AdminSportConfigPayload,
  AdminSportsDashboard,
  AdminSportsMap,
  createAdminSport,
  getAdminSportsAnalytics,
  getAdminSportsAudit,
  getAdminSportsCatalog,
  getAdminSportsDashboard,
  seedAdminSportsDefaults,
  simulateSportRules,
} from "@/modules/admin/services/adminService";

const defaultDraft: AdminSportConfigPayload = {
  name: "",
  code: "",
  icon: "SP",
  rules: "",
  active: true,
  minPlayers: 2,
  maxPlayers: 12,
  scoringType: "CUSTOM",
  matchType: "CUSTOM",
  rankingType: "CUSTOM",
  teamBased: true,
  tournamentEnabled: true,
  statisticsEnabled: true,
};

export default function AdminSportsPage() {
  const [dashboard, setDashboard] = useState<AdminSportsDashboard | null>(null);
  const [catalog, setCatalog] = useState<AdminSportsMap[]>([]);
  const [analytics, setAnalytics] = useState<AdminSportsMap | null>(null);
  const [audit, setAudit] = useState<AdminSportsMap[]>([]);
  const [simulation, setSimulation] = useState<AdminSportsMap | null>(null);
  const [draft, setDraft] = useState<AdminSportConfigPayload>(defaultDraft);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  async function loadSports() {
    try {
      setLoading(true);
      const [dashboardRes, catalogRes, analyticsRes, auditRes] = await Promise.all([
        getAdminSportsDashboard(),
        getAdminSportsCatalog(),
        getAdminSportsAnalytics(),
        getAdminSportsAudit(),
      ]);
      setDashboard(dashboardRes.data);
      setCatalog(catalogRes.data);
      setAnalytics(analyticsRes.data);
      setAudit(auditRes.data);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void loadSports(), 0);
    return () => window.clearTimeout(timer);
  }, []);

  const metrics = useMemo(
    () => [
      { label: "Configured Sports", value: numberValue(dashboard?.kpis?.configuredSports), detail: "catalog entries" },
      { label: "Active Sports", value: numberValue(dashboard?.kpis?.activeSports), detail: "globally enabled" },
      { label: "Multi-Sport Turfs", value: numberValue(dashboard?.kpis?.multiSportTurfs), detail: "supporting 2+ sports" },
      { label: "Turf Mappings", value: numberValue(dashboard?.kpis?.sportTurfMappings), detail: "sport to venue links" },
      { label: "Team Sports", value: numberValue(dashboard?.kpis?.teamLinkedSports), detail: "team segmentation" },
      { label: "Tournament Sports", value: numberValue(dashboard?.kpis?.tournamentLinkedSports), detail: "event segmentation" },
      { label: "Match Sports", value: numberValue(dashboard?.kpis?.matchLinkedSports), detail: "match engine usage" },
      { label: "Readiness", value: `${numberValue(dashboard?.kpis?.futureSportReadinessScore)}%`, detail: "future sport onboarding" },
    ],
    [dashboard]
  );
  const segmentationRows = useMemo(
    () => (Array.isArray(analytics?.segmentation) ? (analytics.segmentation as Array<Record<string, unknown>>) : []),
    [analytics]
  );

  async function handleSeed() {
    setSaving(true);
    try {
      await seedAdminSportsDefaults();
      await loadSports();
    } finally {
      setSaving(false);
    }
  }

  async function handleCreateSport() {
    if (!draft.name?.trim()) {
      return;
    }
    setSaving(true);
    try {
      await createAdminSport({
        ...draft,
        minPlayers: Number(draft.minPlayers || 1),
        maxPlayers: Number(draft.maxPlayers || draft.minPlayers || 1),
      });
      setDraft(defaultDraft);
      await loadSports();
    } finally {
      setSaving(false);
    }
  }

  async function handleSimulate(code: string) {
    const response = await simulateSportRules({ sportCode: code, playersPerSide: 11 });
    setSimulation(response.data);
  }

  return (
    <main className="min-h-screen bg-slate-950 px-4 py-8 text-white sm:px-8" data-i18n-skip="true">
      <div className="mx-auto max-w-7xl space-y-8">
        <header className="rounded-[2rem] border border-white/10 bg-gradient-to-br from-cyan-500/15 via-slate-900 to-emerald-500/10 p-6 shadow-2xl">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.26em] text-cyan-300">EPIC 24 Multi-Sport Expansion Framework</p>
              <h1 className="mt-3 text-3xl font-black text-white sm:text-4xl">Sports OS Configuration Center</h1>
              <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-300">
                Govern sport catalog, rule templates, scoring models, rankings, turf mappings, analytics segmentation, and future no-code sport onboarding.
              </p>
            </div>
            <div className="flex flex-wrap gap-3">
              <button onClick={() => void handleSeed()} disabled={saving} className="rounded-2xl border border-cyan-300/30 bg-cyan-400/10 px-4 py-3 text-sm font-black text-cyan-100 hover:bg-cyan-400/20 disabled:opacity-60">
                Seed Templates
              </button>
              <button onClick={() => void loadSports()} className="rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-emerald-300">
                Refresh Sports OS
              </button>
            </div>
          </div>
        </header>

        {loading ? (
          <section className="rounded-[2rem] border border-white/10 bg-white/[0.04] p-10 text-center text-sm font-bold text-slate-300">Loading multi-sport control plane...</section>
        ) : (
          <>
            <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              {metrics.map((metric) => (
                <MetricCard key={metric.label} {...metric} />
              ))}
            </section>

            <section className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
              <Panel title="Sport Catalog" subtitle="Active and inactive sports with players, scoring, match, ranking, team, tournament, and statistics configuration.">
                <div className="grid gap-3 md:grid-cols-2">
                  {(catalog.length ? catalog : dashboard?.catalog || []).map((sport) => (
                    <button key={String(sport.id ?? sport.code)} onClick={() => void handleSimulate(String(sport.code))} className="rounded-3xl border border-white/10 bg-slate-900/80 p-4 text-left transition hover:border-cyan-300/40">
                      <div className="flex items-center justify-between gap-3">
                        <div>
                          <p className="text-lg font-black text-white">{String(sport.name || "Sport")}</p>
                          <p className="mt-1 text-xs font-black uppercase tracking-[0.18em] text-cyan-300">{String(sport.code || "CUSTOM")}</p>
                        </div>
                        <span className={`rounded-full px-3 py-1 text-xs font-black ${sport.active ? "bg-emerald-400/15 text-emerald-200" : "bg-rose-400/15 text-rose-200"}`}>
                          {sport.active ? "ACTIVE" : "DISABLED"}
                        </span>
                      </div>
                      <div className="mt-4 grid gap-2 text-xs text-slate-300 sm:grid-cols-2">
                        <span>Players: {String(sport.minPlayers ?? "-")} - {String(sport.maxPlayers ?? "-")}</span>
                        <span>Scoring: {String(sport.scoringType || "CUSTOM")}</span>
                        <span>Match: {String(sport.matchType || "CUSTOM")}</span>
                        <span>Ranking: {String(sport.rankingType || "CUSTOM")}</span>
                      </div>
                    </button>
                  ))}
                </div>
              </Panel>

              <Panel title="Create Future Sport" subtitle="No-code sport onboarding for Padel, Swimming, Skating, or any future sport.">
                <div className="space-y-3">
                  <Field label="Sport Name" value={draft.name || ""} onChange={(value) => setDraft((current) => ({ ...current, name: value }))} placeholder="Padel" />
                  <div className="grid gap-3 sm:grid-cols-2">
                    <Field label="Code" value={draft.code || ""} onChange={(value) => setDraft((current) => ({ ...current, code: value }))} placeholder="PADEL" />
                    <Field label="Icon" value={draft.icon || ""} onChange={(value) => setDraft((current) => ({ ...current, icon: value }))} placeholder="PD" />
                    <Field label="Min Players" value={String(draft.minPlayers ?? "")} onChange={(value) => setDraft((current) => ({ ...current, minPlayers: Number(value) }))} />
                    <Field label="Max Players" value={String(draft.maxPlayers ?? "")} onChange={(value) => setDraft((current) => ({ ...current, maxPlayers: Number(value) }))} />
                  </div>
                  <div className="grid gap-3 sm:grid-cols-3">
                    <Select label="Scoring" value={draft.scoringType || "CUSTOM"} onChange={(value) => setDraft((current) => ({ ...current, scoringType: value }))} options={["CUSTOM", "POINTS", "SETS", "GOALS", "RUNS_WICKETS_OVERS"]} />
                    <Select label="Match" value={draft.matchType || "CUSTOM"} onChange={(value) => setDraft((current) => ({ ...current, matchType: value }))} options={["CUSTOM", "TEAM", "SINGLES", "DOUBLES", "TEAM_OR_INDIVIDUAL"]} />
                    <Select label="Ranking" value={draft.rankingType || "CUSTOM"} onChange={(value) => setDraft((current) => ({ ...current, rankingType: value }))} options={["CUSTOM", "POINTS_TABLE", "ELO", "WIN_PERCENTAGE", "SET_POINTS"]} />
                  </div>
                  <textarea value={draft.rules || ""} onChange={(event) => setDraft((current) => ({ ...current, rules: event.target.value }))} placeholder="Sport rules, scoring notes, and winner logic..." className="min-h-24 w-full rounded-2xl border border-white/10 bg-slate-950 px-4 py-3 text-sm text-white outline-none focus:border-cyan-300/60" />
                  <button onClick={() => void handleCreateSport()} disabled={saving || !draft.name?.trim()} className="w-full rounded-2xl bg-cyan-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-cyan-300 disabled:opacity-60">
                    Create Configured Sport
                  </button>
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 lg:grid-cols-3">
              <Panel title="Rule Templates" subtitle="Default configuration templates for current and future sports.">
                <div className="space-y-3">
                  {(dashboard?.sportTemplates || []).map((template) => (
                    <CompactRow key={String(template.code)} title={String(template.name)} meta={`${String(template.scoringType)} | ${String(template.matchType)} | ${String(template.rankingType)}`} status={`${String(template.minPlayers)}-${String(template.maxPlayers)} players`} />
                  ))}
                </div>
              </Panel>

              <Panel title="Cross-Module Status" subtitle="Booking, teams, matches, tournaments, analytics, and catalog readiness.">
                <div className="space-y-3">
                  {(dashboard?.crossModuleStatus || []).map((item) => (
                    <CompactRow key={String(item.module)} title={String(item.module)} meta={String(item.description)} status={String(item.status)} />
                  ))}
                </div>
              </Panel>

              <Panel title="Future Sport Readiness" subtitle="No-code sport enablement, dynamic UI hints, and custom scoring support.">
                <div className="space-y-3 text-sm text-slate-300">
                  {Object.entries(dashboard?.futureSportReadiness || {}).map(([key, value]) => (
                    <div key={key} className="rounded-2xl border border-white/10 bg-slate-950/60 p-3">
                      <p className="text-xs font-black uppercase tracking-[0.18em] text-cyan-300">{labelize(key)}</p>
                      <p className="mt-1 font-semibold text-white">{Array.isArray(value) ? value.join(", ") : String(value)}</p>
                    </div>
                  ))}
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
              <Panel title="Rule Simulation" subtitle="Validate sport-aware score model, winner logic, ranking formula, and dynamic UI hints.">
                {simulation ? (
                  <div className="space-y-3">
                    <MetricCard label="Sport" value={String(simulation.sportCode)} detail={String(simulation.matchType || "configured match")} compact />
                    <pre className="overflow-auto rounded-3xl border border-white/10 bg-slate-950 p-4 text-xs leading-6 text-slate-200">{JSON.stringify(simulation, null, 2)}</pre>
                  </div>
                ) : (
                  <p className="text-sm text-slate-400">Select any sport from the catalog to simulate its rule engine.</p>
                )}
              </Panel>

              <Panel title="Analytics Segmentation" subtitle="Sport-specific teams, tournaments, matches, turf mappings, and future BI segmentation.">
                <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
                  {segmentationRows.map((row) => (
                    <MetricCard key={String(row.sportCode)} label={String(row.sportCode)} value={`${numberValue(row.teams)} teams`} detail={`${numberValue(row.tournaments)} tournaments | ${numberValue(row.matches)} matches | ${numberValue(row.turfMappings)} turfs`} compact />
                  ))}
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 lg:grid-cols-2">
              <Panel title="Governance Controls" subtitle="Configuration audit, lifecycle governance, data isolation, no-code expansion, and rollback readiness.">
                <div className="space-y-3">
                  {(dashboard?.governanceControls || []).map((item) => (
                    <CompactRow key={String(item.key)} title={String(item.key)} meta={String(item.description)} status={String(item.status)} />
                  ))}
                </div>
              </Panel>

              <Panel title="Priority Actions & Audit" subtitle="Operational queue and latest sport configuration audit entries.">
                <div className="space-y-3">
                  {(dashboard?.priorityActions || []).map((item) => (
                    <CompactRow key={String(item.title)} title={String(item.title)} meta={String(item.description)} status={String(item.severity)} />
                  ))}
                  {audit.slice(0, 5).map((item) => (
                    <CompactRow key={`audit-${String(item.id)}`} title={String(item.action)} meta={`${String(item.sportCode || "SPORT")} | ${String(item.summary || "")}`} status={String(item.actor || "system")} />
                  ))}
                </div>
              </Panel>
            </section>
          </>
        )}
      </div>
    </main>
  );
}

function Panel({ title, subtitle, children }: { title: string; subtitle: string; children: React.ReactNode }) {
  return (
    <section className="rounded-[2rem] border border-white/10 bg-white/[0.04] p-5 shadow-xl">
      <div className="mb-5">
        <h2 className="text-xl font-black text-white">{title}</h2>
        <p className="mt-1 text-sm leading-6 text-slate-400">{subtitle}</p>
      </div>
      {children}
    </section>
  );
}

function MetricCard({ label, value, detail, compact = false }: { label: string; value: string | number; detail: string; compact?: boolean }) {
  return (
    <div className={`rounded-3xl border border-white/10 bg-slate-900/80 ${compact ? "p-4" : "p-5"}`}>
      <div className="flex min-w-0 items-start justify-between gap-2">
        <p className="min-w-0 break-words text-xs font-black uppercase leading-5 tracking-[0.2em] text-slate-500">{label}</p>
        <TileInfo label={`${label} information`}>{detail}</TileInfo>
      </div>
      <p className="mt-3 text-2xl font-black text-white">{value}</p>
    </div>
  );
}

function CompactRow({ title, meta, status }: { title: string; meta: string; status: string }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-slate-950/60 p-3">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="font-black text-white">{title}</p>
          <p className="mt-1 text-xs leading-5 text-slate-400">{meta}</p>
        </div>
        <span className="shrink-0 rounded-full bg-cyan-400/10 px-3 py-1 text-[10px] font-black uppercase tracking-[0.14em] text-cyan-200">{status}</span>
      </div>
    </div>
  );
}

function Field({ label, value, onChange, placeholder }: { label: string; value: string; onChange: (value: string) => void; placeholder?: string }) {
  return (
    <label className="block">
      <span className="text-xs font-black uppercase tracking-[0.18em] text-slate-500">{label}</span>
      <input value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} className="mt-2 w-full rounded-2xl border border-white/10 bg-slate-950 px-4 py-3 text-sm text-white outline-none focus:border-cyan-300/60" />
    </label>
  );
}

function Select({ label, value, onChange, options }: { label: string; value: string; onChange: (value: string) => void; options: string[] }) {
  return (
    <label className="block">
      <span className="text-xs font-black uppercase tracking-[0.18em] text-slate-500">{label}</span>
      <select value={value} onChange={(event) => onChange(event.target.value)} className="mt-2 w-full rounded-2xl border border-white/10 bg-slate-950 px-4 py-3 text-sm text-white outline-none focus:border-cyan-300/60">
        {options.map((option) => (
          <option key={option} value={option}>
            {option}
          </option>
        ))}
      </select>
    </label>
  );
}

function numberValue(value: unknown) {
  const number = Number(value ?? 0);
  return Number.isFinite(number) ? number : 0;
}

function labelize(value: string) {
  return value.replace(/([A-Z])/g, " $1").replace(/^./, (char) => char.toUpperCase());
}
