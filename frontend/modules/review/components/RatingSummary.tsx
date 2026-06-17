import StarRating from "./StarRating";
import { ReviewSummary } from "../types/review.types";

interface RatingSummaryProps {
  avgRating: number;
  total: number;
  summary?: ReviewSummary | null;
}

export default function RatingSummary({ avgRating, total, summary }: RatingSummaryProps) {
  const effectiveAverage = summary?.averageRating ?? avgRating;
  const effectiveTotal = summary?.totalReviews ?? total;
  const ratingLabel = (rating: number) => {
    if (rating >= 4.5) return "Excellent";
    if (rating >= 3.5) return "Good";
    if (rating >= 2.5) return "Average";
    return "Poor";
  };

  return (
    <div className="flex flex-col gap-6 lg:flex-row lg:items-center">
      <div className="shrink-0 text-center">
        <p className="theme-title text-5xl font-bold leading-none">{effectiveTotal > 0 ? effectiveAverage.toFixed(1) : "-"}</p>
        <StarRating rating={Math.round(effectiveAverage)} size="sm" readonly />
        <p className="theme-muted mt-1 text-xs">
          {effectiveTotal > 0 ? `${ratingLabel(effectiveAverage)} / ${effectiveTotal} review${effectiveTotal !== 1 ? "s" : ""}` : "No reviews yet"}
        </p>
      </div>

      {effectiveTotal > 0 && <div className="hidden h-16 w-px shrink-0 bg-white/10 lg:block" />}

      {effectiveTotal > 0 && (
        <div className="grid flex-1 gap-4 md:grid-cols-[1fr_220px]">
          <div className="space-y-1.5">
            {[5, 4, 3, 2, 1].map((rating) => {
              const count = summary?.ratingBreakdown?.[rating] ?? 0;
              const percent = effectiveTotal > 0 ? (count / effectiveTotal) * 100 : 0;
              return (
                <div key={rating} className="grid grid-cols-[44px_1fr_34px] items-center gap-2 text-xs">
                  <span className="theme-muted">{rating} star</span>
                  <div className="h-2 overflow-hidden rounded-full bg-white/10">
                    <div className="h-full rounded-full bg-amber-400" style={{ width: `${percent}%` }} />
                  </div>
                  <span className="theme-soft text-right">{count}</span>
                </div>
              );
            })}
          </div>
          <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/10 p-4">
            <p className="theme-soft text-xs font-semibold uppercase tracking-wider">Reputation</p>
            <p className="theme-title mt-1 text-2xl font-bold">{summary?.reputationScore ?? 0}/100</p>
            <p className="text-sm font-semibold text-emerald-500">{summary?.reputationLabel ?? "New turf"}</p>
            <p className="theme-muted mt-2 text-xs">Owner response rate: {summary?.ownerResponseRate ?? 0}%</p>
          </div>
        </div>
      )}
    </div>
  );
}
