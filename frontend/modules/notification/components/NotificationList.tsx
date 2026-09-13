"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  deleteManyNotifications,
  getActiveAnnouncements,
  getNotificationSummary,
  getNotifications,
  markAllAsRead,
  markManyAsRead,
} from "../services/notificationService";
import { Notification, NotificationAnnouncement, NotificationSummary } from "../types/notification.types";
import { isNotificationRead } from "../utils/notificationState";
import CompactSelect from "@/components/ui/CompactSelect";
import NotificationItem from "./NotificationItem";

const folders = [
  { key: "ALL", label: "All" },
  { key: "UNREAD", label: "Unread" },
  { key: "READ", label: "Read" },
  { key: "ARCHIVE", label: "Archived" },
];

const emptySummary: NotificationSummary = {
  total: 0,
  unread: 0,
  read: 0,
  archived: 0,
  pending: 0,
  failed: 0,
  byChannel: {},
  byCategory: {},
};

function labelize(value: string) {
  return value
    .replace(/[_-]+/g, " ")
    .replace(/\s+/g, " ")
    .trim()
    .toLowerCase()
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function buildFilterOptions(values: Array<string | null | undefined>, allLabel: string, selectedValue: string) {
  const unique = new Map<string, string>();
  [...values, selectedValue]
    .map((value) => String(value || "").trim())
    .filter(Boolean)
    .forEach((value) => {
      const normalized = value.toUpperCase();
      if (!unique.has(normalized)) {
        unique.set(normalized, labelize(value));
      }
    });

  return [
    { value: "", label: allLabel },
    ...Array.from(unique.entries())
      .sort(([, left], [, right]) => left.localeCompare(right))
      .map(([value, label]) => ({ value, label })),
  ];
}

export default function NotificationList() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [summary, setSummary] = useState<NotificationSummary>(emptySummary);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [folder, setFolder] = useState("ALL");
  const [searchDraft, setSearchDraft] = useState("");
  const [search, setSearch] = useState("");
  const [channel, setChannel] = useState("");
  const [type, setType] = useState("");
  const [priority, setPriority] = useState("");
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [announcements, setAnnouncements] = useState<NotificationAnnouncement[]>([]);
  const [busy, setBusy] = useState<"mark-all" | "bulk-read" | "bulk-delete" | null>(null);

  const selectedCount = selectedIds.length;
  const unreadCount =
    summary.unread || notifications.filter((notification) => !isNotificationRead(notification) && !notification.archived).length;
  const sortedNotifications = useMemo(
    () => [...notifications].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()),
    [notifications]
  );
  const channelOptions = useMemo(
    () => buildFilterOptions([...Object.keys(summary.byChannel || {}), ...notifications.map((notification) => notification.channel)], "All channels", channel),
    [channel, notifications, summary.byChannel]
  );
  const categoryOptions = useMemo(
    () =>
      buildFilterOptions(
        [
          ...Object.keys(summary.byCategory || {}),
          ...notifications.map((notification) => notification.category || notification.type),
        ],
        "All categories",
        type
      ),
    [notifications, summary.byCategory, type]
  );
  const priorityOptions = useMemo(
    () => buildFilterOptions(notifications.map((notification) => notification.priority), "All priorities", priority),
    [notifications, priority]
  );

  const fetchData = useCallback(async () => {
    try {
      const [listResponse, summaryResponse] = await Promise.all([
        getNotifications({
          folder,
          search: search || undefined,
          channel: channel || undefined,
          type: type || undefined,
          priority: priority || undefined,
        }),
        getNotificationSummary(),
      ]);
      setNotifications(listResponse.data);
      setSummary(summaryResponse.data ?? emptySummary);
      setSelectedIds([]);
      setError("");
    } catch {
      setError("Could not load notifications. Please refresh or try again after login.");
    } finally {
      setLoading(false);
    }
  }, [channel, folder, priority, search, type]);

  useEffect(() => {
    let active = true;
    void getActiveAnnouncements()
      .then((response) => {
        if (active) setAnnouncements(response.data ?? []);
      })
      .catch(() => {
        if (active) setAnnouncements([]);
      });
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => void fetchData(), 0);
    const interval = window.setInterval(() => void fetchData(), 30000);
    return () => {
      window.clearTimeout(timer);
      window.clearInterval(interval);
    };
  }, [fetchData]);

  const refreshAfterAction = async () => {
    await fetchData();
  };

  const handleRead = (id: number) => {
    setNotifications((prev) =>
      prev.map((notification) => (notification.id === id ? { ...notification, status: "READ", read: true, readAt: new Date().toISOString() } : notification))
    );
    setSummary((prev) => ({
      ...prev,
      unread: Math.max(0, prev.unread - 1),
      read: prev.read + 1,
    }));
  };

  const handleDelete = (id: number) => {
    setNotifications((prev) => prev.filter((notification) => notification.id !== id));
    setSelectedIds((prev) => prev.filter((selectedId) => selectedId !== id));
  };

  const handleMarkAll = async () => {
    setBusy("mark-all");
    try {
      await markAllAsRead();
      setNotifications((prev) => prev.map((notification) => ({ ...notification, status: "READ", read: true, readAt: new Date().toISOString() })));
      setSummary((prev) => ({ ...prev, read: prev.read + prev.unread, unread: 0 }));
    } finally {
      setBusy(null);
    }
  };

  const handleBulkRead = async () => {
    if (selectedIds.length === 0) return;
    setBusy("bulk-read");
    try {
      await markManyAsRead(selectedIds);
      await refreshAfterAction();
    } finally {
      setBusy(null);
    }
  };

  const handleBulkDelete = async () => {
    if (selectedIds.length === 0) return;
    setBusy("bulk-delete");
    try {
      await deleteManyNotifications(selectedIds);
      await refreshAfterAction();
    } finally {
      setBusy(null);
    }
  };

  const toggleSelected = (id: number, selected: boolean) => {
    setSelectedIds((prev) => (selected ? [...new Set([...prev, id])] : prev.filter((selectedId) => selectedId !== id)));
  };

  const clearFilters = () => {
    setFolder("ALL");
    setSearchDraft("");
    setSearch("");
    setChannel("");
    setType("");
    setPriority("");
  };

  if (loading) {
    return (
      <div className="theme-panel flex h-40 items-center justify-center rounded-3xl">
        <div className="theme-muted flex items-center gap-3">
          <svg className="h-5 w-5 animate-spin text-emerald-400" viewBox="0 0 24 24" fill="none">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
          </svg>
          Loading notifications...
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {error && <div className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-400">{error}</div>}

      {announcements.length > 0 && (
        <div className="grid gap-2">
          {announcements.slice(0, 2).map((announcement) => (
            <div key={announcement.id} className="rounded-2xl border border-amber-400/20 bg-amber-500/10 px-4 py-3">
              <p className="text-xs font-black uppercase tracking-[0.2em] text-amber-200">{announcement.priority || "NOTICE"}</p>
              <p className="theme-title mt-1 text-sm font-bold">{announcement.title}</p>
              <p className="theme-muted mt-1 text-sm">{announcement.message}</p>
            </div>
          ))}
        </div>
      )}

      <div className="grid gap-3 md:grid-cols-5">
        {[
          ["Total", summary.total],
          ["Unread", summary.unread],
          ["Archived", summary.archived],
          ["Pending", summary.pending],
          ["Failed", summary.failed],
        ].map(([label, value]) => (
          <div key={label} className="theme-panel rounded-2xl p-4">
            <p className="theme-muted text-xs font-bold uppercase tracking-[0.2em]">{label}</p>
            <p className="theme-title mt-2 text-2xl font-black">{value}</p>
          </div>
        ))}
      </div>

      <div className="theme-panel space-y-4 rounded-2xl p-4">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p className="theme-title text-sm font-bold">{notifications.length} notifications in this view</p>
            <p className="theme-muted text-sm">
              {unreadCount > 0 ? `${unreadCount} unread across your notification center.` : "All visible notifications are read."}
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            {folders.map((item) => (
              <button
                key={item.key}
                onClick={() => setFolder(item.key)}
                className={`rounded-xl border px-3 py-2 text-xs font-bold transition ${
                  folder === item.key
                    ? "border-emerald-400/40 bg-emerald-500/15 text-emerald-200"
                    : "border-white/10 bg-white/5 text-slate-300 hover:bg-white/10"
                }`}
              >
                {item.label}
              </button>
            ))}
          </div>
        </div>

        <div className="grid gap-3 lg:grid-cols-[1fr_auto_auto_auto]">
          <input
            value={searchDraft}
            onChange={(event) => setSearchDraft(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter") setSearch(searchDraft.trim());
            }}
            placeholder="Search by title, content, category, or priority"
            className="rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm text-white outline-none transition placeholder:text-slate-500 focus:border-emerald-400/50"
          />
          <button
            onClick={() => setSearch(searchDraft.trim())}
            className="rounded-xl bg-emerald-500 px-4 py-2 text-sm font-bold text-white transition hover:bg-emerald-400"
          >
            Search
          </button>
          <button
            onClick={clearFilters}
            className="rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm font-bold text-slate-300 transition hover:bg-white/10"
          >
            Clear
          </button>
          <button
            onClick={handleMarkAll}
            disabled={unreadCount === 0 || busy !== null}
            className="rounded-xl border border-emerald-500/25 bg-emerald-500/10 px-4 py-2 text-sm font-bold text-emerald-300 transition hover:bg-emerald-500/20 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {busy === "mark-all" ? "Marking..." : "Mark all read"}
          </button>
        </div>

        <div className="grid gap-3 md:grid-cols-3">
          <CompactSelect value={channel} onChange={setChannel} options={channelOptions} ariaLabel="Filter notifications by channel" />
          <CompactSelect value={type} onChange={setType} options={categoryOptions} ariaLabel="Filter notifications by category" />
          <CompactSelect value={priority} onChange={setPriority} options={priorityOptions} ariaLabel="Filter notifications by priority" />
        </div>

        {selectedCount > 0 && (
          <div className="flex flex-wrap items-center gap-2 rounded-xl border border-white/10 bg-slate-950/40 p-3 text-sm">
            <span className="theme-muted mr-2">{selectedCount} selected</span>
            <button onClick={handleBulkRead} disabled={busy !== null} className="rounded-lg bg-emerald-500/15 px-3 py-1.5 font-bold text-emerald-200 disabled:opacity-50">
              {busy === "bulk-read" ? "Updating..." : "Mark read"}
            </button>
            <button onClick={handleBulkDelete} disabled={busy !== null} className="rounded-lg bg-red-500/15 px-3 py-1.5 font-bold text-red-200 disabled:opacity-50">
              {busy === "bulk-delete" ? "Deleting..." : "Delete"}
            </button>
          </div>
        )}
      </div>

      {notifications.length === 0 && (
        <div className="theme-card theme-muted flex h-52 flex-col items-center justify-center rounded-2xl border-dashed">
          <span className="mb-3 text-3xl">🔔</span>
          <p className="theme-title font-medium">No notifications in this view.</p>
          <p className="theme-muted mt-1 text-sm">Try clearing filters or wait for new platform activity.</p>
        </div>
      )}

      <div className="space-y-2">
        {sortedNotifications.map((notification) => (
          <NotificationItem
            key={notification.id}
            notification={notification}
            selected={selectedIds.includes(notification.id)}
            onSelect={toggleSelected}
            onDelete={handleDelete}
            onRead={handleRead}
          />
        ))}
      </div>
    </div>
  );
}
