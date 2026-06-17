"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { useI18n } from "@/modules/i18n/I18nProvider";
import type { Booking } from "@/modules/booking/types/booking.types";
import type { Review } from "@/modules/review/types/review.types";
import type { Turf } from "@/modules/turf/types/turf.types";
import BookingList from "./BookingList";
import EditProfileForm from "./EditProfileForm";
import ReviewList from "./ReviewList";

type OwnerDashboardStats = {
  todayBookings?: number;
  monthlyRevenue?: number;
  occupancyPercent?: number;
  cancellationPercent?: number;
  activeTurfs?: number;
  inactiveTurfs?: number;
  maintenanceTurfs?: number;
};

type ProfileTabUser = {
  name?: string;
  phone?: string;
  email?: string;
  photoUrl?: string;
};

type ProfileTabsProps = {
  user: ProfileTabUser;
  role: string;
  bookings: Booking[];
  reviews: Review[];
  adminAnalytics?: { activeUsers?: number; totalOwners?: number; totalBookings?: number; flaggedRiskCount?: number };
  ownerDashboard?: OwnerDashboardStats;
  ownerTurfs?: Turf[];
  refresh: () => void;
};

export default function ProfileTabs({ user, role, bookings, reviews, adminAnalytics, ownerDashboard, ownerTurfs = [], refresh }: ProfileTabsProps) {
  const { t } = useI18n();
  const searchParams = useSearchParams();
  const isAdmin = role === "ADMIN";
  const isOwner = role === "OWNER";
  const accent = isAdmin || isOwner ? "sky" : "emerald";
  const tabs = useMemo(
    () =>
      isAdmin
        ? [
            ["profile", t("profile.tab.personalInfo")],
            ["platform", t("profile.tab.platform")],
            ["moderation", t("profile.tab.moderation")],
          ]
        : isOwner
          ? [
              ["profile", t("profile.tab.businessInfo")],
              ["operations", t("profile.tab.operations")],
              ["turfs", t("profile.tab.myTurfs")],
              ["bookings", t("profile.tab.bookings")],
              ["reviews", t("profile.tab.reviews")],
            ]
          : [
              ["profile", t("profile.tab.personalInfo")],
              ["bookings", t("profile.tab.bookings")],
              ["reviews", t("profile.tab.reviews")],
              ["saved", t("profile.tab.savedTurfs")],
            ],
    [isAdmin, isOwner, t]
  );
  const [tab, setTab] = useState(() => {
    const requestedTab = searchParams.get("tab");
    return requestedTab && tabs.some(([key]) => key === requestedTab) ? requestedTab : "profile";
  });

  const btnCls = (value: string) =>
    `rounded-xl px-4 py-2.5 text-sm font-semibold transition ${
      tab === value
        ? accent === "sky"
          ? "bg-sky-500 text-white shadow-lg shadow-sky-500/20"
          : "bg-emerald-500 text-white shadow-lg shadow-emerald-500/20"
        : "theme-muted hover:bg-white/5 hover:text-[var(--theme-text)]"
    }`;

  return (
    <div className="mt-8">
      <div className="theme-panel-soft mb-6 flex w-fit flex-wrap gap-2 rounded-2xl p-1.5">
        {tabs.map(([key, label]) => (
          <button key={key} onClick={() => setTab(key)} className={btnCls(key)}>
            {label}
          </button>
        ))}
      </div>

      <div className="min-h-[300px]">
        {tab === "profile" && <EditProfileForm user={user} refresh={refresh} />}
        {isOwner && tab === "operations" && <OwnerOperations dashboard={ownerDashboard} turfs={ownerTurfs} />}
        {isOwner && tab === "turfs" && <OwnerTurfs turfs={ownerTurfs} />}
        {!isAdmin && tab === "bookings" && <BookingList bookings={bookings} />}
        {!isAdmin && tab === "reviews" && <ReviewList reviews={reviews} />}
        {!isAdmin && !isOwner && tab === "saved" && <PlayerLinks />}
        {isAdmin && tab === "platform" && <AdminPlatform analytics={adminAnalytics} />}
        {isAdmin && tab === "moderation" && <AdminModeration reviews={reviews} />}
      </div>
    </div>
  );
}

function OwnerOperations({ dashboard, turfs }: { dashboard?: OwnerDashboardStats; turfs: Turf[] }) {
  const { t } = useI18n();
  return (
    <div className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Metric title={t("owner.metric.todayBookings")} value={dashboard?.todayBookings ?? 0} />
        <Metric title={t("owner.metric.monthlyRevenue")} value={`${t("common.currencyRs")} ${dashboard?.monthlyRevenue ?? 0}`} />
        <Metric title={t("owner.metric.occupancy")} value={`${dashboard?.occupancyPercent ?? 0}%`} />
        <Metric title={t("owner.metric.cancellation")} value={`${dashboard?.cancellationPercent ?? 0}%`} />
      </div>
      <div className="grid gap-4 md:grid-cols-3">
        <ActionCard title={t("profile.metric.ownerDashboard")} text={t("profile.ownerDashboardText")} href="/owner/dashboard" />
        <ActionCard title={t("profile.metric.slotManager")} text={t("profile.slotManagerText")} href="/owner/slots" />
        <ActionCard title={t("profile.metric.earnings")} text={t("profile.earningsText")} href="/owner/earnings" />
      </div>
      <div className="theme-panel rounded-2xl p-5">
        <h3 className="theme-title font-semibold">{t("profile.turfStatus")}</h3>
        <p className="theme-muted mt-1 text-sm">
          {t("profile.turfStatusSummary", {
            active: dashboard?.activeTurfs ?? turfs.filter((turf) => turf.status === "ACTIVE").length,
            maintenance: dashboard?.maintenanceTurfs ?? turfs.filter((turf) => turf.status === "MAINTENANCE").length,
          })}
        </p>
      </div>
    </div>
  );
}

