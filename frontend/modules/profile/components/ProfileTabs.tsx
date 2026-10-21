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
import SecuritySettings from "./SecuritySettings";

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
  city?: string;
  bio?: string;
  designation?: string;
  alternatePhone?: string;
  businessName?: string;
  businessType?: string;
  gstin?: string;
  address?: string;
  supportEmail?: string;
  photoUrl?: string;
  profileCompletionPercentage?: number;
};

type ProfileTabsProps = {
  user: ProfileTabUser;
  role: string;
  bookings: Booking[];
  reviews: Review[];
  adminAnalytics?: { activeUsers?: number; totalOwners?: number; totalBookings?: number; flaggedRiskCount?: number };
  ownerDashboard?: OwnerDashboardStats;
  ownerTurfs?: Turf[];
  refresh: () => void | Promise<void>;
};

export default function ProfileTabs({
  user,
  role,
  bookings,
  reviews,
  adminAnalytics,
  ownerDashboard,
  ownerTurfs = [],
  refresh,
}: ProfileTabsProps) {
  const { t } = useI18n();
  const searchParams = useSearchParams();
  const isAdmin = role === "ADMIN";
  const isOwner = role === "OWNER";
  const accent: "sky" | "emerald" = isAdmin || isOwner ? "sky" : "emerald";

  const tabs = useMemo(
    () =>
      isAdmin
        ? [
            { key: "profile", label: "Personal Details", description: "Identity and contact details", icon: "user" },
            { key: "platform", label: t("profile.tab.platform"), description: "Platform-level account summary", icon: "platform" },
            { key: "moderation", label: t("profile.tab.moderation"), description: "Review and safety activity", count: reviews.length, icon: "moderation" },
            { key: "security", label: "Security", description: "Password and authentication settings", icon: "security" },
          ]
        : isOwner
          ? [
              { key: "profile", label: "Personal Details", description: "Identity and contact details", icon: "user" },
              { key: "operations", label: "Business Details", description: "Business performance overview", icon: "business" },
              { key: "turfs", label: t("profile.tab.myTurfs"), description: "Manage your venue portfolio", count: ownerTurfs.length, icon: "turfs" },
              { key: "bookings", label: t("profile.tab.bookings"), description: "Recent customer bookings", count: bookings.length, icon: "bookings" },
              { key: "reviews", label: t("profile.tab.reviews"), description: "Customer feedback history", count: reviews.length, icon: "reviews" },
              { key: "security", label: "Security", description: "Password and account protection", icon: "security" },
            ]
          : [
              { key: "profile", label: "Personal Details", description: "Identity and contact details", icon: "user" },
              { key: "bookings", label: t("profile.tab.bookings"), description: "Your booking activity", count: bookings.length, icon: "bookings" },
              { key: "reviews", label: t("profile.tab.reviews"), description: "Feedback you have shared", count: reviews.length, icon: "reviews" },
              { key: "saved", label: t("profile.tab.savedTurfs"), description: "Saved venues and tools", icon: "saved" },
              { key: "security", label: "Security", description: "Password and account protection", icon: "security" },
            ],
    [bookings.length, isAdmin, isOwner, ownerTurfs.length, reviews.length, t]
  );

  const [tab, setTab] = useState(() => {
    const requestedTab = searchParams.get("tab");
    return requestedTab && tabs.some((item) => item.key === requestedTab) ? requestedTab : "profile";
  });

  return (
    <section id="profile-workspace" className="grid scroll-mt-6 gap-6 lg:grid-cols-[260px_minmax(0,1fr)]">
      {/* Left Sidebar Navigation */}
      <aside className="theme-panel h-fit rounded-3xl p-3 sm:p-4 border border-[var(--theme-border)] shadow-xl lg:sticky lg:top-6">
        <div className="px-2 pb-3 pt-1">
          <p className="theme-soft text-[11px] font-black uppercase tracking-[0.2em]">Workspace</p>
          <p className="theme-muted mt-0.5 text-xs">Profile sections & tools</p>
        </div>

        <nav className="flex gap-1.5 overflow-x-auto pb-1 lg:flex-col lg:overflow-visible" aria-label="Profile sections">
          {tabs.map((item) => {
            const isSelected = tab === item.key;
            return (
              <button
                key={item.key}
                type="button"
                onClick={() => setTab(item.key)}
                className={`group flex min-w-max items-center gap-3 rounded-2xl px-3.5 py-3 text-left text-sm font-bold transition-all duration-150 lg:w-full ${
                  isSelected
                    ? accent === "sky"
                      ? "bg-sky-500 text-white shadow-lg shadow-sky-500/25"
                      : "bg-emerald-500 text-white shadow-lg shadow-emerald-500/25"
                    : "theme-muted hover:bg-white/5 hover:text-white"
                }`}
                aria-current={isSelected ? "page" : undefined}
              >
                <span
                  className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-xl transition ${
                    isSelected ? "bg-white/20 text-white" : "bg-white/5 text-slate-400 group-hover:text-white"
                  }`}
                >
                  <TabIcon name={item.icon} />
                </span>

                <span className="min-w-0 flex-1">
                  <span className="block truncate font-bold">{item.label}</span>
                </span>

                {item.count !== undefined && (
                  <span
                    className={`rounded-full px-2 py-0.5 text-[11px] font-black transition ${
                      isSelected ? "bg-white/20 text-white" : "bg-white/10 text-slate-300"
                    }`}
                  >
                    {item.count}
                  </span>
                )}
              </button>
            );
          })}
        </nav>
      </aside>

      {/* Right Content Tab Pane */}
      <div className="min-w-0">
        <div className="min-h-[400px]">
          {tab === "profile" && <EditProfileForm user={user} refresh={refresh} accent={accent} role={role} />}
          {isOwner && tab === "operations" && <OwnerOperations dashboard={ownerDashboard} turfs={ownerTurfs} />}
          {isOwner && tab === "turfs" && <OwnerTurfs turfs={ownerTurfs} />}
          {!isAdmin && tab === "bookings" && <BookingList bookings={bookings} accent={accent} role={role} />}
          {!isAdmin && tab === "reviews" && <ReviewList reviews={reviews} />}
          {!isAdmin && !isOwner && tab === "saved" && <PlayerLinks />}
          {isAdmin && tab === "platform" && <AdminPlatform analytics={adminAnalytics} />}
          {isAdmin && tab === "moderation" && <AdminModeration reviews={reviews} />}
          {tab === "security" && <SecuritySettings userEmail={user.email} accent={accent} />}
        </div>
      </div>
    </section>
  );
}

function OwnerOperations({ dashboard, turfs }: { dashboard?: OwnerDashboardStats; turfs: Turf[] }) {
  const { t } = useI18n();
  return (
    <div className="space-y-6">
      <div className="theme-panel rounded-3xl border border-[var(--theme-border)] p-6">
        <h3 className="theme-title text-lg font-black">Business Operations Overview</h3>
        <p className="theme-muted mt-1 text-xs sm:text-sm">Real-time metrics across your venues and customer bookings.</p>

        <div className="mt-5 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <Metric title={t("owner.metric.todayBookings")} value={dashboard?.todayBookings ?? 0} />
          <Metric title={t("owner.metric.monthlyRevenue")} value={`${t("common.currencyRs")} ${dashboard?.monthlyRevenue ?? 0}`} />
          <Metric title={t("owner.metric.occupancy")} value={`${dashboard?.occupancyPercent ?? 0}%`} />
          <Metric title={t("owner.metric.cancellation")} value={`${dashboard?.cancellationPercent ?? 0}%`} />
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <ActionCard title={t("profile.metric.ownerDashboard")} text={t("profile.ownerDashboardText")} href="/owner/dashboard" />
        <ActionCard title={t("profile.metric.slotManager")} text={t("profile.slotManagerText")} href="/owner/slots" />
        <ActionCard title={t("profile.metric.earnings")} text={t("profile.earningsText")} href="/owner/earnings" />
      </div>

      <div className="theme-panel rounded-3xl border border-[var(--theme-border)] p-6">
        <h3 className="theme-title font-bold">{t("profile.turfStatus")}</h3>
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
      <div className="theme-panel rounded-3xl border-dashed border-[var(--theme-border)] p-12 text-center">
        <p className="theme-title text-base font-bold">{t("profile.noTurfs")}</p>
        <Link
          href="/turf/my-turfs"
          className="mt-4 inline-block rounded-xl bg-sky-500 px-5 py-2.5 text-sm font-bold text-white transition hover:bg-sky-400 shadow-md shadow-sky-500/20"
        >
          {t("profile.addTurf")}
        </Link>
      </div>
    );
  }

  return (
    <div className="grid gap-4 md:grid-cols-2">
      {turfs.map((turf) => (
        <div key={turf.id} className="theme-panel rounded-3xl border border-[var(--theme-border)] p-5 transition hover:border-sky-500/30">
          <div className="flex items-start justify-between gap-3">
            <div>
              <h3 className="theme-title text-base font-black">{turf.name}</h3>
              <p className="theme-muted mt-1 text-xs">{turf.city} - {turf.location}</p>
              <p className="theme-soft mt-2 text-xs font-bold">{t("common.currencyRs")} {turf.pricePerHour ?? 0}/hour</p>
            </div>
            <span className="rounded-full bg-sky-500/15 border border-sky-500/25 px-2.5 py-1 text-xs font-bold text-sky-300">
              {translateText(turf.status || t("common.status.active"))}
            </span>
          </div>
          <div className="mt-4 flex flex-wrap gap-2">
            <Link href={`/turf/${turf.id}/edit`} className="theme-card theme-text rounded-xl px-3.5 py-2 text-xs font-bold transition hover:bg-white/10">
              {t("profile.edit")}
            </Link>
            <Link href={`/turf/${turf.id}/slots`} className="rounded-xl border border-sky-500/30 bg-sky-500/10 px-3.5 py-2 text-xs font-bold text-sky-300 transition hover:bg-sky-500/20">
              {t("profile.slots")}
            </Link>
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
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Metric title={t("profile.activeUsers")} value={analytics?.activeUsers ?? 0} />
        <Metric title={t("profile.owners")} value={analytics?.totalOwners ?? 0} />
        <Metric title={t("profile.metric.bookings")} value={analytics?.totalBookings ?? 0} />
        <Metric title={t("profile.riskSignals")} value={analytics?.flaggedRiskCount ?? 0} />
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        <ActionCard title={t("profile.superAdmin")} text={t("profile.superAdminText")} href="/admin" />
        <ActionCard title={t("profile.globalSettings")} text={t("profile.globalSettingsText")} href="/global" />
      </div>
    </div>
  );
}

function AdminModeration({ reviews }: { reviews: Review[] }) {
  const { t } = useI18n();
  return (
    <div className="space-y-4">
      <div className="theme-panel rounded-3xl border border-[var(--theme-border)] p-6">
        <h3 className="theme-title text-lg font-black">{t("profile.reviewQueue")}</h3>
        <p className="theme-muted mt-1 text-sm">{t("profile.reviewQueueText")}</p>
        <Link href="/admin" className="mt-4 inline-block rounded-xl bg-sky-500 px-5 py-2.5 text-sm font-bold text-white hover:bg-sky-400">
          {t("profile.openAdmin")}
        </Link>
      </div>
      <ReviewList reviews={reviews} />
    </div>
  );
}

function ActionCard({ title, text, href }: { title: string; text: string; href: string }) {
  return (
    <Link href={href} className="theme-panel rounded-3xl border border-[var(--theme-border)] p-5 transition hover:border-white/20 hover:bg-white/[0.04]">
      <h3 className="theme-title font-bold text-base">{title}</h3>
      <p className="theme-muted mt-1 text-xs leading-relaxed">{text}</p>
    </Link>
  );
}

function Metric({ title, value }: { title: string; value: string | number }) {
  return (
    <div className="theme-panel-soft rounded-2xl border border-[var(--theme-border)] p-4 sm:p-5">
      <p className="theme-soft break-words text-[10px] font-black uppercase tracking-wider">{title}</p>
      <p className="theme-title mt-2 text-2xl sm:text-3xl font-black">{value}</p>
    </div>
  );
}

function TabIcon({ name }: { name: string }) {
  const iconProps = { className: "h-4 w-4", fill: "none", viewBox: "0 0 24 24", stroke: "currentColor", strokeWidth: 2 };
  switch (name) {
    case "user":
      return (
        <svg {...iconProps}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
        </svg>
      );
    case "business":
      return (
        <svg {...iconProps}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
        </svg>
      );
    case "turfs":
      return (
        <svg {...iconProps}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      );
    case "bookings":
      return (
        <svg {...iconProps}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
      );
    case "reviews":
      return (
        <svg {...iconProps}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
        </svg>
      );
    case "security":
      return (
        <svg {...iconProps}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
        </svg>
      );
    case "saved":
      return (
        <svg {...iconProps}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
        </svg>
      );
    case "platform":
      return (
        <svg {...iconProps}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
        </svg>
      );
    case "moderation":
      return (
        <svg {...iconProps}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
      );
    default:
      return (
        <svg {...iconProps}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      );
  }
}
