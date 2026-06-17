"use client";

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
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
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
        const statusLabel = status === "AVAILABLE" ? `Rs ${slot.price}` : status.charAt(0) + status.slice(1).toLowerCase();

        return (
          <button
            key={slot.slotId}
            disabled={unavailable}
            onClick={() => onSelect(slot)}
            className={`relative flex flex-col items-center rounded-xl border px-2 py-3 text-sm transition-all duration-150 ${
              unavailable
                ? "theme-card cursor-not-allowed opacity-50 theme-soft"
                : isSelected
                  ? "border-emerald-500 bg-emerald-500/20 text-emerald-700 shadow-lg shadow-emerald-500/10 dark:text-emerald-300"
                  : "theme-card theme-text hover:-translate-y-0.5 hover:border-emerald-500/50 hover:bg-emerald-500/5"
            }`}
          >
            <span className="text-xs font-semibold">{slot.startTime}</span>
            <span className="theme-soft my-0.5 text-xs">to</span>
            <span className="text-xs font-semibold">{slot.endTime}</span>
            <span className={`mt-2 text-xs font-bold ${isSelected ? "text-emerald-700 dark:text-emerald-300" : statusStyles[status] ?? "theme-soft"}`}>
              {statusLabel}
            </span>
            {isSelected && (
              <span className="absolute -right-1.5 -top-1.5 flex h-4 w-4 items-center justify-center rounded-full bg-emerald-500 text-[10px] font-bold text-white">
                Y
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
}
