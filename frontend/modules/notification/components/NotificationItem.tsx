"use client";

import Link from "next/link";
import { useState } from "react";
import { deleteNotification, markAsRead, trackMetric } from "../services/notificationService";
import { Notification } from "../types/notification.types";
import { isNotificationArchived, isNotificationRead } from "../utils/notificationState";
import { usePortal } from "@/hooks/usePortal";

interface NotificationItemProps {
  notification: Notification;
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
  GENERAL: "bg-slate-500/10 border-slate-500/20 text-slate-300 notification-type-general",
};

const priorityBg: Record<string, string> = {
  CRITICAL: "border-red-400/50 bg-red-500/15 text-red-200",
  HIGH: "border-amber-400/50 bg-amber-500/15 text-amber-200",
  NORMAL: "border-white/10 bg-white/5 text-slate-300 notification-badge-normal",
  LOW: "border-slate-500/25 bg-slate-500/10 text-slate-400",
};

export default function NotificationItem({ notification, onDelete, onRead }: NotificationItemProps) {
  const { isOwner } = usePortal();
  const [busy, setBusy] = useState<"read" | "delete" | null>(null);
  const isUnread = !isNotificationRead(notification);
  const isArchived = isNotificationArchived(notification);
  const isRecent = notification.createdAt
    ? Date.now() - new Date(notification.createdAt).getTime() < 24 * 60 * 60 * 1000
    : false;
  const isNew = isUnread && !isArchived && isRecent;
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

  const handleCardClick = (e: React.MouseEvent) => {
    const target = e.target as HTMLElement;
    // Don't mark as read if clicking on buttons or links
    if (target.closest("button") || target.closest("a")) {
      return;
    }
    if (isUnread && !isArchived) {
      void handleRead();
    }
  };

  const handleOpen = async (e: React.MouseEvent) => {
    e.stopPropagation();
    if (isUnread && !isArchived) {
      void handleRead();
    }
    try {
      await trackMetric(notification.id, "click");
    } catch {
      // Do not block navigation because engagement tracking failed.
    }
  };

  return (
    <article
      onClick={handleCardClick}
      className={`flex items-start gap-4 rounded-2xl border p-4 transition-all duration-200 ${
        isUnread && !isArchived
          ? isOwner
            ? "cursor-pointer border-sky-400/30 bg-sky-500/10 shadow-lg shadow-sky-950/20 hover:border-sky-400/50 hover:bg-sky-500/[0.14]"
            : "cursor-pointer border-emerald-400/30 bg-emerald-500/10 shadow-lg shadow-emerald-950/20 hover:border-emerald-400/50 hover:bg-emerald-500/[0.14]"
          : "border-[var(--theme-border)] bg-[var(--theme-card)] opacity-80 hover:opacity-100"
      }`}
    >
      <div className="shrink-0">
        <div
          className={`flex h-11 w-11 items-center justify-center rounded-xl border text-xs font-black transition ${
            type === "BOOKING"
              ? isOwner
                ? "bg-sky-500/10 border-sky-500/20 text-sky-300"
                : "bg-emerald-500/10 border-emerald-500/20 text-emerald-300"
              : typeBg[type] ?? typeBg.GENERAL
          }`}
          aria-label={isUnread ? "Notification unread" : "Notification read"}
        >
          {typeLabel[type] ?? typeLabel.GENERAL}
        </div>
      </div>

      <div className="min-w-0 flex-1">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <p className={`break-words text-sm font-bold ${isUnread && !isArchived ? "theme-title" : "theme-muted"}`}>{notification.title}</p>
              {isNew && (
                <span
                  className={`rounded-full px-2 py-0.5 text-[10px] font-black uppercase tracking-wide text-white ${
                    isOwner ? "bg-sky-500" : "bg-emerald-500"
                  }`}
                >
                  New
                </span>
              )}
              <span className={`rounded-full border px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide ${priorityBg[priority] ?? priorityBg.NORMAL}`}>
                {priority}
              </span>
              {isArchived && (
                <span className="notification-badge-archived rounded-full border border-slate-500/30 bg-slate-500/15 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide text-slate-400">
                  Archived
                </span>
              )}
            </div>
            <p className={`mt-1 break-words text-sm leading-6 ${isUnread && !isArchived ? "theme-muted" : "theme-soft"}`}>{notification.message}</p>
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
              {notification.deliveryStatus === "FAILED" && <span className="text-red-400">Delivery failed</span>}
            </div>
          </div>

          <div className="flex shrink-0 flex-wrap items-center gap-2">
            {notification.deepLink && (
              <Link
                href={notification.deepLink}
                onClick={handleOpen}
                className="notification-link-open rounded-xl border border-blue-500/25 bg-blue-500/10 px-3 py-2 text-xs font-bold text-blue-300 transition hover:bg-blue-500/20"
              >
                Open
              </Link>
            )}
            <button
              onClick={(e) => {
                e.stopPropagation();
                void handleDelete();
              }}
              disabled={busy !== null}
              title="Delete notification"
              aria-label="Delete notification"
              className="notification-btn-delete flex h-9 w-9 items-center justify-center rounded-xl border border-red-500/20 bg-red-500/10 text-red-400 transition hover:border-red-500/40 hover:bg-red-500/20 hover:text-red-300 disabled:opacity-50"
            >
              {busy === "delete" ? (
                <svg className="h-4 w-4 animate-spin text-red-400" viewBox="0 0 24 24" fill="none">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
                </svg>
              ) : (
                <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.75}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0" />
                </svg>
              )}
            </button>
          </div>
        </div>
      </div>
    </article>
  );
}
