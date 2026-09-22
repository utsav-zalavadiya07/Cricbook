"use client";
import { TileInfo } from "@/components/ui";

import { useCallback, useEffect, useRef, useState } from "react";
import { useI18n } from "@/modules/i18n/I18nProvider";
import { useToast } from "@/modules/ui/ToastProvider";
import { useAuthStore } from "@/store/authStore";
import {
  cancelWalletTopupOrder,
  createWalletTopupOrder,
  getCoupons,
  getWallet,
  validateCoupon,
  verifyWalletTopupPayment,
} from "@/modules/wallet/services/walletService";

type WalletTransaction = {
  id: number;
  amount: number;
  type: string;
  status: string;
  reference?: string;
  description?: string;
  createdAt?: string;
};

type Wallet = {
  walletId: number;
  balance: number;
  transactions: WalletTransaction[];
};

type Coupon = {
  code: string;
  discountType: string;
  discountValue: number;
  validUntil?: string;
  valid: boolean;
  message?: string;
};

const PRESET_AMOUNTS = [200, 500, 1000, 2000, 5000];

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

export default function WalletPage() {
  const { t } = useI18n();
  const { toast } = useToast();
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [coupons, setCoupons] = useState<Coupon[]>([]);
  const [amount, setAmount] = useState(500);
  const [couponCode, setCouponCode] = useState("");
  const [bookingAmount, setBookingAmount] = useState(1000);
  const [couponResult, setCouponResult] = useState<Coupon | null>(null);
  const [loading, setLoading] = useState(true);
  const [paymentStatus, setPaymentStatus] = useState<"idle" | "creating_order" | "awaiting_payment" | "verifying">("idle");
  const inFlight = useRef(false);
  const authUser = useAuthStore((s) => s.user);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [walletRes, couponsRes] = await Promise.all([getWallet(), getCoupons()]);
      setWallet(walletRes.data);
      setCoupons(couponsRes.data);
    } catch {
      toast(t("wallet.loadError"), "error");
    } finally {
      setLoading(false);
    }
  }, [t, toast]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void load();
    }, 0);

    return () => window.clearTimeout(timer);
  }, [load]);

  const handleAddMoneyWithRazorpay = async () => {
    if (inFlight.current) return;
    if (!amount || amount < 1) {
      toast("Please enter a valid amount of at least Rs 1.", "error");
      return;
    }
    if (amount > 100000) {
      toast("Maximum top-up amount is Rs 1,00,000.", "error");
      return;
    }

    inFlight.current = true;
    setPaymentStatus("creating_order");

    try {
      // 1. Load Razorpay Checkout SDK
      const scriptLoaded = await loadScript("https://checkout.razorpay.com/v1/checkout.js");
      if (!scriptLoaded) {
        throw new Error("Unable to load Razorpay payment SDK. Please check your internet connection.");
      }

      // 2. Create real order on backend
      const orderRes = await createWalletTopupOrder(amount);
      const { orderId, amount: orderAmount, currency, keyId } = orderRes.data;

      setPaymentStatus("awaiting_payment");

      // 3. Launch official Razorpay Checkout Modal
      const options: ConstructorParameters<Window["Razorpay"]>[0] = {
        key: keyId || process.env.NEXT_PUBLIC_RAZORPAY_KEY_ID || "",
        amount: Math.round(Number(orderAmount) * 100), // in paise
        currency: currency || "INR",
        name: "Cricbook",
        description: `Wallet Top-up – ₹${amount}`,
        image: "/favicon.ico",
        order_id: orderId,
        handler: async (response: {
          razorpay_order_id: string;
          razorpay_payment_id: string;
          razorpay_signature: string;
        }) => {
          setPaymentStatus("verifying");
          try {
            const verifyRes = await verifyWalletTopupPayment({
              orderId: response.razorpay_order_id,
              paymentId: response.razorpay_payment_id,
              signature: response.razorpay_signature,
            });
            setWallet(verifyRes.data);
            toast(`₹${amount} added to your wallet successfully!`, "success");
          } catch (err: unknown) {
            const errObj = err as { response?: { data?: { message?: string } }; message?: string };
            const msg = errObj.response?.data?.message || errObj.message || "Payment verification failed. Please contact support.";
            toast(msg, "error");
          } finally {
            setPaymentStatus("idle");
            inFlight.current = false;
          }
        },
        modal: {
          backdropclose: true,
          escape: true,
          confirm_close: true,
          animation: true,
          ondismiss: async () => {
            setPaymentStatus("idle");
            inFlight.current = false;
            try {
              await cancelWalletTopupOrder(orderId);
            } catch {
              // Ignore
            }
            toast("Payment cancelled. Wallet was not charged.", "info");
          },
        },
        prefill: {
          name: authUser?.name || "Cricbook User",
          email: authUser?.email || "user@cricbook.com",
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
      rzp.on("payment.failed", async (res) => {
        setPaymentStatus("idle");
        inFlight.current = false;
        try {
          await cancelWalletTopupOrder(orderId);
        } catch {
          // Ignore
        }
        toast(`Payment failed: ${res.error.description}`, "error");
      });
      rzp.open();
    } catch (err: unknown) {
      setPaymentStatus("idle");
      inFlight.current = false;
      const errObj = err as { response?: { data?: { message?: string } }; message?: string };
      const msg = errObj.response?.data?.message || errObj.message || "Failed to initiate payment.";
      toast(msg, "error");
    }
  };

  const handleValidateCoupon = async () => {
    try {
      const res = await validateCoupon(couponCode, bookingAmount);
      setCouponResult(res.data);
    } catch {
      toast(t("wallet.validateError"), "error");
    }
  };

  return (
    <main className="theme-page-player min-h-screen px-4 py-8 sm:px-6">
      <div className="mx-auto max-w-6xl space-y-6">
        <div>
          <h1 className="theme-title text-3xl font-bold">
            {t("wallet.title")}
            <span className="inline-flex align-middle ml-3 mb-1">
              <TileInfo label="Page information" focusable={false}>
                {t("wallet.subtitle")}
              </TileInfo>
            </span>
          </h1>
        </div>

        {loading && <p className="theme-muted py-20 text-center">{t("wallet.loading")}</p>}

        {!loading && (
          <div className="grid gap-6 lg:grid-cols-3">
            {/* Top-up Card */}
            <section className="theme-panel rounded-3xl p-6 lg:col-span-1 flex flex-col justify-between">
              <div>
                <p className="theme-soft text-xs font-semibold uppercase tracking-wider">{t("wallet.balance")}</p>
                <p className="theme-title mt-2 text-4xl font-bold tracking-tight">₹{wallet?.balance ?? 0}</p>
                <p className="theme-muted text-xs mt-1">Available for bookings & turf rentals</p>

                <div className="mt-6 space-y-4">
                  <div>
                    <label className="text-xs font-medium theme-soft mb-1.5 block">Enter Top-up Amount</label>
                    <div className="relative">
                      <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-sm font-semibold theme-muted">₹</span>
                      <input
                        type="number"
                        min="1"
                        max="100000"
                        step="1"
                        value={amount}
                        onChange={(e) => setAmount(Number(e.target.value))}
                        className="theme-input w-full rounded-xl pl-8 pr-4 py-3 text-sm font-medium outline-none focus:border-emerald-500"
                        placeholder="500"
                      />
                    </div>
                  </div>

                  {/* Preset Amount Chips */}
                  <div>
                    <p className="text-[11px] font-medium theme-muted mb-2">Quick presets</p>
                    <div className="flex flex-wrap gap-1.5">
                      {PRESET_AMOUNTS.map((preset) => (
                        <button
                          key={preset}
                          type="button"
                          onClick={() => setAmount(preset)}
                          className={`rounded-lg px-2.5 py-1 text-xs font-semibold transition ${
                            amount === preset
                              ? "bg-emerald-500 text-white shadow-sm"
                              : "theme-panel-soft theme-title hover:border-emerald-500/40"
                          }`}
                        >
                          +₹{preset}
                        </button>
                      ))}
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={handleAddMoneyWithRazorpay}
                    disabled={paymentStatus !== "idle" || amount < 1}
                    className="w-full rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 px-4 py-3.5 text-sm font-semibold text-white shadow-md shadow-emerald-500/20 hover:from-emerald-400 hover:to-teal-400 disabled:cursor-not-allowed disabled:opacity-50 transition cursor-pointer"
                  >
                    {paymentStatus === "creating_order" && "Initiating Order..."}
                    {paymentStatus === "awaiting_payment" && "Opening Razorpay..."}
                    {paymentStatus === "verifying" && "Verifying Payment Signature..."}
                    {paymentStatus === "idle" && (
                      <span className="flex items-center justify-center gap-2">
                        <span>🔒</span>
                        <span>Pay ₹{amount} with Razorpay</span>
                      </span>
                    )}
                  </button>

                  <div className="flex items-center justify-center gap-2 pt-1 text-[11px] theme-muted">
                    <span>UPI (GPay/PhonePe/Paytm)</span>
                    <span>•</span>
                    <span>Cards</span>
                    <span>•</span>
                    <span>Net Banking</span>
                  </div>
                </div>
              </div>
            </section>

            {/* Transactions Section */}
            <section className="theme-panel rounded-3xl p-6 lg:col-span-2">
              <h2 className="theme-title text-lg font-bold">{t("wallet.transactions")}</h2>
              <div className="mt-4 space-y-3 max-h-[380px] overflow-y-auto pr-1">
                {(!wallet?.transactions || wallet.transactions.length === 0) && <Empty text={t("wallet.noTransactions")} />}
                {wallet?.transactions?.map((tx) => {
                  const isPositive = tx.amount > 0;
                  return (
                    <div key={tx.id} className="theme-panel-soft flex items-center justify-between rounded-2xl p-4">
                      <div className="space-y-0.5">
                        <div className="flex items-center gap-2">
                          <p className="theme-title font-semibold text-sm">{tx.type}</p>
                          <span
                            className={`rounded-full px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider ${
                              tx.status === "SUCCESS"
                                ? "bg-emerald-500/15 text-emerald-400 border border-emerald-500/30"
                                : tx.status === "PENDING"
                                ? "bg-amber-500/15 text-amber-400 border border-amber-500/30"
                                : "bg-red-500/15 text-red-400 border border-red-500/30"
                            }`}
                          >
                            {tx.status}
                          </span>
                        </div>
                        <p className="theme-muted text-xs">{tx.description || tx.reference || tx.status}</p>
                        {tx.createdAt && (
                          <p className="text-[10px] theme-muted">{new Date(tx.createdAt).toLocaleString()}</p>
                        )}
                      </div>
                      <p className={`font-bold text-sm ${isPositive ? "text-emerald-400" : "text-red-400"}`}>
                        {isPositive ? `+₹${tx.amount}` : `-₹${Math.abs(tx.amount)}`}
                      </p>
                    </div>
                  );
                })}
              </div>
            </section>

            {/* Coupons & Promo Section */}
            <section className="theme-panel rounded-3xl p-6 lg:col-span-3">
              <div className="grid gap-6 lg:grid-cols-2">
                <div>
                  <h2 className="theme-title text-lg font-bold">{t("wallet.availableCoupons")}</h2>
                  <div className="mt-4 grid gap-3 sm:grid-cols-2">
                    {coupons.length === 0 && <Empty text={t("wallet.noCoupons")} />}
                    {coupons.map((coupon) => (
                      <button
                        key={coupon.code}
                        onClick={() => setCouponCode(coupon.code)}
                        className="theme-panel-soft rounded-2xl p-4 text-left hover:border-emerald-500/40 transition"
                      >
                        <p className="theme-title font-semibold">{coupon.code}</p>
                        <p className="theme-muted mt-1 text-sm">
                          {coupon.discountValue}
                          {coupon.discountType === "PERCENT" ? t("wallet.offPercent") : t("wallet.offFlat")}
                        </p>
                      </button>
                    ))}
                  </div>
                </div>
                <div>
                  <h2 className="theme-title text-lg font-bold">{t("wallet.validateTitle")}</h2>
                  <div className="mt-4 space-y-3">
                    <input
                      value={couponCode}
                      onChange={(e) => setCouponCode(e.target.value.toUpperCase())}
                      placeholder={t("wallet.promoPlaceholder")}
                      className="theme-input w-full rounded-xl px-4 py-3 text-sm outline-none focus:border-emerald-500"
                    />
                    <input
                      type="number"
                      value={bookingAmount}
                      onChange={(e) => setBookingAmount(Number(e.target.value))}
                      className="theme-input w-full rounded-xl px-4 py-3 text-sm outline-none focus:border-emerald-500"
                    />
                    <button
                      onClick={handleValidateCoupon}
                      className="rounded-xl bg-sky-500 px-5 py-3 text-sm font-semibold text-white hover:bg-sky-400 transition"
                    >
                      {t("wallet.validateButton")}
                    </button>
                    {couponResult && (
                      <div
                        className={`rounded-2xl border p-4 text-sm ${
                          couponResult.valid
                            ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-200"
                            : "border-red-500/30 bg-red-500/10 text-red-200"
                        }`}
                      >
                        {couponResult.message}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </section>
          </div>
        )}
      </div>
    </main>
  );
}

function Empty({ text }: { text: string }) {
  return <p className="theme-card theme-muted rounded-2xl border-dashed p-5 text-center text-sm">{text}</p>;
}
