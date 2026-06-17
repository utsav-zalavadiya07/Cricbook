import { Suspense } from "react";
import BookingPageContent from "./BookingPageContent";

export const metadata = { title: "Confirm Booking | Cricbook" };

export default function BookingPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-emerald-950 to-slate-900 flex items-center justify-center">
        <div className="text-slate-400 text-sm">Loading booking...</div>
      </div>
    }>
      <BookingPageContent />
    </Suspense>
  );
}
