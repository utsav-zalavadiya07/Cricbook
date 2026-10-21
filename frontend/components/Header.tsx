"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useAuthStore } from "@/store/authStore";
import { useI18n } from "@/modules/i18n/I18nProvider";
import { languageOptions, LocaleCode } from "@/modules/i18n/messages";
import { getMyProfileMedia } from "@/modules/media/services/mediaService";
import NotificationBell from "@/modules/notification/components/NotificationBell";
import ThemeToggle from "@/modules/theme/ThemeToggle";
import { Select } from "@/components/ui";
import { getImageUrl } from "@/utils/image";

const playerNav = [
  { href: "/matches/discover", labelKey: "nav.discoverMatches", icon: "Discover" },
  { href: "/search", labelKey: "nav.search", icon: "Search" },
  { href: "/booking/history", labelKey: "nav.bookings", icon: "Cal" },
  { href: "/rebook", labelKey: "nav.rebook", icon: "Repeat" },
  { href: "/favorites", labelKey: "nav.favorites", icon: "Heart" },
  { href: "/teams", labelKey: "nav.teams", icon: "Team" },
  { href: "/social", labelKey: "nav.community", icon: "Community" },
  { href: "/feed", labelKey: "nav.feed", icon: "Feed" },
  { href: "/wallet", labelKey: "nav.wallet", icon: "Wallet" },
  { href: "/split-payments", labelKey: "nav.splitPayments", icon: "Split" },
  { href: "/weather", labelKey: "nav.weather", icon: "Weather" },
  { href: "/tournaments", labelKey: "nav.tournaments", icon: "Cup" },
  { href: "/live-scoring", labelKey: "nav.scoring", icon: "Score" },
  { href: "/progress", labelKey: "nav.progress", icon: "XP" },
  { href: "/career", labelKey: "nav.career", icon: "Career" },
  { href: "/governance", labelKey: "nav.governance", icon: "Gov" },
  { href: "/super-app", labelKey: "nav.superApp", icon: "ID" },
  { href: "/sports-os", labelKey: "nav.sportsOs", icon: "OS" },
  { href: "/fan-economy", labelKey: "nav.fanEconomy", icon: "Fan" },
  { href: "/saas", labelKey: "nav.saasPlatform", icon: "SaaS" },
  { href: "/personalized", labelKey: "nav.forYou", icon: "Spark" },
  { href: "/marketplace", labelKey: "nav.marketplace", icon: "Market" },
  { href: "/rewards", labelKey: "nav.rewards", icon: "Reward" },
  { href: "/referrals", labelKey: "nav.referrals", icon: "Invite" },
  { href: "/pricing", labelKey: "nav.pricing", icon: "Price" },
  { href: "/subscription", labelKey: "nav.subscription", icon: "Sub" },
  { href: "/trust", labelKey: "nav.trust", icon: "Trust" },
  { href: "/smart", labelKey: "nav.smart", icon: "AI" },
  { href: "/notifications", labelKey: "nav.notifications", icon: "Bell" },
];

const ownerNav = [
  { href: "/owner/dashboard", labelKey: "nav.dashboard", icon: "Dash" },
  { href: "/owner/slots", labelKey: "nav.slotManager", icon: "Slot" },
  { href: "/owner/bookings", labelKey: "nav.bookings", icon: "Book" },
  { href: "/turf/my-turfs", labelKey: "nav.myTurfs", icon: "Turf" },
  { href: "/owner/earnings", labelKey: "nav.earnings", icon: "Rs" },
  { href: "/owner/reviews", labelKey: "nav.reviews", icon: "Star" },
  { href: "/owner/pricing", labelKey: "nav.pricing", icon: "Price" },
  { href: "/owner/payment-rules", labelKey: "nav.paymentRules", icon: "Pay" },
  { href: "/owner/insights", labelKey: "nav.insights", icon: "Insights" },
  { href: "/owner/booking-intelligence", labelKey: "nav.recurringCustomers", icon: "Repeat" },
  { href: "/owner/community", labelKey: "nav.community", icon: "Community" },
  { href: "/owner/teams", labelKey: "nav.topTeams", icon: "Team" },
  { href: "/owner/weather", labelKey: "nav.weatherPolicy", icon: "Weather" },
  { href: "/owner/resale", labelKey: "nav.resaleRules", icon: "Market" },
  { href: "/owner/engagement", labelKey: "nav.engagementAnalytics", icon: "XP" },
  { href: "/owner/trust", labelKey: "nav.trustRules", icon: "Trust" },
  { href: "/owner/wallet", labelKey: "nav.wallet", icon: "Wallet" },
  { href: "/owner/subscription", labelKey: "nav.subscription", icon: "Sub" },
];

