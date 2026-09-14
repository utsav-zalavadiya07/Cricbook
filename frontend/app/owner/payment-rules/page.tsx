"use client";
import { TileInfo } from "@/components/ui";

import { useEffect, useState } from "react";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import { getPaymentAnalytics, getOwnerPaymentRules, updateOwnerPaymentRules, PaymentRule, SplitAnalytics } from "@/modules/payments/services/splitPaymentService";

export default function OwnerPaymentRulesPage() {
  const [rules, setRules] = useState<Partial<PaymentRule>>({ minimumPercentage: 50, autoCancel: false, fullPaymentRequired: false, reminderHoursBefore: 24 });
  const [analytics, setAnalytics] = useState<SplitAnalytics | null>(null);
  const [status, setStatus] = useState("");

  const load = () => {
    Promise.all([getOwnerPaymentRules(), getPaymentAnalytics()])
      .then(([ruleRes, analyticsRes]) => {
        setRules(ruleRes.data);
        setAnalytics(analyticsRes.data);
      })
      .catch((error) => setStatus(errorMessage(error, "Unable to load payment rules.")));
  };

  useEffect(() => {
    load();
  }, []);

  const save = () => {
    updateOwnerPaymentRules(rules)
      .then((res) => {
        setRules(res.data);
        setStatus("Payment rules saved.");
      })
      .catch((error) => setStatus(errorMessage(error, "Payment rules could not be saved.")));
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <section className="theme-panel rounded-[2rem] p-6 sm:p-8">
          <p className="theme-soft text-xs font-bold uppercase tracking-[0.28em]">EPIC 28</p>
          <div className="mt-3">
            <h1 className="theme-title text-3xl font-black sm:text-5xl">Payment Rules
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Payment Rules information" focusable={false}>Protect owner revenue with minimum collection thresholds, smart reminders, and split-payment visibility.</TileInfo>
              </span></h1>
          </div>
        </section>
        {status && <div className="theme-panel-soft rounded-2xl p-4 text-sm font-semibold text-sky-600 dark:text-sky-300">{status}</div>}
        <div className="grid gap-6 xl:grid-cols-[0.85fr_1.15fr]">
          <section className="theme-panel rounded-3xl p-6">
            <h2 className="theme-title text-xl font-black">Partial Payment Protection</h2>
            <div className="mt-5 grid gap-4">
              <Field label="Minimum upfront %" value={String(rules.minimumPercentage ?? 50)} onChange={(value) => setRules((current) => ({ ...current, minimumPercentage: Number(value) }))} />
              <Field label="Reminder hours before" value={String(rules.reminderHoursBefore ?? 24)} onChange={(value) => setRules((current) => ({ ...current, reminderHoursBefore: Number(value) }))} />
              <Toggle label="Auto-cancel unpaid bookings" active={Boolean(rules.autoCancel)} onClick={() => setRules((current) => ({ ...current, autoCancel: !current.autoCancel }))} />
              <Toggle label="Full payment required" active={Boolean(rules.fullPaymentRequired)} onClick={() => setRules((current) => ({ ...current, fullPaymentRequired: !current.fullPaymentRequired }))} />
              <button onClick={save} className="rounded-2xl bg-sky-500 px-4 py-3 text-sm font-bold text-white hover:bg-sky-400">Save Rules</button>
            </div>
          </section>
          <section className="theme-panel rounded-3xl p-6">
            <h2 className="theme-title text-xl font-black">Collection Analytics</h2>
            <div className="mt-5 grid gap-4 sm:grid-cols-2">
              <Metric label="Total Splits" value={analytics?.totalSplits ?? 0} />
              <Metric label="Paid" value={analytics?.paidSplits ?? 0} />
              <Metric label="Pending Rs" value={analytics?.totalPending ?? 0} />
              <Metric label="Success" value={`${analytics?.collectionSuccessRate ?? 0}%`} />
            </div>
            <div className="theme-panel-soft mt-5 rounded-2xl p-4">
              <p className="theme-title font-bold">Insight</p>
              <p className="theme-muted mt-2 text-sm">{analytics?.insight || "No split payment activity yet."}</p>
            </div>
          </section>
        </div>
      </div>
    </DashboardLayout>
  );
}

function Field({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <label className="grid gap-2 text-sm font-semibold">
      <span className="theme-muted">{label}</span>
      <input value={value} onChange={(e) => onChange(e.target.value)} className="theme-input rounded-2xl px-4 py-3" inputMode="numeric" />
    </label>
  );
}

function Toggle({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return <button onClick={onClick} className={`theme-card rounded-2xl px-4 py-3 text-left text-sm font-bold ${active ? "text-sky-600 dark:text-sky-300" : "theme-muted"}`}>{active ? "Enabled" : "Disabled"} - {label}</button>;
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return <div className="theme-panel-soft rounded-2xl p-4"><p className="theme-soft text-xs uppercase tracking-[0.2em]">{label}</p><p className="theme-title mt-2 text-2xl font-black">{value}</p></div>;
}

function errorMessage(error: unknown, fallback: string) {
  if (typeof error === "object" && error !== null && "response" in error) {
    return (error as { response?: { data?: { message?: string } } }).response?.data?.message || fallback;
  }
  return fallback;
}
