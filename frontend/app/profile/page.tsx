"use client";

import Link from "next/link";
import { useI18n } from "@/modules/i18n/I18nProvider";
import ProfileCard from "@/modules/profile/components/ProfileCard";
import ProfileTabs from "@/modules/profile/components/ProfileTabs";
import useProfile from "@/modules/profile/hooks/useProfile";
import { useAuthStore } from "@/store/authStore";

export default function ProfilePage() {
  const { t } = useI18n();
  const role = useAuthStore((state) => state.role);
  const { user, bookings, reviews, adminAnalytics, ownerDashboard, ownerTurfs, profileRole, error, loading, refresh } = useProfile();
  const currentRole = profileRole || role || "USER";
  const isOwner = currentRole === "OWNER";
  const isAdmin = currentRole === "ADMIN";

  if (loading) {
    return (
      <div className={`flex min-h-screen items-center justify-center ${isOwner ? "theme-page-owner" : "theme-page-player"}`}>
        <div className="flex flex-col items-center gap-3">
          <svg className="h-8 w-8 animate-spin text-emerald-400" viewBox="0 0 24 24" fill="none">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
          </svg>
          <span className="font-medium text-emerald-400">{t("profile.loading")}</span>
        </div>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="theme-page-player flex min-h-screen items-center justify-center">
        <div className="text-center">
          <h2 className="theme-title mb-2 text-2xl font-bold">{t("profile.notAuthenticated")}</h2>
          <p className="theme-muted mb-6">{error || t("profile.loginPrompt")}</p>
          <Link href="/auth/login" className="rounded-xl bg-emerald-500 px-6 py-3 font-medium text-white transition hover:bg-emerald-400">
            {t("profile.goLogin")}
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className={`min-h-screen ${isOwner || isAdmin ? "theme-page-owner" : "theme-page-player"}`}>
      <div className="mx-auto max-w-6xl space-y-6 px-4 py-8 sm:px-6">
        <div>
          <h1 className="theme-title text-3xl font-bold">
            {isAdmin ? t("profile.adminTitle") : isOwner ? t("profile.ownerTitle") : t("profile.playerTitle")}
          </h1>
          <p className="theme-muted mt-1 text-sm">
            {isAdmin ? t("profile.adminSubtitle") : isOwner ? t("profile.ownerSubtitle") : t("profile.playerSubtitle")}
          </p>
        </div>

        <ProfileCard user={user} role={currentRole} stats={{ bookings: bookings.length, reviews: reviews.length, adminAnalytics, ownerDashboard, ownerTurfs }} />

        <ProfileTabs
          user={user}
          role={currentRole}
          bookings={bookings}
          reviews={reviews}
          adminAnalytics={adminAnalytics}
          ownerDashboard={ownerDashboard}
          ownerTurfs={ownerTurfs}
          refresh={refresh}
        />
      </div>
    </div>
  );
}
