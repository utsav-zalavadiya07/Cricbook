"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import * as adminApi from "@/modules/admin/services/adminService";
import {
  AdminActionButton,
  AdminDataTable,
  AdminErrorState,
  AdminMetric,
  AdminMetricCard,
  AdminPageHeader,
  AdminTableRow,
  formatMoney,
} from "./AdminPrimitives";

type ApiResponse<T> = { data: T };
type LoadPart<T> = { key: string; data?: T; error?: string };

type AdminDashboardState = {
  analytics: LoadPart<adminApi.AdminExecutiveAnalytics>;
  users: LoadPart<adminApi.AdminUserCenterSummary>;
  bookings: LoadPart<adminApi.AdminBookingOpsDashboard>;
  finance: LoadPart<adminApi.AdminFinanceDashboard>;
  support: LoadPart<adminApi.AdminSupportDashboard>;
  safety: LoadPart<adminApi.AdminSafetyDashboard>;
  marketing: LoadPart<adminApi.AdminMarketingDashboard>;
  system: LoadPart<adminApi.AdminSystemDashboard>;
  governance: LoadPart<adminApi.AdminGovernanceDashboard>;
  ownerVerifications: LoadPart<unknown[]>;
  turfModerations: LoadPart<unknown[]>;
  bookingDisputes: LoadPart<adminApi.AdminBookingDispute[]>;
  refunds: LoadPart<adminApi.AdminFinanceRefund[]>;
  tickets: LoadPart<adminApi.AdminSupportTicket[]>;
};

const commandModules = [
  { href: "/admin/users", title: "Users & Owners", detail: "Verification, risk, teams, activity", epic: "EPIC 37" },
  { href: "/admin/bookings", title: "Booking Ops", detail: "Disputes, cancellations, conflicts", epic: "EPIC 40" },
  { href: "/admin/finance", title: "Finance", detail: "Transactions, refunds, payouts", epic: "EPIC 39" },
  { href: "/admin/support", title: "Support", detail: "Tickets, SLA, CSAT", epic: "EPIC 44" },
  { href: "/admin/safety", title: "Trust & Safety", detail: "Reports, media, appeals", epic: "EPIC 41" },
  { href: "/admin/roles", title: "Governance & RBAC", detail: "Roles, page access, audit controls", epic: "EPIC 17" },
  { href: "/admin/system", title: "System Control", detail: "Flags, releases, config", epic: "EPIC 45" },
];

