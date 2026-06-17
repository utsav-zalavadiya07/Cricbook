"use client";

import { useEffect, useState } from "react";
import { getAdminRevenue } from "@/modules/membership/services/subscriptionService";

type FinancialStats = {
  mrr: number;
  arr: number;
  ltv: number;
  totalRevenue: number;
  churnRate: number;
  conversionRate: number;
  subscriptionRevenue: number;
  bookingRevenue: number;
  promotionRevenue: number;
  totalUsers: number;
};

export default function AdminRevenueDashboard() {
  const [stats, setStats] = useState<FinancialStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getAdminRevenue()
      .then((res) => setStats(res.data))
      .catch((err) => console.error(err))
      .finally(() => setLoading(false));
  }, []);

  return (
    <main className="min-h-screen bg-gradient-to-b from-slate-950 via-slate-900 to-slate-950 px-4 py-16 sm:px-6 text-white">
      <div className="mx-auto max-w-6xl space-y-8">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight">Financial Intelligence Room</h1>
          <p className="mt-1.5 text-sm text-slate-400">
            Real-time tracking of MRR, ARR, player conversions, LTV, and churn performance metrics.
          </p>
        </div>

        {loading ? (
          <div className="py-20 text-center text-slate-400 font-semibold animate-pulse">
            Calculating high-fidelity financial analytics...
          </div>
        ) : stats ? (
          <>
            {/* KPI Cards Grid */}
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <Metric title="Monthly Recurring (MRR)" value={`Rs ${stats.mrr}`} />
              <Metric title="Annual Recurring (ARR)" value={`Rs ${stats.arr}`} />
              <Metric title="Total Platform Rev" value={`Rs ${stats.totalRevenue}`} />
              <Metric title="Player Lifetime Value" value={`Rs ${stats.ltv}`} />
            </div>

            {/* Middle Section: Funnel & Revenue Distribution */}
            <div className="grid gap-6 md:grid-cols-3">
              {/* Conversion Performance Card */}
              <div className="md:col-span-1 rounded-3xl border border-white/10 bg-white/[0.03] p-6 space-y-4 backdrop-blur-xl">
                <h2 className="text-lg font-bold">Conversion & Churn</h2>
                <div className="mt-4 grid gap-4 grid-cols-2">
                  <div className="bg-black/20 rounded-2xl p-4 border border-white/5 text-center">
                    <span className="block text-2xs uppercase tracking-wider text-slate-500 font-extrabold">Conversion</span>
                    <span className="block text-2xl font-black text-emerald-400 mt-2">{stats.conversionRate}%</span>
                  </div>
                  <div className="bg-black/20 rounded-2xl p-4 border border-white/5 text-center">
                    <span className="block text-2xs uppercase tracking-wider text-slate-500 font-extrabold">Churn Rate</span>
                    <span className="block text-2xl font-black text-rose-400 mt-2">{stats.churnRate}%</span>
                  </div>
                </div>
                <div className="bg-black/20 rounded-2xl p-4 border border-white/5 space-y-2 text-xs text-slate-400">
                  <p>Total Registered Users: <span className="text-white font-bold">{stats.totalUsers}</span></p>
                  <p>Average conversion benchmark: <span className="text-emerald-400 font-semibold">10% - 15%</span></p>
                </div>
              </div>

              {/* Revenue Splits Graph Card */}
              <div className="md:col-span-2 rounded-3xl border border-white/10 bg-white/[0.03] p-6 backdrop-blur-xl space-y-4">
                <h2 className="text-lg font-bold">Revenue Source Distribution</h2>
                <div className="mt-6 flex h-48 items-end gap-6 rounded-2xl border border-white/5 bg-black/30 p-5">
                  <Bar title="Subscriptions" amount={stats.subscriptionRevenue} max={stats.totalRevenue} color="bg-emerald-400" />
                  <Bar title="Bookings" amount={stats.bookingRevenue} max={stats.totalRevenue} color="bg-sky-400" />
                  <Bar title="Promotions" amount={stats.promotionRevenue} max={stats.totalRevenue} color="bg-indigo-400" />
                </div>
              </div>
            </div>
          </>
        ) : (
          <p className="text-slate-400 text-center font-bold">Failed to load revenue stats.</p>
        )}
      </div>
    </main>
  );
}

function Metric({ title, value }: { title: string; value: string | number }) {
  return (
    <div className="rounded-3xl border border-white/10 bg-white/[0.03] p-6 shadow-lg backdrop-blur-xl">
      <p className="text-xs font-bold uppercase tracking-widest text-slate-500">{title}</p>
      <p className="mt-3 text-3xl font-black text-white">{value}</p>
    </div>
  );
}

function Bar({ title, amount, max, color }: { title: string; amount: number; max: number; color: string }) {
  const percent = max > 0 ? (amount / max) * 100 : 0;
  return (
    <div className="flex-1 flex flex-col items-center gap-3">
      <span className="text-xs font-bold text-slate-400">Rs {amount}</span>
      <div className="w-full bg-white/5 rounded-full overflow-hidden h-28 flex items-end">
        <div className={`w-full rounded-t-full ${color}`} style={{ height: `${Math.max(percent, 4)}%` }} />
      </div>
      <span className="text-2xs uppercase tracking-wider font-extrabold text-slate-500">{title}</span>
    </div>
  );
}
