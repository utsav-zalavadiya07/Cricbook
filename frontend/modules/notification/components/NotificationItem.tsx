"use client";

import Link from "next/link";
import { useState } from "react";
import { deleteNotification, markAsRead, trackMetric } from "../services/notificationService";
import { Notification } from "../types/notification.types";
import { isNotificationArchived, isNotificationRead } from "../utils/notificationState";

interface NotificationItemProps {
  notification: Notification;
  selected?: boolean;
  onSelect?: (id: number, selected: boolean) => void;
  onDelete: (id: number) => void;
  onRead: (id: number) => void;
}

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

const typeBg: Record<string, string> = {
  BOOKING: "bg-emerald-500/10 border-emerald-500/20 text-emerald-300",
  PAYMENT: "bg-violet-500/10 border-violet-500/20 text-violet-300",
  CANCELLATION: "bg-red-500/10 border-red-500/20 text-red-300",
  TEAM: "bg-cyan-500/10 border-cyan-500/20 text-cyan-300",
  MATCH: "bg-blue-500/10 border-blue-500/20 text-blue-300",
  TOURNAMENT: "bg-amber-500/10 border-amber-500/20 text-amber-300",
  REVIEW: "bg-fuchsia-500/10 border-fuchsia-500/20 text-fuchsia-300",
  SOCIAL: "bg-sky-500/10 border-sky-500/20 text-sky-300",
  MARKETING: "bg-lime-500/10 border-lime-500/20 text-lime-300",
  GENERAL: "bg-slate-500/10 border-slate-500/20 text-slate-300",
};

const priorityBg: Record<string, string> = {
  CRITICAL: "border-red-400/50 bg-red-500/15 text-red-200",
  HIGH: "border-amber-400/50 bg-amber-500/15 text-amber-200",
  NORMAL: "border-white/10 bg-white/5 text-slate-300",
  LOW: "border-slate-500/25 bg-slate-500/10 text-slate-400",
};

export default function NotificationItem({ notification, selected = false, onSelect, onDelete, onRead }: NotificationItemProps) {
  const [busy, setBusy] = useState<"read" | "delete" | null>(null);
  const isUnread = !isNotificationRead(notification);
  const isArchived = isNotificationArchived(notification);
  const type = (notification.category ?? notification.type ?? "GENERAL").toUpperCase();
  const priority = (notification.priority ?? "NORMAL").toUpperCase();

  const handleRead = async () => {
    if (!isUnread || busy) return;
    setBusy("read");
    try {
      await markAsRead(notification.id);
      onRead(notification.id);
    } finally {
      setBusy(null);
    }
  };

  const handleDelete = async () => {
    if (busy) return;
    setBusy("delete");
    try {
      await deleteNotification(notification.id);
      onDelete(notification.id);
    } finally {
      setBusy(null);
    }
  };

  const handleOpen = async () => {
    try {
      await trackMetric(notification.id, "click");
    } catch {
      // Do not block navigation because engagement tracking failed.
    }
  };

  return (
    <article
      className={`flex items-start gap-4 rounded-2xl border p-4 transition ${
        isUnread
          ? "border-emerald-400/30 bg-emerald-500/10 shadow-lg shadow-emerald-950/20"
          : "border-[var(--theme-border)] bg-[var(--theme-card)] opacity-80"
      }`}
    >
      {onSelect && (
        <label className="mt-3 inline-flex">
          <input
            type="checkbox"
            checked={selected}
            onChange={(event) => onSelect(notification.id, event.target.checked)}
            className="h-4 w-4 rounded border-white/20 bg-slate-900 text-emerald-500"
            aria-label={`Select notification ${notification.title}`}
          />
        </label>
      )}

      {isUnread && !isArchived ? (
        <button
          onClick={handleRead}
          className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl border text-xs font-black ${typeBg[type] ?? typeBg.GENERAL}`}
          aria-label="Mark notification as read"
        >
          {typeLabel[type] ?? typeLabel.GENERAL}
        </button>
      ) : (
        <div
          className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl border text-xs font-black ${typeBg[type] ?? typeBg.GENERAL}`}
          aria-label={isUnread ? "Notification unread" : "Notification read"}
        >
          {typeLabel[type] ?? typeLabel.GENERAL}
        </div>
      )}

      <div className="min-w-0 flex-1">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <p className={`break-words text-sm font-bold ${isUnread ? "theme-title" : "theme-muted"}`}>{notification.title}</p>
              {isUnread && <span className="rounded-full bg-emerald-500 px-2 py-0.5 text-[10px] font-black uppercase tracking-wide text-white">New</span>}
              <span className={`rounded-full border px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide ${priorityBg[priority] ?? priorityBg.NORMAL}`}>
                {priority}
              </span>
            </div>
            <p className={`mt-1 break-words text-sm leading-6 ${isUnread ? "theme-muted" : "theme-soft"}`}>{notification.message}</p>
            <div className="theme-soft mt-2 flex flex-wrap gap-2 text-xs">
              <span>{notification.channel ?? "EMAIL"}</span>
              <span>•</span>
              <span>{type}</span>
              <span>•</span>
              <span>
                {new Date(notification.createdAt).toLocaleString("en-IN", {
                  day: "numeric",
                  month: "short",
                  year: "numeric",
                  hour: "2-digit",
                  minute: "2-digit",
                })}
              </span>
              {notification.deliveryStatus === "FAILED" && <span className="text-red-300">Delivery failed</span>}
            </div>
          </div>

          <div className="flex shrink-0 flex-wrap items-center gap-2">
            {notification.deepLink && (
              <Link
                href={notification.deepLink}
                onClick={handleOpen}
                className="rounded-xl border border-blue-500/25 bg-blue-500/10 px-3 py-2 text-xs font-bold text-blue-300 transition hover:bg-blue-500/20"
              >
                Open
              </Link>
            )}
            {isArchived ? (
              <span
                className={`rounded-xl border px-3 py-2 text-xs font-bold ${
                  isUnread
                    ? "border-amber-500/25 bg-amber-500/10 text-amber-200"
                    : "border-emerald-500/25 bg-emerald-500/10 text-emerald-200"
                }`}
              >
                {isUnread ? "Unread" : "Read"}
              </span>
            ) : isUnread ? (
              <button
                onClick={handleRead}
                disabled={busy !== null}
                className="rounded-xl border border-emerald-500/25 bg-emerald-500/10 px-3 py-2 text-xs font-bold text-emerald-300 transition hover:bg-emerald-500/20 disabled:opacity-60"
              >
                {busy === "read" ? "Reading..." : "Mark read"}
              </button>
            ) : null}
            <button
              onClick={handleDelete}
              disabled={busy !== null}
              className="rounded-xl border border-red-500/25 bg-red-500/10 px-3 py-2 text-xs font-bold text-red-300 transition hover:bg-red-500/20 disabled:opacity-60"
            >
              {busy === "delete" ? "Deleting..." : "Delete"}
            </button>
          </div>
        </div>
      </div>

      {isUnread && <span className="mt-2 h-2.5 w-2.5 shrink-0 rounded-full bg-emerald-400 shadow shadow-emerald-400/50" />}
    </article>
  );
}
