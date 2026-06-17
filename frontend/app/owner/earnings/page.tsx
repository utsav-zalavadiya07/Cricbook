"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import {
  exportOwnerFinanceCsv,
  generateOwnerSettlements,
  getOwnerFinanceDashboard,
  getOwnerTurfs,
  type OwnerFinanceDashboard,
  type OwnerFinanceFilters,
} from "@/modules/owner/services/ownerService";

type TurfOption = { id: number; name: string };
type TurfApiItem = { id?: number; name?: string };

const currency = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 0,
});

export default function OwnerEarningsPage() {
  const [finance, setFinance] = useState<OwnerFinanceDashboard | null>(null);
  const [turfs, setTurfs] = useState<TurfOption[]>([]);
  const [start, setStart] = useState("");
  const [end, setEnd] = useState("");
  const [turfId, setTurfId] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const filters: OwnerFinanceFilters = useMemo(() => ({ start, end, turfId }), [start, end, turfId]);

  const loadFinance = useCallback(async (overrideFilters = filters) => {
    setLoading(true);
    setError("");
    try {
      const response = await getOwnerFinanceDashboard(overrideFilters);
      setFinance(response.data);
    } catch (err: unknown) {
      setFinance(null);
      setError(errorMessage(err, "Could not load owner finance data."));
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => {
    let mounted = true;
    getOwnerFinanceDashboard()
      .then((response) => {
        if (mounted) setFinance(response.data);
      })
      .catch((err: unknown) => {
        if (mounted) {
          setFinance(null);
          setError(errorMessage(err, "Could not load owner finance data."));
        }
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });
    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    getOwnerTurfs()
      .then((response) => setTurfs((response.data || [])
        .filter((item: TurfApiItem) => item.id && item.name)
        .map((item: TurfApiItem) => ({ id: Number(item.id), name: String(item.name) }))))
      .catch(() => setTurfs([]));
  }, []);

  const handleGenerateSettlements = async () => {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      const response = await generateOwnerSettlements();
      setFinance(response.data.dashboard);
      setNotice(`${response.data.created} settlement${response.data.created === 1 ? "" : "s"} generated.`);
    } catch (err: unknown) {
      setError(errorMessage(err, "Could not generate settlements."));
    } finally {
      setBusy(false);
    }
  };

  const handleExport = async () => {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      const response = await exportOwnerFinanceCsv(filters);
      const url = window.URL.createObjectURL(response.data);
      const link = document.createElement("a");
      link.href = url;
      link.download = "owner-finance-report.csv";
      link.click();
      window.URL.revokeObjectURL(url);
      setNotice("Finance report exported.");
    } catch (err: unknown) {
      setError(errorMessage(err, "Could not export finance report."));
    } finally {
      setBusy(false);
    }
  };

  const resetFilters = () => {
    setStart("");
    setEnd("");
    setTurfId("");
    void loadFinance({});
  };

  const maxTrend = Math.max(...(finance?.revenueTrend || []).map((item) => Number(item.revenue || 0)), 1);

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <header className="theme-panel rounded-3xl p-6">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="theme-soft text-xs font-black uppercase tracking-[0.32em]">EPIC 9 · Revenue Operations</p>
              <h1 className="theme-title mt-3 text-4xl font-black">Revenue, settlements and payout control.</h1>
              <p className="theme-muted mt-3 max-w-3xl">
                Track paid bookings, refund impact, commission, GST on commission, settlement status, and owner payout readiness from one place.
              </p>
            </div>
            <div className="flex flex-wrap gap-3">
              <button className="theme-button-secondary" disabled={busy} onClick={handleExport}>Export CSV</button>
              <button className="theme-button" disabled={busy} onClick={handleGenerateSettlements}>Generate Settlements</button>
            </div>
          </div>
        </header>

        <section className="theme-panel rounded-3xl p-5">
          <div className="grid gap-4 md:grid-cols-4">
            <Field label="Start date">
              <input className="theme-input" type="date" value={start} onChange={(event) => setStart(event.target.value)} />
            </Field>
            <Field label="End date">
              <input className="theme-input" type="date" value={end} onChange={(event) => setEnd(event.target.value)} />
            </Field>
            <Field label="Turf">
              <select className="theme-input" value={turfId} onChange={(event) => setTurfId(event.target.value)}>
                <option value="">All turfs</option>
                {turfs.map((turf) => <option key={turf.id} value={turf.id}>{turf.name}</option>)}
              </select>
            </Field>
            <div className="flex items-end gap-3">
              <button className="theme-button-secondary w-full" onClick={resetFilters}>Clear</button>
              <button className="theme-button w-full" disabled={loading} onClick={() => void loadFinance()}>Apply</button>
            </div>
          </div>
        </section>

        {notice && <p className="rounded-2xl border border-emerald-500/30 bg-emerald-500/10 p-4 text-sm font-semibold text-emerald-300">{notice}</p>}
        {error && <p className="rounded-2xl border border-red-500/30 bg-red-500/10 p-4 text-sm font-semibold text-red-300">{error}</p>}

        {loading && <p className="theme-panel theme-muted rounded-3xl p-12 text-center">Loading owner finance dashboard...</p>}

        {!loading && finance && (
          <>
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
              <Metric title="Today Revenue" value={currencyValue(finance.todayRevenue)} />
              <Metric title="Monthly Revenue" value={currencyValue(finance.monthlyRevenue)} />
              <Metric title="Net Collected" value={currencyValue(finance.netCollectedRevenue)} />
              <Metric title="Net Payable" value={currencyValue(finance.netPayable)} accent />
              <Metric title="Pending Settlements" value={currencyValue(finance.pendingSettlementAmount)} sub={`${finance.pendingSettlements} open`} />
              <Metric title="Paid Settlements" value={currencyValue(finance.paidSettlementAmount)} sub={`${finance.paidSettlements} paid`} />
              <Metric title="Commission Due" value={currencyValue(finance.commissionDue)} sub={`${finance.commissionRate}% commission`} />
              <Metric title="GST On Commission" value={currencyValue(finance.gstOnCommission)} sub={`${finance.gstRate}% GST`} />
            </div>

            <section className="grid gap-5 xl:grid-cols-[1fr_0.9fr]">
              <div className="theme-panel rounded-3xl p-6">
                <div className="flex items-center justify-between gap-4">
                  <div>
                    <h2 className="theme-title text-xl font-black">Revenue Trend</h2>
                    <p className="theme-muted text-sm">{finance.insight}</p>
                  </div>
                  <p className="theme-soft text-xs font-bold uppercase tracking-[0.24em]">Paid bookings {finance.paidBookings}</p>
                </div>
                <div className="mt-6 flex h-52 items-end gap-2 overflow-x-auto">
                  {finance.revenueTrend.map((point) => (
                    <div key={point.date} className="flex min-w-10 flex-1 flex-col items-center gap-2">
                      <div
                        className="w-full rounded-t-xl bg-gradient-to-t from-emerald-500 to-sky-400"
                        style={{ height: `${Math.max((Number(point.revenue || 0) / maxTrend) * 180, point.revenue ? 8 : 2)}px` }}
                        title={`${point.date}: ${currencyValue(point.revenue)}`}
                      />
                      <span className="theme-muted text-[10px]">{point.date?.slice(5)}</span>
                    </div>
                  ))}
                </div>
              </div>

              <div className="theme-panel rounded-3xl p-6">
                <h2 className="theme-title text-xl font-black">Collection Snapshot</h2>
                <div className="mt-5 space-y-4">
                  <Row label="Gross revenue" value={currencyValue(finance.grossRevenue)} />
                  <Row label="Refund deductions" value={currencyValue(finance.refundedAmount)} danger />
                  <Row label="Lifetime revenue" value={currencyValue(finance.lifetimeRevenue)} />
                  <Row label="Refunded payments" value={finance.refundedPayments} />
                </div>
              </div>
            </section>

            <section className="theme-panel rounded-3xl p-6">
              <TableHeader title="Turf Revenue" subtitle="Revenue grouped by turf for the selected period." />
              <div className="mt-4 overflow-x-auto">
                <table className="min-w-full text-left text-sm">
                  <thead className="theme-soft uppercase tracking-[0.18em]">
                    <tr>
                      <th className="py-3">Turf</th>
                      <th className="py-3">Paid Bookings</th>
                      <th className="py-3">Average Booking</th>
                      <th className="py-3 text-right">Revenue</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/10">
                    {finance.turfRevenue.map((item) => (
                      <tr key={item.turfId}>
                        <td className="theme-title py-4 font-semibold">{item.turfName}</td>
                        <td className="theme-muted py-4">{item.paidBookings}</td>
                        <td className="theme-muted py-4">{currencyValue(item.averageBookingValue)}</td>
                        <td className="py-4 text-right font-black text-emerald-300">{currencyValue(item.revenue)}</td>
                      </tr>
                    ))}
                    {finance.turfRevenue.length === 0 && <EmptyRow colSpan={4} text="No paid turf revenue in this period." />}
                  </tbody>
                </table>
              </div>
            </section>

            <section className="grid gap-5 xl:grid-cols-2">
              <DataTable title="Settlements" subtitle="Generated payout rows, net of commission and GST on commission.">
                <thead className="theme-soft uppercase tracking-[0.18em]">
                  <tr>
                    <th className="py-3">Reference</th>
                    <th className="py-3">Booking</th>
                    <th className="py-3">Status</th>
                    <th className="py-3 text-right">Net</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/10">
                  {finance.settlements.map((item) => (
                    <tr key={item.id}>
                      <td className="theme-title py-4 font-semibold">{item.reference || `#${item.id}`}</td>
                      <td className="theme-muted py-4">#{item.bookingId || "--"}</td>
                      <td className="py-4"><StatusBadge status={item.status} /></td>
                      <td className="py-4 text-right font-bold">{currencyValue(item.netAmount)}</td>
                    </tr>
                  ))}
                  {finance.settlements.length === 0 && <EmptyRow colSpan={4} text="No settlements generated yet." />}
                </tbody>
              </DataTable>

              <DataTable title="Recent Transactions" subtitle="Latest successful payment records with refund deductions.">
                <thead className="theme-soft uppercase tracking-[0.18em]">
                  <tr>
                    <th className="py-3">Booking</th>
                    <th className="py-3">Turf</th>
                    <th className="py-3">Status</th>
                    <th className="py-3 text-right">Amount</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/10">
                  {finance.transactions.map((item) => (
                    <tr key={item.paymentId}>
                      <td className="theme-title py-4 font-semibold">#{item.bookingId || item.paymentId}</td>
                      <td className="theme-muted py-4">{item.turfName || "--"}</td>
                      <td className="py-4"><StatusBadge status={item.status} /></td>
                      <td className="py-4 text-right">
                        <p className="font-bold">{currencyValue(item.amount)}</p>
                        {Number(item.refundedAmount || 0) > 0 && <p className="text-xs text-red-300">Refund {currencyValue(item.refundedAmount)}</p>}
                      </td>
                    </tr>
                  ))}
                  {finance.transactions.length === 0 && <EmptyRow colSpan={4} text="No paid transactions in this period." />}
                </tbody>
              </DataTable>
            </section>
          </>
        )}
      </div>
    </DashboardLayout>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="theme-title mb-2 block text-sm font-bold">{label}</span>
      {children}
    </label>
  );
}

