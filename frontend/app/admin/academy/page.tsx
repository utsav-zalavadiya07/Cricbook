"use client";

import { useEffect, useMemo, useState } from "react";
import {
  AcademyRegistrationPayload,
  AdminAcademyDashboard,
  AdminAcademyMap,
  createAdminAcademy,
  getAdminAcademies,
  getAdminAcademyAnalytics,
  getAdminAcademyAssessments,
  getAdminAcademyAttendance,
  getAdminAcademyAudit,
  getAdminAcademyCertificates,
  getAdminAcademyDashboard,
  getAdminAcademyEnrollments,
  getAdminAcademyRevenue,
  getAdminAcademyVideos,
  getAdminCoaches,
  getAdminTalentProfiles,
  getAdminTrainingBatches,
  getAdminTrainingPrograms,
} from "@/modules/admin/services/adminService";

const defaultAcademy: AcademyRegistrationPayload = {
  name: "",
  sportsOffered: "CRICKET",
  city: "",
  email: "",
  website: "",
  description: "",
};

export default function AdminAcademyPage() {
  const [dashboard, setDashboard] = useState<AdminAcademyDashboard | null>(null);
  const [academies, setAcademies] = useState<AdminAcademyMap[]>([]);
  const [coaches, setCoaches] = useState<AdminAcademyMap[]>([]);
  const [programs, setPrograms] = useState<AdminAcademyMap[]>([]);
  const [batches, setBatches] = useState<AdminAcademyMap[]>([]);
  const [enrollments, setEnrollments] = useState<AdminAcademyMap[]>([]);
  const [attendance, setAttendance] = useState<AdminAcademyMap[]>([]);
  const [assessments, setAssessments] = useState<AdminAcademyMap[]>([]);
  const [videos, setVideos] = useState<AdminAcademyMap[]>([]);
  const [talent, setTalent] = useState<AdminAcademyMap[]>([]);
  const [certificates, setCertificates] = useState<AdminAcademyMap[]>([]);
  const [analytics, setAnalytics] = useState<AdminAcademyMap | null>(null);
  const [revenue, setRevenue] = useState<AdminAcademyMap | null>(null);
  const [audit, setAudit] = useState<AdminAcademyMap[]>([]);
  const [draft, setDraft] = useState<AcademyRegistrationPayload>(defaultAcademy);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  async function loadAcademy() {
    try {
      setLoading(true);
      const [dashboardRes, academiesRes, coachesRes, programsRes, batchesRes, enrollmentsRes, attendanceRes, assessmentsRes, videosRes, talentRes, certificatesRes, analyticsRes, revenueRes, auditRes] = await Promise.all([
        getAdminAcademyDashboard(),
        getAdminAcademies(),
        getAdminCoaches(),
        getAdminTrainingPrograms(),
        getAdminTrainingBatches(),
        getAdminAcademyEnrollments(),
        getAdminAcademyAttendance(),
        getAdminAcademyAssessments(),
        getAdminAcademyVideos(),
        getAdminTalentProfiles(),
        getAdminAcademyCertificates(),
        getAdminAcademyAnalytics(),
        getAdminAcademyRevenue(),
        getAdminAcademyAudit(),
      ]);
      setDashboard(dashboardRes.data);
      setAcademies(academiesRes.data);
      setCoaches(coachesRes.data);
      setPrograms(programsRes.data);
      setBatches(batchesRes.data);
      setEnrollments(enrollmentsRes.data);
      setAttendance(attendanceRes.data);
      setAssessments(assessmentsRes.data);
      setVideos(videosRes.data);
      setTalent(talentRes.data);
      setCertificates(certificatesRes.data);
      setAnalytics(analyticsRes.data);
      setRevenue(revenueRes.data);
      setAudit(auditRes.data);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void loadAcademy(), 0);
    return () => window.clearTimeout(timer);
  }, []);

  const metrics = useMemo(
    () => [
      { label: "Academies", value: formatNumber(dashboard?.kpis?.academies), detail: `${formatNumber(dashboard?.kpis?.approvedAcademies)} approved` },
      { label: "Coaches", value: formatNumber(dashboard?.kpis?.coaches), detail: `${formatNumber(dashboard?.kpis?.verifiedCoaches)} verified` },
      { label: "Programs", value: formatNumber(dashboard?.kpis?.programs), detail: `${formatNumber(dashboard?.kpis?.publishedPrograms)} published` },
      { label: "Enrollments", value: formatNumber(dashboard?.kpis?.enrollments), detail: `${formatNumber(dashboard?.kpis?.confirmedEnrollments)} confirmed` },
      { label: "Attendance", value: `${formatNumber(dashboard?.kpis?.attendancePercent)}%`, detail: `${attendance.length} records` },
      { label: "Assessment Avg", value: formatNumber(dashboard?.kpis?.averageAssessment), detail: `${assessments.length} assessments` },
      { label: "Talent", value: formatNumber(dashboard?.kpis?.discoverableTalent), detail: "discoverable profiles" },
      { label: "Revenue", value: formatCurrency(revenue?.programRevenue ?? dashboard?.kpis?.revenue), detail: "training revenue" },
    ],
    [assessments.length, attendance.length, dashboard, revenue]
  );

  async function handleCreateAcademy() {
    if (!draft.name.trim() || !draft.sportsOffered.trim()) {
      return;
    }
    setSaving(true);
    try {
      await createAdminAcademy(draft);
      setDraft(defaultAcademy);
      await loadAcademy();
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="min-h-screen bg-slate-950 px-4 py-8 text-white sm:px-8" data-i18n-skip="true">
      <div className="mx-auto max-w-7xl space-y-8">
        <header className="rounded-[2rem] border border-white/10 bg-gradient-to-br from-emerald-500/15 via-slate-900 to-sky-500/10 p-6 shadow-2xl">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.26em] text-emerald-300">EPIC 26 Academy, Coaching & Talent Development</p>
              <h1 className="mt-3 text-3xl font-black text-white sm:text-4xl">Academy & Talent Lifecycle Center</h1>
              <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-300">
                Manage academies, coaches, programs, batches, enrollments, attendance, assessments, video feedback, talent discovery, certificates, CRM, and training revenue.
              </p>
            </div>
            <button onClick={() => void loadAcademy()} className="rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-emerald-300">
              Refresh Academy OS
            </button>
          </div>
        </header>

        {loading ? (
          <section className="rounded-[2rem] border border-white/10 bg-white/[0.04] p-10 text-center text-sm font-bold text-slate-300">Loading academy control plane...</section>
        ) : (
          <>
            <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              {metrics.map((metric) => (
                <MetricCard key={metric.label} {...metric} />
              ))}
            </section>

            <section className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
              <Panel title="Academy Approval Pipeline" subtitle="Registration, document checks, accreditation, and marketplace visibility.">
                <DataTable rows={academies} columns={[["name", "Academy"], ["sportsOffered", "Sports"], ["city", "City"], ["status", "Status"]]} empty="No academies registered yet." />
              </Panel>
              <Panel title="Register Academy" subtitle="Create a new academy draft for admin approval and document verification.">
                <div className="space-y-3">
                  <Field label="Academy Name" value={draft.name} onChange={(value) => setDraft((current) => ({ ...current, name: value }))} />
                  <Field label="Sports Offered" value={draft.sportsOffered} onChange={(value) => setDraft((current) => ({ ...current, sportsOffered: value }))} />
                  <Field label="City" value={draft.city || ""} onChange={(value) => setDraft((current) => ({ ...current, city: value }))} />
                  <Field label="Email" value={draft.email || ""} onChange={(value) => setDraft((current) => ({ ...current, email: value }))} />
                  <Field label="Website" value={draft.website || ""} onChange={(value) => setDraft((current) => ({ ...current, website: value }))} />
                  <textarea value={draft.description || ""} onChange={(event) => setDraft((current) => ({ ...current, description: event.target.value }))} placeholder="Academy description..." className="min-h-24 w-full rounded-2xl border border-white/10 bg-slate-950 px-4 py-3 text-sm text-white outline-none focus:border-emerald-300/60" />
                  <button onClick={() => void handleCreateAcademy()} disabled={saving || !draft.name.trim()} className="w-full rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-emerald-300 disabled:opacity-60">
                    Create Academy
                  </button>
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 lg:grid-cols-3">
              <Panel title="Coach Marketplace" subtitle="Verification, certifications, experience, pricing, and student counts.">
                <DataTable rows={coaches} columns={[["name", "Coach"], ["sport", "Sport"], ["experienceYears", "Exp"], ["status", "Status"]]} empty="No coaches registered yet." />
              </Panel>
              <Panel title="Programs & Batches" subtitle="Beginner, advanced, professional, morning, evening, and weekend training supply.">
                <DataTable rows={programs} columns={[["title", "Program"], ["sport", "Sport"], ["level", "Level"], ["status", "Status"]]} empty="No training programs yet." />
                <div className="mt-4">
                  <DataTable rows={batches} columns={[["name", "Batch"], ["scheduleLabel", "Schedule"], ["capacity", "Cap"], ["enrolledCount", "Enrolled"]]} empty="No batches yet." />
                </div>
              </Panel>
              <Panel title="Enrollment CRM" subtitle="Active, inactive, waitlisted students and retention signals.">
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-1">
                  {Object.entries(dashboard?.academyCrm || {}).map(([key, value]) => (
                    <MetricCard key={key} label={labelize(key)} value={formatNumber(value)} detail="CRM segment" compact />
                  ))}
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
              <Panel title="Attendance & Assessments" subtitle="Attendance percentage, skill scores, progress analytics, and assessment auditability.">
                <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                  {Object.entries((analytics?.skillImprovementTrend as Record<string, unknown> | undefined) || dashboard?.skillProgression || {}).map(([key, value]) => (
                    <MetricCard key={key} label={labelize(key)} value={formatNumber(value)} detail="skill metric" compact />
                  ))}
                </div>
                <div className="mt-5">
                  <DataTable rows={assessments} columns={[["playerName", "Player"], ["sport", "Sport"], ["overallScore", "Score"], ["coachId", "Coach"]]} empty="No assessments yet." />
                </div>
              </Panel>

              <Panel title="Talent Discovery & Certificates" subtitle="Talent score, age groups, selector-ready profiles, certificate issuance, and QR verification.">
                <DataTable rows={talent.length ? talent : dashboard?.talentLeaderboard || []} columns={[["playerName", "Player"], ["ageGroup", "Age"], ["sport", "Sport"], ["talentScore", "Score"]]} empty="No talent profiles yet." />
                <div className="mt-5">
                  <DataTable rows={certificates} columns={[["certificateCode", "Code"], ["playerName", "Player"], ["certificateType", "Type"], ["status", "Status"]]} empty="No certificates issued yet." />
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 lg:grid-cols-3">
              <Panel title="Video Analysis" subtitle="Batting, bowling, fielding uploads, coach feedback, and improvement areas.">
                <DataTable rows={videos} columns={[["playerEmail", "Player"], ["category", "Category"], ["videoUrl", "Video"], ["createdAt", "Created"]]} empty="No training videos yet." />
              </Panel>
              <Panel title="Governance Controls" subtitle="Accreditation, coach compliance, assessment audit, talent privacy, and certificate verification.">
                <div className="space-y-3">
                  {(dashboard?.governanceControls || []).map((item) => (
                    <CompactRow key={String(item.key)} title={String(item.key)} meta={String(item.description)} status={String(item.status)} />
                  ))}
                </div>
              </Panel>
              <Panel title="Priority & Audit" subtitle="Operational queue and latest academy/talent audit events.">
                <div className="space-y-3">
                  {(dashboard?.priorityActions || []).slice(0, 4).map((item) => (
                    <CompactRow key={String(item.title)} title={String(item.title)} meta={String(item.description)} status={String(item.severity)} />
                  ))}
                  {audit.slice(0, 4).map((item) => (
                    <CompactRow key={`audit-${String(item.id)}`} title={String(item.action)} meta={String(item.summary)} status={String(item.actor || "system")} />
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

function MetricCard({ label, value, detail, compact = false }: { label: string; value: string; detail: string; compact?: boolean }) {
  return (
    <div className={`rounded-[1.5rem] border border-white/10 bg-slate-900/80 ${compact ? "p-4" : "p-5"}`}>
      <p className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">{label}</p>
      <p className={`mt-2 font-black text-white ${compact ? "text-2xl" : "text-3xl"}`}>{value}</p>
      <p className="mt-2 text-xs text-slate-400">{detail}</p>
    </div>
  );
}

function CompactRow({ title, meta, status }: { title: string; meta: string; status: string }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-slate-950/60 p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <p className="text-sm font-black text-white">{title}</p>
        <span className="rounded-full border border-emerald-400/20 bg-emerald-400/10 px-3 py-1 text-[10px] font-black uppercase tracking-[0.16em] text-emerald-200">{status}</span>
      </div>
      <p className="mt-2 text-xs leading-5 text-slate-400">{meta}</p>
    </div>
  );
}

function DataTable({ rows, columns, empty }: { rows: AdminAcademyMap[]; columns: Array<[string, string]>; empty: string }) {
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

function formatCell(value: AdminAcademyMap[string]) {
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

function formatCurrency(value: unknown) {
  const number = Number(value ?? 0);
  if (!Number.isFinite(number)) {
    return "₹0";
  }
  return new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 }).format(number);
}

function labelize(value: string) {
  return value.replace(/([A-Z])/g, " $1").replace(/_/g, " ").trim();
}
