"use client";

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
  AdminToolbar,
  formatAdminDate,
  formatMoney,
} from "./AdminPrimitives";

export type AdminResourceKey = "users" | "turfs" | "bookings" | "finance" | "safety" | "support" | "marketing" | "roles" | "system" | "analytics";

type ApiResponse<T> = { data: T };
type LoadPart<T> = { key: string; data?: T; error?: string };
type ResourceDataset = { key: string; title: string; description: string; rows: AdminTableRow[] };
type ResourceState = { metrics: AdminMetric[]; datasets: ResourceDataset[]; errors: string[] };

const moduleCopy: Record<AdminResourceKey, { eyebrow: string; title: string; description: string }> = {
  users: {
    eyebrow: "EPIC 37 - User control",
    title: "User & owner management center",
    description: "Search users, review owner verification, monitor teams, and take risk actions from one operational grid.",
  },
  turfs: {
    eyebrow: "Supply operations",
    title: "Turf moderation and status control",
    description: "Approve pending turfs, reject bad submissions, and update live turf status without leaving the admin portal.",
  },
  bookings: {
    eyebrow: "EPIC 40 - Booking command center",
    title: "Booking operations and dispute resolution",
    description: "Operate bookings, cancellations, disputes, conflicts, no-shows, weather cases, and escalations from a single command page.",
  },
  finance: {
    eyebrow: "EPIC 39 - Money operations",
    title: "Financial operations center",
    description: "Track transactions, approve refunds, process settlements, review disputes, reconcile payments, and monitor fraud.",
  },
  safety: {
    eyebrow: "EPIC 41 - Trust and safety",
    title: "Community moderation and trust center",
    description: "Resolve reports, moderate media, review appeals, scan fraud profiles, and enforce safety controls.",
  },
  support: {
    eyebrow: "EPIC 44 - Customer operations",
    title: "Support desk and SLA center",
    description: "Manage tickets, agents, escalations, knowledge base, FAQs, chat, SLA, and CSAT in one queue.",
  },
  marketing: {
    eyebrow: "EPIC 43 - Growth engine",
    title: "CMS, marketing, and promotion center",
    description: "Run campaigns, coupons, banners, announcements, CMS pages, segments, templates, landing pages, and SEO.",
  },
  roles: {
    eyebrow: "EPIC 38 - Access control",
    title: "Roles, permissions, and access policies",
    description: "Review RBAC roles, permissions, admin assignments, groups, temporary access, resource grants, and permission audit logs.",
  },
  system: {
    eyebrow: "EPIC 45 - Platform control",
    title: "System administration and feature flags",
    description: "Control settings, feature flags, maintenance mode, releases, configuration audit, and platform defaults.",
  },
  analytics: {
    eyebrow: "EPIC 42 - Business intelligence",
    title: "Platform analytics and executive BI",
    description: "Review revenue, growth, booking funnel, retention, geography, forecasts, and AI insights from live analytics APIs.",
  },
};

export default function AdminResourcePage({ moduleKey }: { moduleKey: AdminResourceKey }) {
  const copy = moduleCopy[moduleKey];
  const [state, setState] = useState<ResourceState>({ metrics: [], datasets: [], errors: [] });
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("ALL");
  const [notice, setNotice] = useState("");
  const [busyAction, setBusyAction] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setNotice("");
    const nextState = await loadResourceState(moduleKey);
    setState(nextState);
    setLoading(false);
  }, [moduleKey]);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const filteredDatasets = useMemo(() => {
    return state.datasets.map((dataset) => ({
      ...dataset,
      rows: dataset.rows.filter((row) => matchesFilter(row, search, status)),
    }));
  }, [state.datasets, search, status]);

  const statuses = useMemo(() => {
    const values = new Set(["ALL"]);
    state.datasets.flatMap((dataset) => dataset.rows).forEach((row) => {
      if (row.status) values.add(row.status.toUpperCase());
      if (row.priority) values.add(row.priority.toUpperCase());
    });
    return Array.from(values);
  }, [state.datasets]);

  const handleActionDone = (label: string) => {
    setBusyAction("");
    setNotice(`Action completed: ${label.split(":").slice(1).join(":") || label}`);
    void load();
  };

  const handleActionError = (_label: string, error: unknown) => {
    setBusyAction("");
    setNotice(readableError(error));
  };

  return (
    <div className="space-y-6">
      <AdminPageHeader
        eyebrow={copy.eyebrow}
        title={copy.title}
        description={copy.description}
        actions={renderTopActions(moduleKey, load)}
      />

      {state.errors.length > 0 ? (
        <AdminErrorState
          message={`${state.errors.length} API call(s) failed: ${state.errors.join(", ")}. The page still shows any successfully loaded records.`}
          onRetry={load}
        />
      ) : null}

      {notice ? (
        <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/10 px-4 py-3 text-sm font-black text-emerald-200">
          {notice}
        </div>
      ) : null}

      <section className="grid gap-4 md:grid-cols-2 2xl:grid-cols-4">
        {state.metrics.map((metric) => <AdminMetricCard key={metric.label} {...metric} />)}
      </section>

      <AdminToolbar
        search={search}
        onSearchChange={setSearch}
        status={status}
        onStatusChange={setStatus}
        statuses={statuses}
        onRefresh={load}
        loading={loading}
      />

      {filteredDatasets.map((dataset) => (
        <AdminDataTable
          key={dataset.key}
          title={dataset.title}
          description={dataset.description}
          rows={dataset.rows}
          loading={loading}
          actionBusyLabel={busyAction}
          onActionStart={setBusyAction}
          onActionDone={handleActionDone}
          onActionError={handleActionError}
        />
      ))}
    </div>
  );
}

async function loadResourceState(moduleKey: AdminResourceKey): Promise<ResourceState> {
  switch (moduleKey) {
    case "users":
      return loadUsers();
    case "turfs":
      return loadTurfs();
    case "bookings":
      return loadBookings();
    case "finance":
      return loadFinance();
    case "safety":
      return loadSafety();
    case "support":
      return loadSupport();
    case "marketing":
      return loadMarketing();
    case "roles":
      return loadRoles();
    case "system":
      return loadSystem();
    case "analytics":
      return loadAnalytics();
  }
}

async function loadUsers(): Promise<ResourceState> {
  const [summary, users, ownerVerifications, teams, activity] = await Promise.all([
    loadPart("user summary", adminApi.getAdminUserCenterSummary),
    loadPart("managed users", () => adminApi.searchAdminManagedUsers()),
    loadPart("owner verifications", adminApi.getOwnerVerifications),
    loadPart("teams", () => adminApi.getAdminManagedTeams()),
    loadPart("activity logs", adminApi.getAdminUserActivity),
  ]);

  const metrics: AdminMetric[] = [
    { label: "Players", value: summary.data?.totalPlayers ?? "-", detail: `${summary.data?.newRegistrations ?? 0} new registrations`, tone: "sky" },
    { label: "Owners", value: summary.data?.totalOwners ?? "-", detail: `${summary.data?.pendingVerifications ?? 0} pending verifications`, tone: "emerald" },
    { label: "High Risk", value: summary.data?.highRiskUsers ?? "-", detail: `${summary.data?.suspendedAccounts ?? 0} suspended accounts`, tone: "rose" },
    { label: "Teams", value: summary.data?.totalTeams ?? "-", detail: "Managed team directory", tone: "violet" },
  ];

  return {
    metrics,
    errors: collectErrors(summary, users, ownerVerifications, teams, activity),
    datasets: [
      {
        key: "users",
        title: "Managed users",
        description: "Players, owners, and staff accounts with trust and risk controls.",
        rows: (users.data || []).map((user) => ({
          id: user.id,
          title: user.name,
          subtitle: user.email,
          meta: [user.phone, user.city || "No city", `Trust ${user.trustScore}`, `${user.bookings} bookings`, `${user.reports} reports`],
          status: user.status,
          priority: user.riskLevel,
          updatedAt: user.createdAt,
          actions: [
            {
              label: user.status === "BANNED" || user.status === "BLOCKED" ? "Unban" : "Ban",
              intent: user.status === "BANNED" || user.status === "BLOCKED" ? "primary" : "danger",
              execute: () => user.status === "BANNED" || user.status === "BLOCKED" ? adminApi.unbanUser(user.id).then(noop) : adminApi.banUser(user.id, "Admin portal action").then(noop),
            },
            { label: "Reset Trust", execute: () => adminApi.resetAdminUserTrustScore(user.id).then(noop) },
          ],
        })),
      },
      {
        key: "owner-verifications",
        title: "Owner verification queue",
        description: "Business document and approval queue for owner onboarding.",
        rows: unknownArray(ownerVerifications).map((item, index) => {
          const record = toRecord(item);
          const id = toNumber(record.id) ?? index + 1;
          return {
            id,
            title: text(record.ownerName ?? record.businessName ?? record.name, "Owner verification"),
            subtitle: text(record.documentType ?? record.email ?? record.phone, "Document review required"),
            meta: [text(record.city, "No city"), text(record.documentUrl, "No document URL")],
            status: text(record.status, "PENDING"),
            priority: "HIGH",
            updatedAt: stringValue(record.createdAt ?? record.updatedAt),
            actions: [
              { label: "Approve", intent: "primary", execute: () => adminApi.approveOwner(id).then(noop) },
              { label: "Reject", intent: "danger", execute: () => adminApi.rejectOwner(id).then(noop) },
            ],
          } satisfies AdminTableRow;
        }),
      },
      {
        key: "teams",
        title: "Teams",
        description: "Team directory for moderation and captain tracking.",
        rows: (teams.data || []).map((team) => ({
          id: team.id,
          title: team.name,
          subtitle: `${team.sport} in ${team.city}`,
          meta: [`Captain ${team.captainName}`, `${team.members} members`, team.visibility],
          status: team.visibility,
          updatedAt: team.createdAt,
        })),
      },
      {
        key: "activity",
        title: "Recent activity",
        description: "Timeline of user and owner platform events.",
        rows: (activity.data || []).map((item) => ({
          id: item.id,
          title: item.summary,
          subtitle: item.activityType,
          meta: [`User #${item.userId}`, item.entityId ? `Entity #${item.entityId}` : "No entity"],
          status: item.activityType,
          updatedAt: item.createdAt,
        })),
      },
    ],
  };
}