function Metric({ title, value, sub, accent = false }: { title: string; value: string; sub?: string; accent?: boolean }) {
  return (
    <div className={`theme-panel rounded-3xl p-5 ${accent ? "ring-1 ring-emerald-400/40" : ""}`}>
      <p className="theme-soft text-xs font-black uppercase tracking-[0.24em]">{title}</p>
      <p className={`mt-3 text-3xl font-black ${accent ? "text-emerald-300" : "theme-title"}`}>{value}</p>
      {sub && <p className="theme-muted mt-1 text-sm">{sub}</p>}
    </div>
  );
}

function Row({ label, value, danger = false }: { label: string; value: string | number; danger?: boolean }) {
  return (
    <div className="flex items-center justify-between gap-4 border-b border-white/10 pb-3">
      <span className="theme-muted">{label}</span>
      <span className={`font-black ${danger ? "text-red-300" : "theme-title"}`}>{value}</span>
    </div>
  );
}

function TableHeader({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <div>
      <h2 className="theme-title text-xl font-black">{title}</h2>
      <p className="theme-muted text-sm">{subtitle}</p>
    </div>
  );
}

function DataTable({ title, subtitle, children }: { title: string; subtitle: string; children: ReactNode }) {
  return (
    <section className="theme-panel rounded-3xl p-6">
      <TableHeader title={title} subtitle={subtitle} />
      <div className="mt-4 overflow-x-auto">
        <table className="min-w-full text-left text-sm">{children}</table>
      </div>
    </section>
  );
}

function StatusBadge({ status }: { status: string }) {
  const normalized = status?.toUpperCase() || "UNKNOWN";
  const tone = normalized === "PAID" || normalized === "SUCCESS"
    ? "border-emerald-400/40 bg-emerald-500/10 text-emerald-300"
    : normalized === "PENDING" || normalized === "PROCESSING"
      ? "border-amber-400/40 bg-amber-500/10 text-amber-300"
      : "border-slate-400/30 bg-slate-500/10 text-slate-300";
  return <span className={`rounded-full border px-3 py-1 text-xs font-black ${tone}`}>{normalized}</span>;
}

function EmptyRow({ colSpan, text }: { colSpan: number; text: string }) {
  return (
    <tr>
      <td colSpan={colSpan} className="theme-muted py-10 text-center">{text}</td>
    </tr>
  );
}

function currencyValue(value?: number) {
  return currency.format(Number(value || 0));
}

function errorMessage(error: unknown, fallback: string) {
  if (typeof error === "object" && error !== null && "response" in error) {
    const response = (error as { response?: { data?: { message?: string } } }).response;
    return response?.data?.message || fallback;
  }
  return fallback;
}
