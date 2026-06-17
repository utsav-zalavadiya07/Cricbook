"use client";

import { useEffect, useState } from "react";
import api from "@/services/api";
import Link from "next/link";

type Experiment = {
  id: number;
  experimentName: string;
  variant: string;
  impressions: number;
  conversions: number;
  conversionRate: number;
};

export default function AdminExperimentsDashboard() {
  const [experiments, setExperiments] = useState<Experiment[]>([]);
  const [loading, setLoading] = useState(true);
  const [recording, setRecording] = useState(false);

  async function fetchExperiments() {
    try {
      setLoading(true);
      const res = await api.get("/admin/ai/experiments");
      setExperiments(res.data);
    } catch (err) {
      console.error("Error fetching experiments metrics", err);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void fetchExperiments(), 0);
    return () => window.clearTimeout(timer);
  }, []);

  const handleRecordMetric = async (name: string, variant: string, type: "IMPRESSION" | "CONVERSION") => {
    try {
      setRecording(true);
      await api.post("/admin/ai/experiments/record", {
        experimentName: name,
        variant,
        metricType: type,
      });
      // Refresh
      const res = await api.get("/admin/ai/experiments");
      setExperiments(res.data);
    } catch (err) {
      console.error("Error recording dynamic test metric", err);
    } finally {
      setRecording(false);
    }
  };

  return (
    <main className="min-h-screen bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-slate-950 via-slate-900 to-black text-white px-4 py-8 sm:px-8">
      <div className="mx-auto max-w-6xl space-y-10">
        
        {/* Navigation Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between border-b border-white/10 pb-6 gap-4">
          <div>
            <h1 className="text-3xl font-extrabold bg-gradient-to-r from-white via-indigo-200 to-emerald-400 bg-clip-text text-transparent">
              🧪 A/B Layout Optimization Board
            </h1>
            <p className="text-sm text-slate-400 mt-1">Track homepage variant CTR conversions and measure vector scoring conversions yield.</p>
          </div>
          <div className="flex gap-3">
            <Link href="/admin/ai" className="inline-flex items-center justify-center rounded-xl bg-white/5 border border-white/10 hover:bg-white/10 px-4 py-2.5 text-xs font-bold transition">
              🛡️ Admin Intelligence Board
            </Link>
            <Link href="/personalized" className="inline-flex items-center justify-center rounded-xl bg-indigo-600 hover:bg-indigo-500 px-4 py-2.5 text-xs font-bold transition">
              Personalized Dashboard
            </Link>
          </div>
        </div>

        {loading ? (
          <div className="text-center py-20 text-slate-400">
            Analyzing search ranking variables...
          </div>
        ) : (
          <div className="space-y-8">
            <div className="rounded-3xl border border-white/10 bg-white/5 p-6 backdrop-blur-xl space-y-5 shadow-lg">
              <h3 className="text-lg font-bold text-white">Variant Split Comparison</h3>
              <p className="text-xs text-slate-400">
                Layout Variant A uses high rating priority sorting. Layout Variant B utilizes neural time-aligned collaborative vector-based recommendation.
              </p>

              <div className="overflow-x-auto">
                {experiments.length === 0 ? (
                  <div className="rounded-2xl border border-dashed border-white/15 p-10 text-center text-slate-500 space-y-4">
                    <p>No active variants compiled. Trigger standard telemetry impressions below to seed dynamic trackers.</p>
                    <div className="flex justify-center gap-3">
                      <button
                        onClick={() => handleRecordMetric("HomepageLayout", "A", "IMPRESSION")}
                        className="rounded-lg bg-indigo-500 hover:bg-indigo-400 px-4 py-2 text-xs font-bold transition"
                      >
                        Seed Variant A Impression
                      </button>
                      <button
                        onClick={() => handleRecordMetric("HomepageLayout", "B", "IMPRESSION")}
                        className="rounded-lg bg-emerald-500 hover:bg-emerald-400 px-4 py-2 text-xs font-bold transition"
                      >
                        Seed Variant B Impression
                      </button>
                    </div>
                  </div>
                ) : (
                  <table className="w-full text-left text-xs border-collapse">
                    <thead>
                      <tr className="border-b border-white/10 text-slate-400 font-semibold">
                        <th className="pb-3">Experiment Name</th>
                        <th className="pb-3">Variant Split</th>
                        <th className="pb-3">Impressions</th>
                        <th className="pb-3">Conversions</th>
                        <th className="pb-3 text-emerald-400">CTR Rate</th>
                        <th className="pb-3 text-right">Interactive Mocks</th>
                      </tr>
                    </thead>
                    <tbody>
                      {experiments.map((exp) => (
                        <tr key={exp.id} className="border-b border-white/5 text-slate-300 hover:bg-white/5 transition">
                          <td className="py-3 font-semibold">{exp.experimentName}</td>
                          <td className="py-3 font-bold font-mono">
                            {exp.variant === "A" ? "🅰️ Variant A (Ratings)" : "🅱️ Variant B (AI Vectors)"}
                          </td>
                          <td className="py-3">{exp.impressions}</td>
                          <td className="py-3">{exp.conversions}</td>
                          <td className="py-3 font-bold text-emerald-400">{exp.conversionRate}%</td>
                          <td className="py-3 text-right space-x-2">
                            <button
                              disabled={recording}
                              onClick={() => handleRecordMetric(exp.experimentName, exp.variant, "IMPRESSION")}
                              className="rounded-md border border-white/10 hover:bg-white/10 px-2.5 py-1 text-[10px] transition"
                            >
                              + Impression
                            </button>
                            <button
                              disabled={recording}
                              onClick={() => handleRecordMetric(exp.experimentName, exp.variant, "CONVERSION")}
                              className="rounded-md bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 hover:bg-emerald-500/35 px-2.5 py-1 text-[10px] transition"
                            >
                              + Conversion
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </div>
          </div>
        )}

      </div>
    </main>
  );
}
