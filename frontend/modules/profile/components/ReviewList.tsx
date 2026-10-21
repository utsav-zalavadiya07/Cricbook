import type { Review } from "@/modules/review/types/review.types";

type ProfileReview = Review & {
  turfName?: string;
};

type ReviewListProps = {
  reviews?: ProfileReview[];
};

export default function ReviewList({ reviews }: ReviewListProps) {
  if (!reviews || reviews.length === 0) {
    return <EmptyState title="No reviews yet" body="Reviews connected to this account will appear here." />;
  }

  return (
    <div className="grid gap-4 xl:grid-cols-2">
      {reviews.map((review) => {
        const rating = Math.max(0, Math.min(5, Math.round(Number(review.rating) || 0)));
        return (
          <article key={review.id} className="theme-panel flex min-h-52 flex-col rounded-2xl p-5 transition hover:border-sky-500/25">
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0">
                <p className="theme-soft text-[11px] font-black uppercase tracking-[0.16em]">Turf Review</p>
                <h3 className="theme-title mt-1 truncate font-black">{review.turfName || `Turf #${review.turfId ?? "-"}`}</h3>
                {review.userName && <p className="theme-muted mt-1 text-xs">Shared by {review.userName}</p>}
              </div>
              <div className="shrink-0 text-right">
                <div className="flex items-center gap-1" aria-label={`${rating} out of 5 stars`}>
                  {[1, 2, 3, 4, 5].map((star) => (
                    <svg key={star} className={`h-4 w-4 ${star <= rating ? "fill-amber-400 text-amber-400" : "fill-slate-700 text-slate-700"}`} viewBox="0 0 20 20" aria-hidden="true">
                      <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 0 0 .95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 0 0-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 0 0-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 0 0-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 0 0 .951-.69l1.07-3.292Z" />
                    </svg>
                  ))}
                  <span className="ml-1 text-xs font-black theme-title">{rating}.0</span>
                </div>
                <p className="theme-soft mt-1 text-[11px]">{formatDate(review.createdAt)}</p>
              </div>
            </div>

            <div className="mt-4 flex-1 rounded-xl border border-[var(--theme-border)] bg-white/[0.025] p-4">
              {review.title && <p className="theme-title mb-1 text-sm font-bold">{review.title}</p>}
              <p className="theme-text text-sm leading-6">“{review.comment || "No comment provided."}”</p>
            </div>

            {review.ownerReply?.trim() && (
              <div className="mt-3 rounded-xl border border-emerald-500/20 bg-emerald-500/[0.05] px-4 py-3">
                <p className="text-[11px] font-black uppercase tracking-wider text-emerald-400">Owner response</p>
                <p className="theme-muted mt-1 text-sm leading-5">{review.ownerReply}</p>
              </div>
            )}
          </article>
        );
      })}
    </div>
  );
}

function EmptyState({ title, body }: { title: string; body: string }) {
  return (
    <div className="theme-panel rounded-2xl border-dashed p-10 text-center">
      <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-white/5 theme-soft">
        <svg className="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" aria-hidden="true">
          <path strokeLinecap="round" strokeLinejoin="round" d="m12 3 2.7 5.47 6.04.88-4.37 4.25 1.03 6.02L12 17.77 6.6 20.6l1.03-6.02-4.37-4.25 6.04-.88L12 3Z" />
        </svg>
      </div>
      <h3 className="theme-title mt-3 font-black">{title}</h3>
      <p className="theme-muted mt-1 text-sm">{body}</p>
    </div>
  );
}

function formatDate(value?: string) {
  if (!value) return "Date unavailable";
  return new Date(value).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });
}
