"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useI18n } from "@/modules/i18n/I18nProvider";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import { TileInfo } from "@/components/ui";
import { PageMetricCard, PageMetricGrid, PageSection, PageSectionHeader } from "@/components/ui/PageSurface";
import { getOwnerDashboard } from "@/modules/owner/services/ownerService";
import { Booking } from "@/modules/booking/types/booking.types";

import { formatDate } from "@/utils/dateFormatter";
import { Turf } from "@/modules/turf/types/turf.types";

type AnalyticsPoint = {
  date: string;
  bookings: number;
  revenue: number;
};

type OwnerDashboard = {
  todayBookings: number;
  totalBookings: number;
  todayRevenue: number;
  monthlyRevenue: number;
  totalRevenue: number;
  last14DaysRevenue: number;
  periodRevenue?: number;
  periodDays?: number;
  revenueGrowthPercent: number;
  occupancyPercent: number;
  cancellationPercent: number;
  activeTurfs: number;
  inactiveTurfs: number;
  maintenanceTurfs: number;
  popularTurf?: Turf | null;
  revenueSeries: AnalyticsPoint[];
  upcomingBookings: Booking[];
  recentReviews: Array<{ id: number; rating: number; comment?: string; turfName?: string; turfId: number }>;
};

const TIME_SPAN_OPTIONS = [
  { label: "Last 7 days", value: 7 },
  { label: "Last 14 days", value: 14 },
  { label: "Last 30 days", value: 30 },
  { label: "Last 60 days", value: 60 },
  { label: "Last 90 days", value: 90 },
] as const;

