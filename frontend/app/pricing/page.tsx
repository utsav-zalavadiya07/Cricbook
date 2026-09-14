"use client";
import { TileInfo } from "@/components/ui";

import { useEffect, useState } from "react";
import { getPricingPage, startSubscription } from "@/modules/membership/services/subscriptionService";
import type { MembershipPlan } from "@/modules/membership/types";

export default function PricingPage() {
  const [plans, setPlans] = useState<MembershipPlan[]>([]);
  const [variant, setVariant] = useState<string>("A");
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<{ text: string; type: "success" | "error" } | null>(null);

  useEffect(() => {
    getPricingPage("USER_PREMIUM")
      .then((res) => {
        setPlans(res.data.plans);
        setVariant(res.data.variant);
      })
      .catch((err) => console.error(err))
      .finally(() => setLoading(false));
  }, []);

  const handleSubscribe = (planId: number) => {
    setMessage(null);
    startSubscription(planId)
      .then(() => {
        setMessage({
          text: "Subscription successfully activated! Cricbook Premium benefits unlocked.",
          type: "success",
        });
      })
      .catch((err) => {
        const errorMsg = err.response?.data?.error || "Failed to start subscription.";
        setMessage({ text: errorMsg, type: "error" });
      });
  };

  return (
    <main className="theme-page-player min-h-screen px-4 py-6 sm:px-6 lg:px-8">
      <div className="w-full space-y-10">
        <div className="text-center space-y-4">
          <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-500/10 px-3 py-1 text-xs font-semibold text-emerald-400 border border-emerald-500/20">
            A/B Pricing Variant: <span className="font-extrabold">{variant}</span>
          </span>
          <h1 className="text-4xl font-extrabold tracking-tight text-white sm:text-5xl">Choose Your <span className="bg-gradient-to-r from-emerald-400 to-teal-300 bg-clip-text text-transparent">Power Tier</span>
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Choose Your Power Tier information" focusable={false}>Unlock priority booking slots, advanced report counters, turf search prioritization, and custom campaigns.</TileInfo>
              </span></h1>

          <p className="mx-auto max-w-2xl text-xs font-semibold uppercase tracking-[0.28em] text-emerald-300">
            Player premium only
          </p>
        </div>

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

              return (
                <div
                  key={plan.id}
                  className="relative flex flex-col rounded-3xl border border-white/10 bg-white/[0.03] p-8 shadow-2xl backdrop-blur-xl transition-all duration-300 hover:scale-[1.03] hover:border-emerald-500/30 hover:bg-white/[0.05]"
                >
                  {showDiscountLabel && (
                    <span className="absolute -top-3.5 right-6 inline-flex rounded-full bg-rose-500 px-3 py-1 text-2xs font-extrabold uppercase tracking-wide text-white animate-bounce">
                      Special 15% Off
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
                      <span className="text-xs text-slate-500">/ {plan.durationDays} days</span>
                      {showDiscountLabel && (
                        <span className="ml-2 text-sm line-through text-slate-500">
                          Rs {plan.originalPrice}
                        </span>
                      )}
                    </div>
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

                  <button
                    onClick={() => handleSubscribe(plan.id)}
                    className="mt-8 w-full rounded-2xl bg-gradient-to-r from-emerald-500 to-teal-500 py-3 text-sm font-bold text-white transition-all hover:opacity-90 active:scale-95 shadow-lg shadow-emerald-500/10"
                  >
                    Activate Plan
                  </button>
                </div>
              );
            })}
          </div>
        )}
      </div>
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
