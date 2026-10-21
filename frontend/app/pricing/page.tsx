"use client";
import { TileInfo } from "@/components/ui";
import { useCallback, useEffect, useMemo, useState } from "react";
import { getPricingPage, getCurrentSubscription, startSubscription } from "@/modules/membership/services/subscriptionService";
import { TaxInclusiveNote } from "@/modules/membership/components/TaxInclusiveNote";
import { getWallet } from "@/modules/wallet/services/walletService";
import type { MembershipPlan, Subscription } from "@/modules/membership/types";
import { formatDateTime } from "@/utils/dateFormatter";

export default function PricingPage() {
  const [plans, setPlans] = useState<MembershipPlan[]>([]);
  const [variant, setVariant] = useState<string>("A");
  const [currentSub, setCurrentSub] = useState<Subscription | null>(null);
  const [walletBalance, setWalletBalance] = useState<number | null>(null);
  const [loadedAt, setLoadedAt] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<{ text: string; type: "success" | "error" } | null>(null);

  // Confirmation Modal state
  const [selectedPlanForPurchase, setSelectedPlanForPurchase] = useState<MembershipPlan | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [pricingResult, subResult, walletResult] = await Promise.allSettled([
        getPricingPage("USER_PREMIUM"),
        getCurrentSubscription("USER_PREMIUM"),
        getWallet(),
      ]);

      if (pricingResult.status === "fulfilled") {
        setPlans(pricingResult.value.data.plans || []);
        setVariant(pricingResult.value.data.variant || "A");
      }
      if (subResult.status === "fulfilled") {
        setCurrentSub(subResult.value.data);
      }
      if (walletResult.status === "fulfilled") {
        setWalletBalance(Number(walletResult.value.data.balance ?? 0));
      }
      setLoadedAt(Date.now());
    } catch (err) {
      console.error("Failed to load subscription pricing data", err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => void loadData(), 0);
    return () => window.clearTimeout(timer);
  }, [loadData]);

  // Check if user has an active paid plan
  const hasActivePaidPlan = useMemo(() => {
    if (!currentSub || currentSub.status !== "ACTIVE" || !currentSub.endDate || loadedAt === null) return false;
    const isExpired = new Date(currentSub.endDate).getTime() <= loadedAt;
    const isPaid = Number(currentSub.price || 0) > 0;
    return !isExpired && isPaid;
  }, [currentSub, loadedAt]);

  // Preview expiry calculation matching backend logic (exact day and time)
  const calculatePreviewExpiry = (durationDays: number): string => {
    const now = new Date();
    if (durationDays > 0 && durationDays % 30 === 0 && durationDays <= 365) {
      const months = Math.round(durationDays / 30);
      const target = new Date(now);
      target.setMonth(target.getMonth() + months);
      return formatDateTime(target.toISOString());
    } else {
      const target = new Date(now.getTime() + durationDays * 24 * 60 * 60 * 1000);
      return formatDateTime(target.toISOString());
    }
  };

  const handleOpenConfirmModal = (plan: MembershipPlan) => {
    setMessage(null);
    setSelectedPlanForPurchase(plan);
  };

  const handleConfirmSubscribe = async () => {
    if (!selectedPlanForPurchase) return;
    setIsSubmitting(true);
    setMessage(null);

    try {
      const planToActivate = selectedPlanForPurchase;
      await startSubscription(planToActivate.id);
      
      const newExpiry = calculatePreviewExpiry(planToActivate.durationDays);
      setMessage({
        text: `🎉 Congratulations! Your ${planToActivate.name} plan is now active until ${newExpiry}. Benefit perks unlocked!`,
        type: "success",
      });

      setSelectedPlanForPurchase(null);
      // Refresh page data so active plan and wallet update immediately
      await loadData();
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { message?: string; error?: string } } };
      const data = apiErr.response?.data;
      const errorMsg = data?.message || data?.error || "Failed to activate subscription. Please try again.";
      setMessage({ text: errorMsg, type: "error" });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="theme-page-player min-h-screen px-4 py-6 sm:px-6 lg:px-8">
      <div className="w-full space-y-10">
        <div className="text-center space-y-4">
          <div className="flex flex-wrap items-center justify-center gap-3">
            <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-500/10 px-3 py-1 text-xs font-semibold text-emerald-400 border border-emerald-500/20">
              A/B Pricing Variant: <span className="font-extrabold">{variant}</span>
            </span>
            {walletBalance !== null && (
              <span className="inline-flex items-center gap-1.5 rounded-full bg-sky-500/10 px-3 py-1 text-xs font-semibold text-sky-400 border border-sky-500/20">
                Wallet Balance: <span className="font-extrabold text-white">Rs {walletBalance}</span>
              </span>
            )}
          </div>

          <h1 className="text-4xl font-extrabold tracking-tight text-white sm:text-5xl">
            Choose Your <span className="bg-gradient-to-r from-emerald-400 to-teal-300 bg-clip-text text-transparent">Power Tier</span>
            <span className="inline-flex align-middle ml-3 mb-1">
              <TileInfo label="Choose Your Power Tier information" focusable={false}>
                Unlock priority booking slots, advanced report counters, turf search prioritization, and custom campaigns.
              </TileInfo>
            </span>
          </h1>

          <p className="mx-auto max-w-2xl text-xs font-semibold uppercase tracking-[0.28em] text-emerald-300">
            Player premium only • Single Active Plan Policy
          </p>
        </div>

        {/* Active Plan Status Banner */}
        {hasActivePaidPlan && currentSub && (
          <div className="mx-auto max-w-3xl rounded-3xl border border-emerald-500/30 bg-emerald-950/30 p-6 backdrop-blur-xl shadow-2xl">
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div className="space-y-1.5">
                <div className="flex items-center gap-2">
                  <span className="relative flex h-2.5 w-2.5">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                    <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500"></span>
                  </span>
                  <span className="text-xs font-black uppercase tracking-wider text-emerald-400">Current Active Plan</span>
                </div>
                <h2 className="text-2xl font-black text-white">{currentSub.planName} Membership</h2>
                <p className="text-xs text-slate-300">
                  Valid until: <span className="font-bold text-emerald-300">{formatDateTime(currentSub.endDate)}</span>
                </p>
              </div>

              <div className="flex flex-col sm:items-end text-left sm:text-right gap-1 border-t sm:border-t-0 pt-3 sm:pt-0 border-white/10">
                <span className="text-xs font-semibold text-slate-400">Policy: Single Active Plan</span>
                <span className="text-2xs text-slate-400 max-w-xs leading-relaxed">
                  You can upgrade to a higher tier at any time. Downgrading is enabled after your current plan expires.
                </span>
              </div>
            </div>
          </div>
        )}

        {message && (
          <div
            className={`mx-auto max-w-2xl rounded-2xl border p-4 text-center text-sm font-medium transition-all ${
              message.type === "success"
                ? "border-emerald-500/20 bg-emerald-500/10 text-emerald-300"
                : "border-red-500/20 bg-red-500/10 text-red-300"
            }`}
          >
            {message.text}
          </div>
        )}

        {loading ? (
          <div className="py-20 text-center text-slate-400 font-semibold animate-pulse">
            Loading subscription pricing engine...
          </div>
        ) : (
          <div className="grid gap-6 md:grid-cols-3">
            {plans.map((plan) => {
              const isPro = plan.name.toUpperCase() === "PRO";
              const isB = variant === "B";
              const showDiscountLabel = isPro && isB;

              // Determine plan relationship relative to user's active plan
              const currentPrice = Number(currentSub?.price || 0);
              const planPrice = Number(plan.price || 0);

              const isCurrentPlan = hasActivePaidPlan
                ? currentSub?.planName?.toUpperCase() === plan.name.toUpperCase()
                : plan.name.toUpperCase() === "FREE";

              const isDowngrade = hasActivePaidPlan && planPrice < currentPrice;
              const isUpgrade = hasActivePaidPlan && planPrice > currentPrice;
              const isNewPaid = !hasActivePaidPlan && planPrice > 0;

              return (
                <div
                  key={plan.id}
                  className={`relative flex flex-col rounded-3xl border p-8 shadow-2xl backdrop-blur-xl transition-all duration-300 ${
                    isCurrentPlan
                      ? "border-emerald-500/50 bg-emerald-500/[0.07] ring-2 ring-emerald-500/30"
                      : isUpgrade
                      ? "border-sky-500/40 bg-white/[0.04] hover:border-sky-400/60 hover:scale-[1.02]"
                      : isDowngrade
                      ? "border-white/5 bg-white/[0.02] opacity-75"
                      : "border-white/10 bg-white/[0.03] hover:scale-[1.02] hover:border-emerald-500/30"
                  }`}
                >
                  {showDiscountLabel && (
                    <span className="absolute -top-3.5 right-6 inline-flex rounded-full bg-rose-500 px-3 py-1 text-2xs font-extrabold uppercase tracking-wide text-white animate-bounce">
                      Special 15% Off
                    </span>
                  )}

                  {isCurrentPlan && (
                    <span className="absolute -top-3.5 left-6 inline-flex rounded-full bg-emerald-500 px-3 py-1 text-2xs font-black uppercase tracking-wide text-black shadow-lg">
                      Active Plan
                    </span>
                  )}

                  {isUpgrade && (
                    <span className="absolute -top-3.5 right-6 inline-flex rounded-full bg-sky-500 px-3 py-1 text-2xs font-black uppercase tracking-wide text-white shadow-lg shadow-sky-500/20">
                      Tier Upgrade
                    </span>
                  )}

                  <div className="space-y-4">
                    <p className="text-xs font-bold uppercase tracking-wider text-emerald-400">
                      {plan.name}
                    </p>
                    <h3 className="text-2xl font-bold text-white">{plan.name}</h3>
                    <p className="text-sm text-slate-400 min-h-[48px]">{plan.description}</p>
                    <div className="pt-4 flex items-baseline gap-1">
                      <span className="text-4xl font-black text-white">Rs {plan.price}</span>
                      <span className="text-xs text-slate-500">/ 1 month</span>
                      {showDiscountLabel && (
                        <span className="ml-2 text-sm line-through text-slate-500">
                          Rs {plan.originalPrice}
                        </span>
                      )}
                    </div>
                    {Number(plan.price || 0) > 0 && <TaxInclusiveNote className="mt-1" />}
                  </div>

                  <ul className="mt-8 space-y-3.5 text-sm text-slate-300 flex-1">
                    <li className="flex items-center gap-2">
                      <CheckIcon />
                      <span>
                        {plan.name === "FREE"
                          ? "Max 3 bookings / month"
                          : plan.name === "PRO"
                          ? "Max 15 bookings / month"
                          : "Unlimited monthly bookings"}
                      </span>
                    </li>
                    <li className="flex items-center gap-2">
                      <CheckIcon />
                      <span>{plan.discountPercent}% Direct Slot Discount</span>
                    </li>
                    {plan.prioritySlots && (
                      <li className="flex items-center gap-2">
                        <CheckIcon />
                        <span>Priority Booking Slot Access</span>
                      </li>
                    )}
                    {plan.name === "ELITE" && (
                      <>
                        <li className="flex items-center gap-2">
                          <CheckIcon />
                          <span>Exclusive Tournament Invites</span>
                        </li>
                        <li className="flex items-center gap-2">
                          <CheckIcon />
                          <span>Priority 24/7 Support Line</span>
                        </li>
                      </>
                    )}
                  </ul>

                  {/* Smart Tier Button */}
                  <div className="mt-8 space-y-2">
                    {isCurrentPlan ? (
                      <button
                        disabled
                        className="w-full flex items-center justify-center gap-2 rounded-2xl bg-emerald-500/20 border border-emerald-500/30 py-3 text-sm font-bold text-emerald-300 cursor-not-allowed"
                      >
                        <CheckIcon /> Current Plan
                      </button>
                    ) : isDowngrade ? (
                      <div>
                        <button
                          disabled
                          className="w-full rounded-2xl bg-white/5 border border-white/10 py-3 text-sm font-bold text-slate-500 cursor-not-allowed"
                        >
                          Available After Expiry
                        </button>
                        <p className="text-[11px] text-center text-slate-500 mt-1">
                          Switch after active plan expires
                        </p>
                      </div>
                    ) : isUpgrade ? (
                      <div>
                        <button
                          onClick={() => handleOpenConfirmModal(plan)}
                          className="w-full rounded-2xl bg-gradient-to-r from-sky-500 to-emerald-500 py-3 text-sm font-bold text-white transition-all hover:opacity-90 active:scale-95 shadow-lg shadow-sky-500/20"
                        >
                          Upgrade to {plan.name}
                        </button>
                        <p className="text-[11px] text-center text-sky-400/80 mt-1">
                          Instant upgrade • Valid for 1 month
                        </p>
                      </div>
                    ) : isNewPaid ? (
                      <div>
                        <button
                          onClick={() => handleOpenConfirmModal(plan)}
                          className="w-full rounded-2xl bg-gradient-to-r from-emerald-500 to-teal-500 py-3 text-sm font-bold text-white transition-all hover:opacity-90 active:scale-95 shadow-lg shadow-emerald-500/10"
                        >
                          Activate Plan
                        </button>
                        <p className="text-[11px] text-center text-emerald-400/80 mt-1">
                          Valid for 1 month from activation
                        </p>
                      </div>
                    ) : (
                      <button
                        disabled
                        className="w-full rounded-2xl bg-white/5 border border-white/10 py-3 text-sm font-bold text-slate-500 cursor-not-allowed"
                      >
                        Current Free Plan
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Confirmation & Wallet Deduction Modal */}
      {selectedPlanForPurchase && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/35 backdrop-blur-[2px] p-4 animate-in fade-in duration-150"
          onClick={(e) => {
            if (e.target === e.currentTarget && !isSubmitting) {
              setSelectedPlanForPurchase(null);
            }
          }}
        >
          <div className="relative w-full max-w-md rounded-2xl border border-slate-700/60 bg-[#0f172a]/95 p-6 sm:p-7 shadow-2xl shadow-black/40 space-y-5 text-white">
            <button
              onClick={() => setSelectedPlanForPurchase(null)}
              disabled={isSubmitting}
              aria-label="Close modal"
              className="absolute top-4 right-4 rounded-lg p-1.5 text-slate-400 hover:text-white hover:bg-white/10 transition"
            >
              <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>

            {/* Header Content */}
            <div className="text-center space-y-1.5 pt-1">
              <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 shadow-lg shadow-emerald-500/5">
                <svg className="h-7 w-7" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.75}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12c0 1.268-.63 2.39-1.593 3.068a3.745 3.745 0 01-1.043 3.296 3.745 3.745 0 01-3.296 1.043A3.745 3.745 0 0112 21c-1.268 0-2.39-.63-3.068-1.593a3.746 3.746 0 01-3.296-1.043 3.745 3.745 0 01-1.043-3.296A3.745 3.745 0 013 12c0-1.268.63-2.39 1.593-3.068a3.745 3.745 0 011.043-3.296 3.746 3.746 0 013.296-1.043A3.746 3.746 0 0112 3c1.268 0 2.39.63 3.068 1.593a3.746 3.746 0 013.296 1.043 3.746 3.746 0 011.043 3.296A3.745 3.745 0 0121 12z" />
                </svg>
              </div>
              <h3 className="text-xl font-bold tracking-tight text-white mt-2">
                {hasActivePaidPlan ? `Upgrade to ${selectedPlanForPurchase.name}` : `Activate ${selectedPlanForPurchase.name} Plan`}
              </h3>
              <p className="text-xs text-slate-300 max-w-sm mx-auto leading-relaxed">
                {hasActivePaidPlan
                  ? `Your plan will be instantly upgraded to ${selectedPlanForPurchase.name} for 1 month.`
                  : `Confirm your subscription details and wallet deduction below.`}
              </p>
            </div>

            {/* Plan Details & Exact Validity Box */}
            <div className="rounded-xl border border-white/10 bg-white/[0.03] p-3.5 space-y-2.5 text-xs">
              <div className="flex justify-between items-center">
                <span className="text-slate-400">Selected Tier:</span>
                <span className="font-bold text-emerald-400">{selectedPlanForPurchase.name}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-400">Plan price (GST included):</span>
                <span className="text-sm font-bold text-white">Rs {selectedPlanForPurchase.price}</span>
              </div>
              <div className="border-t border-white/5 pt-2 flex justify-between items-center">
                <span className="text-slate-400">Valid Until:</span>
                <span className="font-semibold text-emerald-400">
                  {calculatePreviewExpiry(selectedPlanForPurchase.durationDays)}
                </span>
              </div>
            </div>

            {/* Wallet Deduction Breakdown */}
            <div className="rounded-xl border border-sky-500/20 bg-sky-950/20 p-3.5 space-y-2 text-xs">
              <div className="flex justify-between items-center">
                <span className="text-slate-300">Wallet Balance:</span>
                <span className="font-bold text-white">Rs {walletBalance ?? 0}</span>
              </div>
              <div className="flex justify-between items-center text-rose-300">
                <span>Total plan charge:</span>
                <span className="font-bold">- Rs {selectedPlanForPurchase.price}</span>
              </div>
              <div className="border-t border-sky-500/20 pt-2 flex justify-between items-center font-bold">
                <span className="text-slate-200">Balance After Activation:</span>
                <span className={(walletBalance ?? 0) >= selectedPlanForPurchase.price ? "text-emerald-400" : "text-amber-400"}>
                  {(walletBalance ?? 0) >= selectedPlanForPurchase.price
                    ? `Rs ${(walletBalance ?? 0) - selectedPlanForPurchase.price}`
                    : `Rs 0 (Simulated payment)`}
                </span>
              </div>
            </div>

            {/* Modal Actions */}
            <div className="flex gap-3 pt-1">
              <button
                type="button"
                onClick={() => setSelectedPlanForPurchase(null)}
                disabled={isSubmitting}
                className="flex-1 rounded-xl border border-white/10 bg-white/5 py-2.5 text-xs font-semibold text-slate-300 hover:bg-white/10 hover:text-white transition active:scale-95 disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmSubscribe}
                disabled={isSubmitting}
                className="flex-1 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 py-2.5 text-xs font-bold text-white shadow-lg shadow-emerald-500/20 hover:opacity-90 active:scale-95 transition disabled:opacity-50 flex items-center justify-center gap-2"
              >
                {isSubmitting ? (
                  <>
                    <span className="h-3.5 w-3.5 rounded-full border-2 border-white/30 border-t-white animate-spin"></span>
                    Processing...
                  </>
                ) : (
                  `Confirm & Pay Rs ${selectedPlanForPurchase.price}`
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}

function CheckIcon() {
  return (
    <svg
      className="h-4.5 w-4.5 shrink-0 text-emerald-400"
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
      strokeWidth={3}
    >
      <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
    </svg>
  );
}
