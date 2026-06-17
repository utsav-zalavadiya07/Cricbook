"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import api from "@/services/api";
import {
  AdminEnterpriseCenter,
  AdminEnterpriseDashboard,
  AdminEnterpriseFranchise,
  AdminEnterpriseGovernance,
  AdminEnterpriseReports,
  AdminEnterpriseStaff,
  getAdminEnterpriseCenters,
  getAdminEnterpriseDashboard,
  getAdminEnterpriseFranchises,
  getAdminEnterpriseGovernance,
  getAdminEnterpriseReports,
  getAdminEnterpriseStaff,
} from "@/modules/admin/services/adminService";

type BrandingState = {
  logoUrl: string;
  primaryColor: string;
  secondaryColor: string;
};

const defaultBranding: BrandingState = {
  logoUrl: "/assets/brand-default.png",
  primaryColor: "222.2 47.4% 11.2%",
  secondaryColor: "142.1 76.2% 36.3%",
};

export default function AdminExpansionPage() {
  const [dashboard, setDashboard] = useState<AdminEnterpriseDashboard | null>(null);
  const [centers, setCenters] = useState<AdminEnterpriseCenter[]>([]);
  const [staff, setStaff] = useState<AdminEnterpriseStaff[]>([]);
  const [franchises, setFranchises] = useState<AdminEnterpriseFranchise[]>([]);
  const [reports, setReports] = useState<AdminEnterpriseReports | null>(null);
  const [governance, setGovernance] = useState<AdminEnterpriseGovernance | null>(null);
  const [branding, setBranding] = useState<BrandingState>(defaultBranding);
  const [loading, setLoading] = useState(true);
  const [savingBranding, setSavingBranding] = useState(false);

  async function fetchEnterpriseData() {
    try {
      setLoading(true);
      const [dashboardRes, centersRes, staffRes, franchiseRes, reportsRes, governanceRes, brandingRes] = await Promise.all([
        getAdminEnterpriseDashboard(),
        getAdminEnterpriseCenters(),
        getAdminEnterpriseStaff(),
        getAdminEnterpriseFranchises(),
        getAdminEnterpriseReports(),
        getAdminEnterpriseGovernance(),
        api.get("/admin/whitelabel/branding", { params: { tenantId: 1 } }).catch(() => null),
      ]);

      setDashboard(dashboardRes.data);
      setCenters(centersRes.data);
      setStaff(staffRes.data);
      setFranchises(franchiseRes.data);
      setReports(reportsRes.data);
      setGovernance(governanceRes.data);

      if (brandingRes?.data) {
        setBranding({
          logoUrl: brandingRes.data.logoUrl || defaultBranding.logoUrl,
          primaryColor: brandingRes.data.primaryColor || defaultBranding.primaryColor,
          secondaryColor: brandingRes.data.secondaryColor || defaultBranding.secondaryColor,
        });
      }
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void fetchEnterpriseData(), 0);
    return () => window.clearTimeout(timer);
  }, []);

  const kpis = dashboard?.kpis;
  const controls = governance?.controls || dashboard?.governance?.controls || [];
  const settings = governance?.settings || dashboard?.governance?.settings || [];
  const insights = dashboard?.insights || [];

  const metricCards = useMemo(
    () => [
      { label: "Businesses", value: formatNumber(kpis?.totalBusinesses), detail: "enterprise entities" },
      { label: "Centers", value: formatNumber(kpis?.totalCenters), detail: "city and branch operations" },
      { label: "Turfs", value: formatNumber(kpis?.totalTurfs), detail: `${formatNumber(kpis?.activeTurfs)} active` },
      { label: "Staff", value: formatNumber(kpis?.totalStaff), detail: "mapped enterprise roles" },
      { label: "Revenue", value: formatCurrency(kpis?.totalRevenue), detail: "successful payment roll-up" },
      { label: "Occupancy", value: `${formatNumber(kpis?.occupancyPercent)}%`, detail: "cross-center utilization" },
      { label: "Customers", value: formatNumber(kpis?.customerCount), detail: "consolidated CRM profiles" },
      { label: "Franchises", value: formatNumber(kpis?.totalFranchises), detail: "agreement lifecycle" },
    ],
    [kpis]
  );

  const handleSaveBranding = async (event: FormEvent) => {
    event.preventDefault();
    try {
      setSavingBranding(true);
      await api.post("/admin/whitelabel/branding", {
        tenantId: 1,
        logoUrl: branding.logoUrl,
        primaryColor: branding.primaryColor,
        secondaryColor: branding.secondaryColor,
      });
    } finally {
      setSavingBranding(false);
    }
  };

  return (
    <main className="min-h-screen bg-slate-950 px-4 py-8 text-white sm:px-8" data-i18n-skip="true">
      <div className="mx-auto max-w-7xl space-y-8">
        <header className="rounded-[2rem] border border-white/10 bg-gradient-to-br from-emerald-500/15 via-slate-900 to-indigo-500/10 p-6 shadow-2xl">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.26em] text-emerald-300">EPIC 20 Enterprise SaaS</p>
              <h1 className="mt-3 text-3xl font-black text-white sm:text-4xl">Multi-Center Franchise Control Tower</h1>
              <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-300">
                Govern business entities, centers, franchises, staff hierarchy, revenue roll-ups, CRM aggregation, and cross-center analytics from one enterprise workspace.
              </p>
            </div>
            <div className="flex flex-wrap gap-3">
              <Link href="/admin/analytics" className="rounded-2xl border border-white/10 bg-white/10 px-4 py-3 text-sm font-black text-white hover:bg-white/15">
                BI Dashboard
              </Link>
              <button
                type="button"
                onClick={() => void fetchEnterpriseData()}
                className="rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-black text-white shadow-lg shadow-emerald-950/40 hover:bg-emerald-400"
              >
                Refresh Enterprise Data
              </button>
            </div>
          </div>
        </header>

        {loading ? (
          <section className="rounded-[2rem] border border-white/10 bg-white/[0.04] p-10 text-center text-sm font-bold text-slate-300">Loading enterprise hierarchy...</section>
        ) : (
          <>
            <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              {metricCards.map((card) => (
                <EnterpriseMetric key={card.label} label={card.label} value={card.value} detail={card.detail} />
              ))}
            </section>

            <section className="grid gap-6 xl:grid-cols-[1.4fr_0.9fr]">
              <EnterprisePanel title="Center Performance Ranking" subtitle="Revenue, occupancy, turf count, and manager coverage by center.">
                <div className="overflow-x-auto">
                  <table className="w-full min-w-[720px] text-left text-sm">
                    <thead className="text-xs uppercase tracking-[0.16em] text-slate-500">
                      <tr>
                        <th className="pb-3">Center</th>
                        <th className="pb-3">City</th>
                        <th className="pb-3">Turfs</th>
                        <th className="pb-3">Bookings</th>
                        <th className="pb-3">Occupancy</th>
                        <th className="pb-3">Revenue</th>
                        <th className="pb-3">Manager</th>
                      </tr>
                    </thead>
                    <tbody>
                      {centers.map((center) => (
                        <tr key={String(center.id)} className="border-t border-white/10 text-slate-300">
                          <td className="py-4 font-black text-white">{String(center.title || "Center")}</td>
                          <td className="py-4">{String(center.city || "Unmapped")}</td>
                          <td className="py-4">{formatNumber(center.turfs)}</td>
                          <td className="py-4">{formatNumber(center.bookings)}</td>
                          <td className="py-4">{formatNumber(center.occupancyPercent)}%</td>
                          <td className="py-4 font-black text-emerald-300">{formatCurrency(center.revenue)}</td>
                          <td className="py-4">{String(center.managerName || "Needs assignment")}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                {centers.length === 0 ? <p className="py-8 text-center text-sm text-slate-400">No centers are available yet.</p> : null}
              </EnterprisePanel>

              <EnterprisePanel title="Governance Controls" subtitle={`Status ${governance?.status || dashboard?.governance?.status || "WATCH"} with score ${governance?.score ?? dashboard?.governance?.score ?? 0}.`}>
                <div className="space-y-3">
                  {controls.map((control) => (
                    <div key={control.name} className="rounded-2xl border border-white/10 bg-slate-950/60 p-4">
                      <div className="flex items-center justify-between gap-3">
                        <p className="font-black text-white">{control.name}</p>
                        <span className={`rounded-full px-3 py-1 text-[10px] font-black uppercase tracking-[0.18em] ${control.status === "PASS" ? "bg-emerald-500/15 text-emerald-300" : "bg-amber-500/15 text-amber-300"}`}>
                          {control.status}
                        </span>
                      </div>
                      <p className="mt-2 text-xs leading-5 text-slate-400">{control.detail}</p>
                    </div>
                  ))}
                </div>
              </EnterprisePanel>
            </section>

            <section className="grid gap-6 lg:grid-cols-3">
              <EnterprisePanel title="Staff Hierarchy" subtitle="Enterprise owner, regional, center, finance, support, and ground staff mappings.">
                <div className="space-y-3">
                  {staff.slice(0, 6).map((member) => (
                    <CompactRow
                      key={String(member.id)}
                      title={String(member.title || "Staff member")}
                      meta={`${String(member.role || "STAFF")} | ${String(member.centerName || "All centers")} | score ${formatNumber(member.kpiScore)}`}
                      status={String(member.status || "ACTIVE")}
                    />
                  ))}
                  {staff.length === 0 ? <p className="text-sm text-slate-400">No staff assignments yet.</p> : null}
                </div>
              </EnterprisePanel>

              <EnterprisePanel title="Franchise Operations" subtitle="Agreement lifecycle, revenue share, SLA, and compliance readiness.">
                <div className="space-y-3">
                  {franchises.slice(0, 6).map((franchise) => (
                    <CompactRow
                      key={String(franchise.id)}
                      title={String(franchise.title || "Franchise")}
                      meta={`${String(franchise.city || "Multi-city")} | ${formatCurrency(franchise.revenue)} | share ${formatNumber(franchise.revenueSharePercent)}%`}
                      status={String(franchise.status || "PENDING")}
                    />
                  ))}
                  {franchises.length === 0 ? <p className="text-sm text-slate-400">No franchise agreements yet.</p> : null}
                </div>
              </EnterprisePanel>

              <EnterprisePanel title="Enterprise CRM" subtitle="Unified customer view across centers.">
                <div className="grid gap-3">
                  <EnterpriseMetric label="Repeat Customers" value={formatNumber(dashboard?.crm?.repeatCustomers)} detail="returning customer base" compact />
                  <EnterpriseMetric label="Multi-Center Customers" value={formatNumber(dashboard?.crm?.multiCenterCustomers)} detail="cross-location journeys" compact />
                  <EnterpriseMetric label="Favorite Centers" value={formatNumber(dashboard?.crm?.favoriteCenters)} detail="CRM preference signals" compact />
                </div>
              </EnterprisePanel>
            </section>

            <section className="grid gap-6 xl:grid-cols-[0.8fr_1.2fr]">
              <EnterprisePanel title="Global Settings & Overrides" subtitle="Enterprise rules with center-specific precedence.">
                <div className="space-y-3">
                  {settings.map((setting) => (
                    <CompactRow key={setting.key} title={setting.key.replaceAll("_", " ")} meta={setting.description} status={setting.precedence} />
                  ))}
                </div>
              </EnterprisePanel>

              <EnterprisePanel title="Reports & Exports" subtitle="Revenue, occupancy, staff, and customer growth reporting.">
                <div className="grid gap-3 md:grid-cols-3">
                  {(reports?.exports || []).map((exportItem) => (
                    <div key={String(exportItem.fileName)} className="rounded-2xl border border-white/10 bg-slate-950/50 p-4">
                      <p className="text-xs font-black uppercase tracking-[0.2em] text-emerald-300">{String(exportItem.format)}</p>
                      <p className="mt-2 break-words text-sm font-black text-white">{String(exportItem.fileName)}</p>
                      <p className="mt-2 text-xs text-slate-500">{String(exportItem.status || "READY")}</p>
                    </div>
                  ))}
                </div>
              </EnterprisePanel>
            </section>

            <section className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
              <EnterprisePanel title="Enterprise Insights" subtitle="Automation priorities for multi-center scaling.">
                <div className="grid gap-3 md:grid-cols-2">
                  {insights.map((insight) => (
                    <div key={insight.title} className="rounded-2xl border border-white/10 bg-slate-950/50 p-4">
                      <p className="text-sm font-black text-white">{insight.title}</p>
                      <p className="mt-2 text-xs leading-5 text-slate-400">{insight.message}</p>
                      <p className="mt-3 text-[10px] font-black uppercase tracking-[0.2em] text-emerald-300">{insight.severity}</p>
                    </div>
                  ))}
                </div>
              </EnterprisePanel>

              <EnterprisePanel title="White-Label Branding" subtitle="Keep franchise and enterprise owner surfaces brand-ready.">
                <form onSubmit={handleSaveBranding} className="space-y-4">
                  <BrandingInput label="Logo URL" value={branding.logoUrl} onChange={(logoUrl) => setBranding((prev) => ({ ...prev, logoUrl }))} />
                  <BrandingInput label="Primary Color HSL" value={branding.primaryColor} onChange={(primaryColor) => setBranding((prev) => ({ ...prev, primaryColor }))} />
                  <BrandingInput label="Secondary Color HSL" value={branding.secondaryColor} onChange={(secondaryColor) => setBranding((prev) => ({ ...prev, secondaryColor }))} />
                  <button type="submit" disabled={savingBranding} className="w-full rounded-2xl bg-indigo-500 px-4 py-3 text-sm font-black text-white hover:bg-indigo-400 disabled:cursor-wait disabled:opacity-70">
                    {savingBranding ? "Saving Branding..." : "Apply Branding"}
                  </button>
                </form>
              </EnterprisePanel>
            </section>
          </>
        )}
      </div>
    </main>
  );
}

function EnterprisePanel({ title, subtitle, children }: { title: string; subtitle: string; children: React.ReactNode }) {
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

function EnterpriseMetric({ label, value, detail, compact = false }: { label: string; value: string; detail: string; compact?: boolean }) {
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
    <div className="rounded-2xl border border-white/10 bg-slate-950/50 p-4">
      <div className="flex items-start justify-between gap-3">
        <p className="font-black text-white">{title}</p>
        <span className="shrink-0 rounded-full bg-white/10 px-3 py-1 text-[10px] font-black uppercase tracking-[0.16em] text-slate-300">{status}</span>
      </div>
      <p className="mt-2 text-xs leading-5 text-slate-400">{meta}</p>
    </div>
  );
}

function BrandingInput({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <label className="block">
      <span className="text-xs font-black uppercase tracking-[0.18em] text-slate-500">{label}</span>
      <input
        type="text"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-2 w-full rounded-2xl border border-white/10 bg-slate-950 px-4 py-3 text-sm font-semibold text-white outline-none transition focus:border-emerald-400"
      />
    </label>
  );
}

function formatCurrency(value: unknown) {
  return `Rs ${toNumber(value).toLocaleString("en-IN")}`;
}

function formatNumber(value: unknown) {
  return toNumber(value).toLocaleString("en-IN", { maximumFractionDigits: 2 });
}

function toNumber(value: unknown) {
  const numericValue = Number(value ?? 0);
  return Number.isFinite(numericValue) ? numericValue : 0;
}
