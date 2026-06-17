"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import ThemeToggle from "@/modules/theme/ThemeToggle";
import { getAdminAccessContext } from "@/modules/admin/services/adminService";
import { useAuthStore } from "@/store/authStore";

const adminSections = [
  {
    title: "Command",
    items: [{ href: "/admin", label: "Dashboard", hint: "Executive overview", icon: "📊", permissionKey: "ADMIN_DASHBOARD", fallback: ["REPORT_VIEW", "ANALYTICS_VIEW"] }],
  },
  {
    title: "Operations",
    items: [
      { href: "/admin/users", label: "Users & Owners", hint: "Players, owners, verification", icon: "👥", permissionKey: "ADMIN_USERS", fallback: ["USER_VIEW", "OWNER_APPROVE"] },
      { href: "/admin/turfs", label: "Turfs", hint: "Approvals and status", icon: "🏟️", permissionKey: "ADMIN_TURFS", fallback: ["TURF_MODERATE"] },
      { href: "/admin/bookings", label: "Bookings", hint: "Disputes and interventions", icon: "🎟️", permissionKey: "ADMIN_BOOKINGS", fallback: ["BOOKING_VIEW"] },
    ],
  },
  {
    title: "Money",
    items: [{ href: "/admin/finance", label: "Finance", hint: "Payments, refunds, payouts", icon: "💳", permissionKey: "ADMIN_FINANCE", fallback: ["PAYMENT_VIEW"] }],
  },
  {
    title: "Growth & Safety",
    items: [
      { href: "/admin/safety", label: "Trust & Safety", hint: "Reports and moderation", icon: "🛡️", permissionKey: "ADMIN_SAFETY", fallback: ["REPORT_VIEW", "MEDIA_MODERATE"] },
      { href: "/admin/analytics", label: "Analytics", hint: "BI and forecasts", icon: "📈", permissionKey: "ADMIN_ANALYTICS", fallback: ["ANALYTICS_VIEW"] },
      { href: "/admin/ai", label: "AI Intelligence", hint: "Copilots and governance", icon: "AI", permissionKey: "ADMIN_AI", fallback: ["AI_INSIGHTS_VIEW", "FORECAST_VIEW"] },
      { href: "/admin/sports", label: "Sports OS", hint: "Rules and catalog", icon: "SP", permissionKey: "ADMIN_SPORTS", fallback: ["SYSTEM_CONFIG_EDIT", "ANALYTICS_VIEW"] },
      { href: "/admin/developers", label: "Developer Platform", hint: "API keys and webhooks", icon: "API", permissionKey: "ADMIN_DEVELOPERS", fallback: ["SYSTEM_CONFIG_EDIT", "ANALYTICS_VIEW"] },
      { href: "/admin/academy", label: "Academy & Talent", hint: "Training and discovery", icon: "AC", permissionKey: "ADMIN_ACADEMY", fallback: ["ANALYTICS_VIEW", "SYSTEM_CONFIG_EDIT"] },
      { href: "/admin/career", label: "Career & Scouting", hint: "Profiles, scouts, trials", icon: "CR", permissionKey: "ADMIN_CAREER", fallback: ["ADMIN_ACADEMY_READ", "ANALYTICS_VIEW", "SYSTEM_CONFIG_EDIT"] },
      { href: "/admin/governance", label: "Federation Governance", hint: "Leagues, drafts, auctions, integrity", icon: "GV", permissionKey: "ADMIN_GOVERNANCE", fallback: ["ADMIN_CAREER_READ", "ADMIN_CORPORATE", "ANALYTICS_VIEW", "SYSTEM_CONFIG_EDIT"] },
      { href: "/admin/corporate", label: "Corporate Sports", hint: "B2B wellness and leagues", icon: "CO", permissionKey: "ADMIN_CORPORATE", fallback: ["ANALYTICS_VIEW", "SYSTEM_CONFIG_EDIT"] },
      { href: "/admin/marketing", label: "Marketing", hint: "CMS, coupons, campaigns", icon: "📣", permissionKey: "ADMIN_MARKETING", fallback: ["CMS_EDIT", "CAMPAIGN_CREATE"] },
      { href: "/admin/monetization", label: "Monetization", hint: "Sponsors, ads, leads", icon: "AD", permissionKey: "ADMIN_MONETIZATION", fallback: ["CAMPAIGN_CREATE", "REPORT_VIEW"] },
      { href: "/admin/support", label: "Support", hint: "Tickets and SLAs", icon: "🎧", permissionKey: "ADMIN_SUPPORT", fallback: ["TICKET_VIEW"] },
    ],
  },
  {
    title: "Enterprise",
    items: [
      { href: "/admin/expansion", label: "Enterprise", hint: "Centers, franchise, staff", icon: "EN", permissionKey: "ADMIN_ENTERPRISE", fallback: ["ANALYTICS_VIEW", "OWNER_APPROVE", "ROLE_MANAGE"] },
      { href: "/admin/workforce", label: "Workforce", hint: "Staff app and operations", icon: "WF", permissionKey: "ADMIN_WORKFORCE", fallback: ["ROLE_MANAGE", "OWNER_APPROVE", "BOOKING_VIEW"] },
    ],
  },
  {
    title: "Governance",
    items: [
      { href: "/admin/roles", label: "RBAC", hint: "Roles and permissions", icon: "🔐", permissionKey: "ADMIN_ROLES", fallback: ["ROLE_MANAGE", "PERMISSION_VIEW"] },
      { href: "/admin/system", label: "System", hint: "Flags and configuration", icon: "⚙️", permissionKey: "ADMIN_SYSTEM", fallback: ["SYSTEM_CONFIG_VIEW"] },
    ],
  },
];

