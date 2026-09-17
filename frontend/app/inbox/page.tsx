"use client";
import { TileInfo } from "@/components/ui";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import {
  getNotifications,
  markAsRead,
  archiveNotification,
  getRecommendations,
  trackMetric,
  triggerEvent
} from "@/modules/notification/services/notificationService";
import { Notification } from "@/modules/notification/types/notification.types";
import { isNotificationArchived, isNotificationRead } from "@/modules/notification/utils/notificationState";

type FolderName = "ALL" | "TODAY" | "THIS_WEEK" | "ARCHIVE";

type Recommendation = {
  type: string;
  message: string;
  score: number;
};

export default function InboxCenterPage() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [recommendations, setRecommendations] = useState<Recommendation[]>([]);
  const [activeFolder, setActiveFolder] = useState<FolderName>("ALL");
  const [searchQuery, setSearchQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [statusMsg, setStatusMsg] = useState("");
  const [unreadsCount, setUnreadsCount] = useState(0);

  // Trigger values for demo
  const [eventInput, setEventInput] = useState("BOOKING_CREATED");
  const [userIdInput, setUserIdInput] = useState("1");
  const [bookingIdInput, setBookingIdInput] = useState("502");

  const loadInboxData = useCallback(async () => {
    setLoading(true);
    try {
      const [notifRes, recRes] = await Promise.all([
        getNotifications({ folder: activeFolder, search: searchQuery }),
        getRecommendations()
      ]);
      setNotifications(notifRes.data || []);
      setRecommendations(recRes.data || []);

      // Calculate unreads
      const nextNotifications = (notifRes.data || []) as Notification[];
      const unreads = nextNotifications.filter((notification) => !isNotificationRead(notification) && !isNotificationArchived(notification)).length;
      setUnreadsCount(unreads);

      nextNotifications.forEach((notification) => {
        trackMetric(notification.id, "view").catch(() => {});
      });
    } catch (err) {
      console.error("Failed loading inbox", err);
    } finally {
      setLoading(false);
    }
  }, [activeFolder, searchQuery]);

  useEffect(() => {
    const timer = window.setTimeout(() => void loadInboxData(), 0);
    return () => window.clearTimeout(timer);
  }, [loadInboxData]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    void loadInboxData();
  };

  const handleRead = async (id: number) => {
    try {
      const target = notifications.find((notification) => notification.id === id);
      if (!target || isNotificationRead(target) || isNotificationArchived(target)) {
        return;
      }
      await markAsRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, status: "READ", read: true, readAt: new Date().toISOString() } : n))
      );
      setUnreadsCount((prev) => Math.max(0, prev - 1));
    } catch (err) {
      console.error(err);
    }
  };

  const handleArchive = async (id: number) => {
    try {
      const target = notifications.find((notification) => notification.id === id);
      if (!target || isNotificationArchived(target)) {
        return;
      }
      await archiveNotification(id);
      setNotifications((prev) => prev.filter((notification) => notification.id !== id));
      if (!isNotificationRead(target)) {
        setUnreadsCount((prev) => Math.max(0, prev - 1));
      }
      setStatusMsg("Notification moved to Archive folder.");
      setTimeout(() => setStatusMsg(""), 3000);
    } catch (err) {
      console.error(err);
    }
  };

  const handleRecClick = (rec: Recommendation) => {
    setStatusMsg(`Opened action: ${rec.message}`);
    setTimeout(() => setStatusMsg(""), 4000);
  };

  const handleDeepLinkClick = async (notif: Notification) => {
    try {
      await trackMetric(notif.id, "click");
      await markAsRead(notif.id);
      setStatusMsg(`Redirecting to deep link target: ${notif.deepLink || "/"}`);
      setTimeout(() => setStatusMsg(""), 4000);
    } catch (err) {
      console.error(err);
    }
  };

  const handleTriggerDemoEvent = async () => {
    try {
      setStatusMsg(`Publishing event ${eventInput} to Event Bus...`);
      await triggerEvent(eventInput, {
        userId: userIdInput,
        email: "player@cricbook.com",
        name: "Utsav",
        bookingId: bookingIdInput,
        amount: "1200",
        date: "2026-05-28",
        turfName: "Super Arena Ahmedabad"
      });
      setStatusMsg(`Event ${eventInput} successfully processed! Fetching fresh inbox...`);
      setTimeout(() => {
        setStatusMsg("");
        loadInboxData();
      }, 1500);
    } catch {
      setStatusMsg("Failed publishing event to Event Bus.");
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans">
      {/* Top sticky navbar */}
      <header className="border-b border-white/5 bg-slate-900/60 backdrop-blur-md px-6 py-4 flex justify-between items-center sticky top-0 z-50">
        <div className="flex items-center gap-3">
          <span className="text-xl">📥</span>
          <div>
            <h1 className="text-base font-bold text-white tracking-wide">Communication Hub
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Communication Hub information" focusable={false}>Manage transactional inbox alerts and campaigns.</TileInfo>
              </span></h1>
          </div>
        </div>
        <div className="flex gap-2">
          <Link
            href="/notifications/settings"
            className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-white/5 rounded-xl text-xs font-semibold tracking-wide transition-all"
          >
            ⚙️ Preferences Settings
          </Link>
        </div>
      </header>

      <main className="flex-1 max-w-6xl mx-auto w-full px-6 py-8 grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Left column: folders + demo triggers */}
        <div className="space-y-6">
          {/* Folders widget */}
          <div className="rounded-2xl border border-white/5 bg-slate-900/60 p-5 space-y-4">
            <h3 className="font-bold text-white text-xs uppercase tracking-wider text-slate-400">Folders</h3>
            <nav className="space-y-1">
              <FolderButton label="All Notifications" active={activeFolder === "ALL"} count={notifications.length} onClick={() => setActiveFolder("ALL")} />
              <FolderButton label="Today Alerts" active={activeFolder === "TODAY"} count={activeFolder === "TODAY" ? notifications.length : undefined} onClick={() => setActiveFolder("TODAY")} />
              <FolderButton label="This Week" active={activeFolder === "THIS_WEEK"} count={activeFolder === "THIS_WEEK" ? notifications.length : undefined} onClick={() => setActiveFolder("THIS_WEEK")} />
              <FolderButton label="Archived Logs" active={activeFolder === "ARCHIVE"} count={activeFolder === "ARCHIVE" ? notifications.length : undefined} onClick={() => setActiveFolder("ARCHIVE")} />
            </nav>
          </div>

          {/* Quick simulator widget */}
          <div className="rounded-2xl border border-white/5 bg-slate-900/60 p-5 space-y-4">
            <h3 className="font-bold text-white text-xs uppercase tracking-wider text-slate-400">Rules Engine Test</h3>
            <p className="text-[10px] text-slate-500">Inject automated workflow behavior events into our dynamic Event Bus.</p>
            <div className="space-y-3">
              <div>
                <label className="block text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1">Event Type</label>
                <select
                  value={eventInput}
                  onChange={(e) => setEventInput(e.target.value)}
                  className="w-full bg-slate-950 text-xs border border-white/10 rounded-xl px-3 py-2 text-slate-300 outline-none"
                >
                  <option value="BOOKING_CREATED">BOOKING_CREATED (Transactional)</option>
                  <option value="PAYMENT_SUCCESS">PAYMENT_SUCCESS (Receipt)</option>
                  <option value="MATCH_STARTED">MATCH_STARTED (Match Live Broadcast)</option>
                  <option value="REVIEW_RECEIVED">REVIEW_RECEIVED (Owner Alert)</option>
                </select>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1">User ID</label>
                  <input
                    type="text"
                    value={userIdInput}
                    onChange={(e) => setUserIdInput(e.target.value)}
                    className="w-full bg-slate-950 text-xs border border-white/10 rounded-xl px-3 py-2 text-slate-300 outline-none"
                  />
                </div>
                <div>
                  <label className="block text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1">Booking ID</label>
                  <input
                    type="text"
                    value={bookingIdInput}
                    onChange={(e) => setBookingIdInput(e.target.value)}
                    className="w-full bg-slate-950 text-xs border border-white/10 rounded-xl px-3 py-2 text-slate-300 outline-none"
                  />
                </div>
              </div>

              <button
                onClick={handleTriggerDemoEvent}
                className="w-full py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-xl text-xs font-bold transition-all shadow-lg shadow-emerald-500/10"
              >
                Publish Event
              </button>
            </div>
          </div>
        </div>

        {/* Right 2 columns: Inbox messages & Personal recommendations */}
        <div className="lg:col-span-2 space-y-6">
          {statusMsg && (
            <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-emerald-400 text-xs animate-pulse">
              {statusMsg}
            </div>
          )}

          {/* Epic 11 dynamic recommendations strip */}
          {recommendations.length > 0 && (
            <div className="rounded-2xl border border-white/5 bg-slate-900/40 p-5 space-y-3">
              <h3 className="font-bold text-white text-xs uppercase tracking-wider text-slate-400">Smart Personalization Recommendations</h3>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                {recommendations.map((r, idx) => (
                  <div
                    key={idx}
                    onClick={() => handleRecClick(r)}
                    className="p-3.5 bg-slate-950 border border-emerald-500/10 rounded-xl cursor-pointer hover:border-emerald-500/30 transition-all flex flex-col justify-between"
                  >
                    <div>
                      <span className="text-[9px] font-bold px-1.5 py-0.5 bg-emerald-500/20 text-emerald-300 rounded uppercase tracking-wider">
                        {r.type.replace("_", " ")}
                      </span>
                      <p className="text-[11px] text-slate-300 mt-2 font-medium leading-relaxed">{r.message}</p>
                    </div>
                    <span className="text-[9px] text-slate-500 mt-3 self-end">Score: {(r.score * 100).toFixed(0)}%</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Inbox main messages panel */}
          <div className="rounded-2xl border border-white/5 bg-slate-900/60 p-6 space-y-6">
            <div className="flex flex-col md:flex-row justify-between md:items-center gap-4">
              <div>
                <h2 className="text-lg font-bold text-white">Inbox folder: {activeFolder}</h2>
                <p className="text-xs text-slate-500 mt-0.5">{unreadsCount} unread system notifications</p>
              </div>

              {/* Advanced search */}
              <form onSubmit={handleSearch} className="flex gap-2">
                <input
                  type="text"
                  placeholder="Search keywords..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="bg-black/20 text-xs px-3 py-2 border border-white/10 rounded-xl outline-none text-slate-200 w-48 focus:border-white/20"
                />
                <button
                  type="submit"
                  className="px-3.5 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-white/10 rounded-xl text-xs font-semibold transition-all"
                >
                  Search
                </button>
              </form>
            </div>

            {loading ? (
              <div className="flex items-center justify-center h-48 text-slate-400 text-xs animate-pulse">
                Syncing Cricbook inbox center...
              </div>
            ) : notifications.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12 text-slate-500 space-y-2">
                <span className="text-3xl">📭</span>
                <p className="text-xs font-semibold">Your inbox folder is empty.</p>
              </div>
            ) : (
              <div className="space-y-3">
                {notifications.map((n) => {
                  const isUnread = !isNotificationRead(n) && !isNotificationArchived(n);
                  return (
                    <div
                      key={n.id}
                      onClick={(e) => {
                        const target = e.target as HTMLElement;
                        if (target.closest("button") || target.closest("a")) return;
                        if (isUnread) void handleRead(n.id);
                      }}
                      className={`p-4 rounded-xl border flex flex-col justify-between md:flex-row md:items-center gap-4 transition-all ${
                        isUnread
                          ? "cursor-pointer bg-emerald-500/5 border-emerald-500/20 shadow-md shadow-emerald-950/10 hover:border-emerald-500/40 hover:bg-emerald-500/10"
                          : "bg-black/10 border-white/5 opacity-85 hover:opacity-100"
                      }`}
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          {isUnread && (
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
                          )}
                          <h4 className="font-bold text-slate-200 text-sm">{n.title}</h4>
                          <span className="px-1.5 py-0.2 bg-slate-800 text-slate-400 rounded text-[9px] font-mono tracking-wide">
                            {n.channel}
                          </span>
                        </div>
                        <p className="text-xs text-slate-400 leading-relaxed">{n.message}</p>
                        <span className="text-[9px] text-slate-600 block pt-1">{n.createdAt ? new Date(n.createdAt).toLocaleTimeString() : "N/A"}</span>
                      </div>

                      <div className="flex items-center gap-2 self-end md:self-auto">
                        {n.deepLink && (
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              if (isUnread) void handleRead(n.id);
                              handleDeepLinkClick(n);
                            }}
                            className="px-2.5 py-1 bg-sky-500/10 hover:bg-sky-500/20 text-sky-400 border border-sky-500/20 text-[10px] font-bold rounded-lg transition-all"
                          >
                            Open Link
                          </button>
                        )}
                        {!isNotificationArchived(n) ? (
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              void handleArchive(n.id);
                            }}
                            className="px-2.5 py-1 bg-red-500/5 hover:bg-red-500/10 text-red-400 border border-red-500/10 text-[10px] font-bold rounded-lg transition-all"
                          >
                            Archive
                          </button>
                        ) : (
                          <span
                            className={`px-2.5 py-1 rounded-lg border text-[10px] font-bold ${
                              isNotificationRead(n)
                                ? "bg-emerald-500/10 border-emerald-500/20 text-emerald-300"
                                : "bg-amber-500/10 border-amber-500/20 text-amber-300"
                            }`}
                          >
                            {isNotificationRead(n) ? "Read" : "Unread"}
                          </span>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}

function FolderButton({ label, active, count, onClick }: { label: string; active: boolean; count?: number; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className={`w-full flex justify-between items-center px-4 py-2.5 rounded-xl text-xs font-semibold tracking-wide transition-all outline-none ${
        active ? "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20" : "text-slate-400 hover:bg-white/5 hover:text-white"
      }`}
    >
      <span>{label}</span>
      {count !== undefined && (
        <span className={`px-2 py-0.5 rounded-full text-[9px] font-bold ${active ? "bg-emerald-500/25 text-emerald-300" : "bg-slate-950 text-slate-400"}`}>
          {count}
        </span>
      )}
    </button>
  );
}