const adminNav = [
  { href: "/admin", labelKey: "nav.admin", icon: "Shield" },
  { href: "/admin/users", labelKey: "nav.userOwnerCenter", icon: "Users" },
  { href: "/admin/turfs", labelKey: "nav.turfModeration", icon: "Turf" },
  { href: "/admin/bookings", labelKey: "nav.bookingOps", icon: "Book" },
  { href: "/admin/finance", labelKey: "nav.financeOps", icon: "Finance" },
  { href: "/admin/safety", labelKey: "nav.trustSafety", icon: "Safety" },
  { href: "/admin/analytics", labelKey: "nav.analytics", icon: "Analytics" },
  { href: "/admin/marketing", labelKey: "nav.marketing", icon: "Marketing" },
  { href: "/admin/support", labelKey: "nav.support", icon: "Support" },
  { href: "/admin/roles", labelKey: "nav.rolesPermissions", icon: "Roles" },
  { href: "/admin/governance", labelKey: "nav.governance", icon: "Gov" },
  { href: "/admin/super-app", labelKey: "nav.superApp", icon: "ID" },
  { href: "/admin/sports-os", labelKey: "nav.sportsOs", icon: "OS" },
  { href: "/admin/fan-economy", labelKey: "nav.fanEconomy", icon: "Fan" },
  { href: "/admin/saas", labelKey: "nav.saasPlatform", icon: "SaaS" },
  { href: "/admin/system", labelKey: "nav.systemAdmin", icon: "System" },
];

const compactLanguageOptions = languageOptions.map((option) => ({
  label: option.value.toUpperCase(),
  value: option.value,
}));

