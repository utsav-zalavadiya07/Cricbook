"use client";

import Link from "next/link";
import { TileInfo } from "@/components/ui";
import { useI18n } from "@/modules/i18n/I18nProvider";
import ProfileCard from "@/modules/profile/components/ProfileCard";
import ProfileTabs from "@/modules/profile/components/ProfileTabs";
import useProfile from "@/modules/profile/hooks/useProfile";
import { useAuthStore } from "@/store/authStore";

export default function ProfilePage() {
  const { t } = useI18n();
  const role = useAuthStore((state) => state.role);
  const {
    user,
    bookings,
    reviews,
    adminAnalytics,
    ownerDashboard,
    ownerTurfs,
    profileRole,
    error,
    loading,
    refresh,
  } = useProfile();

  const currentRole = profileRole || role || "USER";
  const isOwner = currentRole === "OWNER";
  const isAdmin = currentRole === "ADMIN";
  const accent = isOwner || isAdmin ? "sky" : "emerald";

  const handleEditClick = () => {
    const el = document.getElementById("profile-workspace");
    if (el) {
      el.scrollIntoView({ behavior: "smooth" });
    }
  };

  if (loading) {
    return (
      <div className={`flex min-h-screen items-center justify-center ${isOwner ? "theme-page-owner" : "theme-page-player"}`}>
        <div className="flex flex-col items-center gap-3">
          <svg
            className={`h-9 w-9 animate-spin ${accent === "sky" ? "text-sky-400" : "text-emerald-400"}`}
            viewBox="0 0 24 24"
            fill="none"
          >
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
          </svg>
          <span className={`font-bold ${accent === "sky" ? "text-sky-400" : "text-emerald-400"}`}>
            {t("profile.loading")}
          </span>
        </div>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="theme-page-player flex min-h-screen items-center justify-center px-4">
        <div className="theme-panel rounded-3xl border border-[var(--theme-border)] p-8 text-center max-w-md shadow-2xl">
          <h2 className="theme-title mb-2 text-2xl font-black">{t("profile.notAuthenticated")}</h2>
          <p className="theme-muted mb-6 text-sm">{error || t("profile.loginPrompt")}</p>
          <Link
            href="/auth/login"
            className="inline-block rounded-xl bg-emerald-500 px-6 py-3 font-bold text-white shadow-lg transition hover:bg-emerald-400"
          >
            {t("profile.goLogin")}
          </Link>
        </div>
      </div>
    );
  }

  const pageTitle = isAdmin
    ? t("profile.adminTitle")
    : isOwner
      ? "Owner Profile"
      : t("profile.playerTitle");

  const pageSubtitle = isOwner
    ? "Manage your business identity, venues, activity, and customer history in one place."
    : isAdmin
      ? "Review your account identity and platform access from one workspace."
      : "Manage your Cricbook identity, bookings, reviews, and personal preferences.";

  const workspaceLabel = isOwner
    ? "Owner workspace"
    : isAdmin
      ? "Admin workspace"
      : "Player workspace";

  return (
    <div className={`min-h-screen ${isOwner || isAdmin ? "theme-page-owner" : "theme-page-player"}`}>
      <main className="mx-auto max-w-[1500px] space-y-6 px-4 py-6 sm:px-6 lg:px-8">
        {/* Top Visual Header Banner with Stadium Backdrop */}
        <section className="relative overflow-hidden rounded-3xl border border-white/10 shadow-2xl">
          {/* Stadium Background Image */}
          <div className="absolute inset-0">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src="/images/stadium-banner.jpg"
              alt="Cricket Stadium"
              className="h-full w-full object-cover object-center filter brightness-90 saturate-110"
            />
            {/* Dark gradient overlay layers for text readability */}
            <div className="absolute inset-0 bg-gradient-to-t from-slate-950/95 via-slate-950/70 to-slate-950/40 backdrop-blur-[1px]" />
            <div className="absolute inset-0 bg-gradient-to-r from-slate-950/80 via-transparent to-slate-950/60" />
          </div>

          {/* Banner Content */}
          <div className="relative z-10 flex flex-col justify-between gap-6 p-6 sm:p-8 md:p-10 min-h-[170px] sm:min-h-[190px]">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
              <div>
                {/* Breadcrumb */}
                <div className="flex items-center gap-2 text-xs font-semibold text-slate-300/80">
                  <Link href="/" className="hover:text-white transition">
                    Profile
                  </Link>
                  <span>/</span>
                  <span className={accent === "sky" ? "text-sky-400 font-bold" : "text-emerald-400 font-bold"}>
                    {pageTitle}
                  </span>
                </div>

                {/* Main Heading */}
                <h1 className="mt-2.5 flex items-center text-2xl sm:text-3xl lg:text-4xl font-black text-white tracking-tight drop-shadow-md">
                  {pageTitle}
                  <span className="mb-1 ml-3 inline-flex align-middle">
                    <TileInfo label={`${pageTitle} information`} focusable={false}>
                      {pageSubtitle}
                    </TileInfo>
                  </span>
                </h1>

                {/* Subtitle */}
                <p className="mt-1.5 max-w-2xl text-xs sm:text-sm text-slate-200/90 leading-relaxed drop-shadow">
                  {pageSubtitle}
                </p>
              </div>

              {/* Workspace Pill */}
              <div className="shrink-0">
                <span className="inline-flex items-center gap-1.5 rounded-full border border-white/20 bg-black/40 px-3.5 py-1.5 text-xs font-bold text-white shadow-lg backdrop-blur-md">
                  <span className={`h-2 w-2 rounded-full ${accent === "sky" ? "bg-sky-400" : "bg-emerald-400"}`} />
                  <span>{workspaceLabel}</span>
                  <svg className="h-3.5 w-3.5 text-slate-300 ml-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                  </svg>
                </span>
              </div>
            </div>
          </div>
        </section>

        {/* Profile Hero Card (Unified Avatar with Click-to-Upload + Identity Details + Metrics) */}
        <ProfileCard
          user={user}
          role={currentRole}
          stats={{
            bookings: bookings.length,
            reviews: reviews.length,
            adminAnalytics,
            ownerDashboard,
            ownerTurfs,
          }}
          refresh={refresh}
          onEditClick={handleEditClick}
        />

        {/* Profile Workspace (Sidebar Tabs + Content Panes) */}
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
      </main>
    </div>
  );
}
