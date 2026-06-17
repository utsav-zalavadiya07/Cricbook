"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { getSegments, createSegment, deleteSegment } from "@/modules/admin/services/adminService";

type AdminSegment = {
  id: number;
  name: string;
  ruleType: string;
  ruleValue: string;
};

export default function AdminSegmentsPage() {
  const [segments, setSegments] = useState<AdminSegment[]>([]);
  const [name, setName] = useState("");
  const [ruleType, setRuleType] = useState("BOOKINGS_COUNT");
  const [ruleValue, setRuleValue] = useState("5");

  const [loading, setLoading] = useState(true);
  const [statusMsg, setStatusMsg] = useState("");

  async function loadSegments() {
    setLoading(true);
    try {
      const res = await getSegments();
      setSegments(res.data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void loadSegments(), 0);
    return () => window.clearTimeout(timer);
  }, []);

  const handleCreateSegment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !ruleValue) {
      setStatusMsg("Validation Error: Please fill all fields.");
      return;
    }

    try {
      setStatusMsg("Saving new segment filter...");
      const res = await createSegment({ name, ruleType, ruleValue });
      setSegments((prev) => [...prev, res.data]);
      setName("");
      setStatusMsg("Audience segment rule successfully configured!");
      setTimeout(() => setStatusMsg(""), 3000);
    } catch {
      setStatusMsg("Failed saving segment filter.");
    }
  };

  const handleDeleteSegment = async (id: number) => {
    try {
      await deleteSegment(id);
      setSegments((prev) => prev.filter((s) => s.id !== id));
      setStatusMsg("Audience segment deleted.");
      setTimeout(() => setStatusMsg(""), 3000);
    } catch {
      setStatusMsg("Failed removing segment filter.");
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex font-sans">
      
      {/* Sidebar Navigation */}
      <aside className="w-64 bg-slate-900 border-r border-white/5 flex flex-col justify-between p-6 shrink-0">
        <div className="space-y-8">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className="text-xl">🛡️</span>
              <span className="font-bold text-white text-lg tracking-tight">Cricbook Admin</span>
            </div>
            <div className="px-2.5 py-0.5 bg-emerald-500/10 rounded-md border border-emerald-500/20 inline-block text-[10px] font-bold text-emerald-400 uppercase tracking-widest">
              Notification Console
            </div>
          </div>

          <nav className="space-y-1">
            <Link href="/admin" className="w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-xs font-semibold text-slate-400 hover:bg-white/5 hover:text-white transition-all">
              <span>📊</span> Dashboard Ops
            </Link>
            <Link href="/admin/campaigns" className="w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-xs font-semibold text-slate-400 hover:bg-white/5 hover:text-white transition-all">
              <span>📢</span> Campaign Manager
            </Link>
            <Link href="/admin/templates" className="w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-xs font-semibold text-slate-400 hover:bg-white/5 hover:text-white transition-all">
              <span>✉️</span> Template Builder
            </Link>
            <Link href="/admin/segments" className="w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 transition-all">
              <span>👥</span> Audience Segments
            </Link>
          </nav>
        </div>

        <div className="pt-6 border-t border-white/5">
          <Link href="/inbox" className="text-xs text-sky-400 hover:text-sky-300 font-medium">← Go to User Inbox</Link>
        </div>
      </aside>

      {/* Main segments workspace */}
      <main className="flex-1 p-8 space-y-6 overflow-y-auto">
        <div className="flex justify-between items-center border-b border-white/5 pb-4">
          <div>
            <h1 className="text-2xl font-bold text-white tracking-tight">Audience Segments</h1>
            <p className="text-slate-400 text-xs mt-1">Configure real-time filter rules to group your target users dynamically.</p>
          </div>
        </div>

        {statusMsg && (
          <div className="p-3 bg-emerald-500/10 border border-emerald-500/25 rounded-xl text-emerald-400 text-xs animate-pulse">
            {statusMsg}
          </div>
        )}

        <div className="grid grid-cols-1 xl:grid-cols-3 gap-8">
          
          {/* Col 1 & 2: Segment Creator form */}
          <div className="xl:col-span-2 space-y-6">
            <div className="rounded-2xl border border-white/5 bg-slate-900/60 p-6 space-y-4">
              <h3 className="font-bold text-white text-sm">Add New Segment Rule</h3>
              <form onSubmit={handleCreateSegment} className="space-y-4">
                
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1">Segment tag name</label>
                    <input
                      type="text"
                      placeholder="e.g. Ahmedabad Active Players"
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      className="w-full bg-slate-950 text-xs border border-white/10 rounded-xl px-3 py-2.5 text-slate-300 outline-none focus:border-white/20"
                    />
                  </div>

                  <div>
                    <label className="block text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1">Rule filter Type</label>
                    <select
                      value={ruleType}
                      onChange={(e) => setRuleType(e.target.value)}
                      className="w-full bg-slate-950 text-xs border border-white/10 rounded-xl px-3 py-2.5 text-slate-300 outline-none cursor-pointer"
                    >
                      <option value="BOOKINGS_COUNT">Total Bookings Count</option>
                      <option value="CITY">Player Profile Location (City)</option>
                      <option value="INACTIVITY">Days Inactive</option>
                    </select>
                  </div>
                </div>

                <div>
                  <label className="block text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1">Rule Threshold Value</label>
                  <input
                    type="text"
                    placeholder="e.g. '5' for bookings, 'Ahmedabad' for city, or '30' for inactive days"
                    value={ruleValue}
                    onChange={(e) => setRuleValue(e.target.value)}
                    className="w-full bg-slate-950 text-xs border border-white/10 rounded-xl px-3 py-2.5 text-slate-300 outline-none focus:border-white/20"
                  />
                </div>

                <button
                  type="submit"
                  className="w-full py-3 bg-emerald-500 hover:bg-emerald-600 text-white rounded-xl text-xs font-bold transition-all shadow-lg shadow-emerald-500/10 uppercase tracking-wider"
                >
                  Configure Audience Segment
                </button>
              </form>
            </div>

            {/* Existing Segments List */}
            <div className="rounded-2xl border border-white/5 bg-slate-900/60 p-6 space-y-4">
              <h3 className="font-bold text-white text-sm">Active Rules List</h3>

              {loading ? (
                <div className="text-xs text-slate-400 animate-pulse">Syncing rules...</div>
              ) : segments.length === 0 ? (
                <div className="text-xs text-slate-500 py-6">No custom segment rules saved. Create one above.</div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {segments.map((s) => (
                    <div key={s.id} className="p-4 bg-slate-950 border border-white/5 rounded-xl flex flex-col justify-between gap-4">
                      <div className="space-y-1">
                        <div className="flex justify-between items-center mb-1">
                          <span className="font-bold text-white text-xs">{s.name}</span>
                          <span className="px-1.5 py-0.2 bg-emerald-500/15 text-[9px] text-emerald-400 rounded font-semibold uppercase">
                            Custom Segment
                          </span>
                        </div>
                        <div className="p-2.5 bg-black/10 rounded-lg text-[11px] text-slate-400 font-mono">
                          <span className="text-slate-500 font-bold block uppercase tracking-wider text-[8px] mb-1">Evaluated Rule</span>
                          WHERE user.{s.ruleType.toLowerCase()} == {s.ruleValue}
                        </div>
                      </div>
                      <button
                        onClick={() => handleDeleteSegment(s.id)}
                        className="w-full py-1.5 bg-red-500/5 hover:bg-red-500/10 text-red-400 border border-red-500/10 text-[10px] font-bold rounded-lg transition-all"
                      >
                        Delete Segment
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Col 3: Default segments help */}
          <div>
            <div className="rounded-2xl border border-white/5 bg-slate-900/60 p-6 space-y-4 sticky top-24">
              <h3 className="font-bold text-white text-base">Built-in Audiences</h3>
              <p className="text-slate-400 text-xs leading-relaxed">
                Cricbook automatically evaluates these default segment tags without requiring custom DB rules:
              </p>
              
              <div className="space-y-3 pt-2 text-[11px] text-slate-400">
                <div className="p-3 bg-black/20 rounded-xl border border-white/5">
                  <span className="font-bold text-white block">New Users</span>
                  <p className="text-[10px] text-slate-500 mt-0.5">Players who registered within the last 7 days.</p>
                </div>
                <div className="p-3 bg-black/20 rounded-xl border border-white/5">
                  <span className="font-bold text-white block">Premium Players</span>
                  <p className="text-[10px] text-slate-500 mt-0.5">Subscribers with active membership plans.</p>
                </div>
                <div className="p-3 bg-black/20 rounded-xl border border-white/5">
                  <span className="font-bold text-white block">Frequent Players</span>
                  <p className="text-[10px] text-slate-500 mt-0.5">Loyal users with 5 or more total slot bookings.</p>
                </div>
                <div className="p-3 bg-black/20 rounded-xl border border-white/5">
                  <span className="font-bold text-white block">Inactive Users</span>
                  <p className="text-[10px] text-slate-500 mt-0.5">Users who have not completed slots in the last 30 days.</p>
                </div>
              </div>
            </div>
          </div>

        </div>
      </main>
    </div>
  );
}
