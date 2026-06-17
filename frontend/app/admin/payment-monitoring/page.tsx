"use client";

import { useEffect, useState } from "react";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import { getAdminPaymentMonitoring, SplitAnalytics } from "@/modules/payments/services/splitPaymentService";

export default function AdminPaymentMonitoringPage() {
  const [analytics, setAnalytics] = useState<SplitAnalytics | null>(null);

  useEffect(() => {
    getAdminPaymentMonitoring().then((res) => setAnalytics(res.data)).catch(() => setAnalytics(null));
  }, []);

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <section className="theme-panel rounded-[2rem] p-6 sm:p-8">
          <p className="theme-soft text-xs font-bold uppercase tracking-[0.28em]">Admin finance</p>
          <h1 className="theme-title mt-3 text-3xl font-black sm:text-5xl">Payment Monitoring</h1>
          <p className="theme-muted mt-3 max-w-3xl text-sm leading-6">Track split payment adoption, pending liabilities, collection success, and settlement pressure across the platform.</p>
        </section>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <Metric label="Total Splits" value={analytics?.totalSplits ?? 0} />
          <Metric label="Paid Shares" value={analytics?.paidSplits ?? 0} />
          <Metric label="Pending Shares" value={analytics?.pendingSplits ?? 0} />
          <Metric label="Success Rate" value={`${analytics?.collectionSuccessRate ?? 0}%`} />
        </div>
        <section className="theme-panel rounded-3xl p-6">
          <h2 className="theme-title text-xl font-black">Financial Insight</h2>
          <p className="theme-muted mt-2 text-sm">{analytics?.insight || "No split-payment activity yet."}</p>
          <div className="mt-5 grid gap-4 sm:grid-cols-2">
            <Metric label="Collected" value={`Rs ${analytics?.totalCollected ?? 0}`} />
            <Metric label="Pending" value={`Rs ${analytics?.totalPending ?? 0}`} />
          </div>
        </section>
      </div>
    </DashboardLayout>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return <div className="theme-panel rounded-3xl p-5"><p className="theme-soft text-xs font-bold uppercase tracking-[0.22em]">{label}</p><p className="theme-title mt-3 break-words text-3xl font-black">{value}</p></div>;
}
