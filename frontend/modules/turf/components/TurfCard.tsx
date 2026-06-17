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
  ACTIVE: "border-emerald-500/35 bg-emerald-500/15 text-emerald-300",
  MAINTENANCE: "border-amber-500/35 bg-amber-500/15 text-amber-300",
  INACTIVE: "border-slate-500/35 bg-slate-500/15 text-slate-300",
  PENDING: "border-sky-500/35 bg-sky-500/15 text-sky-300",
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

function Stat({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="theme-card rounded-xl p-2.5">
      <p className="theme-soft text-[10px] font-black uppercase tracking-[0.18em]">{label}</p>
      <p className="theme-title mt-0.5 text-sm font-black">{value}</p>
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
  intent?: "neutral" | "danger" | "success";
}) {
  const className =
    intent === "danger"
      ? "border-red-500/30 bg-red-500/10 text-red-300 hover:bg-red-500/15"
      : intent === "success"
        ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-300 hover:bg-emerald-500/15"
        : "theme-card theme-text hover:bg-[var(--theme-panel-soft)]";

  return (
    <button
      type="button"
      onClick={onClick}
      className={`inline-flex min-h-9 items-center justify-center rounded-xl border px-3 py-1.5 text-xs font-bold transition ${className}`}
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
  const price = Number(turf.pricePerHour || 0);

  return (
    <article className="theme-panel group flex h-full overflow-hidden rounded-[1.5rem] transition hover:-translate-y-0.5 hover:shadow-xl">
      <div className="flex min-h-full w-full flex-col">
        <div className="relative h-40 overflow-hidden sm:h-44">
          {image ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={image} alt={turf.name} className="h-full w-full object-cover transition duration-500 group-hover:scale-105" />
          ) : (
            <div className="theme-panel-soft flex h-full w-full items-center justify-center">
              <div className="text-center">
                <span className="theme-owner-badge mx-auto grid h-12 w-12 place-items-center rounded-2xl text-base font-black">CB</span>
                <p className="theme-muted mt-2 text-xs font-bold">No image uploaded</p>
              </div>
            </div>
          )}
          <span
            className={`absolute right-3 top-3 rounded-full border px-2.5 py-0.5 text-[10px] font-black uppercase tracking-[0.14em] ${
              statusStyles[status] || statusStyles.ACTIVE
            }`}
          >
            {statusLabels[status] || status}
          </span>
        </div>

        <div className="flex flex-1 flex-col p-4">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <h2 className="theme-title truncate text-lg font-black">{turf.name}</h2>
              <p className="theme-muted mt-0.5 line-clamp-1 text-xs">{cityLine || "Location not configured"}</p>
            </div>
            <div className="shrink-0 text-right">
              <p className="text-base font-black text-[var(--theme-secondary)]">Rs {priceFormatter.format(price)}</p>
              <p className="theme-soft text-[11px]">per hour</p>
            </div>
          </div>

          <div className="mt-3 grid grid-cols-3 gap-2">
            <Stat label="Capacity" value={turf.capacity || "-"} />
            <Stat label="Pitch" value={optionLabel(TURF_PITCH_TYPE_OPTIONS, turf.pitchType)} />
            <Stat label="Box" value={optionLabel(TURF_BOX_TYPE_OPTIONS, turf.boxType)} />
          </div>

          <div className="mt-3 flex flex-wrap gap-1.5">
            {sports.length ? (
              sports.map((sport) => (
                <span key={sport} className="theme-owner-badge rounded-full px-2.5 py-0.5 text-[11px] font-black">
                  {optionLabel(sportOptions, sport)}
                </span>
              ))
            ) : (
              <span className="theme-card theme-soft rounded-full px-2.5 py-0.5 text-[11px] font-black">Sport not configured</span>
            )}
          </div>

          {amenities.length ? (
            <div className="mt-2.5 flex flex-wrap gap-1.5">
              {amenities.map((amenity) => (
                <span key={amenity} className="theme-card theme-muted rounded-full px-2.5 py-0.5 text-[11px] font-bold leading-5">
                  {amenity}
                </span>
              ))}
            </div>
          ) : null}

          {turf.description ? <p className="theme-muted mt-3 line-clamp-1 text-xs">{turf.description}</p> : null}

          <div className="mt-auto pt-4">
            <div className="grid grid-cols-2 gap-2">
              {id ? (
                <button
                  type="button"
                  onClick={() => onEdit?.(turf)}
                  className="theme-card theme-text inline-flex min-h-9 items-center justify-center rounded-xl px-3 py-1.5 text-xs font-bold transition hover:bg-[var(--theme-panel-soft)]"
                >
                  Edit
                </button>
              ) : (
                <span className="theme-card theme-soft inline-flex min-h-9 items-center justify-center rounded-xl px-3 py-1.5 text-xs font-bold">Edit</span>
              )}
              {id && status === "INACTIVE" ? (
                <ActionButton onClick={() => onActivate(id)} intent="success">
                  Activate
                </ActionButton>
              ) : id ? (
                <ActionButton onClick={() => onDeactivate(id)} intent="danger">
                  Deactivate
                </ActionButton>
              ) : (
                <span className="theme-card theme-soft inline-flex min-h-9 items-center justify-center rounded-xl px-3 py-1.5 text-xs font-bold">Deactivate</span>
              )}
            </div>

            <div className="mt-2 grid grid-cols-1 gap-2 sm:grid-cols-2">
              {id ? (
                <ActionButton onClick={() => onMaintenance(id)}>Maintenance</ActionButton>
              ) : (
                <span className="theme-card theme-soft inline-flex min-h-9 items-center justify-center rounded-xl px-3 py-1.5 text-xs font-bold">Maintenance</span>
              )}
              {id ? (
                <Link
                  href={`/owner/slots?turfId=${id}`}
                  className="inline-flex min-h-9 items-center justify-center rounded-xl border border-sky-500/30 bg-sky-500/10 px-3 py-1.5 text-center text-xs font-bold text-sky-300 transition hover:bg-sky-500/15"
                >
                  Manage Slots
                </Link>
              ) : (
                <span className="theme-card theme-soft inline-flex min-h-9 items-center justify-center rounded-xl px-3 py-1.5 text-xs font-bold">Manage Slots</span>
              )}
            </div>
          </div>
        </div>
      </div>
    </article>
  );
}
