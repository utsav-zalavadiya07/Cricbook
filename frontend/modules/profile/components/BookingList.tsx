"use client";

import Link from "next/link";
import { useEffect, useMemo, useRef, useState } from "react";
import type { Booking } from "@/modules/booking/types/booking.types";
import { formatDate } from "@/utils/dateFormatter";

type BookingListProps = {
  bookings?: Booking[];
  accent?: "sky" | "emerald";
  role?: string;
};

type StatusFilter = "ALL" | "CONFIRMED" | "COMPLETED" | "NO_SHOW" | "CANCELLED" | "PENDING";
type SortOption = "date-desc" | "date-asc" | "amount-desc" | "amount-asc";

const SORT_OPTIONS: { id: SortOption; label: string }[] = [
  { id: "date-desc", label: "Date (Newest first)" },
  { id: "date-asc", label: "Date (Oldest first)" },
  { id: "amount-desc", label: "Amount (High to Low)" },
  { id: "amount-asc", label: "Amount (Low to High)" },
];

export default function BookingList({ bookings = [], accent = "sky", role }: BookingListProps) {
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ALL");
  const [sortBy, setSortBy] = useState<SortOption>("date-desc");
  const [viewMode, setViewMode] = useState<"GRID" | "CARDS">("GRID");
  const [page, setPage] = useState(1);
  const [isSortOpen, setIsSortOpen] = useState(false);
  const sortRef = useRef<HTMLDivElement>(null);
  const itemsPerPage = 10;

  const activeAccentColor = accent === "emerald" ? "emerald" : "sky";

  // Close sort dropdown when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (sortRef.current && !sortRef.current.contains(event.target as Node)) {
        setIsSortOpen(false);
      }
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setIsSortOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, []);

  // Summary Metrics
  const metrics = useMemo(() => {
    const total = bookings.length;
    const confirmedCount = bookings.filter(
      (b) => b.status === "CONFIRMED" || b.status === "COMPLETED" || b.status === "CHECKED_IN"
    ).length;
    const completedCount = bookings.filter((b) => b.status === "COMPLETED").length;
    const cancelledCount = bookings.filter((b) => b.status === "CANCELLED").length;
    const noShowCount = bookings.filter((b) => b.status === "NO_SHOW").length;
    const pendingCount = bookings.filter((b) => b.status === "PENDING").length;
    const totalAmount = bookings.reduce((sum, b) => sum + (b.totalAmount ?? b.amount ?? 0), 0);
    return { total, confirmedCount, completedCount, cancelledCount, noShowCount, pendingCount, totalAmount };
  }, [bookings]);

  // Filtered and Sorted Bookings
  const filteredBookings = useMemo(() => {
    let result = [...bookings];

    // Status filter
    if (statusFilter !== "ALL") {
      result = result.filter((b) => {
        if (statusFilter === "CONFIRMED") return b.status === "CONFIRMED" || b.status === "CHECKED_IN";
        return b.status === statusFilter;
      });
    }

    // Search query
    if (search.trim()) {
      const q = search.toLowerCase().trim();
      result = result.filter((b) => {
        const turf = (b.turfName || "").toLowerCase();
        const city = (b.turfCity || b.turfLocation || "").toLowerCase();
        const player = (b.playerName || "").toLowerCase();
        const date = (b.bookingDate || "").toLowerCase();
        return turf.includes(q) || city.includes(q) || player.includes(q) || date.includes(q);
      });
    }

    // Sorting
    result.sort((a, b) => {
      if (sortBy === "date-desc") {
        return (b.bookingDate || "").localeCompare(a.bookingDate || "") || (b.startTime || "").localeCompare(a.startTime || "");
      }
      if (sortBy === "date-asc") {
        return (a.bookingDate || "").localeCompare(b.bookingDate || "") || (a.startTime || "").localeCompare(b.startTime || "");
      }
      if (sortBy === "amount-desc") {
        return (b.totalAmount ?? b.amount ?? 0) - (a.totalAmount ?? a.amount ?? 0);
      }
      if (sortBy === "amount-asc") {
        return (a.totalAmount ?? a.amount ?? 0) - (b.totalAmount ?? b.amount ?? 0);
      }
      return 0;
    });

    return result;
  }, [bookings, statusFilter, search, sortBy]);

  // Pagination
  const totalPages = Math.max(1, Math.ceil(filteredBookings.length / itemsPerPage));
  const currentPage = Math.min(page, totalPages);
  const paginatedBookings = useMemo(() => {
    const start = (currentPage - 1) * itemsPerPage;
    return filteredBookings.slice(start, start + itemsPerPage);
  }, [filteredBookings, currentPage, itemsPerPage]);

  const selectedSortLabel = SORT_OPTIONS.find((opt) => opt.id === sortBy)?.label || "Sort";

  if (!bookings || bookings.length === 0) {
    return (
      <div className="theme-panel rounded-3xl border border-dashed border-[var(--theme-border)] p-12 text-center">
        <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-white/5 text-slate-400">
          <CalendarIcon className="h-7 w-7" />
        </div>
        <h3 className="theme-title mt-4 text-lg font-black">No bookings found</h3>
        <p className="theme-muted mx-auto mt-1 max-w-sm text-sm">
          Booking activity connected to this account will appear here automatically.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* 1. Controls Bar: Search, Custom Dark Sort Dropdown, View Toggle */}
      <div className="theme-panel rounded-2xl border border-[var(--theme-border)] p-4 shadow-sm">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          {/* Search Box */}
          <div className="relative flex-1 max-w-md">
            <input
              type="text"
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(1);
              }}
              placeholder="Search by turf, city, player, or date..."
              className="w-full rounded-xl border border-[var(--theme-border)] bg-black/20 py-2.5 pl-9 pr-8 text-xs sm:text-sm font-medium text-[var(--theme-text)] placeholder:text-slate-500 focus:border-sky-500 focus:outline-none focus:ring-1 focus:ring-sky-500"
            />
            <span className="pointer-events-none absolute left-3 top-3 text-slate-400">
              <SearchIcon className="h-4 w-4" />
            </span>
            {search && (
              <button
                type="button"
                onClick={() => {
                  setSearch("");
                  setPage(1);
                }}
                className="absolute right-2.5 top-2.5 text-xs text-slate-400 hover:text-white"
              >
                ✕
              </button>
            )}
          </div>

          {/* Right Controls: Custom Styled Dark Sort Dropdown & Layout Toggle */}
          <div className="flex flex-wrap items-center gap-2">
            {/* Custom Dark Dropdown */}
            <div className="relative" ref={sortRef}>
              <button
                type="button"
                onClick={() => setIsSortOpen((prev) => !prev)}
                className="flex items-center gap-2 rounded-xl border border-[var(--theme-border)] bg-black/25 px-3.5 py-2 text-xs font-semibold text-[var(--theme-text)] transition hover:border-sky-500/40 hover:bg-black/40"
                aria-haspopup="listbox"
                aria-expanded={isSortOpen}
              >
                <span>{selectedSortLabel}</span>
                <ChevronDownIcon
                  className={`h-3.5 w-3.5 text-slate-400 transition-transform duration-200 ${
                    isSortOpen ? "rotate-180" : ""
                  }`}
                />
              </button>

              {isSortOpen && (
                <div
                  className="absolute right-0 top-full mt-1.5 z-40 w-52 rounded-xl border border-[var(--theme-border)] bg-[#0d1527] p-1.5 shadow-2xl backdrop-blur-xl ring-1 ring-white/10"
                  role="listbox"
                >
                  {SORT_OPTIONS.map((option) => {
                    const isSelected = sortBy === option.id;
                    return (
                      <button
                        key={option.id}
                        type="button"
                        role="option"
                        aria-selected={isSelected}
                        onClick={() => {
                          setSortBy(option.id);
                          setIsSortOpen(false);
                          setPage(1);
                        }}
                        className={`flex w-full items-center justify-between rounded-lg px-3 py-2 text-xs font-bold transition text-left ${
                          isSelected
                            ? activeAccentColor === "emerald"
                              ? "bg-emerald-500/20 text-emerald-300 font-black"
                              : "bg-sky-500/20 text-sky-300 font-black"
                            : "text-slate-300 hover:bg-white/10 hover:text-white"
                        }`}
                      >
                        <span>{option.label}</span>
                        {isSelected && <CheckIcon className="h-3.5 w-3.5 text-current shrink-0" />}
                      </button>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Layout Toggle: Table Grid vs Compact Cards */}
            <div className="flex rounded-xl border border-[var(--theme-border)] bg-black/20 p-0.5">
              <button
                type="button"
                onClick={() => setViewMode("GRID")}
                title="Table Grid View"
                className={`flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-xs font-bold transition ${
                  viewMode === "GRID"
                    ? activeAccentColor === "emerald"
                      ? "bg-emerald-500 text-white shadow"
                      : "bg-sky-500 text-white shadow"
                    : "theme-muted hover:text-white"
                }`}
              >
                <TableIcon className="h-3.5 w-3.5" />
                <span className="hidden sm:inline">Grid</span>
              </button>
              <button
                type="button"
                onClick={() => setViewMode("CARDS")}
                title="Compact Cards View"
                className={`flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-xs font-bold transition ${
                  viewMode === "CARDS"
                    ? activeAccentColor === "emerald"
                      ? "bg-emerald-500 text-white shadow"
                      : "bg-sky-500 text-white shadow"
                    : "theme-muted hover:text-white"
                }`}
              >
                <CardsIcon className="h-3.5 w-3.5" />
                <span className="hidden sm:inline">Cards</span>
              </button>
            </div>
          </div>
        </div>

        {/* Status Filter Pills */}
        <div className="mt-3 flex flex-wrap items-center gap-1.5 border-t border-[var(--theme-border)] pt-3">
          <span className="theme-soft mr-1 text-[11px] font-black uppercase tracking-wider">Status:</span>
          {(
            [
              { id: "ALL", label: "All", count: bookings.length },
              { id: "CONFIRMED", label: "Confirmed", count: metrics.confirmedCount },
              { id: "COMPLETED", label: "Completed", count: metrics.completedCount },
              { id: "NO_SHOW", label: "No Show", count: metrics.noShowCount },
              { id: "CANCELLED", label: "Cancelled", count: metrics.cancelledCount },
              { id: "PENDING", label: "Pending", count: metrics.pendingCount },
            ] as const
          ).map((item) => {
            const isSelected = statusFilter === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => {
                  setStatusFilter(item.id);
                  setPage(1);
                }}
                className={`flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-bold transition ${
                  isSelected
                    ? activeAccentColor === "emerald"
                      ? "bg-emerald-500 text-white shadow-md shadow-emerald-500/20"
                      : "bg-sky-500 text-white shadow-md shadow-sky-500/20"
                    : "theme-panel-soft theme-muted hover:bg-white/10 hover:text-white"
                }`}
              >
                <span>{item.label}</span>
                <span
                  className={`rounded-full px-1.5 py-0.2 text-[10px] font-black ${
                    isSelected ? "bg-white/20 text-white" : "bg-white/5 text-slate-400"
                  }`}
                >
                  {item.count}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* 2. Main Data Presentation */}
      {filteredBookings.length === 0 ? (
        <div className="theme-panel rounded-2xl border border-dashed border-[var(--theme-border)] p-10 text-center">
          <p className="theme-title text-base font-bold">No bookings match your current filter</p>
          <p className="theme-muted mt-1 text-xs">Try adjusting your search query or status filter.</p>
          <button
            type="button"
            onClick={() => {
              setSearch("");
              setStatusFilter("ALL");
              setPage(1);
            }}
            className="mt-4 rounded-xl bg-white/10 px-4 py-2 text-xs font-bold text-white transition hover:bg-white/15"
          >
            Clear Filters
          </button>
        </div>
      ) : viewMode === "GRID" ? (
        /* =================== TABULAR DATA GRID VIEW =================== */
        <div className="theme-panel rounded-2xl border border-[var(--theme-border)] shadow-xl overflow-hidden">
          <div className="overflow-x-auto">
            {/* table-fixed distributes columns proportionally without large empty gaps */}
            <table className="w-full table-fixed min-w-[780px] text-left border-collapse">
              <thead>
                <tr className="border-b border-[var(--theme-border)] bg-black/20 text-[11px] font-black uppercase tracking-wider text-slate-400">
                  <th className="py-3 px-4 w-[16%]">Date</th>
                  <th className="py-3 px-4 w-[16%]">Time Slot</th>
                  <th className="py-3 px-4 w-[25%]">Turf Venue</th>
                  <th className="py-3 px-4 w-[19%]">Booked By</th>
                  <th className="py-3 px-4 w-[11%] text-right">Amount</th>
                  <th className="py-3 px-4 w-[13%] text-center">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--theme-border)]/50 text-xs sm:text-sm">
                {paginatedBookings.map((booking) => {
                  const dateInfo = parseBookingDate(booking.bookingDate);
                  const timeSlot = formatSlotTime(booking.startTime, booking.endTime);
                  const statusStyle = getStatusStyle(booking.status);

                  return (
                    <tr
                      key={booking.id}
                      className="transition-colors hover:bg-white/[0.03]"
                    >
                      {/* 1. Date Column (Clean text without square icon block) */}
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        <div className="min-w-0">
                          <span className="theme-title block text-xs font-bold sm:text-sm">
                            {dateInfo.formatted}
                          </span>
                          <span className="theme-soft text-[10px] font-medium">#{booking.id}</span>
                        </div>
                      </td>

                      {/* 2. Time Slot */}
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        <div className="flex items-center gap-1.5">
                          <ClockIcon className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                          <span className="theme-title font-bold text-xs">{timeSlot}</span>
                        </div>
                      </td>

                      {/* 3. Turf Venue & City */}
                      <td className="py-3.5 px-4">
                        <div className="min-w-0 pr-2">
                          {booking.turfId ? (
                            <Link
                              href={`/turf/${booking.turfId}`}
                              className="theme-title block font-black hover:text-sky-400 truncate"
                            >
                              {booking.turfName || `Turf #${booking.turfId}`}
                            </Link>
                          ) : (
                            <span className="theme-title block font-black truncate">
                              {booking.turfName || "Unnamed Turf"}
                            </span>
                          )}
                          <div className="flex items-center gap-1 mt-0.5 text-slate-400 text-xs">
                            <PinIcon className="h-3 w-3 shrink-0" />
                            <span className="truncate">{booking.turfCity || booking.turfLocation || "Surat"}</span>
                          </div>
                        </div>
                      </td>

                      {/* 4. Booked By (Clean text without player avatar icon circle) */}
                      <td className="py-3.5 px-4">
                        <div className="min-w-0 pr-2">
                          <p className="theme-title truncate font-bold text-xs sm:text-sm">
                            {booking.playerName || "Player"}
                          </p>
                          {booking.playerPhone ? (
                            <p className="theme-soft truncate text-[11px] font-medium mt-0.5">{booking.playerPhone}</p>
                          ) : booking.playerEmail ? (
                            <p className="theme-soft truncate text-[11px] font-medium mt-0.5">{booking.playerEmail}</p>
                          ) : null}
                        </div>
                      </td>

                      {/* 5. Amount */}
                      <td className="py-3.5 px-4 text-right whitespace-nowrap">
                        <span className="theme-title font-black text-sm">
                          ₹{booking.totalAmount ?? booking.amount ?? 0}
                        </span>
                        {booking.paymentStatus && (
                          <span className="block text-[10px] font-bold text-slate-400 uppercase">
                            {booking.paymentStatus}
                          </span>
                        )}
                      </td>

                      {/* 6. Status Badge */}
                      <td className="py-3.5 px-4 text-center whitespace-nowrap">
                        <span
                          className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[10px] font-black uppercase tracking-wide ${statusStyle.badge}`}
                        >
                          <span className={`h-1.5 w-1.5 rounded-full ${statusStyle.dot}`} />
                          {booking.status.replaceAll("_", " ")}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        /* =================== COMPACT CARDS VIEW =================== */
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
          {paginatedBookings.map((booking) => {
            const dateInfo = parseBookingDate(booking.bookingDate);
            const timeSlot = formatSlotTime(booking.startTime, booking.endTime);
            const statusStyle = getStatusStyle(booking.status);

            return (
              <div
                key={booking.id}
                className="theme-panel rounded-2xl border border-[var(--theme-border)] p-4 transition hover:border-sky-500/30 flex flex-col justify-between gap-3 shadow-md"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <h4 className="theme-title font-black text-sm truncate">{booking.turfName || `Turf #${booking.turfId}`}</h4>
                    <p className="theme-muted text-xs truncate mt-0.5">{booking.turfCity || booking.turfLocation || "Surat"}</p>
                  </div>
                  <span
                    className={`inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-[9px] font-black uppercase tracking-wide shrink-0 ${statusStyle.badge}`}
                  >
                    {booking.status.replaceAll("_", " ")}
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-2 border-t border-[var(--theme-border)]/60 pt-2.5 text-xs">
                  <div>
                    <span className="theme-soft text-[10px] font-bold uppercase block">Date & Time</span>
                    <span className="theme-title font-bold text-xs">{dateInfo.formatted}</span>
                    <span className="theme-muted text-[11px] block">{timeSlot}</span>
                  </div>
                  <div className="text-right">
                    <span className="theme-soft text-[10px] font-bold uppercase block">Amount</span>
                    <span className="theme-title font-black text-xs text-emerald-400">
                      ₹{booking.totalAmount ?? booking.amount ?? 0}
                    </span>
                  </div>
                </div>

                {booking.playerName && (
                  <div className="flex items-center justify-between border-t border-[var(--theme-border)]/60 pt-2 text-[11px]">
                    <span className="theme-muted truncate">Booked by {booking.playerName}</span>
                    {booking.turfId && (
                      <Link
                        href={`/turf/${booking.turfId}`}
                        className="font-bold text-sky-400 hover:underline shrink-0 text-xs"
                      >
                        Details →
                      </Link>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* 3. Standard Pagination with Arrows (First, Prev, Page Numbers, Next, Last) */}
      {totalPages > 1 && (
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-3">
          <p className="theme-muted text-xs font-semibold">
            Showing <span className="theme-title font-bold">{(currentPage - 1) * itemsPerPage + 1}</span> to{" "}
            <span className="theme-title font-bold">
              {Math.min(currentPage * itemsPerPage, filteredBookings.length)}
            </span>{" "}
            of <span className="theme-title font-bold">{filteredBookings.length}</span> bookings
          </p>

          <div className="flex items-center gap-1">
            {/* First Page Button */}
            <button
              type="button"
              disabled={currentPage <= 1}
              onClick={() => setPage(1)}
              title="First Page"
              className="flex h-8 w-8 items-center justify-center rounded-xl border border-[var(--theme-border)] bg-white/5 text-slate-300 transition hover:bg-white/10 hover:text-white disabled:pointer-events-none disabled:opacity-30"
            >
              <DoubleLeftArrowIcon className="h-3.5 w-3.5" />
            </button>

            {/* Previous Page Button */}
            <button
              type="button"
              disabled={currentPage <= 1}
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              title="Previous Page"
              className="flex h-8 w-8 items-center justify-center rounded-xl border border-[var(--theme-border)] bg-white/5 text-slate-300 transition hover:bg-white/10 hover:text-white disabled:pointer-events-none disabled:opacity-30"
            >
              <LeftArrowIcon className="h-3.5 w-3.5" />
            </button>

            {/* Numbered Page Buttons */}
            {getPageNumbers(currentPage, totalPages).map((item, idx) => {
              if (item === "...") {
                return (
                  <span key={`dots-${idx}`} className="px-1.5 text-xs font-bold text-slate-500">
                    …
                  </span>
                );
              }
              const pageNum = item as number;
              const isCurrent = currentPage === pageNum;
              return (
                <button
                  key={pageNum}
                  type="button"
                  onClick={() => setPage(pageNum)}
                  className={`flex h-8 min-w-[32px] items-center justify-center rounded-xl px-2 text-xs font-black transition ${
                    isCurrent
                      ? activeAccentColor === "emerald"
                        ? "bg-emerald-500 text-white shadow-md shadow-emerald-500/25"
                        : "bg-sky-500 text-white shadow-md shadow-sky-500/25"
                      : "border border-[var(--theme-border)] bg-white/5 text-slate-300 hover:bg-white/10 hover:text-white"
                  }`}
                >
                  {pageNum}
                </button>
              );
            })}

            {/* Next Page Button */}
            <button
              type="button"
              disabled={currentPage >= totalPages}
              onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
              title="Next Page"
              className="flex h-8 w-8 items-center justify-center rounded-xl border border-[var(--theme-border)] bg-white/5 text-slate-300 transition hover:bg-white/10 hover:text-white disabled:pointer-events-none disabled:opacity-30"
            >
              <RightArrowIcon className="h-3.5 w-3.5" />
            </button>

            {/* Last Page Button */}
            <button
              type="button"
              disabled={currentPage >= totalPages}
              onClick={() => setPage(totalPages)}
              title="Last Page"
              className="flex h-8 w-8 items-center justify-center rounded-xl border border-[var(--theme-border)] bg-white/5 text-slate-300 transition hover:bg-white/10 hover:text-white disabled:pointer-events-none disabled:opacity-30"
            >
              <DoubleRightArrowIcon className="h-3.5 w-3.5" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

// ---------------- Standard Page Numbers Helper ----------------

function getPageNumbers(currentPage: number, totalPages: number): (number | "...")[] {
  // If total pages is 7 or fewer, show all numbers without any dots
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, i) => i + 1);
  }

  // When total pages > 7:
  // If close to the start: 1, 2, 3, 4, 5, ..., totalPages
  if (currentPage <= 4) {
    return [1, 2, 3, 4, 5, "...", totalPages];
  }

  // If close to the end: 1, ..., totalPages - 4, totalPages - 3, totalPages - 2, totalPages - 1, totalPages
  if (currentPage >= totalPages - 3) {
    return [1, "...", totalPages - 4, totalPages - 3, totalPages - 2, totalPages - 1, totalPages];
  }

  // In the middle: 1, ..., currentPage - 1, currentPage, currentPage + 1, ..., totalPages
  return [1, "...", currentPage - 1, currentPage, currentPage + 1, "...", totalPages];
}

// ---------------- Helpers ----------------

function parseBookingDate(dateStr?: string) {
  if (!dateStr) return { day: "--", month: "Date", year: "", formatted: "Pending" };

  if (dateStr.length === 10 && dateStr.includes("-")) {
    const parts = dateStr.split("-");
    if (parts.length === 3) {
      const isYearFirst = parts[0].length === 4;
      const year = isYearFirst ? parts[0] : parts[2];
      const monthNum = parseInt(isYearFirst ? parts[1] : parts[1], 10);
      const day = isYearFirst ? parts[2] : parts[0];
      const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
      const month = months[monthNum - 1] || "Date";
      return {
        day: day.padStart(2, "0"),
        month,
        year,
        formatted: `${day.padStart(2, "0")}-${(monthNum).toString().padStart(2, "0")}-${year}`,
      };
    }
  }

  try {
    const d = new Date(dateStr);
    if (!isNaN(d.getTime())) {
      const day = d.getDate().toString().padStart(2, "0");
      const month = d.toLocaleDateString("en-IN", { month: "short" });
      const year = d.getFullYear().toString();
      return {
        day,
        month,
        year,
        formatted: formatDate(dateStr) || `${day} ${month} ${year}`,
      };
    }
  } catch {}

  return { day: "--", month: "Date", year: "", formatted: dateStr };
}

function formatSlotTime(startTime?: string, endTime?: string): string {
  if (!startTime && !endTime) return "--";
  const clean = (t?: string) => {
    if (!t) return "--";
    const parts = t.trim().split(":");
    return parts.length >= 2 ? `${parts[0]}:${parts[1]}` : t;
  };
  return `${clean(startTime)} – ${clean(endTime)}`;
}

function getStatusStyle(status: Booking["status"]) {
  switch (status) {
    case "CONFIRMED":
    case "COMPLETED":
    case "CHECKED_IN":
      return {
        badge: "border-emerald-500/25 bg-emerald-500/10 text-emerald-400 dark:text-emerald-300",
        dot: "bg-emerald-400",
      };
    case "CANCELLED":
    case "NO_SHOW":
      return {
        badge: "border-rose-500/25 bg-rose-500/10 text-rose-400 dark:text-rose-300",
        dot: "bg-rose-400",
      };
    case "EXPIRED":
      return {
        badge: "border-slate-500/25 bg-slate-500/10 text-slate-400 dark:text-slate-300",
        dot: "bg-slate-400",
      };
    case "PENDING":
      return {
        badge: "border-amber-500/25 bg-amber-500/10 text-amber-400 dark:text-amber-300",
        dot: "bg-amber-400",
      };
    case "REFUNDED":
      return {
        badge: "border-purple-500/25 bg-purple-500/10 text-purple-400 dark:text-purple-300",
        dot: "bg-purple-400",
      };
    default:
      return {
        badge: "border-slate-500/25 bg-slate-500/10 text-slate-400 dark:text-slate-300",
        dot: "bg-slate-400",
      };
  }
}

// ---------------- SVG Icons ----------------

function CalendarIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.75}>
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
      />
    </svg>
  );
}

function ClockIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <circle cx="12" cy="12" r="9" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 7v5l3 3" />
    </svg>
  );
}

function PinIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"
      />
      <circle cx="12" cy="11" r="2" />
    </svg>
  );
}

function SearchIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <circle cx="11" cy="11" r="8" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-4.35-4.35" />
    </svg>
  );
}

function TableIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M3 10h18M3 14h18M10 3v18M3 4a1 1 0 011-1h16a1 1 0 011 1v16a1 1 0 01-1 1H4a1 1 0 01-1-1V4z" />
    </svg>
  );
}

function CardsIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <rect x="3" y="3" width="7" height="7" rx="1.5" />
      <rect x="14" y="3" width="7" height="7" rx="1.5" />
      <rect x="3" y="14" width="7" height="7" rx="1.5" />
      <rect x="14" y="14" width="7" height="7" rx="1.5" />
    </svg>
  );
}

function ChevronDownIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
    </svg>
  );
}

function CheckIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
    </svg>
  );
}

function LeftArrowIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
    </svg>
  );
}

function RightArrowIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
    </svg>
  );
}

function DoubleLeftArrowIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
    </svg>
  );
}

function DoubleRightArrowIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M13 5l7 7-7 7M5 5l7 7-7 7" />
    </svg>
  );
}
