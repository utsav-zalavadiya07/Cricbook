"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { buyResaleListing, getResaleListing, ResaleListing } from "@/modules/resale/services/resaleService";
import { useToast } from "@/modules/ui/ToastProvider";

export default function MarketplaceDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const { toast } = useToast();
  const [listing, setListing] = useState<ResaleListing | null>(null);
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    try {
      const res = await getResaleListing(Number(params.id));
      setListing(res.data);
    } catch (error: unknown) {
      toast(apiMessage(error, "Could not load resale listing."), "error");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.id]);

  const buy = async () => {
    if (!listing) return;
    try {
      await buyResaleListing(listing.id, `WEB-${Date.now()}`);
      toast("Slot transferred to your bookings.", "success");
      router.push("/booking/history");
    } catch (error: unknown) {
      toast(apiMessage(error, "Could not buy this slot."), "error");
    }
  };

  return (
    <main className="theme-page-player min-h-screen px-4 py-8 sm:px-6 lg:px-10">
      <div className="mx-auto max-w-5xl">
        {loading && <p className="theme-muted py-16 text-center">Loading listing...</p>}
        {!loading && !listing && <p className="theme-card theme-muted rounded-3xl p-10 text-center">Listing was not found.</p>}
        {listing && (
          <section className="theme-panel-strong rounded-[2rem] p-6 sm:p-8">
            <p className="theme-accent text-xs font-black uppercase tracking-[0.34em]">Slot Transfer</p>
            <div className="mt-4 grid gap-8 lg:grid-cols-[1fr_0.8fr]">
              <div>
                <h1 className="theme-title text-4xl font-black sm:text-5xl">{listing.turfName}</h1>
                <p className="theme-muted mt-3 text-base">{listing.location}, {listing.city}</p>
                <div className="mt-6 grid gap-3 sm:grid-cols-3">
                  <Metric label="Date" value={listing.bookingDate} />
                  <Metric label="Time" value={`${listing.startTime} - ${listing.endTime}`} />
                  <Metric label="Seller Trust" value={listing.sellerTrustScore} />
                </div>
                <p className="theme-muted mt-6 leading-7">{listing.notes || listing.recommendationReason}</p>
              </div>
              <aside className="theme-card-strong rounded-3xl p-5">
                <p className="theme-soft text-xs font-black uppercase tracking-[0.24em]">Transfer Price</p>
                <p className="theme-title mt-3 text-5xl font-black">₹{listing.price}</p>
                <p className="theme-muted mt-2 text-sm">Original amount ₹{listing.originalAmount}. Platform commission ₹{listing.commissionAmount}.</p>
                <button
                  onClick={buy}
                  disabled={listing.status !== "ACTIVE"}
                  className="mt-6 w-full rounded-2xl bg-emerald-500 px-5 py-3 text-sm font-black text-white disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {listing.status === "ACTIVE" ? "Buy & Transfer Slot" : listing.status}
                </button>
              </aside>
            </div>
          </section>
        )}
      </div>
    </main>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="theme-panel-soft rounded-2xl p-4">
      <p className="theme-soft text-[10px] font-black uppercase tracking-[0.2em]">{label}</p>
      <p className="theme-title mt-2 text-xl font-black">{value}</p>
    </div>
  );
}

function apiMessage(error: unknown, fallback: string) {
  const response = (error as { response?: { data?: { message?: string } } } | null)?.response;
  return response?.data?.message || fallback;
}