export default function AdminDashboardV2() {
  const [state, setState] = useState<AdminDashboardState | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    const [
      analytics,
      users,
      bookings,
      finance,
      support,
      safety,
      marketing,
      system,
      governance,
      ownerVerifications,
      turfModerations,
      bookingDisputes,
      refunds,
      tickets,
    ] = await Promise.all([
      loadPart("Analytics", adminApi.getAdminAnalytics),
      loadPart("Users", adminApi.getAdminUserCenterSummary),
      loadPart("Bookings", adminApi.getAdminBookingOpsDashboard),
      loadPart("Finance", adminApi.getAdminFinanceDashboard),
      loadPart("Support", adminApi.getAdminSupportDashboard),
      loadPart("Safety", adminApi.getAdminSafetyDashboard),
      loadPart("Marketing", adminApi.getAdminMarketingDashboard),
      loadPart("System", adminApi.getAdminSystemDashboard),
      loadPart("Governance", adminApi.getAdminGovernanceDashboard),
      loadPart("Owner verification queue", adminApi.getOwnerVerifications),
      loadPart("Turf moderation queue", adminApi.getTurfModerations),
      loadPart("Booking disputes", adminApi.getAdminBookingDisputes),
      loadPart("Refund queue", adminApi.getAdminFinanceRefunds),
      loadPart("Support tickets", adminApi.getAdminSupportTickets),
    ]);

    setState({
      analytics,
      users,
      bookings,
      finance,
      support,
      safety,
      marketing,
      system,
      governance,
      ownerVerifications: normalizeUnknownArray(ownerVerifications),
      turfModerations: normalizeUnknownArray(turfModerations),
      bookingDisputes,
      refunds,
      tickets,
    });
    setLoading(false);
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const metrics = useMemo<AdminMetric[]>(() => {
    const data = state;
    return [
      {
        label: "GMV",
        value: formatMoney(data?.finance.data?.gmv ?? data?.analytics.data?.totalRevenue),
        detail: `${formatMoney(data?.finance.data?.commissionRevenue ?? data?.analytics.data?.commissionRevenue)} commission`,
        tone: "emerald",
      },
      {
        label: "Users",
        value: data?.users.data?.totalPlayers ?? data?.analytics.data?.totalUsers ?? "-",
        detail: `${data?.users.data?.pendingVerifications ?? 0} owner verifications pending`,
        tone: "sky",
      },
      {
        label: "Bookings Today",
        value: data?.bookings.data?.todaysBookings ?? data?.analytics.data?.bookingsToday ?? "-",
        detail: `${data?.bookings.data?.activeDisputes ?? 0} active booking disputes`,
        tone: "violet",
      },
      {
        label: "Refunds",
        value: formatMoney(data?.finance.data?.refundAmount ?? data?.analytics.data?.refunds),
        detail: `${data?.refunds.data?.filter((item) => item.status !== "APPROVED" && item.status !== "REJECTED").length ?? 0} queue items`,
        tone: "amber",
      },
      {
        label: "Support",
        value: data?.support.data?.openTickets ?? 0,
        detail: `${data?.support.data?.slaBreaches ?? 0} SLA breaches`,
        tone: "rose",
      },
      {
        label: "Safety",
        value: data?.safety.data?.openReports ?? 0,
        detail: `${data?.safety.data?.fraudCases ?? 0} fraud cases`,
        tone: "slate",
      },
      {
        label: "Governance",
        value: `${data?.governance.data?.governanceScore ?? data?.analytics.data?.safetyScore ?? 0}%`,
        detail: `${data?.governance.data?.riskLevel ?? "LOW"} platform risk`,
        tone: data?.governance.data?.riskLevel === "CRITICAL" || data?.governance.data?.riskLevel === "HIGH" ? "rose" : "emerald",
      },
      {
        label: "Campaigns",
        value: data?.marketing.data?.activeCampaigns ?? 0,
        detail: `${data?.marketing.data?.emailOpenRate ?? 0}% email open rate`,
        tone: "emerald",
      },
      {
        label: "System",
        value: data?.system.data?.maintenanceStatus ? "Maintenance" : "Live",
        detail: `${data?.system.data?.activeFeatures ?? 0}/${data?.system.data?.totalFeatures ?? 0} feature flags active`,
        tone: data?.system.data?.maintenanceStatus ? "amber" : "sky",
      },
    ];
  }, [state]);

  const errors = useMemo(() => Object.values(state || {}).filter((part) => part.error), [state]);
  const priorityRows = useMemo(() => buildPriorityRows(state), [state]);

  return (
    <div className="space-y-6">
      <AdminPageHeader
        eyebrow="Platform command center"
        title="Admin operations dashboard"
        description="The admin portal is now organized around operational work: verify supply, resolve booking and money issues, control safety, and monitor platform health."
        actions={
          <>
            <AdminActionButton label={loading ? "Refreshing..." : "Refresh live data"} onClick={load} disabled={loading} intent="primary" />
            <Link href="/admin/system" className="rounded-xl border border-white/10 bg-slate-950/70 px-4 py-2 text-xs font-black text-slate-200 hover:bg-white/10">
              System Controls
            </Link>
          </>
        }
      />

      {errors.length > 0 ? (
        <AdminErrorState
          message={`${errors.length} admin API group(s) failed: ${errors.map((part) => part.key).join(", ")}. Loaded modules continue to render so the portal remains usable.`}
          onRetry={load}
        />
      ) : null}

      <section className="grid gap-4 md:grid-cols-2 2xl:grid-cols-4">
        {metrics.map((metric) => <AdminMetricCard key={metric.label} {...metric} />)}
      </section>

      <section className="grid gap-4 xl:grid-cols-3">
        {commandModules.map((module) => (
          <Link
            key={module.href}
            href={module.href}
            className="rounded-3xl border border-white/10 bg-slate-900/75 p-5 shadow-xl transition hover:border-emerald-500/35 hover:bg-emerald-500/[0.04]"
          >
            <span className="rounded-full bg-emerald-500/10 px-3 py-1 text-[10px] font-black uppercase tracking-[0.18em] text-emerald-300">{module.epic}</span>
            <h2 className="mt-4 text-xl font-black text-white">{module.title}</h2>
            <p className="mt-2 text-sm text-slate-400">{module.detail}</p>
          </Link>
        ))}
      </section>

      <AdminDataTable
        title="Priority work queue"
        description="Cross-module queue for records that need admin action first."
        rows={priorityRows}
        loading={loading}
        emptyLabel="No active admin queue items."
      />

      <section className="grid gap-4 xl:grid-cols-[1.2fr_0.8fr]">
        <div className="rounded-3xl border border-white/10 bg-slate-900/75 p-5 shadow-xl">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.22em] text-slate-500">EPIC 17 control tower</p>
              <h2 className="mt-2 text-xl font-black text-white">Governance health</h2>
              <p className="mt-1 text-sm text-slate-400">RBAC, audit, refunds, support, incidents, and fraud signals in one admin view.</p>
            </div>
            <span className="rounded-full border border-emerald-500/30 bg-emerald-500/10 px-3 py-1 text-xs font-black text-emerald-200">
              Score {state?.governance.data?.governanceScore ?? 0}
            </span>
          </div>
          <div className="mt-5 grid gap-3 md:grid-cols-2">
            {(state?.governance.data?.controlHealth || []).map((control) => (
              <div key={control.name} className="rounded-2xl border border-white/10 bg-slate-950/60 p-4">
                <div className="flex items-center justify-between gap-3">
                  <h3 className="text-sm font-black text-white">{control.name}</h3>
                  <span className={`rounded-full px-2.5 py-1 text-[10px] font-black uppercase tracking-[0.16em] ${control.status === "PASSING" ? "bg-emerald-500/10 text-emerald-300" : "bg-amber-500/10 text-amber-300"}`}>
                    {control.status.replace("_", " ")}
                  </span>
                </div>
                <p className="mt-2 text-xs text-slate-400">{control.detail}</p>
              </div>
            ))}
          </div>
        </div>

        <div className="rounded-3xl border border-white/10 bg-slate-900/75 p-5 shadow-xl">
          <p className="text-xs font-black uppercase tracking-[0.22em] text-slate-500">RBAC summary</p>
          <h2 className="mt-2 text-xl font-black text-white">Access coverage</h2>
          <div className="mt-5 space-y-3">
            <AccessSummary label="Roles" value={state?.governance.data?.rbacSummary.roles ?? 0} />
            <AccessSummary label="Permissions" value={state?.governance.data?.rbacSummary.permissions ?? 0} />
            <AccessSummary label="Page Catalog" value={state?.governance.data?.rbacSummary.catalogPages ?? 0} />
            <AccessSummary label="Empty Roles" value={state?.governance.data?.rbacSummary.rolesWithoutPermissions ?? 0} />
          </div>
          <Link href="/admin/roles" className="mt-5 inline-flex rounded-xl border border-white/10 bg-slate-950/70 px-4 py-2 text-xs font-black text-slate-200 hover:bg-white/10">
            Open RBAC Matrix
          </Link>
        </div>
      </section>
    </div>
  );
}