async function loadTurfs(): Promise<ResourceState> {
  const [turfs, moderations] = await Promise.all([
    loadPart("turfs", adminApi.getAdminTurfs),
    loadPart("turf moderations", adminApi.getTurfModerations),
  ]);

  const allTurfs = unknownArray(turfs);
  const pendingModerations = unknownArray(moderations);

  return {
    metrics: [
      { label: "Turfs", value: allTurfs.length, detail: "Total platform supply", tone: "emerald" },
      { label: "Moderation", value: pendingModerations.length, detail: "Pending review records", tone: "amber" },
      { label: "Active", value: allTurfs.filter((item) => text(toRecord(item).status, "") === "ACTIVE").length, detail: "Live turf listings", tone: "sky" },
      { label: "Maintenance", value: allTurfs.filter((item) => text(toRecord(item).status, "") === "MAINTENANCE").length, detail: "Unavailable listings", tone: "rose" },
    ],
    errors: collectErrors(turfs, moderations),
    datasets: [
      {
        key: "turfs",
        title: "Turf inventory",
        description: "Operational status for every turf listing.",
        rows: allTurfs.map((item, index) => {
          const record = toRecord(item);
          const id = toNumber(record.id) ?? index + 1;
          return {
            id,
            title: text(record.name ?? record.turfName, "Turf listing"),
            subtitle: text(record.location ?? record.address ?? record.city, "No location"),
            meta: [text(record.ownerName ?? record.owner, "No owner"), text(record.boxType, "No box type"), text(record.pitchType, "No pitch")],
            amount: moneyFromUnknown(record.pricePerHour ?? record.price),
            status: text(record.status, "UNKNOWN"),
            updatedAt: stringValue(record.updatedAt ?? record.createdAt),
            actions: [
              { label: "Set Active", intent: "primary", execute: () => adminApi.updateAdminTurfStatus(id, "ACTIVE").then(noop) },
              { label: "Maintenance", execute: () => adminApi.updateAdminTurfStatus(id, "MAINTENANCE").then(noop) },
              { label: "Deactivate", intent: "danger", execute: () => adminApi.updateAdminTurfStatus(id, "INACTIVE").then(noop) },
            ],
          } satisfies AdminTableRow;
        }),
      },
      {
        key: "turf-moderations",
        title: "Turf moderation queue",
        description: "New or edited turf listings waiting for admin decision.",
        rows: pendingModerations.map((item, index) => {
          const record = toRecord(item);
          const id = toNumber(record.id) ?? index + 1;
          return {
            id,
            title: text(record.turfName ?? record.name, "Turf moderation"),
            subtitle: text(record.reason ?? record.location ?? record.city, "Review requested"),
            meta: [text(record.ownerName ?? record.owner, "No owner"), text(record.photoUrl, "No media")],
            status: text(record.status, "PENDING"),
            priority: "HIGH",
            updatedAt: stringValue(record.createdAt ?? record.updatedAt),
            actions: [
              { label: "Approve", intent: "primary", execute: () => adminApi.approveTurf(id).then(noop) },
              { label: "Reject", intent: "danger", execute: () => adminApi.rejectTurf(id, "Admin portal rejection").then(noop) },
            ],
          } satisfies AdminTableRow;
        }),
      },
    ],
  };
}

async function loadBookings(): Promise<ResourceState> {
  const [dashboard, records, cancellations, disputes, conflicts, noShows, weatherCases, timeline, escalations] = await Promise.all([
    loadPart("booking dashboard", adminApi.getAdminBookingOpsDashboard),
    loadPart("bookings", () => adminApi.getAdminBookingRecords()),
    loadPart("cancellations", adminApi.getAdminCancellations),
    loadPart("disputes", adminApi.getAdminBookingDisputes),
    loadPart("slot conflicts", adminApi.getAdminSlotConflicts),
    loadPart("no shows", adminApi.getAdminNoShowCases),
    loadPart("weather cases", adminApi.getAdminWeatherBookingCases),
    loadPart("booking timeline", adminApi.getAdminBookingTimeline),
    loadPart("escalations", adminApi.getAdminBookingEscalations),
  ]);

  return {
    metrics: [
      { label: "Today", value: dashboard.data?.todaysBookings ?? "-", detail: `${dashboard.data?.activeMatches ?? 0} active matches`, tone: "emerald" },
      { label: "Disputes", value: dashboard.data?.activeDisputes ?? disputes.data?.length ?? "-", detail: `${dashboard.data?.disputeRate ?? 0}% dispute rate`, tone: "rose" },
      { label: "Cancellations", value: dashboard.data?.pendingCancellations ?? cancellations.data?.length ?? "-", detail: `${dashboard.data?.cancellationRate ?? 0}% cancellation rate`, tone: "amber" },
      { label: "Escalations", value: dashboard.data?.criticalEscalations ?? escalations.data?.length ?? "-", detail: `${dashboard.data?.noShowCases ?? 0} no-show cases`, tone: "violet" },
    ],
    errors: collectErrors(dashboard, records, cancellations, disputes, conflicts, noShows, weatherCases, timeline, escalations),
    datasets: [
      {
        key: "bookings",
        title: "Bookings",
        description: "All booking records with force status controls for operations.",
        rows: (records.data || []).map((booking) => ({
          id: booking.id,
          title: `${booking.turf} - ${booking.player}`,
          subtitle: `${booking.bookingDate} | ${booking.slot}`,
          meta: [booking.owner, booking.playerEmail, `Slot #${booking.slotId}`, booking.paymentStatus],
          amount: formatMoney(booking.amount),
          status: booking.status,
          updatedAt: booking.createdAt,
          actions: [
            { label: "Complete", intent: "primary", execute: () => adminApi.updateAdminBookingStatus(booking.id, { status: "COMPLETED", reason: "Admin portal action" }).then(noop) },
            { label: "Cancel", intent: "danger", execute: () => adminApi.updateAdminBookingStatus(booking.id, { status: "CANCELLED", reason: "Admin portal action" }).then(noop) },
          ],
        })),
      },
      {
        key: "cancellations",
        title: "Cancellation queue",
        description: "Player, owner, system, and weather cancellation decisions.",
        rows: (cancellations.data || []).map((item) => ({
          id: item.id,
          title: `${item.turf} - ${item.player}`,
          subtitle: item.reason,
          meta: [`Booking #${item.bookingId}`, item.requestedBy, item.cancellationType],
          status: item.status,
          updatedAt: item.createdAt,
          actions: [
            { label: "Approve", intent: "primary", execute: () => adminApi.decideAdminCancellation(item.id, { decision: "APPROVED", reason: "Approved from admin portal" }).then(noop) },
            { label: "Reject", intent: "danger", execute: () => adminApi.decideAdminCancellation(item.id, { decision: "REJECTED", reason: "Rejected from admin portal" }).then(noop) },
          ],
        })),
      },
      {
        key: "disputes",
        title: "Disputes",
        description: "Open booking conflicts requiring admin resolution.",
        rows: (disputes.data || []).map((item) => ({
          id: item.id,
          title: `Booking #${item.bookingId} dispute`,
          subtitle: item.summary || item.disputeType,
          meta: [item.disputeType, item.assignedTo || "Unassigned"],
          status: item.status,
          priority: item.status === "OPEN" ? "HIGH" : "MEDIUM",
          updatedAt: item.createdAt,
          actions: [{ label: "Resolve", intent: "primary", execute: () => adminApi.resolveAdminBookingDispute(item.id, "Resolved from admin portal").then(noop) }],
        })),
      },
      {
        key: "conflicts",
        title: "Slot conflicts",
        description: "Double-booking or overlapping slot conflicts.",
        rows: (conflicts.data || []).map((item) => ({
          id: item.id,
          title: `Slot #${item.slotId} conflict`,
          subtitle: item.notes || "Potential overlapping bookings",
          meta: [`Booking A #${item.bookingA}`, item.bookingB ? `Booking B #${item.bookingB}` : "No second booking"],
          status: item.status,
          priority: "CRITICAL",
          updatedAt: item.createdAt,
          actions: [{ label: "Resolve", intent: "primary", execute: () => adminApi.resolveAdminSlotConflict(item.id, "Resolved from admin portal").then(noop) }],
        })),
      },
      {
        key: "no-shows",
        title: "No-show cases",
        description: "Attendance cases with penalty controls.",
        rows: (noShows.data || []).map((item) => ({
          id: item.id,
          title: item.userName,
          subtitle: item.notes || item.noShowType,
          meta: [`Booking #${item.bookingId}`, `Penalty ${item.penalty}`, item.noShowType],
          status: item.status,
          updatedAt: item.createdAt,
          actions: [{ label: "Resolve", intent: "primary", execute: () => adminApi.resolveAdminNoShowCase(item.id, "Resolved from admin portal").then(noop) }],
        })),
      },
      {
        key: "weather-cases",
        title: "Weather cases",
        description: "Weather refund and reschedule decisions.",
        rows: (weatherCases.data || []).map((item) => ({
          id: item.id,
          title: `Weather case #${item.bookingId}`,
          subtitle: item.reason || item.refundType,
          meta: [item.refundType],
          amount: formatMoney(item.amount),
          status: item.status,
          updatedAt: item.createdAt,
          actions: [
            { label: "Approve", intent: "primary", execute: () => adminApi.decideAdminWeatherBookingCase(item.id, { decision: "APPROVED", reason: "Approved from admin portal" }).then(noop) },
            { label: "Reject", intent: "danger", execute: () => adminApi.decideAdminWeatherBookingCase(item.id, { decision: "REJECTED", reason: "Rejected from admin portal" }).then(noop) },
          ],
        })),
      },
      {
        key: "escalations",
        title: "Escalations",
        description: "Critical booking operations cases assigned to admin teams.",
        rows: (escalations.data || []).map((item) => ({
          id: item.id,
          title: `Escalation for booking #${item.bookingId}`,
          subtitle: item.summary,
          meta: [item.assignedTo, item.ticketId ? `Ticket #${item.ticketId}` : "No ticket"],
          status: item.status,
          priority: item.priority,
          updatedAt: item.createdAt,
          actions: [{ label: "Resolve", intent: "primary", execute: () => adminApi.resolveAdminBookingEscalation(item.id, "Resolved from admin portal").then(noop) }],
        })),
      },
      {
        key: "timeline",
        title: "Booking timeline",
        description: "Audit trail for booking lifecycle changes.",
        rows: (timeline.data || []).map((item) => ({
          id: item.id,
          title: item.eventType,
          subtitle: item.details,
          meta: [`Booking #${item.bookingId}`, item.performedBy],
          status: item.eventType,
          updatedAt: item.createdAt,
        })),
      },
    ],
  };
}

