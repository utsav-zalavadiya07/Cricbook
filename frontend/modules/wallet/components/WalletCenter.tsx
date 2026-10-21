"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Button, Field, Input } from "@/components/ui";
import { PageSection, PageSectionHeader } from "@/components/ui/PageSurface";
import { useI18n } from "@/modules/i18n/I18nProvider";
import { useToast } from "@/modules/ui/ToastProvider";
import {
  cancelWalletTopupOrder,
  createWalletTopupOrder,
  getCoupons,
  getWallet,
  validateCoupon,
  verifyWalletTopupPayment,
} from "@/modules/wallet/services/walletService";
import type { Coupon, Wallet, WalletTransaction } from "@/modules/wallet/types";
import { useAuthStore } from "@/store/authStore";
import { formatDateTime } from "@/utils/dateFormatter";

type WalletPortal = "player" | "owner";
type PaymentStatus = "idle" | "creating_order" | "awaiting_payment" | "verifying";

const PRESET_AMOUNTS = [200, 500, 1000, 2000, 5000];
const MAX_TOPUP_AMOUNT = 100000;
const RAZORPAY_SCRIPT = "https://checkout.razorpay.com/v1/checkout.js";

const currency = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 2,
});

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

function validateTopupAmount(value: string) {
  const normalized = value.trim();
  if (!normalized) return "Enter a top-up amount.";
  if (!/^\d+(\.\d{1,2})?$/.test(normalized)) return "Use a number with no more than 2 decimal places.";

  const amount = Number(normalized);
  if (!Number.isFinite(amount) || amount < 1) return "Top-up amount must be at least Rs 1.";
  if (amount > MAX_TOPUP_AMOUNT) return "Top-up amount cannot exceed Rs 1,00,000.";
  return "";
}

function apiErrorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { error?: string; message?: string } }; message?: string };
  return apiError.response?.data?.error || apiError.response?.data?.message || apiError.message || fallback;
}

