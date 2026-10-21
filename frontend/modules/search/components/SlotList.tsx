"use client";

import { SlotPriceDisplay } from "@/components/ui";
import { SlotAvailability } from "../types/search.types";

interface SlotListProps {
  slots: SlotAvailability[];
  selectedSlot?: SlotAvailability | null;
  selectedSlots?: SlotAvailability[];
  onSelect: (slot: SlotAvailability) => void;
}

export default function SlotList({ slots, selectedSlot, selectedSlots, onSelect }: SlotListProps) {
  if (slots.length === 0) {
    return (
      <div className="theme-panel-soft flex h-28 items-center justify-center rounded-2xl border border-dashed">
        <p className="theme-muted text-sm">No slots available for this date</p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-[repeat(auto-fill,minmax(190px,1fr))] gap-3">
      {slots.map((slot) => {
        const isSelected = selectedSlots
          ? selectedSlots.some((item) => item.slotId === slot.slotId)
          : selectedSlot?.slotId === slot.slotId;
        const status = slot.status ?? (slot.blocked ? "BLOCKED" : slot.locked ? "LOCKED" : slot.available ? "AVAILABLE" : "BOOKED");
        const unavailable = !slot.available || slot.blocked || slot.locked || status !== "AVAILABLE";
        const statusStyles: Record<string, string> = {
          AVAILABLE: "text-emerald-700 dark:text-emerald-300",
          LOCKED: "text-amber-700 dark:text-amber-300",
          BOOKED: "text-red-700 dark:text-red-300",
          BLOCKED: "theme-soft",
          EXPIRED: "theme-soft",
          UNAVAILABLE: "theme-soft",
        };
        const statusLabel = status.charAt(0) + status.slice(1).toLowerCase();
        const hasSpecialPrice = Number(slot.listingPrice) > Number(slot.price);

        return (
          <button
            key={slot.slotId}
            disabled={unavailable}
            onClick={() => onSelect(slot)}
            className={`relative flex min-h-[112px] flex-col items-stretch rounded-2xl border p-4 text-left text-sm transition-all duration-150 ${
              unavailable
                ? "theme-card cursor-not-allowed opacity-50 theme-soft"
                : isSelected
                  ? "border-emerald-500 bg-emerald-500/20 text-emerald-700 shadow-lg shadow-emerald-500/10 dark:text-emerald-300"
                  : "theme-card theme-text hover:-translate-y-0.5 hover:border-emerald-500/50 hover:bg-emerald-500/5"
            }`}
          >
            <div className="flex items-start justify-between gap-3">
              <div>
                <span className="theme-soft block text-[10px] font-bold uppercase tracking-wider">Time</span>
                <span className="mt-1 block text-sm font-bold">{formatTime(slot.startTime)} – {formatTime(slot.endTime)}</span>
              </div>
              <span className={`mt-0.5 h-2.5 w-2.5 shrink-0 rounded-full ${status === "AVAILABLE" ? "bg-emerald-500" : status === "LOCKED" ? "bg-amber-500" : status === "BOOKED" ? "bg-red-500" : "bg-slate-500"}`} aria-hidden="true" />
            </div>
            {status === "AVAILABLE" ? (
              <SlotPriceDisplay
                price={Number(slot.price)}
                compareAtPrice={hasSpecialPrice ? Number(slot.listingPrice) : undefined}
                showOfferBadge={hasSpecialPrice}
                suffix=""
                className="mt-auto pt-3"
                priceClassName="text-base"
              />
            ) : (
              <span className={`mt-auto pt-3 text-xs font-bold ${isSelected ? "text-emerald-700 dark:text-emerald-300" : statusStyles[status] ?? "theme-soft"}`}>
                {statusLabel}
              </span>
            )}
            {isSelected && (
              <span className="absolute -right-1.5 -top-1.5 flex h-5 w-5 items-center justify-center rounded-full bg-emerald-500 text-[11px] font-bold text-white shadow-md">
                ✓
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
}

function formatTime(value: string) {
  return value?.slice(0, 5) ?? value;
}