async function loadFinance(): Promise<ResourceState> {
  const [dashboard, transactions, refunds, settlements, commissions, wallets, disputes, reconciliation, taxes, audits, fraud] = await Promise.all([
    loadPart("finance dashboard", adminApi.getAdminFinanceDashboard),
    loadPart("transactions", () => adminApi.getAdminFinanceTransactions()),
    loadPart("refunds", adminApi.getAdminFinanceRefunds),
    loadPart("settlements", adminApi.getAdminOwnerSettlements),
    loadPart("commission rules", adminApi.getAdminCommissionRules),
    loadPart("wallets", adminApi.getAdminWallets),
    loadPart("finance disputes", adminApi.getAdminFinanceDisputes),
    loadPart("reconciliation", adminApi.getAdminReconciliationLogs),
    loadPart("tax reports", adminApi.getAdminTaxReports),
    loadPart("finance audits", adminApi.getAdminFinanceAudits),
    loadPart("finance fraud", adminApi.getAdminFinanceFraudAlerts),
  ]);

  return {
    metrics: [
      { label: "GMV", value: formatMoney(dashboard.data?.gmv), detail: `${formatMoney(dashboard.data?.netRevenue)} net revenue`, tone: "emerald" },
      { label: "Refunds", value: formatMoney(dashboard.data?.refundAmount), detail: `${refunds.data?.length ?? 0} refund records`, tone: "amber" },
      { label: "Settlements", value: dashboard.data?.pendingSettlements ?? settlements.data?.filter((item) => item.status !== "PAID").length ?? "-", detail: "Pending owner payouts", tone: "sky" },
      { label: "Fraud", value: dashboard.data?.fraudAlerts ?? fraud.data?.length ?? "-", detail: `${dashboard.data?.openDisputes ?? 0} finance disputes`, tone: "rose" },
    ],
    errors: collectErrors(dashboard, transactions, refunds, settlements, commissions, wallets, disputes, reconciliation, taxes, audits, fraud),
    datasets: [
      {
        key: "transactions",
        title: "Transactions",
        description: "Gateway transactions tied to bookings, players, and owners.",
        rows: (transactions.data || []).map((item) => ({
          id: item.id,
          title: item.transactionId || item.orderId || `Payment #${item.id}`,
          subtitle: `${item.player} paid ${item.owner}`,
          meta: [item.gateway, item.bookingId ? `Booking #${item.bookingId}` : "No booking"],
          amount: formatMoney(item.amount),
          status: item.status,
          updatedAt: item.createdAt,
        })),
      },
      {
        key: "refunds",
        title: "Refund center",
        description: "Approve, reject, and audit booking refund requests.",
        rows: (refunds.data || []).map((item) => ({
          id: item.id,
          title: `Refund for booking #${item.bookingId}`,
          subtitle: item.reason,
          meta: [`Payment #${item.paymentId}`, item.gatewayRefundId || "No gateway refund"],
          amount: formatMoney(item.amount),
          status: item.status,
          updatedAt: item.createdAt,
          actions: [
            { label: "Approve", intent: "primary", execute: () => adminApi.updateAdminRefundStatus(item.id, "APPROVED").then(noop) },
            { label: "Reject", intent: "danger", execute: () => adminApi.updateAdminRefundStatus(item.id, "REJECTED").then(noop) },
          ],
        })),
      },
      {
        key: "settlements",
        title: "Owner settlements",
        description: "Gross amount, commission, and net payout records.",
        rows: (settlements.data || []).map((item) => ({
          id: item.id,
          title: item.ownerName,
          subtitle: item.reference || (item.bookingId ? `Booking #${item.bookingId}` : "Bulk settlement"),
          meta: [`Gross ${formatMoney(item.grossAmount)}`, `Commission ${formatMoney(item.commission)}`],
          amount: formatMoney(item.netAmount),
          status: item.status,
          updatedAt: item.createdAt,
          actions: [{ label: "Mark Paid", intent: "primary", execute: () => adminApi.updateAdminSettlementStatus(item.id, "PAID").then(noop) }],
        })),
      },
      {
        key: "commissions",
        title: "Commission rules",
        description: "Platform commission configuration currently active in finance.",
        rows: (commissions.data || []).map((item) => ({
          id: item.id,
          title: item.description || item.type,
          subtitle: item.ownerId ? `Owner #${item.ownerId}` : "Platform default",
          meta: [item.type, `${item.value}`, item.active ? "Active" : "Inactive"],
          status: item.active ? "ACTIVE" : "INACTIVE",
          updatedAt: item.createdAt,
        })),
      },
      {
        key: "wallets",
        title: "Wallet center",
        description: "Player and owner wallet balances with transaction counts.",
        rows: (wallets.data || []).map((item) => ({
          id: item.id,
          title: item.userName,
          subtitle: `User #${item.userId}`,
          meta: [`${item.transactions.length} transactions`],
          amount: formatMoney(item.balance),
          status: "ACTIVE",
          updatedAt: item.updatedAt,
        })),
      },
      {
        key: "disputes",
        title: "Financial disputes",
        description: "Money conflicts requiring resolution.",
        rows: (disputes.data || []).map((item) => ({
          id: item.id,
          title: item.type,
          subtitle: item.summary || item.resolution || "Finance dispute",
          meta: [`User #${item.userId}`, item.bookingId ? `Booking #${item.bookingId}` : "No booking"],
          amount: formatMoney(item.amount),
          status: item.status,
          priority: item.status === "OPEN" ? "HIGH" : "MEDIUM",
          updatedAt: item.createdAt,
          actions: [{ label: "Resolve", intent: "primary", execute: () => adminApi.resolveAdminFinanceDispute(item.id, "Resolved from admin portal").then(noop) }],
        })),
      },
      {
        key: "reconciliation",
        title: "Reconciliation",
        description: "Gateway amount vs system amount mismatch checks.",
        rows: (reconciliation.data || []).map((item) => ({
          id: item.id,
          title: item.transactionId,
          subtitle: item.notes || "Reconciliation check",
          meta: [`Gateway ${formatMoney(item.gatewayAmount)}`, `System ${formatMoney(item.systemAmount)}`],
          status: item.status,
          updatedAt: item.createdAt,
        })),
      },
      {
        key: "tax-reports",
        title: "Tax reports",
        description: "GST and finance report exports generated by admin finance.",
        rows: (taxes.data || []).map((item) => ({
          id: item.id,
          title: item.reportType,
          subtitle: item.period,
          meta: [`Taxable ${formatMoney(item.taxableAmount)}`, `GST ${formatMoney(item.gstAmount)}`],
          status: "GENERATED",
          updatedAt: item.generatedAt,
        })),
      },
      {
        key: "audits",
        title: "Finance audit log",
        description: "Audit trail for financial admin actions.",
        rows: (audits.data || []).map((item) => ({
          id: item.id,
          title: item.action,
          subtitle: item.details,
          meta: [item.actor, item.entity, item.userId ? `User #${item.userId}` : "No user"],
          status: item.action,
          updatedAt: item.timestamp,
        })),
      },
    ],
  };
}