export default function WalletCenter({ portal }: { portal: WalletPortal }) {
  const { t } = useI18n();
  const { toast } = useToast();
  const authUser = useAuthStore((state) => state.user);
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [coupons, setCoupons] = useState<Coupon[]>([]);
  const [amountInput, setAmountInput] = useState("500");
  const [amountTouched, setAmountTouched] = useState(false);
  const [couponCode, setCouponCode] = useState("");
  const [bookingAmountInput, setBookingAmountInput] = useState("1000");
  const [couponResult, setCouponResult] = useState<Coupon | null>(null);
  const [couponCodeError, setCouponCodeError] = useState("");
  const [bookingAmountError, setBookingAmountError] = useState("");
  const [couponServiceError, setCouponServiceError] = useState("");
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [paymentStatus, setPaymentStatus] = useState<PaymentStatus>("idle");
  const inFlight = useRef(false);

  const isOwner = portal === "owner";
  const accent = isOwner ? "owner" : "player";
  const amountValidation = validateTopupAmount(amountInput);
  const topupAmount = amountValidation ? 0 : Number(amountInput);
  const transactions = wallet?.transactions || [];
  const successfulTransactions = transactions.filter((transaction) => transaction.status === "SUCCESS").length;
  const pendingTransactions = transactions.filter((transaction) => transaction.status === "PENDING").length;
  const lastActivity = transactions.find((transaction) => transaction.createdAt)?.createdAt;

  const pageCopy = useMemo(
    () => isOwner
      ? {
          eyebrow: "Owner finance",
          title: "Owner Wallet",
          description: "Add funds securely for subscription purchases and automatic plan renewals, then review every wallet transaction.",
          balanceHint: "Available for owner plans and automatic renewals",
        }
      : {
          eyebrow: "Player payments",
          title: t("wallet.title"),
          description: t("wallet.subtitle"),
          balanceHint: "Available for bookings, memberships, and eligible purchases",
        },
    [isOwner, t],
  );

  const load = useCallback(async () => {
    setLoading(true);
    setLoadError("");
    try {
      const walletResponse = await getWallet();
      setWallet(walletResponse.data);

      if (!isOwner) {
        try {
          const couponResponse = await getCoupons();
          setCoupons(couponResponse.data || []);
        } catch {
          setCoupons([]);
        }
      }
    } catch (error) {
      const message = apiErrorMessage(error, t("wallet.loadError"));
      setLoadError(message);
      toast(message, "error");
    } finally {
      setLoading(false);
    }
  }, [isOwner, t, toast]);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const resetPaymentState = () => {
    setPaymentStatus("idle");
    inFlight.current = false;
  };

  const cancelPendingOrder = async (orderId: string) => {
    try {
      await cancelWalletTopupOrder(orderId);
    } catch {
      // The backend also prevents unverified orders from crediting the wallet.
    }
  };

  const handleAddMoneyWithRazorpay = async () => {
    if (inFlight.current) return;

    setAmountTouched(true);
    if (amountValidation) return;

    inFlight.current = true;
    setPaymentStatus("creating_order");

    try {
      const scriptLoaded = await loadScript(RAZORPAY_SCRIPT);
      if (!scriptLoaded) {
        throw new Error("Unable to load Razorpay. Check your connection and try again.");
      }

      const orderResponse = await createWalletTopupOrder(topupAmount);
      const { orderId, amount: orderAmount, currency: orderCurrency, keyId } = orderResponse.data;
      setPaymentStatus("awaiting_payment");

      const checkout = new window.Razorpay({
        key: keyId || process.env.NEXT_PUBLIC_RAZORPAY_KEY_ID || "",
        amount: Math.round(Number(orderAmount) * 100),
        currency: orderCurrency || "INR",
        name: "Cricbook",
        description: `${isOwner ? "Owner wallet" : "Player wallet"} top-up - Rs ${topupAmount}`,
        image: "/favicon.ico",
        order_id: orderId,
        handler: async (response) => {
          setPaymentStatus("verifying");
          try {
            const verifyResponse = await verifyWalletTopupPayment({
              orderId: response.razorpay_order_id,
              paymentId: response.razorpay_payment_id,
              signature: response.razorpay_signature,
            });
            setWallet(verifyResponse.data);
            toast(`${currency.format(topupAmount)} added to your wallet.`, "success");
          } catch (error) {
            toast(apiErrorMessage(error, "Payment verification failed. Contact support if money was deducted."), "error");
          } finally {
            resetPaymentState();
          }
        },
        modal: {
          backdropclose: true,
          escape: true,
          confirm_close: true,
          animation: true,
          ondismiss: async () => {
            resetPaymentState();
            await cancelPendingOrder(orderId);
            toast("Payment cancelled. Your wallet was not charged.", "info");
          },
        },
        prefill: {
          name: authUser?.name,
          email: authUser?.email,
        },
        theme: {
          color: isOwner ? "#0ea5e9" : "#10b981",
          backdrop_color: "rgba(7, 17, 30, 0.75)",
        },
        config: {
          display: {
            blocks: {
              upi: {
                name: "Pay using UPI or QR",
                instruments: [{ method: "upi", flows: ["qr", "intent", "collect"] }],
              },
            },
            sequence: ["block.upi"],
            preferences: { show_default_blocks: true },
          },
        },
      });

      checkout.on("payment.failed", async (response) => {
        resetPaymentState();
        await cancelPendingOrder(orderId);
        toast(`Payment failed: ${response.error.description}`, "error");
      });
      checkout.open();
    } catch (error) {
      resetPaymentState();
      toast(apiErrorMessage(error, "Unable to start the payment. Try again."), "error");
    }
  };

  const handleValidateCoupon = async () => {
    const normalizedCode = couponCode.trim().toUpperCase();
    const bookingAmount = Number(bookingAmountInput);
    setCouponCodeError("");
    setBookingAmountError("");
    setCouponServiceError("");
    if (!normalizedCode) {
      setCouponCodeError("Enter a coupon code.");
      return;
    }
    if (!Number.isFinite(bookingAmount) || bookingAmount <= 0) {
      setBookingAmountError("Booking amount must be greater than Rs 0.");
      return;
    }

    setCouponResult(null);
    try {
      const response = await validateCoupon(normalizedCode, bookingAmount);
      setCouponResult(response.data);
    } catch (error) {
      const message = apiErrorMessage(error, t("wallet.validateError"));
      setCouponServiceError(message);
      toast(message, "error");
    }
  };

  const paymentButtonLabel = paymentStatus === "creating_order"
    ? "Creating secure order..."
    : paymentStatus === "awaiting_payment"
      ? "Opening Razorpay..."
      : paymentStatus === "verifying"
        ? "Verifying payment..."
        : `Add ${currency.format(topupAmount || 0)}`;

  return (
    <div className="space-y-6">
      <PageSection>
        <PageSectionHeader
          eyebrow={pageCopy.eyebrow}
          title={pageCopy.title}
          description={pageCopy.description}
          actions={(
            <span className={`inline-flex items-center gap-2 rounded-full border px-3 py-2 text-xs font-bold ${
              isOwner
                ? "border-sky-500/25 bg-sky-500/10 text-sky-300"
                : "border-emerald-500/25 bg-emerald-500/10 text-emerald-300"
            }`}>
              <span aria-hidden="true">●</span>
              Secured by Razorpay
            </span>
          )}
        />

        <div className="mt-5 grid gap-3 sm:grid-cols-3">
          <WalletSummary label="Successful" value={successfulTransactions} helper="Completed wallet entries" />
          <WalletSummary label="Pending" value={pendingTransactions} helper="Orders awaiting completion" />
          <WalletSummary label="Last activity" value={lastActivity ? formatDateTime(lastActivity) : "No activity"} helper="Most recent wallet update" />
        </div>
      </PageSection>

      {loading ? (
        <PageSection className="theme-muted py-20 text-center">Loading wallet...</PageSection>
      ) : loadError ? (
        <PageSection className="py-12 text-center">
          <p role="alert" className="text-sm font-semibold text-red-400">{loadError}</p>
          <Button tone={accent} className="mt-4" onClick={() => void load()}>Try again</Button>
        </PageSection>
      ) : (
        <div className="grid gap-6 xl:grid-cols-5">
          <PageSection className="xl:col-span-2">
            <div className={`rounded-2xl border p-5 ${
              isOwner ? "border-sky-500/25 bg-sky-500/10" : "border-emerald-500/25 bg-emerald-500/10"
            }`}>
              <p className="theme-soft text-[11px] font-black uppercase tracking-[0.2em]">Available balance</p>
              <p className="theme-title mt-2 text-4xl font-black tracking-tight">{currency.format(Number(wallet?.balance || 0))}</p>
              <p className="theme-muted mt-2 text-xs">{pageCopy.balanceHint}</p>
              {isOwner ? (
                <Link href="/owner/subscription" className="mt-3 inline-flex text-xs font-bold text-sky-400 hover:text-sky-300">
                  Review subscription billing →
                </Link>
              ) : null}
            </div>

            <div className="mt-5">
              <h2 className="theme-title text-lg font-black">Add money</h2>
              <p className="theme-muted mt-1 text-xs">Choose a preset or enter an amount between Rs 1 and Rs 1,00,000.</p>

              <Field
                className="mt-4"
                label="Top-up amount"
                error={amountTouched ? amountValidation : undefined}
                hint={!amountTouched || !amountValidation ? `Balance after top-up: ${currency.format(Number(wallet?.balance || 0) + topupAmount)}` : undefined}
              >
                <div className="relative">
                  <span className="theme-muted pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-sm font-bold">Rs</span>
                  <Input
                    id={`${portal}-wallet-topup-amount`}
                    accent={accent}
                    inputMode="decimal"
                    value={amountInput}
                    onChange={(event) => {
                      setAmountInput(event.target.value);
                      setAmountTouched(true);
                    }}
                    onBlur={() => setAmountTouched(true)}
                    className={`pl-11 ${amountTouched && amountValidation ? "border-red-500/60" : ""}`}
                    aria-invalid={Boolean(amountTouched && amountValidation)}
                    aria-describedby={amountTouched && amountValidation ? `${portal}-wallet-topup-error` : undefined}
                  />
                </div>
              </Field>
              {amountTouched && amountValidation ? (
                <span id={`${portal}-wallet-topup-error`} className="sr-only">{amountValidation}</span>
              ) : null}

              <div className="mt-4">
                <p className="theme-soft mb-2 text-[11px] font-black uppercase tracking-[0.16em]">Quick amounts</p>
                <div className="grid grid-cols-3 gap-2 sm:grid-cols-5 xl:grid-cols-3">
                  {PRESET_AMOUNTS.map((preset) => (
                    <button
                      key={preset}
                      type="button"
                      onClick={() => {
                        setAmountInput(String(preset));
                        setAmountTouched(false);
                      }}
                      className={`rounded-xl border px-2 py-2 text-xs font-bold transition ${
                        Number(amountInput) === preset
                          ? isOwner
                            ? "border-sky-500/50 bg-sky-500/15 text-sky-300"
                            : "border-emerald-500/50 bg-emerald-500/15 text-emerald-300"
                          : "theme-card theme-title hover:border-current"
                      }`}
                    >
                      {currency.format(preset)}
                    </button>
                  ))}
                </div>
              </div>

              <Button
                tone={accent}
                className="mt-5 w-full"
                onClick={() => void handleAddMoneyWithRazorpay()}
                disabled={paymentStatus !== "idle" || Boolean(amountValidation)}
              >
                {paymentButtonLabel}
              </Button>
              <p className="theme-soft mt-3 text-center text-[11px]">UPI, cards, and net banking are completed in Razorpay Checkout.</p>
            </div>
          </PageSection>

          <PageSection className="xl:col-span-3">
            <div className="flex flex-wrap items-end justify-between gap-3">
              <div>
                <h2 className="theme-title text-lg font-black">Wallet activity</h2>
                <p className="theme-muted mt-1 text-xs">Credits, payments, refunds, and pending top-up orders.</p>
              </div>
              <span className={`rounded-full border px-3 py-1.5 text-xs font-black ${
                isOwner
                  ? "border-sky-500/25 bg-sky-500/10 text-sky-300"
                  : "border-emerald-500/25 bg-emerald-500/10 text-emerald-300"
              }`}>
                {transactions.length} transaction{transactions.length === 1 ? "" : "s"}
              </span>
            </div>

            <div className="mt-5 max-h-[520px] space-y-3 overflow-y-auto pr-1">
              {transactions.length === 0 ? (
                <EmptyState text="No wallet activity yet. Your completed top-ups and payments will appear here." />
              ) : transactions.map((transaction) => (
                <TransactionRow key={transaction.id} transaction={transaction} isOwner={isOwner} />
              ))}
            </div>
          </PageSection>
        </div>
      )}

      {!loading && !loadError && !isOwner ? (
        <PageSection>
          <div className="grid gap-6 lg:grid-cols-2">
            <div>
              <h2 className="theme-title text-lg font-black">Available coupons</h2>
              <p className="theme-muted mt-1 text-xs">Select a coupon to check it against your planned booking amount.</p>
              <div className="mt-4 grid gap-3 sm:grid-cols-2">
                {coupons.length === 0 ? <EmptyState text={t("wallet.noCoupons")} /> : coupons.map((coupon) => (
                  <button
                    key={coupon.code}
                    type="button"
                    onClick={() => {
                      setCouponCode(coupon.code);
                      setCouponCodeError("");
                      setCouponServiceError("");
                      setCouponResult(null);
                    }}
                    className="theme-card rounded-2xl p-4 text-left transition hover:border-emerald-500/40"
                  >
                    <p className="theme-title font-black">{coupon.code}</p>
                    <p className="theme-muted mt-1 text-sm">
                      {coupon.discountValue}{coupon.discountType === "PERCENT" ? "% off" : " rupees off"}
                    </p>
                  </button>
                ))}
              </div>
            </div>

            <div>
              <h2 className="theme-title text-lg font-black">Check a coupon</h2>
              <p className="theme-muted mt-1 text-xs">Validation does not reserve or apply the coupon to a booking.</p>
              <div className="mt-4 grid gap-4 sm:grid-cols-2">
                <Field label="Coupon code" error={couponCodeError || undefined}>
                  <Input
                    accent="player"
                    value={couponCode}
                    onChange={(event) => {
                      setCouponCode(event.target.value.toUpperCase());
                      setCouponCodeError("");
                      setCouponServiceError("");
                      setCouponResult(null);
                    }}
                    placeholder="Enter code"
                    aria-invalid={Boolean(couponCodeError)}
                  />
                </Field>
                <Field label="Booking amount" error={bookingAmountError || undefined}>
                  <Input
                    accent="player"
                    inputMode="decimal"
                    value={bookingAmountInput}
                    onChange={(event) => {
                      setBookingAmountInput(event.target.value);
                      setBookingAmountError("");
                      setCouponServiceError("");
                      setCouponResult(null);
                    }}
                    aria-invalid={Boolean(bookingAmountError)}
                  />
                </Field>
              </div>
              <Button tone="player" className="mt-4" onClick={() => void handleValidateCoupon()}>
                Check coupon
              </Button>
              {couponServiceError ? <p role="alert" className="mt-3 text-sm text-red-400">{couponServiceError}</p> : null}
              {couponResult ? (
                <div
                  role="status"
                  className={`mt-4 rounded-2xl border p-4 text-sm ${
                    couponResult.valid
                      ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-300"
                      : "border-red-500/30 bg-red-500/10 text-red-300"
                  }`}
                >
                  {couponResult.message}
                </div>
              ) : null}
            </div>
          </div>
        </PageSection>
      ) : null}
    </div>
  );
}

