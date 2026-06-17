"use client";

import { useEffect, useMemo, useState } from "react";
import {
  AdminCareerDashboard,
  AdminCareerMap,
  AdminCareerOpportunityPayload,
  createAdminCareerOpportunity,
  evaluateAdminCareerApplication,
  getAdminCareerApplications,
  getAdminCareerAudit,
  getAdminCareerDashboard,
  getAdminCareerEvaluations,
  getAdminCareerOpportunities,
  getAdminCareerProfiles,
  getAdminCareerScouts,
  updateAdminCareerApplicationStatus,
  updateAdminCareerScoutStatus,
  verifyAdminCareerAchievement,
} from "@/modules/admin/services/adminService";

const defaultOpportunity: AdminCareerOpportunityPayload = {
  opportunityType: "TRIAL",
  title: "",
  organizationName: "",
  sport: "CRICKET",
  location: "",
  category: "U19",
  eligibility: "",
  capacity: 20,
  status: "PUBLISHED",
};

export default function AdminCareerPage() {
  const [dashboard, setDashboard] = useState<AdminCareerDashboard | null>(null);
  const [profiles, setProfiles] = useState<AdminCareerMap[]>([]);
  const [scouts, setScouts] = useState<AdminCareerMap[]>([]);
  const [opportunities, setOpportunities] = useState<AdminCareerMap[]>([]);
  const [applications, setApplications] = useState<AdminCareerMap[]>([]);
  const [evaluations, setEvaluations] = useState<AdminCareerMap[]>([]);
  const [audit, setAudit] = useState<AdminCareerMap[]>([]);
  const [draft, setDraft] = useState<AdminCareerOpportunityPayload>(defaultOpportunity);
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState("");

  async function loadCareer() {
    try {
      setLoading(true);
      const [dashboardRes, profilesRes, scoutsRes, opportunitiesRes, applicationsRes, evaluationsRes, auditRes] = await Promise.all([
        getAdminCareerDashboard(),
        getAdminCareerProfiles(),
        getAdminCareerScouts(),
        getAdminCareerOpportunities(),
        getAdminCareerApplications(),
        getAdminCareerEvaluations(),
        getAdminCareerAudit(),
      ]);
      setDashboard(dashboardRes.data);
      setProfiles(profilesRes.data);
      setScouts(scoutsRes.data);
      setOpportunities(opportunitiesRes.data);
      setApplications(applicationsRes.data);
      setEvaluations(evaluationsRes.data);
      setAudit(auditRes.data);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void loadCareer(), 0);
    return () => window.clearTimeout(timer);
  }, []);

  const metrics = useMemo(
    () => [
      { label: "Profiles", value: dashboard?.kpis?.careerProfiles ?? profiles.length, detail: `${formatNumber(dashboard?.kpis?.discoverableProfiles)} discoverable` },
      { label: "Verified Scouts", value: dashboard?.kpis?.verifiedScouts ?? 0, detail: `${scouts.length} registered` },
      { label: "Open Opportunities", value: dashboard?.kpis?.openOpportunities ?? opportunities.length, detail: "recruitment, trials, scholarships" },
      { label: "Applications", value: dashboard?.kpis?.applications ?? applications.length, detail: `${formatNumber(dashboard?.kpis?.selectedPlayers)} selected` },
      { label: "Pending Achievements", value: dashboard?.kpis?.pendingAchievements ?? 0, detail: "verification queue" },
      { label: "Avg Evaluation", value: dashboard?.kpis?.averageEvaluation ?? 0, detail: `${evaluations.length} evaluations` },
      { label: "Resume Downloads", value: dashboard?.careerAnalytics?.resumeDownloads ?? 0, detail: "career analytics" },
      { label: "Scout Views", value: dashboard?.careerAnalytics?.scoutViews ?? 0, detail: "marketplace demand" },
    ],
    [applications.length, dashboard, evaluations.length, opportunities.length, profiles.length, scouts.length]
  );

  async function handleCreateOpportunity() {
    if (!draft.title.trim() || !draft.organizationName.trim()) {
      setStatus("Title and organization are required.");
      return;
    }
    try {
      await createAdminCareerOpportunity(draft);
      setDraft(defaultOpportunity);
      setStatus("Career opportunity published.");
      await loadCareer();
    } catch (error) {
      setStatus(errorMessage(error, "Could not create opportunity."));
    }
  }

  async function handleScoutStatus(id: number, nextStatus: string) {
    await updateAdminCareerScoutStatus(id, { status: nextStatus });
    await loadCareer();
  }

  async function handleApplicationStatus(id: number, nextStatus: string) {
    await updateAdminCareerApplicationStatus(id, { status: nextStatus, notes: `Marked ${nextStatus} from admin career portal.` });
    await loadCareer();
  }

  async function handleEvaluate(id: number) {
    await evaluateAdminCareerApplication(id, {
      evaluatorName: "Admin Evaluator",
      techniqueScore: 82,
      fitnessScore: 78,
      disciplineScore: 84,
      awarenessScore: 80,
      decision: "SHORTLISTED",
      notes: "Default evaluation recorded from admin portal.",
    });
    await loadCareer();
  }

  async function handleVerifyAchievement(id: number) {
    await verifyAdminCareerAchievement(id, { status: "VERIFIED", notes: "Verified from admin career portal." });
    await loadCareer();
  }

  return (
    <main className="min-h-screen bg-slate-950 px-4 py-8 text-white sm:px-8" data-i18n-skip="true">
      <div className="mx-auto max-w-7xl space-y-8">
        <header className="rounded-[2rem] border border-white/10 bg-gradient-to-br from-emerald-500/15 via-slate-900 to-sky-500/10 p-6 shadow-2xl">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.26em] text-emerald-300">EPIC 31 Sports Career, Recruitment & Scouting</p>
              <h1 className="mt-3 text-3xl font-black text-white sm:text-4xl">Career Ecosystem Control Center</h1>
              <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-300">
                Govern verified sports profiles, resumes, achievements, scouts, recruitment, trials, evaluations, scholarships, rankings, and career analytics.
              </p>
            </div>
            <button onClick={() => void loadCareer()} className="rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-emerald-300">
              Refresh Career OS
            </button>
          </div>
        </header>

        {status ? <div className="rounded-2xl border border-emerald-400/20 bg-emerald-400/10 px-4 py-3 text-sm font-bold text-emerald-100">{status}</div> : null}

        {loading ? (
          <section className="rounded-[2rem] border border-white/10 bg-white/[0.04] p-10 text-center text-sm font-bold text-slate-300">Loading career control plane...</section>
        ) : (
          <>
            <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              {metrics.map((metric) => (
                <MetricCard key={metric.label} {...metric} />
              ))}
            </section>

            <section className="grid gap-6 xl:grid-cols-[1fr_0.9fr]">
              <Panel title="Create Opportunity" subtitle="Publish recruitment, trial, scholarship, or academy admission opportunities.">
                <div className="grid gap-3 sm:grid-cols-2">
                  <Field label="Type" value={draft.opportunityType} onChange={(value) => setDraft((current) => ({ ...current, opportunityType: value }))} />
                  <Field label="Sport" value={draft.sport || ""} onChange={(value) => setDraft((current) => ({ ...current, sport: value }))} />
                  <Field label="Title" value={draft.title} onChange={(value) => setDraft((current) => ({ ...current, title: value }))} />
                  <Field label="Organization" value={draft.organizationName} onChange={(value) => setDraft((current) => ({ ...current, organizationName: value }))} />
                  <Field label="Location" value={draft.location || ""} onChange={(value) => setDraft((current) => ({ ...current, location: value }))} />
                  <Field label="Capacity" value={String(draft.capacity || "")} onChange={(value) => setDraft((current) => ({ ...current, capacity: Number(value) || 0 }))} />
                </div>
                <textarea value={draft.eligibility || ""} onChange={(event) => setDraft((current) => ({ ...current, eligibility: event.target.value }))} placeholder="Eligibility criteria..." className="mt-3 min-h-24 w-full rounded-2xl border border-white/10 bg-slate-950 px-4 py-3 text-sm text-white outline-none focus:border-emerald-300/60" />
                <button onClick={() => void handleCreateOpportunity()} className="mt-4 w-full rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-emerald-300">
                  Publish Opportunity
                </button>
              </Panel>

              <Panel title="Governance Controls" subtitle="Privacy, verification, scout access, trial evaluations, and scholarship governance.">
                <div className="space-y-3">
                  {(dashboard?.governanceControls || []).map((item) => (
                    <CompactRow key={String(item.key)} title={String(item.key)} meta={String(item.description)} status={String(item.status)} />
                  ))}
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 xl:grid-cols-2">
              <Panel title="Talent Rankings" subtitle="Verified profile completeness, achievements, videos, and selections drive ranking scores.">
                <DataTable rows={profiles.length ? profiles : dashboard?.profileLeaderboard || []} columns={[["playerName", "Player"], ["sport", "Sport"], ["city", "City"], ["rankingScore", "Score"], ["rankingBand", "Band"]]} empty="No career profiles yet." />
              </Panel>
              <Panel title="Scout Verification" subtitle="Scouts cannot log marketplace activity until verified.">
                <div className="space-y-3">
                  {scouts.slice(0, 8).map((scout) => (
                    <CompactRow
                      key={String(scout.id)}
                      title={String(scout.scoutName)}
                      meta={`${String(scout.organizationName)} · ${String(scout.region || "--")}`}
                      status={String(scout.status)}
                      actionLabel={String(scout.status) === "VERIFIED" ? "Suspend" : "Verify"}
                      onAction={() => void handleScoutStatus(Number(scout.id), String(scout.status) === "VERIFIED" ? "SUSPENDED" : "VERIFIED")}
                    />
                  ))}
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
              <Panel title="Applications & Selection Pipeline" subtitle="Move players through applied, shortlisted, selected, rejected, and waitlisted states.">
                <div className="grid gap-3 sm:grid-cols-3">
                  {Object.entries(dashboard?.pipeline || {}).map(([key, value]) => (
                    <MetricCard key={key} label={labelize(key)} value={formatNumber(value)} detail="pipeline stage" compact />
                  ))}
                </div>
                <div className="mt-5 space-y-3">
                  {applications.slice(0, 8).map((application) => (
                    <CompactRow
                      key={String(application.id)}
                      title={`${String(application.playerName)} → ${String(application.opportunityTitle)}`}
                      meta={`Score ${String(application.evaluatorScore || 0)} · ${String(application.applicationType)}`}
                      status={String(application.status)}
                      actionLabel="Shortlist"
                      onAction={() => void handleApplicationStatus(Number(application.id), "SHORTLISTED")}
                      secondaryActionLabel="Evaluate"
                      onSecondaryAction={() => void handleEvaluate(Number(application.id))}
                    />
                  ))}
                </div>
              </Panel>

              <Panel title="Achievement & Audit Queue" subtitle="Verify achievements and review immutable career audit events.">
                <div className="space-y-3">
                  {(dashboard?.pendingVerification || []).slice(0, 5).map((achievement) => (
                    <CompactRow
                      key={String(achievement.id)}
                      title={String(achievement.title)}
                      meta={`${String(achievement.playerName)} · ${String(achievement.issuer || "self submitted")}`}
                      status={String(achievement.status)}
                      actionLabel="Verify"
                      onAction={() => void handleVerifyAchievement(Number(achievement.id))}
                    />
                  ))}
                  {audit.slice(0, 5).map((item) => (
                    <CompactRow key={`audit-${String(item.id)}`} title={String(item.action)} meta={String(item.summary)} status={String(item.actor || "system")} />
                  ))}
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 lg:grid-cols-3">
              <Panel title="Opportunities" subtitle="Marketplace supply across recruitment, trials, scholarships, and admissions.">
                <DataTable rows={opportunities} columns={[["title", "Title"], ["opportunityType", "Type"], ["sport", "Sport"], ["status", "Status"]]} empty="No opportunities yet." />
              </Panel>
              <Panel title="Evaluations" subtitle="Technique, fitness, discipline, awareness, and decision scores.">
                <DataTable rows={evaluations} columns={[["playerName", "Player"], ["opportunityTitle", "Opportunity"], ["overallScore", "Score"], ["decision", "Decision"]]} empty="No evaluations yet." />
              </Panel>
              <Panel title="Priority Actions" subtitle="Operational gaps that affect career marketplace readiness.">
                <div className="space-y-3">
                  {(dashboard?.priorityActions || []).map((item) => (
                    <CompactRow key={String(item.title)} title={String(item.title)} meta={String(item.description)} status={String(item.severity)} />
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
    <section className="rounded-[2rem] border border-white/10 bg-white/[0.04] p-5 shadow-2xl shadow-black/20">
      <div className="mb-5">
        <h2 className="text-lg font-black text-white">{title}</h2>
        <p className="mt-1 text-sm leading-5 text-slate-400">{subtitle}</p>
      </div>
      {children}
    </section>
  );
}

function MetricCard({ label, value, detail, compact = false }: { label: string; value: unknown; detail: string; compact?: boolean }) {
  return (
    <div className={`rounded-[1.5rem] border border-white/10 bg-slate-900/80 ${compact ? "p-4" : "p-5"}`}>
      <p className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">{label}</p>
      <p className={`mt-2 font-black text-white ${compact ? "text-2xl" : "text-3xl"}`}>{formatNumber(value)}</p>
      <p className="mt-2 text-xs text-slate-400">{detail}</p>
    </div>
  );
}

function CompactRow({ title, meta, status, actionLabel, onAction, secondaryActionLabel, onSecondaryAction }: { title: string; meta: string; status: string; actionLabel?: string; onAction?: () => void; secondaryActionLabel?: string; onSecondaryAction?: () => void }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-slate-950/60 p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm font-black text-white">{title}</p>
          <p className="mt-2 text-xs leading-5 text-slate-400">{meta}</p>
        </div>
        <span className="rounded-full border border-emerald-400/20 bg-emerald-400/10 px-3 py-1 text-[10px] font-black uppercase tracking-[0.16em] text-emerald-200">{status}</span>
      </div>
      {(actionLabel || secondaryActionLabel) ? (
        <div className="mt-4 flex flex-wrap gap-2">
          {actionLabel ? <button onClick={onAction} className="rounded-xl bg-emerald-400 px-3 py-2 text-xs font-black text-slate-950">{actionLabel}</button> : null}
          {secondaryActionLabel ? <button onClick={onSecondaryAction} className="rounded-xl border border-white/10 px-3 py-2 text-xs font-black text-slate-200">{secondaryActionLabel}</button> : null}
        </div>
      ) : null}
    </div>
  );
}

function DataTable({ rows, columns, empty }: { rows: AdminCareerMap[]; columns: Array<[string, string]>; empty: string }) {
  if (rows.length === 0) {
    return <p className="py-6 text-center text-sm text-slate-400">{empty}</p>;
  }
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[560px] text-left text-sm">
        <thead className="text-xs uppercase tracking-[0.16em] text-slate-500">
          <tr>
            {columns.map(([, label]) => (
              <th key={label} className="border-b border-white/10 px-3 py-3 font-black">{label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.slice(0, 8).map((row, index) => (
            <tr key={String(row.id || index)} className="border-b border-white/5 text-slate-300">
              {columns.map(([key]) => (
                <td key={key} className="px-3 py-3">{formatCell(row[key])}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function Field({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <label className="block text-xs font-black uppercase tracking-[0.18em] text-slate-400">
      {label}
      <input value={value} onChange={(event) => onChange(event.target.value)} className="mt-2 w-full rounded-2xl border border-white/10 bg-slate-950 px-4 py-3 text-sm font-semibold normal-case tracking-normal text-white outline-none focus:border-emerald-300/60" />
    </label>
  );
}

function formatCell(value: AdminCareerMap[string]) {
  if (value === null || value === undefined || value === "") {
    return "--";
  }
  if (Array.isArray(value)) {
    return value.join(", ");
  }
  if (typeof value === "object") {
    return JSON.stringify(value);
  }
  if (typeof value === "number" && Math.abs(value) >= 1000) {
    return formatNumber(value);
  }
  return String(value);
}

function formatNumber(value: unknown) {
  const number = Number(value ?? 0);
  if (!Number.isFinite(number)) {
    return "0";
  }
  return new Intl.NumberFormat("en-IN", { maximumFractionDigits: 1 }).format(number);
}

function labelize(value: string) {
  return value.replace(/([A-Z])/g, " $1").replace(/_/g, " ").trim();
}

function errorMessage(error: unknown, fallback: string) {
  const maybeError = error as { response?: { data?: { message?: string } } };
  return maybeError.response?.data?.message || fallback;
}
