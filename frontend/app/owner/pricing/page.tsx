"use client";
import { TileInfo } from "@/components/ui";

import { useCallback, useEffect, useState } from "react";
import api from "@/services/api";
import Link from "next/link";
import { getOwnerTurfs } from "@/modules/owner/services/ownerService";
import { Turf } from "@/modules/turf/types/turf.types";

type PricingRule = {
  id: number;
  turfId: number;
  ruleName: string;
  adjustmentPercent: number;
  isActive: boolean;
};

type PricingHistory = {
  id: number;
  turfId: number;
  slotId: number;
  originalPrice: number;
  dynamicPrice: number;
  reason: string;
  createdAt: string;
};

const ruleLabel = (ruleName: string) => (ruleName === "PEAK_HOUR" ? "Peak Hours" : "Weekend Slots");

export default function OwnerPricingRulesDashboard() {
  const [turfs, setTurfs] = useState<Turf[]>([]);
  const [turfId, setTurfId] = useState<number | null>(null);
  const [rules, setRules] = useState<PricingRule[]>([]);
  const [history, setHistory] = useState<PricingHistory[]>([]);
  const [draftPercents, setDraftPercents] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(true);
  const [toggling, setToggling] = useState<string | null>(null);
  const [error, setError] = useState("");

  const fetchData = useCallback(async () => {
    if (!turfId) {
      setRules([]);
      setHistory([]);
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError("");
      const [rulesRes, historyRes] = await Promise.all([
        api.get(`/owner/pricing/rules?turfId=${turfId}`),
        api.get(`/owner/pricing/history?turfId=${turfId}`),
      ]);
      const nextRules: PricingRule[] = rulesRes.data || [];
      setRules(nextRules);
      setHistory(historyRes.data || []);
      setDraftPercents(Object.fromEntries(nextRules.map((rule) => [rule.ruleName, rule.adjustmentPercent])));
    } catch (err) {
      console.error("Error loading pricing rules data", err);
      setError("Could not load pricing rules for this turf.");
    } finally {
      setLoading(false);
    }
  }, [turfId]);

  useEffect(() => {
    getOwnerTurfs()
      .then((res) => {
        const nextTurfs: Turf[] = res.data || [];
        setTurfs(nextTurfs);
        setTurfId((current) => current ?? nextTurfs.find((turf) => turf.id)?.id ?? null);
      })
      .catch(() => setError("Could not load your turfs."))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!turfId) return;
    const timer = window.setTimeout(() => void fetchData(), 0);
    return () => window.clearTimeout(timer);
  }, [fetchData, turfId]);

  const handleToggleRule = async (ruleName: string, currentlyActive: boolean) => {
    if (!turfId) return;
    try {
      setToggling(ruleName);
      setError("");
      await api.post("/owner/pricing/toggle", {
        turfId,
        ruleName,
        active: !currentlyActive,
        adjustmentPercent: draftPercents[ruleName],
      });
      await fetchData();
    } catch (err) {
      console.error("Error toggling rule", err);
      setError("Could not update pricing rule.");
    } finally {
      setToggling(null);
    }
  };

  return (
    <main className="min-h-screen bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-slate-950 via-slate-900 to-black px-4 py-8 text-white sm:px-8">
      <div className="mx-auto max-w-6xl space-y-8">
        <div className="flex flex-col gap-4 border-b border-white/10 pb-6 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <div>
            <h1 className="bg-gradient-to-r from-white via-indigo-200 to-emerald-400 bg-clip-text text-3xl font-extrabold text-transparent">Dynamic Pricing & Tariff Optimization
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Pricing information" focusable={false}>
                Manage protected peak-hour and weekend pricing rules for your real turfs.
              </TileInfo>
              </span></h1>
          </div>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row">
            <select
              value={turfId ?? ""}
              onChange={(event) => setTurfId(Number(event.target.value))}
              className="rounded-xl border border-white/15 bg-white/5 px-4 py-2.5 text-sm outline-none focus:border-indigo-500"
              disabled={turfs.length === 0}
            >
              {turfs.map((turf) => (
                <option key={turf.id} value={turf.id} className="bg-slate-950">
                  {turf.name} ({turf.city})
                </option>
              ))}
            </select>
            <Link
              href="/owner/slots"
              className="inline-flex items-center justify-center rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-xs font-bold transition hover:bg-white/10"
            >
              Manage Slots
            </Link>
          </div>
        </div>

        {error && (
          <div className="rounded-2xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-200">
            {error}
          </div>
        )}

        {loading ? (
          <div className="flex items-center justify-center py-20 text-slate-400">
            Loading pricing rules...
          </div>
        ) : turfs.length === 0 ? (
          <div className="rounded-3xl border border-dashed border-white/15 bg-white/5 p-10 text-center text-sm text-slate-400">
            Create a turf before configuring dynamic pricing rules.
          </div>
        ) : (
          <div className="grid gap-8 lg:grid-cols-3">
            <div className="space-y-6 lg:col-span-1">
              <div className="space-y-5 rounded-3xl border border-white/10 bg-white/5 p-6 shadow-lg backdrop-blur-xl">
                <h3 className="text-lg font-bold text-white">Rule Parameters</h3>
                <p className="text-xs leading-relaxed text-slate-400">
                  Use percentage adjustments carefully. High pricing increases can reduce booking conversion.
                </p>

                <div className="space-y-4 pt-3">
                  {rules.map((rule) => (
                    <div key={rule.ruleName} className="space-y-3 rounded-2xl border border-white/5 bg-black/30 p-4">
                      <div className="flex items-center justify-between">
                        <span className="text-sm font-bold text-white">{ruleLabel(rule.ruleName)}</span>
                        <button
                          disabled={toggling === rule.ruleName}
                          onClick={() => handleToggleRule(rule.ruleName, rule.isActive)}
                          className={`rounded-full border px-3 py-1 text-xs font-bold transition disabled:opacity-50 ${
                            rule.isActive
                              ? "border-emerald-500/30 bg-emerald-500/20 text-emerald-300"
                              : "border-red-500/20 bg-red-500/10 text-red-400"
                          }`}
                        >
                          {toggling === rule.ruleName ? "Applying..." : rule.isActive ? "ACTIVE" : "INACTIVE"}
                        </button>
                      </div>

                      <label className="block">
                        <span className="mb-1 block text-[11px] font-semibold uppercase tracking-wide text-slate-500">
                          Adjustment %
                        </span>
                        <input
                          type="number"
                          min={-90}
                          max={200}
                          value={draftPercents[rule.ruleName] ?? rule.adjustmentPercent}
                          onChange={(event) =>
                            setDraftPercents((current) => ({ ...current, [rule.ruleName]: Number(event.target.value) }))
                          }
                          className="w-full rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white outline-none focus:border-indigo-400"
                        />
                      </label>

                      <p className="text-[11px] text-slate-400">
                        {rule.ruleName === "PEAK_HOUR"
                          ? "Applies during 6:00 PM - 10:00 PM daily."
                          : "Applies on Saturdays and Sundays."}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            <div className="space-y-6 lg:col-span-2">
              <div className="space-y-4 rounded-3xl border border-white/10 bg-white/5 p-6 shadow-lg backdrop-blur-xl">
                <h3 className="text-lg font-bold text-white">Real-time Price Adjustments Log</h3>
                <p className="text-xs text-slate-400">
                  Logs generated when dynamic pricing changes a player-visible slot price.
                </p>

                <div className="overflow-x-auto">
                  {history.length === 0 ? (
                    <div className="rounded-2xl border border-dashed border-white/15 p-10 text-center text-sm text-slate-500">
                      No adjustments logged yet.
                    </div>
                  ) : (
                    <table className="w-full border-collapse text-left text-xs">
                      <thead>
                        <tr className="border-b border-white/10 font-semibold text-slate-400">
                          <th className="pb-3">Slot ID</th>
                          <th className="pb-3">Original Price</th>
                          <th className="pb-3 text-emerald-400">Adjusted Price</th>
                          <th className="pb-3">Reason</th>
                          <th className="pb-3">Logged Date</th>
                        </tr>
                      </thead>
                      <tbody>
                        {history.map((logItem) => (
                          <tr key={logItem.id} className="border-b border-white/5 text-slate-300 transition hover:bg-white/5">
                            <td className="py-3 font-mono font-medium">#{logItem.slotId}</td>
                            <td className="py-3 text-slate-500 line-through">Rs {logItem.originalPrice}</td>
                            <td className="py-3 font-bold text-emerald-400">Rs {logItem.dynamicPrice}</td>
                            <td className="py-3">{logItem.reason}</td>
                            <td className="py-3 text-slate-400">{new Date(logItem.createdAt).toLocaleString()}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </main>
  );
}
