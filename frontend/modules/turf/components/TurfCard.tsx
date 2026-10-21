"use client";

import Link from "next/link";
import type { Turf } from "../types/turf.types";
import { optionLabel, TURF_BOX_TYPE_OPTIONS, TURF_PITCH_TYPE_OPTIONS, TURF_SPORT_OPTIONS } from "../constants/turfOptions";
import type { SelectOption } from "@/components/ui/Select";
import { getImageUrl } from "@/utils/image";

interface TurfCardProps {
  turf: Turf;
  onActivate: (id: number) => void;
  onDeactivate: (id: number) => void;
  onMaintenance: (id: number) => void;
  onEdit?: (turf: Turf) => void;
  sportOptions?: SelectOption[];
}

const statusStyles: Record<string, string> = {
  ACTIVE: "border-emerald-400/35 bg-emerald-500/90 text-white",
  MAINTENANCE: "border-amber-300/40 bg-amber-500/90 text-white",
  INACTIVE: "border-slate-300/35 bg-slate-700/90 text-white",
  PENDING: "border-sky-300/35 bg-sky-600/90 text-white",
};

const statusLabels: Record<string, string> = {
  ACTIVE: "Active",
  MAINTENANCE: "Maintenance",
  INACTIVE: "Inactive",
  PENDING: "Pending",
};

const priceFormatter = new Intl.NumberFormat("en-IN", {
  maximumFractionDigits: 0,
});

function normalizeStatus(status?: string) {
  return status || "ACTIVE";
}

function primaryImage(images?: string[]) {
  return images?.find(Boolean) || "";
}

type CardIconName = "location" | "capacity" | "pitch" | "box";

function CardIcon({ name }: { name: CardIconName }) {
  if (name === "location") {
    return <path d="M12 21s6-5.1 6-11a6 6 0 1 0-12 0c0 5.9 6 11 6 11Zm0-8.5a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5Z" />;
  }
  if (name === "capacity") {
    return <path d="M8.5 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6Zm7 0a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5ZM3 19v-1.5A4.5 4.5 0 0 1 7.5 13h2A4.5 4.5 0 0 1 14 17.5V19m1-5.7a4 4 0 0 1 6 3.45V19h-4" />;
  }
  if (name === "pitch") {
    return <path d="M4 5h16v14H4V5Zm5 0v14m6-14v14M4 9h5m6 0h5M4 15h5m6 0h5" />;
  }
  return <path d="M4 5h16v14H4V5Zm4 0v14m8-14v14M8 9h8m-8 6h8" />;
}

function Fact({ icon, label, value }: { icon: CardIconName; label: string; value: string | number }) {
  return (
    <div className="min-w-0 px-3 py-3.5 first:pl-0 last:pr-0">
      <div className="flex items-center gap-2 text-[var(--theme-secondary)]">
        <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" className="h-4 w-4 shrink-0">
          <CardIcon name={icon} />
        </svg>
        <p className="theme-soft truncate text-[9px] font-black uppercase tracking-[0.16em]">{label}</p>
      </div>
      <p className="theme-title mt-1 truncate text-sm font-black">{value}</p>
    </div>
  );
}

function ActionButton({
  children,
  onClick,
  intent = "neutral",
}: {
  children: string;
  onClick: () => void;
  intent?: "neutral" | "danger" | "success" | "warning";
}) {
  const className =
    intent === "danger"
      ? "border-red-500/30 bg-red-500/10 text-red-300 hover:bg-red-500/15"
      : intent === "success"
        ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-300 hover:bg-emerald-500/15"
        : intent === "warning"
          ? "border-amber-500/30 bg-amber-500/10 text-amber-700 hover:bg-amber-500/15 dark:text-amber-200"
        : "theme-card theme-text hover:bg-[var(--theme-panel-soft)]";

  return (
    <button
      type="button"
      onClick={onClick}
      className={`inline-flex min-h-9 items-center justify-center rounded-xl border px-3 py-1.5 text-xs font-bold transition focus:outline-none focus:ring-2 focus:ring-sky-500/30 ${className}`}
    >
      {children}
    </button>
  );
}

