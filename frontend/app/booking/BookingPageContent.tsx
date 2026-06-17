"use client";

import { useSearchParams } from "next/navigation";
import Link from "next/link";
import BookingSummary from "@/modules/booking/components/BookingSummary";
import BookingButton from "@/modules/booking/components/BookingButton";

export default function BookingPageContent() {
  const params = useSearchParams();

  const turfId = Number(params.get("turfId"));
  const legacySlotId = Number(params.get("slotId"));
  const slotIds = (params.get("slotIds") ?? (legacySlotId ? String(legacySlotId) : ""))
    .split(",")
    .map((value) => Number(value))
    .filter(Boolean);
  const date = params.get("date") ?? "";
  const price = Number(params.get("price") ?? 500);
  const startTime = params.get("startTime") ?? undefined;
  const endTime = params.get("endTime") ?? undefined;
  const turfName = params.get("turfName") ?? undefined;
  const selectedSlots = parseSlots(params.get("slots"));

  // Guard: if params missing, send back to search
  if (!turfId || slotIds.length === 0 || !date) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-emerald-950 to-slate-900 flex items-center justify-center">
        <div className="text-center space-y-4">
          <p className="text-slate-400">No booking data found.</p>
          <Link href="/search" className="text-emerald-400 hover:text-emerald-300 text-sm">
            ← Back to Search
          </Link>
        </div>
      </div>
    );
  }

  const bookingData = slotIds.length > 1
    ? { turfId, slotIds, bookingDate: date }
    : { turfId, slotId: slotIds[0], bookingDate: date };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-emerald-950 to-slate-900">


      <div className="max-w-lg mx-auto px-6 py-10 space-y-6">
        {/* Header */}
        <div>
          <h1 className="text-white text-2xl font-bold">Confirm Booking</h1>
          <p className="text-slate-400 text-sm mt-1">Review your booking details before proceeding to payment.</p>
        </div>

        {/* Summary */}
        <BookingSummary
          turfId={turfId}
          slotId={slotIds[0]}
          slots={selectedSlots.length ? selectedSlots : undefined}
          date={date}
          price={price}
          startTime={startTime}
          endTime={endTime}
          turfName={turfName}
        />

        {/* Booking policies */}
        <div className="bg-white/3 border border-white/5 rounded-xl p-4 space-y-2">
          <p className="text-slate-400 text-xs font-medium uppercase tracking-wide">Policies</p>
          <ul className="space-y-1.5 text-slate-500 text-xs">
            <li>• Cancellations are allowed until 24 hours before the slot.</li>
            <li>• Payment must be completed before the hold expires.</li>
            <li>• Slot is held for 5 minutes after booking.</li>
          </ul>
        </div>

        {/* Confirm button */}
        <BookingButton
          bookingData={bookingData}
          price={price}
          paymentMeta={{
            turfName,
            date,
            startTime,
            endTime,
          }}
        />

        {/* History link */}
        <p className="text-center text-slate-500 text-xs">
          View your{" "}
          <Link href="/booking/history" className="text-emerald-400 hover:text-emerald-300 transition">
            booking history
          </Link>
        </p>
      </div>
    </div>
  );
}

function parseSlots(value: string | null) {
  if (!value) return [];
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}
