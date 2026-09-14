"use client";
import { TileInfo } from "@/components/ui";

import { useCallback, useEffect, useMemo, useState } from "react";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import {
  addOwnerBookingNote,
  cancelOwnerBooking,
  checkInOwnerBooking,
  completeOwnerBooking,
  getOwnerBookingDetails,
  getOwnerBookings,
  getOwnerBookingSummary,
  markOwnerNoShow,
  triggerRefund,
  type OwnerBookingDetails,
  type OwnerBookingFilters,
  type OwnerBookingSummary,
} from "@/modules/owner/services/ownerService";
import { Booking } from "@/modules/booking/types/booking.types";
import { useToast } from "@/modules/ui/ToastProvider";

const STATUS_FILTERS = [
  { label: "All", value: "" },
  { label: "Pending", value: "PENDING" },
  { label: "Confirmed", value: "CONFIRMED" },
  { label: "Checked-in", value: "CHECKED_IN" },
  { label: "Completed", value: "COMPLETED" },
  { label: "No-show", value: "NO_SHOW" },
  { label: "Cancelled", value: "CANCELLED" },
];

export default function OwnerBookingsPage() {
  const { toast } = useToast();
  const [filters, setFilters] = useState<OwnerBookingFilters>({ status: "CONFIRMED" });
  const [summary, setSummary] = useState<OwnerBookingSummary | null>(null);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [selected, setSelected] = useState<OwnerBookingDetails | null>(null);
  const [note, setNote] = useState("");
  const [loading, setLoading] = useState(true);
  const [actionBusy, setActionBusy] = useState<number | null>(null);

  const loadData = useCallback(async (nextFilters = filters) => {
    setLoading(true);
    try {
      const [summaryRes, bookingsRes] = await Promise.all([
        getOwnerBookingSummary(),
        getOwnerBookings(nextFilters),
      ]);
      setSummary(summaryRes.data);
      setBookings(bookingsRes.data || []);
    } catch {
      setSummary(null);
      setBookings([]);
      toast("Could not load owner booking operations.", "error");
    } finally {
      setLoading(false);
    }
  }, [filters, toast]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadData(filters);
    }, 0);
    return () => window.clearTimeout(timer);
  }, [filters, loadData]);

  const updateFilter = (key: keyof OwnerBookingFilters, value: string) => {
    setFilters((current) => ({ ...current, [key]: value || undefined }));
  };

  const clearFilters = () => {
    setFilters({});
    setSelected(null);
  };

  const refreshSelected = async (bookingId: number) => {
    const detailRes = await getOwnerBookingDetails(bookingId);
    setSelected(detailRes.data);
  };

  const runBookingAction = async (booking: Booking, action: () => Promise<{ data: Booking }>, successMessage: string) => {
    setActionBusy(booking.id);
    try {
      const res = await action();
      setBookings((current) => current.map((item) => (item.id === booking.id ? res.data : item)));
      if (selected?.booking.id === booking.id) {
        await refreshSelected(booking.id);
      }
      await getOwnerBookingSummary().then((summaryRes) => setSummary(summaryRes.data));
      toast(successMessage, "success");
    } catch {
      toast("Operation failed. Please verify booking status and timing.", "error");
    } finally {
      setActionBusy(null);
    }
  };

  const addNote = async () => {
    if (!selected || !note.trim()) return;
    try {
      await addOwnerBookingNote(selected.booking.id, note.trim());
      setNote("");
      await refreshSelected(selected.booking.id);
      toast("Note added.", "success");
    } catch {
      toast("Could not add note.", "error");
    }
  };

  const visibleRevenue = useMemo(
    () => bookings.reduce((total, booking) => total + Number(booking.amount ?? booking.totalAmount ?? 0), 0),
    [bookings]
  );

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <header className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
          <div>
            <p className="text-xs font-black uppercase tracking-[0.28em] text-sky-400">Owner Operations</p>
            <div>
            <h1 className="theme-title mt-2 text-3xl font-black">Booking command center
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Booking command center information" focusable={false}>Manage arrivals, check-ins, no-shows, cancellations, revenue, and booking history from one workspace.</TileInfo>
              </span></h1>
          </div>
          </div>
          <div className="theme-card rounded-2xl px-4 py-3 text-sm">
            <span className="theme-soft">Visible value</span>
            <span className="theme-title ml-3 font-black">Rs {visibleRevenue.toLocaleString("en-IN")}</span>
          </div>
        </header>

        <section className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
          <Metric label="Today's Bookings" value={summary?.todayBookings ?? 0} />
          <Metric label="Pending Arrivals" value={summary?.pendingArrivals ?? 0} />
          <Metric label="Checked In Today" value={summary?.checkedInToday ?? 0} />
          <Metric label="No-show Rate" value={`${summary?.noShowRate ?? 0}%`} tone="danger" />
          <Metric label="Today's Revenue" value={`Rs ${Number(summary?.todayRevenue ?? 0).toLocaleString("en-IN")}`} />
          <Metric label="Active Matches" value={summary?.activeMatches ?? 0} />
          <Metric label="Completed" value={summary?.completedBookings ?? 0} />
          <Metric label="Attendance" value={`${summary?.attendanceRate ?? 0}%`} />
        </section>

        <section className="theme-panel rounded-3xl p-4">
          <div className="grid gap-3 md:grid-cols-4">
            <Field label="Search">
              <input
                value={filters.query ?? ""}
                onChange={(event) => updateFilter("query", event.target.value)}
                placeholder="Booking #, player, phone, turf"
                className="theme-input w-full rounded-xl px-3 py-2 text-sm"
              />
            </Field>
            <Field label="Date">
              <input
                type="date"
                value={filters.date ?? ""}
                onChange={(event) => updateFilter("date", event.target.value)}
                className="theme-input w-full rounded-xl px-3 py-2 text-sm"
              />
            </Field>
            <Field label="Status">
              <select
                value={filters.status ?? ""}
                onChange={(event) => updateFilter("status", event.target.value)}
                className="theme-input w-full rounded-xl px-3 py-2 text-sm"
              >
                {STATUS_FILTERS.map((status) => (
                  <option key={status.value || "ALL"} value={status.value}>{status.label}</option>
                ))}
              </select>
            </Field>
            <div className="flex items-end gap-2">
              <button onClick={() => void loadData(filters)} className="rounded-xl bg-sky-500 px-4 py-2 text-sm font-bold text-white hover:bg-sky-400">
                Refresh
              </button>
              <button onClick={clearFilters} className="theme-card rounded-xl px-4 py-2 text-sm font-bold theme-muted hover:bg-white/5">
                Clear
              </button>
            </div>
          </div>
        </section>

        <div className="grid gap-6 xl:grid-cols-[1.4fr_0.8fr]">
          <section className="theme-panel overflow-hidden rounded-3xl">
            <div className="border-b border-white/10 px-5 py-4">
              <h2 className="theme-title text-lg font-black">Booking queue</h2>
              <p className="theme-muted text-sm">{bookings.length} records loaded for the selected criteria.</p>
            </div>
            {loading && <p className="theme-muted py-16 text-center">Loading bookings...</p>}
            {!loading && bookings.length === 0 && <p className="theme-card theme-muted m-5 rounded-2xl border-dashed p-10 text-center">No bookings found.</p>}
            {!loading && bookings.length > 0 && (
              <div className="divide-y divide-white/10">
                {bookings.map((booking) => (
                  <BookingRow
                    key={booking.id}
                    booking={booking}
                    busy={actionBusy === booking.id}
                    selected={selected?.booking.id === booking.id}
                    onSelect={() => void refreshSelected(booking.id)}
                    onCancel={() => void runBookingAction(booking, () => cancelOwnerBooking(booking.id), "Booking cancelled.")}
                    onRefund={() => void runBookingAction(booking, () => triggerRefund(booking.id).then(() => ({ data: { ...booking, paymentStatus: "REFUNDED" as const } })), "Refund requested.")}
                    onCheckIn={() => void runBookingAction(booking, () => checkInOwnerBooking(booking.id, "Player arrived at venue."), "Player checked in.")}
                    onComplete={() => void runBookingAction(booking, () => completeOwnerBooking(booking.id, "Match completed by owner."), "Booking completed.")}
                    onNoShow={() => void runBookingAction(booking, () => markOwnerNoShow(booking.id, "Player did not arrive."), "No-show recorded.")}
                  />
                ))}
              </div>
            )}
          </section>

          <aside className="theme-panel rounded-3xl p-5">
            {!selected && (
              <div className="theme-card theme-muted rounded-2xl border-dashed p-8 text-center">
                Select a booking to view player, payment, attendance, cancellation, and timeline details.
              </div>
            )}
            {selected && (
              <div className="space-y-5">
                <div>
                  <p className="theme-soft text-xs font-black uppercase tracking-[0.22em]">Booking #{selected.booking.id}</p>
                  <h2 className="theme-title mt-1 text-xl font-black">{selected.booking.playerName || "Player"}</h2>
                  <p className="theme-muted text-sm">{selected.booking.playerPhone || selected.booking.playerEmail || "No contact available"}</p>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <Detail label="Payment" value={selected.latestPaymentStatus || selected.booking.paymentStatus || "PENDING"} />
                  <Detail label="Attendance" value={selected.booking.attendanceStatus || "--"} />
                  <Detail label="Cancellation" value={selected.latestCancellationStatus || "--"} />
                  <Detail label="Slot" value={`${selected.booking.startTime || "--"} - ${selected.booking.endTime || "--"}`} />
                </div>
                <div>
                  <label className="theme-title text-sm font-bold">Add operation note</label>
                  <textarea
                    value={note}
                    onChange={(event) => setNote(event.target.value)}
                    rows={3}
                    placeholder="VIP customer, equipment request, late arrival details..."
                    className="theme-input mt-2 w-full rounded-2xl px-3 py-2 text-sm"
                  />
                  <button onClick={() => void addNote()} disabled={!note.trim()} className="mt-2 rounded-xl bg-emerald-500 px-4 py-2 text-sm font-bold text-white disabled:cursor-not-allowed disabled:bg-slate-500">
                    Save note
                  </button>
                </div>
                <div>
                  <h3 className="theme-title text-sm font-black">Timeline</h3>
                  <div className="mt-3 space-y-3">
                    {selected.timeline.length === 0 && <p className="theme-card theme-muted rounded-2xl border-dashed p-4 text-sm">No timeline events yet.</p>}
                    {selected.timeline.map((item) => (
                      <div key={item.id} className="theme-panel-soft rounded-2xl p-3">
                        <p className="theme-title text-sm font-bold">{item.eventType.replaceAll("_", " ")}</p>
                        <p className="theme-muted mt-1 text-xs">{item.details}</p>
                        <p className="theme-soft mt-2 text-[11px]">{item.performedBy} · {item.createdAt ? new Date(item.createdAt).toLocaleString() : "--"}</p>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </aside>
        </div>
      </div>
    </DashboardLayout>
  );
}

function BookingRow({
  booking,
  busy,
  selected,
  onSelect,
  onCancel,
  onRefund,
  onCheckIn,
  onComplete,
  onNoShow,
}: {
  booking: Booking;
  busy: boolean;
  selected: boolean;
  onSelect: () => void;
  onCancel: () => void;
  onRefund: () => void;
  onCheckIn: () => void;
  onComplete: () => void;
  onNoShow: () => void;
}) {
  const isConfirmed = booking.status === "CONFIRMED";
  const isCheckedIn = booking.status === "CHECKED_IN";
  const isCancellable = booking.status === "PENDING" || booking.status === "CONFIRMED";
  const bookingDate = new Date(`${booking.bookingDate}T00:00:00`);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const isTodayOrPast = bookingDate.getTime() <= today.getTime();

  return (
    <article className={`p-5 transition ${selected ? "bg-sky-500/10" : "hover:bg-white/5"}`}>
      <div className="flex flex-col gap-4 2xl:flex-row 2xl:items-center 2xl:justify-between">
        <button onClick={onSelect} className="min-w-0 text-left">
          <div className="flex flex-wrap items-center gap-2">
            <p className="theme-title font-black">{booking.turfName || `Turf #${booking.turfId}`}</p>
            <span className={`rounded-full px-3 py-1 text-[11px] font-black ${statusTone(booking.status)}`}>{booking.status.replaceAll("_", " ")}</span>
          </div>
          <p className="theme-muted mt-1 text-sm">{booking.bookingDate} · {booking.startTime || "--"} - {booking.endTime || "--"}</p>
          <p className="theme-soft mt-1 text-xs">
            #{booking.id} · {booking.playerName || "Player"} · {booking.playerPhone || booking.playerEmail || "No contact"}
          </p>
        </button>
        <div className="flex flex-wrap items-center gap-2">
          <span className="theme-title mr-2 font-black">Rs {Number(booking.amount ?? booking.totalAmount ?? 0).toLocaleString("en-IN")}</span>
          {isConfirmed && isTodayOrPast && <Action disabled={busy} label="Check In" onClick={onCheckIn} tone="success" />}
          {(isCheckedIn || (isConfirmed && isTodayOrPast)) && <Action disabled={busy} label="Complete" onClick={onComplete} tone="primary" />}
          {isConfirmed && isTodayOrPast && <Action disabled={busy} label="No Show" onClick={onNoShow} tone="warning" />}
          {isCancellable && <Action disabled={busy} label="Cancel" onClick={onCancel} tone="danger" />}
          {(booking.status === "CANCELLED" || booking.status === "REFUNDED") && <Action disabled={busy} label="Refund" onClick={onRefund} tone="success" />}
        </div>
      </div>
    </article>
  );
}

function Action({ label, onClick, disabled, tone }: { label: string; onClick: () => void; disabled: boolean; tone: "primary" | "success" | "warning" | "danger" }) {
  const styles = {
    primary: "border-sky-500/30 text-sky-300 hover:bg-sky-500/10",
    success: "border-emerald-500/30 text-emerald-300 hover:bg-emerald-500/10",
    warning: "border-amber-500/30 text-amber-300 hover:bg-amber-500/10",
    danger: "border-red-500/30 text-red-300 hover:bg-red-500/10",
  };
  return (
    <button disabled={disabled} onClick={onClick} className={`rounded-xl border px-3 py-1.5 text-xs font-black disabled:cursor-not-allowed disabled:opacity-50 ${styles[tone]}`}>
      {label}
    </button>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="theme-title mb-1 block text-xs font-bold">{label}</span>
      {children}
    </label>
  );
}

function Metric({ label, value, tone = "default" }: { label: string; value: string | number; tone?: "default" | "danger" }) {
  return (
    <div className="theme-panel rounded-2xl p-4">
      <p className="theme-soft text-xs font-black uppercase tracking-[0.18em]">{label}</p>
      <p className={`mt-2 text-2xl font-black ${tone === "danger" ? "text-red-300" : "theme-title"}`}>{value}</p>
    </div>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div className="theme-panel-soft rounded-2xl p-3">
      <p className="theme-soft text-[11px] font-black uppercase tracking-[0.16em]">{label}</p>
      <p className="theme-title mt-1 break-words text-sm font-bold">{value}</p>
    </div>
  );
}

function statusTone(status: Booking["status"]) {
  if (status === "CHECKED_IN") return "bg-sky-500/15 text-sky-300";
  if (status === "COMPLETED") return "bg-emerald-500/15 text-emerald-300";
  if (status === "NO_SHOW") return "bg-amber-500/15 text-amber-300";
  if (status === "CANCELLED" || status === "EXPIRED" || status === "REFUNDED") return "bg-red-500/15 text-red-300";
  if (status === "PENDING") return "bg-yellow-500/15 text-yellow-300";
  return "bg-emerald-500/15 text-emerald-300";
}
