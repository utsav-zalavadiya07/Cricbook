"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { TileInfo } from "@/components/ui";
import { useI18n } from "@/modules/i18n/I18nProvider";
import { searchSuggestions } from "../services/searchService";
import { SearchFilters } from "../types/search.types";
import {
  TURF_BOX_TYPE_OPTIONS,
  TURF_PITCH_TYPE_OPTIONS,
} from "@/modules/turf/constants/turfOptions";
import { useTurfTaxonomy } from "@/modules/turf/hooks/useTurfTaxonomy";

interface SearchBarProps {
  filters: SearchFilters;
  setFilters: (f: SearchFilters) => void;
  onSearch: () => void;
  onApplyFilters: (filters: SearchFilters) => void;
  onClear: () => void;
  selectedSport: string | null;
  setSelectedSport: (sport: string | null) => void;
}

const RATING_OPTIONS = [
  { value: "4", label: "4 Stars" },
  { value: "3", label: "3 Stars" },
  { value: "2", label: "2 Stars" },
];

const SORT_OPTIONS = [
  { value: "RATING", label: "Highest rated" },
  { value: "PRICE_ASC", label: "Price low to high" },
  { value: "PRICE_DESC", label: "Price high to low" },
  { value: "DISTANCE", label: "Nearest" },
  { value: "MOST_BOOKED", label: "Most reviewed" },
];