function WalletSummary({ label, value, helper }: { label: string; value: string | number; helper: string }) {
  return (
    <div className="theme-card rounded-xl p-4">
      <p className="theme-soft text-[10px] font-black uppercase tracking-[0.2em]">{label}</p>
      <p className="theme-title mt-2 truncate text-lg font-black">{value}</p>
      <p className="theme-muted mt-1 text-xs">{helper}</p>
    </div>
  );
}

function TransactionRow({ transaction, isOwner }: { transaction: WalletTransaction; isOwner: boolean }) {
  const isCredit = Number(transaction.amount) >= 0;
  const statusClass = transaction.status === "SUCCESS"
    ? "border-emerald-500/25 bg-emerald-500/10 text-emerald-300"
    : transaction.status === "PENDING"
      ? "border-amber-500/25 bg-amber-500/10 text-amber-300"
      : "border-red-500/25 bg-red-500/10 text-red-300";

  return (
    <article className={`theme-card rounded-2xl border-l-4 p-4 ${isOwner ? "border-l-sky-500" : "border-l-emerald-500"}`}>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <p className="theme-title font-bold">{transactionLabel(transaction.type)}</p>
            <span className={`rounded-full border px-2.5 py-1 text-[10px] font-black uppercase tracking-wide ${statusClass}`}>
              {transaction.status}
            </span>
          </div>
          <p className="theme-muted mt-1 text-xs">{transaction.description || transaction.reference || "Wallet transaction"}</p>
          {transaction.createdAt ? <p className="theme-soft mt-2 text-[11px]">{formatDateTime(transaction.createdAt)}</p> : null}
        </div>
        <p className={`text-base font-black ${isCredit ? "text-emerald-400" : "text-red-400"}`}>
          {isCredit ? "+" : "-"}{currency.format(Math.abs(Number(transaction.amount)))}
        </p>
      </div>
    </article>
  );
}

function transactionLabel(type: string) {
  return type
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function EmptyState({ text }: { text: string }) {
  return <div className="theme-card theme-muted rounded-2xl border-dashed p-6 text-center text-sm">{text}</div>;
}
