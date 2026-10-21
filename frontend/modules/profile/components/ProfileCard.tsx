"use client";

import { useRef, useState } from "react";
import Link from "next/link";
import { useI18n } from "@/modules/i18n/I18nProvider";
import { getImageUrl } from "@/utils/image";
import { uploadProfilePhoto, removeProfilePhoto } from "@/modules/media/services/mediaService";

type ProfileCardProps = {
  user: {
    name?: string;
    email?: string;
    phone?: string;
    city?: string;
    designation?: string;
    businessName?: string;
    status?: string;
    photoUrl?: string | null;
    profileCompletionPercentage?: number;
  };
  role: string;
  stats?: {
    bookings?: number;
    reviews?: number;
    adminAnalytics?: { totalUsers?: number; totalTurfs?: number; totalRevenue?: number };
    ownerDashboard?: { monthlyRevenue?: number };
    ownerTurfs?: Array<unknown>;
  };
  refresh?: () => void | Promise<void>;
  onEditClick?: () => void;
};

export default function ProfileCard({ user, role, stats, refresh, onEditClick }: ProfileCardProps) {
  const { t } = useI18n();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadFeedback, setUploadFeedback] = useState<{ type: "success" | "error"; text: string } | null>(null);

  const isAdmin = role === "ADMIN";
  const isOwner = role === "OWNER";
  const isBusinessRole = isAdmin || isOwner;
  const accent = isBusinessRole ? "sky" : "emerald";

  const initials = getInitials(user.name || user.email || "CB");
  const roleLabel = isAdmin ? "Administrator" : isOwner ? "Turf Owner" : "Player";
  const photoUrl = getImageUrl(user.photoUrl);

  const metrics = isAdmin
    ? [
        { label: t("profile.metric.users"), value: stats?.adminAnalytics?.totalUsers ?? 0, hint: "Platform users" },
        { label: t("profile.metric.turfs"), value: stats?.adminAnalytics?.totalTurfs ?? 0, hint: "Listed venues" },
        { label: t("profile.metric.revenue"), value: formatCurrency(stats?.adminAnalytics?.totalRevenue), hint: "Total revenue" },
      ]
    : isOwner
      ? [
          { label: t("profile.tab.myTurfs"), value: stats?.ownerTurfs?.length ?? 0, hint: "Managed venues" },
          { label: t("profile.metric.bookings"), value: stats?.bookings ?? 0, hint: "Total bookings" },
          { label: t("profile.metric.revenue"), value: formatCurrency(stats?.ownerDashboard?.monthlyRevenue), hint: "This month" },
        ]
      : [
          { label: t("profile.metric.bookings"), value: stats?.bookings ?? 0, hint: "Your activity" },
          { label: t("profile.metric.reviews"), value: stats?.reviews ?? 0, hint: "Shared feedback" },
          { label: t("profile.metric.member"), value: "Active", hint: "Member status" },
        ];

  const handleAvatarClick = () => {
    if (uploading) return;
    fileInputRef.current?.click();
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
      setUploadFeedback({ type: "error", text: "Please select a JPG, PNG, or WEBP image." });
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      setUploadFeedback({ type: "error", text: "Image size must be 5 MB or smaller." });
      return;
    }

    setUploading(true);
    setUploadFeedback(null);
    try {
      const response = await uploadProfilePhoto(file);
      window.dispatchEvent(new CustomEvent("cricbookMediaUpdated", { detail: response.data }));
      if (refresh) await refresh();
      setUploadFeedback({ type: "success", text: "Profile photo updated!" });
      setTimeout(() => setUploadFeedback(null), 3500);
    } catch {
      setUploadFeedback({ type: "error", text: "Failed to upload photo. Please try again." });
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  const handleRemovePhoto = async (e: React.MouseEvent) => {
    e.stopPropagation();
    if (uploading) return;
    setUploading(true);
    setUploadFeedback(null);
    try {
      await removeProfilePhoto();
      window.dispatchEvent(new CustomEvent("cricbookMediaUpdated", { detail: null }));
      if (refresh) await refresh();
      setUploadFeedback({ type: "success", text: "Profile photo removed." });
      setTimeout(() => setUploadFeedback(null), 3500);
    } catch {
      setUploadFeedback({ type: "error", text: "Failed to remove photo." });
    } finally {
      setUploading(false);
    }
  };

  return (
    <section className="theme-panel relative overflow-hidden rounded-3xl border border-[var(--theme-border)] shadow-xl backdrop-blur-xl">
      <input
        ref={fileInputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        className="hidden"
        onChange={handleFileChange}
      />

      {/* Top subtle decorative ambient glow line */}
      <div
        className={`h-1.5 w-full bg-gradient-to-r ${
          accent === "sky"
            ? "from-sky-500 via-blue-500 to-indigo-500"
            : "from-emerald-500 via-teal-500 to-green-500"
        }`}
      />

      <div className="p-5 sm:p-7">
        <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          {/* Left: Unified Avatar + User Identity Details */}
          <div className="flex min-w-0 flex-col gap-5 sm:flex-row sm:items-center">
            {/* Interactive Single Avatar */}
            <div className="relative group shrink-0">
              <button
                type="button"
                onClick={handleAvatarClick}
                disabled={uploading}
                title="Click to upload profile photo"
                aria-label="Upload profile photo"
                className={`relative flex h-24 w-24 sm:h-28 sm:w-28 shrink-0 items-center justify-center overflow-hidden rounded-3xl border-2 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 ${
                  accent === "sky"
                    ? "border-sky-500/40 hover:border-sky-400 focus:ring-sky-400 focus:ring-offset-slate-900 shadow-sky-500/10"
                    : "border-emerald-500/40 hover:border-emerald-400 focus:ring-emerald-400 focus:ring-offset-slate-900 shadow-emerald-500/10"
                } shadow-2xl group-hover:scale-[1.02] cursor-pointer`}
              >
                {photoUrl ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img
                    src={photoUrl}
                    alt={user.name || "Profile"}
                    className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                  />
                ) : (
                  <div
                    className={`flex h-full w-full items-center justify-center bg-gradient-to-br ${
                      accent === "sky" ? "from-sky-500 to-blue-700" : "from-emerald-500 to-teal-700"
                    } text-3xl sm:text-4xl font-black text-white`}
                  >
                    {initials}
                  </div>
                )}

                {/* Hover / Uploading Camera Overlay */}
                <div
                  className={`absolute inset-0 flex flex-col items-center justify-center bg-black/55 backdrop-blur-[2px] transition-opacity duration-200 ${
                    uploading ? "opacity-100" : "opacity-0 group-hover:opacity-100"
                  }`}
                >
                  {uploading ? (
                    <svg className="h-6 w-6 animate-spin text-white" viewBox="0 0 24 24" fill="none">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
                    </svg>
                  ) : (
                    <>
                      <svg className="h-6 w-6 text-white drop-shadow" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                        <path strokeLinecap="round" strokeLinejoin="round" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
                        <path strokeLinecap="round" strokeLinejoin="round" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
                      </svg>
                      <span className="mt-1 text-[10px] font-bold text-white drop-shadow">Change</span>
                    </>
                  )}
                </div>
              </button>

              {/* Quick Remove Button badge if user has a custom photo */}
              {photoUrl && !uploading && (
                <button
                  type="button"
                  onClick={handleRemovePhoto}
                  title="Remove profile photo"
                  aria-label="Remove profile photo"
                  className="absolute -bottom-1 -right-1 flex h-6 w-6 items-center justify-center rounded-full bg-slate-900 border border-red-500/40 text-red-400 shadow-md hover:bg-red-500 hover:text-white transition"
                >
                  <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              )}
            </div>

            {/* Name, Badges & Contact Information */}
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-2.5">
                <h2 className="theme-title truncate text-2xl sm:text-3xl font-black">
                  {user.name || t("profile.userFallback")}
                </h2>

                {onEditClick && (
                  <button
                    type="button"
                    onClick={onEditClick}
                    title="Edit personal details"
                    aria-label="Edit personal details"
                    className="flex h-7 w-7 items-center justify-center rounded-lg border border-[var(--theme-border)] text-slate-400 hover:text-sky-400 hover:border-sky-500/40 hover:bg-white/5 transition"
                  >
                    <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                    </svg>
                  </button>
                )}

                <span
                  className={`rounded-full px-3 py-1 text-xs font-black uppercase tracking-wider ${
                    accent === "sky"
                      ? "bg-sky-500/15 text-sky-300 border border-sky-500/25"
                      : "bg-emerald-500/15 text-emerald-300 border border-emerald-500/25"
                  }`}
                >
                  {roleLabel}
                </span>

                <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-500/25 bg-emerald-500/10 px-3 py-1 text-xs font-bold text-emerald-300">
                  <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
                  {user.status || t("common.status.active")}
                </span>
              </div>

              {/* Contact Icons Row */}
              <div className="theme-muted mt-3 flex flex-wrap items-center gap-x-5 gap-y-2 text-xs sm:text-sm font-medium">
                <span className="inline-flex items-center gap-2">
                  <svg className="h-4 w-4 shrink-0 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" d="M3 7.5 12 14l9-6.5M5 19h14a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2Z" />
                  </svg>
                  <span>{user.email || t("profile.noEmail")}</span>
                </span>

                <span className="inline-flex items-center gap-2">
                  <svg className="h-4 w-4 shrink-0 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" d="M8.5 3H6a2 2 0 0 0-2 2c0 8.284 6.716 15 15 15a2 2 0 0 0 2-2v-2.5l-4-1-1.2 2a12.1 12.1 0 0 1-8.3-8.3l2-1.2-1-4Z" />
                  </svg>
                  <span>{user.phone || t("profile.noPhone")}</span>
                </span>

                {user.city && (
                  <span className="inline-flex items-center gap-1.5">
                    <svg className="h-4 w-4 shrink-0 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                      <circle cx="12" cy="11" r="2" />
                    </svg>
                    <span>{user.city}</span>
                  </span>
                )}

                {user.designation && (
                  <span className="inline-flex items-center gap-1.5 text-sky-400 font-bold">
                    <span>•</span>
                    <span>{user.designation}</span>
                  </span>
                )}
              </div>

              {/* Upload Feedback Toast Alert */}
              {uploadFeedback && (
                <div
                  className={`mt-2.5 inline-flex items-center gap-1.5 rounded-lg px-2.5 py-1 text-xs font-semibold ${
                    uploadFeedback.type === "success"
                      ? "bg-emerald-500/15 text-emerald-300 border border-emerald-500/30"
                      : "bg-red-500/15 text-red-300 border border-red-500/30"
                  }`}
                >
                  <span>{uploadFeedback.type === "success" ? "✓" : "!"}</span>
                  <span>{uploadFeedback.text}</span>
                </div>
              )}
            </div>
          </div>

          {/* Right: Metric Cards Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-2.5 sm:gap-3 lg:shrink-0">
            {metrics.map((metric) => (
              <div
                key={metric.label}
                className="theme-panel-soft min-w-[100px] sm:min-w-[120px] rounded-2xl border border-[var(--theme-border)] p-3.5 sm:p-4 text-center transition hover:border-white/15"
              >
                <p className="theme-title text-xl sm:text-2xl font-black">{metric.value}</p>
                <p className="theme-muted mt-1 text-xs font-bold leading-tight">{metric.label}</p>
                <p className="theme-soft mt-0.5 text-[10px] hidden sm:block">{metric.hint}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}

function getInitials(value: string) {
  return (
    value
      .trim()
      .split(/\s+/)
      .map((part) => part[0])
      .slice(0, 2)
      .join("")
      .toUpperCase() || "CB"
  );
}

function formatCurrency(value?: number) {
  return `Rs ${new Intl.NumberFormat("en-IN", { maximumFractionDigits: 0 }).format(value ?? 0)}`;
}
