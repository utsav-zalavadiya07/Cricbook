"use client";
import { TileInfo } from "@/components/ui";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  getCampaigns,
  createCampaign,
  sendCampaign,
  getCampaignAnalytics
} from "@/modules/admin/services/adminService";

type CampaignChannel = "EMAIL" | "PUSH" | "SMS" | "WHATSAPP";
type OwnerCampaign = {
  id: number;
  name: string;
  title?: string;
  channel?: CampaignChannel;
  status?: string;
  sentCount?: number;
};
type CampaignAnalytics = {
  sends?: number;
  views?: number;
  clicks?: number;
  conversions?: number;
};

export default function OwnerCampaignsPage() {
  const [campaigns, setCampaigns] = useState<OwnerCampaign[]>([]);
  const [selectedCampaignId, setSelectedCampaignId] = useState<number | null>(null);
  const [analytics, setAnalytics] = useState<CampaignAnalytics | null>(null);

  // Creation State
  const [name, setName] = useState("");
  const [title, setTitle] = useState("");
  const [message, setMessage] = useState("");
  const [channel, setChannel] = useState<CampaignChannel>("EMAIL");
  const [deepLink, setDeepLink] = useState("/turf");

  const [loading, setLoading] = useState(true);
  const [statusMsg, setStatusMsg] = useState("");

  async function loadOwnerCampaigns() {
    setLoading(true);
    try {
      const res = await getCampaigns();
      setCampaigns(res.data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void loadOwnerCampaigns(), 0);
    return () => window.clearTimeout(timer);
  }, []);

  const handleSelectCampaign = async (id: number) => {
    setSelectedCampaignId(id);
    setAnalytics(null);
    try {
      const res = await getCampaignAnalytics(id);
      setAnalytics(res.data);
    } catch (err) {
      console.error("Failed loading analytics", err);
    }
  };

  const handleCreateCampaign = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !title || !message) {
      setStatusMsg("Validation Error: Please fill all required fields.");
      return;
    }

    try {
      setStatusMsg("Creating new turf promotion campaign...");
      const res = await createCampaign({
        name,
        title,
        message,
        channel,
        targetRole: "ROLE_USER", // Targets players
        deepLink
      });
      setCampaigns((prev) => [res.data, ...prev]);
      setName("");
      setTitle("");
      setMessage("");
      setStatusMsg("Turf campaign saved successfully! Set as DRAFT.");
      setTimeout(() => setStatusMsg(""), 3000);
    } catch {
      setStatusMsg("Failed creating local campaign.");
    }
  };

  const handleSendCampaign = async (id: number) => {
    try {
      setStatusMsg("Broadcasting turf offers to players...");
      const res = await sendCampaign(id);
      setCampaigns((prev) =>
        prev.map((c) => (c.id === id ? res.data : c))
      );
      setStatusMsg(`Successfully sent promotion! Turf players notified.`);
      setTimeout(() => setStatusMsg(""), 3000);
      handleSelectCampaign(id);
    } catch {
      setStatusMsg("Failed sending turf promotion.");
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex font-sans">
      
      {/* Turf Owner Sidebar Navigation */}
      <aside className="w-64 bg-slate-900 border-r border-white/5 flex flex-col justify-between p-6 shrink-0">
        <div className="space-y-8">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className="text-xl">🏟️</span>
              <span className="font-bold text-white text-lg tracking-tight">Turf Partner</span>
            </div>
            <div className="px-2.5 py-0.5 bg-emerald-500/10 rounded-md border border-emerald-500/20 inline-block text-[10px] font-bold text-emerald-400 uppercase tracking-widest">
              Owner Panel
            </div>
          </div>

          <nav className="space-y-1">
            <Link href="/owner/dashboard" className="w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-xs font-semibold text-slate-400 hover:bg-white/5 hover:text-white transition-all">
              <span>📊</span> Dashboard
            </Link>
            <Link href="/owner/bookings" className="w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-xs font-semibold text-slate-400 hover:bg-white/5 hover:text-white transition-all">
              <span>🎟️</span> Slot Bookings
            </Link>
            <Link href="/owner/campaigns" className="w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 transition-all">
              <span>📢</span> Marketing Campaigns
            </Link>
            <Link href="/owner/slots" className="w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-xs font-semibold text-slate-400 hover:bg-white/5 hover:text-white transition-all">
              <span>⏰</span> Turf Slots
            </Link>
          </nav>
        </div>

        <div className="pt-6 border-t border-white/5">
          <Link href="/inbox" className="text-xs text-sky-400 hover:text-sky-300 font-medium">← Go to User Inbox</Link>
        </div>
      </aside>

      {/* Main owner campaigns workspace */}
      <main className="flex-1 p-8 space-y-6 overflow-y-auto">
        <div className="flex justify-between items-center border-b border-white/5 pb-4">
          <div>
            <h1 className="text-2xl font-bold text-white tracking-tight">Turf Campaigns &amp; Marketing
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Turf Campaigns &amp; Marketing information" focusable={false}>Promote open turf slots, offer happy-hour discounts, and track slot conversion ROI.</TileInfo>
              </span></h1>
          </div>
        </div>

        {statusMsg && (
          <div className="p-3 bg-emerald-500/10 border border-emerald-500/25 rounded-xl text-emerald-400 text-xs animate-pulse">
            {statusMsg}
          </div>
        )}

        <div className="grid grid-cols-1 xl:grid-cols-3 gap-8">
          
          {/* Col 1 & 2: Local Campaign builders */}
          <div className="xl:col-span-2 space-y-6">
            <div className="rounded-2xl border border-white/5 bg-slate-900/60 p-6 space-y-4">
              <h3 className="font-bold text-white text-sm">Create Turf Spot Promotion</h3>
              <form onSubmit={handleCreateCampaign} className="space-y-4">
                
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1">Promotion Name</label>
                    <input
                      type="text"
                      placeholder="e.g. Weekend slots 20% Off"
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      className="w-full bg-slate-950 text-xs border border-white/10 rounded-xl px-3 py-2.5 text-slate-300 outline-none focus:border-white/20"
                    />
                  </div>

                  <div>
                    <label className="block text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1">Push Alert Title</label>
                    <input
                      type="text"
                      placeholder="e.g. Happy Hour slots Available!"
                      value={title}
                      onChange={(e) => setTitle(e.target.value)}
                      className="w-full bg-slate-950 text-xs border border-white/10 rounded-xl px-3 py-2.5 text-slate-300 outline-none focus:border-white/20"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1">
                    Alert Message Body
                  </label>
                  <textarea
                    rows={4}
                    placeholder="Hello! Book any open slot this Saturday and receive an automatic 20% discount on arrival!"
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    className="w-full bg-slate-950 text-xs border border-white/10 rounded-xl px-3 py-2.5 text-slate-300 outline-none focus:border-white/20 font-sans resize-none"
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1">Delivery Channel</label>
                    <select
                      value={channel}
                      onChange={(e) => setChannel(e.target.value as CampaignChannel)}
                      className="w-full bg-slate-950 text-xs border border-white/10 rounded-xl px-3 py-2 text-slate-300 outline-none cursor-pointer"
                    >
                      <option value="EMAIL">Email Alert</option>
                      <option value="PUSH">Push Notification</option>
                      <option value="SMS">SMS Text</option>
                      <option value="WHATSAPP">WhatsApp Confirmation</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1">Deep Link redirect target</label>
                    <input
                      type="text"
                      value={deepLink}
                      onChange={(e) => setDeepLink(e.target.value)}
                      className="w-full bg-slate-950 text-xs border border-white/10 rounded-xl px-3 py-2 text-slate-300 outline-none focus:border-white/20"
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  className="w-full py-3 bg-emerald-500 hover:bg-emerald-600 text-white rounded-xl text-xs font-bold transition-all shadow-lg shadow-emerald-500/10 uppercase tracking-wider"
                >
                  Create Local Turf Promotion
                </button>
              </form>
            </div>

            {/* Existing local Promotions */}
            <div className="rounded-2xl border border-white/5 bg-slate-900/60 p-6 space-y-4">
              <h3 className="font-bold text-white text-sm">Local Pitch Promotions Queue</h3>
              
              {loading ? (
                <div className="text-xs text-slate-400 animate-pulse">Syncing promotion logs...</div>
              ) : campaigns.length === 0 ? (
                <div className="text-xs text-slate-500 py-6">No turf promos logged. Design one above.</div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="border-b border-white/5 text-slate-500 font-bold uppercase tracking-wider pb-3">
                        <th className="pb-3">Promotion Name</th>
                        <th className="pb-3">Channel</th>
                        <th className="pb-3">Status</th>
                        <th className="pb-3">Audience Sends</th>
                        <th className="pb-3 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {campaigns.map((c) => (
                        <tr
                          key={c.id}
                          onClick={() => handleSelectCampaign(c.id)}
                          className={`border-b border-white/5 cursor-pointer transition-all hover:bg-white/5 ${
                            selectedCampaignId === c.id ? "bg-emerald-500/5" : ""
                          }`}
                        >
                          <td className="py-4">
                            <p className="font-semibold text-white">{c.name}</p>
                            <p className="text-[10px] text-slate-500 mt-0.5">{c.title}</p>
                          </td>
                          <td className="py-4">
                            <span className="px-1.5 py-0.5 bg-slate-950 text-slate-300 rounded font-mono text-[10px]">
                              {c.channel}
                            </span>
                          </td>
                          <td className="py-4">
                            <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                              c.status === "SENT" ? "bg-emerald-500/10 text-emerald-400" : "bg-yellow-500/10 text-yellow-400"
                            }`}>
                              {c.status}
                            </span>
                          </td>
                          <td className="py-4 font-bold text-white text-center">{c.sentCount}</td>
                          <td className="py-4 text-right">
                            {c.status !== "SENT" && (
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleSendCampaign(c.id);
                                }}
                                className="px-2.5 py-1 bg-emerald-500 text-white text-[10px] font-bold rounded-lg hover:bg-emerald-600 transition-all"
                              >
                                Send Alert
                              </button>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>

          {/* Col 3: Engagement Metrics Funnel */}
          <div>
            <div className="rounded-2xl border border-white/5 bg-slate-900/60 p-6 space-y-6 sticky top-24">
              <div>
                <h3 className="font-bold text-white text-base">Campaign Slot Conversions</h3>
                <p className="text-slate-400 text-[10px] mt-0.5">Select a sent campaign to view user open rates and conversions.</p>
              </div>

              {selectedCampaignId === null ? (
                <div className="py-12 text-center text-slate-500 text-xs">
                  Select a sent turf campaign to retrieve live engagement metrics.
                </div>
              ) : (
                <div className="space-y-6 text-xs">
                  <div className="p-4 bg-slate-950 rounded-xl border border-emerald-500/15">
                    <span className="text-[10px] text-slate-500 block uppercase tracking-wider mb-1">Selected ID</span>
                    <span className="text-sm font-bold text-emerald-400">Campaign ID #{selectedCampaignId}</span>
                  </div>

                  <div className="space-y-4">
                    <FunnelMetric label="Pushed Sends" value={analytics?.sends || 0} sub="Players Dispatched" />
                    <FunnelMetric label="User Views" value={analytics?.views || 0} percent={analytics?.sends ? ((analytics?.views ?? 0) / analytics.sends) * 100 : 0} sub="Open CTR" />
                    <FunnelMetric label="CTR Clicks" value={analytics?.clicks || 0} percent={analytics?.views ? ((analytics?.clicks ?? 0) / analytics.views) * 100 : 0} sub="Deep Link clicks" />
                    <FunnelMetric label="Slot Booking Conversions" value={analytics?.conversions || 0} sub="Completed Pitch Sales" />
                  </div>
                </div>
              )}
            </div>
          </div>

        </div>
      </main>
    </div>
  );
}

function FunnelMetric({ label, value, percent, sub }: { label: string; value: number; percent?: number; sub: string }) {
  return (
    <div className="space-y-1">
      <div className="flex justify-between items-center text-xs">
        <span className="font-semibold text-slate-300">{label}</span>
        <span className="font-bold text-white">
          {value} {percent !== undefined && `(${percent.toFixed(0)}%)`}
        </span>
      </div>
      <div className="w-full bg-slate-950 h-2 rounded-full overflow-hidden border border-white/5">
        <div
          className="bg-gradient-to-r from-emerald-500 to-sky-500 h-full rounded-full transition-all"
          style={{ width: `${percent !== undefined ? Math.min(100, percent) : 100}%` }}
        />
      </div>
      <span className="text-[9px] text-slate-500 block">{sub}</span>
    </div>
  );
}
