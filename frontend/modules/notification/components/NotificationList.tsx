"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  getActiveAnnouncements,
  getNotificationSummary,
  getNotifications,
  markAllAsRead,
} from "../services/notificationService";
import { Notification, NotificationAnnouncement, NotificationSummary } from "../types/notification.types";
import { isNotificationArchived, isNotificationRead } from "../utils/notificationState";
import CompactSelect from "@/components/ui/CompactSelect";
import { TileInfo } from "@/components/ui";
import { usePortal } from "@/hooks/usePortal";
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
  const { isOwner } = usePortal();
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
  const [announcements, setAnnouncements] = useState<NotificationAnnouncement[]>([]);
  const [busy, setBusy] = useState<"mark-all" | null>(null);
  const [dismissedAnnouncements, setDismissedAnnouncements] = useState<number[]>(() => {
    if (typeof window !== "undefined") {
      try {
        const stored = localStorage.getItem("cricbook_dismissed_announcements");
        return stored ? (JSON.parse(stored) as number[]) : [];
      } catch {
        return [];
      }
    }
    return [];
  });

  const handleDismissAnnouncement = (id: number) => {
    setDismissedAnnouncements((prev) => {
      const next = [...new Set([...prev, id])];
      try {
        localStorage.setItem("cricbook_dismissed_announcements", JSON.stringify(next));
      } catch {
        // Ignore localStorage error
      }
      return next;
    });
  };

  const visibleAnnouncements = announcements.filter((a) => !dismissedAnnouncements.includes(a.id));

  const unreadCount =
    summary.unread || notifications.filter((notification) => !isNotificationRead(notification) && !isNotificationArchived(notification)).length;
  const sortedNotifications = useMemo(
    () => [...notifications].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()),
    [notifications]
  );

  const displayedNotifications = useMemo(() => {
    const query = searchDraft.trim().toLowerCase();
    return sortedNotifications.filter((n) => {
      if (channel && (n.channel || "EMAIL").toUpperCase() !== channel.toUpperCase()) {
        return false;
      }
      if (type && (n.category || n.type || "GENERAL").toUpperCase() !== type.toUpperCase()) {
        return false;
      }
      if (priority && (n.priority || "NORMAL").toUpperCase() !== priority.toUpperCase()) {
        return false;
      }
      if (!query) return true;
      const title = (n.title || "").toLowerCase();
      const message = (n.message || "").toLowerCase();
      const cat = (n.category || n.type || "").toLowerCase();
      const prio = (n.priority || "").toLowerCase();
      const chan = (n.channel || "").toLowerCase();
      const dateStr = new Date(n.createdAt)
        .toLocaleString("en-IN", {
          day: "numeric",
          month: "short",
          year: "numeric",
          hour: "2-digit",
          minute: "2-digit",
        })
        .toLowerCase();
      return (
        title.includes(query) ||
        message.includes(query) ||
        cat.includes(query) ||
        prio.includes(query) ||
        chan.includes(query) ||
        dateStr.includes(query)
      );
    });
  }, [searchDraft, sortedNotifications, channel, type, priority]);
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
          folder: folder === "ALL" ? "ALL_WITH_ARCHIVE" : folder,
          search: search || undefined,
          channel: channel || undefined,
          type: type || undefined,
          priority: priority || undefined,
        }),
        getNotificationSummary(),
      ]);
      setNotifications(listResponse.data);
      setSummary(summaryResponse.data ?? emptySummary);
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

  const getFolderCount = (key: string) => {
    switch (key) {
      case "ALL":
        return summary.total || (summary.unread + summary.read + summary.archived);
      case "UNREAD":
        return summary.unread;
      case "READ":
        return summary.read;
      case "ARCHIVE":
        return summary.archived;
      default:
        return 0;
    }
  };

  const handleRead = (id: number) => {
    const target = notifications.find((n) => n.id === id);
    const isTargetArchived = target ? isNotificationArchived(target) : false;
    const wasUnread = target ? !isNotificationRead(target) : false;
    setNotifications((prev) =>
      prev.map((notification) => (notification.id === id ? { ...notification, status: "READ", read: true, readAt: new Date().toISOString() } : notification))
    );
    if (wasUnread && !isTargetArchived) {
      setSummary((prev) => ({
        ...prev,
        unread: Math.max(0, prev.unread - 1),
        read: prev.read + 1,
      }));
    }
    window.dispatchEvent(new Event("notifications-updated"));
  };

  const handleDelete = (id: number) => {
    const target = notifications.find((n) => n.id === id);
    const wasUnread = target ? !isNotificationRead(target) : false;
    const isTargetArchived = target ? isNotificationArchived(target) : false;
    setNotifications((prev) => prev.filter((notification) => notification.id !== id));
    setSummary((prev) => ({
      ...prev,
      total: Math.max(0, prev.total - 1),
      unread: wasUnread && !isTargetArchived ? Math.max(0, prev.unread - 1) : prev.unread,
      read: !wasUnread && !isTargetArchived ? Math.max(0, prev.read - 1) : prev.read,
      archived: isTargetArchived ? Math.max(0, prev.archived - 1) : prev.archived,
    }));
    window.dispatchEvent(new Event("notifications-updated"));
  };

  const handleMarkAll = async () => {
    setBusy("mark-all");
    try {
      await markAllAsRead();
      const nowIso = new Date().toISOString();
      setNotifications((prev) =>
        prev.map((notification) => {
          const archived = isNotificationArchived(notification);
          return {
            ...notification,
            status: archived ? "ARCHIVED" : "READ",
            read: true,
            readAt: notification.readAt || nowIso,
          };
        })
      );
      setSummary((prev) => ({ ...prev, read: prev.read + prev.unread, unread: 0 }));
      window.dispatchEvent(new Event("notifications-updated"));
    } finally {
      setBusy(null);
    }
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
          <svg className={`h-5 w-5 animate-spin ${isOwner ? "text-sky-400" : "text-emerald-400"}`} viewBox="0 0 24 24" fill="none">
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
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="theme-title text-2xl font-black tracking-tight sm:text-3xl">
          Notifications
          <span className="inline-flex align-middle ml-3 mb-1">
            <TileInfo label="Notifications information" focusable={false}>Latest updates first.</TileInfo>
          </span>
        </h1>

        <button
          onClick={handleMarkAll}
          disabled={unreadCount === 0 || busy !== null}
          className={`notification-btn-markread inline-flex items-center justify-center rounded-xl border px-4 py-2 text-xs font-bold transition disabled:cursor-not-allowed disabled:opacity-50 ${
            isOwner
              ? "border-sky-500/25 bg-sky-500/10 text-sky-400 hover:bg-sky-500/20"
              : "border-emerald-500/25 bg-emerald-500/10 text-emerald-300 hover:bg-emerald-500/20"
          }`}
        >
          {busy === "mark-all" ? "Marking..." : "Mark all read"}
        </button>
      </div>

      {error && <div className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-400">{error}</div>}

      {visibleAnnouncements.length > 0 && (
        <div className="grid gap-2">
          {visibleAnnouncements.slice(0, 2).map((announcement) => (
            <div key={announcement.id} className="relative rounded-2xl border border-amber-400/20 bg-amber-500/10 p-4 pr-12 transition animate-in fade-in">
              <button
                onClick={() => handleDismissAnnouncement(announcement.id)}
                className="absolute top-3.5 right-3.5 rounded-lg p-1.5 text-amber-300/60 hover:text-amber-100 hover:bg-amber-400/15 transition"
                aria-label="Dismiss announcement"
              >
                <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
              <p className="text-xs font-black uppercase tracking-[0.2em] text-amber-200">{announcement.priority || "NOTICE"}</p>
              <p className="theme-title mt-1 text-sm font-bold">{announcement.title}</p>
              <p className="theme-muted mt-1 text-sm">{announcement.message}</p>
            </div>
          ))}
        </div>
      )}

      <div className="theme-panel relative z-20 space-y-4 rounded-2xl p-4">
        <div className="flex flex-wrap items-center gap-2">
          {folders.map((item) => {
            const count = getFolderCount(item.key);
            const isSelected = folder === item.key;
            const isUnreadTab = item.key === "UNREAD";
            return (
              <button
                key={item.key}
                onClick={() => setFolder(item.key)}
                className={`inline-flex items-center gap-2 rounded-xl px-3.5 py-2 text-xs font-bold transition-all ${
                  isSelected
                    ? isOwner
                      ? "bg-sky-500 text-white shadow-md shadow-sky-500/20"
                      : "bg-emerald-500 text-white shadow-md shadow-emerald-500/20"
                    : "notification-folder-inactive border border-white/10 bg-white/5 text-slate-300 hover:bg-white/10 hover:text-white"
                }`}
              >
                <span>{item.label}</span>
                <span
                  className={`rounded-full px-2 py-0.5 text-[10px] font-black transition-colors ${
                    isSelected
                      ? "bg-white/20 text-white"
                      : isUnreadTab && count > 0
                      ? isOwner
                        ? "bg-sky-500 text-white"
                        : "bg-emerald-500 text-white"
                      : "notification-count-pill border border-white/10 bg-white/10 text-slate-300"
                  }`}
                >
                  {count}
                </span>
              </button>
            );
          })}
        </div>

        <div className="flex flex-col gap-2.5 xl:flex-row xl:items-center">
          <div className="relative min-w-0 flex-1">
            <input
              value={searchDraft}
              onChange={(event) => {
                const val = event.target.value;
                setSearchDraft(val);
                if (!val.trim()) {
                  setSearch("");
                }
              }}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  setSearch(searchDraft.trim());
                  void fetchData();
                }
              }}
              placeholder="Search by title, content, category, or priority"
              className={`theme-input w-full rounded-xl pl-4 pr-9 py-2 text-sm outline-none transition placeholder:text-[var(--theme-soft)] ${
                isOwner
                  ? "focus:border-sky-500/60 focus:ring-1 focus:ring-sky-500/40"
                  : "focus:border-emerald-500/60 focus:ring-1 focus:ring-emerald-500/40"
              }`}
            />
            {searchDraft && (
              <button
                type="button"
                onClick={() => {
                  setSearchDraft("");
                  setSearch("");
                }}
                className="theme-soft hover:theme-text absolute right-2.5 top-1/2 -translate-y-1/2 p-1 transition"
                aria-label="Clear search input"
              >
                <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            )}
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <CompactSelect
              value={channel}
              onChange={setChannel}
              options={channelOptions}
              ariaLabel="Filter notifications by channel"
              className="w-full sm:w-36 md:w-40"
            />
            <CompactSelect
              value={type}
              onChange={setType}
              options={categoryOptions}
              ariaLabel="Filter notifications by category"
              className="w-full sm:w-36 md:w-40"
            />
            <CompactSelect
              value={priority}
              onChange={setPriority}
              options={priorityOptions}
              ariaLabel="Filter notifications by priority"
              className="w-full sm:w-36 md:w-40"
            />
            <button
              onClick={() => {
                setSearch(searchDraft.trim());
                void fetchData();
              }}
              className={`rounded-xl px-4 py-2 text-sm font-bold text-white shadow-sm transition active:scale-95 ${
                isOwner
                  ? "bg-sky-500 hover:bg-sky-400"
                  : "bg-emerald-500 hover:bg-emerald-600"
              }`}
            >
              Search
            </button>
            <button
              onClick={clearFilters}
              className="rounded-xl border border-[var(--theme-border)] bg-[var(--theme-panel-soft)] px-4 py-2 text-sm font-bold text-[var(--theme-muted)] transition hover:bg-[var(--theme-card)] hover:text-[var(--theme-text)]"
            >
              Clear
            </button>
          </div>
        </div>
      </div>

      {displayedNotifications.length === 0 && (
        <div className="theme-card theme-muted flex h-52 flex-col items-center justify-center rounded-2xl border-dashed">
          <span className="mb-3 text-3xl">🔍</span>
          <p className="theme-title font-medium">No notifications found.</p>
          <p className="theme-muted mt-1 text-sm">
            {searchDraft || channel || type || priority
              ? "No notifications match your search or active filters."
              : "Try clearing filters or wait for new platform activity."}
          </p>
          {(searchDraft || channel || type || priority) && (
            <button
              onClick={clearFilters}
              className="mt-3 rounded-xl border border-[var(--theme-border)] bg-[var(--theme-panel-soft)] px-4 py-2 text-xs font-bold text-[var(--theme-text)] transition hover:bg-[var(--theme-card)]"
            >
              Clear filters
            </button>
          )}
        </div>
      )}

      <div className="space-y-2">
        {displayedNotifications.map((notification) => (
          <NotificationItem
            key={notification.id}
            notification={notification}
            onDelete={handleDelete}
            onRead={handleRead}
          />
        ))}
      </div>
    </div>
  );
}
