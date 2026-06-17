"use client";

interface StarRatingProps {
  rating: number;
  setRating?: (r: number) => void;
  size?: "sm" | "md" | "lg";
  readonly?: boolean;
}

export default function StarRating({
  rating,
  setRating,
  size = "md",
  readonly = false,
}: StarRatingProps) {
  const sizes = { sm: "text-base", md: "text-2xl", lg: "text-3xl" };

  return (
    <div className="flex gap-0.5">
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          key={star}
          type="button"
          disabled={readonly}
          onClick={() => !readonly && setRating?.(star)}
          className={`${sizes[size]} transition-all duration-100 ${
            readonly ? "cursor-default" : "cursor-pointer hover:scale-110"
          } ${
            star <= rating
              ? "text-amber-400 drop-shadow-sm"
              : "text-slate-600 hover:text-amber-300"
          }`}
          aria-label={`Rate ${star} star${star > 1 ? "s" : ""}`}
        >
          ★
        </button>
      ))}
    </div>
  );
}