async function loadSafety(): Promise<ResourceState> {
  const [dashboard, reports, warnings, suspensions, bans, appeals, media, fraud, teams, reviews, trust, community] = await Promise.all([
    loadPart("safety dashboard", adminApi.getAdminSafetyDashboard),
    loadPart("reports", adminApi.getAdminSafetyReports),
    loadPart("warnings", adminApi.getAdminSafetyWarnings),
    loadPart("suspensions", adminApi.getAdminSafetySuspensions),
    loadPart("bans", adminApi.getAdminSafetyBans),
    loadPart("appeals", adminApi.getAdminSafetyAppeals),
    loadPart("media", adminApi.getAdminSafetyMedia),
    loadPart("fraud profiles", adminApi.getAdminSafetyFraudProfiles),
    loadPart("teams", adminApi.getAdminSafetyTeams),
    loadPart("reviews", adminApi.getAdminSafetyReviews),
    loadPart("trust adjustments", adminApi.getAdminSafetyTrustAdjustments),
    loadPart("community moderation", adminApi.getAdminCommunityModeration),
  ]);

  return {
    metrics: [
      { label: "Open Reports", value: dashboard.data?.openReports ?? reports.data?.length ?? "-", detail: "User safety queue", tone: "rose" },
      { label: "Appeals", value: dashboard.data?.pendingAppeals ?? appeals.data?.length ?? "-", detail: "Pending appeal decisions", tone: "amber" },
      { label: "Fraud", value: dashboard.data?.fraudCases ?? fraud.data?.length ?? "-", detail: "Risk profiles", tone: "violet" },
      { label: "Health", value: dashboard.data?.safetyHealthScore ?? "-", detail: `${dashboard.data?.trustViolations ?? 0} trust violations`, tone: "emerald" },
    ],
    errors: collectErrors(dashboard, reports, warnings, suspensions, bans, appeals, media, fraud, teams, reviews, trust, community),
    datasets: [
      {
        key: "reports",
        title: "User reports",
        description: "Abuse, fraud, spam, fake profile, and no-show reports.",
        rows: (reports.data || []).map((item) => ({
          id: item.id,
          title: item.reason,
          subtitle: item.description || `${item.reporter} reported ${item.reportedUser}`,
          meta: [item.reporter, item.reportedUser, item.reportedUserId ? `Reported #${item.reportedUserId}` : "No ID"],
          status: item.status,
          priority: item.status === "OPEN" ? "HIGH" : "MEDIUM",
          updatedAt: item.createdAt,
          actions: [{ label: "Resolve", intent: "primary", execute: () => adminApi.resolveAdminSafetyReport(item.id, { status: "RESOLVED", reason: "Resolved from admin portal" }).then(noop) }],
        })),
      },
      {
        key: "media",
        title: "Media moderation",
        description: "Profile photos, banners, logos, and user uploads waiting for review.",
        rows: (media.data || []).map((item) => ({
          id: item.id,
          title: `${item.ownerType} ${item.mediaType}`,
          subtitle: item.imageUrl,
          meta: [`Owner #${item.ownerId}`, item.active ? "Active" : "Inactive"],
          status: item.moderationStatus,
          updatedAt: item.uploadedAt,
          actions: [
            { label: "Approve", intent: "primary", execute: () => adminApi.decideAdminSafetyMedia(item.id, { decision: "APPROVED", reason: "Approved from admin portal" }).then(noop) },
            { label: "Reject", intent: "danger", execute: () => adminApi.decideAdminSafetyMedia(item.id, { decision: "REJECTED", reason: "Rejected from admin portal" }).then(noop) },
          ],
        })),
      },
      {
        key: "appeals",
        title: "Appeals",
        description: "Appeals from users against moderation and enforcement actions.",
        rows: (appeals.data || []).map((item) => ({
          id: item.id,
          title: item.userName,
          subtitle: item.message,
          meta: [item.caseType, `Case #${item.caseId}`],
          status: item.status,
          updatedAt: item.createdAt,
          actions: [
            { label: "Approve", intent: "primary", execute: () => adminApi.decideAdminSafetyAppeal(item.id, { decision: "APPROVED", reason: "Approved from admin portal" }).then(noop) },
            { label: "Reject", intent: "danger", execute: () => adminApi.decideAdminSafetyAppeal(item.id, { decision: "REJECTED", reason: "Rejected from admin portal" }).then(noop) },
          ],
        })),
      },
      {
        key: "fraud",
        title: "Fake account and fraud profiles",
        description: "Risk profiles generated from suspicious activity signals.",
        rows: (fraud.data || []).map((item) => ({
          id: item.id,
          title: item.userName,
          subtitle: item.reason,
          meta: [`User #${item.userId}`, item.signals || "No signals"],
          status: item.status,
          priority: item.riskLevel,
          updatedAt: item.createdAt,
        })),
      },
      {
        key: "warnings",
        title: "Warnings",
        description: "Warnings issued before stronger moderation actions.",
        rows: (warnings.data || []).map((item) => ({
          id: item.id,
          title: item.userName,
          subtitle: item.reason,
          meta: [`User #${item.userId}`, item.issuedBy],
          status: item.warningLevel,
          updatedAt: item.createdAt,
        })),
      },
      {
        key: "suspensions",
        title: "Suspensions and bans",
        description: "Temporary and permanent enforcement records.",
        rows: [
          ...(suspensions.data || []).map((item) => ({
            id: `S-${item.id}`,
            title: `${item.entityType} #${item.entityId}`,
            subtitle: item.reason,
            meta: [item.startDate || "No start", item.endDate || "No end"],
            status: item.status,
            updatedAt: item.endDate || item.startDate,
            actions: [{ label: "Resolve", intent: "primary", execute: () => adminApi.resolveAdminSafetySuspension(item.id).then(noop) }],
          } satisfies AdminTableRow)),
          ...(bans.data || []).map((item) => ({
            id: `B-${item.id}`,
            title: `${item.type}: ${item.value}`,
            subtitle: item.reason,
            meta: [item.active ? "Active ban" : "Inactive ban"],
            status: item.active ? "BANNED" : "UNBANNED",
            updatedAt: item.createdAt,
            actions: item.active ? [{ label: "Unban", intent: "primary" as const, execute: () => adminApi.unbanAdminSafetyEntity(item.id).then(noop) }] : [],
          } satisfies AdminTableRow)),
        ],
      },
      {
        key: "teams-reviews",
        title: "Teams and reviews",
        description: "Community team and review moderation queues.",
        rows: [
          ...(teams.data || []).map((item) => ({
            id: `T-${item.id}`,
            title: item.name,
            subtitle: `${item.sport} in ${item.city}`,
            meta: [item.captain, item.visibility],
            status: item.visibility,
            updatedAt: item.createdAt,
            actions: [
              { label: "Approve", intent: "primary" as const, execute: () => adminApi.moderateAdminSafetyTeam(item.id, { decision: "APPROVED", reason: "Approved from admin portal" }).then(noop) },
              { label: "Hide", intent: "danger" as const, execute: () => adminApi.moderateAdminSafetyTeam(item.id, { decision: "HIDDEN", reason: "Hidden from admin portal" }).then(noop) },
            ],
          } satisfies AdminTableRow)),
          ...(reviews.data || []).map((item) => ({
            id: `R-${item.id}`,
            title: `Review ${item.rating}/5`,
            subtitle: item.comment || "No comment",
            meta: [`User #${item.userId}`, `Turf #${item.turfId}`, `${item.reports} reports`],
            status: item.reports > 0 ? "FLAGGED" : "ACTIVE",
            updatedAt: item.createdAt,
            actions: [
              { label: "Approve", intent: "primary" as const, execute: () => adminApi.moderateAdminSafetyReview(item.id, { decision: "APPROVED", reason: "Approved from admin portal" }).then(noop) },
              { label: "Hide", intent: "danger" as const, execute: () => adminApi.moderateAdminSafetyReview(item.id, { decision: "HIDDEN", reason: "Hidden from admin portal" }).then(noop) },
            ],
          } satisfies AdminTableRow)),
        ],
      },
      {
        key: "trust-community",
        title: "Trust and community audit",
        description: "Manual trust changes and community content moderation records.",
        rows: [
          ...(trust.data || []).map((item) => ({
            id: `TA-${item.id}`,
            title: item.userName,
            subtitle: item.reason,
            meta: [`${item.oldScore} -> ${item.newScore}`, item.adminId],
            status: "TRUST_ADJUSTED",
            updatedAt: item.createdAt,
          } satisfies AdminTableRow)),
          ...(community.data || []).map((item) => ({
            id: `CM-${item.id}`,
            title: `${item.contentType} #${item.contentId}`,
            subtitle: item.reason,
            meta: [item.moderatorId],
            status: item.action,
            updatedAt: item.createdAt,
          } satisfies AdminTableRow)),
        ],
      },
    ],
  };
}

