"use client";

import { useEffect, useState, useCallback } from "react";
import { getReviews, getReviewSummary } from "../services/reviewService";
import { Review, ReviewSummary } from "../types/review.types";

export default function useReviews(turfId: number) {
  const [reviews, setReviews] = useState<Review[]>([]);
  const [summary, setSummary] = useState<ReviewSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const fetchReviews = useCallback(async () => {
    if (!turfId) return;
    setLoading(true);
    setError("");
    try {
      const [reviewsRes, summaryRes] = await Promise.all([
        getReviews(turfId),
        getReviewSummary(turfId),
      ]);
      setReviews(reviewsRes.data);
      setSummary(summaryRes.data);
    } catch {
      setError("Failed to load reviews.");
    } finally {
      setLoading(false);
    }
  }, [turfId]);

  useEffect(() => {
    const timer = window.setTimeout(() => void fetchReviews(), 0);
    return () => window.clearTimeout(timer);
  }, [fetchReviews]);

  const avgRating =
    reviews.length > 0
      ? reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length
      : 0;

  return { reviews, summary, loading, error, refresh: fetchReviews, avgRating };
}
