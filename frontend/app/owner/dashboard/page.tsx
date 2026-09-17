"use client";

import { useEffect, useState } from "react";
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

export default function OwnerDashboardPage() {
  const { t } = useI18n();
  const [dashboard, setDashboard] = useState<OwnerDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    getOwnerDashboard()
      .then((res) => setDashboard(res.data))
      .catch(() => setError("Unable to load owner dashboard right now. Please try again."))
      .finally(() => setLoading(false));
  }, []);

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

            <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
              <section className="theme-panel flex min-h-[24rem] flex-col rounded-2xl p-4 sm:p-5 xl:col-span-2">
                <div className="flex flex-wrap items-start justify-between gap-3 border-b border-white/5 pb-6">
                  <div>
                    <h2 className="theme-title text-2xl font-bold">
                      {t("owner.section.revenueTrend") || "Revenue Trend"}
                      <span className="inline-flex align-middle ml-3 mb-1">
                        <TileInfo label="Revenue Trend information" focusable={false}>Visual breakdown of revenue generated over the last 14 days.</TileInfo>
                      </span>
                    </h2>
                    <p className="theme-muted mt-1 text-sm">Daily revenue trend · {t("owner.section.last14Days") || "Last 14 days"}</p>
                  </div>
                  
                  <div className="flex items-center gap-8">
                    <div className="flex flex-col">
                      <span className="theme-soft text-xs font-medium">Total Revenue</span>
                      <span className="theme-title mt-1 text-xl font-bold">{t("common.currencyRs")}{(dashboard.last14DaysRevenue || 0).toLocaleString()}</span>
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
                      <span className="theme-title mt-1 text-xl font-bold">{t("common.currencyRs")}{Math.round((dashboard.last14DaysRevenue || 0) / 14).toLocaleString()}</span>
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
                <OwnerRevenueChart points={dashboard.revenueSeries} currencyLabel={t("common.currencyRs")} />
              </section>

              <section className="theme-panel rounded-2xl p-4 sm:p-5">
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
                <h2 className="theme-title break-words text-lg font-bold">{t("owner.section.recentReviews")}</h2>
                <div className="mt-4 space-y-3">
                  {dashboard.recentReviews.length === 0 && <Empty text={t("owner.empty.reviews")} />}
                  {dashboard.recentReviews.map((review) => (
                    <div key={review.id} className="theme-panel-soft rounded-2xl p-4">
                      <p className="break-words text-sm font-semibold text-amber-200">Rating {review.rating}/5</p>
                      <p className="theme-muted mt-2 break-words text-sm">{review.comment || t("owner.noComment")}</p>
                      <p className="theme-soft mt-2 break-words text-xs">{review.turfName || `Turf #${review.turfId}`}</p>
                    </div>
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

function OwnerRevenueChart({ points, currencyLabel }: { points: AnalyticsPoint[]; currencyLabel: string }) {
  const maxRevenue = Math.max(...points.map((item) => Number(item.revenue) || 0), 1000);
  const hasRevenue = points.some((item) => Number(item.revenue) > 0);

  // Y-axis ticks (e.g., 50K, 40K, 30K, 20K, 10K, 0)
  const yTicks = [1, 0.8, 0.6, 0.4, 0.2, 0];

  return (
    <div className="mt-6 flex flex-1 flex-col">
      {/* Chart Area */}
      <div className="relative flex min-h-[20rem] w-full flex-1">
        {/* Background Grid & Y-Axis */}
        <div className="absolute inset-x-0 bottom-6 top-6 flex flex-col justify-between">
          {yTicks.map((tick) => (
            <div key={tick} className="flex w-full items-center gap-4">
              <span className="theme-soft w-8 text-right text-xs font-medium">{tick === 0 ? "0" : formatCurrency(maxRevenue * tick, "").replace(currencyLabel, "")}</span>
              <div className="flex-1 border-t border-dashed border-white/5" />
            </div>
          ))}
        </div>

        {/* Bars Container */}
        <div className="relative z-10 ml-12 flex flex-1 items-stretch justify-around px-2 pb-10 pt-6">
          {points.map((point) => {
            const revenue = Number(point.revenue) || 0;
            const height = revenue > 0 ? Math.max((revenue / maxRevenue) * 100, 2) : 0;

            return (
              <div key={point.date} className="group relative flex h-full w-full max-w-[48px] flex-col items-center justify-end">
                {/* Value above bar */}
                <div className={`mb-1 whitespace-nowrap text-[11px] font-bold tracking-tight text-teal-400 opacity-0 transition-opacity group-hover:opacity-100 ${revenue > 0 ? "sm:opacity-100" : ""}`}>
                  {revenue > 0 ? formatCurrency(revenue, currencyLabel) : ""}
                </div>
                {/* The Bar */}
                <div
                  className="z-10 w-full rounded-t-[4px] bg-gradient-to-b from-[#0ea5e9] to-[#10b981] transition-all duration-500 ease-out"
                  style={{ height: `${height}%`, minHeight: revenue > 0 ? "4px" : "0px" }}
                  title={`${currencyLabel} ${revenue}`}
                />
                {/* X Axis Label */}
                <div className="theme-soft absolute -bottom-10 flex flex-col items-center whitespace-nowrap text-[10px] leading-tight">
                  <span className="font-semibold opacity-80">
                    {(() => {
                      const [y, m, d] = point.date.split("-").map(Number);
                      return new Date(y, m - 1, d).toLocaleDateString("en-US", { weekday: "short" });
                    })()}
                  </span>
                  <span>
                    {point.date.split("-")[2]}-{point.date.split("-")[1]}-{point.date.split("-")[0]}
                  </span>
                </div>
              </div>
            );
          })}
          
          {!hasRevenue && (
            <div className="pointer-events-none absolute inset-0 z-20 flex items-center justify-center">
              <div className="theme-panel-soft rounded-2xl px-6 py-4 text-center backdrop-blur-sm">
                <p className="theme-title text-sm font-semibold">No revenue recorded yet</p>
                <p className="theme-muted mt-1 text-xs">Bars will appear here as bookings are paid.</p>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Footer / Legend */}
      <div className="mt-8 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="h-3 w-3 rounded-full bg-gradient-to-b from-[#0ea5e9] to-[#10b981]" />
          <span className="theme-title text-sm font-medium">Revenue</span>
        </div>
        <button className="theme-panel flex items-center gap-2 rounded-lg border border-white/10 px-3 py-1.5 text-xs font-medium transition hover:border-white/20">
          <svg className="theme-soft h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
          Last 14 days
          <svg className="theme-soft ml-1 h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" /></svg>
        </button>
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
