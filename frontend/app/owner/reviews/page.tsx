"use client";
import { TileInfo } from "@/components/ui";

import { useEffect, useMemo, useState } from "react";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import { getOwnerReviews } from "@/modules/owner/services/ownerService";
import { replyToReview } from "@/modules/review/services/reviewService";
import { Review } from "@/modules/review/types/review.types";

export default function OwnerReviewsPage() {
  const [reviews, setReviews] = useState<Review[]>([]);
  const [replyDrafts, setReplyDrafts] = useState<Record<number, string>>({});
  const [savingReplyId, setSavingReplyId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    getOwnerReviews()
      .then((res) => setReviews(res.data || []))
      .catch(() => {
        setReviews([]);
        setError("Could not load reviews right now.");
      })
      .finally(() => setLoading(false));
  }, []);

  const metrics = useMemo(() => {
    const published = reviews.filter((review) => (review.status || "PUBLISHED") === "PUBLISHED");
    const average = published.length
      ? (published.reduce((sum, review) => sum + Number(review.rating || 0), 0) / published.length).toFixed(1)
      : "0.0";
    const replied = published.filter((review) => review.ownerReply?.trim()).length;
    return {
      average,
      publishedCount: published.length,
      flaggedCount: reviews.filter((review) => review.status === "FLAGGED").length,
      responseRate: published.length ? Math.round((replied / published.length) * 100) : 0,
    };
  }, [reviews]);

  const handleReply = async (review: Review) => {
    const reply = (replyDrafts[review.id] ?? "").trim();
    if (reply.length < 2) {
      setError("Owner response must be at least 2 characters.");
      return;
    }
    setSavingReplyId(review.id);
    setError("");
    try {
      const res = await replyToReview(review.id, reply);
      setReviews((current) => current.map((item) => (item.id === review.id ? res.data : item)));
      setReplyDrafts((current) => ({ ...current, [review.id]: "" }));
    } catch {
      setError("Could not save owner response.");
    } finally {
      setSavingReplyId(null);
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <div>
            <h1 className="theme-title text-3xl font-bold">Reviews
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Reviews information" focusable={false}>Track average rating, recent reviews and turf-wise feedback.</TileInfo>
              </span></h1>
          </div>
        </div>

        {error && <p className="rounded-xl border border-red-500/30 bg-red-500/10 p-3 text-sm text-red-400">{error}</p>}

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <Metric title="Average Rating" value={metrics.average} />
          <Metric title="Published Reviews" value={metrics.publishedCount} />
          <Metric title="Response Rate" value={`${metrics.responseRate}%`} />
        </div>
        {metrics.flaggedCount > 0 && (
          <div className="theme-panel-soft rounded-2xl border border-amber-500/30 p-4 text-sm text-amber-300">
            {metrics.flaggedCount} review{metrics.flaggedCount !== 1 ? "s" : ""} need moderation attention.
          </div>
        )}

        <section className="theme-panel rounded-3xl p-6">
          {loading && <p className="theme-muted py-20 text-center">Loading reviews...</p>}
          {!loading && reviews.length === 0 && <p className="theme-card theme-muted rounded-2xl border-dashed p-10 text-center">No reviews yet.</p>}
          <div className="grid gap-3 md:grid-cols-2">
            {reviews.map((review) => (
              <article key={review.id} className="theme-panel-soft space-y-4 rounded-2xl p-4">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <p className="theme-title font-semibold">{review.turfName || `Turf #${review.turfId}`}</p>
                    <p className="theme-soft text-xs">{review.userName || "Player"} / {formatDate(review.createdAt)}</p>
                  </div>
                  <div className="flex flex-col items-end gap-1">
                    <span className="rounded-full bg-amber-500/10 px-3 py-1 text-xs font-semibold text-amber-400">{review.rating || 0}/5</span>
                    <span className={`rounded-full px-3 py-1 text-[10px] font-semibold uppercase tracking-wider ${statusClass(review.status)}`}>
                      {review.status || "Published"}
                    </span>
                  </div>
                </div>
                {review.title && <p className="theme-title text-sm font-semibold">{review.title}</p>}
                <p className="theme-muted mt-3 text-sm">{review.comment || "No comment provided."}</p>
                {review.ownerReply ? (
                  <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/10 p-3">
                    <p className="text-xs font-semibold uppercase tracking-wider text-emerald-500">Your response</p>
                    <p className="theme-muted mt-1 text-sm">{review.ownerReply}</p>
                  </div>
                ) : (
                  <div className="space-y-2">
                    <textarea
                      value={replyDrafts[review.id] ?? ""}
                      onChange={(event) => setReplyDrafts((current) => ({ ...current, [review.id]: event.target.value }))}
                      rows={2}
                      maxLength={800}
                      placeholder="Reply professionally to this player review..."
                      className="theme-input w-full resize-none rounded-xl px-4 py-3 text-sm focus:border-emerald-500 focus:outline-none"
                    />
                    <button
                      type="button"
                      onClick={() => handleReply(review)}
                      disabled={savingReplyId === review.id}
                      className="rounded-xl bg-emerald-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-emerald-400 disabled:opacity-60"
                    >
                      {savingReplyId === review.id ? "Saving..." : "Post response"}
                    </button>
                  </div>
                )}
              </article>
            ))}
          </div>
        </section>
      </div>
    </DashboardLayout>
  );
}

function formatDate(value?: string) {
  if (!value) return "No date";
  return new Date(value).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });
}

function statusClass(status?: string) {
  switch (status) {
    case "FLAGGED":
      return "bg-amber-500/10 text-amber-400";
    case "HIDDEN":
    case "DELETED":
      return "bg-red-500/10 text-red-400";
    default:
      return "bg-emerald-500/10 text-emerald-400";
  }
}

function Metric({ title, value }: { title: string; value: string | number }) {
  return (
    <div className="theme-panel rounded-2xl p-5">
      <p className="theme-soft text-xs font-semibold uppercase tracking-wider">{title}</p>
      <p className="theme-title mt-3 text-3xl font-bold">{value}</p>
    </div>
  );
}
