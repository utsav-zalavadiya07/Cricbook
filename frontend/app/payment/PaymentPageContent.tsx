"use client";

import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useState } from "react";
import PaymentSummary from "@/modules/payment/components/PaymentSummary";
import RazorpayButton from "@/modules/payment/components/RazorpayButton";
import { payWithWallet } from "@/modules/payment/services/paymentService";

type PaymentMethod = "RAZORPAY" | "WALLET";

export default function PaymentPageContent() {
  const router = useRouter();
  const params = useSearchParams();
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>("RAZORPAY");
  const [walletLoading, setWalletLoading] = useState(false);
  const [walletError, setWalletError] = useState("");

  const bookingId = Number(params.get("bookingId"));
  const bookingIds = (params.get("bookingIds") ?? (bookingId ? String(bookingId) : ""))
    .split(",")
    .map((value) => Number(value))
    .filter(Boolean);
  const price = Number(params.get("price") ?? 0);
  const turfName = params.get("turfName") ?? undefined;
  const date = params.get("date") ?? undefined;
  const startTime = params.get("startTime") ?? undefined;
  const endTime = params.get("endTime") ?? undefined;

  if (!bookingId || bookingIds.length === 0) {
    return (
      <div className="theme-page-player flex min-h-screen items-center justify-center">
        <div className="space-y-4 text-center">
          <p className="theme-muted">No booking found.</p>
          <Link href="/search" className="text-sm text-emerald-600 hover:text-emerald-500 dark:text-emerald-300">
            Back to Search
          </Link>
        </div>
      </div>
    );
  }

  const methodLabel = paymentMethod === "WALLET"
    ? "Cricbook Wallet"
    : "Razorpay (UPI / Card / Netbanking)";

  const handleWalletPayment = async () => {
    if (walletLoading) return;
    setWalletLoading(true);
    setWalletError("");
    try {
      if (bookingIds.length > 1) {
        throw new Error("Wallet payment supports one booking at a time right now.");
      }
      await payWithWallet(bookingId);
      router.push(`/payment/success?bookingId=${bookingId}&method=wallet`);
    } catch (error: unknown) {
      const apiError = error as { response?: { data?: { message?: string } }; message?: string };
      setWalletError(apiError.response?.data?.message ?? apiError.message ?? "Wallet payment failed.");
    } finally {
      setWalletLoading(false);
    }
  };

  return (
    <div className="theme-page-player min-h-screen">
      <div className="mx-auto max-w-lg space-y-6 px-6 py-10">
        <div>
          <h1 className="theme-title text-2xl font-bold">Complete Payment</h1>
          <p className="theme-muted mt-1 text-sm">
            Your slot is held. Complete payment within <span className="font-medium text-amber-600 dark:text-amber-300">5 minutes</span>.
          </p>
        </div>

        <div className="flex items-center gap-3 rounded-xl border border-amber-500/20 bg-amber-500/10 px-4 py-3">
          <span className="flex h-9 w-9 items-center justify-center rounded-full bg-amber-500/15 text-sm font-bold text-amber-700 dark:text-amber-300">5</span>
          <div>
            <p className="text-sm font-medium text-amber-700 dark:text-amber-300">Slot reservation active</p>
            <p className="theme-soft text-xs">Slot will be released if payment is not completed</p>
          </div>
        </div>

        <div className="theme-panel rounded-2xl p-4">
          <p className="theme-title text-sm font-semibold">Choose payment method</p>
          <div className="mt-3 grid gap-3 sm:grid-cols-2">
            <PaymentMethodCard
              active={paymentMethod === "RAZORPAY"}
              title="UPI / Card / Net Banking"
              body="Pay securely through Razorpay."
              onClick={() => setPaymentMethod("RAZORPAY")}
            />
            <PaymentMethodCard
              active={paymentMethod === "WALLET"}
              title="Cricbook Wallet"
              body="Use available wallet balance."
              onClick={() => setPaymentMethod("WALLET")}
            />
          </div>
        </div>

        <PaymentSummary
          amount={price}
          bookingId={bookingId}
          bookingIds={bookingIds}
          turfName={turfName}
          date={date}
          startTime={startTime}
          endTime={endTime}
          paymentMethod={methodLabel}
        />

        {paymentMethod === "RAZORPAY" ? (
          <RazorpayButton bookingId={bookingId} bookingIds={bookingIds} amount={price} turfName={turfName} />
        ) : (
          <div className="space-y-3">
            {walletError && (
              <div className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">
                {walletError}
              </div>
            )}
            <button
              onClick={handleWalletPayment}
              disabled={walletLoading}
              className="w-full rounded-2xl bg-gradient-to-r from-emerald-500 to-teal-500 py-4 text-base font-bold text-white shadow-lg shadow-emerald-500/25 transition hover:from-emerald-400 hover:to-teal-400 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {walletLoading ? "Processing wallet payment..." : `Pay Rs ${price} from Wallet`}
            </button>
          </div>
        )}

        <p className="theme-soft text-center text-xs">
          Having trouble?{" "}
          <Link href="/booking/history" className="text-emerald-600 transition hover:text-emerald-500 dark:text-emerald-300">
            View booking history
          </Link>
        </p>
      </div>
    </div>
  );
}

function PaymentMethodCard({
  active,
  title,
  body,
  onClick,
}: {
  active: boolean;
  title: string;
  body: string;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-2xl border p-4 text-left transition ${
        active
          ? "border-emerald-400/60 bg-emerald-500/15"
          : "theme-panel-soft hover:border-emerald-400/40"
      }`}
    >
      <span className="theme-title text-sm font-semibold">{title}</span>
      <span className="theme-muted mt-1 block text-xs">{body}</span>
    </button>
  );
}
