"use client";

import { useEffect, useMemo, useState } from "react";
import {
  AdminCorporateDashboard,
  AdminCorporateMap,
  CorporateCompanyPayload,
  createAdminCorporateCompany,
  getAdminCorporateAnalytics,
  getAdminCorporateAudit,
  getAdminCorporateBilling,
  getAdminCorporateBookingPlans,
  getAdminCorporateCompanies,
  getAdminCorporateContracts,
  getAdminCorporateDashboard,
  getAdminCorporateDepartments,
  getAdminCorporateEmployees,
  getAdminCorporateInvoices,
  getAdminCorporateLeagues,
  getAdminCorporateLocations,
  getAdminCorporateRewards,
  getAdminCorporateTeamMemberships,
  getAdminCorporateTeams,
  getAdminCorporateTournaments,
  getAdminCorporateWellnessActivities,
  getAdminCorporateWellnessPrograms,
} from "@/modules/admin/services/adminService";

const defaultCompany: CorporateCompanyPayload = {
  name: "",
  industry: "Technology",
  city: "",
  locations: "",
  hrContactName: "",
  hrEmail: "",
  adminEmail: "",
  monthlyBudget: 0,
  annualContractValue: 0,
};

export default function AdminCorporatePage() {
  const [dashboard, setDashboard] = useState<AdminCorporateDashboard | null>(null);
  const [companies, setCompanies] = useState<AdminCorporateMap[]>([]);
  const [locations, setLocations] = useState<AdminCorporateMap[]>([]);
  const [departments, setDepartments] = useState<AdminCorporateMap[]>([]);
  const [employees, setEmployees] = useState<AdminCorporateMap[]>([]);
  const [teams, setTeams] = useState<AdminCorporateMap[]>([]);
  const [memberships, setMemberships] = useState<AdminCorporateMap[]>([]);
  const [bookingPlans, setBookingPlans] = useState<AdminCorporateMap[]>([]);
  const [tournaments, setTournaments] = useState<AdminCorporateMap[]>([]);
  const [wellnessPrograms, setWellnessPrograms] = useState<AdminCorporateMap[]>([]);
  const [activities, setActivities] = useState<AdminCorporateMap[]>([]);
  const [rewards, setRewards] = useState<AdminCorporateMap[]>([]);
  const [invoices, setInvoices] = useState<AdminCorporateMap[]>([]);
  const [contracts, setContracts] = useState<AdminCorporateMap[]>([]);
  const [leagues, setLeagues] = useState<AdminCorporateMap[]>([]);
  const [analytics, setAnalytics] = useState<AdminCorporateMap | null>(null);
  const [billing, setBilling] = useState<AdminCorporateMap | null>(null);
  const [audit, setAudit] = useState<AdminCorporateMap[]>([]);
  const [draft, setDraft] = useState<CorporateCompanyPayload>(defaultCompany);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  async function loadCorporate() {
    try {
      setLoading(true);
      const [dashboardRes, companiesRes, locationsRes, departmentsRes, employeesRes, teamsRes, membershipsRes, bookingPlansRes, tournamentsRes, wellnessRes, activitiesRes, rewardsRes, invoicesRes, contractsRes, leaguesRes, analyticsRes, billingRes, auditRes] = await Promise.all([
        getAdminCorporateDashboard(),
        getAdminCorporateCompanies(),
        getAdminCorporateLocations(),
        getAdminCorporateDepartments(),
        getAdminCorporateEmployees(),
        getAdminCorporateTeams(),
        getAdminCorporateTeamMemberships(),
        getAdminCorporateBookingPlans(),
        getAdminCorporateTournaments(),
        getAdminCorporateWellnessPrograms(),
        getAdminCorporateWellnessActivities(),
        getAdminCorporateRewards(),
        getAdminCorporateInvoices(),
        getAdminCorporateContracts(),
        getAdminCorporateLeagues(),
        getAdminCorporateAnalytics(),
        getAdminCorporateBilling(),
        getAdminCorporateAudit(),
      ]);
      setDashboard(dashboardRes.data);
      setCompanies(companiesRes.data);
      setLocations(locationsRes.data);
      setDepartments(departmentsRes.data);
      setEmployees(employeesRes.data);
      setTeams(teamsRes.data);
      setMemberships(membershipsRes.data);
      setBookingPlans(bookingPlansRes.data);
      setTournaments(tournamentsRes.data);
      setWellnessPrograms(wellnessRes.data);
      setActivities(activitiesRes.data);
      setRewards(rewardsRes.data);
      setInvoices(invoicesRes.data);
      setContracts(contractsRes.data);
      setLeagues(leaguesRes.data);
      setAnalytics(analyticsRes.data);
      setBilling(billingRes.data);
      setAudit(auditRes.data);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadCorporate();
  }, []);

  const metrics = useMemo(() => {
    const kpis = dashboard?.kpis || {};
    return [
      ["Companies", kpis.companies ?? companies.length],
      ["Employees", kpis.employees ?? employees.length],
      ["Teams", kpis.teams ?? teams.length],
      ["Participation", `${kpis.participationRate ?? 0}%`],
      ["Monthly Spend", `₹${formatNumber(kpis.monthlySpend)}`],
      ["Invoice Spend", `₹${formatNumber(kpis.invoiceSpend)}`],
    ];
  }, [dashboard, companies.length, employees.length, teams.length]);

  async function submitCompany() {
    if (!draft.name.trim()) return;
    try {
      setSaving(true);
      await createAdminCorporateCompany(draft);
      setDraft(defaultCompany);
      await loadCorporate();
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="min-h-screen bg-slate-950 px-4 py-8 text-white sm:px-6 lg:px-8">
      <div className="mx-auto flex max-w-7xl flex-col gap-6">
        <section className="rounded-[2rem] border border-cyan-400/20 bg-gradient-to-br from-cyan-500/20 via-slate-900 to-blue-500/10 p-8 shadow-2xl shadow-cyan-950/30">
          <p className="text-xs font-black uppercase tracking-[0.4em] text-cyan-200">EPIC 27</p>
          <h1 className="mt-3 text-3xl font-black text-white sm:text-4xl">Corporate Sports & Employee Wellness Control Center</h1>
          <p className="mt-3 max-w-3xl text-sm text-slate-300">
            Manage company onboarding, employees, departments, teams, recurring bookings, tournaments, wellness programs, rewards, billing, contracts, CRM, and inter-company leagues.
          </p>
        </section>

        {loading ? (
          <section className="rounded-[2rem] border border-white/10 bg-white/[0.04] p-10 text-center text-sm font-bold text-slate-300">Loading corporate control plane...</section>
        ) : (
          <>
            <section className="grid gap-4 md:grid-cols-3 xl:grid-cols-6">
              {metrics.map(([label, value]) => (
                <MetricCard key={label} label={String(label)} value={String(value)} />
              ))}
            </section>

            <section className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
              <Panel title="Corporate Approval Pipeline" subtitle="Company onboarding, HR contacts, ARR potential, and approval status.">
                <div className="space-y-3">
                  {companies.slice(0, 6).map((company) => (
                    <Row key={company.id} title={String(company.name)} meta={`${company.city || "No city"} · ${company.industry || "Industry pending"} · ${company.status}`} badge={`₹${formatNumber(company.annualContractValue)}`} />
                  ))}
                  {companies.length === 0 && <Empty text="No corporate companies yet." />}
                </div>
              </Panel>

              <Panel title="Register Company" subtitle="Create a pending B2B account for approval and HR onboarding.">
                <div className="grid gap-3">
                  <Field label="Company" value={draft.name} onChange={(value) => setDraft((current) => ({ ...current, name: value }))} placeholder="Acme Sports Pvt Ltd" />
                  <Field label="City" value={draft.city || ""} onChange={(value) => setDraft((current) => ({ ...current, city: value }))} placeholder="Ahmedabad" />
                  <Field label="HR Email" value={draft.hrEmail || ""} onChange={(value) => setDraft((current) => ({ ...current, hrEmail: value }))} placeholder="hr@company.com" />
                  <Field label="Monthly Budget" value={String(draft.monthlyBudget || "")} onChange={(value) => setDraft((current) => ({ ...current, monthlyBudget: Number(value) || 0 }))} placeholder="250000" />
                  <button disabled={saving} onClick={submitCompany} className="rounded-2xl bg-cyan-300 px-5 py-3 text-sm font-black text-slate-950 disabled:opacity-60">
                    {saving ? "Creating..." : "Create Company"}
                  </button>
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 xl:grid-cols-3">
              <Panel title="Employees & Departments" subtitle="Bulk import readiness, employee profiles, department hierarchy, and participation scoring.">
                <StatLine label="Employees" value={employees.length} />
                <StatLine label="Departments" value={departments.length} />
                <StatLine label="Locations" value={locations.length} />
                <div className="mt-4 space-y-3">{employees.slice(0, 4).map((employee) => <Row key={employee.id} title={String(employee.name)} meta={`${employee.email} · score ${employee.participationScore}`} badge={`${employee.rewardBalance || 0} pts`} />)}</div>
              </Panel>
              <Panel title="Teams, Bookings & Tournaments" subtitle="Corporate teams, recurring booking plans, tournaments, and fixture readiness.">
                <StatLine label="Teams" value={teams.length} />
                <StatLine label="Recurring plans" value={bookingPlans.length} />
                <StatLine label="Tournaments" value={tournaments.length} />
                <div className="mt-4 space-y-3">{bookingPlans.slice(0, 3).map((plan) => <Row key={plan.id} title={String(plan.sport)} meta={`${plan.recurrenceType} · ${plan.generatedBookings} bookings`} badge={`₹${formatNumber(plan.monthlySpend)}`} />)}</div>
              </Panel>
              <Panel title="Wellness & Rewards" subtitle="Wellness programs, attendance, participation hours, points credit, and redemption ledger.">
                <StatLine label="Programs" value={wellnessPrograms.length} />
                <StatLine label="Activities" value={activities.length} />
                <StatLine label="Rewards" value={rewards.length} />
                <div className="mt-4 space-y-3">{wellnessPrograms.slice(0, 3).map((program) => <Row key={program.id} title={String(program.title)} meta={`${program.programType} · ${program.status}`} badge={`${program.rewardPoints || 0} pts`} />)}</div>
              </Panel>
            </section>

            <section className="grid gap-6 lg:grid-cols-2">
              <Panel title="Billing, Contracts & CRM" subtitle="Invoices, payment state, ARR, renewal probability, account health, and upsell signals.">
                <StatLine label="Invoices" value={invoices.length} />
                <StatLine label="Contracts" value={contracts.length} />
                <StatLine label="Active ARR" value={`₹${formatNumber(billing?.activeArr)}`} />
                <div className="mt-4 space-y-3">{contracts.slice(0, 4).map((contract) => <Row key={contract.id} title={String(contract.companyName || contract.contractNumber)} meta={`${contract.planName || "Plan"} · ${contract.status}`} badge={`${contract.renewalProbability || 0}% renewal`} />)}</div>
              </Panel>
              <Panel title="Analytics, Leagues & Audit" subtitle="Participation analytics, department rankings, inter-company leagues, governance controls, and audit trail.">
                <StatLine label="Wellness score" value={String(analytics?.wellnessScore ?? 0)} />
                <StatLine label="Engagement score" value={String(analytics?.employeeEngagementScore ?? 0)} />
                <StatLine label="Leagues" value={leagues.length} />
                <div className="mt-4 space-y-3">{audit.slice(0, 4).map((item) => <Row key={item.id} title={String(item.action)} meta={String(item.summary || "Corporate audit")} badge={String(item.actor || "system")} />)}</div>
              </Panel>
            </section>

            <Panel title="Governance Queue" subtitle="Highest-risk B2B controls for employee privacy, billing reconciliation, renewals, and cross-company isolation.">
              <div className="grid gap-3 md:grid-cols-2">
                {(dashboard?.priorityActions || dashboard?.governanceControls || []).slice(0, 6).map((item, index) => (
                  <div key={`${item.title}-${index}`} className="rounded-2xl border border-white/10 bg-white/[0.04] p-4">
                    <p className="text-sm font-black text-white">{item.title}</p>
                    <p className="mt-1 text-xs text-slate-400">{item.detail}</p>
                    <p className="mt-3 text-[10px] font-black uppercase tracking-[0.2em] text-cyan-200">{item.severity}</p>
                  </div>
                ))}
              </div>
            </Panel>
          </>
        )}
      </div>
    </main>
  );
}

function Panel({ title, subtitle, children }: { title: string; subtitle: string; children: React.ReactNode }) {
  return (
    <section className="rounded-[1.75rem] border border-white/10 bg-white/[0.04] p-5 shadow-xl shadow-black/20">
      <h2 className="text-lg font-black text-white">{title}</h2>
      <p className="mt-1 text-xs text-slate-400">{subtitle}</p>
      <div className="mt-4">{children}</div>
    </section>
  );
}

function MetricCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-3xl border border-cyan-400/20 bg-cyan-400/10 p-4">
      <p className="text-[10px] font-black uppercase tracking-[0.25em] text-cyan-200">{label}</p>
      <p className="mt-2 text-2xl font-black text-white">{value}</p>
    </div>
  );
}

