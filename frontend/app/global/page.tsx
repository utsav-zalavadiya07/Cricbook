"use client";
import { TileInfo } from "@/components/ui";

import { useEffect, useState } from "react";
import { createGlobalCity, getGlobalCities } from "@/modules/global/services/globalService";

type CityConfig = {
  id: number;
  city: string;
  country: string;
  currency: string;
  locale: string;
  timeZone: string;
  active: boolean;
};

export default function GlobalPage() {
  const [cities, setCities] = useState<CityConfig[]>([]);
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState("");
  const [form, setForm] = useState({ city: "", country: "India", currency: "INR", locale: "en-IN", timeZone: "Asia/Kolkata", active: true });

  const load = () => {
    setLoading(true);
    getGlobalCities().then((res) => setCities(res.data)).finally(() => setLoading(false));
  };

  useEffect(() => {
    let active = true;
    getGlobalCities()
      .then((res) => {
        if (active) setCities(res.data);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  const submit = (event: React.FormEvent) => {
    event.preventDefault();
    createGlobalCity(form)
      .then(() => {
        setForm({ city: "", country: "India", currency: "INR", locale: "en-IN", timeZone: "Asia/Kolkata", active: true });
        setStatus("City added");
        load();
      })
      .catch((error) => setStatus(errorMessage(error, "City creation failed")));
  };

  return (
    <main className="min-h-screen bg-gradient-to-br from-slate-900 via-emerald-950 to-slate-900 px-4 py-8 sm:px-6">
      <div className="mx-auto max-w-7xl space-y-6">
        <div>
          <h1 className="text-3xl font-bold text-white">Global Settings
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Global Settings information" focusable={false}>Multi-city, currency, locale and timezone configuration.</TileInfo>
              </span></h1>
        </div>

        {status && <p className="rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-emerald-200">{status}</p>}

        <form onSubmit={submit} className="grid gap-3 rounded-2xl border border-white/10 bg-white/5 p-5 md:grid-cols-6">
          <input required value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} placeholder="City" className="rounded-xl border border-white/10 bg-black/20 px-3 py-2 text-sm text-white outline-none" />
          <input required value={form.country} onChange={(e) => setForm({ ...form, country: e.target.value })} placeholder="Country" className="rounded-xl border border-white/10 bg-black/20 px-3 py-2 text-sm text-white outline-none" />
          <input required value={form.currency} onChange={(e) => setForm({ ...form, currency: e.target.value })} placeholder="Currency" className="rounded-xl border border-white/10 bg-black/20 px-3 py-2 text-sm text-white outline-none" />
          <input required value={form.locale} onChange={(e) => setForm({ ...form, locale: e.target.value })} placeholder="Locale" className="rounded-xl border border-white/10 bg-black/20 px-3 py-2 text-sm text-white outline-none" />
          <input required value={form.timeZone} onChange={(e) => setForm({ ...form, timeZone: e.target.value })} placeholder="Timezone" className="rounded-xl border border-white/10 bg-black/20 px-3 py-2 text-sm text-white outline-none" />
          <button className="rounded-xl bg-emerald-500 px-4 py-2 text-sm font-bold text-white">Add</button>
        </form>

        {loading && <p className="py-10 text-center text-slate-400">Loading cities...</p>}
        {!loading && (
          <section className="grid gap-3 md:grid-cols-3">
            {cities.length === 0 && <Empty text="No cities configured." />}
            {cities.map((city) => (
              <div key={city.id} className="rounded-2xl border border-white/10 bg-white/5 p-5">
                <p className="font-semibold text-white">{city.city}, {city.country}</p>
                <p className="mt-1 text-sm text-slate-400">{city.currency} - {city.locale} - {city.timeZone}</p>
              </div>
            ))}
          </section>
        )}
      </div>
    </main>
  );
}

function Empty({ text }: { text: string }) {
  return <p className="rounded-xl border border-dashed border-white/10 p-4 text-center text-sm text-slate-400">{text}</p>;
}

function errorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { error?: string } } };
  return apiError.response?.data?.error || fallback;
}
