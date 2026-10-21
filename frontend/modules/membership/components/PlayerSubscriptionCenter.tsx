"use client";
import { TileInfo } from "@/components/ui";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useI18n } from "@/modules/i18n/I18nProvider";
import {
  downloadInvoicePdf,
  getCurrentSubscription,
  getMyInvoices,
  invoiceStatusLabel,
  setAutoRenew,
} from "@/modules/membership/services/subscriptionService";
import type { Invoice, Subscription } from "@/modules/membership/types";
import { formatDateTime } from "@/utils/dateFormatter";

const PLAYER_SUBSCRIPTION_TYPE = "USER_PREMIUM" as const;

export default function PlayerSubscriptionCenter() {
  const { t } = useI18n();
  const [sub, setSub] = useState<Subscription | null>(null);
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<{ text: string; type: "success" | "error" } | null>(null);
  const [downloadingInvoiceId, setDownloadingInvoiceId] = useState<number | null>(null);

  // Auto-renew confirmation modal state
  const [confirmAutoRenewModal, setConfirmAutoRenewModal] = useState<"ENABLE" | "DISABLE" | null>(null);
  const [isTogglingAutoRenew, setIsTogglingAutoRenew] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    setMessage(null);
    try {
      const [subResult, invoiceResult] = await Promise.allSettled([
        getCurrentSubscription(PLAYER_SUBSCRIPTION_TYPE),
        getMyInvoices(PLAYER_SUBSCRIPTION_TYPE),
      ]);

      if (subResult.status === "fulfilled") {
        setSub(subResult.value.data);
      } else {
        setSub(null);
      }

      if (invoiceResult.status === "fulfilled") {
        setInvoices(invoiceResult.value.data);
      } else {
        setInvoices([]);
      }

      if (subResult.status === "rejected" && invoiceResult.status === "rejected") {
        setMessage({ text: t("subscription.error.loadFailed"), type: "error" });
      } else if (invoiceResult.status === "rejected") {
        setMessage({ text: t("subscription.error.invoiceLoadFailed"), type: "error" });
      } else if (subResult.status === "rejected") {
        setMessage({ text: t("subscription.error.subscriptionLoadFailed"), type: "error" });
      }
    } catch (err) {
      console.error("Error loading subscription data:", err);
      setMessage({ text: t("subscription.error.loadFailed"), type: "error" });
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadData();
    }, 0);

    return () => window.clearTimeout(timer);
  }, [loadData]);

  const handleConfirmToggleAutoRenew = async () => {
    if (!confirmAutoRenewModal) return;
    const targetEnable = confirmAutoRenewModal === "ENABLE";
    setIsTogglingAutoRenew(true);
    setMessage(null);
    try {
      await setAutoRenew(targetEnable, PLAYER_SUBSCRIPTION_TYPE);
      setMessage({
        text: targetEnable
          ? "Auto-renewal successfully enabled! Your plan will renew automatically on expiry."
          : "Auto-renewal turned off. Your plan remains active until its expiration date.",
        type: "success",
      });
      setConfirmAutoRenewModal(null);
      await loadData();
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { message?: string; error?: string } } };
      setMessage({
        text: apiErr.response?.data?.message || apiErr.response?.data?.error || "Failed to update auto-renewal setting.",
        type: "error",
      });
    } finally {
      setIsTogglingAutoRenew(false);
    }
  };

  const handleDownloadInvoice = async (invoice: Invoice) => {
    setDownloadingInvoiceId(invoice.id);
    setMessage(null);
    try {
      await downloadInvoicePdf(invoice.id, invoice.invoiceNumber);
    } catch (err) {
      console.error("Unable to download invoice:", err);
      setMessage({ text: t("subscription.error.invoiceDownloadFailed"), type: "error" });
    } finally {
      setDownloadingInvoiceId(null);
    }
  };

  return (
    <main className="theme-page-player min-h-screen px-4 py-6 sm:px-6 lg:px-8">
      <div className="w-full space-y-8">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="theme-title text-3xl font-extrabold tracking-tight">
              {t("subscription.title")}
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Page information" focusable={false}>
                  {t("subscription.subtitle")}
                </TileInfo>
              </span>
            </h1>
          </div>

          <Link
            href="/pricing"
            className="inline-flex items-center gap-2 rounded-2xl bg-gradient-to-r from-emerald-500 to-teal-500 px-5 py-2.5 text-xs font-bold text-white shadow-lg shadow-emerald-500/15 transition hover:opacity-90 active:scale-95"
          >
            Upgrade or Change Plan →
          </Link>
        </div>

        {message && (
          <div
            className={`rounded-2xl border p-4 text-center text-sm font-medium transition-all ${
              message.type === "success"
                ? "border-emerald-500/20 bg-emerald-500/10 text-emerald-300"
                : "border-red-500/20 bg-red-500/10 text-red-300"
            }`}
          >
            {message.text}
          </div>
        )}

        {loading ? (
          <div className="theme-muted py-20 text-center font-semibold animate-pulse">
            {t("subscription.loading")}
          </div>
        ) : (
          <div className="grid gap-6 xl:grid-cols-3">
            <div className="theme-panel xl:col-span-1 rounded-3xl p-6 space-y-6">
              <h2 className="theme-title text-lg font-bold">{t("subscription.activePlan")}</h2>

              {sub ? (
                <div className="space-y-4">
                  <div>
                    <span className="inline-flex rounded-full bg-emerald-500/10 border border-emerald-500/20 px-3 py-1 text-2xs font-extrabold uppercase tracking-wide text-emerald-400">
                      {sub.planName}
                    </span>
                    <h3 className="theme-title mt-2 text-2xl font-black">{sub.planName} Plan</h3>
                    <p className="theme-soft text-xs uppercase tracking-wider">Player premium</p>
                  </div>

                  <div className="theme-border border-t pt-4 space-y-3.5 text-sm">
                    <PlanDetail label={t("subscription.status")} value={sub.status} valueClassName={sub.status === "ACTIVE" ? "text-emerald-400" : "text-amber-400"} />
                    <PlanDetail label={t("subscription.price")} value={`Rs ${sub.price}`} />
                    <PlanDetail label={t("subscription.billingPeriod")} value={`${sub.durationDays} Days`} />
                    <PlanDetail label={t("subscription.startsAt")} value={formatDateTime(sub.startDate)} mutedValue />
                    <PlanDetail label={t("subscription.endsAt")} value={formatDateTime(sub.endDate)} mutedValue />
                    <PlanDetail
                      label={t("subscription.autoRenewal")}
                      value={sub.autoRenew ? t("subscription.enabled") : t("subscription.disabled")}
                      valueClassName={sub.autoRenew ? "text-emerald-400" : "text-amber-400"}
                    />
                  </div>

                  <Link
                    href="/pricing"
                    className="block text-center w-full mt-3 rounded-xl bg-gradient-to-r from-emerald-500/20 to-teal-500/20 border border-emerald-500/30 py-2.5 text-xs font-bold text-emerald-300 transition-all hover:bg-emerald-500/30 active:scale-95"
                  >
                    Change plan
                  </Link>

                  {/* Auto-Renewal Toggle Button (Always visible for paid plans) */}
                  {Number(sub.price || 0) > 0 && (
                    <div className="pt-1">
                      {sub.autoRenew ? (
                        <button
                          type="button"
                          onClick={() => setConfirmAutoRenewModal("DISABLE")}
                          className="w-full rounded-xl border border-red-500/25 bg-red-500/10 py-2.5 text-xs font-bold text-red-300 transition-all hover:bg-red-500/20 active:scale-95 flex items-center justify-center gap-2"
                        >
                          Turn off automatic renewal
                        </button>
                      ) : (
                        <button
                          type="button"
                          onClick={() => setConfirmAutoRenewModal("ENABLE")}
                          className="w-full rounded-xl border border-emerald-500/30 bg-emerald-500/15 py-2.5 text-xs font-bold text-emerald-300 transition-all hover:bg-emerald-500/25 active:scale-95 flex items-center justify-center gap-2 shadow-sm shadow-emerald-500/10"
                        >
                          Enable automatic renewal
                        </button>
                      )}
                    </div>
                  )}

                </div>
              ) : (
                <p className="theme-muted text-sm">{t("subscription.noActivePlan")}</p>
              )}
            </div>

            <div className="theme-panel xl:col-span-2 rounded-3xl p-6 space-y-6">
              <h2 className="theme-title text-lg font-bold">{t("subscription.history")}</h2>

              <div className="space-y-4 max-h-[500px] overflow-y-auto pr-2">
                {invoices.length === 0 ? (
                  <p className="theme-soft theme-card py-12 text-center text-sm border-dashed rounded-2xl">
                    {t("subscription.emptyInvoices")}
                  </p>
                ) : (
                  invoices.map((inv) => (
                    <InvoiceRow
                      key={inv.id}
                      invoice={inv}
                      issuedLabel={t("subscription.issued")}
                      gstLabel={t("subscription.includesGst", { amount: `Rs ${inv.taxAmount}` })}
                      downloadLabel={t("subscription.downloadInvoice")}
                      downloading={downloadingInvoiceId === inv.id}
                      onDownload={() => handleDownloadInvoice(inv)}
                    />
                  ))
                )}
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Auto-Renewal Confirmation Dialog */}
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
                  ? `Your ${sub?.planName} plan will renew automatically on ${formatDateTime(sub?.endDate)} using your wallet balance.`
                  : `Your current membership perks remain fully active until ${formatDateTime(sub?.endDate)}, but the plan will expire without any automatic wallet deductions.`}
              </p>
            </div>

            {/* Plan Info Box */}
            <div className="rounded-xl border border-white/10 bg-white/[0.03] p-3.5 space-y-2 text-xs">
              <div className="flex justify-between items-center">
                <span className="text-slate-400">Current Plan:</span>
                <span className="font-bold text-white">{sub?.planName} Plan</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-400">Valid Until:</span>
                <span className="font-semibold text-emerald-400">{formatDateTime(sub?.endDate)}</span>
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
                    ? "bg-emerald-600 hover:bg-emerald-500 shadow-emerald-600/20"
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
    </main>
  );
}