function CustomSelect({
  label,
  value,
  options,
  onChange,
  placeholder,
}: {
  label: string;
  value: string;
  options: { value: string; label: string }[];
  onChange: (val: string) => void;
  placeholder: string;
}) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (ref.current && !ref.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const selectedLabel = options.find((option) => option.value === value)?.label || placeholder;

  return (
    <div className={`relative flex flex-col ${open ? "z-50" : "z-10"}`} ref={ref}>
      <label className="theme-text ml-1 mb-1 text-xs font-semibold">{label}</label>
      <div
        onClick={() => setOpen((current) => !current)}
        className={`theme-input flex w-full cursor-pointer items-center justify-between rounded-xl px-3 py-2 text-sm transition ${
          open ? "border-emerald-500 ring-1 ring-emerald-500" : "hover:bg-white/5"
        }`}
      >
        <span className={value ? "theme-text" : "theme-soft"}>{selectedLabel}</span>
        <svg className={`theme-muted h-4 w-4 transition-transform ${open ? "rotate-180" : ""}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
        </svg>
      </div>

      {open && (
        <div className="theme-panel-strong absolute left-0 right-0 top-[calc(100%+8px)] z-50 overflow-hidden rounded-xl">
          <div
            onClick={() => {
              onChange("");
              setOpen(false);
            }}
            className="theme-muted cursor-pointer px-3 py-2 text-sm transition hover:bg-emerald-500/10 hover:text-emerald-400"
          >
            {placeholder}
          </div>
          {options.map((option) => (
            <div
              key={option.value}
              onClick={() => {
                onChange(option.value);
                setOpen(false);
              }}
              className={`cursor-pointer px-3 py-2 text-sm transition ${
                value === option.value
                  ? "bg-emerald-500/20 font-medium text-emerald-400"
                  : "theme-text hover:bg-emerald-500/10 hover:text-emerald-400"
              }`}
            >
              {option.label}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default function SearchBar({ filters, setFilters, onSearch, onApplyFilters, onClear, selectedSport, setSelectedSport }: SearchBarProps) {
  const { t } = useI18n();
  const today = new Date().toISOString().split("T")[0];
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const selectedAmenityExtras = useMemo(() => filters.amenities ?? [], [filters.amenities]);
  const selectedSportExtras = useMemo(() => (selectedSport ? [selectedSport] : []), [selectedSport]);
  const { sportOptions, amenityOptions } = useTurfTaxonomy(
    selectedAmenityExtras,
    selectedSportExtras
  );
  const sports = useMemo(
    () =>
      sportOptions.map((sport) => ({
        id: String(sport.value),
        label: sport.label,
        labelKey: "labelKey" in sport ? (sport as { labelKey?: string }).labelKey : undefined,
      })),
    [sportOptions]
  );

  const getPriceErrors = (criteria: SearchFilters) => {
    const minPriceError = criteria.minPrice != null && criteria.minPrice < 0
      ? "Minimum price cannot be negative."
      : "";
    const maxPriceError = criteria.maxPrice != null && criteria.maxPrice < 0
      ? "Maximum price cannot be negative."
      : criteria.minPrice != null && criteria.maxPrice != null && criteria.minPrice > criteria.maxPrice
        ? "Minimum price cannot be greater than maximum price."
        : "";

    return { minPriceError, maxPriceError };
  };

  const getPriceError = (criteria: SearchFilters) => {
    const errors = getPriceErrors(criteria);
    return errors.minPriceError || errors.maxPriceError;
  };

  const updateFilters = (nextFilters: SearchFilters, applyImmediately = false) => {
    setFilters(nextFilters);
    if (applyImmediately && !getPriceError(nextFilters)) {
      onApplyFilters(nextFilters);
    }
  };

  const setText = (field: keyof SearchFilters) => (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value || undefined;
    if (field === "query" && (!value || value.trim().length < 2)) {
      setSuggestions([]);
    }
    updateFilters({ ...filters, [field]: value });
  };

  const setNumber = (field: "minPrice" | "maxPrice") => (e: React.ChangeEvent<HTMLInputElement>) =>
    updateFilters({ ...filters, [field]: e.target.value ? Number(e.target.value) : undefined });

  const setCustom = (field: keyof SearchFilters) => (val: string) =>
    updateFilters({ ...filters, [field]: val || undefined }, true);

  const toggleAmenity = (amenity: string) => {
    const current = filters.amenities ?? [];
    const next = current.includes(amenity) ? current.filter((item) => item !== amenity) : [...current, amenity];
    updateFilters({ ...filters, amenities: next.length ? next : undefined }, true);
  };

  const clearAll = () => {
    setSuggestions([]);
    onClear();
  };

  const { minPriceError, maxPriceError } = getPriceErrors(filters);
  const priceError = minPriceError || maxPriceError;

  const runSearch = () => {
    if (!priceError) {
      onSearch();
    }
  };

  useEffect(() => {
    const query = filters.query?.trim();
    if (!query || query.length < 2) {
      return;
    }

    let active = true;
    const timeout = window.setTimeout(() => {
      searchSuggestions(query)
        .then((res) => {
          if (active) setSuggestions(res.data);
        })
        .catch(() => {
          if (active) setSuggestions([]);
        });
    }, 250);

    return () => {
      active = false;
      window.clearTimeout(timeout);
    };
  }, [filters.query]);

  return (
    <div className="theme-panel relative z-40 w-full rounded-2xl p-4 shadow-2xl shadow-black/5">
      <div className="mb-4 flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
        <div>
          <h2 className="theme-title text-base font-black">
            Find and filter turfs
            <span className="inline-flex align-middle ml-2 mb-0.5">
              <TileInfo label="Search filters information" focusable={false}>Active turfs load first. Apply only the filters you need.</TileInfo>
            </span>
          </h2>
        </div>

        <div className="flex items-center gap-2">
          <button onClick={clearAll} className="theme-card rounded-xl px-4 py-2 text-sm font-medium theme-muted transition hover:bg-white/5 hover:text-current">
            {t("common.clear")}
          </button>
          <button
            onClick={runSearch}
            disabled={Boolean(priceError)}
            className="flex items-center gap-2 rounded-xl bg-emerald-500 px-5 py-2 text-sm font-semibold text-white shadow-lg shadow-emerald-500/20 transition-colors hover:bg-emerald-400 disabled:cursor-not-allowed disabled:bg-slate-500 disabled:shadow-none"
          >
            {t("common.searchTurfs")}
          </button>
        </div>
      </div>

      <div className="flex flex-col gap-3">
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-5">
          <div className="flex flex-col">
            <label className="theme-text ml-1 mb-1 text-xs font-semibold">Search turf</label>
            <input
              list="turf-search-suggestions"
              placeholder="Turf name, area, or city"
              value={filters.query ?? ""}
              onChange={setText("query")}
              onKeyDown={(event) => {
                if (event.key === "Enter") runSearch();
              }}
              className="theme-input w-full rounded-xl px-3 py-2 text-sm transition placeholder:text-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
            />
            <datalist id="turf-search-suggestions">
              {suggestions.map((suggestion) => (
                <option key={suggestion} value={suggestion} />
              ))}
            </datalist>
          </div>

          <div className="flex flex-col">
            <label className="theme-text ml-1 mb-1 text-xs font-semibold">{t("label.city")}</label>
            <input
              placeholder={t("search.placeholder.city")}
              value={filters.city ?? ""}
              onChange={setText("city")}
              className="theme-input w-full rounded-xl px-3 py-2 text-sm transition placeholder:text-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
            />
          </div>

          <div className="flex flex-col">
            <label className="theme-text ml-1 mb-1 text-xs font-semibold">{t("search.field.date")}</label>
            <input
              type="date"
              min={today}
              value={filters.date ?? ""}
              onChange={(event) => updateFilters({ ...filters, date: event.target.value || undefined }, true)}
              style={{ colorScheme: "light dark" }}
              className="theme-input w-full cursor-pointer rounded-xl px-3 py-2 text-sm transition focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500 [&::-webkit-calendar-picker-indicator]:opacity-70"
            />
          </div>

          <div className="flex flex-col">
            <label className="theme-text ml-1 mb-1 text-xs font-semibold">{t("search.field.minPrice")}</label>
            <input
              type="number"
              min={0}
              placeholder={t("search.placeholder.minPrice")}
              value={filters.minPrice ?? ""}
              onChange={setNumber("minPrice")}
              className="theme-input w-full rounded-xl px-3 py-2 text-sm transition placeholder:text-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
            />
            {minPriceError && <p className="ml-1 mt-2 text-xs font-semibold text-red-400">{minPriceError}</p>}
          </div>

          <div className="flex flex-col">
            <label className="theme-text ml-1 mb-1 text-xs font-semibold">{t("search.field.maxPrice")}</label>
            <input
              type="number"
              min={0}
              placeholder={t("search.placeholder.maxPrice")}
              value={filters.maxPrice ?? ""}
              onChange={setNumber("maxPrice")}
              className="theme-input w-full rounded-xl px-3 py-2 text-sm transition placeholder:text-slate-500 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
            />
            {maxPriceError && <p className="ml-1 mt-2 text-xs font-semibold text-red-400">{maxPriceError}</p>}
          </div>
        </div>

        <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4">
          <CustomSelect label={t("search.field.boxType")} value={filters.boxType ?? ""} options={TURF_BOX_TYPE_OPTIONS} onChange={setCustom("boxType")} placeholder={t("search.selectBoxType")} />
          <CustomSelect label={t("search.field.pitchType")} value={filters.pitchType ?? ""} options={TURF_PITCH_TYPE_OPTIONS} onChange={setCustom("pitchType")} placeholder={t("search.selectPitchType")} />
          <CustomSelect
            label={t("search.field.rating")}
            value={filters.rating ? String(filters.rating) : ""}
            options={RATING_OPTIONS}
            onChange={(val) => updateFilters({ ...filters, rating: val ? Number(val) : undefined }, true)}
            placeholder={t("search.selectRating")}
          />
          <CustomSelect
            label={t("search.sort")}
            value={filters.sortBy ?? ""}
            options={SORT_OPTIONS}
            onChange={setCustom("sortBy")}
            placeholder={t("common.recommended")}
          />
        </div>

        <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:gap-6">
          <div className="min-w-0 xl:shrink-0">
            <p className="theme-text ml-1 mb-1.5 text-xs font-semibold">Sports</p>
            <div className="flex flex-nowrap gap-2 overflow-x-auto pb-1 xl:overflow-visible xl:pb-0">
              {sports.map((sport) => (
                <button
                  key={sport.id}
                  onClick={() => {
                    const nextSport = selectedSport === sport.id ? null : sport.id;
                    setSelectedSport(nextSport);
                    updateFilters({ ...filters, sportCode: nextSport ?? undefined }, true);
                  }}
                  className={`inline-flex items-center rounded-xl border px-3 py-1.5 text-xs font-bold whitespace-nowrap transition duration-200 xl:px-2.5 ${
                    selectedSport === sport.id
                      ? "border-emerald-400 bg-emerald-500 text-white"
                      : "theme-card theme-muted hover:bg-white/10"
                  }`}
                >
                  {sport.labelKey?.startsWith("search.") ? t(sport.labelKey) : sport.label}
                </button>
              ))}
            </div>
          </div>

          <div className="min-w-0 xl:flex-1">
            <p className="theme-text ml-1 mb-1.5 text-xs font-semibold">{t("search.amenities")}</p>
            <div className="flex flex-wrap gap-2">
              {amenityOptions.map((amenity) => {
                const amenityValue = String(amenity.value);
                const active = filters.amenities?.includes(amenityValue);
                return (
                  <button
                    key={amenityValue}
                    onClick={() => toggleAmenity(amenityValue)}
                    className={`rounded-xl border px-3 py-1.5 text-xs font-bold whitespace-nowrap transition xl:px-2.5 ${
                      active
                        ? "border-emerald-500/50 bg-emerald-500/20 text-emerald-300"
                        : "theme-card theme-muted hover:text-current"
                    }`}
                  >
                    {amenity.label}
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
