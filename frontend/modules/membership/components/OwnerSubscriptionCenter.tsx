"use client";
import { Button, Select } from "@/components/ui";
import { DataGrid, type DataGridColumn } from "@/components/ui/DataGrid";
import { PageMetricCard, PageSection, PageSectionHeader } from "@/components/ui/PageSurface";
import { TaxInclusiveNote } from "@/modules/membership/components/TaxInclusiveNote";

import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import { getOwnerTurfs } from "@/modules/owner/services/ownerService";
import {
  downloadInvoicePdf,
  getCurrentSubscription,
  getMyInvoices,
  getPricingPage,
  invoiceStatusLabel,
  setAutoRenew,
  startSubscription,
} from "@/modules/membership/services/subscriptionService";
import { getWallet } from "@/modules/wallet/services/walletService";
import { useToast } from "@/modules/ui/ToastProvider";
import type { Invoice, MembershipPlan, Subscription } from "@/modules/membership/types";
import { formatDate, formatDateTime } from "@/utils/dateFormatter";

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
  const { showSuccess } = useToast();
  const [subscription, setSubscription] = useState<Subscription | null>(null);
  const [plans, setPlans] = useState<MembershipPlan[]>([]);
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [turfs, setTurfs] = useState<TurfOption[]>([]);
  const [selectedTurfId, setSelectedTurfId] = useState("");
  const [walletBalance, setWalletBalance] = useState<number | null>(null);
  const [loadedAt, setLoadedAt] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<{ text: string; type: "success" | "error" } | null>(null);
  const [downloadingInvoiceId, setDownloadingInvoiceId] = useState<number | null>(null);

  // Confirmation Modal state
  const [selectedPlanForPurchase, setSelectedPlanForPurchase] = useState<MembershipPlan | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Auto-renew confirmation modal state
  const [confirmAutoRenewModal, setConfirmAutoRenewModal] = useState<"ENABLE" | "DISABLE" | null>(null);
  const [isTogglingAutoRenew, setIsTogglingAutoRenew] = useState(false);

  const [purchaseTurfError, setPurchaseTurfError] = useState("");

  const loadData = useCallback(async () => {
    setLoading(true);
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
      setLoadedAt(Date.now());
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
    if (!subscription || subscription.status !== "ACTIVE" || !subscription.endDate || loadedAt === null) return false;
    const isExpired = new Date(subscription.endDate).getTime() <= loadedAt;
    const isPaid = Number(subscription.price || 0) > 0;
    return !isExpired && isPaid;
  }, [loadedAt, subscription]);

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
    setPurchaseTurfError("");
    setSelectedPlanForPurchase(plan);
  };

  const handleConfirmActivatePlan = async () => {
    if (!selectedPlanForPurchase) return;
    const plan = selectedPlanForPurchase;
    if (plan.featuredListing && !selectedTurfId) {
      setPurchaseTurfError("Select a turf to receive the featured listing benefit.");
      return;
    }
    setIsSubmitting(true);
    setMessage(null);

    try {
      const turfId = selectedTurfId ? Number(selectedTurfId) : undefined;
      await startSubscription(plan.id, turfId);
      const newExpiry = calculatePreviewExpiry(plan.durationDays);
      showSuccess("Subscription activated", `${plan.name} is active until ${newExpiry}.`);
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
      showSuccess(
        targetEnable ? "Auto-renew enabled" : "Auto-renew turned off",
        targetEnable
          ? `Your ${subscription?.planName || "owner"} plan will renew automatically at expiry.`
          : "Your current plan benefits remain active until the expiry date.",
      );
      setConfirmAutoRenewModal(null);
      await loadData();
    } catch (err: unknown) {
      setMessage({ text: errorMessage(err, "Could not update owner auto-renewal."), type: "error" });
    } finally {
      setIsTogglingAutoRenew(false);
    }
  };

  const handleDownloadInvoice = async (invoice: Invoice) => {
    setDownloadingInvoiceId(invoice.id);
    setMessage(null);
    try {
      await downloadInvoicePdf(invoice.id, invoice.invoiceNumber);
      showSuccess("Invoice downloaded", `${invoice.invoiceNumber} is ready in your downloads.`);
    } catch (err) {
      console.error("Unable to download invoice:", err);
      setMessage({ text: "Your invoice could not be downloaded. Please try again.", type: "error" });
    } finally {
      setDownloadingInvoiceId(null);
    }
  };

  const invoiceColumns: DataGridColumn<Invoice>[] = [
    {
      key: "invoice",
      header: "Invoice",
      render: (invoice) => <span className="theme-title text-sm font-bold">{invoice.invoiceNumber}</span>,
    },
    {
      key: "plan",
      header: "Plan",
      render: (invoice) => <span className="theme-title text-sm font-semibold">{invoice.planName}</span>,
    },
    {
      key: "issued",
      header: "Invoice date",
      render: (invoice) => <span className="theme-muted text-xs">{formatDateTime(invoice.createdAt)}</span>,
    },
    {
      key: "tax",
      header: "GST (18%)",
      render: (invoice) => <span className="theme-muted text-sm">{currency.format(Number(invoice.taxAmount || 0))}</span>,
    },
    {
      key: "total",
      header: "Total",
      render: (invoice) => <span className="theme-title text-sm font-black">{currency.format(Number(invoice.totalAmount || 0))}</span>,
    },
    {
      key: "status",
      header: "Payment status",
      render: (invoice) => (
        <span className="inline-flex rounded-full border border-emerald-500/25 bg-emerald-500/10 px-2.5 py-1 text-[10px] font-black uppercase tracking-wide text-emerald-700 dark:text-emerald-300">
          {invoiceStatusLabel(invoice.status)}
        </span>
      ),
    },
    {
      key: "action",
      header: "Invoice",
      render: (invoice) => (
        <button
          type="button"
          onClick={() => handleDownloadInvoice(invoice)}
          disabled={downloadingInvoiceId === invoice.id}
          aria-label={`Download invoice ${invoice.invoiceNumber}`}
          className="theme-input rounded-xl px-3 py-2 text-xs font-bold transition hover:border-sky-500/40 hover:text-sky-500 disabled:cursor-wait disabled:opacity-60"
        >
          {downloadingInvoiceId === invoice.id ? "Preparing..." : "Download invoice"}
        </button>
      ),
    },
  ];

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <PageSection>
          <PageSectionHeader
            title="Subscription & billing"
            description="Manage your owner plan, featured listing benefit, automatic renewal, wallet balance, and invoice history."
          />

          <div className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <PageMetricCard
              label="Current plan"
              value={subscription?.planName || "STARTER"}
              helper={hasActivePaidPlan ? "Your active owner subscription tier" : "Default owner plan"}
            />
            <PageMetricCard
              label="Plan expiry"
              value={hasActivePaidPlan && subscription?.endDate ? formatDate(subscription.endDate) : "Not applicable"}
              helper={hasActivePaidPlan && subscription?.endDate ? `Active until ${formatDateTime(subscription.endDate)}` : "Paid plans show their expiry date here"}
            />
            <PageMetricCard
              label="Auto renewal"
              value={hasActivePaidPlan ? (subscription?.autoRenew ? "Enabled" : "Disabled") : "Not applicable"}
              helper="Controls renewal at the end of the current plan"
              supportingText={hasActivePaidPlan && subscription?.endDate
                ? subscription?.autoRenew
                  ? `Renews on ${formatDate(subscription.endDate)}`
                  : `Active until ${formatDate(subscription.endDate)}`
                : undefined}
              valueClassName={hasActivePaidPlan
                ? subscription?.autoRenew
                  ? "text-emerald-600 dark:text-emerald-300"
                  : "text-amber-600 dark:text-amber-300"
                : ""}
              action={hasActivePaidPlan ? (
                <button
                  type="button"
                  onClick={() => setConfirmAutoRenewModal(subscription?.autoRenew ? "DISABLE" : "ENABLE")}
                  disabled={isTogglingAutoRenew}
                  aria-label={subscription?.autoRenew ? "Turn off automatic renewal" : "Enable automatic renewal"}
                  className={`shrink-0 rounded-xl border px-3 py-2 text-xs font-black transition focus:outline-none focus:ring-2 focus:ring-sky-500/40 disabled:cursor-wait disabled:opacity-60 ${
                    subscription?.autoRenew
                      ? "border-red-500/25 bg-red-500/10 text-red-600 hover:bg-red-500/15 dark:text-red-300"
                      : "border-emerald-500/30 bg-emerald-500/10 text-emerald-700 hover:bg-emerald-500/15 dark:text-emerald-300"
                  }`}
                >
                  {isTogglingAutoRenew ? "Updating..." : subscription?.autoRenew ? "Turn off" : "Enable"}
                </button>
              ) : undefined}
            />
            <Link
              href="/owner/wallet"
              aria-label={`Open owner wallet with balance ${currency.format(walletBalance ?? 0)}`}
              className="group block rounded-xl outline-none focus-visible:ring-2 focus-visible:ring-sky-400/70"
            >
              <PageMetricCard
                label="Wallet balance"
                value={currency.format(walletBalance ?? 0)}
                helper={(
                  <>
                    Open wallet to add funds
                    <br />
                    and view transactions
                  </>
                )}
                helperFocusable={false}
                helperAlign="right"
                helperTooltipClassName="!w-48 whitespace-normal"
                navigationHint="Open wallet"
                className="h-full transition duration-200 group-hover:-translate-y-0.5 group-hover:border-sky-500/40"
                valueClassName="text-sky-500 dark:text-sky-400"
              />
            </Link>
          </div>
        </PageSection>

        {message?.type === "error" && (
          <div
            role="alert"
            className="rounded-xl border border-red-500/20 bg-red-500/10 px-4 py-3 text-sm font-semibold text-red-300"
          >
            {message.text}
          </div>
        )}

        {loading ? (
          <div className="theme-panel rounded-2xl py-20 text-center">
            <p className="theme-muted font-semibold animate-pulse">Loading owner subscription controls...</p>
          </div>
        ) : (
          <>
            <div className="space-y-6">
              <PageSection>
                <div>
                  <h2 className="theme-title text-lg font-black">Choose your owner plan</h2>
                  <p className="theme-muted mt-1 text-xs">Compare all tiers together. Upgrades start immediately; lower tiers become available after expiry.</p>
                </div>

                <div className="mt-4 grid gap-3 md:grid-cols-2 xl:grid-cols-4">
                  {plans.length === 0 && (
                    <div className="theme-card theme-muted col-span-full rounded-2xl border-dashed py-12 text-center text-sm">
                      Owner plans are currently unavailable. Please try again shortly.
                    </div>
                  )}
                  {plans.map((plan) => {
                    const currentPrice = Number(subscription?.price || 0);
                    const planPrice = Number(plan.price || 0);

                    const isActive = hasActivePaidPlan
                      ? subscription?.planName?.toUpperCase() === plan.name.toUpperCase()
                      : plan.name.toUpperCase() === "STARTER";

                    const isDowngrade = hasActivePaidPlan && planPrice < currentPrice;
                    const isUpgrade = hasActivePaidPlan && planPrice > currentPrice;
                    const isNewPaid = !hasActivePaidPlan && planPrice > 0;

                    return (
                      <article
                        key={plan.id}
                        className={`theme-card flex h-full flex-col rounded-2xl border p-4 transition ${
                          isActive
                            ? "border-emerald-500/40 bg-emerald-500/10 ring-1 ring-emerald-500/25"
                            : isUpgrade
                            ? "border-sky-500/30 hover:border-sky-400/50 hover:-translate-y-0.5"
                            : isDowngrade
                            ? "opacity-70"
                            : "hover:-translate-y-0.5"
                        }`}
                      >
                        <div className="flex items-start justify-between gap-3 border-b border-[var(--theme-border)] pb-3">
                          <div className="min-w-0">
                            <p className="theme-accent-owner truncate text-[11px] font-black uppercase tracking-[0.24em]">{plan.name}</p>
                            <div className="mt-1.5 flex flex-wrap items-center gap-x-1.5 gap-y-1">
                              <h3 className="theme-title text-2xl font-black leading-none">{currency.format(Number(plan.price || 0))}</h3>
                              <span className="theme-soft text-xs">/ month</span>
                              {Number(plan.price || 0) > 0 && (
                                <TaxInclusiveNote className="ml-1 border-l border-[var(--theme-border)] pl-2" />
                              )}
                            </div>
                          </div>
                          {isActive && (
                            <span className="shrink-0 rounded-full border border-emerald-500/25 bg-emerald-500/15 px-2.5 py-1 text-[10px] font-black uppercase tracking-wide text-emerald-700 dark:text-emerald-300">
                              Current
                            </span>
                          )}
                          {isUpgrade && (
                            <span className="shrink-0 rounded-full border border-sky-500/20 bg-sky-500/10 px-2.5 py-1 text-[10px] font-black uppercase tracking-wide text-sky-700 dark:text-sky-300">
                              Upgrade
                            </span>
                          )}
                        </div>

                        <p className="theme-muted mt-3 text-xs leading-5">{ownerPlanDescription(plan)}</p>

                        <div className="mt-3 flex-1 rounded-xl bg-[var(--theme-panel-soft)] p-3">
                          <p className="theme-soft text-[10px] font-black uppercase tracking-[0.18em]">Plan highlights</p>
                          <ul className="mt-2.5 grid grid-cols-1 gap-y-2.5">
                            {ownerBenefits(plan).map((benefit) => (
                              <li key={benefit} className="theme-title flex min-w-0 items-start gap-2 text-xs leading-4">
                                <span className="mt-0.5 inline-flex h-4 w-4 shrink-0 items-center justify-center rounded-full bg-emerald-500/15 text-emerald-700 dark:text-emerald-300">
                                  <svg aria-hidden="true" className="h-2.5 w-2.5" viewBox="0 0 12 12" fill="none">
                                    <path d="m2.5 6.2 2.1 2.1 4.9-5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
                                  </svg>
                                </span>
                                <span>{benefit}</span>
                              </li>
                            ))}
                          </ul>
                        </div>

                        {isActive && plan.featuredListing && (
                          <div className={`mt-3 flex items-center justify-between gap-3 rounded-xl border px-3 py-2.5 ${subscription?.turfName ? "border-sky-500/25 bg-sky-500/10" : "border-amber-500/25 bg-amber-500/10"}`}>
                            <div className="min-w-0">
                              <p className="theme-soft text-[9px] font-black uppercase tracking-[0.16em]">Featured turf</p>
                              <p className="theme-title mt-0.5 truncate text-xs font-bold">{subscription?.turfName || "Venue setup required"}</p>
                            </div>
                            <span className={`shrink-0 rounded-full px-2 py-1 text-[9px] font-black uppercase tracking-wide ${subscription?.turfName ? "bg-sky-500/15 text-sky-700 dark:text-sky-300" : "bg-amber-500/15 text-amber-700 dark:text-amber-300"}`}>
                              {subscription?.turfName ? "Active" : "Set up"}
                            </span>
                          </div>
                        )}

                        {isActive && !plan.featuredListing && (
                          <div className="theme-input theme-muted mt-3 rounded-xl border px-3 py-2 text-[11px] font-semibold">
                            Featured listing is not included in this plan.
                          </div>
                        )}

                        <div className="mt-3">
                          {isActive ? (
                            <button
                              disabled
                              className="w-full cursor-not-allowed rounded-xl border border-emerald-500/30 bg-emerald-500/20 px-4 py-2.5 text-xs font-black text-emerald-700 dark:text-emerald-300"
                            >
                              Current Plan
                            </button>
                          ) : isDowngrade ? (
                            <button
                              disabled
                              title="Lower tiers become available when the current plan expires"
                              className="theme-input theme-muted w-full cursor-not-allowed rounded-xl border px-4 py-2.5 text-xs font-black opacity-70"
                            >
                              Available after expiry
                            </button>
                          ) : isUpgrade ? (
                            <Button
                              tone="owner"
                              onClick={() => handleOpenConfirmModal(plan)}
                              className="w-full"
                            >
                              {`Upgrade to ${plan.name}`}
                            </Button>
                          ) : isNewPaid ? (
                            <Button
                              tone="owner"
                              onClick={() => handleOpenConfirmModal(plan)}
                              className="w-full"
                            >
                              {`Activate ${plan.name}`}
                            </Button>
                          ) : (
                            <button
                              disabled
                              className="theme-input theme-muted w-full cursor-not-allowed rounded-xl border px-4 py-2.5 text-xs font-black opacity-70"
                            >
                              Default Plan
                            </button>
                          )}
                        </div>
                      </article>
                    );
                  })}
                </div>
              </PageSection>

              <PageSection>
                <div className="flex flex-wrap items-end justify-between gap-3">
                  <div>
                    <h2 className="theme-title text-lg font-black">Billing history</h2>
                    <p className="theme-muted mt-1 text-xs">Review plan payments and download a printable invoice for your records.</p>
                  </div>
                  <span className="theme-owner-badge rounded-full px-3 py-1.5 text-xs font-black">
                    {invoices.length} invoice{invoices.length === 1 ? "" : "s"}
                  </span>
                </div>
                <DataGrid
                  className="mt-5"
                  columns={invoiceColumns}
                  rows={invoices}
                  getRowKey={(invoice) => invoice.id}
                  gridTemplateColumns="minmax(180px,1.2fr) minmax(170px,1.1fr) minmax(190px,1.2fr) 100px 120px 100px 140px"
                  emptyState={(
                    <div className="theme-card theme-muted mt-5 rounded-2xl border-dashed py-12 text-center text-sm">
                      No owner invoices yet.
                    </div>
                  )}
                />
              </PageSection>
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
                  ? `Upgrading will immediately activate your ${selectedPlanForPurchase.name} tier.`
                  : "Review your subscription details and wallet deduction before confirming."}
              </p>
            </div>

            {selectedPlanForPurchase.featuredListing && (
              <div className="rounded-xl border border-sky-500/20 bg-sky-950/20 p-3.5">
                <label htmlFor="purchase-featured-turf" className="text-xs font-bold text-slate-200">
                  Turf receiving the featured listing
                </label>
                <Select
                  id="purchase-featured-turf"
                  value={selectedTurfId}
                  onChange={(event) => {
                    setSelectedTurfId(event.target.value);
                    setPurchaseTurfError("");
                  }}
                  accent="owner"
                  aria-describedby="purchase-featured-turf-help"
                  className="mt-2"
                  options={turfs.length > 0
                    ? turfs.map((turf) => ({ value: turf.id, label: turf.name }))
                    : [{ value: "", label: "No turf available", disabled: true }]}
                />
                <p id="purchase-featured-turf-help" className="mt-2 text-[11px] leading-relaxed text-slate-400">
                  This venue receives a visibility boost in search and recommendations until the plan expires. Bookings are not guaranteed.
                </p>
                {turfs.length === 0 && (
                  <p className="mt-2 text-xs font-semibold text-amber-300">
                    Add a venue from <Link href="/turf/my-turfs" className="underline underline-offset-2">My Turfs</Link> before purchasing this plan.
                  </p>
                )}
                {purchaseTurfError && <p role="alert" className="mt-2 text-xs font-semibold text-rose-300">{purchaseTurfError}</p>}
              </div>
            )}

            {/* Plan Details & Exact Validity Box */}
            <div className="rounded-xl border border-white/10 bg-white/[0.03] p-3.5 space-y-2.5 text-xs">
              <div className="flex justify-between items-center">
                <span className="text-slate-400">Selected Tier:</span>
                <span className="font-bold text-sky-400">{selectedPlanForPurchase.name}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-400">Plan price (GST included):</span>
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
