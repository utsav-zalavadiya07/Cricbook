"use client";
import { TileInfo } from "@/components/ui";

import { useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import Link from "next/link";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import { useI18n } from "@/modules/i18n/I18nProvider";
import {
  addOwnerExpense,
  askBusinessCoach,
  getOwnerInsights,
  OwnerInsights,
} from "@/modules/insights/services/insightsService";

const today = new Date().toISOString().slice(0, 10);

export default function OwnerInsightsDashboard() {
  const { t } = useI18n();
  const [insights, setInsights] = useState<OwnerInsights | null>(null);
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState("");
  const [expense, setExpense] = useState({ type: "Electricity", amount: "", expenseDate: today, note: "" });
  const [question, setQuestion] = useState("Why is weekday revenue low?");

  const load = () => {
    setLoading(true);
    getOwnerInsights()
      .then((res) => {
        setInsights(res.data);
        setStatus("");
      })
      .catch((error) => setStatus(errorMessage(error, "Unable to load business insights.")))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    const timer = window.setTimeout(load, 0);
    return () => window.clearTimeout(timer);
  }, []);

  const maxRevenue = useMemo(
    () => Math.max(...(insights?.revenue.dailyTrend.map((item) => Number(item.revenue)) || [1]), 1),
    [insights]
  );

  const submitExpense = () => {
    const amount = Number(expense.amount);
    if (!amount || amount <= 0) {
      setStatus("Enter a valid expense amount.");
      return;
    }
    addOwnerExpense({ ...expense, amount })
      .then(() => {
        setExpense({ type: "Electricity", amount: "", expenseDate: today, note: "" });
        setStatus("Expense saved.");
        load();
      })
      .catch((error) => setStatus(errorMessage(error, "Expense could not be saved.")));
  };

  const askCoach = () => {
    askBusinessCoach(question)
      .then((res) => {
        setInsights((current) => current ? { ...current, coach: res.data } : current);
        setStatus("Business coach refreshed.");
      })
      .catch((error) => setStatus(errorMessage(error, "Business coach is unavailable right now.")));
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <section className="theme-panel overflow-hidden rounded-[2rem] p-6 sm:p-8">
          <div className="grid gap-6 xl:grid-cols-[1.4fr_0.8fr]">
            <div>
              <p className="theme-soft text-xs font-bold uppercase tracking-[0.28em]">{t("insights.epic")}</p>
              <div className="mt-3">
            <h1 className="theme-title max-w-3xl text-3xl font-black leading-tight sm:text-5xl">{t("insights.title")}
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Insights information" focusable={false}>{t("insights.subtitle")}</TileInfo>
              </span></h1>
          </div>
              <div className="mt-6 flex flex-wrap gap-3">
                <Link href="/owner/pricing" className="rounded-2xl bg-sky-500 px-4 py-3 text-sm font-bold text-white shadow-lg shadow-sky-500/20 hover:bg-sky-400">
                  {t("insights.action.pricing")}
                </Link>
                <Link href="/owner/resale" className="theme-card rounded-2xl px-4 py-3 text-sm font-bold">
                  {t("insights.action.marketplace")}
                </Link>
              </div>
            </div>
            <div className="theme-panel-soft rounded-3xl p-5">
              <p className="theme-soft text-xs uppercase tracking-[0.22em]">{t("insights.forecast")}</p>
              <p className="theme-title mt-3 text-3xl font-black">{formatMoney(insights?.forecast.expectedRevenue || 0)}</p>
              <p className="theme-muted mt-2 text-sm">{insights?.forecast.insight || t("common.loading")}</p>
              <div className="mt-4 grid grid-cols-2 gap-3">
                <MiniMetric label={t("insights.demand")} value={insights?.forecast.demandLevel || "--"} />
                <MiniMetric label={t("insights.occupancy")} value={`${insights?.forecast.expectedOccupancy || 0}%`} />
              </div>
            </div>
          </div>
        </section>

        {loading && <p className="theme-muted py-16 text-center">{t("common.loading")}</p>}
        {status && <div className="theme-panel-soft rounded-2xl p-4 text-sm font-semibold text-sky-600 dark:text-sky-200">{status}</div>}

        {!loading && insights && (
          <>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <Metric title={t("insights.todayRevenue")} value={formatMoney(insights.revenue.todayRevenue)} tone="emerald" />
              <Metric title={t("insights.monthRevenue")} value={formatMoney(insights.revenue.monthRevenue)} tone="sky" />
              <Metric title={t("insights.occupancyRate")} value={`${insights.occupancy.occupancyPercent}%`} tone="amber" />
              <Metric title={t("insights.profit")} value={formatMoney(insights.profit.profit)} tone="violet" />
            </div>

            <div className="grid gap-6 xl:grid-cols-3">
              <section className="theme-panel rounded-3xl p-6 xl:col-span-2">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <h2 className="theme-title text-xl font-black">{t("insights.revenueTrend")}</h2>
                    <p className="theme-muted mt-1 text-sm">{t("insights.revenueTrendBody")}</p>
                  </div>
                  <Badge value={`${insights.revenue.weeklyGrowthPercent}%`} label={t("insights.weeklyGrowth")} />
                </div>
                <div className="theme-panel-soft mt-6 flex h-60 items-end gap-2 rounded-3xl p-4">
                  {insights.revenue.dailyTrend.map((point) => {
                    const height = Math.max((Number(point.revenue) / maxRevenue) * 100, point.revenue > 0 ? 8 : 2);
                    return (
                      <div key={point.date} className="flex min-w-0 flex-1 flex-col items-center gap-2">
                        <div className="w-full rounded-t-xl bg-gradient-to-t from-sky-500 to-emerald-300" style={{ height: `${height}%` }} />
                        <span className="theme-soft truncate text-[10px]">{point.date.slice(5)}</span>
                      </div>
                    );
                  })}
                </div>
              </section>

              <section className="theme-panel rounded-3xl p-6">
                <h2 className="theme-title text-xl font-black">{t("insights.profitCenter")}</h2>
                <div className="mt-5 grid gap-3">
                  <MiniMetric label={t("insights.revenue")} value={formatMoney(insights.profit.revenue)} />
                  <MiniMetric label={t("insights.expenses")} value={formatMoney(insights.profit.expenses)} />
                  <MiniMetric label={t("insights.margin")} value={`${insights.profit.marginPercent}%`} />
                </div>
                <div className="theme-panel-soft mt-5 rounded-2xl p-4">
                  <p className="theme-title text-sm font-bold">{t("insights.addExpense")}</p>
                  <div className="mt-3 grid gap-2">
                    <input value={expense.type} onChange={(e) => setExpense((value) => ({ ...value, type: e.target.value }))} className="theme-input rounded-xl px-3 py-2 text-sm" placeholder={t("insights.expenseType")} />
                    <input value={expense.amount} onChange={(e) => setExpense((value) => ({ ...value, amount: e.target.value }))} className="theme-input rounded-xl px-3 py-2 text-sm" placeholder={t("insights.amount")} inputMode="decimal" />
                    <input value={expense.expenseDate} onChange={(e) => setExpense((value) => ({ ...value, expenseDate: e.target.value }))} className="theme-input rounded-xl px-3 py-2 text-sm" type="date" />
                    <button onClick={submitExpense} className="rounded-xl bg-sky-500 px-3 py-2 text-sm font-bold text-white hover:bg-sky-400">{t("insights.saveExpense")}</button>
                  </div>
                </div>
              </section>
            </div>

            <div className="grid gap-6 xl:grid-cols-2">
              <Panel title={t("insights.heatmap")} body={t("insights.heatmapBody")}>
                <div className="grid gap-3 sm:grid-cols-2">
                  {insights.heatmap.length === 0 && <Empty text={t("insights.emptyHeatmap")} />}
                  {insights.heatmap.map((cell) => (
                    <div key={`${cell.dayOfWeek}-${cell.hour}`} className="theme-panel-soft rounded-2xl p-4">
                      <div className="flex items-center justify-between gap-3">
                        <p className="theme-title font-bold">{cell.dayOfWeek} {cell.hourLabel}</p>
                        <span className={intensityClass(cell.intensity)}>{cell.intensity}</span>
                      </div>
                      <p className="theme-muted mt-2 text-sm">{cell.bookingCount} bookings - {cell.utilizationPercent}% utilization</p>
                    </div>
                  ))}
                </div>
              </Panel>

              <Panel title={t("insights.pricing")} body={t("insights.pricingBody")}>
                <div className="space-y-3">
                  {insights.pricingSuggestions.map((item) => (
                    <div key={item.slotId} className="theme-panel-soft rounded-2xl p-4">
                      <div className="flex flex-wrap items-center justify-between gap-3">
                        <p className="theme-title font-bold">{item.turfName}</p>
                        <p className="text-sm font-black text-sky-600 dark:text-sky-300">{formatMoney(item.currentPrice)} to {formatMoney(item.recommendedPrice)}</p>
                      </div>
                      <p className="theme-muted mt-2 text-sm">{item.reason}</p>
                      <p className="theme-soft mt-2 text-xs">{item.confidence}% confidence</p>
                    </div>
                  ))}
                </div>
              </Panel>
            </div>

            <div className="grid gap-6 xl:grid-cols-3">
              <Panel title={t("insights.customers")} body={t("insights.customersBody")}>
                <div className="space-y-3">
                  {insights.customers.length === 0 && <Empty text={t("insights.emptyCustomers")} />}
                  {insights.customers.slice(0, 5).map((customer) => (
                    <div key={customer.customerId} className="theme-panel-soft rounded-2xl p-4">
                      <p className="theme-title font-bold">{customer.customerName}</p>
                      <p className="theme-muted mt-1 text-sm">{customer.segment} - {customer.bookings} bookings - Trust {customer.trustScore}</p>
                    </div>
                  ))}
                </div>
              </Panel>

              <Panel title={t("insights.promotions")} body={t("insights.promotionsBody")}>
                <div className="space-y-3">
                  {insights.promotions.map((promo) => (
                    <div key={promo.title} className="theme-panel-soft rounded-2xl p-4">
                      <p className="theme-title font-bold">{promo.title}</p>
                      <p className="theme-muted mt-1 text-sm">{promo.description}</p>
                      <p className="theme-soft mt-2 text-xs">{promo.expectedImpact}</p>
                    </div>
                  ))}
                </div>
              </Panel>

              <Panel title={t("insights.aiCoach")} body={t("insights.aiCoachBody")}>
                <textarea value={question} onChange={(e) => setQuestion(e.target.value)} className="theme-input min-h-24 w-full rounded-2xl px-4 py-3 text-sm" />
                <button onClick={askCoach} className="mt-3 w-full rounded-2xl bg-sky-500 px-4 py-3 text-sm font-bold text-white hover:bg-sky-400">{t("insights.askCoach")}</button>
                <div className="theme-panel-soft mt-4 rounded-2xl p-4">
                  <p className="theme-title text-sm font-bold">{insights.coach.answer}</p>
                  <ul className="mt-3 space-y-2">
                    {insights.coach.recommendedActions.map((action) => (
                      <li key={action} className="theme-muted text-sm">{action}</li>
                    ))}
                  </ul>
                </div>
              </Panel>
            </div>
          </>
        )}
      </div>
    </DashboardLayout>
  );
}

