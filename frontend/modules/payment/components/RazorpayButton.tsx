"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/authStore";
import { createBatchOrder, createOrder, verifyPayment } from "../services/paymentService";

declare global {
  interface Window {
    Razorpay: new (options: RazorpayOptions) => RazorpayInstance;
  }
}

type RazorpayFailure = {
  error: {
    description: string;
  };
};

type RazorpayInstance = {
  on: (event: "payment.failed", handler: (res: RazorpayFailure) => void) => void;
  open: () => void;
};

type RazorpayOptions = {
  key: string;
  amount: number;
  currency: string;
  name: string;
  description: string;
  image: string;
  order_id: string;
  handler: (response: {
    razorpay_order_id: string;
    razorpay_payment_id: string;
    razorpay_signature: string;
  }) => Promise<void>;
  modal?: {
    ondismiss?: () => void;
    backdropclose?: boolean;
    escape?: boolean;
    confirm_close?: boolean;
    animation?: boolean;
  };
  prefill?: {
    name?: string;
    email?: string;
    contact?: string;
    method?: string;
    vpa?: string;
  };
  theme?: {
    color?: string;
    backdrop_color?: string;
  };
  config?: Record<string, unknown>;
};

type ApiError = {
  response?: {
    data?: {
      message?: string;
    };
  };
  message?: string;
};

interface RazorpayButtonProps {
  bookingId: number;
  bookingIds?: number[];
  amount: number;
  turfName?: string;
}

const RAZORPAY_KEY = process.env.NEXT_PUBLIC_RAZORPAY_KEY_ID ?? "";

const loadScript = (src: string): Promise<boolean> =>
  new Promise((resolve) => {
    if (document.querySelector(`script[src="${src}"]`)) {
      resolve(true);
      return;
    }
    const script = document.createElement("script");
    script.src = src;
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });

export default function RazorpayButton({ bookingId, bookingIds = [bookingId], amount, turfName }: RazorpayButtonProps) {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const inFlight = useRef(false);
  const authUser = useAuthStore((s) => s.user);

  const handlePayment = async () => {
    if (inFlight.current) return;
    inFlight.current = true;
    setLoading(true);
    setError("");

    try {
      // 1. Load Razorpay SDK
      const loaded = await loadScript("https://checkout.razorpay.com/v1/checkout.js");
      if (!loaded) throw new Error("Razorpay SDK failed to load.");

      // 2. Create order on backend
      const orderRes = bookingIds.length > 1
        ? await createBatchOrder(bookingIds)
        : await createOrder(bookingId);
      const { orderId, currency, amount: orderAmount } = orderRes.data;

      // 3. Open Razorpay checkout
      const options = {
        key: RAZORPAY_KEY,
        amount: orderAmount, // in paise (backend sends this)
        currency: currency ?? "INR",
        name: "Cricbook",
        description: turfName ? `Booking – ${turfName}` : "Turf Booking",
        image: "/favicon.ico",
        order_id: orderId,

        handler: async (response: {
          razorpay_order_id: string;
          razorpay_payment_id: string;
          razorpay_signature: string;
        }) => {
          try {
            await verifyPayment({
              orderId: response.razorpay_order_id,
              paymentId: response.razorpay_payment_id,
              signature: response.razorpay_signature,
              bookingId,
              bookingIds,
            });
            const successParams = new URLSearchParams({
              bookingId: String(bookingId),
              bookingIds: bookingIds.join(","),
            });
            router.push(`/payment/success?${successParams.toString()}`);
          } catch {
            setError("Payment verification failed. Please contact support.");
            inFlight.current = false;
            setLoading(false);
          }
        },

        modal: {
          backdropclose: true,
          confirm_close: true,
          escape: true,
          animation: true,
          ondismiss: () => {
            inFlight.current = false;
            setLoading(false);
            setError("Payment cancelled. Your booking is held for 5 minutes.");
          },
        },

        prefill: {
          name: authUser?.name || "Cricbook Player",
          email: authUser?.email || "player@cricbook.com",
          contact: "6356444321",
          method: "upi",
        },

        theme: {
          color: "#00c853",
          backdrop_color: "rgba(7, 17, 30, 0.75)",
        },

        config: {
          display: {
            blocks: {
              upi: {
                name: "Pay using UPI / QR",
                instruments: [
                  {
                    method: "upi",
                    flows: ["qr", "intent", "collect"],
                  },
                ],
              },
            },
            sequence: ["block.upi"],
            preferences: {
              show_default_blocks: true,
            },
          },
        },
      };

      const rzp = new window.Razorpay(options);
      rzp.on("payment.failed", (res) => {
        setError(`Payment failed: ${res.error.description}`);
        inFlight.current = false;
        setLoading(false);
      });
      rzp.open();
    } catch (err: unknown) {
      const apiError = err as ApiError;
      setError(apiError.response?.data?.message ?? apiError.message ?? "Payment initiation failed.");
      inFlight.current = false;
      setLoading(false);
    }
  };

  return (
    <div className="space-y-3">
      {error && (
        <div className="bg-red-500/10 border border-red-500/30 text-red-400 text-sm px-4 py-3 rounded-xl">
          {error}
        </div>
      )}

      <button
        onClick={handlePayment}
        disabled={loading}
        className="w-full bg-gradient-to-r from-violet-500 to-purple-600 hover:from-violet-400 hover:to-purple-500 disabled:opacity-50 disabled:cursor-not-allowed text-white font-bold py-4 rounded-2xl transition-all duration-200 shadow-lg shadow-violet-500/25 hover:shadow-violet-500/40 hover:-translate-y-0.5 text-base"
      >
        {loading ? (
          <span className="flex items-center justify-center gap-2">
            <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24" fill="none">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
            </svg>
            Opening payment...
          </span>
        ) : (
          <span className="flex items-center justify-center gap-2">
            <span>💳</span>
            Pay ₹{amount.toLocaleString("en-IN")} with Razorpay
          </span>
        )}
      </button>

      {/* Payment methods hint */}
      <div className="flex items-center justify-center gap-3 text-slate-600 text-xs">
        <span>UPI</span><span>·</span>
        <span>Credit / Debit Card</span><span>·</span>
        <span>Net Banking</span><span>·</span>
        <span>Wallets</span>
      </div>
    </div>
  );
}
