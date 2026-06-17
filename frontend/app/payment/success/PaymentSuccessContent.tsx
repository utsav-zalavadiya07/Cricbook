"use client";

import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { useEffect, useState } from "react";

export default function PaymentSuccessContent() {
  const params = useSearchParams();
  const bookingId = params.get("bookingId");
  const bookingIds = (params.get("bookingIds") ?? bookingId ?? "").split(",").filter(Boolean);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => setVisible(true), 100);
    return () => clearTimeout(timer);
  }, []);

  return (
    <div className="theme-page-player flex min-h-screen items-center justify-center px-4">
      <div className={`w-full max-w-md space-y-6 text-center transition-all duration-700 ${visible ? "translate-y-0 opacity-100" : "translate-y-6 opacity-0"}`}>
        <div className="flex justify-center">
          <div className="flex h-24 w-24 items-center justify-center rounded-full border-2 border-emerald-500/40 bg-emerald-500/15 shadow-lg shadow-emerald-500/10">
            <svg className="h-12 w-12 text-emerald-600 dark:text-emerald-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
            </svg>
          </div>
        </div>

        <div>
          <h1 className="theme-title text-3xl font-bold">Payment Successful</h1>
          <p className="theme-muted mt-2 text-sm">Your turf has been booked. Have a great game.</p>
          {bookingIds.length > 0 && (
            <p className="theme-soft mt-2 text-xs">
              {bookingIds.length > 1 ? `Bookings ${bookingIds.map((id) => `#${id}`).join(", ")}` : `Booking #${bookingIds[0]}`}
            </p>
          )}
        </div>

        <div className="theme-panel rounded-2xl p-5 text-left">
          <p className="theme-title text-sm font-semibold">What happens next?</p>
          <ul className="theme-muted mt-3 space-y-2 text-sm">
            <li>Booking confirmation sent to your email</li>
            <li>Slot is confirmed and reserved for you</li>
            <li>Show booking ID at the turf entrance</li>
          </ul>
        </div>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <QuickLink href="/booking/history" label="Booking Details" />
          <a
            href={`https://calendar.google.com/calendar/render?action=TEMPLATE&text=${encodeURIComponent("Cricbook turf booking")}&details=${encodeURIComponent(`Booking ${bookingId || ""}`)}`}
            target="_blank"
            className="theme-card rounded-xl px-4 py-3 text-center text-sm font-semibold theme-text hover:border-emerald-400/40"
          >
            Add to Calendar
          </a>
          <QuickLink href="/search" label="Directions" />
          <QuickLink href="/favorites" label="Saved Turfs" />
        </div>

        <div className="flex flex-col gap-3">
          <Link href="/booking/history" className="w-full rounded-xl bg-emerald-500 py-3 text-center font-semibold text-white shadow-lg shadow-emerald-500/20 transition hover:bg-emerald-400">
            View My Bookings
          </Link>
          <Link href="/search" className="theme-card w-full rounded-xl py-3 text-center text-sm font-semibold theme-muted transition hover:border-emerald-400/40 hover:text-current">
            Book Another Turf
          </Link>
        </div>
      </div>
    </div>
  );
}

function QuickLink({ href, label }: { href: string; label: string }) {
  return (
    <Link href={href} className="theme-card rounded-xl px-4 py-3 text-center text-sm font-semibold theme-text transition hover:border-emerald-400/40">
      {label}
    </Link>
  );
}
