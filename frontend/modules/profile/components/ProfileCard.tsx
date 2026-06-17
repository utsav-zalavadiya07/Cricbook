"use client";

import { useI18n } from "@/modules/i18n/I18nProvider";
import { getImageUrl } from "@/utils/image";

type ProfileCardProps = {
  user: { name?: string; email?: string; phone?: string; status?: string; photoUrl?: string | null };
  role: string;
  stats?: {
    bookings?: number;
    reviews?: number;
    adminAnalytics?: { totalUsers?: number; totalTurfs?: number; totalRevenue?: number };
    ownerDashboard?: { monthlyRevenue?: number };
    ownerTurfs?: Array<unknown>;
  };
};

export default function ProfileCard({ user, role, stats }: ProfileCardProps) {
  const { t } = useI18n();
  const isAdmin = role === "ADMIN";
  const isOwner = role === "OWNER";
  const accent = isAdmin || isOwner ? "from-sky-400 to-blue-600" : "from-emerald-400 to-teal-600";
  const initials = (user.name || user.email || "CB").slice(0, 2).toUpperCase();
  const roleLabel = isAdmin ? "ADMIN" : isOwner ? "OWNER" : "PLAYER";
  const badgeClass = isAdmin || isOwner ? "bg-sky-500/15 text-sky-300" : "bg-emerald-500/15 text-emerald-300";
  const photoUrl = getImageUrl(user.photoUrl);

  return (
    <div className="theme-panel rounded-2xl p-6">
      <div className="flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
        <div className="flex items-center gap-5">
          <div className={`flex h-20 w-20 shrink-0 items-center justify-center overflow-hidden rounded-full bg-gradient-to-br ${accent} text-2xl font-black text-white ring-4 ring-white/10`}>
            {photoUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={photoUrl} alt={user.name || "Profile"} className="h-full w-full object-cover" />
            ) : (
              initials
            )}
          </div>
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="theme-title text-2xl font-bold">{user.name || t("profile.userFallback")}</h2>
              <span className={`rounded-full px-2.5 py-1 text-xs font-bold ${badgeClass}`}>{roleLabel}</span>
            </div>
            <div className="theme-muted mt-2 flex flex-col gap-1 text-sm">
              <p>{user.email || t("profile.noEmail")}</p>
              <p>{user.phone || t("profile.noPhone")}</p>
              <p>{t("profile.status", { status: user.status || t("common.status.active") })}</p>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
          <Metric label={isAdmin ? t("profile.metric.users") : isOwner ? t("profile.tab.myTurfs") : t("profile.metric.bookings")} value={isAdmin ? stats?.adminAnalytics?.totalUsers ?? 0 : isOwner ? stats?.ownerTurfs?.length ?? 0 : stats?.bookings ?? 0} />
          <Metric label={isAdmin ? t("profile.metric.turfs") : isOwner ? t("profile.metric.bookings") : t("profile.metric.reviews")} value={isAdmin ? stats?.adminAnalytics?.totalTurfs ?? 0 : isOwner ? stats?.bookings ?? 0 : stats?.reviews ?? 0} />
          <Metric label={isAdmin || isOwner ? t("profile.metric.revenue") : t("profile.metric.member")} value={isAdmin ? `${t("common.currencyRs")} ${stats?.adminAnalytics?.totalRevenue ?? 0}` : isOwner ? `${t("common.currencyRs")} ${stats?.ownerDashboard?.monthlyRevenue ?? 0}` : "Basic"} />
        </div>
      </div>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="theme-panel-soft min-w-24 rounded-xl p-3">
      <p className="theme-soft text-[11px] font-semibold uppercase tracking-wider">{label}</p>
      <p className="theme-title mt-1 text-lg font-bold">{value}</p>
    </div>
  );
}
