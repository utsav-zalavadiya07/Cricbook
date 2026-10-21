interface SlotPriceDisplayProps {
  price: number;
  compareAtPrice?: number;
  prefix?: string;
  suffix?: string;
  showOfferBadge?: boolean;
  className?: string;
  priceClassName?: string;
}

const formatPrice = (value: number) => `₹${value.toLocaleString("en-IN")}`;

export default function SlotPriceDisplay({
  price,
  compareAtPrice,
  prefix,
  suffix = "/ slot",
  showOfferBadge = false,
  className = "",
  priceClassName = "text-lg",
}: SlotPriceDisplayProps) {
  const hasComparison = Number.isFinite(compareAtPrice) && Number(compareAtPrice) > price;

  return (
    <div className={`flex flex-wrap items-baseline gap-x-2 gap-y-1 ${className}`.trim()}>
      {prefix && <span className="theme-soft text-xs font-semibold">{prefix}</span>}
      <span className={`${priceClassName} font-bold text-emerald-700 dark:text-emerald-300`}>{formatPrice(price)}</span>
      {hasComparison && <span className="theme-soft text-xs line-through">{formatPrice(Number(compareAtPrice))}</span>}
      {suffix && <span className="theme-soft text-xs font-normal">{suffix}</span>}
      {showOfferBadge && hasComparison && (
        <span className="rounded-full border border-emerald-500/25 bg-emerald-500/10 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide text-emerald-700 dark:text-emerald-300">
          Special price
        </span>
      )}
    </div>
  );
}
