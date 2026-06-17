import type { Review } from "@/modules/review/types/review.types";

type ProfileReview = Review & {
  turfName?: string;
};

type ReviewListProps = {
  reviews?: ProfileReview[];
};

export default function ReviewList({ reviews }: ReviewListProps) {
  if (!reviews || reviews.length === 0) {
    return (
      <div className="mt-4 rounded-2xl border border-white/10 bg-white/5 p-8 text-center">
        <p className="text-slate-400">No reviews found.</p>
      </div>
    );
  }

  return (
    <div className="mt-4 space-y-4">
      {reviews.map((r) => (
        <div key={r.id} className="rounded-2xl border border-white/10 bg-white/5 p-5 shadow-md">
          <div className="mb-2 flex flex-wrap items-center gap-2">
            <span className="text-lg text-yellow-400">{"*".repeat(Number(r.rating) || 0)}</span>
            <span className="text-xs text-slate-400">
              {r.createdAt ? new Date(r.createdAt).toLocaleDateString("en-IN") : r.turfName || `Turf #${r.turfId ?? "-"}`}
            </span>
          </div>
          {r.turfName && <p className="mb-1 text-sm font-semibold text-white">{r.turfName}</p>}
          <p className="text-sm text-slate-200">{r.comment || "No comment"}</p>
        </div>
      ))}
    </div>
  );
}
