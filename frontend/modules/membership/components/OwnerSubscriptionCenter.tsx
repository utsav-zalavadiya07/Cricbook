"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import { getOwnerTurfs } from "@/modules/owner/services/ownerService";
import {
  cancelSubscription,
  downloadInvoice,
  getCurrentSubscription,
  getMyInvoices,
  getPricingPage,
  startSubscription,
} from "@/modules/membership/services/subscriptionService";
import type { Invoice, MembershipPlan, Subscription } from "@/modules/membership/types";

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
  const [loading, setLoading] = useState(true);
  const [busyPlanId, setBusyPlanId] = useState<number | null>(null);
  const [message, setMessage] = useState<{ text: string; type: "success" | "error" } | null>(null);

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
      const [subscriptionResult, invoiceResult, pricingResult, turfResult] = await Promise.allSettled([
        getCurrentSubscription(OWNER_SUBSCRIPTION_TYPE),
        getMyInvoices(OWNER_SUBSCRIPTION_TYPE),
        getPricingPage(OWNER_SUBSCRIPTION_TYPE),
        getOwnerTurfs(),
      ]);

      if (subscriptionResult.status === "fulfilled") {
        setSubscription(subscriptionResult.value.data);
      } else {
        setSubscription(null);
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

  const handleActivatePlan = async (plan: MembershipPlan) => {
    setBusyPlanId(plan.id);
    setMessage(null);
    try {
      const turfId = selectedTurfId ? Number(selectedTurfId) : undefined;
      await startSubscription(plan.id, turfId);
      setMessage({ text: `${plan.name} owner subscription activated.`, type: "success" });
      await loadData();
    } catch (err: unknown) {
      setMessage({ text: errorMessage(err, "Could not activate owner subscription."), type: "error" });
    } finally {
      setBusyPlanId(null);
    }
  };

  const handleCancelAutoRenew = async () => {
    setBusyPlanId(subscription?.planId ?? -1);
    setMessage(null);
    try {
      await cancelSubscription(OWNER_SUBSCRIPTION_TYPE);
      setMessage({ text: "Owner plan auto-renewal turned off.", type: "success" });
      await loadData();
    } catch (err: unknown) {
      setMessage({ text: errorMessage(err, "Could not turn off owner auto-renewal."), type: "error" });
    } finally {
      setBusyPlanId(null);
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
              <p className="theme-accent-owner text-xs font-black uppercase tracking-[0.34em]">Owner Subscription Center</p>
              <h1 className="theme-title mt-3 text-3xl font-black">Owner plans, featured listings and billing.</h1>
              <p className="theme-muted mt-2 text-sm">
                Manage owner-only plans separately from player premium so portal billing, turf promotion, and invoice history do not overlap.
              </p>
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
              <Metric title="Active plan" value={subscription?.planName || "STARTER"} />
              <Metric title="Linked turf" value={selectedTurfName} />
              <Metric title="Auto renewal" value={subscription?.autoRenew ? "Enabled" : "Disabled"} tone={subscription?.autoRenew ? "good" : "warn"} />
              <Metric title="Owner billing" value={currency.format(paidInvoiceTotal)} />
            </div>

            <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
              <section className="theme-panel rounded-3xl p-6">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <h2 className="theme-title text-xl font-black">Owner plan controls</h2>
                    <p className="theme-muted mt-1 text-sm">Plans here apply only to turf-owner business features.</p>
                  </div>
                  {subscription?.autoRenew && Number(subscription.price || 0) > 0 && (
                    <button
                      onClick={handleCancelAutoRenew}
                      disabled={busyPlanId !== null}
                      className="rounded-2xl border border-red-500/25 bg-red-500/10 px-4 py-2 text-xs font-bold text-red-300 transition hover:bg-red-500/20 disabled:opacity-60"
                    >
                      Turn off auto-renew
                    </button>
                  )}
                </div>

                <div className="mt-6 grid gap-4 lg:grid-cols-3">
                  {plans.map((plan) => {
                    const isActive = subscription?.planId === plan.id || subscription?.planName?.toUpperCase() === plan.name.toUpperCase();
                    const needsTurf = Boolean(plan.featuredListing);
                    const disabled = busyPlanId !== null || (needsTurf && !selectedTurfId);

                    return (
                      <article
                        key={plan.id}
                        className={`theme-panel-soft flex min-h-[360px] flex-col rounded-3xl p-5 transition ${
                          isActive ? "border-emerald-500/40 bg-emerald-500/10" : "hover:-translate-y-0.5"
                        }`}
                      >
                        <div className="flex items-start justify-between gap-3">
                          <div>
                            <p className="theme-accent-owner text-xs font-black uppercase tracking-[0.28em]">{plan.name}</p>
                            <h3 className="theme-title mt-2 text-2xl font-black">{currency.format(Number(plan.price || 0))}</h3>
                            <p className="theme-soft mt-1 text-xs">per {plan.durationDays} days</p>
                          </div>
                          {isActive && (
                            <span className="rounded-full bg-emerald-500/15 px-3 py-1 text-2xs font-black uppercase tracking-wide text-emerald-300">
                              Active
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

                        <button
                          onClick={() => handleActivatePlan(plan)}
                          disabled={disabled || isActive}
                          className="mt-6 rounded-2xl bg-gradient-to-r from-sky-500 to-blue-600 px-4 py-3 text-sm font-black text-white shadow-lg shadow-sky-500/10 transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-55"
                        >
                          {busyPlanId === plan.id ? "Activating..." : isActive ? "Current Plan" : "Activate Owner Plan"}
                        </button>
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
                            <p className="theme-soft mt-1 text-xs">{invoice.createdAt.slice(0, 10)} • GST {currency.format(Number(invoice.taxAmount || 0))}</p>
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
    </DashboardLayout>
  );
}

function Metric({ title, value, tone }: { title: string; value: string; tone?: "good" | "warn" }) {
  return (
    <div className="theme-panel rounded-3xl p-5">
      <p className="theme-soft text-xs font-black uppercase tracking-[0.22em]">{title}</p>
      <p className={`mt-3 truncate text-2xl font-black ${tone === "good" ? "text-emerald-300" : tone === "warn" ? "text-amber-300" : "theme-title"}`}>
        {value}
      </p>
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
