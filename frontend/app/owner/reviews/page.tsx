"use client";

import { useEffect, useMemo, useState } from "react";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import { getOwnerReviews } from "@/modules/owner/services/ownerService";
import { replyToReview } from "@/modules/review/services/reviewService";
import { Review } from "@/modules/review/types/review.types";
import { TileInfo } from "@/components/ui";

const QUICK_REPLIES = [
  "Thank you for the fantastic feedback! Look forward to hosting you again soon! 🏏",
  "Thanks for playing at our turf! Glad you enjoyed the outfield and lighting!",
  "Thank you for visiting! We appreciate the note on parking and are arranging extra space.",
  "We truly appreciate your review and support! See you for the next match!",
];

export default function OwnerReviewsPage() {
  const [reviews, setReviews] = useState<Review[]>([]);
  const [replyDrafts, setReplyDrafts] = useState<Record<number, string>>({});
  const [activeReplyId, setActiveReplyId] = useState<number | null>(null);
  const [savingReplyId, setSavingReplyId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  // Filters and display state
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedStar, setSelectedStar] = useState<number | null>(null);
  const [statusFilter, setStatusFilter] = useState<"ALL" | "NEEDS_REPLY" | "REPLIED">("ALL");
  const [selectedTurf, setSelectedTurf] = useState<string>("ALL");
  const [sortBy, setSortBy] = useState<"NEWEST" | "OLDEST" | "RATING_HIGH" | "RATING_LOW" | "NEEDS_REPLY">("NEWEST");
  const [viewMode, setViewMode] = useState<"TABLE" | "GRID">("TABLE");

  useEffect(() => {
    getOwnerReviews()
      .then((res) => setReviews(res.data || []))
      .catch(() => {
        setReviews([]);
        setError("Could not load reviews right now. Please try again.");
      })
      .finally(() => setLoading(false));
  }, []);

  // Filter out soft-deleted / hidden reviews
  const activeReviews = useMemo(() => {
    return reviews.filter((r) => r.status !== "HIDDEN" && r.status !== "DELETED");
  }, [reviews]);

  // Overall statistics & metrics
  const stats = useMemo(() => {
    const total = activeReviews.length;
    const totalRating = activeReviews.reduce((sum, r) => sum + Number(r.rating || 0), 0);
    const average = total > 0 ? (totalRating / total).toFixed(1) : "0.0";
    const replied = activeReviews.filter((r) => Boolean(r.ownerReply?.trim())).length;
    const needsReply = total - replied;
    const responseRate = total > 0 ? Math.round((replied / total) * 100) : 0;

    // Distribution breakdown
    const distribution: Record<number, number> = { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 };
    for (const r of activeReviews) {
      const star = Math.max(1, Math.min(5, Math.round(Number(r.rating) || 5)));
      distribution[star] = (distribution[star] || 0) + 1;
    }

    return {
      average,
      total,
      replied,
      needsReply,
      responseRate,
      distribution,
      flaggedCount: reviews.filter((r) => r.status === "FLAGGED").length,
    };
  }, [activeReviews, reviews]);

  // Unique turf names for filter
  const turfNames = useMemo(() => {
    const names = new Set<string>();
    for (const r of activeReviews) {
      if (r.turfName) names.add(r.turfName);
    }
    return Array.from(names);
  }, [activeReviews]);

  // Filtered & sorted reviews
  const displayedReviews = useMemo(() => {
    let result = activeReviews.filter((r) => {
      // Star filter
      if (selectedStar !== null && Math.round(Number(r.rating) || 0) !== selectedStar) {
        return false;
      }

      // Response status filter
      if (statusFilter === "NEEDS_REPLY" && r.ownerReply?.trim()) {
        return false;
      }
      if (statusFilter === "REPLIED" && !r.ownerReply?.trim()) {
        return false;
      }

      // Turf filter
      if (selectedTurf !== "ALL" && r.turfName !== selectedTurf) {
        return false;
      }

      // Search query
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchesPlayer = (r.userName || "").toLowerCase().includes(q);
        const matchesTurf = (r.turfName || "").toLowerCase().includes(q);
        const matchesComment = (r.comment || "").toLowerCase().includes(q);
        const matchesTitle = (r.title || "").toLowerCase().includes(q);
        if (!matchesPlayer && !matchesTurf && !matchesComment && !matchesTitle) {
          return false;
        }
      }

      return true;
    });

    // Sorting
    result = [...result].sort((a, b) => {
      if (sortBy === "NEWEST") {
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      }
      if (sortBy === "OLDEST") {
        return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
      }
      if (sortBy === "RATING_HIGH") {
        return (Number(b.rating) || 0) - (Number(a.rating) || 0);
      }
      if (sortBy === "RATING_LOW") {
        return (Number(a.rating) || 0) - (Number(b.rating) || 0);
      }
      if (sortBy === "NEEDS_REPLY") {
        const aReplied = Boolean(a.ownerReply?.trim());
        const bReplied = Boolean(b.ownerReply?.trim());
        if (aReplied !== bReplied) return aReplied ? 1 : -1;
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      }
      return 0;
    });

    return result;
  }, [activeReviews, selectedStar, statusFilter, selectedTurf, searchQuery, sortBy]);

  const handleReply = async (review: Review) => {
    const reply = (replyDrafts[review.id] ?? "").trim();
    if (reply.length < 2) {
      setError("Owner response must be at least 2 characters.");
      return;
    }
    setSavingReplyId(review.id);
    setError("");
    setSuccessMessage("");

    try {
      const res = await replyToReview(review.id, reply);
      setReviews((current) => current.map((item) => (item.id === review.id ? res.data : item)));
      setActiveReplyId(null);
      setReplyDrafts((current) => ({ ...current, [review.id]: "" }));
      setSuccessMessage(`Response posted for ${review.userName || "player"}'s review!`);
      setTimeout(() => setSuccessMessage(""), 4000);
    } catch {
      setError("Could not save owner response. Please try again.");
    } finally {
      setSavingReplyId(null);
    }
  };

  const handleApplyTemplate = (reviewId: number, template: string) => {
    setReplyDrafts((current) => ({ ...current, [reviewId]: template }));
  };

  const resetFilters = () => {
    setSearchQuery("");
    setSelectedStar(null);
    setStatusFilter("ALL");
    setSelectedTurf("ALL");
    setSortBy("NEWEST");
  };

  const hasActiveFilters =
    Boolean(searchQuery.trim()) || selectedStar !== null || statusFilter !== "ALL" || selectedTurf !== "ALL";

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Page Header */}
        <header className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
          <div>
            <h1 className="theme-title text-3xl font-black">
              Customer Reviews
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Reviews information" focusable={false}>
                  Monitor player feedback, view rating trends, and engage directly with your customers by replying to reviews.
                </TileInfo>
              </span>
            </h1>
          </div>

        </header>

        {/* Alerts */}
        {error && (
          <div className="rounded-2xl border border-rose-500/30 bg-rose-500/10 p-4 text-sm text-rose-300">
            {error}
          </div>
        )}
        {successMessage && (
          <div className="rounded-2xl border border-emerald-500/30 bg-emerald-500/10 p-4 text-sm text-emerald-300">
            {successMessage}
          </div>
        )}

        {/* Top Metric Cards */}
        <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <button
            type="button"
            onClick={() => setSelectedStar(null)}
            className="theme-panel rounded-2xl p-4 text-left transition hover:-translate-y-0.5 hover:border-sky-500/30"
            aria-label="Show reviews from every rating"
          >
            <p className="theme-soft text-xs font-black uppercase tracking-[0.18em]">Average Rating</p>
            <div className="mt-2 flex items-center justify-between gap-3">
              <div className="flex items-baseline gap-2">
                <span className="theme-title text-2xl font-black">{stats.average}</span>
                <span className="theme-muted text-sm font-semibold">/ 5.0</span>
              </div>
              <span className="text-lg tracking-[0.08em] text-amber-400" aria-hidden="true">★★★★★</span>
            </div>
          </button>
          <div className="theme-panel rounded-2xl p-4">
            <p className="theme-soft text-xs font-black uppercase tracking-[0.18em]">Total Reviews</p>
            <div className="mt-2 flex items-end justify-between gap-3">
              <p className="theme-title text-2xl font-black">{stats.total}</p>
              <p className="theme-muted text-xs">Verified feedback</p>
            </div>
          </div>
          <div className="theme-panel rounded-2xl p-4">
            <p className="theme-soft text-xs font-black uppercase tracking-[0.18em]">Response Rate</p>
            <div className="mt-2 flex items-center gap-3">
              <p className="theme-title text-2xl font-black">{stats.responseRate}%</p>
              <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-white/10">
                <div className="h-full rounded-full bg-emerald-400" style={{ width: `${stats.responseRate}%` }} />
              </div>
            </div>
          </div>
          <button
            type="button"
            onClick={() => setStatusFilter("NEEDS_REPLY")}
            className="theme-panel rounded-2xl p-4 text-left transition hover:-translate-y-0.5 hover:border-amber-500/30"
            aria-label="Show reviews awaiting a response"
          >
            <p className="theme-soft text-xs font-black uppercase tracking-[0.18em]">Pending Reply</p>
            <div className="mt-2 flex items-end justify-between gap-3">
              <p className={`text-2xl font-black ${stats.needsReply > 0 ? "text-amber-400" : "theme-title"}`}>
                {stats.needsReply}
              </p>
              <span className="text-xs font-bold text-amber-300">View queue →</span>
            </div>
          </button>
        </section>

        {/* Filter Bar: Status Pills placed BEFORE the Search bar as requested */}
        <section className="theme-panel rounded-2xl p-3.5 shadow-sm">
          <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
          {/* Left: Status Pills FIRST, then Search Bar */}
          <div className="flex flex-1 flex-col items-stretch gap-3 lg:flex-row lg:items-center">
            {/* Status Pills (All, Needs Reply, Replied) */}
            <div className="grid grid-cols-3 rounded-xl border border-white/5 bg-white/5 p-1 sm:flex sm:shrink-0">
              <button
                type="button"
                onClick={() => setStatusFilter("ALL")}
                className={`rounded-lg px-3 py-1.5 text-xs font-bold transition ${
                  statusFilter === "ALL" ? "bg-sky-500 text-white shadow-sm" : "text-slate-400 hover:text-white"
                }`}
              >
                All ({activeReviews.length})
              </button>
              <button
                type="button"
                onClick={() => setStatusFilter("NEEDS_REPLY")}
                className={`rounded-lg px-3 py-1.5 text-xs font-bold transition ${
                  statusFilter === "NEEDS_REPLY" ? "bg-sky-500 text-white shadow-sm" : "text-slate-400 hover:text-white"
                }`}
              >
                Needs Reply ({stats.needsReply})
              </button>
              <button
                type="button"
                onClick={() => setStatusFilter("REPLIED")}
                className={`rounded-lg px-3 py-1.5 text-xs font-bold transition ${
                  statusFilter === "REPLIED" ? "bg-sky-500 text-white shadow-sm" : "text-slate-400 hover:text-white"
                }`}
              >
                Replied ({stats.replied})
              </button>
            </div>

            {/* Search Input - placed AFTER the status pills */}
            <div className="relative min-w-0 flex-1 xl:max-w-md">
              <svg
                className="theme-soft absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search player, turf, comment..."
                className="theme-input h-10 w-full rounded-xl pl-9 pr-7 text-sm font-medium focus:border-sky-500 focus:outline-none"
              />
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => setSearchQuery("")}
                  className="theme-soft absolute right-3 top-1/2 -translate-y-1/2 text-sm hover:text-white"
                  aria-label="Clear search"
                >
                  ✕
                </button>
              )}
            </div>
          </div>

          {/* Right Controls: Sort + Turf + View Switcher */}
          <div className="grid grid-cols-2 gap-2.5 sm:flex sm:flex-wrap sm:items-center sm:shrink-0">
            <select
              value={selectedStar ?? "ALL"}
              onChange={(e) => setSelectedStar(e.target.value === "ALL" ? null : Number(e.target.value))}
              className="theme-input h-10 rounded-xl px-3 text-xs font-bold focus:border-sky-500 focus:outline-none"
              aria-label="Filter by rating"
            >
              <option value="ALL">All Ratings</option>
              {[5, 4, 3, 2, 1].map((rating) => (
                <option key={rating} value={rating}>{rating} Stars ({stats.distribution[rating]})</option>
              ))}
            </select>

            {/* Sort Selector */}
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as typeof sortBy)}
              className="theme-input h-10 rounded-xl px-3 text-xs font-bold focus:border-sky-500 focus:outline-none"
            >
              <option value="NEWEST">Newest First</option>
              <option value="OLDEST">Oldest First</option>
              <option value="RATING_HIGH">Highest Rating</option>
              <option value="RATING_LOW">Lowest Rating</option>
              <option value="NEEDS_REPLY">Needs Reply First</option>
            </select>

            {/* Turf Selector if multiple turfs exist */}
            {turfNames.length > 1 && (
              <select
                value={selectedTurf}
                onChange={(e) => setSelectedTurf(e.target.value)}
                className="theme-input h-10 rounded-xl px-3 text-xs font-bold focus:border-sky-500 focus:outline-none"
              >
                <option value="ALL">All Turfs</option>
                {turfNames.map((name) => (
                  <option key={name} value={name}>
                    {name}
                  </option>
                ))}
              </select>
            )}

            {/* View Switcher: Table vs Cards */}
            <div className="hidden rounded-xl border border-white/5 bg-white/5 p-1 lg:flex">
              <button
                type="button"
                onClick={() => setViewMode("TABLE")}
                className={`flex items-center gap-1.5 rounded-lg px-3 py-1 text-xs font-bold transition ${
                  viewMode === "TABLE" ? "bg-sky-500 text-white shadow-sm" : "text-slate-400 hover:text-white"
                }`}
                title="Structured Table View"
              >
                <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                </svg>
                <span>Table</span>
              </button>
              <button
                type="button"
                onClick={() => setViewMode("GRID")}
                className={`flex items-center gap-1.5 rounded-lg px-3 py-1 text-xs font-bold transition ${
                  viewMode === "GRID" ? "bg-sky-500 text-white shadow-sm" : "text-slate-400 hover:text-white"
                }`}
                title="Card Grid View"
              >
                <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
                </svg>
                <span>Cards</span>
              </button>
            </div>
          </div>
          </div>

          {hasActiveFilters && (
            <div className="mt-3 flex justify-end border-t border-white/5 pt-3 text-xs">
              <button
                type="button"
                onClick={resetFilters}
                className="font-bold text-sky-400 transition hover:text-sky-300"
              >
                Clear active filters
              </button>
            </div>
          )}
        </section>

        {/* Reviews Section */}
        <section className="space-y-4">
          {loading && (
            <div className="theme-panel flex items-center justify-center rounded-2xl py-16 text-center">
              <div className="flex flex-col items-center gap-2.5">
                <div className="h-7 w-7 animate-spin rounded-full border-2 border-sky-400 border-t-transparent" />
                <p className="theme-muted text-sm font-semibold">Loading player reviews...</p>
              </div>
            </div>
          )}

          {!loading && displayedReviews.length === 0 && (
            <div className="theme-panel rounded-2xl p-10 text-center">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-white/5 text-slate-400">
                <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                </svg>
              </div>
              <h3 className="theme-title mt-3 text-base font-bold">No reviews match your filters</h3>
              <p className="theme-muted mt-1 text-sm">Try clearing your search query or changing your star filter.</p>
              <button
                type="button"
                onClick={resetFilters}
                className="mt-4 rounded-xl bg-sky-500/20 px-4 py-2 text-sm font-bold text-sky-400 transition hover:bg-sky-500/30"
              >
                Reset all filters
              </button>
            </div>
          )}

          {/* TABLE VIEW: Redesigned with Separate Columns for Turf, Status, Action with Defined Column Boundaries */}
          {viewMode === "TABLE" && !loading && displayedReviews.length > 0 && (
            <div className="theme-panel hidden overflow-x-auto rounded-2xl border border-white/10 shadow-lg lg:block">
              <table className="w-full text-left border-collapse min-w-[1020px]">
                <thead>
                  <tr className="bg-white/[0.03] border-b border-white/10 text-xs font-black uppercase tracking-wider text-slate-400">
                    <th className="py-3.5 px-4 w-[220px]">Customer</th>
                    <th className="py-3.5 px-4 w-[170px]">Turf Venue</th>
                    <th className="py-3.5 px-4 w-[130px]">Rating</th>
                    <th className="py-3.5 px-4 min-w-[280px]">Player Feedback</th>
                    <th className="py-3.5 px-4 w-[120px] text-center">Action</th>
                    <th className="py-3.5 px-4 w-[180px] text-center">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                  {displayedReviews.map((review) => {
                    const rating = Math.max(1, Math.min(5, Math.round(Number(review.rating) || 5)));
                    const initials = (review.userName || "Player")
                      .split(" ")
                      .map((n) => n[0])
                      .slice(0, 2)
                      .join("")
                      .toUpperCase();
                    const hasReplied = Boolean(review.ownerReply?.trim());
                    const isComposing = activeReplyId === review.id;

                    return (
                      <tr
                        key={review.id}
                        className="transition-colors hover:bg-white/[0.015]"
                      >
                        {/* 1. Customer Column */}
                        <td className="py-4 px-4 align-top w-[220px]">
                          <div className="flex items-start gap-3">
                            <ReviewerAvatar initials={initials} />
                            <div className="min-w-0">
                              <div className="flex min-w-0 items-center gap-1.5 whitespace-nowrap">
                                <span className="min-w-0 truncate text-sm font-bold text-white">
                                  {review.userName || "Player"}
                                </span>
                                <span
                                  className="inline-flex shrink-0 items-center rounded bg-emerald-500/10 px-1.5 py-0.5 text-[10px] font-bold text-emerald-400"
                                  title="Verified Booking"
                                >
                                  ✓
                                </span>
                              </div>
                              <p className="text-xs text-slate-400 mt-1">
                                {formatDate(review.createdAt)}
                              </p>
                            </div>
                          </div>
                        </td>

                        {/* 2. Turf Venue Column (Distinct separate column requested by user) */}
                        <td className="py-4 px-4 align-top w-[170px]">
                          <div className="pt-0.5">
                            <span className="inline-flex max-w-full items-center rounded-lg border border-white/5 bg-white/5 px-2.5 py-1 text-xs font-semibold text-slate-200">
                              <span className="truncate font-medium">
                                {review.turfName || `Turf #${review.turfId}`}
                              </span>
                            </span>
                          </div>
                        </td>

                        {/* 3. Rating Column */}
                        <td className="py-4 px-4 align-top w-[130px]">
                          <div className="pt-0.5">
                            <RatingStars rating={rating} />
                            <p className="text-[11px] text-slate-400 mt-1">
                              {rating === 5 ? "Exceptional" : rating === 4 ? "Very Good" : rating === 3 ? "Average" : "Needs Attention"}
                            </p>
                          </div>
                        </td>

                        {/* 4. Player Feedback Column */}
                        <td className="py-4 px-4 align-top min-w-[280px]">
                          <div>
                            {review.title && (
                              <h4 className="text-sm font-bold text-slate-100 mb-1">
                                {review.title}
                              </h4>
                            )}
                            <p className="text-sm leading-relaxed text-slate-200 font-normal">
                              &ldquo;{review.comment || "No comment provided."}&rdquo;
                            </p>

                            {/* Existing Reply Display */}
                            {hasReplied && (
                              <div className="mt-2.5 rounded-xl border border-emerald-500/20 bg-emerald-500/[0.04] p-3 text-xs">
                                <span className="text-emerald-400 font-bold block mb-1">↳ Your Response:</span>
                                <p className="text-slate-300 leading-relaxed font-normal">{review.ownerReply}</p>
                              </div>
                            )}

                            {/* Inline Reply Composer */}
                            {isComposing && (
                              <div className="mt-3 rounded-2xl border border-sky-500/30 bg-sky-500/[0.04] p-3.5 space-y-2.5">
                                <div>
                                  <p className="text-[11px] font-bold uppercase tracking-wider text-slate-400 mb-1.5">
                                    Quick Templates
                                  </p>
                                  <div className="flex flex-wrap gap-1.5">
                                    {QUICK_REPLIES.map((tpl, idx) => (
                                      <button
                                        key={idx}
                                        type="button"
                                        onClick={() => handleApplyTemplate(review.id, tpl)}
                                        className="rounded-lg border border-white/10 bg-white/5 hover:border-sky-400/40 hover:bg-sky-500/10 hover:text-white px-2.5 py-1 text-xs text-slate-300 transition"
                                      >
                                        {tpl}
                                      </button>
                                    ))}
                                  </div>
                                </div>

                                <textarea
                                  autoFocus
                                  value={replyDrafts[review.id] ?? ""}
                                  onChange={(e) =>
                                    setReplyDrafts((current) => ({ ...current, [review.id]: e.target.value }))
                                  }
                                  rows={2}
                                  maxLength={800}
                                  placeholder="Type your response to this player..."
                                  className="theme-input w-full rounded-xl p-2.5 text-xs leading-relaxed focus:border-sky-500 focus:outline-none"
                                />

                                <div className="flex items-center justify-between">
                                  <span className="text-[11px] text-slate-400">
                                    {(replyDrafts[review.id] || "").length}/800
                                  </span>
                                  <div className="flex items-center gap-2">
                                    <button
                                      type="button"
                                      onClick={() => setActiveReplyId(null)}
                                      className="rounded-xl px-3 py-1 text-xs text-slate-400 hover:text-white transition"
                                    >
                                      Cancel
                                    </button>
                                    <button
                                      type="button"
                                      onClick={() => handleReply(review)}
                                      disabled={savingReplyId === review.id || !(replyDrafts[review.id] || "").trim()}
                                      className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 px-3.5 py-1 text-xs font-bold text-white shadow-md disabled:opacity-40 disabled:cursor-not-allowed transition"
                                    >
                                      {savingReplyId === review.id ? "Posting..." : "Post"}
                                    </button>
                                  </div>
                                </div>
                              </div>
                            )}
                          </div>
                        </td>

                        {/* 5. Action Column */}
                        <td className="py-4 px-4 align-middle w-[120px] text-center">
                          <div className="flex flex-col items-center justify-center gap-1.5">
                            {hasReplied ? (
                              <>
                                {review.ownerReplyAt && (
                                  <span className="text-[11px] text-slate-400">
                                    {formatDate(review.ownerReplyAt)}
                                  </span>
                                )}
                                <button
                                  type="button"
                                  onClick={() => {
                                    setActiveReplyId(isComposing ? null : review.id);
                                    setReplyDrafts((c) => ({ ...c, [review.id]: review.ownerReply || "" }));
                                  }}
                                  className="inline-flex min-w-20 items-center justify-center gap-1 rounded-lg border border-white/10 bg-white/5 px-2.5 py-1 text-xs font-semibold text-sky-400 transition hover:border-white/25 hover:bg-white/10 hover:text-sky-300"
                                >
                                  {isComposing ? "Close" : "Edit"}
                                </button>
                              </>
                            ) : (
                              <button
                                type="button"
                                onClick={() => {
                                  setActiveReplyId(isComposing ? null : review.id);
                                  if (!replyDrafts[review.id]) {
                                    setReplyDrafts((c) => ({ ...c, [review.id]: "" }));
                                  }
                                }}
                                className="inline-flex min-w-20 items-center justify-center gap-1.5 rounded-xl bg-sky-500 px-3.5 py-1.5 text-xs font-bold text-white shadow-sm transition hover:bg-sky-400 active:scale-95"
                              >
                                <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6" />
                                </svg>
                                <span>{isComposing ? "Cancel" : "Reply"}</span>
                              </button>
                            )}
                          </div>
                        </td>

                        {/* 6. Status Column */}
                        <td className="py-4 px-4 align-middle w-[180px] text-center">
                          <div className="flex justify-center">
                            <ReviewStatusBadge hasReplied={hasReplied} />
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}

          {/* CARD GRID VIEW: Sleek, Balanced 3-Column Layout */}
          {!loading && displayedReviews.length > 0 && (
            <div className={`grid grid-cols-1 gap-4 md:grid-cols-2 ${viewMode === "GRID" ? "lg:grid 2xl:grid-cols-3" : "lg:hidden"}`}>
              {displayedReviews.map((review) => {
                const rating = Math.max(1, Math.min(5, Math.round(Number(review.rating) || 5)));
                const initials = (review.userName || "Player")
                  .split(" ")
                  .map((n) => n[0])
                  .slice(0, 2)
                  .join("")
                  .toUpperCase();
                const hasReplied = Boolean(review.ownerReply?.trim());
                const isComposing = activeReplyId === review.id;

                return (
                  <article
                    key={review.id}
                    className="theme-panel group relative flex flex-col justify-between rounded-2xl p-5 border border-white/10 hover:border-sky-500/30 hover:shadow-xl transition-all duration-200"
                  >
                    <div>
                      {/* Top Header */}
                      <div className="flex items-start justify-between gap-3">
                        <div className="flex items-center gap-3 min-w-0">
                          <ReviewerAvatar initials={initials} large />
                          <div className="min-w-0">
                            <div className="flex items-center gap-1.5">
                              <h3 className="font-bold text-sm text-white truncate leading-tight">
                                {review.userName || "Player"}
                              </h3>
                              <span
                                className="inline-flex shrink-0 items-center rounded bg-emerald-500/10 px-1.5 py-0.5 text-[10px] font-bold text-emerald-400"
                                title="Verified Booking"
                              >
                                ✓
                              </span>
                            </div>
                            <p className="theme-muted text-xs font-medium mt-0.5 truncate">
                              {review.turfName || `Turf #${review.turfId}`}
                            </p>
                            <p className="text-xs text-slate-400 mt-0.5">
                              {formatDate(review.createdAt)}
                            </p>
                          </div>
                        </div>

                        {/* Star Rating Badge */}
                        <div className="flex flex-col items-end gap-1 shrink-0">
                          <div className="flex items-center gap-1">
                            <span className="text-amber-400 text-xs font-black">★</span>
                            <span className="text-xs font-bold text-white">{rating}.0</span>
                          </div>
                          <ReviewStatusBadge hasReplied={hasReplied} compact />
                        </div>
                      </div>

                      {/* Comment */}
                      <div className="mt-4 rounded-xl bg-white/[0.02] p-3.5 border border-white/5">
                        {review.title && (
                          <h4 className="text-sm font-bold text-slate-100 mb-1">
                            {review.title}
                          </h4>
                        )}
                        <p className="text-sm leading-relaxed text-slate-200">
                          &ldquo;{review.comment || "No comment provided."}&rdquo;
                        </p>
                      </div>

                      {/* Existing Reply Display */}
                      {hasReplied && (
                        <div className="mt-3 rounded-xl border border-emerald-500/20 bg-emerald-500/[0.04] p-3">
                          <div className="flex items-center justify-between text-xs font-bold text-emerald-400 mb-1">
                            <span>↳ Your Response</span>
                            {review.ownerReplyAt && (
                              <span className="text-[11px] font-normal text-slate-400">
                                {formatDate(review.ownerReplyAt)}
                              </span>
                            )}
                          </div>
                          <p className="text-xs text-slate-300 leading-relaxed">
                            {review.ownerReply}
                          </p>
                        </div>
                      )}
                    </div>

                    {/* Footer Actions */}
                    <div className="mt-4 pt-3.5 border-t border-white/10">
                      {isComposing ? (
                        <div className="space-y-2.5">
                          <div className="flex flex-wrap gap-1">
                            {QUICK_REPLIES.slice(0, 2).map((tpl, idx) => (
                              <button
                                key={idx}
                                type="button"
                                onClick={() => handleApplyTemplate(review.id, tpl)}
                                className="truncate max-w-[160px] rounded-lg border border-white/10 bg-white/5 px-2 py-0.5 text-xs text-slate-300 hover:border-sky-400/40 hover:bg-sky-500/10 hover:text-white transition"
                                title={tpl}
                              >
                                {tpl.slice(0, 22)}...
                              </button>
                            ))}
                          </div>
                          <textarea
                            autoFocus
                            value={replyDrafts[review.id] ?? ""}
                            onChange={(e) =>
                              setReplyDrafts((current) => ({ ...current, [review.id]: e.target.value }))
                            }
                            rows={2}
                            maxLength={800}
                            placeholder="Write your reply..."
                            className="theme-input w-full resize-none rounded-xl p-2.5 text-xs leading-relaxed focus:border-sky-500 focus:outline-none"
                          />
                          <div className="flex items-center justify-between">
                            <span className="text-xs text-slate-400">
                              {(replyDrafts[review.id] || "").length}/800
                            </span>
                            <div className="flex items-center gap-2">
                              <button
                                type="button"
                                onClick={() => setActiveReplyId(null)}
                                className="rounded-lg px-2.5 py-1 text-xs text-slate-400 hover:text-white transition"
                              >
                                Cancel
                              </button>
                              <button
                                type="button"
                                onClick={() => handleReply(review)}
                                disabled={savingReplyId === review.id || !(replyDrafts[review.id] || "").trim()}
                                className="rounded-lg bg-emerald-500 hover:bg-emerald-400 px-3 py-1 text-xs font-bold text-white shadow-sm disabled:opacity-40 disabled:cursor-not-allowed transition"
                              >
                                {savingReplyId === review.id ? "Posting..." : "Send"}
                              </button>
                            </div>
                          </div>
                        </div>
                      ) : (
                        <div className="flex items-center justify-between">
                          {hasReplied ? (
                            <>
                              <span className="text-xs text-emerald-400 font-semibold flex items-center gap-1">
                                ✓ Response sent
                              </span>
                              <button
                                type="button"
                                onClick={() => {
                                  setActiveReplyId(review.id);
                                  setReplyDrafts((curr) => ({ ...curr, [review.id]: review.ownerReply || "" }));
                                }}
                                className="text-xs text-slate-400 hover:text-white font-medium transition"
                              >
                                Edit reply
                              </button>
                            </>
                          ) : (
                            <>
                              <span className="text-xs text-amber-300 font-medium flex items-center gap-1.5">
                                <span className="h-1.5 w-1.5 rounded-full bg-amber-400 animate-pulse" />
                                Awaiting reply
                              </span>
                              <button
                                type="button"
                                onClick={() => {
                                  setActiveReplyId(review.id);
                                  if (!replyDrafts[review.id]) {
                                    setReplyDrafts((curr) => ({ ...curr, [review.id]: "" }));
                                  }
                                }}
                                className="inline-flex items-center gap-1.5 rounded-xl border border-sky-500/30 bg-sky-500/10 hover:bg-sky-500 hover:text-white px-3 py-1.5 text-xs font-bold text-sky-300 transition shadow-sm active:scale-95"
                              >
                                <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6" />
                                </svg>
                                Reply
                              </button>
                            </>
                          )}
                        </div>
                      )}
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </section>
      </div>
    </DashboardLayout>
  );
}

function formatDate(value?: string) {
  if (!value) return "No date";
  return new Date(value).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });
}

function ReviewerAvatar({ initials, large = false }: { initials: string; large?: boolean }) {
  return (
    <div
      className={`flex shrink-0 items-center justify-center bg-gradient-to-br from-sky-500 to-emerald-500 font-black text-white shadow-sm ${
        large ? "h-11 w-11 rounded-2xl text-sm" : "mt-0.5 h-10 w-10 rounded-xl text-xs"
      }`}
      aria-hidden="true"
    >
      {initials}
    </div>
  );
}

function RatingStars({ rating }: { rating: number }) {
  return (
    <div className="flex items-center gap-1" aria-label={`${rating} out of 5 stars`}>
      {[1, 2, 3, 4, 5].map((star) => (
        <svg
          key={star}
          className={`h-3.5 w-3.5 ${
            star <= rating ? "fill-amber-400 text-amber-400" : "fill-slate-700 text-slate-700"
          }`}
          viewBox="0 0 20 20"
          aria-hidden="true"
        >
          <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
        </svg>
      ))}
      <span className="ml-1 text-xs font-bold text-white">{rating}.0</span>
    </div>
  );
}

function ReviewStatusBadge({ hasReplied, compact = false }: { hasReplied: boolean; compact?: boolean }) {
  const sizing = compact ? "px-2 py-0.5 text-[10px]" : "min-w-[132px] justify-center px-3 py-1.5 text-xs";
  return (
    <span
      className={`inline-flex whitespace-nowrap items-center gap-1.5 rounded-full border font-semibold ${sizing} ${
        hasReplied
          ? "border-emerald-500/20 bg-emerald-500/10 text-emerald-400"
          : "border-amber-500/20 bg-amber-500/10 text-amber-300"
      }`}
    >
      <span className={`h-1.5 w-1.5 rounded-full ${hasReplied ? "bg-emerald-400" : "bg-amber-400"}`} />
      {hasReplied ? "Replied" : compact ? "Needs Reply" : "Pending Response"}
    </span>
  );
}
