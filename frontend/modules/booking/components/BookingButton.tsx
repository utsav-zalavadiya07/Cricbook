"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { createBatchBooking, createBooking } from "../services/bookingService";
import { BatchBookingRequest, BookingRequest } from "../types/booking.types";

type ApiError = {
  message?: string;
  response?: {
    data?: {
      message?: string;
      error?: string;
    };
  };
};

interface BookingButtonProps {
  bookingData: BookingRequest | BatchBookingRequest;
  price: number;
  paymentMeta?: Record<string, string | undefined>;
}

export default function BookingButton({ bookingData, price, paymentMeta = {} }: BookingButtonProps) {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const inFlight = useRef(false);
  const isPlanLimitError = /limit reached|upgrade your subscription|current .* plan/i.test(error);

  const handleBooking = async () => {
    if (inFlight.current) return;
    inFlight.current = true;
    setLoading(true);
    setError("");
    try {
      const isBatch = "slotIds" in bookingData;
      const res = isBatch
        ? await createBatchBooking(bookingData)
        : await createBooking(bookingData);
      const bookings = (Array.isArray(res.data) ? res.data : [res.data]) as Array<{ id: number; amount?: number; totalAmount?: number }>;
      const payableAmount = bookings.reduce(
        (total, booking) => total + Number(booking.amount ?? booking.totalAmount ?? 0),
        0,
      );
      if (payableAmount <= 0) throw new Error("The final booking amount could not be calculated.");
      const params = new URLSearchParams({
        bookingIds: bookings.map((booking) => booking.id).join(","),
        bookingId: String(bookings[0].id),
        price: String(payableAmount),
        regularPrice: String(price),
      });
      Object.entries(paymentMeta).forEach(([key, value]) => {
        if (value) params.set(key, value);
      });
      router.push(`/payment?${params.toString()}`);
    } catch (err: unknown) {
      const apiError = err as ApiError;
      setError(
        apiError.response?.data?.message ||
          apiError.response?.data?.error ||
          apiError.message ||
          "Booking failed. The slot may already be taken."
      );
    } finally {
      inFlight.current = false;
      setLoading(false);
    }
  };

  return (
    <div className="space-y-3">
      {error && (
        <div className="space-y-3">
          <div className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-400">
            {error}
          </div>
          {isPlanLimitError ? (
            <div className="rounded-2xl border border-emerald-500/25 bg-emerald-500/10 p-4 text-sm shadow-lg shadow-emerald-950/20">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="min-w-0">
                  <p className="font-bold text-emerald-200">Upgrade your plan to continue booking</p>
                  <p className="mt-1 text-xs leading-5 text-slate-400">
                    Choose a higher player plan to increase or remove your monthly booking limit.
                  </p>
                </div>
                <div className="flex shrink-0 flex-col gap-2 sm:flex-row">
                  <button
                    type="button"
                    onClick={() => router.push("/pricing")}
                    className="rounded-xl bg-emerald-500 px-4 py-2 text-xs font-bold text-white transition hover:bg-emerald-400"
                  >
                    Upgrade Plan
                  </button>
                  <button
                    type="button"
                    onClick={() => router.push("/subscription")}
                    className="rounded-xl border border-white/10 px-4 py-2 text-xs font-bold text-slate-200 transition hover:bg-white/10"
                  >
                    View Plan
                  </button>
                </div>
              </div>
            </div>
          ) : null}
        </div>
      )}
      <button
        onClick={handleBooking}
        disabled={loading}
        className="w-full bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 disabled:opacity-50 disabled:cursor-not-allowed text-white font-bold py-4 rounded-2xl transition-all duration-200 shadow-lg shadow-emerald-500/25 hover:shadow-emerald-500/40 hover:-translate-y-0.5 text-base"
      >
        {loading ? (
          <span className="flex items-center justify-center gap-2">
            <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24" fill="none">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
            </svg>
            Confirming booking...
          </span>
        ) : (
          "Confirm booking →"
        )}
      </button>
      <p className="text-center text-slate-500 text-xs">
        You won&apos;t be charged yet. Payment happens on the next step.
      </p>
    </div>
  );
}
