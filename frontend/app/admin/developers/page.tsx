"use client";

import { useEffect, useMemo, useState } from "react";
import {
  AdminDeveloperDashboard,
  AdminDeveloperMap,
  DeveloperRegistrationPayload,
  createAdminDeveloper,
  createAdminDeveloperSandbox,
  getAdminDeveloperAnalytics,
  getAdminDeveloperApiKeys,
  getAdminDeveloperApplications,
  getAdminDeveloperAudit,
  getAdminDeveloperBilling,
  getAdminDeveloperMarketplace,
  getAdminDeveloperPlatformDashboard,
  getAdminDeveloperWebhooks,
  getAdminDevelopers,
} from "@/modules/admin/services/adminService";

const defaultDeveloper: DeveloperRegistrationPayload = {
  name: "",
  email: "",
  companyName: "",
  website: "",
  plan: "FREE",
};

export default function AdminDeveloperPlatformPage() {
  const [dashboard, setDashboard] = useState<AdminDeveloperDashboard | null>(null);
  const [developers, setDevelopers] = useState<AdminDeveloperMap[]>([]);
  const [applications, setApplications] = useState<AdminDeveloperMap[]>([]);
  const [apiKeys, setApiKeys] = useState<AdminDeveloperMap[]>([]);
  const [webhooks, setWebhooks] = useState<AdminDeveloperMap[]>([]);
  const [analytics, setAnalytics] = useState<AdminDeveloperMap | null>(null);
  const [billing, setBilling] = useState<AdminDeveloperMap | null>(null);
  const [marketplace, setMarketplace] = useState<AdminDeveloperMap[]>([]);
  const [audit, setAudit] = useState<AdminDeveloperMap[]>([]);
  const [draft, setDraft] = useState<DeveloperRegistrationPayload>(defaultDeveloper);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  async function loadDeveloperPlatform() {
    try {
      setLoading(true);
      const [dashboardRes, developersRes, appsRes, keysRes, webhooksRes, analyticsRes, billingRes, marketplaceRes, auditRes] = await Promise.all([
        getAdminDeveloperPlatformDashboard(),
        getAdminDevelopers(),
        getAdminDeveloperApplications(),
        getAdminDeveloperApiKeys(),
        getAdminDeveloperWebhooks(),
        getAdminDeveloperAnalytics(),
        getAdminDeveloperBilling(),
        getAdminDeveloperMarketplace(),
        getAdminDeveloperAudit(),
      ]);
      setDashboard(dashboardRes.data);
      setDevelopers(developersRes.data);
      setApplications(appsRes.data);
      setApiKeys(keysRes.data);
      setWebhooks(webhooksRes.data);
      setAnalytics(analyticsRes.data);
      setBilling(billingRes.data);
      setMarketplace(marketplaceRes.data);
      setAudit(auditRes.data);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void loadDeveloperPlatform(), 0);
    return () => window.clearTimeout(timer);
  }, []);

  const metrics = useMemo(
    () => [
      { label: "Developers", value: formatNumber(dashboard?.kpis?.developers), detail: `${formatNumber(dashboard?.kpis?.activeDevelopers)} active accounts` },
      { label: "Applications", value: formatNumber(dashboard?.kpis?.applications), detail: `${formatNumber(dashboard?.kpis?.activeApplications)} active OAuth clients` },
      { label: "API Keys", value: formatNumber(dashboard?.kpis?.activeApiKeys), detail: "hashed and visible once" },
      { label: "Webhooks", value: formatNumber(dashboard?.kpis?.webhooks), detail: "event endpoints" },
      { label: "API Calls", value: formatNumber(dashboard?.kpis?.apiCalls), detail: "tracked in usage ledger" },
      { label: "Errors", value: formatNumber(dashboard?.kpis?.errorCount), detail: `${formatNumber(dashboard?.kpis?.avgLatencyMs)} ms avg latency` },
      { label: "Marketplace", value: formatNumber(dashboard?.kpis?.marketplaceListings), detail: `${formatNumber(dashboard?.kpis?.approvedListings)} approved listings` },
      { label: "Billing", value: formatCurrency(billing?.totalRevenue), detail: "developer platform revenue" },
    ],
    [billing, dashboard]
  );

  async function handleCreateDeveloper() {
    if (!draft.name.trim() || !draft.email.trim()) {
      return;
    }
    setSaving(true);
    try {
      await createAdminDeveloper(draft);
      setDraft(defaultDeveloper);
      await loadDeveloperPlatform();
    } finally {
      setSaving(false);
    }
  }

  async function handleCreateSandbox(developerId: unknown) {
    const id = Number(developerId);
    if (!Number.isFinite(id) || id <= 0) {
      return;
    }
    setSaving(true);
    try {
      await createAdminDeveloperSandbox(id);
      await loadDeveloperPlatform();
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="min-h-screen bg-slate-950 px-4 py-8 text-white sm:px-8" data-i18n-skip="true">
      <div className="mx-auto max-w-7xl space-y-8">
        <header className="rounded-[2rem] border border-white/10 bg-gradient-to-br from-sky-500/15 via-slate-900 to-violet-500/10 p-6 shadow-2xl">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.26em] text-sky-300">EPIC 25 API Marketplace & Developer Platform</p>
              <h1 className="mt-3 text-3xl font-black text-white sm:text-4xl">Developer Ecosystem Control Center</h1>
              <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-300">
                Govern developer accounts, OAuth clients, API keys, public API access, webhooks, sandbox apps, usage analytics, billing, and partner marketplace approvals.
              </p>
            </div>
            <button onClick={() => void loadDeveloperPlatform()} className="rounded-2xl bg-sky-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-sky-300">
              Refresh API Hub
            </button>
          </div>
        </header>

        {loading ? (
          <section className="rounded-[2rem] border border-white/10 bg-white/[0.04] p-10 text-center text-sm font-bold text-slate-300">Loading developer platform...</section>
        ) : (
          <>
            <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              {metrics.map((metric) => (
                <MetricCard key={metric.label} {...metric} />
              ))}
            </section>

            <section className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
              <Panel title="Developer Accounts" subtitle="Partner onboarding, plan assignment, status governance, and sandbox provisioning.">
                <div className="space-y-3">
                  {developers.slice(0, 8).map((item) => (
                    <div key={String(item.id)} className="rounded-3xl border border-white/10 bg-slate-900/80 p-4">
                      <div className="flex flex-wrap items-start justify-between gap-3">
                        <div>
                          <p className="font-black text-white">{String(item.name || "Developer")}</p>
                          <p className="mt-1 text-xs text-slate-400">{String(item.companyName || "Independent")} | {String(item.email || "")}</p>
                        </div>
                        <span className="rounded-full bg-sky-400/10 px-3 py-1 text-[10px] font-black uppercase tracking-[0.16em] text-sky-200">{String(item.plan || "FREE")} / {String(item.status || "ACTIVE")}</span>
                      </div>
                      <button onClick={() => void handleCreateSandbox(item.id)} disabled={saving} className="mt-4 rounded-2xl border border-sky-300/30 px-3 py-2 text-xs font-black text-sky-100 hover:bg-sky-400/10 disabled:opacity-60">
                        Create Sandbox App + Key
                      </button>
                    </div>
                  ))}
                  {developers.length === 0 ? <p className="text-sm text-slate-400">No developer accounts yet.</p> : null}
                </div>
              </Panel>

              <Panel title="Create Developer" subtitle="Register coaching apps, scoring systems, academy partners, and corporate sports integrations.">
                <div className="space-y-3">
                  <Field label="Name" value={draft.name} onChange={(value) => setDraft((current) => ({ ...current, name: value }))} placeholder="Partner developer" />
                  <Field label="Email" value={draft.email} onChange={(value) => setDraft((current) => ({ ...current, email: value }))} placeholder="dev@example.com" />
                  <Field label="Company" value={draft.companyName || ""} onChange={(value) => setDraft((current) => ({ ...current, companyName: value }))} placeholder="Academy platform" />
                  <Field label="Website" value={draft.website || ""} onChange={(value) => setDraft((current) => ({ ...current, website: value }))} placeholder="https://partner.example" />
                  <Select label="Plan" value={draft.plan || "FREE"} onChange={(value) => setDraft((current) => ({ ...current, plan: value }))} options={["FREE", "PRO", "ENTERPRISE"]} />
                  <button onClick={() => void handleCreateDeveloper()} disabled={saving || !draft.name.trim() || !draft.email.trim()} className="w-full rounded-2xl bg-sky-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-sky-300 disabled:opacity-60">
                    Create Developer Account
                  </button>
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 lg:grid-cols-3">
              <Panel title="OAuth Applications" subtitle="Client IDs, environments, callback URLs, scopes, and active application lifecycle.">
                <DataTable rows={applications} columns={[["name", "Application"], ["developerName", "Developer"], ["environment", "Env"], ["status", "Status"]]} empty="No applications yet." />
              </Panel>

              <Panel title="API Key Lifecycle" subtitle="Development, testing, production keys, rotation, revocation, and rate-limit envelope.">
                <DataTable rows={apiKeys} columns={[["keyPrefix", "Key Prefix"], ["applicationName", "App"], ["environment", "Env"], ["status", "Status"]]} empty="No API keys yet." />
              </Panel>

              <Panel title="Webhook Engine" subtitle="Event subscriptions, endpoint verification, signature protection, retries, and delivery tests.">
                <DataTable rows={webhooks} columns={[["url", "Endpoint"], ["applicationName", "App"], ["status", "Status"], ["retryCount", "Retries"]]} empty="No webhooks configured." />
              </Panel>
            </section>

            <section className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
              <Panel title="API Products & Docs" subtitle="Public Booking, Turf, Tournament, Team, Player Stats, Analytics, and Webhook APIs.">
                <div className="space-y-3">
                  {(dashboard?.apiProducts || []).map((item) => (
                    <CompactRow key={String(item.name)} title={String(item.name)} meta={String(item.description)} status={String(item.status || "AVAILABLE")} />
                  ))}
                </div>
              </Panel>

              <Panel title="Usage, Billing & Rate Plans" subtitle="Usage tracking, errors, latency, invoice reconciliation, tiers, and billing readiness.">
                <div className="grid gap-3 sm:grid-cols-3">
                  <MetricCard label="Requests" value={formatNumber((analytics?.usageAnalytics as Record<string, unknown> | undefined)?.requestCount)} detail="tracked calls" compact />
                  <MetricCard label="Errors" value={formatNumber((analytics?.usageAnalytics as Record<string, unknown> | undefined)?.errorCount)} detail="4xx/5xx calls" compact />
                  <MetricCard label="Invoices" value={formatNumber((billing?.invoices as unknown[] | undefined)?.length)} detail="billing records" compact />
                </div>
                <div className="mt-5 grid gap-3 sm:grid-cols-3">
                  {(dashboard?.ratePlans || []).map((plan) => (
                    <MetricCard key={String(plan.plan)} label={String(plan.plan)} value={formatNumber(plan.dailyLimit)} detail={`${formatCurrency(plan.monthlyPrice)} / month`} compact />
                  ))}
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 lg:grid-cols-3">
              <Panel title="Partner Marketplace" subtitle="Coaching, scoring, academy, corporate sports, and tournament integrations.">
                <DataTable rows={marketplace.length ? marketplace : dashboard?.marketplace || []} columns={[["title", "Listing"], ["developerName", "Developer"], ["category", "Category"], ["status", "Status"]]} empty="No marketplace listings yet." />
              </Panel>

              <Panel title="Security & Governance" subtitle="OAuth, key hashing, SSRF guard, replay prevention, audit, and tenant boundary controls.">
                <div className="space-y-3">
                  {(dashboard?.securityControls || []).map((item) => (
                    <CompactRow key={String(item.key)} title={String(item.key)} meta={String(item.description)} status={String(item.status)} />
                  ))}
                </div>
              </Panel>

              <Panel title="Priority & Audit" subtitle="Operational queue plus latest API platform audit events.">
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
        <span className="rounded-full border border-sky-400/20 bg-sky-400/10 px-3 py-1 text-[10px] font-black uppercase tracking-[0.16em] text-sky-200">{status}</span>
      </div>
      <p className="mt-2 text-xs leading-5 text-slate-400">{meta}</p>
    </div>
  );
}

function DataTable({ rows, columns, empty }: { rows: AdminDeveloperMap[]; columns: Array<[string, string]>; empty: string }) {
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

function Field({ label, value, onChange, placeholder = "" }: { label: string; value: string; onChange: (value: string) => void; placeholder?: string }) {
  return (
    <label className="block text-xs font-black uppercase tracking-[0.18em] text-slate-400">
      {label}
      <input value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} className="mt-2 w-full rounded-2xl border border-white/10 bg-slate-950 px-4 py-3 text-sm font-semibold normal-case tracking-normal text-white outline-none focus:border-sky-300/60" />
    </label>
  );
}

function Select({ label, value, onChange, options }: { label: string; value: string; onChange: (value: string) => void; options: string[] }) {
  return (
    <label className="block text-xs font-black uppercase tracking-[0.18em] text-slate-400">
      {label}
      <select value={value} onChange={(event) => onChange(event.target.value)} className="mt-2 w-full rounded-2xl border border-white/10 bg-slate-950 px-4 py-3 text-sm font-semibold normal-case tracking-normal text-white outline-none focus:border-sky-300/60">
        {options.map((option) => (
          <option key={option} value={option}>{option}</option>
        ))}
      </select>
    </label>
  );
}

function formatCell(value: AdminDeveloperMap[string]) {
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
