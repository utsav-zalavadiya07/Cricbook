"use client";

import { useEffect, useMemo, useState } from "react";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import { getAdminOwnerInsights, OwnerInsights } from "@/modules/insights/services/insightsService";

export default function AdminOwnerInsightsPage() {
  const [insights, setInsights] = useState<OwnerInsights | null>(null);
  const [status, setStatus] = useState("");

  useEffect(() => {
    getAdminOwnerInsights()
      .then((res) => setInsights(res.data))
      .catch(() => setStatus("Unable to load platform owner insights."));
  }, []);

  const maxRevenue = useMemo(
    () => Math.max(...(insights?.revenue.dailyTrend.map((item) => Number(item.revenue)) || [1]), 1),
    [insights]
  );

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <section className="theme-panel rounded-[2rem] p-6 sm:p-8">
          <p className="theme-soft text-xs font-bold uppercase tracking-[0.28em]">Admin analytics</p>
          <h1 className="theme-title mt-3 text-3xl font-black sm:text-5xl">Owner Trends</h1>
          <p className="theme-muted mt-3 max-w-2xl text-sm leading-6">
            Platform-wide owner business health, marketplace occupancy, revenue trends, and optimization opportunities.
          </p>
        </section>

        {status && <div className="theme-panel-soft rounded-2xl p-4 text-sm font-semibold text-sky-600 dark:text-sky-200">{status}</div>}

        {insights ? (
          <>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <Metric label="Monthly GMV" value={formatMoney(insights.revenue.monthRevenue)} />
              <Metric label="Occupancy" value={`${insights.occupancy.occupancyPercent}%`} />
              <Metric label="Repeat Customers" value={String(insights.community.repeatCustomers)} />
              <Metric label="Demand" value={insights.forecast.demandLevel} />
            </div>

            <div className="grid gap-6 xl:grid-cols-3">
              <section className="theme-panel rounded-3xl p-6 xl:col-span-2">
                <h2 className="theme-title text-xl font-black">Revenue Trend</h2>
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
                <h2 className="theme-title text-xl font-black">Optimization Queue</h2>
                <div className="mt-4 space-y-3">
                  {insights.pricingSuggestions.slice(0, 5).map((item) => (
                    <div key={item.slotId} className="theme-panel-soft rounded-2xl p-4">
                      <p className="theme-title font-bold">{item.turfName}</p>
                      <p className="theme-muted mt-1 text-sm">{item.currentPrice} to {item.recommendedPrice} - {item.reason}</p>
                    </div>
                  ))}
                </div>
              </section>
            </div>
          </>
        ) : (
          <p className="theme-muted py-16 text-center">Loading platform owner insights...</p>
        )}
      </div>
    </DashboardLayout>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="theme-panel rounded-3xl p-5">
      <p className="theme-soft text-xs font-bold uppercase tracking-[0.22em]">{label}</p>
      <p className="theme-title mt-3 break-words text-3xl font-black">{value}</p>
    </div>
  );
}

function formatMoney(value: number) {
  return `Rs ${Math.round(Number(value || 0)).toLocaleString("en-IN")}`;
}