function Field({ label, value, onChange, placeholder }: { label: string; value: string; onChange: (value: string) => void; placeholder: string }) {
  return (
    <label className="grid gap-1 text-xs font-bold text-slate-300">
      {label}
      <input value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} className="rounded-2xl border border-white/10 bg-slate-950 px-4 py-3 text-sm text-white outline-none focus:border-cyan-300" />
    </label>
  );
}

function Row({ title, meta, badge }: { title: string; meta: string; badge?: string }) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-2xl border border-white/10 bg-slate-950/60 p-3">
      <div>
        <p className="text-sm font-black text-white">{title}</p>
        <p className="text-xs text-slate-400">{meta}</p>
      </div>
      {badge && <span className="rounded-full bg-cyan-300/10 px-3 py-1 text-[10px] font-black uppercase text-cyan-200">{badge}</span>}
    </div>
  );
}

function StatLine({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="flex items-center justify-between border-b border-white/10 py-2 text-sm">
      <span className="text-slate-400">{label}</span>
      <span className="font-black text-white">{value}</span>
    </div>
  );
}

function Empty({ text }: { text: string }) {
  return <p className="rounded-2xl border border-dashed border-white/10 p-4 text-center text-xs text-slate-500">{text}</p>;
}

function formatNumber(value: unknown) {
  const number = Number(value || 0);
  return number.toLocaleString("en-IN");
}
