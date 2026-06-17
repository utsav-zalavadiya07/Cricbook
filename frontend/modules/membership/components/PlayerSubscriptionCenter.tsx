"use client";

import { useCallback, useEffect, useState } from "react";
import { useI18n } from "@/modules/i18n/I18nProvider";
import { cancelSubscription, downloadInvoice, getCurrentSubscription, getMyInvoices, triggerRenewalTest } from "@/modules/membership/services/subscriptionService";
import type { Invoice, Subscription } from "@/modules/membership/types";

const PLAYER_SUBSCRIPTION_TYPE = "USER_PREMIUM" as const;

export default function PlayerSubscriptionCenter() {
  const { t } = useI18n();
  const [sub, setSub] = useState<Subscription | null>(null);
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<{ text: string; type: "success" | "error" } | null>(null);

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

  const handleCancelAutoRenew = () => {
    cancelSubscription(PLAYER_SUBSCRIPTION_TYPE)
      .then(() => {
        setMessage({ text: t("subscription.success.autoRenewOff"), type: "success" });
        loadData();
      })
      .catch((err) => {
        const errorMsg = err.response?.data?.error || t("subscription.error.autoRenewOff");
        setMessage({ text: errorMsg, type: "error" });
      });
  };

  const handleTriggerRenewTest = () => {
    triggerRenewalTest()
      .then(() => {
        setMessage({ text: t("subscription.success.renewalCheck"), type: "success" });
        loadData();
      })
      .catch((err) => {
        setMessage({ text: err.response?.data?.error || t("subscription.error.renewalCheck"), type: "error" });
      });
  };

  const handleDownloadInvoice = (inv: Invoice) => {
    downloadInvoice(inv.id)
      .then((res) => {
        const dataStr = `data:text/json;charset=utf-8,${encodeURIComponent(JSON.stringify(res.data, null, 2))}`;
        const downloadAnchor = document.createElement("a");
        downloadAnchor.setAttribute("href", dataStr);
        downloadAnchor.setAttribute("download", `invoice-${inv.invoiceNumber}.json`);
        document.body.appendChild(downloadAnchor);
        downloadAnchor.click();
        downloadAnchor.remove();
      })
      .catch((err) => console.error(err));
  };

  return (
    <main className="theme-page-player min-h-screen px-4 py-6 sm:px-6 lg:px-8">
      <div className="w-full space-y-8">
        <div>
          <h1 className="theme-title text-3xl font-extrabold tracking-tight">{t("subscription.title")}</h1>
          <p className="theme-muted mt-1.5 text-sm">{t("subscription.subtitle")}</p>
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
                    <PlanDetail label={t("subscription.startsAt")} value={sub.startDate.slice(0, 10)} mutedValue />
                    <PlanDetail label={t("subscription.endsAt")} value={sub.endDate.slice(0, 10)} mutedValue />
                    <PlanDetail
                      label={t("subscription.autoRenewal")}
                      value={sub.autoRenew ? t("subscription.enabled") : t("subscription.disabled")}
                      valueClassName={sub.autoRenew ? "text-emerald-400" : "text-red-400"}
                    />
                  </div>

                  {sub.autoRenew && sub.price > 0 && (
                    <button
                      onClick={handleCancelAutoRenew}
                      className="w-full mt-4 rounded-xl border border-red-500/20 bg-red-500/15 py-2.5 text-xs font-bold text-red-300 transition-all hover:bg-red-500/25 active:scale-95"
                    >
                      {t("subscription.turnOffAutoRenew")}
                    </button>
                  )}

                  <div className="theme-border border-t pt-4 space-y-2">
                    <p className="theme-soft text-2xs font-semibold uppercase tracking-wider">{t("subscription.devSandbox")}</p>
                    <button
                      onClick={handleTriggerRenewTest}
                      className="w-full rounded-xl border border-white/10 bg-white/5 py-2 text-2xs font-semibold text-slate-300 transition-all hover:bg-white/10 active:scale-95"
                    >
                      {t("subscription.triggerRenewal")}
                    </button>
                  </div>
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
                      downloadLabel={t("subscription.downloadJson")}
                      onDownload={() => handleDownloadInvoice(inv)}
                    />
                  ))
                )}
              </div>
            </div>
          </div>
        )}
      </div>
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
  onDownload,
}: {
  invoice: Invoice;
  issuedLabel: string;
  gstLabel: string;
  downloadLabel: string;
  onDownload: () => void;
}) {
  return (
    <div className="theme-panel-soft flex flex-col gap-4 rounded-2xl p-4 sm:flex-row sm:items-center sm:justify-between">
      <div className="space-y-1">
        <div className="flex items-center gap-2">
          <span className="theme-soft text-xs font-bold">{invoice.invoiceNumber}</span>
          <span className="inline-flex rounded-full bg-emerald-500/10 px-2 py-0.5 text-3xs font-extrabold uppercase tracking-wide text-emerald-400">
            {invoice.status}
          </span>
        </div>
        <h4 className="theme-title text-sm font-bold">{invoice.planName} Plan</h4>
        <p className="theme-soft text-2xs">{issuedLabel}: {invoice.createdAt.slice(0, 10)}</p>
      </div>

      <div className="flex items-center justify-between sm:justify-end gap-6">
        <div className="text-left sm:text-right">
          <span className="theme-title block text-base font-black">Rs {invoice.totalAmount}</span>
          <span className="theme-soft block max-w-[220px] text-[10px] leading-4 sm:max-w-none">{gstLabel}</span>
        </div>
        <button
          onClick={onDownload}
          className="rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-xs font-bold text-slate-200 transition-all hover:bg-white/10 active:scale-95"
        >
          {downloadLabel}
        </button>
      </div>
    </div>
  );
}