async function loadSupport(): Promise<ResourceState> {
  const [dashboard, tickets, chat, sla, escalations, agents, kb, faqs, csat, notes, automations, channels] = await Promise.all([
    loadPart("support dashboard", adminApi.getAdminSupportDashboard),
    loadPart("tickets", adminApi.getAdminSupportTickets),
    loadPart("chat", adminApi.getAdminSupportChat),
    loadPart("SLA", adminApi.getAdminSlaPolicies),
    loadPart("escalations", adminApi.getAdminTicketEscalations),
    loadPart("agents", adminApi.getAdminSupportAgents),
    loadPart("knowledge base", adminApi.getAdminKnowledgeBase),
    loadPart("FAQs", adminApi.getAdminFaqs),
    loadPart("CSAT", adminApi.getAdminCsat),
    loadPart("notes", adminApi.getAdminTicketNotes),
    loadPart("automations", adminApi.getAdminSupportAutomations),
    loadPart("channels", adminApi.getAdminSupportChannels),
  ]);

  return {
    metrics: [
      { label: "Open Tickets", value: dashboard.data?.openTickets ?? tickets.data?.filter((item) => item.status !== "RESOLVED" && item.status !== "CLOSED").length ?? "-", detail: `${dashboard.data?.resolvedTickets ?? 0} resolved`, tone: "emerald" },
      { label: "SLA Breaches", value: dashboard.data?.slaBreaches ?? tickets.data?.filter((item) => item.slaBreached).length ?? "-", detail: `${dashboard.data?.averageResponseHours ?? 0}h avg response`, tone: "rose" },
      { label: "Escalations", value: dashboard.data?.escalations ?? escalations.data?.length ?? "-", detail: `${dashboard.data?.liveChats ?? 0} live chats`, tone: "amber" },
      { label: "CSAT", value: dashboard.data?.csatScore ?? "-", detail: `${dashboard.data?.agentAvailability ?? 0}% agent availability`, tone: "sky" },
    ],
    errors: collectErrors(dashboard, tickets, chat, sla, escalations, agents, kb, faqs, csat, notes, automations, channels),
    datasets: [
      {
        key: "tickets",
        title: "Ticket queue",
        description: "Central support tickets with assignment and resolution actions.",
        rows: (tickets.data || []).map((item) => ({
          id: item.id,
          title: item.ticketNumber,
          subtitle: item.issue,
          meta: [item.category, item.channel, item.assignedTo || "Unassigned", item.slaBreached ? "SLA breached" : "SLA ok"],
          status: item.status,
          priority: item.priority,
          updatedAt: item.updatedAt || item.createdAt,
          actions: [
            { label: "Resolve", intent: "primary", execute: () => adminApi.replyAdminSupportTicket(item.id, { reply: "Resolved from admin portal", status: "RESOLVED" }).then(noop) },
            { label: "Assign Ops", execute: () => adminApi.assignAdminSupportTicket(item.id, "operations").then(noop) },
          ],
        })),
      },
      {
        key: "agents",
        title: "Agents and escalations",
        description: "Support staff availability and escalation ownership.",
        rows: [
          ...(agents.data || []).map((item) => ({
            id: `A-${item.id ?? item.name}`,
            title: item.name,
            subtitle: item.role,
            meta: [item.userId ? `User #${item.userId}` : "No linked user"],
            status: item.status,
          } satisfies AdminTableRow)),
          ...(escalations.data || []).map((item) => ({
            id: `E-${item.id ?? item.ticketId}`,
            title: `Ticket #${item.ticketId}`,
            subtitle: item.reason || item.level,
            meta: [item.assignedTo, item.level],
            status: item.status || "OPEN",
            updatedAt: item.createdAt,
          } satisfies AdminTableRow)),
        ],
      },
      {
        key: "knowledge",
        title: "Knowledge base and FAQs",
        description: "Support self-service content managed by admins.",
        rows: [
          ...(kb.data || []).map((item) => ({
            id: `KB-${item.id ?? item.title}`,
            title: item.title,
            subtitle: item.category,
            meta: [trimText(item.content, 90)],
            status: item.status,
          } satisfies AdminTableRow)),
          ...(faqs.data || []).map((item) => ({
            id: `FAQ-${item.id ?? item.question}`,
            title: item.question,
            subtitle: item.category,
            meta: [trimText(item.answer, 90)],
            status: item.status,
          } satisfies AdminTableRow)),
        ],
      },
      {
        key: "sla",
        title: "SLA, chat, CSAT, and automation",
        description: "Operational quality controls and support automation.",
        rows: [
          ...(sla.data || []).map((item) => ({
            id: `SLA-${item.id ?? item.priority}`,
            title: `${item.priority} SLA`,
            subtitle: `${item.responseTimeHours}h first response, ${item.resolutionTimeHours}h resolution`,
            status: item.priority,
          } satisfies AdminTableRow)),
          ...(chat.data || []).slice(0, 12).map((item) => ({
            id: `CHAT-${item.id}`,
            title: `Ticket #${item.ticketId}`,
            subtitle: item.message,
            meta: [item.senderType, item.senderId ? `Sender #${item.senderId}` : "No sender"],
            status: "CHAT",
            updatedAt: item.createdAt,
          } satisfies AdminTableRow)),
          ...(csat.data || []).map((item) => ({
            id: `CSAT-${item.id ?? item.ticketId}`,
            title: `Ticket #${item.ticketId}`,
            subtitle: item.feedback || "No written feedback",
            meta: [`${item.rating}/5`],
            status: item.rating >= 4 ? "GOOD" : "LOW",
            updatedAt: item.createdAt,
          } satisfies AdminTableRow)),
          ...(automations.data || []).map((item) => ({
            id: `AUTO-${item.id ?? item.ruleName}`,
            title: item.ruleName,
            subtitle: item.condition,
            meta: [item.action],
            status: item.active ? "ACTIVE" : "INACTIVE",
          } satisfies AdminTableRow)),
          ...(channels.data || []).slice(0, 12).map((item) => ({
            id: `CH-${item.id ?? item.ticketId}`,
            title: item.channel,
            subtitle: item.message,
            meta: [`Ticket #${item.ticketId}`, item.externalId || "No external ID"],
            status: "CHANNEL",
            updatedAt: item.createdAt,
          } satisfies AdminTableRow)),
          ...(notes.data || []).slice(0, 12).map((item) => ({
            id: `N-${item.id ?? item.ticketId}`,
            title: `Ticket #${item.ticketId}`,
            subtitle: item.note,
            meta: [item.agentId || "No agent"],
            status: "NOTE",
            updatedAt: item.createdAt,
          } satisfies AdminTableRow)),
        ],
      },
    ],
  };
}