async function loadPart<T>(key: string, request: () => Promise<ApiResponse<T>>): Promise<LoadPart<T>> {
  try {
    const response = await request();
    return { key, data: response.data };
  } catch (error) {
    return { key, error: readableError(error) };
  }
}

function normalizeUnknownArray(part: LoadPart<unknown>): LoadPart<unknown[]> {
  if (part.error) {
    return { key: part.key, error: part.error };
  }
  return { key: part.key, data: Array.isArray(part.data) ? part.data : [] };
}

function buildPriorityRows(state: AdminDashboardState | null): AdminTableRow[] {
  if (!state) {
    return [];
  }

  const ownerRows = (state.ownerVerifications.data || []).slice(0, 3).map((item, index) => {
    const record = toRecord(item);
    const id = toDisplay(record.id, index + 1);
    return {
      id,
      title: toDisplay(record.ownerName ?? record.name ?? record.businessName, "Owner verification"),
      subtitle: toDisplay(record.email ?? record.documentType ?? record.status, "Pending owner review"),
      meta: ["Owner verification", toDisplay(record.status, "PENDING")],
      status: toDisplay(record.status, "PENDING"),
      priority: "HIGH",
      updatedAt: asString(record.createdAt ?? record.updatedAt),
    } satisfies AdminTableRow;
  });

  const turfRows = (state.turfModerations.data || []).slice(0, 3).map((item, index) => {
    const record = toRecord(item);
    return {
      id: toDisplay(record.id, `turf-${index + 1}`),
      title: toDisplay(record.turfName ?? record.name, "Turf moderation"),
      subtitle: toDisplay(record.ownerName ?? record.reason, "Pending turf approval"),
      meta: ["Turf moderation", toDisplay(record.status, "PENDING")],
      status: toDisplay(record.status, "PENDING"),
      priority: "HIGH",
      updatedAt: asString(record.createdAt ?? record.updatedAt),
    } satisfies AdminTableRow;
  });

  const disputeRows = (state.bookingDisputes.data || []).slice(0, 3).map((item) => ({
    id: item.id,
    title: `Booking dispute #${item.bookingId}`,
    subtitle: item.summary || item.disputeType,
    meta: ["Booking dispute", item.disputeType, item.assignedTo || "Unassigned"],
    status: item.status,
    priority: "CRITICAL",
    updatedAt: item.createdAt,
  } satisfies AdminTableRow));

  const refundRows = (state.refunds.data || []).filter((item) => item.status !== "APPROVED" && item.status !== "REJECTED").slice(0, 3).map((item) => ({
    id: item.id,
    title: `Refund request for booking #${item.bookingId}`,
    subtitle: item.reason,
    meta: [`Payment #${item.paymentId}`, "Refund queue"],
    amount: formatMoney(item.amount),
    status: item.status,
    priority: "HIGH",
    updatedAt: item.createdAt,
  } satisfies AdminTableRow));

  const ticketRows = (state.tickets.data || []).filter((item) => item.status !== "RESOLVED" && item.status !== "CLOSED").slice(0, 3).map((item) => ({
    id: item.id,
    title: item.ticketNumber,
    subtitle: item.issue,
    meta: [item.category, item.channel, item.assignedTo || "Unassigned"],
    status: item.status,
    priority: item.priority,
    updatedAt: item.updatedAt || item.createdAt,
  } satisfies AdminTableRow));

  const governanceRows = (state.governance.data?.priorityActions || []).slice(0, 6).map((item) => ({
    id: item.label,
    title: item.label,
    subtitle: item.recommendation,
    meta: [item.owner, `${item.count} item(s)`],
    status: "ACTION_REQUIRED",
    priority: item.owner === "SECURITY" || item.owner === "FINANCE" ? "CRITICAL" : "HIGH",
  } satisfies AdminTableRow));

  return [...governanceRows, ...ownerRows, ...turfRows, ...disputeRows, ...refundRows, ...ticketRows];
}

function AccessSummary({ label, value }: { label: string; value: number }) {
  return (
    <div className="flex items-center justify-between rounded-2xl border border-white/10 bg-slate-950/60 px-4 py-3">
      <span className="text-sm font-semibold text-slate-300">{label}</span>
      <span className="text-lg font-black text-white">{value}</span>
    </div>
  );
}

function readableError(error: unknown) {
  const record = toRecord(error);
  const response = toRecord(record.response);
  const data = toRecord(response.data);
  return asString(data.message) || asString(record.message) || "Request failed";
}

function toRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === "object" ? value as Record<string, unknown> : {};
}

function asString(value: unknown) {
  return typeof value === "string" ? value : "";
}

function toDisplay(value: unknown, fallback: string | number) {
  if (typeof value === "string") {
    return value;
  }
  if (typeof value === "number") {
    return String(value);
  }
  return String(fallback);
}
