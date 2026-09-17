"use client";
import { TileInfo } from "@/components/ui";

import { useCallback, useEffect, useMemo, useState } from "react";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import { getOwnerTurfs } from "@/modules/owner/services/ownerService";
import {
  cancelSubscription,
  downloadInvoice,
  getCurrentSubscription,
  getMyInvoices,
  getPricingPage,
  setAutoRenew,
  startSubscription,
} from "@/modules/membership/services/subscriptionService";
import { getWallet } from "@/modules/wallet/services/walletService";
import type { Invoice, MembershipPlan, Subscription } from "@/modules/membership/types";
import { formatDateTime } from "@/utils/dateFormatter";

const OWNER_SUBSCRIPTION_TYPE = "OWNER_FEATURED" as const;

type TurfOption = {
  id: number;
  name: string;
};

type TurfApiItem = {
  id?: number;
  name?: string;
};

const currency = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 0,
});

export default function OwnerSubscriptionCenter() {
  const [subscription, setSubscription] = useState<Subscription | null>(null);
  const [plans, setPlans] = useState<MembershipPlan[]>([]);
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [turfs, setTurfs] = useState<TurfOption[]>([]);
  const [selectedTurfId, setSelectedTurfId] = useState("");
  const [walletBalance, setWalletBalance] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [busyPlanId, setBusyPlanId] = useState<number | null>(null);
  const [message, setMessage] = useState<{ text: string; type: "success" | "error" } | null>(null);

  // Confirmation Modal state
  const [selectedPlanForPurchase, setSelectedPlanForPurchase] = useState<MembershipPlan | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Auto-renew confirmation modal state
  const [confirmAutoRenewModal, setConfirmAutoRenewModal] = useState<"ENABLE" | "DISABLE" | null>(null);
  const [isTogglingAutoRenew, setIsTogglingAutoRenew] = useState(false);

  const selectedTurfName = useMemo(
    () => turfs.find((turf) => String(turf.id) === selectedTurfId)?.name || subscription?.turfName || "No turf selected",
    [selectedTurfId, subscription?.turfName, turfs]
  );

  const paidInvoiceTotal = useMemo(
    () => invoices.reduce((total, invoice) => total + Number(invoice.totalAmount || 0), 0),
    [invoices]
  );

  const loadData = useCallback(async () => {
    setLoading(true);
    setMessage(null);
    try {
      const [subscriptionResult, invoiceResult, pricingResult, turfResult, walletResult] = await Promise.allSettled([
        getCurrentSubscription(OWNER_SUBSCRIPTION_TYPE),
        getMyInvoices(OWNER_SUBSCRIPTION_TYPE),
        getPricingPage(OWNER_SUBSCRIPTION_TYPE),
        getOwnerTurfs(),
        getWallet(),
      ]);

      if (subscriptionResult.status === "fulfilled") {
        setSubscription(subscriptionResult.value.data);
      } else {
        setSubscription(null);
      }

      if (walletResult.status === "fulfilled") {
        setWalletBalance(Number(walletResult.value.data.balance ?? 0));
      }

      setInvoices(invoiceResult.status === "fulfilled" ? invoiceResult.value.data || [] : []);
      setPlans(
        pricingResult.status === "fulfilled"
          ? (pricingResult.value.data.plans || []).filter((plan: MembershipPlan) => plan.type === OWNER_SUBSCRIPTION_TYPE)
          : []
      );

      if (turfResult.status === "fulfilled") {
        const ownerTurfs = (turfResult.value.data || [])
          .filter((item: TurfApiItem) => item.id && item.name)
          .map((item: TurfApiItem) => ({ id: Number(item.id), name: String(item.name) }));
        setTurfs(ownerTurfs);
        if (ownerTurfs.length > 0) {
          const activeTurfId = subscriptionResult.status === "fulfilled" && subscriptionResult.value.data?.turfId
            ? String(subscriptionResult.value.data.turfId)
            : String(ownerTurfs[0].id);
          setSelectedTurfId((current) => current || activeTurfId);
        }
      } else {
        setTurfs([]);
      }

      if (subscriptionResult.status === "rejected" || pricingResult.status === "rejected") {
        setMessage({ text: "Owner subscription data could not be fully loaded.", type: "error" });
      }
    } catch (err) {
      console.error("Error loading owner subscription data:", err);
      setMessage({ text: "Unable to load owner subscription controls.", type: "error" });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadData();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [loadData]);

  const hasActivePaidPlan = useMemo(() => {
    if (!subscription || subscription.status !== "ACTIVE" || !subscription.endDate) return false;
    const isExpired = new Date(subscription.endDate).getTime() <= Date.now();
    const isPaid = Number(subscription.price || 0) > 0;
    return !isExpired && isPaid;
  }, [subscription]);

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

  const handleConfirmActivatePlan = async () => {
    if (!selectedPlanForPurchase) return;
    const plan = selectedPlanForPurchase;
    setIsSubmitting(true);
    setMessage(null);

    try {
      const turfId = selectedTurfId ? Number(selectedTurfId) : undefined;
      await startSubscription(plan.id, turfId);
      const newExpiry = calculatePreviewExpiry(plan.durationDays);
      setMessage({
        text: `🎉 ${plan.name} owner subscription activated! Valid until ${newExpiry}.`,
        type: "success",
      });
      setSelectedPlanForPurchase(null);
      await loadData();
    } catch (err: unknown) {
      setMessage({ text: errorMessage(err, "Could not activate owner subscription."), type: "error" });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleConfirmToggleAutoRenew = async () => {
    if (!confirmAutoRenewModal) return;
    const targetEnable = confirmAutoRenewModal === "ENABLE";
    setIsTogglingAutoRenew(true);
    setMessage(null);
    try {
      await setAutoRenew(targetEnable, OWNER_SUBSCRIPTION_TYPE);
      setMessage({
        text: targetEnable
          ? "Owner plan auto-renewal successfully enabled!"
          : "Owner plan auto-renewal turned off. Your turf benefits remain active until the expiration date.",
        type: "success",
      });
      setConfirmAutoRenewModal(null);
      await loadData();
    } catch (err: unknown) {
      setMessage({ text: errorMessage(err, "Could not update owner auto-renewal."), type: "error" });
    } finally {
      setIsTogglingAutoRenew(false);
    }
  };

  const handleDownloadInvoice = (invoice: Invoice) => {
    downloadInvoice(invoice.id)
      .then((res) => {
        const dataStr = `data:text/json;charset=utf-8,${encodeURIComponent(JSON.stringify(res.data, null, 2))}`;
        const downloadAnchor = document.createElement("a");
        downloadAnchor.setAttribute("href", dataStr);
        downloadAnchor.setAttribute("download", `owner-invoice-${invoice.invoiceNumber}.json`);
        document.body.appendChild(downloadAnchor);
        downloadAnchor.click();
        downloadAnchor.remove();
      })
      .catch((err) => console.error(err));
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <section className="theme-panel rounded-3xl p-6">
          <div className="flex flex-col gap-6 xl:flex-row xl:items-end xl:justify-between">
            <div className="max-w-3xl">
              <div className="flex items-center gap-3">
                <p className="theme-accent-owner text-xs font-black uppercase tracking-[0.34em]">Owner Subscription Center</p>
                {walletBalance !== null && (
                  <span className="inline-flex items-center gap-1 rounded-full bg-sky-500/10 px-3 py-0.5 text-xs font-bold text-sky-400 border border-sky-500/20">
                    Wallet: Rs {walletBalance}
                  </span>
                )}
              </div>
              <h1 className="theme-title mt-3 text-3xl font-black">Owner plans, featured listings and billing.
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Owner plans, featured listings and billing. information" focusable={false}>
                  Manage owner-only plans separately from player premium so portal billing, turf promotion, and invoice history do not overlap.
                </TileInfo>
              </span></h1>
            </div>
            <div className="theme-panel-soft rounded-2xl p-4">
              <label className="theme-soft text-xs font-bold uppercase tracking-wider" htmlFor="owner-subscription-turf">
                Promotion turf
              </label>
              <select
                id="owner-subscription-turf"
                value={selectedTurfId}
                onChange={(event) => setSelectedTurfId(event.target.value)}
                className="theme-input mt-2 min-w-[260px] rounded-2xl px-4 py-3 text-sm font-semibold"
              >
                {turfs.length === 0 ? (
                  <option value="">Add a turf first</option>
                ) : (
                  turfs.map((turf) => (
                    <option key={turf.id} value={turf.id}>
                      {turf.name}
                    </option>
                  ))
                )}
              </select>
            </div>
          </div>
        </section>

        {message && (
          <div
            className={`rounded-2xl border p-4 text-sm font-semibold ${
              message.type === "success"
                ? "border-emerald-500/20 bg-emerald-500/10 text-emerald-300"
                : "border-red-500/20 bg-red-500/10 text-red-300"
            }`}
          >
            {message.text}
          </div>
        )}

        {loading ? (
          <div className="theme-panel rounded-3xl py-20 text-center">
            <p className="theme-muted font-semibold animate-pulse">Loading owner subscription controls...</p>
          </div>
        ) : (
          <>
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
              <Metric
                title="Active plan"
                value={subscription?.planName || "STARTER"}
                sub={hasActivePaidPlan && subscription?.endDate ? `Valid until ${formatDateTime(subscription.endDate)}` : "Default owner plan"}
              />
              <Metric title="Linked turf" value={selectedTurfName} />
              <Metric title="Auto renewal" value={subscription?.autoRenew ? "Enabled" : "Disabled"} tone={subscription?.autoRenew ? "good" : "warn"} />
              <Metric title="Owner billing" value={currency.format(paidInvoiceTotal)} />
            </div>

            <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
              <section className="theme-panel rounded-3xl p-6">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <h2 className="theme-title text-xl font-black">Owner plan controls</h2>
                    <p className="theme-muted mt-1 text-sm">Single active plan policy. Upgrades take effect immediately.</p>
                  </div>
                  {Number(subscription?.price || 0) > 0 && (
                    subscription?.autoRenew ? (
                      <button
                        type="button"
                        onClick={() => setConfirmAutoRenewModal("DISABLE")}
                        disabled={busyPlanId !== null || isTogglingAutoRenew}
                        className="rounded-2xl border border-red-500/25 bg-red-500/10 px-4 py-2 text-xs font-bold text-red-300 transition hover:bg-red-500/20 disabled:opacity-60"
                      >
                        Turn off auto-renew
                      </button>
                    ) : (
                      <button
                        type="button"
                        onClick={() => setConfirmAutoRenewModal("ENABLE")}
                        disabled={busyPlanId !== null || isTogglingAutoRenew}
                        className="rounded-2xl border border-emerald-500/30 bg-emerald-500/15 px-4 py-2 text-xs font-bold text-emerald-300 transition hover:bg-emerald-500/25 disabled:opacity-60 shadow-sm"
                      >
                        ↻ Enable auto-renew
                      </button>
                    )
                  )}
                </div>

                <div className="mt-6 grid gap-4 lg:grid-cols-3">
                  {plans.map((plan) => {
                    const currentPrice = Number(subscription?.price || 0);
                    const planPrice = Number(plan.price || 0);

                    const isActive = hasActivePaidPlan
                      ? subscription?.planName?.toUpperCase() === plan.name.toUpperCase()
                      : plan.name.toUpperCase() === "STARTER";

                    const isDowngrade = hasActivePaidPlan && planPrice < currentPrice;
                    const isUpgrade = hasActivePaidPlan && planPrice > currentPrice;
                    const isNewPaid = !hasActivePaidPlan && planPrice > 0;

                    const needsTurf = Boolean(plan.featuredListing);
                    const turfMissing = needsTurf && !selectedTurfId;

                    return (
                      <article
                        key={plan.id}
                        className={`theme-panel-soft flex min-h-[360px] flex-col rounded-3xl p-5 transition ${
                          isActive
                            ? "border-emerald-500/40 bg-emerald-500/10 ring-1 ring-emerald-500/25"
                            : isUpgrade
                            ? "border-sky-500/30 hover:border-sky-400/50 hover:-translate-y-0.5"
                            : isDowngrade
                            ? "opacity-70"
                            : "hover:-translate-y-0.5"
                        }`}
                      >
                        <div className="flex items-start justify-between gap-3">
                          <div>
                            <p className="theme-accent-owner text-xs font-black uppercase tracking-[0.28em]">{plan.name}</p>
                            <h3 className="theme-title mt-2 text-2xl font-black">{currency.format(Number(plan.price || 0))}</h3>
                            <p className="theme-soft mt-1 text-xs">per 1 month</p>
                          </div>
                          {isActive && (
                            <span className="rounded-full bg-emerald-500/15 px-3 py-1 text-2xs font-black uppercase tracking-wide text-emerald-300">
                              Active
                            </span>
                          )}
                          {isUpgrade && (
                            <span className="rounded-full bg-sky-500/15 px-2.5 py-1 text-2xs font-black uppercase tracking-wide text-sky-300">
                              Upgrade
                            </span>
                          )}
                        </div>

                        <p className="theme-muted mt-4 min-h-[54px] text-sm">{ownerPlanDescription(plan)}</p>
                        <ul className="mt-5 flex-1 space-y-3 text-sm">
                          {ownerBenefits(plan).map((benefit) => (
                            <li key={benefit} className="theme-title flex items-start gap-2">
                              <span className="mt-0.5 inline-flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-emerald-500/15 text-xs font-black text-emerald-300">✓</span>
                              <span>{benefit}</span>
                            </li>
                          ))}
                        </ul>

                        <div className="mt-6 space-y-1.5">
                          {isActive ? (
                            <button
                              disabled
                              className="w-full rounded-2xl bg-emerald-500/20 border border-emerald-500/30 px-4 py-3 text-sm font-black text-emerald-300 cursor-not-allowed"
                            >
                              Current Plan
                            </button>
                          ) : isDowngrade ? (
                            <div>
                              <button
                                disabled
                                className="w-full rounded-2xl bg-white/5 border border-white/10 px-4 py-3 text-sm font-black text-slate-500 cursor-not-allowed"
                              >
                                Available After Expiry
                              </button>
                              <p className="text-[11px] text-center text-slate-500 mt-1">
                                Switch available after active plan expires
                              </p>
                            </div>
                          ) : isUpgrade ? (
                            <div>
                              <button
                                onClick={() => handleOpenConfirmModal(plan)}
                                disabled={turfMissing}
                                className="w-full rounded-2xl bg-gradient-to-r from-sky-500 to-emerald-500 px-4 py-3 text-sm font-black text-white shadow-lg shadow-sky-500/15 transition hover:opacity-90 active:scale-95 disabled:opacity-50"
                              >
                                {turfMissing ? "Select Turf to Upgrade" : `Upgrade to ${plan.name}`}
                              </button>
                              <p className="text-[11px] text-center text-sky-400 mt-1">
                                Instant upgrade • 1 month validity
                              </p>
                            </div>
                          ) : isNewPaid ? (
                            <div>
                              <button
                                onClick={() => handleOpenConfirmModal(plan)}
                                disabled={turfMissing}
                                className="w-full rounded-2xl bg-gradient-to-r from-sky-500 to-blue-600 px-4 py-3 text-sm font-black text-white shadow-lg shadow-sky-500/10 transition hover:opacity-90 active:scale-95 disabled:opacity-50"
                              >
                                {turfMissing ? "Select Turf to Activate" : `Activate ${plan.name}`}
                              </button>
                              <p className="text-[11px] text-center text-emerald-400 mt-1">
                                1 month validity from purchase
                              </p>
                            </div>
                          ) : (
                            <button
                              disabled
                              className="w-full rounded-2xl bg-white/5 border border-white/10 px-4 py-3 text-sm font-black text-slate-500 cursor-not-allowed"
                            >
                              Default Plan
                            </button>
                          )}
                        </div>
                      </article>
                    );
                  })}
                </div>
              </section>

              <section className="theme-panel rounded-3xl p-6">
                <h2 className="theme-title text-xl font-black">Owner billing history</h2>
                <p className="theme-muted mt-1 text-sm">Only owner plan invoices are listed here.</p>
                <div className="mt-5 max-h-[520px] space-y-3 overflow-y-auto pr-1">
                  {invoices.length === 0 ? (
                    <p className="theme-panel-soft rounded-2xl border-dashed py-10 text-center text-sm theme-soft">
                      No owner invoices yet.
                    </p>
                  ) : (
                    invoices.map((invoice) => (
                      <div key={invoice.id} className="theme-panel-soft rounded-2xl p-4">
                        <div className="flex items-start justify-between gap-3">
                          <div>
                            <p className="theme-soft text-xs font-bold">{invoice.invoiceNumber}</p>
                            <h3 className="theme-title mt-1 font-bold">{invoice.planName} Owner Plan</h3>
                            <p className="theme-soft mt-1 text-xs">{formatDateTime(invoice.createdAt)} • GST {currency.format(Number(invoice.taxAmount || 0))}</p>
                          </div>
                          <span className="rounded-full bg-emerald-500/10 px-2 py-1 text-2xs font-black uppercase text-emerald-300">
                            {invoice.status}
                          </span>
                        </div>
                        <div className="mt-4 flex items-center justify-between gap-3">
                          <span className="theme-title text-lg font-black">{currency.format(Number(invoice.totalAmount || 0))}</span>
                          <button
                            onClick={() => handleDownloadInvoice(invoice)}
                            className="rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-xs font-bold theme-title transition hover:bg-white/10"
                          >
                            Download JSON
                          </button>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </section>
            </div>
          </>
        )}
      </div>

      {/* Owner Subscription Confirmation Modal */}
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
              <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-sky-500/10 border border-sky-500/20 text-sky-400 shadow-lg shadow-sky-500/5">
                <svg className="h-7 w-7" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.75}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12c0 1.268-.63 2.39-1.593 3.068a3.745 3.745 0 01-1.043 3.296 3.745 3.745 0 01-3.296 1.043A3.745 3.745 0 0112 21c-1.268 0-2.39-.63-3.068-1.593a3.746 3.746 0 01-3.296-1.043 3.745 3.745 0 01-1.043-3.296A3.745 3.745 0 013 12c0-1.268.63-2.39 1.593-3.068a3.745 3.745 0 011.043-3.296 3.746 3.746 0 013.296-1.043A3.746 3.746 0 0112 3c1.268 0 2.39.63 3.068 1.593a3.746 3.746 0 013.296 1.043 3.746 3.746 0 011.043 3.296A3.745 3.745 0 0121 12z" />
                </svg>
              </div>
              <h3 className="text-xl font-bold tracking-tight text-white mt-2">
                {hasActivePaidPlan ? `Upgrade to ${selectedPlanForPurchase.name}` : `Activate ${selectedPlanForPurchase.name}`}
              </h3>
              <p className="text-xs text-slate-300 max-w-sm mx-auto leading-relaxed">
                {hasActivePaidPlan
                  ? `Upgrading will immediately activate your ${selectedPlanForPurchase.name} tier for ${selectedTurfName}.`
                  : `Review your subscription details and wallet deduction for ${selectedTurfName}.`}
              </p>
            </div>

            {/* Plan Details & Exact Validity Box */}
            <div className="rounded-xl border border-white/10 bg-white/[0.03] p-3.5 space-y-2.5 text-xs">
              <div className="flex justify-between items-center">
                <span className="text-slate-400">Selected Tier:</span>
                <span className="font-bold text-sky-400">{selectedPlanForPurchase.name}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-400">Target Venue:</span>
                <span className="font-medium text-white truncate max-w-[180px]">{selectedTurfName}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-400">Subscription Fee:</span>
                <span className="text-sm font-bold text-white">{currency.format(Number(selectedPlanForPurchase.price || 0))}</span>
              </div>
              <div className="border-t border-white/5 pt-2 flex justify-between items-center">
                <span className="text-slate-400">Valid Until:</span>
                <span className="font-semibold text-emerald-400">
                  {calculatePreviewExpiry(selectedPlanForPurchase.durationDays)}
                </span>
              </div>
            </div>

            {/* Wallet Breakdown */}
            <div className="rounded-xl border border-sky-500/20 bg-sky-950/20 p-3.5 space-y-2 text-xs">
              <div className="flex justify-between items-center">
                <span className="text-slate-300">Wallet Balance:</span>
                <span className="font-bold text-white">Rs {walletBalance ?? 0}</span>
              </div>
              <div className="flex justify-between items-center text-rose-300">
                <span>Fee Deduction:</span>
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
                onClick={handleConfirmActivatePlan}
                disabled={isSubmitting}
                className="flex-1 rounded-xl bg-gradient-to-r from-sky-500 to-blue-600 py-2.5 text-xs font-bold text-white shadow-lg shadow-sky-500/20 hover:opacity-90 active:scale-95 transition disabled:opacity-50 flex items-center justify-center gap-2"
              >
                {isSubmitting ? (
                  <>
                    <span className="h-3.5 w-3.5 rounded-full border-2 border-white/30 border-t-white animate-spin"></span>
                    Activating...
                  </>
                ) : (
                  `Confirm & Pay ${currency.format(Number(selectedPlanForPurchase.price || 0))}`
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Auto-Renewal Confirmation Dialog */}
      {confirmAutoRenewModal && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/35 backdrop-blur-[2px] p-4 animate-in fade-in duration-150"
          onClick={(e) => {
            if (e.target === e.currentTarget && !isTogglingAutoRenew) {
              setConfirmAutoRenewModal(null);
            }
          }}
        >
          <div className="relative w-full max-w-md rounded-2xl border border-slate-700/60 bg-[#0f172a]/95 p-6 sm:p-7 shadow-2xl shadow-black/40 space-y-5 text-white">
            <button
              onClick={() => setConfirmAutoRenewModal(null)}
              disabled={isTogglingAutoRenew}
              aria-label="Close dialog"
              className="absolute top-4 right-4 rounded-lg p-1.5 text-slate-400 hover:text-white hover:bg-white/10 transition"
            >
              <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>

            {/* Centered Icon Badge */}
            {confirmAutoRenewModal === "ENABLE" ? (
              <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 shadow-lg shadow-emerald-500/5">
                <svg className="h-7 w-7" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.75}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
              </div>
            ) : (
              <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-rose-500/10 border border-rose-500/20 text-rose-400 shadow-lg shadow-rose-500/5">
                <svg className="h-7 w-7" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.75}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
                </svg>
              </div>
            )}

            {/* Header Content */}
            <div className="text-center space-y-1.5">
              <h3 className="text-xl font-bold tracking-tight text-white">
                {confirmAutoRenewModal === "ENABLE" ? "Enable Automatic Renewal?" : "Turn Off Automatic Renewal?"}
              </h3>
              <p className="text-xs text-slate-300 max-w-sm mx-auto leading-relaxed">
                {confirmAutoRenewModal === "ENABLE"
                  ? `Your ${subscription?.planName} owner plan will renew automatically on ${formatDateTime(subscription?.endDate)} using your wallet balance.`
                  : `Your venue listing benefits remain fully active until ${formatDateTime(subscription?.endDate)}, but the plan will expire without any automatic deductions.`}
              </p>
            </div>

            {/* Plan Info Box */}
            <div className="rounded-xl border border-white/10 bg-white/[0.03] p-3.5 space-y-2 text-xs">
              <div className="flex justify-between items-center">
                <span className="text-slate-400">Current Plan:</span>
                <span className="font-bold text-white">{subscription?.planName} Plan</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-400">Perks Valid Until:</span>
                <span className="font-semibold text-emerald-400">{formatDateTime(subscription?.endDate)}</span>
              </div>
              <div className="flex justify-between items-center border-t border-white/5 pt-2">
                <span className="text-slate-400">Auto-Renewal:</span>
                <span className={`font-bold ${confirmAutoRenewModal === "ENABLE" ? "text-emerald-400" : "text-rose-400"}`}>
                  {confirmAutoRenewModal === "ENABLE" ? "Will be Enabled" : "Will be Disabled"}
                </span>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex gap-3 pt-1">
              <button
                type="button"
                onClick={() => setConfirmAutoRenewModal(null)}
                disabled={isTogglingAutoRenew}
                className="flex-1 rounded-xl border border-white/10 bg-white/5 py-2.5 text-xs font-semibold text-slate-300 hover:bg-white/10 hover:text-white transition active:scale-95 disabled:opacity-50"
              >
                {confirmAutoRenewModal === "ENABLE" ? "Keep Disabled" : "Keep Auto-Renew"}
              </button>
              <button
                type="button"
                onClick={handleConfirmToggleAutoRenew}
                disabled={isTogglingAutoRenew}
                className={`flex-1 rounded-xl py-2.5 text-xs font-bold text-white shadow-lg transition active:scale-95 disabled:opacity-50 flex items-center justify-center gap-2 ${
                  confirmAutoRenewModal === "ENABLE"
                    ? "bg-gradient-to-r from-emerald-500 to-teal-500 hover:opacity-90 shadow-emerald-500/20"
                    : "bg-rose-600 hover:bg-rose-500 shadow-rose-600/20"
                }`}
              >
                {isTogglingAutoRenew ? (
                  <span className="h-3.5 w-3.5 rounded-full border-2 border-white/30 border-t-white animate-spin"></span>
                ) : confirmAutoRenewModal === "ENABLE" ? (
                  "Confirm & Enable"
                ) : (
                  "Yes, Turn Off"
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}

function Metric({ title, value, tone, sub }: { title: string; value: string; tone?: "good" | "warn"; sub?: string }) {
  return (
    <div className="theme-panel rounded-3xl p-5">
      <p className="theme-soft text-xs font-black uppercase tracking-[0.22em]">{title}</p>
      <p className={`mt-3 truncate text-2xl font-black ${tone === "good" ? "text-emerald-300" : tone === "warn" ? "text-amber-300" : "theme-title"}`}>
        {value}
      </p>
      {sub && <p className="mt-1 text-xs text-slate-400 truncate">{sub}</p>}
    </div>
  );
}

function ownerBenefits(plan: MembershipPlan) {
  const name = plan.name.toUpperCase();
  const description = plan.description.toLowerCase();
  if (name === "STARTER") {
    return ["Manage 1 turf profile", "Basic booking operations", "Owner billing history"];
  }
  if (name === "BUSINESS") {
    return ["Featured listing boost", "Manage up to 5 turfs", "Campaign and promotion access", "Business reporting"];
  }

  const benefits = ["Owner billing history"];
  benefits.push(plan.featuredListing ? "Featured listing boost" : "Standard turf listing");

  if (name.includes("ENTERPRISE") || description.includes("unlimited")) {
    benefits.push("Unlimited turf listings");
  } else if (description.includes("5") || name.includes("BUSINESS")) {
    benefits.push("Manage up to 5 turfs");
  } else {
    benefits.push("Manage owner turf profiles");
  }

  if (description.includes("campaign") || plan.featuredListing) {
    benefits.push("Campaign promotions");
  }

  if (description.includes("report") || description.includes("analytic") || name.includes("ENTERPRISE")) {
    benefits.push("Advanced financial reports");
  } else {
    benefits.push("Business reporting");
  }

  if (description.includes("api") || name.includes("ENTERPRISE")) {
    benefits.push("API access readiness");
  }

  return Array.from(new Set(benefits)).slice(0, 5);
}

function ownerPlanDescription(plan: MembershipPlan) {
  const name = plan.name.toUpperCase();
  const description = plan.description.toLowerCase();
  if (name === "STARTER") {
    return "Start with basic owner billing and one turf profile.";
  }
  if (name === "BUSINESS") {
    return "Boost turf visibility, campaigns, and owner business reporting.";
  }
  if (name === "ENTERPRISE") {
    return "Scale unlimited turf operations with advanced reports and API readiness.";
  }
  if (plan.featuredListing || description.includes("promoted") || description.includes("featured")) {
    return "Promote turf visibility and manage owner subscription billing.";
  }
  return "Owner portal subscription plan for turf operations and billing controls.";
}

function errorMessage(err: unknown, fallback: string) {
  if (typeof err === "object" && err !== null && "response" in err) {
    const response = (err as { response?: { data?: { error?: string; message?: string } } }).response;
    return response?.data?.error || response?.data?.message || fallback;
  }
  return fallback;
}
