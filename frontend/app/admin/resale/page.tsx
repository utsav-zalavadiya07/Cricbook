"use client";

import { useEffect, useState } from "react";
import { getAdminResaleAnalytics, ResaleAnalytics } from "@/modules/resale/services/resaleService";
import { useToast } from "@/modules/ui/ToastProvider";

export default function AdminResalePage() {
  const { toast } = useToast();
  const [analytics, setAnalytics] = useState<ResaleAnalytics | null>(null);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      getAdminResaleAnalytics()
        .then((res) => setAnalytics(res.data))
        .catch((error: unknown) => toast(apiMessage(error, "Could not load marketplace monitoring."), "error"));
    }, 0);
    return () => window.clearTimeout(timer);
  }, [toast]);

  return (
    <main className="theme-page-admin min-h-screen px-4 py-8 sm:px-6 lg:px-10">
      <div className="mx-auto max-w-7xl space-y-6">
        <section className="theme-panel-strong rounded-[2rem] p-6">
          <p className="theme-accent-owner text-xs font-black uppercase tracking-[0.34em]">Marketplace Monitoring</p>
          <h1 className="theme-title mt-3 text-3xl font-black sm:text-5xl">Slot resale health and abuse visibility.</h1>
          <p className="theme-muted mt-3 max-w-3xl text-sm leading-6">Track marketplace GMV, transfer success, waitlist demand, and suspicious resale concentration.</p>
        </section>
        <section className="grid gap-4 sm:grid-cols-5">
          <Metric label="Active" value={analytics?.activeListings ?? 0} />
          <Metric label="Sold" value={analytics?.soldListings ?? 0} />
          <Metric label="Expired" value={analytics?.expiredListings ?? 0} />
          <Metric label="GMV" value={`₹${analytics?.marketplaceGmv ?? 0}`} />
          <Metric label="Waitlist" value={analytics?.waitlistUsers ?? 0} />
        </section>
        <section className="theme-panel rounded-3xl p-6">
          <h2 className="theme-title text-xl font-black">Recent Marketplace Listings</h2>
          <div className="mt-5 grid gap-4 lg:grid-cols-3">
            {(analytics?.recentListings || []).map((listing) => (
              <article key={listing.id} className="theme-panel-soft rounded-2xl p-4">
                <p className="theme-title font-black">{listing.turfName}</p>
                <p className="theme-muted mt-1 text-sm">{listing.city} · {listing.bookingDate} · ₹{listing.price}</p>
                <p className="theme-muted mt-3 text-xs">{listing.status} · Seller {listing.sellerName} · Trust {listing.sellerTrustScore}</p>
              </article>
            ))}
            {(analytics?.recentListings || []).length === 0 && <p className="theme-card theme-muted col-span-full rounded-2xl border-dashed p-8 text-center">No resale listings yet.</p>}
          </div>
        </section>
      </div>
    </main>
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

function apiMessage(error: unknown, fallback: string) {
  const response = (error as { response?: { data?: { message?: string } } } | null)?.response;
  return response?.data?.message || fallback;
}
