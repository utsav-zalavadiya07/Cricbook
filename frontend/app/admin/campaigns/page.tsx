"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  getCampaigns,
  createCampaign,
  sendCampaign,
  getCampaignAnalytics,
  getTemplates,
  getSegments
} from "@/modules/admin/services/adminService";

type AdminChannel = "EMAIL" | "PUSH" | "SMS" | "WHATSAPP";

type LegacyCampaign = {
  id: number;
  name: string;
  title?: string;
  message?: string;
  channel?: AdminChannel;
  targetRole?: string;
  deepLink?: string;
  status?: string;
  sentCount?: number;
};

type LegacyTemplate = {
  id: number;
  name: string;
  subject?: string;
  body: string;
  channel: AdminChannel;
};

type LegacySegment = {
  id: number;
  name: string;
};

type CampaignAnalytics = {
  sends?: number;
  views?: number;
  clicks?: number;
  conversions?: number;
  sent?: number;
  opened?: number;
  clicked?: number;
  conversionRate?: number;
};

export default function AdminCampaignsPage() {
  const [campaigns, setCampaigns] = useState<LegacyCampaign[]>([]);
  const [templates, setTemplates] = useState<LegacyTemplate[]>([]);
  const [segments, setSegments] = useState<LegacySegment[]>([]);
  const [selectedCampaignId, setSelectedCampaignId] = useState<number | null>(null);
  const [analytics, setAnalytics] = useState<CampaignAnalytics | null>(null);

  // Creation State
  const [name, setName] = useState("");
  const [title, setTitle] = useState("");
  const [message, setMessage] = useState("");
  const [channel, setChannel] = useState<AdminChannel>("EMAIL");
  const [targetRole, setTargetRole] = useState("ROLE_USER");
  const [deepLink, setDeepLink] = useState("/turfs");
  const [scheduledAt, setScheduledAt] = useState("");

  const [loading, setLoading] = useState(true);
  const [statusMsg, setStatusMsg] = useState("");

  async function loadData() {
    setLoading(true);
    try {
      const [campRes, tempRes, segRes] = await Promise.all([
        getCampaigns(),
        getTemplates().catch(() => ({ data: [] })),
        getSegments().catch(() => ({ data: [] }))
      ]);
      setCampaigns(campRes.data || []);
      setTemplates(tempRes.data || []);
      setSegments(segRes.data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void loadData(), 0);
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
      setStatusMsg("Creating new campaign entity...");
      const res = await createCampaign({
        name,
        title,
        message,
        channel,
        targetRole,
        deepLink
      });
      setCampaigns((prev) => [res.data, ...prev]);
      setName("");
      setTitle("");
      setMessage("");
      setStatusMsg("Campaign successfully recorded! Set as DRAFT/SCHEDULED.");
      setTimeout(() => setStatusMsg(""), 3000);
    } catch {
      setStatusMsg("Failed creating campaign.");
    }
  };

  const handleSendCampaign = async (id: number) => {
    try {
      setStatusMsg("Broadcasting campaign to preferences-checked user segments...");
      const res = await sendCampaign(id);
      setCampaigns((prev) =>
        prev.map((c) => (c.id === id ? res.data : c))
      );
      setStatusMsg(`Successfully sent campaign! Targeted users notified.`);
      setTimeout(() => setStatusMsg(""), 3000);
      handleSelectCampaign(id);
    } catch {
      setStatusMsg("Failed sending campaign.");
    }
  };

  const handleApplyTemplate = (tempName: string) => {
    const selected = templates.find((t) => t.name === tempName);
    if (selected) {
      setTitle(selected.subject || "Alert");
      setMessage(selected.body);
      setChannel(selected.channel);
    }
  };

  const handleApplySegment = (segName: string) => {
    setTargetRole(segName); // Use the segment name directly as the audience
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex font-sans">
      
      {/* Sidebar Navigation layout */}
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
            <Link href="/admin/campaigns" className="w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 transition-all">
              <span>📢</span> Campaign Manager
            </Link>
            <Link href="/admin/templates" className="w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-xs font-semibold text-slate-400 hover:bg-white/5 hover:text-white transition-all">
              <span>✉️</span> Template Builder
            </Link>
            <Link href="/admin/segments" className="w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-xs font-semibold text-slate-400 hover:bg-white/5 hover:text-white transition-all">
              <span>👥</span> Audience Segments
            </Link>
          </nav>
        </div>

        <div className="pt-6 border-t border-white/5">
          <Link href="/inbox" className="text-xs text-sky-400 hover:text-sky-300 font-medium">← Go to User Inbox</Link>
        </div>
      </aside>

      {/* Main campaigns workspace */}
      <main className="flex-1 p-8 space-y-6 overflow-y-auto">
        <div className="flex justify-between items-center border-b border-white/5 pb-4">
          <div>
            <h1 className="text-2xl font-bold text-white tracking-tight">Campaign Operations</h1>
            <p className="text-slate-400 text-xs mt-1">Design behavior-driven marketing pushes, target segments, and audit real-time conversions.</p>
          </div>
          <button
            onClick={loadData}
            className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-xs font-bold rounded-xl border border-white/5 transition-all"
          >
            Sync Data
          </button>
        </div>

        {statusMsg && (
          <div className="p-3 bg-emerald-500/10 border border-emerald-500/25 rounded-xl text-emerald-400 text-xs animate-pulse">
            {statusMsg}
          </div>
        )}

        <div className="grid grid-cols-1 xl:grid-cols-3 gap-8">
          
          {/* Col 1 & 2: Campaign builders and active campaigns logs */}
          <div className="xl:col-span-2 space-y-6">
            
            {/* Create Campaign form */}
            <div className="rounded-2xl border border-white/5 bg-slate-900/60 p-6 space-y-4">
              <h3 className="font-bold text-white text-sm">Create New Marketing Campaign</h3>
              <form onSubmit={handleCreateCampaign} className="space-y-4">
                
                {/* Seed templates and segment quick selectors if available */}
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1">Apply seeded template</label>
                    <select
                      onChange={(e) => handleApplyTemplate(e.target.value)}
                      defaultValue=""
                      className="w-full bg-slate-950 text-xs border border-white/10 rounded-xl px-3 py-2 text-slate-300 outline-none cursor-pointer"
                    >
                      <option value="" disabled>-- Quick Templates --</option>
                      {templates.map((t) => (
                        <option key={t.id} value={t.name}>{t.name}</option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1">Target Segment</label>
                    <select
                      onChange={(e) => handleApplySegment(e.target.value)}
                      defaultValue=""
                      className="w-full bg-slate-950 text-xs border border-white/10 rounded-xl px-3 py-2 text-slate-300 outline-none cursor-pointer"
                    >
                      <option value="" disabled>-- Target Audience Segments --</option>
                      <option value="ROLE_USER">All Active Users</option>
                      <option value="ROLE_OWNER">Turf Owners Only</option>
                      <option value="Frequent Players">Frequent Players (&gt;5 Bookings)</option>
                      <option value="New Users">New Users (Last 7 Days)</option>
                      {segments.map((s) => (
                        <option key={s.id} value={s.name}>{s.name}</option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1">Campaign name</label>
                    <input
                      type="text"
                      placeholder="e.g. Inactive Winback Coupon"
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      className="w-full bg-slate-950 text-xs border border-white/10 rounded-xl px-3 py-2.5 text-slate-300 outline-none focus:border-white/20"
                    />
                  </div>

                  <div>
                    <label className="block text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1">Alert Subject / Title</label>
                    <input
                      type="text"
                      placeholder="e.g. We Miss You! 🏏"
                      value={title}
                      onChange={(e) => setTitle(e.target.value)}
                      className="w-full bg-slate-950 text-xs border border-white/10 rounded-xl px-3 py-2.5 text-slate-300 outline-none focus:border-white/20"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1">
                    Message Body (Placeholders: <code>{"{{name}}"}</code>, <code>{"{{booking}}"}</code>, <code>{"{{date}}"}</code>)
                  </label>
                  <textarea
                    rows={4}
                    placeholder="Hello {{name}}, book again today and enjoy 20% off at Cricbook!"
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    className="w-full bg-slate-950 text-xs border border-white/10 rounded-xl px-3 py-2.5 text-slate-300 outline-none focus:border-white/20 font-sans resize-none"
                  />
                </div>

                <div className="grid grid-cols-3 gap-4">
                  <div>
                    <label className="block text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1">Delivery Channel</label>
                    <select
                      value={channel}
                      onChange={(e) => setChannel(e.target.value as AdminChannel)}
                      className="w-full bg-slate-950 text-xs border border-white/10 rounded-xl px-3 py-2 text-slate-300 outline-none cursor-pointer"
                    >
                      <option value="EMAIL">Email Confirmation</option>
                      <option value="PUSH">Push Notification</option>
                      <option value="SMS">SMS Text Alert</option>
                      <option value="WHATSAPP">WhatsApp Message</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1">Deep Link Target</label>
                    <input
                      type="text"
                      value={deepLink}
                      onChange={(e) => setDeepLink(e.target.value)}
                      className="w-full bg-slate-950 text-xs border border-white/10 rounded-xl px-3 py-2 text-slate-300 outline-none focus:border-white/20"
                    />
                  </div>

                  <div>
                    <label className="block text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1">Schedule date (Optional)</label>
                    <input
                      type="datetime-local"
                      value={scheduledAt}
                      onChange={(e) => setScheduledAt(e.target.value)}
                      className="w-full bg-slate-950 text-xs border border-white/10 rounded-xl px-3 py-1.5 text-slate-300 outline-none focus:border-white/20"
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  className="w-full py-3 bg-emerald-500 hover:bg-emerald-600 text-white rounded-xl text-xs font-bold transition-all shadow-lg shadow-emerald-500/10 uppercase tracking-wider"
                >
                  Create &amp; Schedule Campaign
                </button>
              </form>
            </div>

            {/* Existing Campaigns List */}
            <div className="rounded-2xl border border-white/5 bg-slate-900/60 p-6 space-y-4">
              <h3 className="font-bold text-white text-sm">Campaigns Dispatch Registry</h3>
              
              {loading ? (
                <div className="text-xs text-slate-400 animate-pulse">Syncing campaigns queue...</div>
              ) : campaigns.length === 0 ? (
                <div className="text-xs text-slate-500 py-6">No campaigns found. Create one above.</div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="border-b border-white/5 text-slate-500 font-bold uppercase tracking-wider pb-3">
                        <th className="pb-3">Campaign Name</th>
                        <th className="pb-3">Target Audience</th>
                        <th className="pb-3">Channel</th>
                        <th className="pb-3">Status</th>
                        <th className="pb-3">Sends</th>
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
                          <td className="py-4 text-slate-400">{c.targetRole || "All Active"}</td>
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
                          <td className="py-4 font-bold text-white">{c.sentCount}</td>
                          <td className="py-4 text-right">
                            {c.status !== "SENT" && (
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleSendCampaign(c.id);
                                }}
                                className="px-2.5 py-1 bg-emerald-500 text-white text-[10px] font-bold rounded-lg hover:bg-emerald-600 transition-all"
                              >
                                Trigger Dispatch
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

          {/* Col 3: Real-Time CTR & ROI Conversion Dashboard */}
          <div>
            <div className="rounded-2xl border border-white/5 bg-slate-900/60 p-6 space-y-6 sticky top-24">
              <div>
                <h3 className="font-bold text-white text-base">Campaign CTR Funnel</h3>
                <p className="text-slate-400 text-[10px] mt-0.5">Select any sent campaign to audit user open rates and conversions.</p>
              </div>

              {selectedCampaignId === null ? (
                <div className="py-12 text-center text-slate-500 text-xs">
                  Select a campaign in the registry table to fetch live engagement metrics.
                </div>
              ) : (
                <div className="space-y-6">
                  <div className="p-4 bg-slate-950 rounded-xl border border-emerald-500/15">
                    <span className="text-[10px] text-slate-500 block uppercase tracking-wider mb-1">Selected ID</span>
                    <span className="text-sm font-bold text-emerald-400">Campaign ID #{selectedCampaignId}</span>
                  </div>

                  <div className="space-y-4">
                    <FunnelMetric label="Pushed Sends" value={analytics?.sends || 0} sub="Dispatched from Bus" />
                    <FunnelMetric label="User Views" value={analytics?.views || 0} percent={analytics?.sends ? ((analytics?.views ?? 0) / analytics.sends) * 100 : 0} sub="CTR Open Rate" />
                    <FunnelMetric label="CTR Clicks" value={analytics?.clicks || 0} percent={analytics?.views ? ((analytics?.clicks ?? 0) / analytics.views) * 100 : 0} sub="Action Button Clicks" />
                    <FunnelMetric label="Conversions ROI" value={analytics?.conversions || 0} sub="Bookings Created" />
                  </div>

                  {/* ROI Conversion metrics */}
                  <div className="pt-4 border-t border-white/5 space-y-2">
                    <h4 className="text-[11px] text-slate-400 font-bold uppercase tracking-wider">ROI Summary</h4>
                    <div className="grid grid-cols-2 gap-3">
                      <div className="p-3 bg-black/20 rounded-xl border border-white/5 text-center">
                        <span className="text-[10px] text-slate-500 block">Click CTR</span>
                        <span className="text-base font-bold text-white">
                          {analytics?.views ? (((analytics?.clicks ?? 0) / analytics.views) * 100).toFixed(1) : "0.0"}%
                        </span>
                      </div>
                      <div className="p-3 bg-black/20 rounded-xl border border-white/5 text-center">
                        <span className="text-[10px] text-slate-500 block">Conversions</span>
                        <span className="text-base font-bold text-emerald-400">
                          {analytics?.conversions ? analytics.conversions : 0} sales
                        </span>
                      </div>
                    </div>
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
      <div className="w-full bg-slate-950 h-2.5 rounded-full overflow-hidden border border-white/5">
        <div
          className="bg-gradient-to-r from-emerald-500 to-sky-500 h-full rounded-full transition-all"
          style={{ width: `${percent !== undefined ? Math.min(100, percent) : 100}%` }}
        />
      </div>
      <span className="text-[9px] text-slate-500 block">{sub}</span>
    </div>
  );
}