function OwnerTurfs({ turfs }: { turfs: Turf[] }) {
  const { t, translateText } = useI18n();
  if (!turfs?.length) {
    return (
      <div className="theme-card rounded-2xl border-dashed p-8 text-center">
        <p className="theme-title font-medium">{t("profile.noTurfs")}</p>
        <Link href="/turf/my-turfs" className="mt-4 inline-block rounded-xl bg-sky-500 px-5 py-2.5 text-sm font-semibold text-white hover:bg-sky-400">
          {t("profile.addTurf")}
        </Link>
      </div>
    );
  }

  return (
    <div className="grid gap-4 md:grid-cols-2">
      {turfs.map((turf) => (
        <div key={turf.id} className="theme-panel rounded-2xl p-5">
          <div className="flex items-start justify-between gap-3">
            <div>
              <h3 className="theme-title font-semibold">{turf.name}</h3>
              <p className="theme-muted mt-1 text-sm">{turf.city} - {turf.location}</p>
              <p className="theme-soft mt-2 text-sm">{t("common.currencyRs")} {turf.pricePerHour ?? 0}/hour</p>
            </div>
            <span className="rounded-full bg-sky-500/15 px-2.5 py-1 text-xs font-bold text-sky-300">{translateText(turf.status || t("common.status.active"))}</span>
          </div>
          <div className="mt-4 flex flex-wrap gap-2">
            <Link href={`/turf/${turf.id}/edit`} className="theme-card theme-text rounded-lg px-3 py-1.5 text-xs font-semibold hover:bg-white/5">{t("profile.edit")}</Link>
            <Link href={`/turf/${turf.id}/slots`} className="rounded-lg border border-sky-500/30 px-3 py-1.5 text-xs font-semibold text-sky-200 hover:bg-sky-500/10">{t("profile.slots")}</Link>
          </div>
        </div>
      ))}
    </div>
  );
}

function PlayerLinks() {
  const { t } = useI18n();
  return (
    <div className="grid gap-4 md:grid-cols-3">
      <ActionCard title={t("profile.savedTurfs")} text={t("profile.savedTurfsText")} href="/favorites" />
      <ActionCard title={t("profile.membership")} text={t("profile.membershipText")} href="/membership" />
      <ActionCard title={t("nav.wallet")} text={t("profile.walletText")} href="/wallet" />
    </div>
  );
}

function AdminPlatform({ analytics }: { analytics?: { activeUsers?: number; totalOwners?: number; totalBookings?: number; flaggedRiskCount?: number } }) {
  const { t } = useI18n();
  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <Metric title={t("profile.activeUsers")} value={analytics?.activeUsers ?? 0} />
      <Metric title={t("profile.owners")} value={analytics?.totalOwners ?? 0} />
      <Metric title={t("profile.metric.bookings")} value={analytics?.totalBookings ?? 0} />
      <Metric title={t("profile.riskSignals")} value={analytics?.flaggedRiskCount ?? 0} />
      <ActionCard title={t("profile.superAdmin")} text={t("profile.superAdminText")} href="/admin" />
      <ActionCard title={t("profile.globalSettings")} text={t("profile.globalSettingsText")} href="/global" />
    </div>
  );
}

function AdminModeration({ reviews }: { reviews: Review[] }) {
  const { t } = useI18n();
  return (
    <div className="space-y-4">
      <div className="theme-panel rounded-2xl p-6">
        <h3 className="theme-title text-lg font-semibold">{t("profile.reviewQueue")}</h3>
        <p className="theme-muted mt-1 text-sm">{t("profile.reviewQueueText")}</p>
        <Link href="/admin" className="mt-4 inline-block rounded-xl bg-sky-500 px-5 py-2.5 text-sm font-semibold text-white hover:bg-sky-400">
          {t("profile.openAdmin")}
        </Link>
      </div>
      <ReviewList reviews={reviews} />
    </div>
  );
}

function ActionCard({ title, text, href }: { title: string; text: string; href: string }) {
  return (
    <Link href={href} className="theme-panel rounded-2xl p-5 transition hover:bg-white/10">
      <h3 className="theme-title font-semibold">{title}</h3>
      <p className="theme-muted mt-1 text-sm">{text}</p>
    </Link>
  );
}

function Metric({ title, value }: { title: string; value: string | number }) {
  return (
    <div className="theme-panel rounded-2xl p-5">
      <p className="theme-soft break-words text-xs font-semibold uppercase leading-5 tracking-wider">{title}</p>
      <p className="theme-title mt-3 text-3xl font-bold">{value}</p>
    </div>
  );
}
