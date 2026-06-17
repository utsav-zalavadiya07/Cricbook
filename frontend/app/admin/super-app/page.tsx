"use client";

import { useEffect, useMemo, useState } from "react";
import {
  SuperAppMap,
  getAdminSuperAppAudit,
  getAdminSuperAppDashboard,
  getAdminSuperAppFamily,
  getAdminSuperAppIdentities,
  getAdminSuperAppMemberships,
  getAdminSuperAppReputation,
  getAdminSuperAppRewards,
  getAdminSuperAppTimeline,
  getAdminSuperAppWalletLedger,
  getAdminSuperAppWallets,
} from "@/modules/superapp/services/superAppService";

export default function AdminSuperAppPage() {
  const [dashboard, setDashboard] = useState<SuperAppMap | null>(null);
  const [identities, setIdentities] = useState<SuperAppMap[]>([]);
  const [wallets, setWallets] = useState<SuperAppMap[]>([]);
  const [walletLedger, setWalletLedger] = useState<SuperAppMap[]>([]);
  const [memberships, setMemberships] = useState<SuperAppMap[]>([]);
  const [rewards, setRewards] = useState<SuperAppMap[]>([]);
  const [reputation, setReputation] = useState<SuperAppMap[]>([]);
  const [timeline, setTimeline] = useState<SuperAppMap[]>([]);
  const [family, setFamily] = useState<SuperAppMap[]>([]);
  const [audit, setAudit] = useState<SuperAppMap[]>([]);
  const [loading, setLoading] = useState(true);

  async function loadSuperApp() {
    try {
      setLoading(true);
      const [dashboardRes, identitiesRes, walletsRes, ledgerRes, membershipsRes, rewardsRes, reputationRes, timelineRes, familyRes, auditRes] = await Promise.all([
        getAdminSuperAppDashboard(),
        getAdminSuperAppIdentities(),
        getAdminSuperAppWallets(),
        getAdminSuperAppWalletLedger(),
        getAdminSuperAppMemberships(),
        getAdminSuperAppRewards(),
        getAdminSuperAppReputation(),
        getAdminSuperAppTimeline(),
        getAdminSuperAppFamily(),
        getAdminSuperAppAudit(),
      ]);
      setDashboard(dashboardRes.data);
      setIdentities(identitiesRes.data);
      setWallets(walletsRes.data);
      setWalletLedger(ledgerRes.data);
      setMemberships(membershipsRes.data);
      setRewards(rewardsRes.data);
      setReputation(reputationRes.data);
      setTimeline(timelineRes.data);
      setFamily(familyRes.data);
      setAudit(auditRes.data);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void loadSuperApp(), 0);
    return () => window.clearTimeout(timer);
  }, []);

  const metrics = useMemo(
    () => [
      { label: "Sports IDs", value: formatNumber(dashboard?.kpis?.sportsIdentities), detail: `${formatNumber(dashboard?.kpis?.activeIdentities)} active identities` },
      { label: "Passports", value: formatNumber(dashboard?.kpis?.publicPassports), detail: "public share links" },
      { label: "Wallets", value: formatNumber(dashboard?.kpis?.walletAccounts), detail: "universal balance accounts" },
      { label: "Memberships", value: formatNumber(dashboard?.kpis?.activeMemberships), detail: "active unified plans" },
      { label: "Rewards", value: formatNumber(dashboard?.kpis?.rewardAccounts), detail: "reward accounts" },
      { label: "Family Links", value: formatNumber(dashboard?.kpis?.familyLinks), detail: "parent/youth controls" },
      { label: "Avg Reputation", value: `${formatNumber(dashboard?.kpis?.averageReputation)}/100`, detail: "trust scoring" },
      { label: "Ledger Events", value: formatNumber(walletLedger.length), detail: "wallet transactions" },
    ],
    [dashboard, walletLedger.length]
  );

  return (
    <main className="min-h-screen bg-slate-950 px-4 py-8 text-white sm:px-8" data-i18n-skip="true">
      <div className="mx-auto max-w-7xl space-y-8">
        <header className="rounded-[2rem] border border-white/10 bg-gradient-to-br from-emerald-500/15 via-slate-900 to-cyan-500/10 p-6 shadow-2xl">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.26em] text-emerald-300">EPIC 33 Sports Super App</p>
              <h1 className="mt-3 text-3xl font-black text-white sm:text-4xl">Digital Identity & Unified Membership Control Center</h1>
              <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-300">
                Govern one Sports ID, one wallet, one membership, one rewards balance, one reputation score, one sports passport, and one timeline across all product modules.
              </p>
            </div>
            <button onClick={() => void loadSuperApp()} className="rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-emerald-300">
              Refresh Super App OS
            </button>
          </div>
        </header>

        {loading ? (
          <section className="rounded-[2rem] border border-white/10 bg-white/[0.04] p-10 text-center text-sm font-bold text-slate-300">Loading Super App control plane...</section>
        ) : (
          <>
            <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              {metrics.map((metric) => (
                <MetricCard key={metric.label} {...metric} />
              ))}
            </section>

            <section className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
              <Panel title="Sports Identity Registry" subtitle="Global Sports IDs, cross-module visibility, reputation, profile visibility, age group, and linked modules.">
                <DataTable
                  rows={identities}
                  columns={[
                    ["sportsId", "Sports ID"],
                    ["displayName", "Name"],
                    ["primarySport", "Sport"],
                    ["city", "City"],
                    ["reputationScore", "Rep"],
                    ["status", "Status"],
                  ]}
                  empty="No Sports Identities yet."
                />
              </Panel>

              <Panel title="Governance Controls" subtitle="Highest-risk controls for privacy, double-spend prevention, uniqueness and youth consent.">
                <div className="space-y-3">
                  {(dashboard?.governanceControls || dashboard?.priorityControls || []).map((item: SuperAppMap) => (
                    <CompactRow key={String(item.name || item.control)} title={String(item.name || item.control)} meta={String(item.risk || item.detail)} status={String(item.status)} />
                  ))}
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 lg:grid-cols-3">
              <Panel title="Universal Wallets" subtitle="Balance accounts and double-spend-safe wallet ledger.">
                <DataTable rows={wallets} columns={[["identityId", "Identity"], ["balance", "Balance"], ["lockedAmount", "Locked"], ["status", "Status"]]} empty="No wallet accounts yet." />
              </Panel>

              <Panel title="Unified Memberships" subtitle="One plan across bookings, tournaments, academy, commerce and marketplace benefits.">
                <DataTable rows={memberships} columns={[["identityId", "Identity"], ["tier", "Tier"], ["billingCycle", "Cycle"], ["status", "Status"]]} empty="No unified memberships yet." />
              </Panel>

              <Panel title="Rewards & Reputation" subtitle="Universal points balance plus reputation events from all modules.">
                <DataTable rows={rewards} columns={[["identityId", "Identity"], ["pointsBalance", "Balance"], ["lifetimeEarned", "Earned"], ["lifetimeRedeemed", "Redeemed"]]} empty="No reward accounts yet." />
                <div className="mt-5">
                  <DataTable rows={reputation.slice(0, 6)} columns={[["eventType", "Event"], ["sourceModule", "Module"], ["impact", "Impact"], ["scoreAfter", "Score"]]} empty="No reputation events yet." />
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
              <Panel title="Wallet Ledger" subtitle="Immutable universal wallet transaction history for credits, debits, transfers and module references.">
                <DataTable rows={walletLedger} columns={[["transactionType", "Type"], ["sourceModule", "Module"], ["amount", "Amount"], ["balanceAfter", "Balance"], ["referenceType", "Ref"]]} empty="No wallet ledger rows yet." />
              </Panel>

              <Panel title="Timeline & Family" subtitle="Sports passport timeline plus family/youth account relationships.">
                <DataTable rows={timeline.slice(0, 8)} columns={[["title", "Title"], ["sourceModule", "Module"], ["eventType", "Event"], ["visibility", "Visibility"]]} empty="No timeline events yet." />
                <div className="mt-5">
                  <DataTable rows={family.slice(0, 8)} columns={[["parentIdentityId", "Parent"], ["childIdentityId", "Child"], ["guardianEmail", "Guardian"], ["status", "Status"]]} empty="No family links yet." />
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 lg:grid-cols-2">
              <Panel title="Cross-Module Signals" subtitle="Current integration signals from bookings, tournaments, career and federation modules.">
                <div className="grid gap-3 sm:grid-cols-2">
                  {Object.entries(dashboard?.crossModuleSignals || {}).map(([key, value]) => (
                    <MetricCard key={key} label={labelize(key)} value={Array.isArray(value) ? value.length.toLocaleString("en-IN") : formatNumber(value)} detail={Array.isArray(value) ? value.join(", ") : "linked module count"} compact />
                  ))}
                </div>
              </Panel>

              <Panel title="Audit Trail" subtitle="Identity, passport, wallet, membership, rewards, reputation, family and timeline changes.">
                <DataTable rows={audit} columns={[["action", "Action"], ["entityType", "Entity"], ["actor", "Actor"], ["details", "Details"]]} empty="No Super App audit events yet." />
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
      <p className="mt-2 text-xs leading-5 text-slate-400">{detail}</p>
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

function DataTable({ rows, columns, empty }: { rows: SuperAppMap[]; columns: Array<[string, string]>; empty: string }) {
  if (!rows.length) {
    return <p className="rounded-2xl border border-dashed border-white/10 p-5 text-center text-sm text-slate-400">{empty}</p>;
  }
  return (
    <div className="overflow-hidden rounded-2xl border border-white/10">
      <div className="overflow-x-auto">
        <table className="min-w-full text-left text-sm">
          <thead className="bg-slate-950/80 text-[10px] uppercase tracking-[0.18em] text-slate-500">
            <tr>
              {columns.map(([, label]) => (
                <th key={label} className="px-4 py-3 font-black">{label}</th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-white/10">
            {rows.slice(0, 12).map((row, index) => (
              <tr key={`${String(row.id || index)}-${index}`} className="bg-white/[0.02]">
                {columns.map(([key]) => (
                  <td key={key} className="max-w-[220px] truncate px-4 py-3 text-slate-200">{formatCell(row[key])}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function formatCell(value: unknown) {
  if (value == null || value === "") {
    return "--";
  }
  if (typeof value === "number") {
    return value.toLocaleString("en-IN");
  }
  return String(value);
}

function formatNumber(value: unknown) {
  const numberValue = Number(value || 0);
  return Number.isFinite(numberValue) ? numberValue.toLocaleString("en-IN") : "0";
}

function labelize(value: string) {
  return value.replace(/([A-Z])/g, " $1").replace(/[_-]/g, " ").replace(/^./, (letter) => letter.toUpperCase()).trim();
}
