"use client";
import { TileInfo } from "@/components/ui";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useConfirmDialog } from "@/components/ui/ConfirmDialog";
import { DataGrid, type DataGridColumn } from "@/components/ui/DataGrid";
import { cancelBooking, getMyBookings } from "@/modules/booking/services/bookingService";
import { Booking } from "@/modules/booking/types/booking.types";
import { useToast } from "@/modules/ui/ToastProvider";
import { formatDate, formatDateTime } from "@/utils/dateFormatter";

const statusColors: Record<string, string> = {
  CONFIRMED: "border-emerald-500/30 bg-emerald-500/12 text-emerald-600 dark:text-emerald-300",
  PENDING: "border-amber-500/30 bg-amber-500/12 text-amber-700 dark:text-amber-300",
  CANCELLED: "border-red-500/30 bg-red-500/12 text-red-700 dark:text-red-300",
  COMPLETED: "border-sky-500/30 bg-sky-500/12 text-sky-700 dark:text-sky-300",
  EXPIRED: "border-slate-400/30 bg-slate-500/12 text-slate-600 dark:text-slate-300",
};

const paymentColors: Record<string, string> = {
  PAID: "text-emerald-600 dark:text-emerald-300",
  PENDING: "text-amber-700 dark:text-amber-300",
  FAILED: "text-red-700 dark:text-red-300",
  REFUNDED: "text-sky-700 dark:text-sky-300",
};

type BookingGridRow = Booking & {
  ids: number[];
  slotIds: number[];
  slotCount: number;
  totalAmount: number;
};

