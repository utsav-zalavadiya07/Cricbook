"use client";

import { Suspense, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { useI18n } from "@/modules/i18n/I18nProvider";
import { getMyBookings } from "@/modules/booking/services/bookingService";
import { Booking } from "@/modules/booking/types/booking.types";
import {
  createSplitPayment,
  getBookingSplit,
  getMySplits,
  payShare,
  PaymentSplit,
  remindSplit,
  SplitPaymentResponse,
} from "@/modules/payments/services/splitPaymentService";

const today = new Date().toISOString().slice(0, 10);

export default function SplitPaymentsPage() {
  return (
    <Suspense fallback={<main className="theme-page-player min-h-screen" />}>
      <SplitPaymentsContent />
    </Suspense>
  );
}

function SplitPaymentsContent() {
  const { t } = useI18n();
  const searchParams = useSearchParams();
  const bookingParam = searchParams.get("bookingId");
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [splits, setSplits] = useState<PaymentSplit[]>([]);
  const [collection, setCollection] = useState<SplitPaymentResponse | null>(null);
  const [bookingId, setBookingId] = useState(bookingParam ?? "");
  const [players, setPlayers] = useState("");
  const [dueDate, setDueDate] = useState(today);
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);

  const selectedBooking = bookings.find((item) => item.id === Number(bookingId));
  const participants = useMemo(() => parsePlayers(players), [players]);
  const selectedAmount = Number(selectedBooking?.totalAmount ?? selectedBooking?.amount ?? 0);
  const estimatedShare = participants.length > 0 ? selectedAmount / participants.length : selectedAmount;

  const splitMetrics = useMemo(() => {
    const paid = splits.filter((split) => split.status === "PAID");
    const pending = splits.filter((split) => split.status !== "PAID");
    return {
      paidAmount: paid.reduce((sum, split) => sum + Number(split.amount ?? 0), 0),
      pendingAmount: pending.reduce((sum, split) => sum + Number(split.amount ?? 0), 0),
      pendingCount: pending.length,
      totalCount: splits.length,
    };
  }, [splits]);

  const load = (showSpinner = true) => {
    if (showSpinner) setLoading(true);
    return Promise.all([getMyBookings(), getMySplits()])
      .then(([bookingRes, splitRes]) => {
        setBookings(bookingRes.data || []);
        setSplits(splitRes.data || []);
        const nextBookingId = bookingId || bookingParam || (bookingRes.data?.[0]?.id ? String(bookingRes.data[0].id) : "");
        if (nextBookingId) setBookingId(nextBookingId);
      })
      .catch((error) => setStatus(errorMessage(error, t("split.loadError"))))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    let cancelled = false;
    Promise.all([getMyBookings(), getMySplits()])
      .then(([bookingRes, splitRes]) => {
        if (cancelled) return;
        setBookings(bookingRes.data || []);
        setSplits(splitRes.data || []);
        const nextBookingId = bookingParam || (bookingRes.data?.[0]?.id ? String(bookingRes.data[0].id) : "");
        if (nextBookingId) setBookingId(nextBookingId);
      })
      .catch((error) => {
        if (!cancelled) setStatus(errorMessage(error, t("split.loadError")));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [bookingParam, t]);

  useEffect(() => {
    let cancelled = false;
    if (!bookingId) return;
    getBookingSplit(Number(bookingId))
      .then((response) => {
        if (!cancelled) setCollection(response.data);
      })
      .catch(() => undefined);
    return () => {
      cancelled = true;
    };
  }, [bookingId]);

  const createSplit = () => {
    const id = Number(bookingId);
    if (!id || participants.length === 0) {
      setStatus(t("split.validation"));
      return;
    }
    setBusy(true);
    setStatus("");
    createSplitPayment({ bookingId: id, splitType: "EQUAL", dueDate, shares: participants.map((loginId) => ({ loginId })) })
      .then((response) => {
        setPlayers("");
        setCollection(response.data);
        setStatus(t("split.created"));
        return load(false);
      })
      .catch((error) => setStatus(errorMessage(error, t("split.createError"))))
      .finally(() => setBusy(false));
  };

  const pay = (splitId: number) => {
    setBusy(true);
    setStatus("");
    payShare({ splitId, useWallet: false })
      .then((response) => {
        setCollection(response.data);
        setStatus("Share marked as paid.");
        return load(false);
      })
      .catch((error) => setStatus(errorMessage(error, t("split.payError"))))
      .finally(() => setBusy(false));
  };

  const remind = () => {
    if (!bookingId) return;
    setBusy(true);
    setStatus("");
    remindSplit(Number(bookingId))
      .then((response) => {
        setCollection(response.data);
        setStatus("Payment reminders sent.");
      })
      .catch((error) => setStatus(errorMessage(error, "Could not send reminders.")))
      .finally(() => setBusy(false));
  };

  return (
    <main className="theme-page-player min-h-screen">
      <div className="mx-auto max-w-[1400px] space-y-6 p-4 sm:p-6 lg:p-8">
        <section className="theme-panel rounded-[2rem] p-6 sm:p-8">
          <div className="grid gap-6 xl:grid-cols-[1fr_360px] xl:items-end">
            <div>
              <p className="theme-soft text-xs font-bold uppercase tracking-[0.28em]">{t("split.epic")}</p>
              <h1 className="theme-title mt-3 text-3xl font-black sm:text-5xl">{t("split.title")}</h1>
              <p className="theme-muted mt-3 max-w-3xl text-sm leading-6">{t("split.subtitle")}</p>
            </div>
            <div className="theme-panel-soft rounded-3xl p-4">
              <p className="theme-soft text-xs font-black uppercase tracking-[0.18em]">Collection Snapshot</p>
              <div className="mt-3 grid grid-cols-3 gap-3">
                <MiniMetric label="Due" value={formatCurrency(splitMetrics.pendingAmount)} />
                <MiniMetric label="Paid" value={formatCurrency(splitMetrics.paidAmount)} />
                <MiniMetric label="Shares" value={`${splitMetrics.pendingCount}/${splitMetrics.totalCount}`} />
              </div>
            </div>
          </div>
        </section>

        {status && <div className="theme-panel-soft rounded-2xl p-4 text-sm font-semibold text-emerald-600 dark:text-emerald-300">{status}</div>}

        <section className="grid gap-4 md:grid-cols-4">
          <Metric title="Selected Booking" value={selectedBooking ? `#${selectedBooking.id}` : "--"} detail={selectedBooking?.turfName || "Choose a booking"} />
          <Metric title="Booking Total" value={formatCurrency(selectedAmount)} detail={selectedBooking ? `${formatDate(selectedBooking.bookingDate)} - ${formatTimeRange(selectedBooking)}` : "No booking selected"} />
          <Metric title="Pending Shares" value={splitMetrics.pendingCount} detail={formatCurrency(splitMetrics.pendingAmount)} />
          <Metric title="Collection Rate" value={`${collection?.analytics?.collectionSuccessRate ?? 0}%`} detail={collection?.analytics?.insight || "No collection selected"} />
        </section>

        <div className="grid gap-6 xl:grid-cols-[420px_1fr]">
          <section className="theme-panel rounded-3xl p-6">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h2 className="theme-title text-xl font-black">Build a Split</h2>
                <p className="theme-muted mt-1 text-sm">Pick the booking, add teammates, and preview each share before creating the collection.</p>
              </div>
              <span className="rounded-full border border-emerald-500/30 bg-emerald-500/10 px-3 py-1 text-xs font-black text-emerald-600 dark:text-emerald-300">Equal</span>
            </div>

            <div className="mt-5 grid gap-4">
              <label className="grid gap-2 text-sm font-semibold">
                <span className="theme-muted">{t("split.booking")}</span>
                <select value={bookingId} onChange={(event) => setBookingId(event.target.value)} className="theme-input rounded-2xl px-4 py-3">
                  {bookings.map((booking) => (
                    <option key={booking.id} value={booking.id}>
                      #{booking.id} - {booking.turfName || booking.turfId} - {formatCurrency(Number(booking.totalAmount ?? booking.amount ?? 0))}
                    </option>
                  ))}
                </select>
              </label>

              {selectedBooking && (
                <div className="theme-panel-soft rounded-2xl p-4">
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <p className="theme-title font-bold">{selectedBooking.turfName || `Booking #${selectedBooking.id}`}</p>
                      <p className="theme-muted mt-1 text-sm">{formatDate(selectedBooking.bookingDate)} - {formatTimeRange(selectedBooking)}</p>
                    </div>
                    <p className="text-right text-sm font-black text-emerald-600 dark:text-emerald-300">{formatCurrency(selectedAmount)}</p>
                  </div>
                </div>
              )}

              <label className="grid gap-2 text-sm font-semibold">
                <span className="theme-muted">{t("split.players")}</span>
                <textarea
                  value={players}
                  onChange={(event) => setPlayers(event.target.value)}
                  className="theme-input min-h-32 rounded-2xl px-4 py-3"
                  placeholder="player@email.com, 98765..."
                />
              </label>

              <div className="grid gap-3 sm:grid-cols-2">
                <label className="grid gap-2 text-sm font-semibold">
                  <span className="theme-muted">{t("split.dueDate")}</span>
                  <input value={dueDate} onChange={(event) => setDueDate(event.target.value)} type="date" className="theme-input rounded-2xl px-4 py-3" />
                </label>
                <div className="theme-panel-soft rounded-2xl p-4">
                  <p className="theme-soft text-xs font-black uppercase tracking-[0.16em]">Share Preview</p>
                  <p className="theme-title mt-2 text-lg font-black">{formatCurrency(estimatedShare)}</p>
                  <p className="theme-muted text-xs">{participants.length || 1} player{participants.length === 1 ? "" : "s"}</p>
                </div>
              </div>

              <button disabled={busy || !bookingId} onClick={createSplit} className="rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-black text-white transition hover:bg-emerald-400 disabled:cursor-not-allowed disabled:opacity-60">
                {busy ? "Working..." : t("split.create")}
              </button>
            </div>
          </section>

          <section className="grid gap-6">
            <section className="theme-panel rounded-3xl p-6">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <h2 className="theme-title text-xl font-black">Selected Collection</h2>
                  <p className="theme-muted mt-1 text-sm">Track who paid, who is pending, and send reminders from the same place.</p>
                </div>
                {collection?.collection && (
                  <button disabled={busy} onClick={remind} className="rounded-2xl border border-emerald-500/30 px-4 py-2 text-sm font-black text-emerald-600 transition hover:bg-emerald-500/10 disabled:opacity-60 dark:text-emerald-300">
                    Send Reminders
                  </button>
                )}
              </div>

              {!collection?.collection && <Empty text="No split plan exists for the selected booking yet." />}
              {collection?.collection && (
                <div className="mt-5 space-y-4">
                  <div className="theme-panel-soft rounded-2xl p-4">
                    <div className="flex items-center justify-between gap-4">
                      <div>
                        <p className="theme-title font-black">Booking #{collection.collection.bookingId}</p>
                        <p className="theme-muted mt-1 text-sm">Due {formatDate(collection.collection.dueDate)} - {collection.collection.status}</p>
                      </div>
                      <div className="text-right">
                        <p className="theme-title text-xl font-black">{collection.collection.collectionRate}%</p>
                        <p className="theme-soft text-xs">collected</p>
                      </div>
                    </div>
                    <div className="mt-4 h-2 overflow-hidden rounded-full bg-white/10">
                      <div className="h-full rounded-full bg-emerald-500" style={{ width: `${collection.collection.collectionRate}%` }} />
                    </div>
                  </div>

                  <div className="grid gap-3 md:grid-cols-2">
                    {collection.splits.map((split) => (
                      <SplitCard key={split.id} split={split} onPay={pay} busy={busy} />
                    ))}
                  </div>
                </div>
              )}
            </section>

            <section className="theme-panel rounded-3xl p-6">
              <h2 className="theme-title text-xl font-black">{t("split.pendingTitle")}</h2>
              <p className="theme-muted mt-1 text-sm">{t("split.pendingBody")}</p>
              {loading && <p className="theme-muted py-10 text-center">{t("common.loading")}</p>}
              {!loading && splits.length === 0 && <Empty text={t("split.empty")} />}
              <div className="mt-5 grid gap-3 md:grid-cols-2">
                {splits.map((split) => (
                  <SplitCard key={split.id} split={split} onPay={pay} busy={busy} />
                ))}
              </div>
            </section>
          </section>
        </div>
      </div>
    </main>
  );
}

function SplitCard({ split, onPay, busy }: { split: PaymentSplit; onPay: (splitId: number) => void; busy: boolean }) {
  const paid = split.status === "PAID";
  return (
    <article className="theme-panel-soft rounded-2xl p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="theme-title font-black">{split.playerName}</p>
          <p className="theme-muted mt-1 text-sm">{split.turfName || `Booking #${split.bookingId}`}</p>
          <p className="theme-soft mt-1 text-xs">{formatDate(split.bookingDate)} - {formatTimeRange(split)}</p>
        </div>
        <StatusBadge status={split.status} />
      </div>
      <div className="mt-4 flex items-end justify-between gap-3">
        <div>
          <p className="text-lg font-black text-emerald-600 dark:text-emerald-300">{formatCurrency(Number(split.amount ?? 0))}</p>
          <p className="theme-soft text-xs">Due {formatDate(split.dueDate)}</p>
        </div>
        {!paid && (
          <button disabled={busy} onClick={() => onPay(split.id)} className="rounded-xl bg-emerald-500 px-4 py-2 text-sm font-black text-white transition hover:bg-emerald-400 disabled:opacity-60">
            Pay
          </button>
        )}
      </div>
    </article>
  );
}

function Metric({ title, value, detail }: { title: string; value: string | number; detail: string }) {
  return (
    <div className="theme-panel rounded-3xl p-5">
      <p className="theme-soft text-xs font-black uppercase tracking-[0.18em]">{title}</p>
      <p className="theme-title mt-3 text-2xl font-black">{value}</p>
      <p className="theme-muted mt-1 truncate text-sm">{detail}</p>
    </div>
  );
}

function MiniMetric({ label, value }: { label: string; value: string | number }) {
  return (
    <div>
      <p className="theme-title text-sm font-black">{value}</p>
      <p className="theme-soft text-[10px] font-bold uppercase tracking-[0.14em]">{label}</p>
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  const classes =
    status === "PAID"
      ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-300"
      : status === "OVERDUE"
        ? "border-red-500/30 bg-red-500/10 text-red-600 dark:text-red-300"
        : "border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-300";
  return <span className={`rounded-full border px-3 py-1 text-[11px] font-black uppercase tracking-[0.12em] ${classes}`}>{status}</span>;
}

function Empty({ text }: { text: string }) {
  return <p className="theme-card theme-muted mt-5 rounded-2xl border-dashed p-6 text-center text-sm">{text}</p>;
}

function parsePlayers(value: string) {
  return value
    .split(/[,\n]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function formatDate(value?: string) {
  if (!value) return "--";
  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
}

function formatTimeRange(value: { startTime?: string; endTime?: string }) {
  if (!value.startTime && !value.endTime) return "--";
  return `${formatClock(value.startTime)} - ${formatClock(value.endTime)}`;
}

function formatClock(value?: string) {
  if (!value) return "--";
  return value.split(":").slice(0, 2).join(":");
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("en-IN", {
    currency: "INR",
    maximumFractionDigits: 0,
    style: "currency",
  }).format(value || 0);
}

function errorMessage(error: unknown, fallback: string) {
  if (typeof error === "object" && error !== null && "response" in error) {
    return (error as { response?: { data?: { message?: string } } }).response?.data?.message || fallback;
  }
  return fallback;
}