const adminRoles = ["ADMIN", "SUPER_ADMIN", "PLATFORM_ADMIN", "FINANCE_ADMIN", "SUPPORT_ADMIN", "CONTENT_ADMIN", "OPERATIONS_ADMIN"];

export default function AdminShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { token, role, hydrated, logout } = useAuthStore();
  const [adminEmail, setAdminEmail] = useState("admin@cricbook.com");
  const [permissionCodes, setPermissionCodes] = useState<string[]>([]);
  const [accessLoaded, setAccessLoaded] = useState(false);
  const isLoginPage = pathname === "/admin/login";
  const isAuthorized = Boolean(token && role && adminRoles.includes(role));

  useEffect(() => {
    if (!hydrated || isLoginPage) {
      return;
    }

    if (!isAuthorized) {
      router.replace("/admin/login");
    }
  }, [hydrated, isAuthorized, isLoginPage, router]);

  useEffect(() => {
    if (!hydrated || !isAuthorized || isLoginPage) {
      return;
    }

    let active = true;
    getAdminAccessContext()
      .then((response) => {
        if (active) {
          setPermissionCodes(response.data.permissions);
        }
      })
      .catch(() => {
        if (active) {
          setPermissionCodes(role === "SUPER_ADMIN" ? ["SUPER_ADMIN"] : []);
        }
      })
      .finally(() => {
        if (active) {
          setAccessLoaded(true);
        }
      });

    return () => {
      active = false;
    };
  }, [hydrated, isAuthorized, isLoginPage, role]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setAdminEmail(window.localStorage.getItem("adminEmail") || "admin@cricbook.com");
    }, 0);
    return () => window.clearTimeout(timer);
  }, []);

  const visibleSections = useMemo(() => {
    if (role === "SUPER_ADMIN" || permissionCodes.includes("SUPER_ADMIN")) {
      return adminSections;
    }
    return adminSections
      .map((section) => ({
        ...section,
        items: section.items.filter((item) => hasPageAccess(permissionCodes, item)),
      }))
      .filter((section) => section.items.length > 0);
  }, [permissionCodes, role]);

  const activeItem = useMemo(() => {
    const items = adminSections.flatMap((section) => section.items);
    return items.find((item) => pathname === item.href || (item.href !== "/admin" && pathname.startsWith(`${item.href}/`)));
  }, [pathname]);

  const currentRoutePermitted = useMemo(() => {
    if (!activeItem || role === "SUPER_ADMIN" || permissionCodes.includes("SUPER_ADMIN") || !accessLoaded) {
      return true;
    }
    return hasPageAccess(permissionCodes, activeItem);
  }, [accessLoaded, activeItem, permissionCodes, role]);

  if (isLoginPage) {
    return <>{children}</>;
  }

  if (!hydrated || !isAuthorized) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-slate-950 text-slate-200">
        <div className="rounded-3xl border border-white/10 bg-slate-900/80 px-8 py-6 text-center shadow-2xl">
          <p className="text-xs font-black uppercase tracking-[0.28em] text-emerald-400">Admin Portal</p>
          <p className="mt-3 text-lg font-black text-white">Checking admin access...</p>
        </div>
      </main>
    );
  }

  const handleLogout = () => {
    window.localStorage.removeItem("adminEmail");
    void logout().finally(() => {
      router.push("/admin/login");
    });
  };

  return (
    <main className="min-h-screen bg-slate-950 text-slate-100" data-i18n-skip="true">
      <div className="flex min-h-screen">
        <aside className="hidden w-[304px] shrink-0 border-r border-white/10 bg-slate-900/95 xl:block">
          <div className="sticky top-0 flex h-screen flex-col p-5">
            <Link href="/admin" className="flex items-center gap-3 rounded-3xl border border-white/10 bg-slate-950/50 p-4">
              <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-500 text-lg font-black text-white">CB</span>
              <span>
                <span className="block text-xl font-black text-white">Cricbook</span>
                <span className="text-[10px] font-black uppercase tracking-[0.24em] text-emerald-300">Admin Portal</span>
              </span>
            </Link>
            <div className="mt-4 rounded-2xl border border-emerald-500/20 bg-emerald-500/10 px-4 py-3 text-xs font-black uppercase tracking-[0.18em] text-emerald-300">
              {role?.replaceAll("_", " ")}
            </div>

            <nav className="mt-6 flex-1 space-y-6 overflow-y-auto pr-1">
              {visibleSections.map((section) => (
                <div key={section.title}>
                  <p className="px-3 text-[10px] font-black uppercase tracking-[0.22em] text-slate-600">{section.title}</p>
                  <div className="mt-2 space-y-1">
                    {section.items.map((item) => (
                      <AdminNavLink key={item.href} item={item} active={activeItem?.href === item.href} />
                    ))}
                  </div>
                </div>
              ))}
            </nav>

            <div className="mt-5 rounded-3xl border border-white/10 bg-slate-950/55 p-4">
              <p className="text-xs font-black uppercase tracking-[0.2em] text-slate-500">Session</p>
              <p className="mt-2 truncate text-sm font-semibold text-slate-300">{adminEmail}</p>
              <button
                type="button"
                onClick={handleLogout}
                className="mt-4 w-full rounded-2xl border border-rose-500/25 bg-rose-500/10 px-4 py-3 text-sm font-black text-rose-200 transition hover:bg-rose-500/20"
              >
                Logout
              </button>
            </div>
          </div>
        </aside>

        <section className="min-w-0 flex-1">
          <header className="sticky top-0 z-40 border-b border-white/10 bg-slate-950/90 px-4 py-3 backdrop-blur-xl sm:px-6">
            <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
              <div className="flex min-w-0 items-center gap-3">
                <Link href="/admin" className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-emerald-500 text-sm font-black text-white xl:hidden">
                  CB
                </Link>
                <div className="min-w-0">
                  <p className="text-[10px] font-black uppercase tracking-[0.24em] text-emerald-400">Admin Portal</p>
                  <h1 className="truncate text-lg font-black text-white">{activeItem?.label || "Admin Workspace"}</h1>
                </div>
              </div>
              <div className="flex items-center gap-2 overflow-x-auto pb-1 xl:pb-0">
                <ThemeToggle compact />
                <Link href="/search" className="rounded-2xl border border-white/10 bg-slate-900 px-4 py-2 text-sm font-black text-slate-200 hover:bg-white/10">
                  Open Player App
                </Link>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="rounded-2xl border border-white/10 bg-slate-900 px-4 py-2 text-sm font-black text-slate-200 hover:bg-rose-500/10 hover:text-rose-200"
                >
                  Logout
                </button>
              </div>
            </div>
            <div className="mt-3 flex gap-2 overflow-x-auto xl:hidden">
              {visibleSections.flatMap((section) => section.items).map((item) => (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`whitespace-nowrap rounded-2xl border px-3 py-2 text-xs font-black ${
                    activeItem?.href === item.href
                      ? "border-emerald-500/30 bg-emerald-500/15 text-emerald-200"
                      : "border-white/10 bg-slate-900 text-slate-300"
                  }`}
                >
                  {item.icon} {item.label}
                </Link>
              ))}
            </div>
          </header>
          <div className="px-4 py-5 sm:px-6 xl:px-8">
            <div className="mx-0 w-full max-w-none">
              {currentRoutePermitted ? children : (
                <div className="rounded-3xl border border-rose-500/20 bg-rose-500/10 p-6 text-rose-100">
                  <p className="text-xl font-black">Access denied</p>
                  <p className="mt-2 text-sm text-rose-200/80">This role does not have read, write, or full page access for {activeItem?.label || "this admin page"}.</p>
                </div>
              )}
            </div>
          </div>
        </section>
      </div>
    </main>
  );
}

type AdminNavItem = (typeof adminSections)[number]["items"][number];

function hasPageAccess(permissionCodes: string[], item: AdminNavItem) {
  const permissions = new Set(permissionCodes);
  return permissions.has(`${item.permissionKey}_READ`)
    || permissions.has(`${item.permissionKey}_WRITE`)
    || permissions.has(`${item.permissionKey}_FULL`)
    || item.fallback.some((permission) => permissions.has(permission));
}

function AdminNavLink({ item, active }: { item: AdminNavItem; active: boolean }) {
  return (
    <Link
      href={item.href}
      className={`group block rounded-2xl border px-3 py-3 transition ${
        active
          ? "border-emerald-500/30 bg-emerald-500/15 text-emerald-200"
          : "border-transparent text-slate-300 hover:border-white/10 hover:bg-white/[0.04] hover:text-white"
      }`}
    >
      <span className="flex items-center gap-3">
        <span className="text-base">{item.icon}</span>
        <span className="min-w-0">
          <span className="block truncate text-sm font-black">{item.label}</span>
          <span className="mt-0.5 block truncate text-xs font-semibold text-slate-500 group-hover:text-slate-400">{item.hint}</span>
        </span>
      </span>
    </Link>
  );
}
