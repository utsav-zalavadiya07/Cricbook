"use client";
import { TileInfo } from "@/components/ui";

import { useCallback, useEffect, useState } from "react";
import { useI18n } from "@/modules/i18n/I18nProvider";
import { useToast } from "@/modules/ui/ToastProvider";
import { addMoney, getCoupons, getWallet, validateCoupon } from "@/modules/wallet/services/walletService";

type WalletTransaction = {
  id: number;
  amount: number;
  type: string;
  status: string;
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

  const handleAddMoney = async () => {
    try {
      const res = await addMoney(amount);
      setWallet(res.data);
      toast(t("wallet.addMoneySuccess"), "success");
    } catch {
      toast(t("wallet.addMoneyError"), "error");
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
          <h1 className="theme-title text-3xl font-bold">{t("wallet.title")}
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Page information" focusable={false}>{t("wallet.subtitle")}</TileInfo>
              </span></h1>
        </div>

        {loading && <p className="theme-muted py-20 text-center">{t("wallet.loading")}</p>}

        {!loading && (
          <div className="grid gap-6 lg:grid-cols-3">
            <section className="theme-panel rounded-3xl p-6 lg:col-span-1">
              <p className="theme-soft text-xs font-semibold uppercase tracking-wider">{t("wallet.balance")}</p>
              <p className="theme-title mt-3 text-4xl font-bold">Rs {wallet?.balance ?? 0}</p>
              <div className="mt-6 space-y-3">
                <input
                  type="number"
                  value={amount}
                  onChange={(e) => setAmount(Number(e.target.value))}
                  className="theme-input w-full rounded-xl px-4 py-3 text-sm outline-none focus:border-emerald-500"
                />
                <button onClick={handleAddMoney} className="w-full rounded-xl bg-emerald-500 px-4 py-3 text-sm font-semibold text-white hover:bg-emerald-400">
                  {t("wallet.addMoney")}
                </button>
              </div>
            </section>

            <section className="theme-panel rounded-3xl p-6 lg:col-span-2">
              <h2 className="theme-title text-lg font-bold">{t("wallet.transactions")}</h2>
              <div className="mt-4 space-y-3">
                {wallet?.transactions.length === 0 && <Empty text={t("wallet.noTransactions")} />}
                {wallet?.transactions.map((tx) => (
                  <div key={tx.id} className="theme-panel-soft flex items-center justify-between rounded-2xl p-4">
                    <div>
                      <p className="theme-title font-semibold">{tx.type}</p>
                      <p className="theme-muted text-sm">{tx.description || tx.status}</p>
                    </div>
                    <p className="font-bold text-emerald-300">Rs {tx.amount}</p>
                  </div>
                ))}
              </div>
            </section>

            <section className="theme-panel rounded-3xl p-6 lg:col-span-3">
              <div className="grid gap-6 lg:grid-cols-2">
                <div>
                  <h2 className="theme-title text-lg font-bold">{t("wallet.availableCoupons")}</h2>
                  <div className="mt-4 grid gap-3 sm:grid-cols-2">
                    {coupons.length === 0 && <Empty text={t("wallet.noCoupons")} />}
                    {coupons.map((coupon) => (
                      <button key={coupon.code} onClick={() => setCouponCode(coupon.code)} className="theme-panel-soft rounded-2xl p-4 text-left hover:border-emerald-500/40">
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
                    <button onClick={handleValidateCoupon} className="rounded-xl bg-sky-500 px-5 py-3 text-sm font-semibold text-white hover:bg-sky-400">
                      {t("wallet.validateButton")}
                    </button>
                    {couponResult && (
                      <div className={`rounded-2xl border p-4 text-sm ${couponResult.valid ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-200" : "border-red-500/30 bg-red-500/10 text-red-200"}`}>
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
