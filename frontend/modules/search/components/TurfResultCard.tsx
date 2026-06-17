"use client";

import Image from "next/image";
import { useRouter } from "next/navigation";
import { memo } from "react";
import { useI18n } from "@/modules/i18n/I18nProvider";
import { SearchTurf } from "../types/search.types";
import { getImageUrl } from "@/utils/image";

interface TurfResultCardProps {
  turf: SearchTurf;
}

function TurfResultCard({ turf }: TurfResultCardProps) {
  const router = useRouter();
  const { t } = useI18n();
  const rating = Number(turf.rating ?? 0);
  const reviews = Number(turf.totalReviews ?? 0);
  const price = turf.pricePerHour != null ? `Rs ${turf.pricePerHour}` : "Price unavailable";

  return (
    <div
      onClick={() => router.push(`/turf/${turf.id}`)}
      className="theme-card group cursor-pointer overflow-hidden rounded-2xl transition-all duration-200 hover:-translate-y-1 hover:border-emerald-500/40 hover:shadow-xl hover:shadow-emerald-500/10"
    >
      <div className="relative flex h-44 items-center justify-center overflow-hidden border-b border-white/10 bg-gradient-to-br from-emerald-100 to-sky-100 dark:from-emerald-900/30 dark:to-slate-800">
        {turf.images && turf.images.length > 0 ? (
          <Image src={getImageUrl(turf.images[0])} alt={turf.name} fill sizes="(min-width: 1280px) 25vw, (min-width: 768px) 50vw, 100vw" className="object-cover transition-transform duration-300 group-hover:scale-105" unoptimized />
        ) : (
          <span className="text-3xl font-black text-emerald-600/30 transition group-hover:text-emerald-600/50">CB</span>
        )}
        <span className="absolute left-3 top-3 rounded-full border border-white/10 bg-white/80 px-2.5 py-1 text-xs font-medium text-slate-700 backdrop-blur-sm dark:bg-black/50 dark:text-white">
          {turf.boxType || "Turf"}
        </span>
        <span className="absolute right-3 top-3 rounded-full border border-emerald-400/30 bg-emerald-500/90 px-2.5 py-1 text-xs font-bold text-white">
          {turf.status === "ACTIVE" ? "Open" : turf.status || "Status unknown"}
        </span>
      </div>

      <div className="space-y-3 p-4">
        <div>
          <h3 className="theme-title font-semibold leading-tight">{turf.name}</h3>
          <p className="theme-muted mt-0.5 text-xs">{turf.city}</p>
        </div>

        <div className="flex items-center justify-between">
          <p className="text-lg font-bold text-emerald-700 dark:text-emerald-300">
            {price}
            {turf.pricePerHour != null && <span className="theme-soft text-xs font-normal">{t("search.perHour")}</span>}
          </p>
          {rating > 0 && (
            <div className="theme-muted flex items-center gap-1 text-xs">
              <span className="theme-title font-medium">{rating.toFixed(1)}</span>
              <span className="theme-soft">({reviews})</span>
            </div>
          )}
        </div>

        {turf.distance != null && (
          <p className="theme-muted text-xs">{turf.distance.toFixed(1)} km away</p>
        )}

        <div className="flex items-center justify-between">
          <span className="theme-card rounded-lg px-2.5 py-1 text-xs theme-muted">{turf.pitchType || "Pitch"}</span>
          <button
            onClick={(event) => {
              event.stopPropagation();
              router.push(`/turf/${turf.id}`);
            }}
            className="rounded-lg bg-emerald-500 px-3 py-1.5 text-xs font-bold text-white transition hover:bg-emerald-400"
          >
            {t("search.viewSlots")}
          </button>
        </div>
      </div>
    </div>
  );
}

export default memo(TurfResultCard);
