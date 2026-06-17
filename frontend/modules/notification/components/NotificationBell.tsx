"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { getNotifications, markAllAsRead, markAsRead } from "../services/notificationService";
import { Notification } from "../types/notification.types";

const typeLabel: Record<string, string> = {
  BOOKING: "BK",
  PAYMENT: "PY",
  CANCELLATION: "CA",
  TEAM: "TM",
  MATCH: "MT",
  TOURNAMENT: "TR",
  REVIEW: "RV",
  SOCIAL: "SC",
  MARKETING: "MK",
  GENERAL: "GN",
};

const isNotificationRead = (notification: Notification) =>
  Boolean(notification.read) || notification.status === "READ";

export default function NotificationBell({ active = false, activeClass = "theme-nav-link-active" }: { active?: boolean; activeClass?: string }) {
  const router = useRouter();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [open, setOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const unread = notifications.filter((n) => !isNotificationRead(n));
  const selected = active || open;

  const fetchNotifications = async () => {
    try {
      const res = await getNotifications();
      setNotifications(res.data);
    } catch {
      // Auth can still be bootstrapping when the shell renders.
    }
  };

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void fetchNotifications();
    }, 0);
    const interval = setInterval(fetchNotifications, 30000);
    return () => {
      window.clearTimeout(timer);
      clearInterval(interval);
    };
  }, []);

  useEffect(() => {
    const handler = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  const handleMarkRead = async (notification: Notification) => {
    if (!isNotificationRead(notification)) {
      await markAsRead(notification.id);
      setNotifications((prev) => prev.map((item) => (item.id === notification.id ? { ...item, status: "READ", read: true } : item)));
    }
    router.push(notification.deepLink || "/notifications");
    setOpen(false);
  };

  const handleMarkAllRead = async () => {
    await markAllAsRead();
    setNotifications((prev) => prev.map((item) => ({ ...item, status: "READ", read: true })));
  };

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setOpen((current) => !current)}
        className={`relative inline-flex items-center gap-2 rounded-xl p-2 transition hover:bg-white/5 hover:text-current ${
          selected ? `${activeClass} px-3 ring-2 ring-emerald-400/30` : "theme-muted"
        }`}
        aria-label="Notifications"
      >
        <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>
        {selected && <span className="hidden text-sm font-semibold xl:inline">Notifications</span>}
        {unread.length > 0 && (
          <span className="absolute -right-0.5 -top-0.5 flex h-[18px] min-w-[18px] items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white shadow">
            {unread.length > 9 ? "9+" : unread.length}
          </span>
        )}
      </button>

      {open && (
        <div className="theme-panel-strong absolute right-0 top-11 z-50 w-80 overflow-hidden rounded-2xl">
          <div className="flex items-center justify-between border-b border-white/10 px-4 py-3">
            <p className="theme-title text-sm font-semibold">Notifications</p>
            {unread.length > 0 && (
              <button onClick={handleMarkAllRead} className="text-xs text-emerald-600 transition hover:text-emerald-500 dark:text-emerald-300">
                Mark all read
              </button>
            )}
          </div>

          <div className="max-h-72 divide-y divide-white/10 overflow-y-auto">
            {notifications.length === 0 ? (
              <div className="theme-muted flex flex-col items-center justify-center py-8 text-sm">
                No notifications yet
              </div>
            ) : (
              notifications.slice(0, 6).map((notification) => (
                <button
                  key={notification.id}
                  onClick={() => handleMarkRead(notification)}
                  className={`flex w-full items-start gap-3 px-4 py-3 text-left transition hover:bg-emerald-500/10 ${
                    !isNotificationRead(notification) ? "bg-emerald-500/5" : ""
                  }`}
                >
                  <span className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-emerald-500/10 text-[10px] font-black text-emerald-700 dark:text-emerald-300">
                    {typeLabel[(notification.category ?? notification.type ?? "GENERAL").toUpperCase()] ?? typeLabel.GENERAL}
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className={`text-sm leading-tight ${!isNotificationRead(notification) ? "theme-title font-semibold" : "theme-muted"}`}>
                      {notification.title}
                    </p>
                    <p className="theme-soft mt-0.5 truncate text-xs">{notification.message}</p>
                    <p className="theme-soft mt-1 text-xs">
                      {new Date(notification.createdAt).toLocaleString("en-IN", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" })}
                    </p>
                  </div>
                  {!isNotificationRead(notification) && <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-emerald-500" />}
                </button>
              ))
            )}
          </div>

          {notifications.length > 0 && (
            <div className="border-t border-white/10 px-4 py-2.5">
              <button
                onClick={() => {
                  router.push("/notifications");
                  setOpen(false);
                }}
                className="w-full text-center text-xs text-emerald-600 transition hover:text-emerald-500 dark:text-emerald-300"
              >
                View all notifications
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