export default function Header() {
  const pathname = usePathname();
  const router = useRouter();
  const { role, logout, token, hydrated } = useAuthStore();
  const { locale, setLocale, t } = useI18n();
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const [loginPortalRole, setLoginPortalRole] = useState<"USER" | "OWNER">(pathname === "/auth/register" ? "OWNER" : "USER");
  const [profileImageUrl, setProfileImageUrl] = useState("");
  const isAdminRoute = pathname.startsWith("/admin");
  const isAdminLoginPage = pathname === "/admin/login";
  const isAuthenticated = hydrated && !!token;
  const currentRole = role;
  const isAdminRole = ["ADMIN", "SUPER_ADMIN", "PLATFORM_ADMIN", "FINANCE_ADMIN", "SUPPORT_ADMIN", "CONTENT_ADMIN", "OPERATIONS_ADMIN"].includes(currentRole || "");
  const isAdmin = isAdminRoute || isAdminRole;
  const isOwner = currentRole === "OWNER" || isAdmin;
  const isLoginPage = pathname === "/auth/login" || isAdminLoginPage;
  const isAuthRolePreviewPage =
    isLoginPage || pathname === "/auth/register" || pathname === "/auth/forgot-password" || pathname === "/auth/reset-password";
  const authPreviewOwner = isAuthRolePreviewPage && !isAuthenticated && loginPortalRole === "OWNER";
  const visualIsOwner = !isAdmin && (isOwner || authPreviewOwner);
  const navItems = useMemo(() => (isAdmin ? adminNav : isOwner ? ownerNav : playerNav), [isAdmin, isOwner]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      const storedLoginRole = localStorage.getItem("loginPortalRole");
      if (storedLoginRole === "OWNER" || storedLoginRole === "USER") {
        setLoginPortalRole(storedLoginRole);
      }
    }, 0);

    return () => window.clearTimeout(timer);
  }, []);

  useEffect(() => {
    const handleLoginRoleChange = (event: Event) => {
      const role = (event as CustomEvent<"USER" | "OWNER">).detail || localStorage.getItem("loginPortalRole");
      if (role === "OWNER" || role === "USER") {
        setLoginPortalRole(role);
      }
    };

    window.addEventListener("loginPortalRoleChange", handleLoginRoleChange);
    window.addEventListener("storage", handleLoginRoleChange);
    return () => {
      window.removeEventListener("loginPortalRoleChange", handleLoginRoleChange);
      window.removeEventListener("storage", handleLoginRoleChange);
    };
  }, []);

  useEffect(() => {
    let active = true;
    if (!isAuthenticated || isAdminRoute) {
      return;
    }

    getMyProfileMedia()
      .then((response) => {
        if (active) {
          setProfileImageUrl(response.data.imageUrl || "");
        }
      })
      .catch(() => {
        if (active) {
          setProfileImageUrl("");
        }
      });

    return () => {
      active = false;
    };
  }, [isAuthenticated, isAdminRoute]);

  useEffect(() => {
    const handleMediaUpdate = (event: Event) => {
      const asset = (event as CustomEvent<{ ownerType?: string; mediaType?: string; imageUrl?: string }>).detail;
      if (asset?.ownerType === "USER" && asset?.mediaType === "PROFILE_PHOTO") {
        setProfileImageUrl(asset.imageUrl || "");
      }
    };

    window.addEventListener("cricbookMediaUpdated", handleMediaUpdate);
    return () => window.removeEventListener("cricbookMediaUpdated", handleMediaUpdate);
  }, []);

  const handleLogout = () => {
    setProfileImageUrl("");
    window.localStorage.removeItem("adminEmail");
    setOpen(false);
    void logout().finally(() => {
      router.push(isAdmin ? "/admin/login" : "/auth/login");
    });
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const value = query.trim();
    if (value) {
      router.push(`/search?city=${encodeURIComponent(value)}`);
      setQuery("");
      setOpen(false);
    }
  };

  const homeHref = isAuthenticated ? (isAdmin ? "/admin" : isOwner ? "/owner/dashboard" : "/search") : "/";
  const desktopNavItems = isAdmin ? adminNav : isOwner ? navItems : playerNav.filter((item) => item.href !== "/notifications");
  const mobileNavItems = navItems.filter((item) => item.href !== "/notifications");
  const activeNavClass = visualIsOwner ? "theme-nav-link-active-owner" : "theme-nav-link-active";
  const profileHref = "/profile";
  const settingsHref = visualIsOwner ? "/owner/settings" : "/settings";
  const languageSelectClass = "!h-10 !w-[76px] !min-w-[76px] px-2 py-2 text-xs";

  if (isAdminRoute) {
    return null;
  }

  return (
    <header className="theme-navbar sticky top-0 z-50 border-b backdrop-blur-xl" data-i18n-skip="true">
      <div className="w-full px-4 py-3 sm:px-6">
        <div className="flex items-center gap-3">
          <Link href={homeHref} className="flex shrink-0 items-center gap-3" data-i18n-skip="true">
            <span className={`flex h-10 w-10 items-center justify-center rounded-xl text-sm font-black text-white shadow-lg ${visualIsOwner ? "bg-sky-500" : "bg-emerald-500"}`}>
              CB
            </span>
            <span className="hidden sm:block">
              <span className="theme-title block text-lg font-bold leading-none">Cricbook</span>
              <span className="theme-soft text-[10px] font-semibold uppercase tracking-[0.24em]">
                {isAdmin ? t("portal.admin") : visualIsOwner ? t("portal.owner") : t("portal.player")}
              </span>
            </span>
          </Link>

          <div className="hidden shrink-0 items-center gap-2 lg:flex">
            <Select
              value={locale}
              onChange={(e) => setLocale(e.target.value as LocaleCode)}
              className={languageSelectClass}
              accent="neutral"
              options={compactLanguageOptions}
              data-i18n-skip="true"
              aria-label="Language"
            />
            <ThemeToggle compact className="hidden 2xl:flex" />
          </div>

          <div className="ml-auto flex shrink-0 items-center gap-2">
            {isAuthenticated ? (
              <>
                <NotificationBell active={pathname.startsWith("/notifications")} activeClass={activeNavClass} />
                <div className="hidden items-center gap-1 sm:flex">
                  <UtilityLink
                    href={profileHref}
                    label={t("nav.profile")}
                    icon="profile"
                    active={pathname === "/profile"}
                    activeClass={activeNavClass}
                    avatarUrl={profileImageUrl}
                    initials={(useAuthStore.getState().user?.name || "CB").slice(0, 2).toUpperCase()}
                  />
                  {!isAdmin && (
                    <UtilityLink
                      href={settingsHref}
                      label={t("nav.settings")}
                      icon="settings"
                      active={visualIsOwner ? pathname === "/owner/settings" : pathname === "/settings"}
                      activeClass={activeNavClass}
                    />
                  )}
                </div>
                <button
                  onClick={() => setOpen((value) => !value)}
                  className="theme-card theme-muted rounded-xl px-3 py-2 text-sm font-medium hover:bg-white/5 lg:hidden"
                  aria-label="Open navigation"
                >
                  {t("nav.menu")}
                </button>
                <button
                  onClick={handleLogout}
                  className="theme-card hidden rounded-xl px-3 py-2 text-sm font-medium text-slate-300 hover:border-red-500/30 hover:bg-red-500/10 hover:text-red-300 sm:block"
                >
                  {t("auth.logout")}
                </button>
              </>
            ) : isLoginPage ? null : (
              <>
                {pathname === "/auth/register" ? (
                  <span className="theme-muted hidden text-sm sm:inline">
                    {t("auth.alreadyAccount")}{" "}
                    <Link href="/auth/login" className={`${visualIsOwner ? "text-sky-400 hover:text-sky-300" : "text-emerald-400 hover:text-emerald-300"} font-semibold`}>
                      Sign in
                    </Link>
                  </span>
                ) : null}
                <Link href="/auth/login" className={`rounded-xl px-5 py-2.5 text-sm font-bold text-white ${visualIsOwner ? "bg-sky-500 hover:bg-sky-400" : "bg-emerald-500 hover:bg-emerald-400"}`}>
                  {t("auth.login")}
                </Link>
              </>
            )}
          </div>
        </div>

        {isAuthenticated && (
          <nav className="mt-3 hidden pb-1 lg:block">
            <div className="theme-card flex flex-wrap items-center gap-1 rounded-2xl p-1">
              {desktopNavItems.map((item) => {
                const active = pathname === item.href || (item.href !== "/search" && pathname.startsWith(item.href));
                return (
                  <Link
                    key={`${item.href}-${item.labelKey}`}
                    href={item.href}
                    className={`theme-nav-link flex-none whitespace-nowrap rounded-xl px-3 py-2 text-sm font-semibold transition ${active ? activeNavClass : ""}`}
                  >
                    {t(item.labelKey)}
                  </Link>
                );
              })}
            </div>
          </nav>
        )}
      </div>

      {isAuthenticated && open && (
        <div className="theme-navbar border-t px-4 py-4 lg:hidden">
          <form onSubmit={handleSearch} className="mb-3">
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={t("common.searchTurfs")}
              className="theme-input w-full rounded-xl px-4 py-2.5 text-sm outline-none focus:border-emerald-500"
            />
          </form>
          <div className="mb-3 flex items-center gap-2">
            <Select
              value={locale}
              onChange={(e) => setLocale(e.target.value as LocaleCode)}
              className="!h-10 !w-[88px] !min-w-[88px] px-2 py-2 text-sm"
              accent="neutral"
              options={compactLanguageOptions}
              data-i18n-skip="true"
              aria-label="Language"
            />
          </div>
          <ThemeToggle className="mb-3" />
          <div className="grid grid-cols-2 gap-2">
            <Link href={profileHref} onClick={() => setOpen(false)} className="theme-card theme-nav-link rounded-xl px-3 py-2 text-sm font-medium">
              {t("nav.profile")}
            </Link>
            {!isAdmin && (
              <Link
                href={settingsHref}
                onClick={() => setOpen(false)}
                className="theme-card theme-nav-link rounded-xl px-3 py-2 text-sm font-medium"
              >
                {t("nav.settings")}
              </Link>
            )}
            {mobileNavItems.map((item) => {
              const active = pathname === item.href || (item.href !== "/search" && pathname.startsWith(item.href));
              return (
                <Link
                  key={`${item.href}-${item.labelKey}-mobile`}
                  href={item.href}
                  onClick={() => setOpen(false)}
                  className={`theme-card theme-nav-link rounded-xl px-3 py-2 text-sm font-medium ${active ? activeNavClass : ""}`}
                >
                  {t(item.labelKey)}
                </Link>
              );
            })}
            <button onClick={handleLogout} className="rounded-xl border border-red-500/30 bg-red-500/10 px-3 py-2 text-left text-sm font-medium text-red-200">
              {t("auth.logout")}
            </button>
          </div>
        </div>
      )}
    </header>
  );
}

