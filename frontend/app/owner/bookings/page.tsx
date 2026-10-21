"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import { Button, Field, Input, Select, Textarea, useConfirmDialog } from "@/components/ui";
import { PageMetricCard, PageSection, PageSectionHeader } from "@/components/ui/PageSurface";
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
import type { Booking } from "@/modules/booking/types/booking.types";
import { useToast } from "@/modules/ui/ToastProvider";
import { formatDate, formatDateTime } from "@/utils/dateFormatter";

const STATUS_FILTERS = [
  { label: "All statuses", value: "" },
  { label: "Pending payment", value: "PENDING" },
  { label: "Confirmed", value: "CONFIRMED" },
  { label: "Checked in", value: "CHECKED_IN" },
  { label: "Completed", value: "COMPLETED" },
  { label: "No-show", value: "NO_SHOW" },
  { label: "Cancelled", value: "CANCELLED" },
];

type QuickView = "TODAY" | "ARRIVALS" | "IN_PROGRESS" | "COMPLETED" | "NO_SHOW" | "ALL";

export default function OwnerBookingsPage() {
  const { toast } = useToast();
  const confirm = useConfirmDialog();
  const [filters, setFilters] = useState<OwnerBookingFilters>({ date: localDateValue(new Date()) });
  const [summary, setSummary] = useState<OwnerBookingSummary | null>(null);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [selected, setSelected] = useState<OwnerBookingDetails | null>(null);
  const [note, setNote] = useState("");
  const [loading, setLoading] = useState(true);
  const [actionBusy, setActionBusy] = useState<number | null>(null);

  const loadData = useCallback(async (nextFilters: OwnerBookingFilters) => {
    setLoading(true);
    try {
      const [summaryRes, bookingsRes] = await Promise.all([
        getOwnerBookingSummary(),
        getOwnerBookings(nextFilters),
      ]);
      setSummary(summaryRes.data);
      setBookings(bookingsRes.data || []);
    } catch (error) {
      setSummary(null);
      setBookings([]);
      toast(errorMessage(error, "Could not load bookings right now."), "error");
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    const timer = window.setTimeout(() => void loadData(filters), filters.query ? 250 : 0);
    return () => window.clearTimeout(timer);
  }, [filters, loadData]);

  const updateFilter = (key: keyof OwnerBookingFilters, value: string) => {
    setFilters((current) => ({ ...current, [key]: value || undefined }));
    setSelected(null);
  };

  const applyQuickView = (view: QuickView) => {
    const today = localDateValue(new Date());
    const views: Record<QuickView, OwnerBookingFilters> = {
      TODAY: { date: today },
      ARRIVALS: { date: today, status: "CONFIRMED" },
      IN_PROGRESS: { date: today, status: "CHECKED_IN" },
      COMPLETED: { status: "COMPLETED" },
      NO_SHOW: { status: "NO_SHOW" },
      ALL: {},
    };
    setFilters(views[view]);
    setSelected(null);
  };

  const clearFilters = () => {
    setFilters({});
    setSelected(null);
  };

  const refreshSelected = async (bookingId: number) => {
    try {
      const detailRes = await getOwnerBookingDetails(bookingId);
      setSelected(detailRes.data);
      setNote("");
    } catch (error) {
      toast(errorMessage(error, "Could not load booking details."), "error");
    }
  };

  const runBookingAction = async (
    booking: Booking,
    action: () => Promise<{ data: Booking }>,
    successMessage: string,
  ) => {
    setActionBusy(booking.id);
    try {
      const res = await action();
      setBookings((current) => current.map((item) => (item.id === booking.id ? res.data : item)));
      if (selected?.booking.id === booking.id) await refreshSelected(booking.id);
      const summaryRes = await getOwnerBookingSummary();
      setSummary(summaryRes.data);
      toast(successMessage, "success");
    } catch (error) {
      toast(errorMessage(error, "The booking could not be updated."), "error");
    } finally {
      setActionBusy(null);
    }
  };

  const confirmAction = async (
    options: { title: string; description: string; confirmLabel: string; tone: "owner" | "danger" },
    action: () => void,
  ) => {
    if (await confirm(options)) action();
  };

  const addNote = async () => {
    if (!selected || !note.trim() || note.length > 700) return;
    try {
      await addOwnerBookingNote(selected.booking.id, note.trim());
      await refreshSelected(selected.booking.id);
      toast("Operation note saved.", "success");
    } catch (error) {
      toast(errorMessage(error, "Could not save the note."), "error");
    }
  };

  const visibleRevenue = useMemo(
    () => bookings.reduce((total, booking) => total + Number(booking.amount ?? booking.totalAmount ?? 0), 0),
    [bookings],
  );
  const activeView = quickViewFor(filters);
  const hasFilters = Boolean(filters.query || filters.date || filters.status || filters.turfId);

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <PageSection>
          <PageSectionHeader
            eyebrow="Owner operations"
            title="Bookings"
            description="Manage arrivals, attendance, cancellations, refunds, notes, and booking history from one workspace."
            actions={
              <div className="theme-card rounded-xl px-4 py-3 text-sm">
                <span className="theme-soft">Filtered booking value</span>
                <span className="theme-title ml-3 font-black">₹{visibleRevenue.toLocaleString("en-IN")}</span>
              </div>
            }
          />
          <div className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-6">
            <PageMetricCard label="Today's bookings" value={summary?.todayBookings ?? 0} helper="All bookings scheduled for today." />
            <PageMetricCard label="Awaiting arrival" value={summary?.pendingArrivals ?? 0} helper="Today's confirmed bookings that have not checked in." />
            <PageMetricCard label="In progress" value={summary?.activeMatches ?? 0} helper="Today's checked-in bookings that are currently active." />
            <PageMetricCard label="Today's revenue" value={`₹${Number(summary?.todayRevenue ?? 0).toLocaleString("en-IN")}`} helper="Value of today's confirmed, checked-in, and completed bookings." />
            <PageMetricCard label="Overall attendance" value={`${summary?.attendanceRate ?? 0}%`} helper="Share of attendance outcomes recorded as checked in or completed." />
            <PageMetricCard label="Overall no-show" value={`${summary?.noShowRate ?? 0}%`} helper="Share of attendance outcomes recorded as no-show." valueClassName={(summary?.noShowRate ?? 0) > 0 ? "text-amber-300" : ""} />
          </div>
        </PageSection>

        <PageSection>
          <div className="flex flex-col gap-4 border-b border-white/10 pb-4 xl:flex-row xl:items-center xl:justify-between">
            <div>
              <h2 className="theme-title text-lg font-black">Booking workflow</h2>
              <p className="theme-muted mt-1 text-sm">Choose a queue, then narrow the results only when needed.</p>
            </div>
            <div className="flex flex-wrap gap-2" aria-label="Booking workflow views">
              <QuickViewButton active={activeView === "TODAY"} label="Today" count={summary?.todayBookings} onClick={() => applyQuickView("TODAY")} />
              <QuickViewButton active={activeView === "ARRIVALS"} label="Awaiting arrival" count={summary?.pendingArrivals} onClick={() => applyQuickView("ARRIVALS")} />
              <QuickViewButton active={activeView === "IN_PROGRESS"} label="In progress" count={summary?.activeMatches} onClick={() => applyQuickView("IN_PROGRESS")} />
              <QuickViewButton active={activeView === "COMPLETED"} label="Completed" count={summary?.completedBookings} onClick={() => applyQuickView("COMPLETED")} />
              <QuickViewButton active={activeView === "NO_SHOW"} label="No-show" count={summary?.noShowBookings} onClick={() => applyQuickView("NO_SHOW")} />
              <QuickViewButton active={activeView === "ALL"} label="All bookings" count={summary?.totalBookings} onClick={() => applyQuickView("ALL")} />
            </div>
          </div>

          <div className="mt-4 grid gap-3 md:grid-cols-2 xl:grid-cols-[1.4fr_0.8fr_0.8fr_auto]">
            <Field label="Search bookings">
              <Input
                value={filters.query ?? ""}
                onChange={(event) => updateFilter("query", event.target.value)}
                placeholder="Booking number, player, phone, or turf"
              />
            </Field>
            <Field label="Booking date">
              <Input type="date" value={filters.date ?? ""} onChange={(event) => updateFilter("date", event.target.value)} />
            </Field>
            <Field label="Booking status">
              <Select value={filters.status ?? ""} onChange={(event) => updateFilter("status", event.target.value)} options={STATUS_FILTERS} />
            </Field>
            <div className="flex items-end gap-2">
              <Button onClick={() => void loadData(filters)} className="min-h-12">Refresh</Button>
              <Button tone="secondary" onClick={clearFilters} disabled={!hasFilters} className="min-h-12">Show all</Button>
            </div>
          </div>
        </PageSection>

        <div className="grid gap-6 xl:grid-cols-[minmax(0,1.45fr)_minmax(22rem,0.75fr)]">
          <section className="theme-panel overflow-hidden rounded-2xl">
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-white/10 px-5 py-4">
              <div>
                <h2 className="theme-title text-lg font-black">{queueTitle(activeView)}</h2>
                <p className="theme-muted mt-1 text-sm">{loading ? "Updating results..." : `${bookings.length} booking${bookings.length === 1 ? "" : "s"} shown`}</p>
              </div>
              {hasFilters && <span className="rounded-full bg-sky-500/10 px-3 py-1 text-xs font-bold text-sky-300">Filters active</span>}
            </div>
            {loading && <LoadingRows />}
            {!loading && bookings.length === 0 && (
              <EmptyQueue filtered={hasFilters} onShowAll={clearFilters} onShowToday={() => applyQuickView("TODAY")} />
            )}
            {!loading && bookings.length > 0 && (
              <div className="divide-y divide-white/10">
                {bookings.map((booking) => (
                  <BookingRow
                    key={booking.id}
                    booking={booking}
                    busy={actionBusy === booking.id}
                    selected={selected?.booking.id === booking.id}
                    onSelect={() => void refreshSelected(booking.id)}
                    onCancel={() => void confirmAction(
                      { title: "Cancel this booking?", description: `Booking #${booking.id} will be cancelled and its slot will be released.`, confirmLabel: "Cancel booking", tone: "danger" },
                      () => void runBookingAction(booking, () => cancelOwnerBooking(booking.id), "Booking cancelled."),
                    )}
                    onRefund={() => void confirmAction(
                      { title: "Issue this refund?", description: `The paid amount for booking #${booking.id} will be returned to the player's wallet.`, confirmLabel: "Issue refund", tone: "owner" },
                      () => void runBookingAction(booking, () => triggerRefund(booking.id).then(() => ({ data: { ...booking, paymentStatus: "REFUNDED" as const } })), "Refund issued to the player's wallet."),
                    )}
                    onCheckIn={() => void runBookingAction(booking, () => checkInOwnerBooking(booking.id, "Player arrived at venue."), "Player checked in.")}
                    onComplete={() => void runBookingAction(booking, () => completeOwnerBooking(booking.id, "Match completed by owner."), "Booking completed.")}
                    onNoShow={() => void confirmAction(
                      { title: "Record a no-show?", description: `Use this only when the player did not arrive for booking #${booking.id}.`, confirmLabel: "Record no-show", tone: "danger" },
                      () => void runBookingAction(booking, () => markOwnerNoShow(booking.id, "Player did not arrive."), "No-show recorded."),
                    )}
                  />
                ))}
              </div>
            )}
          </section>

          <aside className="theme-panel self-start rounded-2xl p-5 xl:sticky xl:top-5">
            {!selected && (
              <div className="theme-card rounded-2xl border-dashed p-8 text-center">
                <div className="mx-auto flex h-11 w-11 items-center justify-center rounded-full bg-sky-500/10 text-sky-300">
                  <svg aria-hidden="true" viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2"><path d="M9 11h6M9 15h4" /><path d="M7 3h10a2 2 0 0 1 2 2v14l-3-2-4 2-4-2-3 2V5a2 2 0 0 1 2-2Z" /></svg>
                </div>
                <h2 className="theme-title mt-4 font-black">Select a booking</h2>
                <p className="theme-muted mt-2 text-sm leading-6">Open a row to review contact, payment, attendance, notes, and the complete activity timeline.</p>
              </div>
            )}
            {selected && (
              <div className="space-y-5">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="theme-soft text-xs font-black uppercase tracking-[0.22em]">Booking #{selected.booking.id}</p>
                    <h2 className="theme-title mt-1 text-xl font-black">{selected.booking.playerName || "Player"}</h2>
                    <p className="theme-muted mt-1 text-sm">{selected.booking.playerPhone || selected.booking.playerEmail || "No contact available"}</p>
                  </div>
                  <StatusBadge status={selected.booking.status} />
                </div>
                <div className="theme-panel-soft rounded-2xl p-4">
                  <p className="theme-title font-bold">{selected.booking.turfName || `Turf #${selected.booking.turfId}`}</p>
                  <p className="theme-muted mt-1 text-sm">{formatDate(selected.booking.bookingDate)} · {selected.booking.startTime || "--"}–{selected.booking.endTime || "--"}</p>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <Detail label="Payment" value={selected.latestPaymentStatus || selected.booking.paymentStatus || "PENDING"} />
                  <Detail label="Attendance" value={selected.booking.attendanceStatus || "Not recorded"} />
                  <Detail label="Cancellation" value={selected.latestCancellationStatus || "None"} />
                  <Detail label="Amount" value={`₹${Number(selected.booking.amount ?? selected.booking.totalAmount ?? 0).toLocaleString("en-IN")}`} />
                </div>
                <div>
                  <div className="flex items-center justify-between gap-3">
                    <label htmlFor="owner-booking-note" className="theme-title text-sm font-bold">Operation note</label>
                    <span className={`text-xs ${note.length > 700 ? "text-red-300" : "theme-soft"}`}>{note.length}/700</span>
                  </div>
                  <Textarea
                    id="owner-booking-note"
                    value={note}
                    maxLength={700}
                    onChange={(event) => setNote(event.target.value)}
                    rows={3}
                    placeholder="Equipment request, late arrival, or other staff context"
                    className="mt-2"
                  />
                  <Button onClick={() => void addNote()} disabled={!note.trim() || note.length > 700} className="mt-2 min-h-10 py-2">Save note</Button>
                </div>
                <div>
                  <h3 className="theme-title text-sm font-black">Activity timeline</h3>
                  <div className="mt-3 space-y-3">
                    {selected.timeline.length === 0 && <p className="theme-card theme-muted rounded-2xl border-dashed p-4 text-sm">No activity has been recorded yet.</p>}
                    {selected.timeline.map((item) => (
                      <div key={item.id} className="theme-panel-soft rounded-2xl p-3">
                        <p className="theme-title text-sm font-bold">{sentenceCase(item.eventType)}</p>
                        <p className="theme-muted mt-1 text-xs leading-5">{item.details}</p>
                        <p className="theme-soft mt-2 text-[11px]">{item.performedBy} · {item.createdAt ? formatDateTime(item.createdAt) : "--"}</p>
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
  const now = new Date();
  const startsAt = bookingDateTime(booking.bookingDate, booking.startTime);
  const endsAt = bookingEndDateTime(booking);
  const checkInOpensAt = startsAt ? new Date(startsAt.getTime() - 2 * 60 * 60 * 1000) : null;
  const checkInClosesAt = endsAt ? new Date(endsAt.getTime() + 30 * 60 * 1000) : null;
  const isConfirmed = booking.status === "CONFIRMED";
  const canCheckIn = isConfirmed && !!checkInOpensAt && !!checkInClosesAt && now >= checkInOpensAt && now <= checkInClosesAt;
  const canComplete = booking.status === "CHECKED_IN" && !!endsAt && now >= endsAt;
  const canNoShow = isConfirmed && !!endsAt && now >= endsAt;
  const canCancel = booking.status === "PENDING" || booking.status === "CONFIRMED";
  const canRefund = booking.status === "CANCELLED" && booking.paymentStatus === "PAID";

  return (
    <article className={`p-5 transition ${selected ? "bg-sky-500/10 ring-1 ring-inset ring-sky-500/20" : "hover:bg-white/5"}`}>
      <div className="flex flex-col gap-4 2xl:flex-row 2xl:items-center 2xl:justify-between">
        <button onClick={onSelect} className="min-w-0 flex-1 text-left" aria-label={`Open booking ${booking.id}`}>
          <div className="flex flex-wrap items-center gap-2">
            <p className="theme-title font-black">{booking.turfName || `Turf #${booking.turfId}`}</p>
            <StatusBadge status={booking.status} />
            {booking.paymentStatus && <span className="theme-soft rounded-full border border-white/10 px-2.5 py-1 text-[10px] font-bold">{sentenceCase(booking.paymentStatus)}</span>}
          </div>
          <p className="theme-muted mt-2 text-sm">{formatDate(booking.bookingDate)} · {booking.startTime || "--"}–{booking.endTime || "--"}</p>
          <p className="theme-soft mt-1 text-xs">#{booking.id} · {booking.playerName || "Player"} · {booking.playerPhone || booking.playerEmail || "No contact"}</p>
          <p className="mt-2 text-xs font-semibold text-sky-300">{workflowHint(booking, startsAt, endsAt, now)}</p>
        </button>
        <div className="flex flex-col items-start gap-3 2xl:items-end">
          <span className="theme-title text-lg font-black">₹{Number(booking.amount ?? booking.totalAmount ?? 0).toLocaleString("en-IN")}</span>
          <div className="flex flex-wrap items-center gap-2">
            {canCheckIn && <Action disabled={busy} label="Check in" onClick={onCheckIn} tone="success" />}
            {canComplete && <Action disabled={busy} label="Complete" onClick={onComplete} tone="primary" />}
            {canNoShow && <Action disabled={busy} label="No-show" onClick={onNoShow} tone="warning" />}
            {canCancel && <Action disabled={busy} label="Cancel" onClick={onCancel} tone="danger" />}
            {canRefund && <Action disabled={busy} label="Issue refund" onClick={onRefund} tone="success" />}
            <Action disabled={busy} label={selected ? "Viewing details" : "View details"} onClick={onSelect} tone="primary" />
          </div>
        </div>
      </div>
    </article>
  );
}

function QuickViewButton({ active, label, count, onClick }: { active: boolean; label: string; count?: number; onClick: () => void }) {
  return (
    <button
      type="button"
      aria-pressed={active}
      onClick={onClick}
      className={`rounded-xl border px-3 py-2 text-xs font-black transition ${active ? "border-sky-400 bg-sky-500 text-white" : "border-white/10 bg-white/5 theme-muted hover:border-sky-500/40 hover:text-sky-300"}`}
    >
      {label} <span className={active ? "text-white/80" : "theme-soft"}>({count ?? 0})</span>
    </button>
  );
}

function EmptyQueue({ filtered, onShowAll, onShowToday }: { filtered: boolean; onShowAll: () => void; onShowToday: () => void }) {
  return (
    <div className="m-5 rounded-2xl border border-dashed border-white/15 bg-white/[0.025] px-6 py-12 text-center">
      <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-emerald-500/10 text-emerald-300">
        <svg aria-hidden="true" viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="2"><path d="m7 12 3 3 7-7" /><circle cx="12" cy="12" r="9" /></svg>
      </div>
      <h3 className="theme-title mt-4 text-lg font-black">{filtered ? "No bookings match these filters" : "No bookings yet"}</h3>
      <p className="theme-muted mx-auto mt-2 max-w-md text-sm leading-6">{filtered ? "Try another workflow view or remove the current filters." : "New bookings will appear here as soon as players reserve your turf."}</p>
      <div className="mt-5 flex justify-center gap-2">
        {filtered && <Button tone="secondary" onClick={onShowAll}>Show all</Button>}
        <Button onClick={onShowToday}>View today</Button>
      </div>
    </div>
  );
}

function LoadingRows() {
  return (
    <div className="space-y-3 p-5" aria-label="Loading bookings">
      {[0, 1, 2].map((item) => <div key={item} className="theme-card h-24 animate-pulse rounded-2xl" />)}
    </div>
  );
}

function Action({ label, onClick, disabled, tone }: { label: string; onClick: () => void; disabled: boolean; tone: "primary" | "success" | "warning" | "danger" }) {
  const styles = {
    primary: "border-sky-500/30 text-sky-300 hover:bg-sky-500/10",
    success: "border-emerald-500/30 text-emerald-300 hover:bg-emerald-500/10",
    warning: "border-amber-500/30 text-amber-300 hover:bg-amber-500/10",
    danger: "border-red-500/30 text-red-300 hover:bg-red-500/10",
  };
  return <button type="button" disabled={disabled} onClick={onClick} className={`rounded-xl border px-3 py-2 text-xs font-black transition disabled:cursor-not-allowed disabled:opacity-50 ${styles[tone]}`}>{busyLabel(label, disabled)}</button>;
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div className="theme-panel-soft rounded-2xl p-3">
      <p className="theme-soft text-[10px] font-black uppercase tracking-[0.16em]">{label}</p>
      <p className="theme-title mt-1 break-words text-sm font-bold">{sentenceCase(value)}</p>
    </div>
  );
}

function StatusBadge({ status }: { status: Booking["status"] }) {
  return <span className={`whitespace-nowrap rounded-full px-3 py-1 text-[10px] font-black uppercase tracking-wide ${statusTone(status)}`}>{sentenceCase(status)}</span>;
}

function statusTone(status: Booking["status"]) {
  if (status === "CHECKED_IN") return "bg-sky-500/15 text-sky-300";
  if (status === "COMPLETED") return "bg-emerald-500/15 text-emerald-300";
  if (status === "NO_SHOW") return "bg-amber-500/15 text-amber-300";
  if (status === "CANCELLED" || status === "EXPIRED" || status === "REFUNDED") return "bg-red-500/15 text-red-300";
  if (status === "PENDING") return "bg-yellow-500/15 text-yellow-300";
  return "bg-emerald-500/15 text-emerald-300";
}

function workflowHint(booking: Booking, startsAt: Date | null, endsAt: Date | null, now: Date) {
  if (booking.status === "CONFIRMED" && startsAt && now < new Date(startsAt.getTime() - 2 * 60 * 60 * 1000)) return `Check-in opens ${formatDateTime(new Date(startsAt.getTime() - 2 * 60 * 60 * 1000).toISOString())}`;
  if (booking.status === "CONFIRMED" && endsAt && now >= endsAt) return "Attendance outcome is ready to record";
  if (booking.status === "CONFIRMED") return "Ready for player check-in";
  if (booking.status === "CHECKED_IN" && endsAt && now < endsAt) return "Match is in progress";
  if (booking.status === "CHECKED_IN") return "Ready to complete";
  if (booking.status === "CANCELLED" && booking.paymentStatus === "PAID") return "Paid cancellation is eligible for refund";
  if (booking.status === "REFUNDED" || booking.paymentStatus === "REFUNDED") return "Refund has been issued";
  return "Open details to review booking history";
}

function queueTitle(view: QuickView | null) {
  const labels: Record<QuickView, string> = {
    TODAY: "Today's bookings",
    ARRIVALS: "Awaiting arrival",
    IN_PROGRESS: "Matches in progress",
    COMPLETED: "Completed bookings",
    NO_SHOW: "No-show records",
    ALL: "All bookings",
  };
  return view ? labels[view] : "Filtered bookings";
}

function quickViewFor(filters: OwnerBookingFilters): QuickView | null {
  if (filters.query || filters.turfId) return null;
  const today = localDateValue(new Date());
  if (filters.date === today && !filters.status) return "TODAY";
  if (filters.date === today && filters.status === "CONFIRMED") return "ARRIVALS";
  if (filters.date === today && filters.status === "CHECKED_IN") return "IN_PROGRESS";
  if (!filters.date && filters.status === "COMPLETED") return "COMPLETED";
  if (!filters.date && filters.status === "NO_SHOW") return "NO_SHOW";
  if (!filters.date && !filters.status) return "ALL";
  return null;
}

function bookingDateTime(date: string, time?: string) {
  if (!date || !time) return null;
  const value = new Date(`${date}T${time}`);
  return Number.isNaN(value.getTime()) ? null : value;
}

function bookingEndDateTime(booking: Booking) {
  const start = bookingDateTime(booking.bookingDate, booking.startTime);
  const end = bookingDateTime(booking.bookingDate, booking.endTime);
  if (!end) return null;
  if (start && end <= start) end.setDate(end.getDate() + 1);
  return end;
}

function localDateValue(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function sentenceCase(value: string) {
  const normalized = value.replaceAll("_", " ").toLowerCase();
  return normalized.charAt(0).toUpperCase() + normalized.slice(1);
}

function busyLabel(label: string, busy: boolean) {
  return busy ? "Working..." : label;
}

function errorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { message?: string; error?: string } } };
  return apiError.response?.data?.message || apiError.response?.data?.error || fallback;
}
