"use client";
import { TileInfo } from "@/components/ui";

import { FormEvent, useEffect, useState } from "react";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import {
  getOwnerResaleAnalytics,
  getOwnerResaleRules,
  ResaleAnalytics,
  ResaleRule,
  updateOwnerResaleRules,
} from "@/modules/resale/services/resaleService";
import { useToast } from "@/modules/ui/ToastProvider";

export default function OwnerResalePage() {
  const { toast } = useToast();
  const [analytics, setAnalytics] = useState<ResaleAnalytics | null>(null);
  const [rules, setRules] = useState<Partial<ResaleRule>>({
    allowResale: true,
    autoApproveTransfer: true,
    restrictLowTrustUsers: true,
    maxMarkupPercent: 0,
    minHoursBeforeStart: 2,
  });

  async function load() {
    try {
      const [analyticsRes, rulesRes] = await Promise.all([getOwnerResaleAnalytics(), getOwnerResaleRules()]);
      setAnalytics(analyticsRes.data);
      setRules(rulesRes.data);
    } catch (error: unknown) {
      toast(apiMessage(error, "Could not load resale controls."), "error");
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const save = async (event: FormEvent) => {
    event.preventDefault();
    try {
      const res = await updateOwnerResaleRules(rules);
      setRules(res.data);
      toast("Resale rules saved.", "success");
      await load();
    } catch (error: unknown) {
      toast(apiMessage(error, "Could not save resale rules."), "error");
    }
  };

  return (
    <DashboardLayout>
      <main className="space-y-6">
        <section className="theme-panel-strong rounded-[2rem] p-6">
          <p className="theme-accent-owner text-xs font-black uppercase tracking-[0.34em]">Resale Rules</p>
          <h1 className="theme-title mt-3 text-3xl font-black sm:text-5xl">Recover cancelled slots without losing control.
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Recover cancelled slots without losing control. information" focusable={false}>Set resale permissions, trust restrictions, transfer windows, and track recovered booking revenue.</TileInfo>
              </span></h1>
        </section>

        <section className="grid gap-4 sm:grid-cols-4">
          <Metric label="Active Listings" value={analytics?.activeListings ?? 0} />
          <Metric label="Sold Slots" value={analytics?.soldListings ?? 0} />
          <Metric label="Recovered Revenue" value={`₹${analytics?.recoveredRevenue ?? 0}`} />
          <Metric label="Success Rate" value={`${analytics?.successRate ?? 0}%`} />
        </section>

        <section className="grid gap-6 xl:grid-cols-[0.8fr_1.2fr]">
          <form onSubmit={save} className="theme-panel rounded-3xl p-6">
            <h2 className="theme-title text-xl font-black">Owner Transfer Policy</h2>
            <div className="mt-5 space-y-4">
              <Toggle label="Allow resale marketplace" checked={!!rules.allowResale} onChange={(value) => setRules((current) => ({ ...current, allowResale: value }))} />
              <Toggle label="Auto approve transfers" checked={!!rules.autoApproveTransfer} onChange={(value) => setRules((current) => ({ ...current, autoApproveTransfer: value }))} />
              <Toggle label="Restrict low-trust buyers" checked={!!rules.restrictLowTrustUsers} onChange={(value) => setRules((current) => ({ ...current, restrictLowTrustUsers: value }))} />
              <Field label="Max Markup %" value={String(rules.maxMarkupPercent ?? 0)} onChange={(value) => setRules((current) => ({ ...current, maxMarkupPercent: Number(value) }))} />
              <Field label="Minimum Hours Before Start" value={String(rules.minHoursBeforeStart ?? 2)} onChange={(value) => setRules((current) => ({ ...current, minHoursBeforeStart: Number(value) }))} />
            </div>
            <button className="mt-5 w-full rounded-2xl bg-sky-500 px-5 py-3 text-sm font-black text-white">Save Rules</button>
          </form>

          <section className="theme-panel rounded-3xl p-6">
            <h2 className="theme-title text-xl font-black">Marketplace Insights</h2>
            <div className="mt-5 grid gap-4 lg:grid-cols-2">
              {(analytics?.recentListings || []).map((listing) => (
                <article key={listing.id} className="theme-panel-soft rounded-2xl p-4">
                  <p className="theme-title font-black">{listing.turfName}</p>
                  <p className="theme-muted mt-1 text-sm">{listing.bookingDate} · {listing.startTime} · ₹{listing.price}</p>
                  <p className="theme-muted mt-3 text-xs">{listing.status} · Seller trust {listing.sellerTrustScore}</p>
                </article>
              ))}
              {(analytics?.recentListings || []).length === 0 && <p className="theme-card theme-muted col-span-full rounded-2xl border-dashed p-8 text-center">No marketplace activity yet.</p>}
            </div>
          </section>
        </section>
      </main>
    </DashboardLayout>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="theme-panel rounded-2xl p-5">
      <p className="theme-soft text-xs font-black uppercase tracking-[0.2em]">{label}</p>
      <p className="theme-title mt-3 text-3xl font-black">{value}</p>
    </div>
  );
}

function Toggle({ label, checked, onChange }: { label: string; checked: boolean; onChange: (value: boolean) => void }) {
  return (
    <label className="theme-card flex items-center justify-between rounded-2xl p-4">
      <span className="theme-title text-sm font-black">{label}</span>
      <input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />
    </label>
  );
}

function Field({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <label>
      <span className="theme-title text-xs font-black uppercase tracking-wider">{label}</span>
      <input type="number" value={value} onChange={(event) => onChange(event.target.value)} className="theme-input mt-2 w-full rounded-xl px-3 py-2 text-sm outline-none" />
    </label>
  );
}

function apiMessage(error: unknown, fallback: string) {
  const response = (error as { response?: { data?: { message?: string } } } | null)?.response;
  return response?.data?.message || fallback;
}
