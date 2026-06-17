"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { getSmartRecommendations } from "@/modules/smart/services/smartService";
import { Turf } from "@/modules/turf/types/turf.types";

type SmartData = {
  bestTurfs: Turf[];
  bestTimings: string[];
  playerSuggestions: Array<{ id: number; name: string; email: string }>;
  suggestedPrice: number;
  pricingReason: string;
};

export default function SmartPage() {
  const [data, setData] = useState<SmartData | null>(null);
  const [city, setCity] = useState("");
  const [turfId, setTurfId] = useState("");
  const [loading, setLoading] = useState(true);

  const load = () => {
    setLoading(true);
    getSmartRecommendations({ city: city || undefined, turfId: turfId ? Number(turfId) : undefined })
      .then((res) => setData(res.data))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    let active = true;
    getSmartRecommendations()
      .then((res) => {
        if (active) setData(res.data);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <main className="min-h-screen bg-gradient-to-br from-slate-900 via-emerald-950 to-slate-900 px-4 py-8 sm:px-6">
      <div className="mx-auto max-w-7xl space-y-6">
        <div>
          <h1 className="text-3xl font-bold text-white">Smart Platform</h1>
          <p className="mt-1 text-sm text-slate-400">Recommendations, player suggestions and demand-based pricing.</p>
        </div>

        <div className="grid gap-3 rounded-2xl border border-white/10 bg-white/5 p-5 md:grid-cols-3">
          <input value={city} onChange={(e) => setCity(e.target.value)} placeholder="City" className="rounded-xl border border-white/10 bg-black/20 px-3 py-2 text-sm text-white outline-none" />
          <input value={turfId} onChange={(e) => setTurfId(e.target.value)} placeholder="Turf id for pricing" className="rounded-xl border border-white/10 bg-black/20 px-3 py-2 text-sm text-white outline-none" />
          <button onClick={load} className="rounded-xl bg-emerald-500 px-4 py-2 text-sm font-bold text-white">Refresh</button>
        </div>

        {loading && <p className="py-10 text-center text-slate-400">Loading smart insights...</p>}

        {!loading && data && (
          <div className="grid gap-6 xl:grid-cols-3">
            <section className="rounded-2xl border border-white/10 bg-white/5 p-5 xl:col-span-2">
              <h2 className="text-lg font-bold text-white">Best Turfs</h2>
              <div className="mt-4 grid gap-3 md:grid-cols-2">
                {data.bestTurfs.map((turf) => (
                  <Link key={turf.id} href={`/turf/${turf.id}`} className="rounded-xl border border-white/10 bg-black/20 p-4 hover:border-emerald-500/40">
                    <p className="font-semibold text-white">{turf.name}</p>
                    <p className="mt-1 text-sm text-slate-400">{turf.city} - Rs {turf.pricePerHour}/hr</p>
                  </Link>
                ))}
              </div>
            </section>

            <section className="space-y-6">
              <div className="rounded-2xl border border-white/10 bg-white/5 p-5">
                <h2 className="text-lg font-bold text-white">Pricing</h2>
                <p className="mt-3 text-3xl font-black text-white">Rs {data.suggestedPrice}</p>
                <p className="mt-2 text-sm text-slate-400">{data.pricingReason}</p>
              </div>
              <div className="rounded-2xl border border-white/10 bg-white/5 p-5">
                <h2 className="text-lg font-bold text-white">Best Timings</h2>
                <div className="mt-3 flex flex-wrap gap-2">
                  {data.bestTimings.map((time) => <span key={time} className="rounded-full border border-white/10 px-3 py-1 text-sm text-slate-200">{time}</span>)}
                </div>
              </div>
            </section>
          </div>
        )}
      </div>
    </main>
  );
}
