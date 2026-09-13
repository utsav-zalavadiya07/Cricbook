"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useI18n } from "@/modules/i18n/I18nProvider";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import { PageMetricCard, PageMetricGrid, PageSection, PageSectionHeader } from "@/components/ui/PageSurface";
import { getOwnerDashboard } from "@/modules/owner/services/ownerService";
import { Booking } from "@/modules/booking/types/booking.types";
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
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <h2 className="theme-title break-words text-lg font-bold">{t("owner.section.revenueTrend")}</h2>
                  <span className="theme-soft text-xs">{t("owner.section.last14Days")}</span>
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
                      <p className="theme-muted mt-1 break-words text-sm">{booking.bookingDate} - {booking.startTime || "--"} - {booking.endTime || "--"}</p>
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

function OwnerRevenueChart({ points, currencyLabel }: { points: AnalyticsPoint[]; currencyLabel: string }) {
  const maxRevenue = Math.max(...points.map((item) => Number(item.revenue) || 0), 1);
  const hasRevenue = points.some((item) => Number(item.revenue) > 0);

  if (points.length === 0) {
    return <Empty text="No revenue trend data available yet." />;
  }

  return (
    <div className="theme-panel-soft mt-5 flex min-h-[18rem] flex-1 rounded-2xl p-4">
      <div className="relative flex min-h-[16rem] w-full flex-1 overflow-hidden">
        <div className="pointer-events-none absolute inset-x-0 bottom-8 top-0 flex flex-col justify-between">
          {Array.from({ length: 4 }).map((_, index) => (
            <span key={index} className="border-t border-slate-300/50 dark:border-white/10" />
          ))}
        </div>

        <div className="relative z-10 flex w-full items-stretch gap-2">
          {points.map((point) => {
            const revenue = Number(point.revenue) || 0;
            const height = revenue > 0 ? Math.max((revenue / maxRevenue) * 100, 10) : 2;

            return (
              <div key={point.date} className="flex min-w-0 flex-1 flex-col items-center">
                <div className="flex w-full flex-1 items-end">
                  <div
                    className="w-full rounded-t-lg bg-gradient-to-t from-emerald-500/80 to-sky-400/90 shadow-[0_0_22px_rgba(14,165,233,0.18)] transition-[height]"
                    style={{ height: `${height}%` }}
                    title={`${currencyLabel} ${revenue}`}
                  />
                </div>
                <span className="theme-soft mt-2 truncate text-[10px]">{point.date.slice(5)}</span>
              </div>
            );
          })}
        </div>

        {!hasRevenue && (
          <div className="pointer-events-none absolute inset-x-0 top-0 z-20 flex h-[calc(100%-2rem)] items-center justify-center">
            <div className="theme-card rounded-2xl px-5 py-3 text-center shadow-sm">
              <p className="theme-title text-sm font-semibold">No revenue recorded yet</p>
              <p className="theme-muted mt-1 text-xs">Revenue bars will expand here as bookings are paid.</p>
            </div>
          </div>
        )}
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
    <Link href={href} className="theme-card group rounded-2xl p-4 transition hover:-translate-y-0.5 hover:border-sky-400/40">
      <p className="theme-title font-bold group-hover:text-sky-600 dark:group-hover:text-sky-300">{title}</p>
      <p className="theme-muted mt-1 text-sm leading-5">{body}</p>
    </Link>
  );
}