async function loadMarketing(): Promise<ResourceState> {
  const [dashboard, campaigns, coupons, banners, cms, referrals, templates, announcements, landing, seo, segments, analytics, growth, growthSegments, growthAutomation, growthReconciliation] = await Promise.all([
    loadPart("marketing dashboard", adminApi.getAdminMarketingDashboard),
    loadPart("campaigns", adminApi.getAdminMarketingCampaigns),
    loadPart("coupons", adminApi.getAdminMarketingCoupons),
    loadPart("banners", adminApi.getAdminMarketingBanners),
    loadPart("CMS pages", adminApi.getAdminCmsPages),
    loadPart("referrals", adminApi.getAdminReferralCampaigns),
    loadPart("templates", adminApi.getAdminMarketingTemplates),
    loadPart("announcements", adminApi.getAdminAnnouncements),
    loadPart("landing pages", adminApi.getAdminLandingPages),
    loadPart("SEO", adminApi.getAdminSeoSettings),
    loadPart("segments", adminApi.getAdminAudienceSegments),
    loadPart("marketing analytics", adminApi.getAdminMarketingAnalytics),
    loadPart("growth dashboard", adminApi.getAdminGrowthDashboard),
    loadPart("growth segments", adminApi.getAdminGrowthSegments),
    loadPart("growth automation", adminApi.getAdminGrowthAutomation),
    loadPart("growth reconciliation", adminApi.getAdminGrowthReconciliation),
  ]);

  return {
    metrics: [
      { label: "CLV", value: formatMoney(growth.data?.scorecard.customerLifetimeValue), detail: `${formatMoney(growth.data?.scorecard.averageClv)} average`, tone: "emerald" },
      { label: "Repeat", value: growth.data?.scorecard.repeatCustomers ?? "-", detail: `${growth.data?.retention.repeatBookingRate ?? 0}% repeat booking rate`, tone: "sky" },
      { label: "VIP", value: growth.data?.scorecard.vipCustomers ?? "-", detail: `${growth.data?.scorecard.inactiveCustomers ?? 0} inactive customers`, tone: "violet" },
      { label: "Wallet Liability", value: formatMoney(growth.data?.wallet.liability), detail: `${growth.data?.loyalty.pointsOutstanding ?? 0} loyalty points`, tone: "amber" },
    ],
    errors: collectErrors(dashboard, campaigns, coupons, banners, cms, referrals, templates, announcements, landing, seo, segments, analytics, growth, growthSegments, growthAutomation, growthReconciliation),
    datasets: [
      {
        key: "growth-scorecard",
        title: "CRM, loyalty, and CLV growth scorecard",
        description: "Customer lifetime value, retention, membership, rewards, wallet, referral, and coupon health.",
        rows: [
          {
            id: "growth-clv",
            title: "Customer lifetime value",
            subtitle: `${growth.data?.scorecard.totalCustomers ?? 0} customers, ${growth.data?.scorecard.activeCustomers ?? 0} active`,
            amount: formatMoney(growth.data?.scorecard.customerLifetimeValue),
            status: "CLV",
          },
          {
            id: "growth-membership",
            title: "Membership revenue",
            subtitle: `${growth.data?.memberships.active ?? 0} active plans, ${growth.data?.memberships.renewalRisk ?? 0} renewal risks`,
            amount: formatMoney(growth.data?.memberships.revenue),
            status: "MEMBERSHIP",
          },
          {
            id: "growth-referrals",
            title: "Referral conversion",
            subtitle: `${growth.data?.referrals.completed ?? 0} completed of ${growth.data?.referrals.total ?? 0} referrals`,
            meta: [`${growth.data?.referrals.conversionRate ?? 0}% conversion`],
            status: "REFERRAL",
          },
          {
            id: "growth-reconciliation",
            title: "Growth reconciliation",
            subtitle: `${growthReconciliation.data?.checks.length ?? 0} control checks`,
            meta: [`Score ${growthReconciliation.data?.score ?? 0}%`],
            status: growthReconciliation.data?.status ?? "REVIEW",
            priority: growthReconciliation.data?.status === "PASS" ? "LOW" : "HIGH",
          },
        ],
      },
      {
        key: "growth-segments",
        title: "CRM segments and marketing automation",
        description: "VIP, frequent, weekend, inactive, membership, and win-back audiences for automated campaigns.",
        rows: [
          ...(growthSegments.data || growth.data?.segments || []).map((item) => ({
            id: `GSEG-${item.name}`,
            title: item.name,
            subtitle: item.description,
            meta: [`${item.customers} customers`, item.rule],
            status: item.recommendedAction,
          } satisfies AdminTableRow)),
          ...(growthAutomation.data || []).map((item) => ({
            id: `AUTO-${item.key}`,
            title: item.trigger,
            subtitle: item.action,
            meta: [`${item.audience} customers`],
            status: item.status,
          } satisfies AdminTableRow)),
          ...(growth.data?.insights || []).map((item, index) => ({
            id: `GINS-${index}`,
            title: item.title,
            subtitle: item.message,
            status: item.severity,
          } satisfies AdminTableRow)),
        ],
      },
      {
        key: "campaigns",
        title: "Campaigns",
        description: "Push, email, SMS, and WhatsApp campaigns.",
        rows: (campaigns.data || []).map((item) => ({
          id: item.id ?? item.name,
          title: item.title,
          subtitle: item.message,
          meta: [item.name, item.channel, item.targetRole || "All roles", `${item.sentCount ?? 0} sent`],
          status: item.status || "DRAFT",
          updatedAt: item.createdAt || item.scheduledAt,
          actions: item.id ? [{ label: "Send", intent: "primary" as const, execute: () => adminApi.sendAdminMarketingCampaign(item.id as number).then(noop) }] : [],
        })),
      },
      {
        key: "coupons-banners",
        title: "Coupons and banners",
        description: "Revenue-driving promos and visual placements.",
        rows: [
          ...(coupons.data || []).map((item) => ({
            id: `C-${item.id ?? item.code}`,
            title: item.code,
            subtitle: `${item.discountType} discount`,
            meta: [`Value ${item.discountValue}`, `Used ${item.usageCount ?? 0}`, item.validUntil ? `Until ${formatAdminDate(item.validUntil)}` : "No expiry"],
            amount: item.maxDiscount ? formatMoney(item.maxDiscount) : undefined,
            status: item.active ? "ACTIVE" : "INACTIVE",
            updatedAt: item.validFrom,
          } satisfies AdminTableRow)),
          ...(banners.data || []).map((item) => ({
            id: `B-${item.id ?? item.title}`,
            title: item.title,
            subtitle: item.imageUrl,
            meta: [item.location, item.bannerType, item.targetUrl || "No target"],
            status: item.status,
            updatedAt: item.createdAt,
          } satisfies AdminTableRow)),
        ],
      },
      {
        key: "content",
        title: "CMS, announcements, landing pages, SEO",
        description: "Content controlled without code deployments.",
        rows: [
          ...(cms.data || []).map((item) => ({
            id: `CMS-${item.id ?? item.slug}`,
            title: item.title,
            subtitle: item.slug,
            meta: [`Version ${item.versionNumber ?? 1}`, trimText(item.content, 90)],
            status: item.status,
            updatedAt: item.updatedAt,
          } satisfies AdminTableRow)),
          ...(announcements.data || []).map((item) => ({
            id: `ANN-${item.id ?? item.title}`,
            title: item.title,
            subtitle: item.message,
            meta: [item.priority, item.displayLocation],
            status: item.status,
          } satisfies AdminTableRow)),
          ...(landing.data || []).map((item) => ({
            id: `LP-${item.id ?? item.slug}`,
            title: item.title,
            subtitle: item.slug,
            meta: [item.templateName],
            status: item.status,
          } satisfies AdminTableRow)),
          ...(seo.data || []).map((item) => ({
            id: `SEO-${item.id ?? item.pageSlug}`,
            title: item.metaTitle,
            subtitle: item.metaDescription,
            meta: [item.pageSlug, item.keywords || "No keywords"],
            status: "SEO",
          } satisfies AdminTableRow)),
        ],
      },
      {
        key: "audience",
        title: "Audience, referral, and templates",
        description: "Targeting and reusable communication assets.",
        rows: [
          ...(segments.data || []).map((item) => ({
            id: `SEG-${item.id ?? item.name}`,
            title: item.name,
            subtitle: item.ruleValue,
            meta: [item.ruleType],
            status: "SEGMENT",
          } satisfies AdminTableRow)),
          ...(referrals.data || []).map((item) => ({
            id: `REF-${item.id ?? item.name}`,
            title: item.name,
            subtitle: item.eligibility,
            meta: [`Reward ${formatMoney(item.reward)}`, item.expiresAt ? `Expires ${formatAdminDate(item.expiresAt)}` : "No expiry"],
            status: item.status,
          } satisfies AdminTableRow)),
          ...(templates.data || []).map((item) => ({
            id: `TMP-${item.id ?? item.name}`,
            title: item.name,
            subtitle: item.subject || item.channel,
            meta: [item.channel, trimText(item.body, 90)],
            status: "TEMPLATE",
          } satisfies AdminTableRow)),
        ],
      },
    ],
  };
}