function UtilityLink({
  href,
  label,
  icon,
  active,
  activeClass,
  avatarUrl,
  initials = "CB",
}: {
  href: string;
  label: string;
  icon: "profile" | "settings";
  active?: boolean;
  activeClass: string;
  avatarUrl?: string;
  initials?: string;
}) {
  const resolvedAvatarUrl = getImageUrl(avatarUrl);
  return (
    <Link
      href={href}
      aria-label={label}
      title={label}
      className={`theme-card theme-header-utility inline-flex h-10 w-10 items-center justify-center rounded-full p-0 text-sm font-semibold transition hover:ring-4 hover:ring-emerald-400/15 ${
        active ? activeClass : "theme-muted hover:border-emerald-400/30 hover:text-[var(--theme-text)]"
      }`}
    >
      <span className={`inline-flex ${icon === "profile" ? "h-10 w-10 rounded-full" : "h-8 w-8 rounded-xl"} items-center justify-center overflow-hidden bg-white/5`}>
        {icon === "profile" ? (
          resolvedAvatarUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={resolvedAvatarUrl} alt={label} className="h-full w-full object-cover" />
          ) : (
            <span className="flex h-full w-full items-center justify-center bg-emerald-500 text-xs font-black text-white">{initials}</span>
          )
        ) : (
          <svg viewBox="0 0 24 24" className="h-4 w-4" fill="none" aria-hidden="true">
            <path d="M12 15.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7Z" stroke="currentColor" strokeWidth="1.8" />
            <path d="M19.4 13.5a7.9 7.9 0 0 0 .05-3l2-1.5-2-3.45-2.4 1a8.1 8.1 0 0 0-2.6-1.5L14.1 2.5h-4.2l-.35 2.55a8.1 8.1 0 0 0-2.6 1.5l-2.4-1-2 3.45 2 1.5a7.9 7.9 0 0 0 .05 3l-2.05 1.55 2 3.45 2.45-1a8 8 0 0 0 2.55 1.45l.35 2.55h4.2l.35-2.55A8 8 0 0 0 17 17.5l2.45 1 2-3.45-2.05-1.55Z" stroke="currentColor" strokeWidth="1.35" strokeLinejoin="round" />
          </svg>
        )}
      </span>
    </Link>
  );
}