export default function BookingHistoryPage() {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const confirmAction = useConfirmDialog();
  const { showError, showSuccess } = useToast();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const refreshBookings = () => {
    setLoading(true);
    setError("");
    getMyBookings()
      .then((res) => setBookings(Array.isArray(res.data) ? res.data : []))
      .catch(() => {
        setBookings([]);
        setError("Failed to load bookings.");
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    let isMounted = true;

    getMyBookings()
      .then((res) => {
        if (isMounted) setBookings(Array.isArray(res.data) ? res.data : []);
      })
      .catch(() => {
        if (!isMounted) return;
        setBookings([]);
        setError("Failed to load bookings.");
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, []);

  const groupedBookings = useMemo(() => groupBookings(bookings), [bookings]);

  const stats = useMemo(() => {
    const total = groupedBookings.length;
    const active = groupedBookings.filter((booking) => ["CONFIRMED", "PENDING"].includes(booking.status)).length;
    const paid = groupedBookings.filter((booking) => booking.paymentStatus === "PAID").length;
    const spend = groupedBookings.reduce((sum, booking) => sum + booking.totalAmount, 0);
    return { total, active, paid, spend };
  }, [groupedBookings]);

  const handleCancel = async (ids: number[]) => {
    const confirmed = await confirmAction({
      title: "Cancel this booking?",
      description: "The selected booking slot will be cancelled. Any refund rules configured for the booking will still apply.",
      confirmLabel: "Cancel booking",
      tone: "danger",
    });
    if (!confirmed) return;
    try {
      await Promise.all(ids.map((id) => cancelBooking(id)));
      refreshBookings();
      showSuccess("Booking Cancelled", "Your booking was cancelled successfully.");
    } catch {
      showError("Cancellation Failed", "Could not cancel booking.");
    }
  };

  const columns: DataGridColumn<BookingGridRow>[] = [
    {
      key: "booking",
      header: "Booking",
      render: (booking) => (
        <div className="flex items-center gap-3">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-emerald-500/12 text-xs font-black text-emerald-600 dark:text-emerald-300">
            CB
          </div>
          <div className="min-w-0">
            <p className="theme-title truncate text-sm font-black">{booking.turfName ?? `Turf #${booking.turfId}`}</p>
            <p className="theme-soft mt-1 text-xs">{formatBookingIds(booking.ids)}</p>
          </div>
        </div>
      ),
      mobileClassName: "sm:col-span-2",
    },
    {
      key: "date",
      header: "Date",
      render: (booking) => <span className="theme-muted whitespace-nowrap text-sm font-semibold">{formatDate(booking.bookingDate)}</span>,
    },
    {
      key: "time",
      header: "Time",
      render: (booking) => <span className="theme-muted whitespace-nowrap text-sm">{formatTimeRange(booking)}</span>,
    },
    {
      key: "slots",
      header: "Slots",
      render: (booking) => (
        <div>
          <p className="theme-muted whitespace-nowrap text-sm font-semibold">{booking.slotCount} slot{booking.slotCount === 1 ? "" : "s"}</p>
          <p className="theme-soft mt-1 truncate text-xs">{booking.slotIds.map((slotId) => `#${slotId}`).join(", ")}</p>
        </div>
      ),
    },
    {
      key: "location",
      header: "Location",
      render: (booking) => (
        <span className="theme-muted line-clamp-2 text-sm">{booking.turfLocation || booking.turfCity || "Location not available"}</span>
      ),
      mobileClassName: "sm:col-span-2",
    },
    {
      key: "payment",
      header: "Payment",
      render: (booking) => {
        const paymentStatus = booking.paymentStatus ?? "PENDING";
        return <span className={`text-xs font-black uppercase tracking-[0.14em] ${paymentColors[paymentStatus] ?? "theme-muted"}`}>{paymentStatus}</span>;
      },
    },
    {
      key: "status",
      header: "Status",
      render: (booking) => <StatusBadge status={booking.status} />,
    },
    {
      key: "amount",
      header: "Amount",
      render: (booking) => (
        <span className="whitespace-nowrap text-sm font-black text-emerald-600 dark:text-emerald-300">
          {formatCurrency(booking.totalAmount)}
        </span>
      ),
    },
    {
      key: "created",
      header: "Booked On",
      render: (booking) => <span className="theme-muted whitespace-nowrap text-sm">{formatDateTime(booking.createdAt)}</span>,
    },
    {
      key: "action",
      header: "Action",
      render: (booking) => <ActionButton booking={booking} onCancel={handleCancel} />,
      mobileClassName: "sm:col-span-2",
    },
  ];

  return (
    <main className="theme-page-player min-h-screen">
      <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <section className="theme-panel-strong overflow-hidden rounded-[2rem]">
          <div className="border-b border-[var(--theme-border)] p-6 sm:p-8">
            <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
              <div>
                <p className="theme-kicker text-xs font-black uppercase tracking-[0.24em]">Booking history</p>
                <h1 className="theme-title mt-3 text-3xl font-black tracking-tight sm:text-4xl">My Bookings
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="My Bookings information" focusable={false}>Review every turf reservation with schedule, payment state, booking status, amount, and quick actions in one grid.</TileInfo>
              </span></h1>
              </div>
              <Link
                href="/search"
                className="inline-flex items-center justify-center rounded-2xl bg-emerald-500 px-5 py-3 text-sm font-black text-white shadow-lg shadow-emerald-500/20 transition hover:bg-emerald-400 focus:outline-none focus:ring-4 focus:ring-emerald-500/20"
              >
                New Booking
              </Link>
            </div>

            <div className="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
              <Metric label="Total bookings" value={stats.total} />
              <Metric label="Active bookings" value={stats.active} />
              <Metric label="Paid bookings" value={stats.paid} />
              <Metric label="Total value" value={formatCurrency(stats.spend)} />
            </div>
          </div>

          <div className="p-4 sm:p-6">
            {loading && (
              <div className="theme-panel-soft flex min-h-64 items-center justify-center rounded-3xl">
                <div className="theme-muted flex items-center gap-3 text-sm font-semibold">
                  <svg className="h-5 w-5 animate-spin text-emerald-500" viewBox="0 0 24 24" fill="none">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
                  </svg>
                  Loading bookings...
                </div>
              </div>
            )}

            {!loading && error && (
              <div className="rounded-2xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm font-semibold text-red-700 dark:text-red-300">
                {error}
              </div>
            )}

            {!loading && !error && bookings.length === 0 && (
              <div className="theme-panel-soft flex min-h-72 flex-col items-center justify-center rounded-3xl border-dashed p-8 text-center">
                <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-500/15 text-2xl">BK</div>
                <p className="theme-title text-lg font-black">No bookings yet</p>
                <p className="theme-muted mt-2 max-w-sm text-sm">Search available turfs and your confirmed slots will appear here.</p>
                <Link href="/search" className="mt-5 rounded-2xl bg-emerald-500 px-5 py-3 text-sm font-black text-white hover:bg-emerald-400">
                  Find Turfs
                </Link>
              </div>
            )}

            {!loading && !error && bookings.length > 0 && (
              <DataGrid
                columns={columns}
                rows={groupedBookings}
                getRowKey={(booking) => booking.id}
                gridTemplateColumns="minmax(200px,1.3fr) minmax(105px,0.7fr) minmax(130px,0.8fr) minmax(105px,0.7fr) minmax(160px,1fr) minmax(105px,0.7fr) minmax(105px,0.7fr) minmax(115px,0.7fr) minmax(130px,0.8fr) minmax(95px,0.6fr)"
              />
            )}
          </div>
        </section>
      </div>
    </main>
  );
}

function groupBookings(bookings: Booking[]): BookingGridRow[] {
  const groups = new Map<string, Booking[]>();
  for (const booking of bookings) {
    const key = [
      booking.turfId,
      booking.bookingDate,
      booking.status,
      booking.paymentStatus ?? "PENDING",
      booking.createdAt ? booking.createdAt.slice(0, 16) : `booking-${booking.id}`,
    ].join("|");
    groups.set(key, [...(groups.get(key) ?? []), booking]);
  }

  return Array.from(groups.values()).map((items) => {
    const sorted = [...items].sort((first, second) => String(first.startTime ?? "").localeCompare(String(second.startTime ?? "")));
    const first = sorted[0];
    const ids = sorted.map((booking) => booking.id);
    const slotIds = sorted.flatMap((booking) => booking.slotIds?.length ? booking.slotIds : [booking.slotId]);
    const totalAmount = sorted.reduce((sum, booking) => sum + Number(booking.totalAmount ?? booking.amount ?? 0), 0);

    return {
      ...first,
      id: ids[0],
      ids,
      slotIds,
      slotCount: slotIds.length,
      startTime: sorted[0]?.startTime,
      endTime: sorted[sorted.length - 1]?.endTime,
      totalAmount,
    };
  });
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="theme-panel-soft rounded-2xl p-4">
      <p className="theme-soft text-[11px] font-black uppercase tracking-[0.18em]">{label}</p>
      <p className="theme-title mt-2 text-2xl font-black">{value}</p>
    </div>
  );
}

function StatusBadge({ status }: { status: Booking["status"] }) {
  return (
    <span className={`inline-flex whitespace-nowrap rounded-full border px-3 py-1 text-[11px] font-black uppercase tracking-[0.12em] ${statusColors[status] ?? "theme-card theme-muted"}`}>
      {status}
    </span>
  );
}

function ActionButton({ booking, onCancel }: { booking: BookingGridRow; onCancel: (ids: number[]) => void }) {
  const canCancel = booking.status === "CONFIRMED" || booking.status === "PENDING";
  if (!canCancel) {
    return <span className="theme-soft inline-flex text-xs font-semibold">No action</span>;
  }

  return (
    <button
      onClick={() => onCancel(booking.ids)}
      className="inline-flex rounded-xl border border-red-500/30 px-4 py-2 text-xs font-black text-red-700 transition hover:bg-red-500/10 focus:outline-none focus:ring-4 focus:ring-red-500/15 dark:text-red-300"
    >
      Cancel
    </button>
  );
}



function formatTimeRange(booking: Booking) {
  if (!booking.startTime && !booking.endTime) return "--";
  return `${formatClock(booking.startTime)} - ${formatClock(booking.endTime)}`;
}

function formatClock(value?: string) {
  if (!value) return "--";
  return value.split(":").slice(0, 2).join(":");
}

function formatBookingIds(ids: number[]) {
  if (ids.length === 1) return `#${ids[0]}`;
  return `#${ids[0]} + ${ids.length - 1} more`;
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("en-IN", {
    currency: "INR",
    maximumFractionDigits: 0,
    style: "currency",
  }).format(value);
}