async function loadRoles(): Promise<ResourceState> {
  const [roles, permissions, adminUsers, groups, policies, resources, temporary, staff, audit] = await Promise.all([
    loadPart("roles", adminApi.getAdminRoles),
    loadPart("permissions", adminApi.getAdminPermissions),
    loadPart("admin users", adminApi.getAdminRbacUsers),
    loadPart("permission groups", adminApi.getPermissionGroups),
    loadPart("access policies", adminApi.getAccessPolicies),
    loadPart("resource permissions", adminApi.getResourcePermissions),
    loadPart("temporary permissions", adminApi.getTemporaryPermissions),
    loadPart("owner staff roles", adminApi.getOwnerStaffRoles),
    loadPart("permission audit", adminApi.getPermissionAudit),
  ]);

  return {
    metrics: [
      { label: "Roles", value: roles.data?.length ?? "-", detail: "Defined RBAC roles", tone: "emerald" },
      { label: "Permissions", value: permissions.data?.length ?? "-", detail: "Backend authorities", tone: "sky" },
      { label: "Admins", value: adminUsers.data?.length ?? "-", detail: "Admin accounts", tone: "violet" },
      { label: "Audit", value: audit.data?.length ?? "-", detail: "Access change events", tone: "amber" },
    ],
    errors: collectErrors(roles, permissions, adminUsers, groups, policies, resources, temporary, staff, audit),
    datasets: [
      {
        key: "roles",
        title: "Roles",
        description: "System and custom roles with assigned permission counts.",
        rows: (roles.data || []).map((item) => ({
          id: item.id,
          title: item.name,
          subtitle: item.description,
          meta: [`${item.permissions.length} permissions`, item.systemRole ? "System role" : "Custom role"],
          status: item.active === false ? "INACTIVE" : "ACTIVE",
          updatedAt: item.createdAt,
        })),
      },
      {
        key: "permissions",
        title: "Permissions",
        description: "Granular capabilities used by API authorization and menu visibility.",
        rows: (permissions.data || []).map((item) => ({
          id: item.id,
          title: item.code,
          subtitle: item.description,
          meta: [item.module, item.name],
          status: "PERMISSION",
        })),
      },
      {
        key: "admin-users",
        title: "Admin users",
        description: "Admin accounts and their assigned roles.",
        rows: (adminUsers.data || []).map((item) => ({
          id: item.id,
          title: item.email,
          subtitle: item.role,
          meta: [item.status],
          status: item.status,
          updatedAt: item.createdAt,
          actions: [{ label: "Make Platform Admin", intent: "primary", execute: () => adminApi.assignAdminRole(item.id, "PLATFORM_ADMIN").then(noop) }],
        })),
      },
      {
        key: "advanced-access",
        title: "Groups, policies, grants, and temporary access",
        description: "Advanced RBAC records for enterprise access management.",
        rows: [
          ...(groups.data || []).map((item) => ({
            id: `G-${item.id}`,
            title: item.name,
            subtitle: item.description,
            meta: [`${item.permissions.length} permissions`],
            status: "GROUP",
          } satisfies AdminTableRow)),
          ...(policies.data || []).map((item) => ({
            id: `P-${item.id}`,
            title: item.roleName,
            subtitle: `${item.policyType}: ${item.policyValue}`,
            meta: [`Role #${item.roleId}`],
            status: "POLICY",
            updatedAt: item.createdAt,
          } satisfies AdminTableRow)),
          ...(resources.data || []).map((item) => ({
            id: `RP-${item.id}`,
            title: `${item.resourceType} #${item.resourceId}`,
            subtitle: item.permission,
            meta: [`User #${item.userId}`],
            status: "RESOURCE",
            updatedAt: item.createdAt,
          } satisfies AdminTableRow)),
          ...(temporary.data || []).map((item) => ({
            id: `TP-${item.id}`,
            title: item.permissionName,
            subtitle: item.permissionCode,
            meta: [`User #${item.userId}`, `Expires ${formatAdminDate(item.expiresAt)}`],
            status: "TEMPORARY",
            updatedAt: item.createdAt,
          } satisfies AdminTableRow)),
          ...(staff.data || []).map((item) => ({
            id: `ST-${item.id}`,
            title: item.roleName,
            subtitle: `Owner #${item.ownerId} staff #${item.staffId}`,
            meta: [`Role #${item.roleId}`],
            status: item.active ? "ACTIVE" : "INACTIVE",
            updatedAt: item.createdAt,
          } satisfies AdminTableRow)),
        ],
      },
      {
        key: "audit",
        title: "Permission audit",
        description: "Role assignment and permission changes.",
        rows: (audit.data || []).map((item) => ({
          id: item.id,
          title: item.action,
          subtitle: item.details,
          meta: [item.actor, item.entity],
          status: item.action,
          updatedAt: item.timestamp,
        })),
      },
    ],
  };
}

async function loadSystem(): Promise<ResourceState> {
  const [dashboard, settings, flags, maintenance, releases, audit] = await Promise.all([
    loadPart("system dashboard", adminApi.getAdminSystemDashboard),
    loadPart("settings", adminApi.getAdminSystemSettings),
    loadPart("feature flags", adminApi.getAdminFeatureFlags),
    loadPart("maintenance", adminApi.getAdminMaintenanceConfig),
    loadPart("releases", adminApi.getAdminReleaseVersions),
    loadPart("configuration audit", adminApi.getAdminConfigurationAudit),
  ]);

  const maintenanceRow: AdminTableRow[] = maintenance.data
    ? [{
      id: maintenance.data.id ?? "maintenance",
      title: "Maintenance mode",
      subtitle: maintenance.data.message,
      meta: [maintenance.data.allowAdminAccess ? "Admin access allowed" : "Admin access blocked", maintenance.data.startTime || "No start", maintenance.data.endTime || "No end"],
      status: maintenance.data.enabled ? "ENABLED" : "DISABLED",
      updatedAt: maintenance.data.startTime,
      actions: [{
        label: maintenance.data.enabled ? "Disable" : "Enable",
        intent: maintenance.data.enabled ? "danger" : "primary",
        execute: () => adminApi.saveAdminMaintenanceConfig({ ...maintenance.data as adminApi.AdminMaintenanceConfig, enabled: !maintenance.data?.enabled }).then(noop),
      }],
    }]
    : [];

  return {
    metrics: [
      { label: "Features", value: `${dashboard.data?.activeFeatures ?? flags.data?.filter((item) => item.enabled).length ?? 0}/${dashboard.data?.totalFeatures ?? flags.data?.length ?? 0}`, detail: "Active feature flags", tone: "emerald" },
      { label: "Maintenance", value: dashboard.data?.maintenanceStatus ? "Enabled" : "Disabled", detail: maintenance.data?.message || "No active message", tone: dashboard.data?.maintenanceStatus ? "amber" : "sky" },
      { label: "Version", value: dashboard.data?.currentVersion ?? "-", detail: `${dashboard.data?.pendingReleases ?? releases.data?.filter((item) => item.status !== "RELEASED").length ?? 0} pending releases`, tone: "violet" },
      { label: "Config Changes", value: dashboard.data?.recentConfigChanges?.length ?? audit.data?.length ?? "-", detail: "Recent audit events", tone: "slate" },
    ],
    errors: collectErrors(dashboard, settings, flags, maintenance, releases, audit),
    datasets: [
      {
        key: "settings",
        title: "Global settings",
        description: "Platform configuration values currently stored in backend.",
        rows: (settings.data || []).map((item) => ({
          id: item.id ?? item.key,
          title: item.key,
          subtitle: item.value,
          status: "SETTING",
        })),
      },
      {
        key: "feature-flags",
        title: "Feature flags",
        description: "Rollout and audience controls for platform features.",
        rows: (flags.data || []).map((item) => ({
          id: item.id ?? item.featureKey,
          title: item.featureKey,
          subtitle: item.description || item.targetAudience,
          meta: [`${item.rolloutPercentage}% rollout`, item.targetAudience],
          status: item.enabled ? "ACTIVE" : "INACTIVE",
          updatedAt: item.updatedAt,
          actions: [{
            label: item.enabled ? "Disable" : "Enable",
            intent: item.enabled ? "danger" : "primary",
            execute: () => adminApi.saveAdminFeatureFlag({ ...item, enabled: !item.enabled }).then(noop),
          }],
        })),
      },
      {
        key: "maintenance",
        title: "Maintenance mode",
        description: "Emergency and scheduled platform maintenance control.",
        rows: maintenanceRow,
      },
      {
        key: "releases",
        title: "Release management",
        description: "Version tracking, release notes, rollout percentage, and status.",
        rows: (releases.data || []).map((item) => ({
          id: item.id ?? item.version,
          title: item.version,
          subtitle: item.notes,
          meta: [`${item.rolloutPercentage}% rollout`],
          status: item.status,
          updatedAt: item.releaseDate,
        })),
      },
      {
        key: "audit",
        title: "Configuration audit",
        description: "Every configuration change recorded for operations review.",
        rows: (audit.data || []).map((item) => ({
          id: item.id,
          title: item.configKey,
          subtitle: `${item.oldValue || "-"} -> ${item.newValue}`,
          meta: [item.changedBy],
          status: "CONFIG_CHANGED",
          updatedAt: item.changedAt,
        })),
      },
    ],
  };
}

