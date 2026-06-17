"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { getTemplates, createTemplate, deleteTemplate } from "@/modules/admin/services/adminService";

type AdminChannel = "EMAIL" | "PUSH" | "SMS" | "WHATSAPP";

type AdminTemplate = {
  id: number;
  name: string;
  subject?: string;
  body: string;
  channel: AdminChannel;
};

export default function AdminTemplatesPage() {
  const [templates, setTemplates] = useState<AdminTemplate[]>([]);
  const [name, setName] = useState("");
  const [subject, setSubject] = useState("");
  const [body, setBody] = useState("");
  const [channel, setChannel] = useState<AdminChannel>("EMAIL");

  const [loading, setLoading] = useState(true);
  const [statusMsg, setStatusMsg] = useState("");

  async function loadTemplates() {
    setLoading(true);
    try {
      const res = await getTemplates();
      setTemplates(res.data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void loadTemplates(), 0);
    return () => window.clearTimeout(timer);
  }, []);

  const handleCreateTemplate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !body) {
      setStatusMsg("Validation Error: Name and Body are required.");
      return;
    }

    try {
      setStatusMsg("Saving new reusable template...");
      const res = await createTemplate({ name, subject: subject || null, body, channel });
      setTemplates((prev) => [...prev, res.data]);
      setName("");
      setSubject("");
      setBody("");
      setStatusMsg("Template successfully added!");
      setTimeout(() => setStatusMsg(""), 3000);
    } catch {
      setStatusMsg("Failed saving template. (Name must be unique)");
    }
  };

  const handleDeleteTemplate = async (id: number) => {
    try {
      await deleteTemplate(id);
      setTemplates((prev) => prev.filter((t) => t.id !== id));
      setStatusMsg("Template removed successfully.");
      setTimeout(() => setStatusMsg(""), 3000);
    } catch {
      setStatusMsg("Failed removing template.");
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
            <Link href="/admin/templates" className="w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 transition-all">
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

      {/* Main templates workspace */}
      <main className="flex-1 p-8 space-y-6 overflow-y-auto">
        <div className="flex justify-between items-center border-b border-white/5 pb-4">
          <div>
            <h1 className="text-2xl font-bold text-white tracking-tight">Notification Templates</h1>
            <p className="text-slate-400 text-xs mt-1">Manage reusable message layouts with dynamic variables.</p>
          </div>
        </div>

        {statusMsg && (
          <div className="p-3 bg-emerald-500/10 border border-emerald-500/25 rounded-xl text-emerald-400 text-xs animate-pulse">
            {statusMsg}
          </div>
        )}

        <div className="grid grid-cols-1 xl:grid-cols-3 gap-8">
          
          {/* Col 1 & 2: Template Builder form */}
          <div className="xl:col-span-2 space-y-6">
            <div className="rounded-2xl border border-white/5 bg-slate-900/60 p-6 space-y-4">
              <h3 className="font-bold text-white text-sm">Add New Template</h3>
              <form onSubmit={handleCreateTemplate} className="space-y-4">
                
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1">Unique template Name</label>
                    <input
                      type="text"
                      placeholder="e.g. booking_confirmation_email"
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      className="w-full bg-slate-950 text-xs border border-white/10 rounded-xl px-3 py-2.5 text-slate-300 outline-none focus:border-white/20"
                    />
                  </div>

                  <div>
                    <label className="block text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1">Email Subject Line (Optional)</label>
                    <input
                      type="text"
                      placeholder="e.g. Booking Confirmed!"
                      value={subject}
                      onChange={(e) => setSubject(e.target.value)}
                      className="w-full bg-slate-950 text-xs border border-white/10 rounded-xl px-3 py-2.5 text-slate-300 outline-none focus:border-white/20"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1">Delivery Channel</label>
                  <select
                    value={channel}
                    onChange={(e) => setChannel(e.target.value as AdminChannel)}
                    className="w-full bg-slate-950 text-xs border border-white/10 rounded-xl px-3 py-2 text-slate-300 outline-none cursor-pointer"
                  >
                    <option value="EMAIL">EMAIL</option>
                    <option value="PUSH">PUSH</option>
                    <option value="SMS">SMS</option>
                    <option value="WHATSAPP">WHATSAPP</option>
                  </select>
                </div>

                <div>
                  <label className="block text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1">
                    Template Body (Supported variables: <code>{"{{name}}"}</code>, <code>{"{{booking}}"}</code>, <code>{"{{date}}"}</code>)
                  </label>
                  <textarea
                    rows={6}
                    placeholder="Hello {{name}}, your booking for {{booking}} on {{date}} is confirmed!"
                    value={body}
                    onChange={(e) => setBody(e.target.value)}
                    className="w-full bg-slate-950 text-xs border border-white/10 rounded-xl px-3 py-2.5 text-slate-300 outline-none focus:border-white/20 font-sans resize-none"
                  />
                </div>

                <button
                  type="submit"
                  className="w-full py-3 bg-emerald-500 hover:bg-emerald-600 text-white rounded-xl text-xs font-bold transition-all shadow-lg shadow-emerald-500/10 uppercase tracking-wider"
                >
                  Save Template Layout
                </button>
              </form>
            </div>

            {/* Existing Templates Registry */}
            <div className="rounded-2xl border border-white/5 bg-slate-900/60 p-6 space-y-4">
              <h3 className="font-bold text-white text-sm">System Templates</h3>

              {loading ? (
                <div className="text-xs text-slate-400 animate-pulse">Syncing templates...</div>
              ) : templates.length === 0 ? (
                <div className="text-xs text-slate-500 py-6">No templates saved. Use creator above.</div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {templates.map((t) => (
                    <div key={t.id} className="p-4 bg-slate-950 border border-white/5 rounded-xl flex flex-col justify-between gap-4">
                      <div className="space-y-2">
                        <div className="flex justify-between items-center">
                          <span className="font-bold text-white text-xs truncate max-w-[150px]">{t.name}</span>
                          <span className="px-1.5 py-0.2 bg-slate-900 text-[10px] text-slate-400 rounded uppercase font-mono">
                            {t.channel}
                          </span>
                        </div>
                        {t.subject && (
                          <p className="text-[10px] font-semibold text-emerald-400 truncate">Subject: {t.subject}</p>
                        )}
                        <p className="text-xs text-slate-400 leading-relaxed font-mono line-clamp-3 bg-black/10 p-2.5 rounded-lg">
                          {t.body}
                        </p>
                      </div>
                      <button
                        onClick={() => handleDeleteTemplate(t.id)}
                        className="w-full py-1.5 bg-red-500/5 hover:bg-red-500/10 text-red-400 border border-red-500/10 text-[10px] font-bold rounded-lg transition-all"
                      >
                        Delete Template
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Col 3: Guidelines widget */}
          <div>
            <div className="rounded-2xl border border-white/5 bg-slate-900/60 p-6 space-y-4 sticky top-24">
              <h3 className="font-bold text-white text-base">Variable Helper</h3>
              <p className="text-slate-400 text-xs leading-relaxed">
                Dynamic templates let you inject dynamic Cricbook properties.
              </p>
              
              <div className="space-y-3 pt-2 text-xs">
                <div className="p-3 bg-black/20 rounded-xl border border-white/5">
                  <span className="font-bold text-white block">{"{{name}}"}</span>
                  <span className="text-[10px] text-slate-500 mt-0.5">Resolves to the player full name.</span>
                </div>
                <div className="p-3 bg-black/20 rounded-xl border border-white/5">
                  <span className="font-bold text-white block">{"{{booking}}"}</span>
                  <span className="text-[10px] text-slate-500 mt-0.5">Resolves to booking slip details.</span>
                </div>
                <div className="p-3 bg-black/20 rounded-xl border border-white/5">
                  <span className="font-bold text-white block">{"{{date}}"}</span>
                  <span className="text-[10px] text-slate-500 mt-0.5">Resolves to appointment booking date.</span>
                </div>
              </div>
            </div>
          </div>

        </div>
      </main>
    </div>
  );
}