function PlanDetail({ label, value, valueClassName = "", mutedValue = false }: { label: string; value: string; valueClassName?: string; mutedValue?: boolean }) {
  return (
    <div className="flex justify-between gap-4">
      <span className="theme-muted break-words">{label}</span>
      <span className={`${mutedValue ? "theme-soft font-semibold" : "theme-title font-bold"} ${valueClassName}`}>{value}</span>
    </div>
  );
}

function InvoiceRow({
  invoice,
  issuedLabel,
  gstLabel,
  downloadLabel,
  downloading,
  onDownload,
}: {
  invoice: Invoice;
  issuedLabel: string;
  gstLabel: string;
  downloadLabel: string;
  downloading: boolean;
  onDownload: () => void;
}) {
  return (
    <div className="theme-panel-soft flex flex-col gap-4 rounded-2xl p-4 sm:flex-row sm:items-center sm:justify-between">
      <div className="space-y-1">
        <div className="flex items-center gap-2">
          <span className="theme-soft text-xs font-bold">{invoice.invoiceNumber}</span>
          <span className="inline-flex rounded-full bg-emerald-500/10 px-2 py-0.5 text-3xs font-extrabold uppercase tracking-wide text-emerald-400">
            {invoiceStatusLabel(invoice.status)}
          </span>
        </div>
        <h4 className="theme-title text-sm font-bold">{invoice.planName} Plan</h4>
        <p className="theme-soft text-2xs">{issuedLabel}: {formatDateTime(invoice.createdAt)}</p>
      </div>

      <div className="flex items-center justify-between sm:justify-end gap-6">
        <div className="text-left sm:text-right">
          <span className="theme-title block text-base font-black">Rs {invoice.totalAmount}</span>
          <span className="theme-soft block max-w-[220px] text-[10px] leading-4 sm:max-w-none">{gstLabel}</span>
        </div>
        <button
          type="button"
          onClick={onDownload}
          disabled={downloading}
          aria-label={`${downloadLabel} ${invoice.invoiceNumber}`}
          className="rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-xs font-bold text-slate-200 transition-all hover:bg-white/10 active:scale-95 disabled:cursor-wait disabled:opacity-60"
        >
          {downloading ? "Preparing..." : downloadLabel}
        </button>
      </div>
    </div>
  );
}
