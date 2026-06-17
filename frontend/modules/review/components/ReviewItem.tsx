"use client";

import { useState } from "react";
import StarRating from "./StarRating";
import { Review } from "../types/review.types";
import { reportReview, voteReview } from "../services/reviewService";

interface ReviewItemProps {
  review: Review;
  onChanged?: () => void;
}

const getInitials = (name?: string) => {
  if (!name) return "U";
  return name.split(" ").map((word) => word[0]).join("").toUpperCase().slice(0, 2);
};

const avatarColors = [
  "bg-emerald-500/20 text-emerald-700 dark:text-emerald-300",
  "bg-violet-500/20 text-violet-700 dark:text-violet-300",
  "bg-blue-500/20 text-blue-700 dark:text-blue-300",
  "bg-amber-500/20 text-amber-700 dark:text-amber-300",
  "bg-rose-500/20 text-rose-700 dark:text-rose-300",
];

function apiErrorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { message?: string } } };
  return apiError.response?.data?.message ?? fallback;
}

export default function ReviewItem({ review, onChanged }: ReviewItemProps) {
  const [busyAction, setBusyAction] = useState<"helpful" | "report" | null>(null);
  const [message, setMessage] = useState("");
  const colorClass = avatarColors[review.id % avatarColors.length];
  const initials = getInitials(review.userName);
  const formattedDate = new Date(review.createdAt).toLocaleDateString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });

  const handleHelpful = async () => {
    setBusyAction("helpful");
    setMessage("");
    try {
      await voteReview(review.id, true);
      setMessage("Thanks. Your vote was saved.");
      onChanged?.();
    } catch (error) {
      setMessage(apiErrorMessage(error, "Could not save your vote."));
    } finally {
      setBusyAction(null);
    }
  };

  const handleReport = async () => {
    const reason = window.prompt("Why are you reporting this review?");
    if (!reason?.trim()) return;
    setBusyAction("report");
    setMessage("");
    try {
      await reportReview(review.id, reason.trim());
      setMessage("Report sent for moderation.");
      onChanged?.();
    } catch (error) {
      setMessage(apiErrorMessage(error, "Could not report this review."));
    } finally {
      setBusyAction(null);
    }
  };

  return (
    <div className="theme-card space-y-3 rounded-2xl p-5 transition-all hover:border-emerald-400/30">
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-xs font-bold ${colorClass}`}>
            {initials}
          </div>
          <div>
            <p className="theme-title text-sm font-medium leading-tight">{review.userName ?? "Anonymous"}</p>
            <div className="mt-1 flex flex-wrap items-center gap-2">
              <p className="theme-soft text-xs">{formattedDate}</p>
              {review.verifiedBooking && (
                <span className="rounded-full bg-emerald-500/10 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-emerald-500">
                  Verified booking
                </span>
              )}
            </div>
          </div>
        </div>
        <StarRating rating={review.rating} size="sm" readonly />
      </div>

      {review.title && <h4 className="theme-title text-sm font-semibold">{review.title}</h4>}
      {review.comment && <p className="theme-muted text-sm leading-relaxed">{review.comment}</p>}

      {review.ownerReply && (
        <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/10 p-3">
          <p className="text-xs font-semibold uppercase tracking-wider text-emerald-500">Owner response</p>
          <p className="theme-muted mt-1 text-sm">{review.ownerReply}</p>
        </div>
      )}

      <div className="flex flex-wrap items-center gap-2 border-t border-white/10 pt-3">
        <button
          type="button"
          onClick={handleHelpful}
          disabled={busyAction !== null}
          className="rounded-full border border-white/10 px-3 py-1.5 text-xs font-semibold text-slate-600 transition hover:border-emerald-500/40 hover:text-emerald-500 disabled:opacity-60 dark:text-slate-300"
        >
          Helpful {review.helpfulCount ? `(${review.helpfulCount})` : ""}
        </button>
        <button
          type="button"
          onClick={handleReport}
          disabled={busyAction !== null}
          className="rounded-full border border-white/10 px-3 py-1.5 text-xs font-semibold text-slate-500 transition hover:border-red-500/40 hover:text-red-400 disabled:opacity-60"
        >
          Report
        </button>
        {message && <span className="theme-soft text-xs">{message}</span>}
      </div>
    </div>
  );
}
