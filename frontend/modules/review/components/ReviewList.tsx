import { Review } from "../types/review.types";
import ReviewItem from "./ReviewItem";

interface ReviewListProps {
  reviews: Review[];
  loading?: boolean;
  onChanged?: () => void;
}

export default function ReviewList({ reviews, loading, onChanged }: ReviewListProps) {
  if (loading) return (
    <div className="theme-muted flex h-24 items-center justify-center gap-2 text-sm">
      <svg className="animate-spin h-4 w-4 text-emerald-400" viewBox="0 0 24 24" fill="none">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
      </svg>
      Loading reviews...
    </div>
  );

  if (reviews.length === 0) return (
    <div className="theme-panel-soft flex h-28 flex-col items-center justify-center rounded-2xl border border-dashed">
      <p className="theme-muted text-sm">No reviews yet. Be the first to review!</p>
    </div>
  );

  return (
    <div className="space-y-3">
      {reviews.map((r) => (
        <ReviewItem key={r.id} review={r} onChanged={onChanged} />
      ))}
    </div>
  );
}