export default function OwnerDashboardPage() {
  const { t } = useI18n();
  const [dashboard, setDashboard] = useState<OwnerDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [chartLoading, setChartLoading] = useState(false);
  const [selectedDays, setSelectedDays] = useState<number>(14);
  const [error, setError] = useState("");

  const fetchDashboard = (days: number, initial = false) => {
    if (initial) {
      setLoading(true);
    } else {
      setChartLoading(true);
    }
    setError("");

    getOwnerDashboard(days)
      .then((res) => setDashboard(res.data))
      .catch(() => setError("Unable to load owner dashboard right now. Please try again."))
      .finally(() => {
        if (initial) {
          setLoading(false);
        } else {
          setChartLoading(false);
        }
      });
  };

  const handleTimespanChange = (days: number) => {
    if (days === selectedDays) return;
    setSelectedDays(days);
    fetchDashboard(days, false);
  };

  useEffect(() => {
    fetchDashboard(selectedDays, true);



  }, []);

  const periodRevenue = dashboard ? (dashboard.periodRevenue ?? dashboard.last14DaysRevenue ?? 0) : 0;
  const activeDays = dashboard?.periodDays ?? selectedDays;
  const avgDailyRevenue = activeDays > 0 ? Math.round(periodRevenue / activeDays) : 0;
  const activeOptionLabel = TIME_SPAN_OPTIONS.find((opt) => opt.value === selectedDays)?.label || `Last ${selectedDays} days`;

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <PageSection className="space-y-0">
          <PageSectionHeader title={t("owner.title")} description={t("owner.subtitle")} />
          <div className="mt-5 grid gap-3 md:grid-cols-3">
            <QuickAction href="/owner/slots" title="Manage Slots" body="Open calendar, block slots, and update pricing." />
            <QuickAction href="/turf/my-turfs" title="Review Turfs" body="Check active, inactive, and maintenance venues." />
            <QuickAction href="/owner/bookings" title="Booking Queue" body="Review upcoming bookings and cancellation requests." />
          </div>
        </PageSection>

        {loading && <p className="theme-muted py-20 text-center">{t("owner.loading")}</p>}

        {!loading && error && (
          <div className="theme-panel rounded-3xl p-8 text-center">
            <p className="theme-title text-lg font-bold">Dashboard unavailable</p>
            <p className="theme-muted mt-2 text-sm">{error}</p>
          </div>
        )}

        {!loading && dashboard && (
          <>
            <PageSection>
              <PageMetricGrid>
                <PageMetricCard label={t("owner.metric.todayBookings")} value={dashboard.todayBookings} helper="Today" />
                <PageMetricCard label={t("owner.metric.monthlyRevenue")} value={`${t("common.currencyRs")} ${dashboard.monthlyRevenue}`} helper="Current month" />
                <PageMetricCard label={t("owner.metric.occupancy")} value={`${dashboard.occupancyPercent}%`} helper="Booked capacity" />
                <PageMetricCard label={t("owner.metric.cancellation")} value={`${dashboard.cancellationPercent}%`} helper="Cancellation rate" />
                <PageMetricCard label={t("owner.metric.totalBookings")} value={dashboard.totalBookings} helper="All-time bookings" />
              </PageMetricGrid>
            </PageSection>

            <div className="grid grid-cols-1 gap-6 xl:grid-cols-12">
              <section className="theme-panel flex min-h-[24rem] flex-col rounded-2xl p-4 sm:p-5 xl:col-span-8 2xl:col-span-9">
                <div className="flex flex-wrap items-start justify-between gap-3 border-b border-white/5 pb-6">
                  <div>
                    <h2 className="theme-title text-2xl font-bold">
                      {t("owner.section.revenueTrend") || "Revenue Trend"}
                      <span className="inline-flex align-middle ml-3 mb-1">
                        <TileInfo label="Revenue Trend information" focusable={false}>Visual breakdown of revenue generated over the {activeOptionLabel.toLowerCase()}.</TileInfo>
                      </span>
                    </h2>
                    <p className="theme-muted mt-1 text-sm">Daily revenue trend · {activeOptionLabel}</p>
                  </div>
                  
                  <div className="flex items-center gap-8">
                    <div className="flex flex-col">
                      <span className="theme-soft text-xs font-medium">Total Revenue</span>
                      <span className="theme-title mt-1 text-xl font-bold">{t("common.currencyRs")}{periodRevenue.toLocaleString()}</span>
                      <span className={`mt-1 flex items-center gap-1 text-xs font-semibold ${(dashboard.revenueGrowthPercent || 0) >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
                        {(dashboard.revenueGrowthPercent || 0) !== 0 && (
                          <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d={(dashboard.revenueGrowthPercent || 0) > 0 ? "M5 10l7-7m0 0l7 7m-7-7v18" : "M19 14l-7 7m0 0l-7-7m7 7V3"} />
                          </svg>
                        )}
                        {(dashboard.revenueGrowthPercent || 0) > 0 ? '+' : ''}{(dashboard.revenueGrowthPercent || 0)}%
                      </span>
                    </div>
                    <div className="theme-panel-soft h-12 w-px" />
                    <div className="flex flex-col">
                      <span className="theme-soft text-xs font-medium">Avg. Daily Revenue</span>
                      <span className="theme-title mt-1 text-xl font-bold">{t("common.currencyRs")}{avgDailyRevenue.toLocaleString()}</span>
                      <span className={`mt-1 flex items-center gap-1 text-xs font-semibold ${(dashboard.revenueGrowthPercent || 0) >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
                        {(dashboard.revenueGrowthPercent || 0) !== 0 && (
                          <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d={(dashboard.revenueGrowthPercent || 0) > 0 ? "M5 10l7-7m0 0l7 7m-7-7v18" : "M19 14l-7 7m0 0l-7-7m7 7V3"} />
                          </svg>
                        )}
                        {(dashboard.revenueGrowthPercent || 0) > 0 ? '+' : ''}{(dashboard.revenueGrowthPercent || 0)}%
                      </span>
                    </div>
                  </div>
                </div>
                <OwnerRevenueChart
                  points={dashboard.revenueSeries}
                  currencyLabel={t("common.currencyRs")}
                  selectedDays={selectedDays}
                  onSelectDays={handleTimespanChange}
                  loading={chartLoading}
                />
              </section>

              <section className="theme-panel rounded-2xl p-4 sm:p-5 xl:col-span-4 2xl:col-span-3">
                <h2 className="theme-title break-words text-lg font-bold">{t("owner.section.turfOperations")}</h2>
                <div className="mt-4 grid gap-3">
                  <Metric title={t("owner.metric.activeTurfs")} value={dashboard.activeTurfs} compact />
                  <Metric title={t("owner.metric.maintenance")} value={dashboard.maintenanceTurfs} compact />
                  <Metric title={t("owner.metric.inactiveTurfs")} value={dashboard.inactiveTurfs ?? 0} compact />
                  <Metric title={t("owner.metric.totalRevenue")} value={`${t("common.currencyRs")} ${dashboard.totalRevenue}`} compact />
                </div>
                {dashboard.popularTurf && (
                  <div className="theme-panel-soft mt-4 rounded-2xl p-4">
                    <p className="theme-soft text-xs uppercase tracking-wider">{t("owner.section.popularTurf")}</p>
                    <p className="theme-title mt-1 break-words font-semibold">{dashboard.popularTurf.name}</p>
                    <p className="theme-muted break-words text-sm">{dashboard.popularTurf.city}</p>
                  </div>
                )}
              </section>
            </div>

            <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
              <section className="theme-panel rounded-2xl p-4 sm:p-5">
                <h2 className="theme-title break-words text-lg font-bold">{t("owner.section.upcomingBookings")}</h2>
                <div className="mt-4 space-y-3">
                  {dashboard.upcomingBookings.length === 0 && <Empty text={t("owner.empty.upcomingBookings")} />}
                  {dashboard.upcomingBookings.map((booking) => (
                    <div key={booking.id} className="theme-panel-soft rounded-2xl p-4">
                      <p className="theme-title break-words font-semibold">{booking.turfName || `Turf #${booking.turfId}`}</p>
                      <p className="theme-muted mt-1 break-words text-sm">{formatDate(booking.bookingDate)} - {booking.startTime || "--"} - {booking.endTime || "--"}</p>
                    </div>
                  ))}
                </div>
              </section>

              <section className="theme-panel rounded-2xl p-4 sm:p-5">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <h2 className="theme-title break-words text-lg font-bold">{t("owner.section.recentReviews")}</h2>
                    {dashboard.recentReviews.length > 0 && (
                      <span className="rounded-full bg-sky-500/15 px-2 py-0.5 text-xs font-bold text-sky-300">
                        {dashboard.recentReviews.length}
                      </span>
                    )}
                  </div>
                  <Link
                    href="/owner/reviews"
                    className="inline-flex items-center gap-1 text-xs font-bold text-sky-400 hover:text-sky-300 transition group"
                  >
                    <span>Manage reviews</span>
                    <span className="transition-transform group-hover:translate-x-0.5">→</span>
                  </Link>
                </div>
                <div className="mt-4 space-y-3">
                  {dashboard.recentReviews.length === 0 && <Empty text={t("owner.empty.reviews")} />}
                  {dashboard.recentReviews.map((review) => (
                    <Link
                      key={review.id}
                      href="/owner/reviews"
                      className="theme-panel-soft group block rounded-2xl p-4 transition-all duration-200 hover:border-sky-500/40 hover:bg-white/[0.04] hover:shadow-md cursor-pointer border border-transparent"
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-1 text-amber-400">
                          {[1, 2, 3, 4, 5].map((star) => (
                            <svg
                              key={star}
                              className={`h-3.5 w-3.5 ${
                                star <= Math.round(Number(review.rating) || 5)
                                  ? "fill-amber-400 text-amber-400"
                                  : "fill-slate-700 text-slate-700"
                              }`}
                              viewBox="0 0 20 20"
                            >
                              <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                            </svg>
                          ))}
                          <span className="ml-1 text-xs font-bold text-amber-300">
                            Rating {review.rating}/5
                          </span>
                        </div>
                      </div>
                      <p className="theme-muted mt-2 break-words text-sm group-hover:text-slate-100 transition-colors">
                        &ldquo;{review.comment || t("owner.noComment")}&rdquo;
                      </p>
                      <p className="theme-soft mt-2 break-words text-xs flex items-center justify-between">
                        <span>{review.turfName || `Turf #${review.turfId}`}</span>
                        <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-slate-500 transition-colors group-hover:text-sky-400">
                          <span>Reply & manage</span>
                          <span className="transition-transform group-hover:translate-x-0.5">→</span>
                        </span>
                      </p>
                    </Link>
                  ))}
                </div>
              </section>
            </div>
          </>

        )}
      </div>
    </DashboardLayout>
  );
}

function formatCurrency(val: number, label: string) {
  if (val >= 1000) return `${label}${(val / 1000).toFixed(1)}K`;
  return `${label}${val}`;
}

function OwnerRevenueChart({
  points,
  currencyLabel,
  selectedDays,
  onSelectDays,
  loading = false,
}: {
  points: AnalyticsPoint[];
  currencyLabel: string;
  selectedDays: number;
  onSelectDays: (days: number) => void;
  loading?: boolean;
}) {
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);
  const maxRevenue = Math.max(...points.map((item) => Number(item.revenue) || 0), 1000);
  const hasRevenue = points.some((item) => Number(item.revenue) > 0);

  // Y-axis ticks (e.g., 50K, 40K, 30K, 20K, 10K, 0)
  const yTicks = [1, 0.8, 0.6, 0.4, 0.2, 0];
  const activeOptionLabel = TIME_SPAN_OPTIONS.find((opt) => opt.value === selectedDays)?.label || `Last ${selectedDays} days`;

  // Auto-scroll to latest dates on right when extended timespan is loaded
  useEffect(() => {
    if (scrollRef.current && points.length > 14) {
      scrollRef.current.scrollLeft = scrollRef.current.scrollWidth;
    }
  }, [points, selectedDays]);

  return (
    <div className="mt-6 flex flex-1 flex-col">
      {/* Chart Area */}
      <div className="relative flex min-h-[22rem] w-full flex-1">
        {/* Loading overlay */}
        {loading && (
          <div className="absolute inset-0 z-30 flex items-center justify-center rounded-xl bg-slate-950/40 backdrop-blur-[2px]">
            <div className="flex items-center gap-2 rounded-xl border border-white/10 bg-slate-900/90 px-4 py-2 shadow-xl">
              <div className="h-4 w-4 animate-spin rounded-full border-2 border-sky-400 border-t-transparent" />
              <span className="text-xs font-semibold text-slate-200">Updating trend...</span>
            </div>
          </div>
        )}

        {/* Fixed Left Y-Axis */}
        <div className="flex w-10 shrink-0 flex-col justify-between pb-12 pt-6 text-right pr-2 select-none">
          {yTicks.map((tick) => (
            <span key={tick} className="theme-soft text-[11px] font-semibold leading-none">
              {tick === 0 ? "0" : formatCurrency(maxRevenue * tick, "").replace(currencyLabel, "")}
            </span>
          ))}
        </div>

        {/* Chart Viewport & Bars */}
        <div className="relative flex-1 min-w-0 overflow-hidden flex flex-col">
          {/* Background Grid Lines */}
          <div className="absolute inset-x-0 bottom-12 top-6 flex flex-col justify-between pointer-events-none">
            {yTicks.map((tick) => (
              <div key={tick} className="w-full border-t border-dashed border-white/5" />
            ))}
          </div>

          {/* Horizontally scrollable bars container with NO vertical scrollbar */}
          <div
            ref={scrollRef}
            className="relative z-10 flex flex-1 overflow-x-auto overflow-y-hidden pb-3 pt-6 [scrollbar-width:thin] [scrollbar-color:rgba(14,165,233,0.35)_rgba(255,255,255,0.03)] [&::-webkit-scrollbar]:h-2 [&::-webkit-scrollbar-track]:bg-white/[0.04] [&::-webkit-scrollbar-track]:rounded-full [&::-webkit-scrollbar-thumb]:bg-sky-500/35 hover:[&::-webkit-scrollbar-thumb]:bg-sky-500/60 [&::-webkit-scrollbar-thumb]:rounded-full"
          >
            <div
              className="flex flex-1 items-stretch justify-around px-2 min-w-full"
              style={{ minWidth: points.length > 14 ? `${points.length * 40}px` : "100%" }}
            >
              {points.map((point) => {
                const revenue = Number(point.revenue) || 0;
                const height = revenue > 0 ? Math.max((revenue / maxRevenue) * 100, 3) : 0;

                const [y, m, d] = point.date.split("-").map(Number);
                const dateObj = new Date(y, m - 1, d);
                const weekday = dateObj.toLocaleDateString("en-US", { weekday: "short" });
                const fullFormatted = `${d.toString().padStart(2, "0")}-${m.toString().padStart(2, "0")}-${y}`;
                const shortDate = `${d.toString().padStart(2, "0")}-${m.toString().padStart(2, "0")}`;

                return (
                  <div
                    key={point.date}
                    className="group flex h-full flex-col items-center justify-end px-1 select-none"
                    style={{ minWidth: points.length > 14 ? "36px" : "auto", maxWidth: "48px", flex: "1 1 0%" }}
                  >
                    {/* Bar Track with Value directly along with the bar */}
                    <div className="relative flex flex-1 w-full flex-col items-center justify-end">
                      {revenue > 0 && (
                        <div className="mb-1 whitespace-nowrap text-[10px] font-bold tracking-tight text-teal-400 select-none">
                          {formatCurrency(revenue, currencyLabel)}
                        </div>
                      )}

                      <div
                        className="w-full max-w-[28px] rounded-t-[4px] bg-gradient-to-b from-[#0ea5e9] to-[#10b981] transition-all duration-300 ease-out hover:brightness-125 hover:shadow-[0_0_12px_rgba(14,165,233,0.4)]"
                        style={{ height: `${height}%`, minHeight: revenue > 0 ? "4px" : "0px" }}
                        title={`${weekday}, ${fullFormatted}: ${currencyLabel} ${revenue.toLocaleString()} (${point.bookings || 0} bookings)`}
                      />
                    </div>

                    {/* Fixed Date & Day Labels - ALWAYS VISIBLE for EVERY bar */}
                    <div className="mt-2.5 flex flex-col items-center justify-center shrink-0 leading-tight">
                      <span className="theme-soft text-[10px] font-bold tracking-tight opacity-90 group-hover:text-sky-300">
                        {weekday}
                      </span>
                      <span className="theme-soft text-[9px] font-medium tracking-tighter opacity-70 group-hover:text-white">
                        {shortDate}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>

            {!hasRevenue && !loading && (
              <div className="pointer-events-none absolute inset-0 z-20 flex items-center justify-center">
                <div className="theme-panel-soft rounded-2xl px-6 py-4 text-center backdrop-blur-sm">
                  <p className="theme-title text-sm font-semibold">No revenue recorded yet</p>
                  <p className="theme-muted mt-1 text-xs">Bars will appear here as bookings are paid.</p>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Footer / Legend */}
      <div className="mt-6 flex items-center justify-between pt-2 border-t border-white/5">
        <div className="flex items-center gap-2">
          <span className="h-3 w-3 rounded-full bg-gradient-to-b from-[#0ea5e9] to-[#10b981]" />
          <span className="theme-title text-sm font-medium">Revenue</span>
        </div>

        {/* Dynamic Timespan Dropdown */}
        <div className="relative">
          <button
            type="button"
            onClick={() => setDropdownOpen((prev) => !prev)}
            className="theme-panel flex items-center gap-2 rounded-lg border border-white/10 px-3 py-1.5 text-xs font-medium transition hover:border-white/20 hover:bg-white/5 active:scale-95"
            aria-haspopup="listbox"
            aria-expanded={dropdownOpen}
          >
            <svg className="theme-soft h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            <span>{activeOptionLabel}</span>
            <svg className={`theme-soft ml-1 h-3 w-3 transition-transform duration-200 ${dropdownOpen ? "rotate-180" : ""}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
            </svg>
          </button>

          {dropdownOpen && (
            <>
              <div className="fixed inset-0 z-40" onClick={() => setDropdownOpen(false)} />
              <div className="absolute right-0 bottom-full mb-2 z-50 w-40 overflow-hidden rounded-xl border border-white/10 bg-slate-900/95 p-1 shadow-2xl backdrop-blur-xl animate-in fade-in zoom-in-95 duration-150">
                {TIME_SPAN_OPTIONS.map((opt) => {
                  const isSelected = opt.value === selectedDays;
                  return (
                    <button
                      key={opt.value}
                      type="button"
                      onClick={() => {
                        onSelectDays(opt.value);
                        setDropdownOpen(false);
                      }}
                      className={`flex w-full items-center justify-between rounded-lg px-3 py-2 text-xs font-medium transition ${
                        isSelected
                          ? "bg-sky-500/20 text-sky-400 font-bold"
                          : "text-slate-300 hover:bg-white/5 hover:text-white"
                      }`}
                    >
                      <span>{opt.label}</span>
                      {isSelected && (
                        <span className="h-1.5 w-1.5 rounded-full bg-sky-400" />
                      )}
                    </button>
                  );
                })}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function Metric({ title, value, compact = false }: { title: string; value: string | number; compact?: boolean }) {
  return (
    <div className={`theme-panel rounded-2xl ${compact ? "p-3.5" : "p-4"}`}>
      <p className="theme-soft break-words text-xs font-semibold uppercase leading-5 tracking-[0.22em]">{title}</p>
      <p className={`${compact ? "mt-2 text-lg" : "mt-2 text-2xl"} theme-title break-words font-bold leading-tight`}>{value}</p>
    </div>
  );
}

function Empty({ text }: { text: string }) {
  return <p className="theme-card theme-muted rounded-2xl border-dashed p-4 text-center text-sm">{text}</p>;
}

function QuickAction({ href, title, body }: { href: string; title: string; body: string }) {
  return (
    <Link 
      href={href} 
      className="theme-card group flex items-center justify-between rounded-2xl p-4 transition-all duration-300 hover:-translate-y-1 hover:border-sky-400/50 hover:shadow-md dark:hover:border-sky-500/50 dark:hover:shadow-[0_4px_20px_rgba(14,165,233,0.1)]"
    >
      <div className="flex min-w-0 items-center gap-2">
        <p className="theme-title min-w-0 break-words font-bold group-hover:text-sky-600 dark:group-hover:text-sky-300">{title}</p>
        <TileInfo variant="standard" label={`${title} information`} focusable={false}>{body}</TileInfo>
      </div>
      
      <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-sky-500 text-white shadow-sm transition-all duration-300 group-hover:bg-sky-600 dark:bg-sky-500/10 dark:text-sky-400 dark:group-hover:bg-sky-500/20 dark:group-hover:text-sky-300 dark:shadow-none">
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" className="transition-transform duration-300 group-hover:translate-x-0.5">
          <polyline points="9 18 15 12 9 6"></polyline>
        </svg>
      </div>
    </Link>
  );
}
