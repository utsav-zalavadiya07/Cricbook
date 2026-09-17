import type { Booking } from "@/modules/booking/types/booking.types";

import { formatDate } from "@/utils/dateFormatter";

type BookingListProps = {
  bookings?: Booking[];
};

export default function BookingList({ bookings }: BookingListProps) {
  if (!bookings || bookings.length === 0) {
    return (
      <div className="mt-4 rounded-2xl border border-white/10 bg-white/5 p-8 text-center">
        <p className="text-slate-400">No bookings found.</p>
      </div>
    );
  }

  return (
    <div className="mt-4 space-y-4">
      {bookings.map((b) => (
        <div key={b.id} className="flex flex-col gap-4 rounded-2xl border border-white/10 bg-white/5 p-5 shadow-md transition hover:border-emerald-500/30 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h4 className="text-lg font-semibold text-white">{b.turfName || `Turf #${b.turfId}`}</h4>
            <p className="mt-1 text-sm text-slate-400">
              {b.startTime || "--"} to {b.endTime || "--"} - {b.bookingDate ? formatDate(b.bookingDate) : "Date pending"}
            </p>
            <p className="text-sm text-slate-500">{b.turfCity || b.turfLocation || "Location pending"}</p>
          </div>
          <div className="flex flex-col gap-2 sm:items-end">
            <span className={`w-fit rounded-full px-3 py-1 text-xs font-semibold ${
              b.status === "CONFIRMED" ? "bg-emerald-500/20 text-emerald-400" :
              b.status === "CANCELLED" ? "bg-red-500/20 text-red-400" :
              "bg-amber-500/20 text-amber-400"
            }`}>
              {b.status}
            </span>
            <p className="font-medium text-white">Rs {b.totalAmount ?? b.amount ?? 0}</p>
          </div>
        </div>
      ))}
    </div>
  );
}
