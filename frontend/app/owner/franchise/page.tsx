"use client";
import { TileInfo } from "@/components/ui";

import { useState } from "react";
import Link from "next/link";

type BranchItem = {
  id: number;
  name: string;
  city: string;
  manager: string;
  occupancy: string;
  revenue: string;
};

export default function FranchiseChainDashboard() {
  const [branches, setBranches] = useState<BranchItem[]>([
    { id: 1, name: "SportsHub Elite Arena", city: "Surat", manager: "Rohan Patel", occupancy: "82%", revenue: "Rs 145,000" },
    { id: 2, name: "SportsHub Padel Central", city: "Dubai", manager: "Sarah Jenkins", occupancy: "74%", revenue: "AED 18,200" },
    { id: 3, name: "SportsHub London Oval", city: "London", manager: "David Miller", occupancy: "68%", revenue: "GBP 9,800" },
  ]);

  const [newCity, setNewCity] = useState("Surat");
  const [newName, setNewName] = useState("");
  const [newManager, setNewManager] = useState("");

  const handleAddBranch = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newName.trim() || !newManager.trim()) return;

    const newItem: BranchItem = {
      id: Math.random(),
      name: newName,
      city: newCity,
      manager: newManager,
      occupancy: "0%",
      revenue: "Rs 0",
    };

    setBranches((prev) => [...prev, newItem]);
    setNewName("");
    setNewManager("");
  };

  return (
    <main className="min-h-screen bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-indigo-950 via-slate-950 to-black text-white px-4 py-8 sm:px-8">
      <div className="mx-auto max-w-6xl space-y-10">
        
        {/* Header navigation */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between border-b border-white/10 pb-6 gap-4">
          <div>
            <h1 className="text-3xl font-extrabold bg-gradient-to-r from-white via-indigo-200 to-emerald-400 bg-clip-text text-transparent">🏢 Franchise Chain &amp; Organization Desk
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="🏢 Franchise Chain &amp; Organization Desk information" focusable={false}>Manage global multi-city sports branches, assign local branch managers, and audit performance.</TileInfo>
              </span></h1>
          </div>
          <div className="flex gap-3">
            <Link href="/owner/pricing" className="inline-flex items-center justify-center rounded-xl bg-white/5 border border-white/10 hover:bg-white/10 px-4 py-2.5 text-xs font-bold transition">
              ⚙️ Manage Dynamic Pricing
            </Link>
            <Link href="/personalized" className="inline-flex items-center justify-center rounded-xl bg-indigo-600 hover:bg-indigo-500 px-4 py-2.5 text-xs font-bold transition">
              Personalized Feed
            </Link>
          </div>
        </div>

        {/* Form and List Grid */}
        <div className="grid gap-8 lg:grid-cols-3">
          
          {/* Add Branch Form */}
          <div className="lg:col-span-1">
            <div className="rounded-3xl border border-white/10 bg-white/5 p-6 backdrop-blur-xl space-y-5 shadow-lg">
              <h3 className="text-lg font-bold text-white">➕ Register New Branch</h3>
              <p className="text-xs text-slate-400">Expand your franchise brand globally. Register turf location parameters instantly.</p>
              
              <form onSubmit={handleAddBranch} className="space-y-4">
                <div className="space-y-2">
                  <label className="text-xs text-slate-400 font-semibold uppercase">Branch name</label>
                  <input
                    type="text"
                    value={newName}
                    onChange={(e) => setNewName(e.target.value)}
                    placeholder="e.g. SportsHub Mumbai"
                    className="w-full rounded-xl border border-white/10 bg-black/40 px-4 py-3 text-white placeholder-slate-600 outline-none focus:border-indigo-500 text-sm"
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-xs text-slate-400 font-semibold uppercase">Branch Manager</label>
                  <input
                    type="text"
                    value={newManager}
                    onChange={(e) => setNewManager(e.target.value)}
                    placeholder="e.g. Amit Sharma"
                    className="w-full rounded-xl border border-white/10 bg-black/40 px-4 py-3 text-white placeholder-slate-600 outline-none focus:border-indigo-500 text-sm"
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-xs text-slate-400 font-semibold uppercase">Target City</label>
                  <select
                    value={newCity}
                    onChange={(e) => setNewCity(e.target.value)}
                    className="w-full rounded-xl border border-white/10 bg-black/40 px-4 py-3 text-white outline-none focus:border-indigo-500 text-sm"
                  >
                    <option value="Ahmedabad" className="bg-slate-950">Ahmedabad</option>
                    <option value="Mumbai" className="bg-slate-950">Mumbai</option>
                    <option value="Dubai" className="bg-slate-950">Dubai</option>
                    <option value="London" className="bg-slate-950">London</option>
                    <option value="Singapore" className="bg-slate-950">Singapore</option>
                  </select>
                </div>

                <button
                  type="submit"
                  className="w-full rounded-xl bg-gradient-to-r from-emerald-500 to-indigo-500 py-3 text-sm font-semibold text-white hover:brightness-110 shadow transition"
                >
                  Activate Branch
                </button>
              </form>
            </div>
          </div>

          {/* Active Branch Roster */}
          <div className="lg:col-span-2 space-y-6">
            <div className="rounded-3xl border border-white/10 bg-white/5 p-6 backdrop-blur-xl space-y-4 shadow-lg">
              <h3 className="text-lg font-bold text-white">🏢 Active Branch Network</h3>
              <p className="text-xs text-slate-400">Auditing active capacity utilization and branch manager operational metrics.</p>

              <div className="grid gap-4 sm:grid-cols-2">
                {branches.map((branch) => (
                  <div key={branch.id} className="rounded-2xl border border-white/5 bg-black/35 p-5 space-y-3 relative group overflow-hidden">
                    <div className="absolute -right-8 -top-8 h-16 w-16 rounded-full bg-indigo-500/10 blur-lg group-hover:bg-emerald-500/20 transition" />
                    
                    <div className="flex justify-between items-start">
                      <div>
                        <h4 className="font-bold text-white group-hover:text-emerald-300 transition">{branch.name}</h4>
                        <span className="inline-block rounded-full bg-emerald-500/10 px-2.5 py-0.5 text-[10px] font-semibold text-emerald-400 mt-2">
                          📍 {branch.city}
                        </span>
                      </div>
                      <span className="text-[11px] font-bold text-slate-400">{branch.occupancy} Occupancy</span>
                    </div>

                    <div className="border-t border-white/5 pt-3 space-y-1">
                      <p className="text-xs text-slate-500">Manager: <span className="text-slate-300 font-medium">{branch.manager}</span></p>
                      <p className="text-xs text-slate-500">Revenue (Base): <span className="text-emerald-400 font-bold">{branch.revenue}</span></p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

        </div>

      </div>
    </main>
  );
}