function Metric({ title, value, tone }: { title: string; value: string; tone: "emerald" | "sky" | "amber" | "violet" }) {
  const toneClass = {
    emerald: "text-emerald-600 dark:text-emerald-300",
    sky: "text-sky-600 dark:text-sky-300",
    amber: "text-amber-600 dark:text-amber-300",
    violet: "text-violet-600 dark:text-violet-300",
  }[tone];
  return (
    <div className="theme-panel rounded-3xl p-5">
      <p className="theme-soft text-xs font-bold uppercase tracking-[0.22em]">{title}</p>
      <p className={`mt-3 break-words text-3xl font-black ${toneClass}`}>{value}</p>
    </div>
  );
}

function MiniMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="theme-card rounded-2xl p-4">
      <p className="theme-soft text-xs uppercase tracking-[0.18em]">{label}</p>
      <p className="theme-title mt-2 break-words text-lg font-black">{value}</p>
    </div>
  );
}

function Badge({ label, value }: { label: string; value: string }) {
  return (
    <div className="theme-card rounded-2xl px-4 py-3 text-right">
      <p className="theme-soft text-[10px] uppercase tracking-[0.2em]">{label}</p>
      <p className="theme-title font-black">{value}</p>
    </div>
  );
}

function Panel({ title, body, children }: { title: string; body: string; children: ReactNode }) {
  return (
    <section className="theme-panel rounded-3xl p-6">
      <h2 className="theme-title text-xl font-black">{title}</h2>
      <p className="theme-muted mt-1 text-sm">{body}</p>
      <div className="mt-5">{children}</div>
    </section>
  );
}

function Empty({ text }: { text: string }) {
  return <p className="theme-card theme-muted rounded-2xl border-dashed p-6 text-center text-sm">{text}</p>;
}

function intensityClass(intensity: string) {
  if (intensity === "HIGH") return "rounded-full bg-red-500/10 px-3 py-1 text-xs font-bold text-red-600 dark:text-red-300";
  if (intensity === "MEDIUM") return "rounded-full bg-amber-500/10 px-3 py-1 text-xs font-bold text-amber-600 dark:text-amber-300";
  return "rounded-full bg-emerald-500/10 px-3 py-1 text-xs font-bold text-emerald-600 dark:text-emerald-300";
}

function formatMoney(value: number) {
  return `Rs ${Math.round(Number(value || 0)).toLocaleString("en-IN")}`;
}

function errorMessage(error: unknown, fallback: string) {
  if (typeof error === "object" && error !== null && "response" in error) {
    const response = (error as { response?: { data?: { message?: string } } }).response;
    return response?.data?.message || fallback;
  }
  return fallback;
}
