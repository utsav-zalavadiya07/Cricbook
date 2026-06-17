"use client";

import { useState } from "react";
import { addReview } from "../services/reviewService";
import StarRating from "./StarRating";

interface ReviewFormProps {
  turfId: number;
  onSuccess: () => void;
}

const RATING_LABELS: Record<number, string> = {
  1: "Poor",
  2: "Below Average",
  3: "Average",
  4: "Good",
  5: "Excellent",
};

function apiErrorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { message?: string } } };
  return apiError.response?.data?.message ?? fallback;
}

export default function ReviewForm({ turfId, onSuccess }: ReviewFormProps) {
  const [rating, setRating] = useState(0);
  const [title, setTitle] = useState("");
  const [comment, setComment] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = async () => {
    if (!rating) {
      setError("Please select a star rating.");
      return;
    }
    if (comment.trim().length < 10) {
      setError("Please write at least 10 characters about your visit.");
      return;
    }

    setLoading(true);
    setError("");
    try {
      await addReview({ turfId, rating, title: title.trim() || undefined, comment: comment.trim() });
      setRating(0);
      setTitle("");
      setComment("");
      setSubmitted(true);
      onSuccess();
    } catch (error) {
      setError(apiErrorMessage(error, "You must have a completed booking to leave a review."));
    } finally {
      setLoading(false);
    }
  };

  if (submitted) {
    return (
      <div className="flex items-start gap-3 rounded-2xl border border-emerald-500/30 bg-emerald-500/10 p-5">
        <span className="mt-0.5 flex h-6 w-6 items-center justify-center rounded-full bg-emerald-500/15 text-xs font-bold text-emerald-700 dark:text-emerald-300">Y</span>
        <div>
          <p className="theme-title font-semibold">Review submitted!</p>
          <p className="theme-muted mt-0.5 text-sm">Thanks for your feedback. It helps others find great turfs.</p>
          <button onClick={() => setSubmitted(false)} className="mt-2 text-xs text-emerald-600 transition hover:text-emerald-500 dark:text-emerald-300">
            Write another review
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="theme-card space-y-4 rounded-2xl p-5">
      <h4 className="theme-title font-semibold">Write a Review</h4>

      {error && <div className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-500 dark:text-red-400">{error}</div>}

      <div className="space-y-1.5">
        <label className="theme-muted text-xs font-medium">Your Rating *</label>
        <div className="flex items-center gap-3">
          <StarRating rating={rating} setRating={setRating} size="lg" />
          {rating > 0 && <span className="text-sm font-medium text-amber-600 dark:text-amber-300">{RATING_LABELS[rating]}</span>}
        </div>
      </div>

      <div className="space-y-1.5">
        <label className="theme-muted text-xs font-medium">Review Title</label>
        <input
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          maxLength={140}
          placeholder="e.g. Clean pitch and strong lights"
          className="theme-input w-full rounded-xl px-4 py-3 text-sm transition placeholder:text-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
        />
      </div>

      <div className="space-y-1.5">
        <label className="theme-muted text-xs font-medium">Your Review *</label>
        <textarea
          rows={3}
          value={comment}
          onChange={(event) => setComment(event.target.value)}
          maxLength={1000}
          placeholder="How was the pitch quality? Was it well-maintained?"
          className="theme-input w-full resize-none rounded-xl px-4 py-3 text-sm transition placeholder:text-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
        />
        <p className="theme-soft text-right text-xs">{comment.length}/1000</p>
      </div>

      <button
        onClick={handleSubmit}
        disabled={loading || rating === 0}
        className="w-full rounded-xl bg-emerald-500 py-3 font-semibold text-white shadow-lg shadow-emerald-500/20 transition-all hover:-translate-y-0.5 hover:bg-emerald-400 disabled:cursor-not-allowed disabled:opacity-50"
      >
        {loading ? "Submitting..." : "Submit Review"}
      </button>

      <p className="theme-soft text-center text-xs">Only users with a completed booking can leave a review.</p>
    </div>
  );
}
