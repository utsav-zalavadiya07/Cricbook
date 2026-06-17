"use client";

import { useEffect, useMemo, useState } from "react";
import {
  AdminMonetizationDashboard,
  AdminMonetizationMap,
  AdminMonetizationReports,
  getAdminMonetizationBrands,
  getAdminMonetizationCampaigns,
  getAdminMonetizationContracts,
  getAdminMonetizationDashboard,
  getAdminMonetizationInvoices,
  getAdminMonetizationLeads,
  getAdminMonetizationOpportunities,
  getAdminMonetizationProposals,
  getAdminMonetizationReports,
} from "@/modules/admin/services/adminService";

export default function AdminMonetizationPage() {
  const [dashboard, setDashboard] = useState<AdminMonetizationDashboard | null>(null);
  const [brands, setBrands] = useState<AdminMonetizationMap[]>([]);
  const [opportunities, setOpportunities] = useState<AdminMonetizationMap[]>([]);
  const [proposals, setProposals] = useState<AdminMonetizationMap[]>([]);
  const [campaigns, setCampaigns] = useState<AdminMonetizationMap[]>([]);
  const [leads, setLeads] = useState<AdminMonetizationMap[]>([]);
  const [contracts, setContracts] = useState<AdminMonetizationMap[]>([]);
  const [invoices, setInvoices] = useState<AdminMonetizationMap[]>([]);
  const [reports, setReports] = useState<AdminMonetizationReports | null>(null);
  const [loading, setLoading] = useState(true);

  async function loadMonetization() {
    try {
      setLoading(true);
      const [dashboardRes, brandsRes, opportunitiesRes, proposalsRes, campaignsRes, leadsRes, contractsRes, invoicesRes, reportsRes] = await Promise.all([
        getAdminMonetizationDashboard(),
        getAdminMonetizationBrands(),
        getAdminMonetizationOpportunities(),
        getAdminMonetizationProposals(),
        getAdminMonetizationCampaigns(),
        getAdminMonetizationLeads(),
        getAdminMonetizationContracts(),
        getAdminMonetizationInvoices(),
        getAdminMonetizationReports(),
      ]);
      setDashboard(dashboardRes.data);
      setBrands(brandsRes.data);
      setOpportunities(opportunitiesRes.data);
      setProposals(proposalsRes.data);
      setCampaigns(campaignsRes.data);
      setLeads(leadsRes.data);
      setContracts(contractsRes.data);
      setInvoices(invoicesRes.data);
      setReports(reportsRes.data);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void loadMonetization(), 0);
    return () => window.clearTimeout(timer);
  }, []);

  const metrics = useMemo(
    () => [
      { label: "Paid Revenue", value: formatCurrency(dashboard?.kpis?.paidRevenue), detail: "collected sponsorship and ad invoices" },
      { label: "Pipeline", value: formatCurrency(dashboard?.kpis?.sponsorshipPipeline), detail: "submitted, countered, accepted proposals" },
      { label: "Brands", value: formatNumber(dashboard?.kpis?.brands), detail: `${formatNumber(dashboard?.kpis?.verifiedBrands)} verified` },
      { label: "Open Opportunities", value: formatNumber(dashboard?.kpis?.openOpportunities), detail: "marketplace supply" },
      { label: "Active Campaigns", value: formatNumber(dashboard?.kpis?.activeCampaigns), detail: "banner, sponsored content, promotion" },
      { label: "Leads", value: formatNumber(dashboard?.kpis?.leads), detail: `${formatNumber(dashboard?.kpis?.convertedLeads)} converted` },
      { label: "CTR", value: `${formatNumber(dashboard?.kpis?.ctr)}%`, detail: `${formatNumber(dashboard?.kpis?.clicks)} clicks / ${formatNumber(dashboard?.kpis?.impressions)} impressions` },
      { label: "ROI", value: `${formatNumber(dashboard?.kpis?.roi)}%`, detail: "paid revenue vs campaign spend" },
    ],
    [dashboard]
  );

  return (
    <main className="min-h-screen bg-slate-950 px-4 py-8 text-white sm:px-8" data-i18n-skip="true">
      <div className="mx-auto max-w-7xl space-y-8">
        <header className="rounded-[2rem] border border-white/10 bg-gradient-to-br from-amber-500/15 via-slate-900 to-emerald-500/10 p-6 shadow-2xl">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.26em] text-amber-300">EPIC 23 Monetization Marketplace</p>
              <h1 className="mt-3 text-3xl font-black text-white sm:text-4xl">Sponsorship & Advertising Control Center</h1>
              <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-300">
                Govern brand partners, sponsorship marketplace supply, proposals, sponsored campaigns, leads, contracts, invoices, ad tracking, ROI, and brand-safety queues.
              </p>
            </div>
            <button onClick={() => void loadMonetization()} className="rounded-2xl bg-amber-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-amber-300">
              Refresh Monetization
            </button>
          </div>
        </header>

        {loading ? (
          <section className="rounded-[2rem] border border-white/10 bg-white/[0.04] p-10 text-center text-sm font-bold text-slate-300">Loading monetization control plane...</section>
        ) : (
          <>
            <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              {metrics.map((metric) => (
                <MetricCard key={metric.label} {...metric} />
              ))}
            </section>

            <section className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
              <Panel title="Sponsorship Marketplace" subtitle="Tournament, team, player, and featured turf sponsorship inventory.">
                <DataTable
                  rows={opportunities.length ? opportunities : dashboard?.marketplace || []}
                  columns={[
                    ["title", "Opportunity"],
                    ["opportunityType", "Type"],
                    ["city", "City"],
                    ["audienceSize", "Audience"],
                    ["amount", "Amount"],
                    ["status", "Status"],
                  ]}
                  empty="No sponsorship opportunities yet."
                />
              </Panel>

              <Panel title="Brand Partner Pipeline" subtitle="Brand registration, verification, documents, and marketplace readiness.">
                <div className="space-y-3">
                  {(brands.length ? brands : dashboard?.brandPipeline || []).slice(0, 7).map((item) => (
                    <CompactRow key={String(item.id)} title={String(item.companyName || "Brand")} meta={`${String(item.brandType || "LOCAL_BUSINESS")} | ${String(item.city || "All cities")} | ${String(item.email || "")}`} status={String(item.status || "PENDING")} />
                  ))}
                  {brands.length === 0 ? <p className="text-sm text-slate-400">No brand partners registered yet.</p> : null}
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 lg:grid-cols-3">
              <Panel title="Proposal Board" subtitle="Proposal submissions, counter offers, accept/reject decisions, and contracts.">
                <div className="space-y-3">
                  {(proposals.length ? proposals : dashboard?.proposalBoard || []).slice(0, 6).map((item) => (
                    <CompactRow key={String(item.id)} title={String(item.brandName || "Brand")} meta={`${String(item.opportunityTitle || "Opportunity")} | ${formatCurrency(item.proposedAmount)}`} status={String(item.status || "SUBMITTED")} />
                  ))}
                  {proposals.length === 0 ? <p className="text-sm text-slate-400">No sponsorship proposals yet.</p> : null}
                </div>
              </Panel>

              <Panel title="Campaign Performance" subtitle="Sponsored content, banner ads, featured listings, impressions, clicks, conversions, and spend.">
                <div className="space-y-3">
                  {(campaigns.length ? campaigns : dashboard?.campaignPerformance || []).slice(0, 6).map((item) => (
                    <CompactRow key={String(item.id)} title={String(item.name || "Campaign")} meta={`${String(item.placement || "HOMEPAGE")} | ${formatNumber(item.impressions)} impressions | ${formatNumber(item.clicks)} clicks`} status={String(item.status || "DRAFT")} />
                  ))}
                  {campaigns.length === 0 ? <p className="text-sm text-slate-400">No sponsored campaigns yet.</p> : null}
                </div>
              </Panel>

              <Panel title="Lead Funnel" subtitle="Player, team, organizer, and owner leads generated by campaigns.">
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-1">
                  {Object.entries(dashboard?.leadFunnel || reports?.leadFunnel || {}).map(([status, count]) => (
                    <MetricCard key={status} label={status} value={formatNumber(count)} detail="lead lifecycle count" compact />
                  ))}
                  {Object.keys(dashboard?.leadFunnel || reports?.leadFunnel || {}).length === 0 ? <p className="text-sm text-slate-400">No lead funnel data yet.</p> : null}
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
              <Panel title="Contracts & Billing" subtitle="Signed contracts, invoice status, GST, paid revenue, and reconciliation queues.">
                <div className="grid gap-4 lg:grid-cols-2">
                  <div className="space-y-3">
                    <h3 className="text-xs font-black uppercase tracking-[0.2em] text-amber-300">Contracts</h3>
                    {contracts.slice(0, 5).map((item) => (
                      <CompactRow key={String(item.id)} title={String(item.contractNumber || "Contract")} meta={`${String(item.brandName || "Brand")} | ${formatCurrency(item.value)}`} status={String(item.status || "DRAFT")} />
                    ))}
                    {contracts.length === 0 ? <p className="text-sm text-slate-400">No contracts yet.</p> : null}
                  </div>
                  <div className="space-y-3">
                    <h3 className="text-xs font-black uppercase tracking-[0.2em] text-emerald-300">Invoices</h3>
                    {invoices.slice(0, 5).map((item) => (
                      <CompactRow key={String(item.id)} title={String(item.invoiceNumber || "Invoice")} meta={`${String(item.revenueCategory || "SPONSORSHIP")} | ${formatCurrency(item.totalAmount)}`} status={String(item.status || "PENDING")} />
                    ))}
                    {invoices.length === 0 ? <p className="text-sm text-slate-400">No invoices yet.</p> : null}
                  </div>
                </div>
              </Panel>

              <Panel title="Monetization Analytics" subtitle="Revenue mix, ad funnel, conversion attribution, and brand-safety controls.">
                <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                  {Object.entries(dashboard?.revenueBreakdown || reports?.revenue || {}).map(([category, value]) => (
                    <MetricCard key={category} label={category} value={formatCurrency(value)} detail="paid invoice revenue" compact />
                  ))}
                </div>
                <div className="mt-5 grid gap-3 sm:grid-cols-3">
                  <MetricCard label="Impressions" value={formatNumber(reports?.adFunnel?.impressions ?? dashboard?.kpis?.impressions)} detail="tracked ad views" compact />
                  <MetricCard label="Clicks" value={formatNumber(reports?.adFunnel?.clicks ?? dashboard?.kpis?.clicks)} detail="tracked ad clicks" compact />
                  <MetricCard label="Conversions" value={formatNumber(reports?.adFunnel?.conversions ?? dashboard?.kpis?.conversions)} detail="attributed outcomes" compact />
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 lg:grid-cols-2">
              <Panel title="Lead Queue" subtitle="Lead assignment, status movement, and conversion readiness.">
                <DataTable
                  rows={leads}
                  columns={[
                    ["name", "Lead"],
                    ["leadType", "Type"],
                    ["brandName", "Brand"],
                    ["estimatedValue", "Value"],
                    ["status", "Status"],
                  ]}
                  empty="No sponsor leads yet."
                />
              </Panel>

              <Panel title="Priority Actions" subtitle="Operational queue for verification, proposal decisions, contracts, billing, and marketplace supply.">
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
        <span className="rounded-full border border-amber-400/20 bg-amber-400/10 px-3 py-1 text-[10px] font-black uppercase tracking-[0.16em] text-amber-200">{status}</span>
      </div>
      <p className="mt-2 text-xs leading-5 text-slate-400">{meta}</p>
    </div>
  );
}

function DataTable({ rows, columns, empty }: { rows: AdminMonetizationMap[]; columns: Array<[string, string]>; empty: string }) {
  if (rows.length === 0) {
    return <p className="py-6 text-center text-sm text-slate-400">{empty}</p>;
  }
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[720px] text-left text-sm">
        <thead className="text-xs uppercase tracking-[0.16em] text-slate-500">
          <tr>
            {columns.map(([, label]) => (
              <th key={label} className="border-b border-white/10 px-3 py-3 font-black">{label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.slice(0, 10).map((row, index) => (
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

function formatCell(value: AdminMonetizationMap[string]) {
  if (value === null || value === undefined || value === "") {
    return "--";
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