async function loadAnalytics(): Promise<ResourceState> {
  const [analytics, bi, reconciliation, revenueReport] = await Promise.all([
    loadPart("analytics", adminApi.getAdminExecutiveAnalytics),
    loadPart("BI workspace", adminApi.getAdminBiWorkspace),
    loadPart("BI reconciliation", adminApi.getAdminBiReconciliation),
    loadPart("revenue report", () => adminApi.getAdminBiReport({ type: "REVENUE", period: "MONTHLY" })),
  ]);
  const data = analytics.data;
  const biData = bi.data;

  return {
    metrics: [
      { label: "Revenue", value: formatMoney(data?.totalRevenue), detail: `${formatMoney(data?.monthlyRevenue)} monthly`, tone: "emerald" },
      { label: "Growth", value: `${data?.growthPercent ?? 0}%`, detail: `${data?.retentionPercent ?? 0}% retention`, tone: "sky" },
      { label: "BI Confidence", value: `${biData?.scorecard.analyticsConfidenceScore ?? 0}%`, detail: `${biData?.scorecard.decisionReadiness ?? "NEEDS_REVIEW"} decision readiness`, tone: "violet" },
      { label: "Data Quality", value: `${biData?.dataQuality.score ?? reconciliation.data?.score ?? 0}%`, detail: `${reconciliation.data?.mismatches.length ?? 0} reconciliation gaps`, tone: "rose" },
    ],
    errors: collectErrors(analytics, bi, reconciliation, revenueReport),
    datasets: [
      {
        key: "revenue",
        title: "Revenue and booking trend",
        description: "Executive revenue, user growth, and booking signals.",
        rows: [
          ...(data?.revenueTrend || []).map((item, index) => ({
            id: `REV-${index}`,
            title: item.date,
            subtitle: "Revenue trend",
            amount: formatMoney(item.revenue),
            status: "REVENUE",
          } satisfies AdminTableRow)),
          ...(data?.bookingTrend || []).map((item, index) => ({
            id: `BOOK-${index}`,
            title: item.date,
            subtitle: "Booking trend",
            meta: [`${item.bookings ?? 0} bookings`],
            status: "BOOKINGS",
          } satisfies AdminTableRow)),
          ...(data?.userGrowth || []).map((item, index) => ({
            id: `USER-${index}`,
            title: item.date,
            subtitle: "User growth",
            meta: [`${item.users ?? 0} users`],
            status: "USERS",
          } satisfies AdminTableRow)),
        ],
      },
      {
        key: "insights",
        title: "AI insights and forecasts",
        description: "Automated business insights and forecasting outputs.",
        rows: [
          ...(data?.aiInsights || []).map((item, index) => ({
            id: `AI-${index}`,
            title: item.title,
            subtitle: item.message,
            status: item.severity,
          } satisfies AdminTableRow)),
          {
            id: "forecast",
            title: "Next month forecast",
            subtitle: `Bookings ${data?.forecasting.expectedBookingsNextMonth ?? 0}, occupancy ${data?.forecasting.expectedOccupancy ?? 0}%`,
            amount: formatMoney(data?.forecasting.expectedRevenueNextMonth),
            status: data?.forecasting.model || "FORECAST",
          },
        ],
      },
      {
        key: "business-units",
        title: "Owners, turfs, retention, geography",
        description: "Business breakdowns used by founders and operators.",
        rows: [
          ...(data?.ownerAnalytics.topOwners || []).map((item, index) => recordToAnalyticsRow(`OWNER-${index}`, item, "Top owner")),
          ...(data?.turfAnalytics.revenuePerTurf || []).map((item, index) => recordToAnalyticsRow(`TURF-${index}`, item, "Turf revenue")),
          ...(data?.retentionAnalytics.cohorts || []).map((item, index) => recordToAnalyticsRow(`COHORT-${index}`, item, "Retention cohort")),
          ...(data?.geographicAnalytics.cities || []).map((item, index) => recordToAnalyticsRow(`CITY-${index}`, item, "Geographic performance")),
        ],
      },
      {
        key: "bi-scorecard",
        title: "BI scorecard and KPI coverage",
        description: "Decision-readiness controls for executive reporting, forecasting, and KPI coverage.",
        rows: [
          {
            id: "confidence",
            title: "Analytics confidence",
            subtitle: `${biData?.scorecard.analyticsConfidenceScore ?? 0}% confidence, ${biData?.scorecard.forecastConfidence ?? 0}% forecast confidence`,
            meta: [`Data quality ${biData?.scorecard.dataQualityScore ?? 0}%`, biData?.scorecard.decisionReadiness ?? "NEEDS_REVIEW"],
            status: biData?.scorecard.decisionReadiness ?? "NEEDS_REVIEW",
          },
          ...(biData?.scorecard.kpiCoverage || []).map((item, index) => ({
            id: `KPI-${index}`,
            title: item,
            subtitle: "Covered in EPIC 18 BI workspace",
            status: "KPI_COVERED",
          } satisfies AdminTableRow)),
        ],
      },
      {
        key: "reports",
        title: "Reporting engine",
        description: "Available executive and operational reports with export support.",
        rows: [
          ...(biData?.reportCatalog || []).map((item) => ({
            id: item.type,
            title: item.title,
            subtitle: item.description,
            meta: item.formats,
            status: item.type,
          } satisfies AdminTableRow)),
          ...(revenueReport.data?.rows || []).slice(0, 8).map((item, index) => recordToAnalyticsRow(`REPORT-${index}`, item, "Revenue report row")),
        ],
      },
      {
        key: "forecast-reconciliation",
        title: "Forecasting and reconciliation",
        description: "Forecast deviation tracking and cross-module data quality checks.",
        rows: [
          ...(biData?.forecastAccuracy || []).map((item) => ({
            id: `FORECAST-${item.metric}`,
            title: `${item.metric} forecast`,
            subtitle: `Forecast ${item.forecast}, actual ${item.actual}, deviation ${item.deviation}`,
            meta: [`Confidence ${item.confidence}%`],
            status: item.status,
          } satisfies AdminTableRow)),
          ...(reconciliation.data?.checks || []).map((item, index) => ({
            id: `RECON-${index}`,
            title: item.name,
            subtitle: `${item.records} records checked, ${item.mismatches} mismatch(es)`,
            status: item.status,
            priority: item.status === "PASS" ? "LOW" : "HIGH",
          } satisfies AdminTableRow)),
        ],
      },
      {
        key: "dashboard-builder",
        title: "Dashboard builder",
        description: "Reusable templates and widgets for owner, admin, executive, and data-quality dashboards.",
        rows: [
          ...(biData?.dashboardBuilder.templates || []).map((item) => ({
            id: item.key,
            title: item.name,
            subtitle: item.description,
            status: "TEMPLATE",
          } satisfies AdminTableRow)),
          ...(biData?.dashboardBuilder.widgets || []).map((item) => ({
            id: item.id,
            title: item.title,
            subtitle: `${item.type} widget from ${item.dataKey}`,
            status: item.type,
          } satisfies AdminTableRow)),
        ],
      },
    ],
  };
}

function renderTopActions(moduleKey: AdminResourceKey, refresh: () => Promise<void>) {
  const run = async (action: () => Promise<unknown>) => {
    await action();
    await refresh();
  };

  if (moduleKey === "finance") {
    return (
      <>
        <AdminActionButton label="Generate Settlements" intent="primary" onClick={() => void run(adminApi.generateAdminSettlements)} />
        <AdminActionButton label="Run Reconciliation" onClick={() => void run(adminApi.runAdminReconciliation)} />
      </>
    );
  }

  if (moduleKey === "bookings") {
    return <AdminActionButton label="Scan Slot Conflicts" intent="primary" onClick={() => void run(adminApi.scanAdminSlotConflicts)} />;
  }

  if (moduleKey === "safety") {
    return <AdminActionButton label="Scan Fraud Profiles" intent="primary" onClick={() => void run(adminApi.scanAdminSafetyFraudProfiles)} />;
  }

  if (moduleKey === "system") {
    return <AdminActionButton label="Seed Defaults" intent="primary" onClick={() => void run(adminApi.seedAdminSystemDefaults)} />;
  }

  if (moduleKey === "analytics") {
    return (
      <>
        <AdminActionButton label="Generate Revenue Report" intent="primary" onClick={() => void run(() => adminApi.getAdminBiReport({ type: "REVENUE", period: "MONTHLY" }))} />
        <AdminActionButton label="Export BI CSV" onClick={() => void run(() => adminApi.exportAdminBiReport({ type: "REVENUE", period: "MONTHLY", format: "CSV" }))} />
      </>
    );
  }

  return null;
}

async function loadPart<T>(key: string, request: () => Promise<ApiResponse<T>>): Promise<LoadPart<T>> {
  try {
    const response = await request();
    return { key, data: response.data };
  } catch (error) {
    return { key, error: readableError(error) };
  }
}

function collectErrors(...parts: LoadPart<unknown>[]) {
  return parts.filter((part) => part.error).map((part) => `${part.key}: ${part.error}`);
}

function matchesFilter(row: AdminTableRow, search: string, status: string) {
  const searchTerm = search.trim().toLowerCase();
  const statusTerm = status.toUpperCase();
  const haystack = [
    row.id,
    row.title,
    row.subtitle,
    row.status,
    row.priority,
    ...(row.meta || []),
    row.amount,
    row.updatedAt,
  ].join(" ").toLowerCase();

  const searchMatch = !searchTerm || haystack.includes(searchTerm);
  const statusMatch = statusTerm === "ALL" || row.status?.toUpperCase() === statusTerm || row.priority?.toUpperCase() === statusTerm || haystack.includes(statusTerm.toLowerCase());
  return searchMatch && statusMatch;
}

function readableError(error: unknown) {
  const record = toRecord(error);
  const response = toRecord(record.response);
  const data = toRecord(response.data);
  return stringValue(data.message) || stringValue(record.message) || "Request failed";
}

function unknownArray(part: LoadPart<unknown>) {
  return Array.isArray(part.data) ? part.data : [];
}

function toRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === "object" ? value as Record<string, unknown> : {};
}

function toNumber(value: unknown) {
  return typeof value === "number" ? value : undefined;
}

function stringValue(value: unknown) {
  return typeof value === "string" ? value : "";
}

function text(value: unknown, fallback: string) {
  if (typeof value === "string" && value.trim()) {
    return value;
  }
  if (typeof value === "number") {
    return String(value);
  }
  return fallback;
}

function moneyFromUnknown(value: unknown) {
  return typeof value === "number" ? formatMoney(value) : undefined;
}

function trimText(value: string, maxLength: number) {
  return value.length > maxLength ? `${value.slice(0, maxLength)}...` : value;
}

function recordToAnalyticsRow(id: string, record: Record<string, string | number>, label: string): AdminTableRow {
  const entries = Object.entries(record);
  const title = text(entries[0]?.[1], label);
  const subtitle = entries.slice(1, 3).map(([key, value]) => `${key}: ${value}`).join(" | ");
  return {
    id,
    title,
    subtitle,
    meta: entries.slice(3).map(([key, value]) => `${key}: ${value}`),
    status: label.toUpperCase().replaceAll(" ", "_"),
  };
}

function noop() {}
