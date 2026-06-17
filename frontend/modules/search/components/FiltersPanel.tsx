"use client";

import { useI18n } from "@/modules/i18n/I18nProvider";
import { SearchFilters } from "../types/search.types";

interface FiltersPanelProps {
  filters: SearchFilters;
  setFilters: (f: SearchFilters) => void;
}

const inputClass =
  "theme-input w-full rounded-xl px-4 py-2.5 text-sm transition placeholder:text-slate-500 focus:border-emerald-500 focus:outline-none";
const labelClass = "theme-muted mb-1.5 block text-xs font-medium";
const sectionClass = "space-y-1";

const BOX_TYPES = ["5v5", "6v6", "7v7", "11v11"];
const PITCH_TYPES = ["TURF", "CONCRETE", "MATTING"];
const AMENITIES = ["Indoor", "Outdoor", "Parking", "Washroom", "Floodlights"];

export default function FiltersPanel({ filters, setFilters }: FiltersPanelProps) {
  const { t } = useI18n();

  const set = (field: keyof SearchFilters) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setFilters({ ...filters, [field]: e.target.value || undefined });

  const clearAll = () => setFilters({});

  const toggleAmenity = (amenity: string) => {
    const current = filters.amenities ?? [];
    const next = current.includes(amenity) ? current.filter((item) => item !== amenity) : [...current, amenity];
    setFilters({ ...filters, amenities: next.length ? next : undefined });
  };

  return (
    <aside className="theme-panel sticky top-6 space-y-5 rounded-2xl p-5">
      <div className="flex items-center justify-between">
        <h3 className="theme-title text-sm font-semibold">{t("search.filters")}</h3>
        <button onClick={clearAll} className="theme-soft text-xs transition hover:text-emerald-500">
          {t("common.clearAll")}
        </button>
      </div>

      <div className={sectionClass}>
        <p className="theme-text mb-2 text-xs font-semibold uppercase tracking-wide">{t("search.pricePerHour")}</p>
        <div className="grid grid-cols-2 gap-2">
          <div>
            <label className={labelClass}>{t("search.min")}</label>
            <input type="number" placeholder="0" className={inputClass} value={filters.minPrice ?? ""} onChange={set("minPrice")} />
          </div>
          <div>
            <label className={labelClass}>{t("search.max")}</label>
            <input type="number" placeholder="5000" className={inputClass} value={filters.maxPrice ?? ""} onChange={set("maxPrice")} />
          </div>
        </div>
      </div>

      <div className={sectionClass}>
        <p className="theme-text mb-2 text-xs font-semibold uppercase tracking-wide">{t("search.field.boxType")}</p>
        <div className="grid grid-cols-2 gap-2">
          {BOX_TYPES.map((type) => (
            <button
              key={type}
              onClick={() => setFilters({ ...filters, boxType: filters.boxType === type ? undefined : type })}
              className={`rounded-lg border py-2 text-xs font-medium transition ${
                filters.boxType === type
                  ? "border-emerald-500/50 bg-emerald-500/20 text-emerald-400"
                  : "theme-card theme-muted hover:border-emerald-500/30 hover:text-current"
              }`}
            >
              {type}
            </button>
          ))}
        </div>
      </div>

      <div className={sectionClass}>
        <p className="theme-text mb-2 text-xs font-semibold uppercase tracking-wide">{t("search.field.pitchType")}</p>
        <div className="space-y-1.5">
          {PITCH_TYPES.map((type) => (
            <label key={type} className="group flex cursor-pointer items-center gap-2.5">
              <input type="radio" name="pitchType" value={type} className="accent-emerald-500" checked={filters.pitchType === type} onChange={set("pitchType")} />
              <span className="theme-muted text-sm transition group-hover:text-current">{type}</span>
            </label>
          ))}
        </div>
      </div>

      <div className={sectionClass}>
        <p className="theme-text mb-2 text-xs font-semibold uppercase tracking-wide">{t("search.field.rating")}</p>
        <select className={inputClass} value={filters.rating ?? ""} onChange={set("rating")}>
          <option value="">{t("common.anyRating")}</option>
          <option value="4">4+ stars</option>
          <option value="3">3+ stars</option>
          <option value="2">2+ stars</option>
        </select>
      </div>

      <div className={sectionClass}>
        <p className="theme-text mb-2 text-xs font-semibold uppercase tracking-wide">{t("search.sort")}</p>
        <select className={inputClass} value={filters.sortBy ?? ""} onChange={set("sortBy" as keyof SearchFilters)}>
          <option value="">{t("common.recommended")}</option>
          <option value="RATING">{t("common.popular")}</option>
          <option value="PRICE">{t("search.sortPriceLowToHigh")}</option>
          <option value="DISTANCE">{t("common.distance")}</option>
        </select>
      </div>

      <div className={sectionClass}>
        <p className="theme-text mb-2 text-xs font-semibold uppercase tracking-wide">{t("search.amenities")}</p>
        <div className="flex flex-wrap gap-2">
          {AMENITIES.map((amenity) => {
            const active = filters.amenities?.includes(amenity);
            return (
              <button
                key={amenity}
                onClick={() => toggleAmenity(amenity)}
                className={`rounded-lg border px-3 py-2 text-xs font-medium ${
                  active
                    ? "border-emerald-500/50 bg-emerald-500/20 text-emerald-300"
                    : "theme-card theme-muted hover:text-current"
                }`}
              >
                {t(`search.amenity.${amenity.toLowerCase()}`)}
              </button>
            );
          })}
        </div>
      </div>

      <div className={sectionClass}>
        <p className="theme-text mb-2 text-xs font-semibold uppercase tracking-wide">{t("search.quickFilters")}</p>
        <div className="theme-text space-y-2 text-sm">
          <label className="flex items-center gap-2">
            <input type="checkbox" className="accent-emerald-500" />
            {t("common.availableNow")}
          </label>
          <label className="flex items-center gap-2">
            <input type="checkbox" className="accent-emerald-500" />
            {t("common.popular")}
          </label>
        </div>
      </div>
    </aside>
  );
}
