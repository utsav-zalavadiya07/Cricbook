"use client";

import { useEffect, useMemo, useState } from "react";
import { TileInfo } from "@/components/ui";
import {
  AdminWorkforceAnalytics,
  AdminWorkforceDashboard,
  AdminWorkforceMap,
  AdminWorkforceReports,
  getAdminWorkforceAnalytics,
  getAdminWorkforceAttendance,
  getAdminWorkforceDashboard,
  getAdminWorkforceEmergencies,
  getAdminWorkforceEquipment,
  getAdminWorkforceMaintenance,
  getAdminWorkforceReports,
  getAdminWorkforceTasks,
} from "@/modules/admin/services/adminService";

export default function AdminWorkforcePage() {
  const [dashboard, setDashboard] = useState<AdminWorkforceDashboard | null>(null);
  const [tasks, setTasks] = useState<AdminWorkforceMap[]>([]);
  const [maintenance, setMaintenance] = useState<AdminWorkforceMap[]>([]);
  const [attendance, setAttendance] = useState<AdminWorkforceMap[]>([]);
  const [equipment, setEquipment] = useState<AdminWorkforceMap[]>([]);
  const [emergencies, setEmergencies] = useState<AdminWorkforceMap[]>([]);
  const [analytics, setAnalytics] = useState<AdminWorkforceAnalytics | null>(null);
  const [reports, setReports] = useState<AdminWorkforceReports | null>(null);
  const [loading, setLoading] = useState(true);

  async function loadWorkforce() {
    try {
      setLoading(true);
      const [dashboardRes, tasksRes, maintenanceRes, attendanceRes, equipmentRes, emergenciesRes, analyticsRes, reportsRes] = await Promise.all([
        getAdminWorkforceDashboard(),
        getAdminWorkforceTasks(),
        getAdminWorkforceMaintenance(),
        getAdminWorkforceAttendance(),
        getAdminWorkforceEquipment(),
        getAdminWorkforceEmergencies(),
        getAdminWorkforceAnalytics(),
        getAdminWorkforceReports(),
      ]);
      setDashboard(dashboardRes.data);
      setTasks(tasksRes.data);
      setMaintenance(maintenanceRes.data);
      setAttendance(attendanceRes.data);
      setEquipment(equipmentRes.data);
      setEmergencies(emergenciesRes.data);
      setAnalytics(analyticsRes.data);
      setReports(reportsRes.data);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void loadWorkforce(), 0);
    return () => window.clearTimeout(timer);
  }, []);

  const metrics = useMemo(
    () => [
      { label: "Today Bookings", value: formatNumber(dashboard?.kpis?.todayBookings), detail: "ground execution queue" },
      { label: "Pending Check-Ins", value: formatNumber(dashboard?.kpis?.pendingCheckIns), detail: "QR/manual validation" },
      { label: "Open Tasks", value: formatNumber(dashboard?.kpis?.openTasks), detail: "staff task lifecycle" },
      { label: "Maintenance", value: formatNumber(dashboard?.kpis?.maintenanceIssues), detail: "open facility issues" },
      { label: "Emergencies", value: formatNumber(dashboard?.kpis?.emergencyAlerts), detail: "active escalation alerts" },
      { label: "Damaged Items", value: formatNumber(dashboard?.kpis?.equipmentDamaged), detail: "inventory repair queue" },
      { label: "Attendance", value: `${formatNumber(analytics?.attendancePercent)}%`, detail: "shift compliance" },
      { label: "Performance", value: `${formatNumber(analytics?.performanceScore)}%`, detail: "staff execution score" },
    ],
    [dashboard, analytics]
  );

  return (
    <main className="min-h-screen bg-slate-950 px-4 py-8 text-white sm:px-8" data-i18n-skip="true">
      <div className="mx-auto max-w-7xl space-y-8">
        <header className="rounded-[2rem] border border-white/10 bg-gradient-to-br from-cyan-500/15 via-slate-900 to-emerald-500/10 p-6 shadow-2xl">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.26em] text-cyan-300">EPIC 21 Staff Operations</p>
              <h1 className="mt-3 text-3xl font-black text-white sm:text-4xl">Workforce Execution Center</h1>
              <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-300">
                Monitor staff attendance, QR check-ins, daily schedules, task execution, maintenance SLA, equipment inventory, emergency alerts, and workforce analytics.
              </p>
            </div>
            <button onClick={() => void loadWorkforce()} className="rounded-2xl bg-cyan-500 px-4 py-3 text-sm font-black text-slate-950 hover:bg-cyan-400">
              Refresh Workforce
            </button>
          </div>
        </header>

        {loading ? (
          <section className="rounded-[2rem] border border-white/10 bg-white/[0.04] p-10 text-center text-sm font-bold text-slate-300">Loading workforce control plane...</section>
        ) : (
          <>
            <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              {metrics.map((metric) => (
                <MetricCard key={metric.label} {...metric} />
              ))}
            </section>

            <section className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
              <Panel title="Task Lifecycle" subtitle="Pending, accepted, in-progress, completed, cancelled, and escalated staff work.">
                <WorkforceTable
                  rows={tasks}
                  columns={[
                    ["title", "Task"],
                    ["priority", "Priority"],
                    ["status", "Status"],
                    ["staffName", "Staff"],
                    ["centerName", "Center"],
                  ]}
                  empty="No workforce tasks yet."
                />
              </Panel>

              <Panel title="Check-In & Attendance" subtitle="Shift compliance and staff-recorded venue execution.">
                <div className="grid gap-3 sm:grid-cols-2">
                  <MetricCard label="Task Completion" value={`${formatNumber(analytics?.taskCompletionPercent)}%`} detail="completed work ratio" compact />
                  <MetricCard label="Issue Resolution" value={`${formatNumber(analytics?.issueResolutionPercent)}%`} detail="maintenance closure ratio" compact />
                </div>
                <div className="mt-4 space-y-3">
                  {attendance.slice(0, 5).map((item) => (
                    <CompactRow key={String(item.id)} title={String(item.staffName || "Staff")} meta={`${String(item.centerName || "Center")} | ${String(item.workDate || "--")} | ${formatNumber(item.durationMinutes)} min`} status={String(item.status || "OPEN")} />
                  ))}
                  {attendance.length === 0 ? <p className="text-sm text-slate-400">No attendance records yet.</p> : null}
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 lg:grid-cols-3">
              <Panel title="Maintenance SLA" subtitle="Ground, floodlight, net, washroom, and facility tickets.">
                <div className="space-y-3">
                  {maintenance.slice(0, 6).map((item) => (
                    <CompactRow key={String(item.id)} title={String(item.title || "Maintenance ticket")} meta={`${String(item.category || "GROUND")} | ${String(item.centerName || "Center")} | SLA ${String(item.slaDueAt || "--")}`} status={String(item.status || "OPEN")} />
                  ))}
                  {maintenance.length === 0 ? <p className="text-sm text-slate-400">No maintenance tickets yet.</p> : null}
                </div>
              </Panel>

              <Panel title="Equipment Inventory" subtitle="Available, in-use, damaged, and repair workflow status.">
                <div className="space-y-3">
                  {equipment.slice(0, 6).map((item) => (
                    <CompactRow key={String(item.id)} title={String(item.name || "Equipment")} meta={`${String(item.equipmentType || "ITEM")} | stock ${formatNumber(item.stockCount)} | damaged ${formatNumber(item.damagedCount)}`} status={String(item.status || "AVAILABLE")} />
                  ))}
                  {equipment.length === 0 ? <p className="text-sm text-slate-400">No equipment records yet.</p> : null}
                </div>
              </Panel>

              <Panel title="Emergency Alerts" subtitle="Medical, fire, power, and security escalation workflows.">
                <div className="space-y-3">
                  {emergencies.slice(0, 6).map((item) => (
                    <CompactRow key={String(item.id)} title={String(item.type || "Emergency")} meta={`${String(item.severity || "HIGH")} | ${String(item.centerName || "Center")} | ${String(item.escalationLevel || "CENTER_MANAGER")}`} status={String(item.status || "OPEN")} />
                  ))}
                  {emergencies.length === 0 ? <p className="text-sm text-slate-400">No active emergency alerts.</p> : null}
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
              <Panel title="Top Performers" subtitle="Attendance, task completion, and issue-resolution weighted scores.">
                <div className="space-y-3">
                  {(analytics?.topPerformers || []).slice(0, 6).map((item) => (
                    <CompactRow key={String(item.staffId)} title={String(item.staffName || "Staff")} meta={`${formatNumber(item.tasks)} task(s) | ${formatNumber(item.attendanceRecords)} attendance record(s)`} status={`${formatNumber(item.performanceScore)}%`} />
                  ))}
                </div>
              </Panel>

              <Panel title="Reports & Exports" subtitle="Attendance, task, maintenance, and staff performance reports.">
                <div className="grid gap-3 sm:grid-cols-3">
                  {(reports?.exports || []).map((item) => (
                    <div key={String(item.fileName)} className="rounded-2xl border border-white/10 bg-slate-950/50 p-4">
                      <p className="text-xs font-black uppercase tracking-[0.2em] text-cyan-300">{String(item.format)}</p>
                      <p className="mt-2 break-words text-sm font-black text-white">{String(item.fileName)}</p>
                      <p className="mt-2 text-xs text-slate-500">{String(item.status || "READY")}</p>
                    </div>
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
      <div className="flex min-w-0 items-start justify-between gap-2">
        <p className="min-w-0 break-words text-[10px] font-black uppercase leading-5 tracking-[0.22em] text-slate-500">{label}</p>
        <TileInfo label={`${label} information`}>{detail}</TileInfo>
      </div>
      <p className={`mt-2 font-black text-white ${compact ? "text-2xl" : "text-3xl"}`}>{value}</p>
    </div>
  );
}

function WorkforceTable({ rows, columns, empty }: { rows: AdminWorkforceMap[]; columns: Array<[string, string]>; empty: string }) {
  if (rows.length === 0) {
    return <p className="py-6 text-center text-sm text-slate-400">{empty}</p>;
  }
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[680px] text-left text-sm">
        <thead className="text-xs uppercase tracking-[0.16em] text-slate-500">
          <tr>
            {columns.map(([, label]) => (
              <th key={label} className="pb-3">{label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.slice(0, 8).map((row) => (
            <tr key={String(row.id)} className="border-t border-white/10 text-slate-300">
              {columns.map(([key]) => (
                <td key={key} className="py-4">{String(row[key] ?? "--")}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
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

function formatNumber(value: unknown) {
  const numericValue = Number(value ?? 0);
  return (Number.isFinite(numericValue) ? numericValue : 0).toLocaleString("en-IN", { maximumFractionDigits: 2 });
}