export default function TurfCard({ turf, onActivate, onDeactivate, onMaintenance, onEdit, sportOptions = TURF_SPORT_OPTIONS }: TurfCardProps) {
  const id = turf.id;
  const status = normalizeStatus(turf.status);
  const image = getImageUrl(primaryImage(turf.images));
  const cityLine = [turf.city, turf.location].filter(Boolean).join(" / ");
  const sports = (turf.sports?.length ? turf.sports : turf.sport ? [turf.sport] : []).map(String);
  const amenities = turf.amenities?.map((item) => (typeof item === "string" ? item : item.amenity)).filter(Boolean) || [];
  const visibleAmenities = amenities.slice(0, 3);
  const hiddenAmenityCount = Math.max(0, amenities.length - visibleAmenities.length);
  const price = Number(turf.pricePerHour || 0);
  const imageCount = turf.images?.filter(Boolean).length || 0;
  const rating = Number(turf.rating || 0);

  return (
    <article className="theme-panel-strong group flex h-full overflow-hidden rounded-[1.75rem] transition duration-300 hover:-translate-y-1 hover:border-sky-500/30 hover:shadow-2xl">
      <div className="flex min-h-full w-full flex-col">
        <div className="relative aspect-[16/9] min-h-48 overflow-hidden">
          {image ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={image} alt={turf.name} className="h-full w-full object-cover transition duration-700 ease-out group-hover:scale-[1.04]" />
          ) : (
            <div className="theme-panel-soft flex h-full w-full items-center justify-center">
              <div className="text-center">
                <span className="theme-owner-badge mx-auto grid h-12 w-12 place-items-center rounded-2xl text-base font-black">CB</span>
                <p className="theme-muted mt-2 text-xs font-bold">No image uploaded</p>
              </div>
            </div>
          )}
          <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-slate-950 via-slate-950/20 to-black/10" />
          <span
            className={`absolute left-4 top-4 inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[10px] font-black uppercase tracking-[0.14em] shadow-lg backdrop-blur-md ${
              statusStyles[status] || statusStyles.ACTIVE
            }`}
          >
            <span className="h-1.5 w-1.5 rounded-full bg-current" />
            {statusLabels[status] || status}
          </span>
          {imageCount > 1 && (
            <span className="absolute right-4 top-4 rounded-full border border-white/20 bg-black/45 px-2.5 py-1 text-[10px] font-bold text-white backdrop-blur-md">
              {imageCount} photos
            </span>
          )}
          <div className="absolute inset-x-0 bottom-0 p-4 sm:p-5">
            <h2 className="truncate text-xl font-black tracking-tight text-white drop-shadow-sm">{turf.name}</h2>
            <div className="mt-1 flex min-w-0 items-center gap-1.5 text-xs text-slate-200">
              <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" className="h-3.5 w-3.5 shrink-0">
                <CardIcon name="location" />
              </svg>
              <span className="truncate">{cityLine || "Location not configured"}</span>
            </div>
          </div>
        </div>

        <div className="flex flex-1 flex-col p-4 sm:p-5">
          <div className="flex items-center justify-between gap-4">
            <div>
              <p className="theme-soft text-[9px] font-black uppercase tracking-[0.18em]">Starting price</p>
              <p className="mt-1 text-xl font-black text-[var(--theme-secondary)]">
                Rs {priceFormatter.format(price)} <span className="theme-soft text-xs font-semibold">/ hour</span>
              </p>
            </div>
            {rating > 0 ? (
              <div className="theme-owner-badge inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-black" aria-label={`${rating.toFixed(1)} rating from ${turf.totalReviews || 0} reviews`}>
                <span aria-hidden="true">★</span>
                {rating.toFixed(1)}
                <span className="font-semibold opacity-70">({turf.totalReviews || 0})</span>
              </div>
            ) : (
              <span className="theme-card theme-soft rounded-full px-3 py-1.5 text-[11px] font-bold">No reviews yet</span>
            )}
          </div>

          <div className="mt-4 grid grid-cols-3 divide-x divide-[var(--theme-border)] border-y border-[var(--theme-border)]">
            <Fact icon="capacity" label="Capacity" value={turf.capacity || "-"} />
            <Fact icon="pitch" label="Pitch" value={optionLabel(TURF_PITCH_TYPE_OPTIONS, turf.pitchType)} />
            <Fact icon="box" label="Format" value={optionLabel(TURF_BOX_TYPE_OPTIONS, turf.boxType)} />
          </div>

          <div className="mt-4 flex flex-wrap gap-2">
            {sports.length ? (
              sports.map((sport) => (
                <span key={sport} className="theme-owner-badge rounded-full px-3 py-1 text-[11px] font-black">
                  {optionLabel(sportOptions, sport)}
                </span>
              ))
            ) : (
              <span className="theme-card theme-soft rounded-full px-2.5 py-0.5 text-[11px] font-black">Sport not configured</span>
            )}
          </div>

          {amenities.length ? (
            <div className="mt-3 flex flex-wrap gap-1.5">
              {visibleAmenities.map((amenity) => (
                <span key={amenity} className="theme-card theme-muted rounded-full px-2.5 py-1 text-[10px] font-bold leading-5">
                  {amenity}
                </span>
              ))}
              {hiddenAmenityCount > 0 && <span className="theme-card theme-muted rounded-full px-2.5 py-1 text-[10px] font-black leading-5">+{hiddenAmenityCount} more</span>}
            </div>
          ) : null}

          {turf.description ? <p className="theme-muted mt-3 line-clamp-2 text-xs leading-5">{turf.description}</p> : null}

          <div className="mt-auto pt-4">
            <div className="grid grid-cols-2 gap-2">
              {id ? (
                <Link
                  href={`/owner/slots?turfId=${id}`}
                  className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-[var(--theme-secondary)] px-4 py-2.5 text-center text-sm font-black text-white shadow-lg shadow-sky-500/15 transition hover:-translate-y-0.5 hover:opacity-90 focus:outline-none focus:ring-4 focus:ring-sky-500/20"
                >
                  Manage Slots <span aria-hidden="true">→</span>
                </Link>
              ) : (
                <span className="theme-card theme-soft inline-flex min-h-11 items-center justify-center rounded-xl px-4 py-2.5 text-sm font-black">Manage Slots</span>
              )}
              {id ? (
                <button
                  type="button"
                  onClick={() => onEdit?.(turf)}
                  className="theme-card theme-text inline-flex min-h-11 items-center justify-center rounded-xl px-4 py-2.5 text-sm font-black transition hover:bg-[var(--theme-panel-soft)] focus:outline-none focus:ring-4 focus:ring-sky-500/15"
                >
                  Edit Details
                </button>
              ) : (
                <span className="theme-card theme-soft inline-flex min-h-11 items-center justify-center rounded-xl px-4 py-2.5 text-sm font-black">Edit Details</span>
              )}
            </div>

            <div className="mt-3 flex flex-wrap items-center justify-between gap-2 border-t border-[var(--theme-border)] pt-3">
              <span className="theme-soft text-[10px] font-black uppercase tracking-[0.16em]">Availability</span>
              <div className="flex flex-wrap gap-2">
                {id && status !== "ACTIVE" && <ActionButton onClick={() => onActivate(id)} intent="success">Activate</ActionButton>}
                {id && status !== "MAINTENANCE" && <ActionButton onClick={() => onMaintenance(id)} intent="warning">Maintenance</ActionButton>}
                {id && status !== "INACTIVE" && <ActionButton onClick={() => onDeactivate(id)} intent="danger">Deactivate</ActionButton>}
              </div>
            </div>
          </div>
        </div>
      </div>
    </article>
  );
}
