"use client";

import { Suspense, useCallback, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { useI18n } from "@/modules/i18n/I18nProvider";
import SearchBar from "@/modules/search/components/SearchBar";
import TurfResultCard from "@/modules/search/components/TurfResultCard";
import { saveRecentSearch } from "@/modules/personalization/services/personalizationService";
import { searchTurfs } from "@/modules/search/services/searchService";
import { SearchFilters, SearchTurf } from "@/modules/search/types/search.types";
import { useAuthStore } from "@/store/authStore";

function normalizeFilters(filters: SearchFilters): SearchFilters {
  const normalized: SearchFilters = {};

  Object.entries(filters).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;

    if (Array.isArray(value)) {
      if (value.length > 0) {
        normalized[key as keyof SearchFilters] = value as never;
      }
      return;
    }

    if (typeof value === "string") {
      const trimmed = value.trim();
      if (trimmed) {
        normalized[key as keyof SearchFilters] = trimmed as never;
      }
      return;
    }

    if (typeof value === "number" && Number.isNaN(value)) return;

    normalized[key as keyof SearchFilters] = value as never;
  });

  return normalized;
}

function hasSearchCriteria(filters: SearchFilters) {
  return Object.keys(normalizeFilters(filters)).length > 0;
}

export default function SearchPage() {
  return (
    <Suspense fallback={<div className="theme-page-player min-h-screen" />}>
      <SearchPageContent />
    </Suspense>
  );
}

function SearchPageContent() {
  const { t } = useI18n();
  const searchParams = useSearchParams();
  const token = useAuthStore((state) => state.token);
  const cityParam = searchParams.get("city") ?? undefined;
  const initialFilters = useMemo(() => (cityParam ? { city: cityParam } : {}), [cityParam]);

  const [filters, setFilters] = useState<SearchFilters>(() => initialFilters);
  const [appliedFilters, setAppliedFilters] = useState<SearchFilters>(() => initialFilters);
  const [turfs, setTurfs] = useState<SearchTurf[]>([]);
  const [loading, setLoading] = useState(true);
  const [searched, setSearched] = useState(true);
  const [error, setError] = useState("");
  const [selectedSport, setSelectedSport] = useState<string | null>(null);

  const applyFilters = useCallback((nextFilters: SearchFilters) => {
    setLoading(true);
    setError("");
    setAppliedFilters(normalizeFilters(nextFilters));
    setSearched(true);
  }, []);

  const handleSearch = useCallback(() => {
    applyFilters(filters);
  }, [applyFilters, filters]);

  const handleClear = useCallback(() => {
    setFilters({});
    setSelectedSport(null);
    setLoading(true);
    setError("");
    setAppliedFilters({});
    setSearched(true);
  }, []);

  useEffect(() => {
    let isMounted = true;

    searchTurfs({ ...appliedFilters, size: 100 })
      .then((res) => {
        if (!isMounted) return;
        setTurfs(res.data);
        if (token && hasSearchCriteria(appliedFilters)) {
          saveRecentSearch(appliedFilters).catch(() => undefined);
        }
      })
      .catch(() => {
        if (!isMounted) return;
        setError(t("search.errorFailed"));
        setTurfs([]);
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [appliedFilters, t, token]);

  const filteredTurfs = useMemo(() => {
    if (!selectedSport) return turfs;
    return turfs.filter((turf) => {
      const availableSports = (turf.sports ?? [turf.sport]).filter(Boolean).map((sport) => String(sport).toLowerCase());
      return availableSports.includes(selectedSport.toLowerCase());
    });
  }, [selectedSport, turfs]);

  return (
    <div className="theme-page-player min-h-screen">
      <div className="mx-auto max-w-7xl px-6 pb-4 pt-5">
        <SearchBar
          filters={filters}
          setFilters={setFilters}
          onSearch={handleSearch}
          onApplyFilters={applyFilters}
          onClear={handleClear}
          selectedSport={selectedSport}
          setSelectedSport={setSelectedSport}
        />
      </div>

      <div className="mx-auto mt-2 max-w-7xl px-6 pb-12">
        {loading && (
          <div className="flex h-48 items-center justify-center">
            <div className="theme-muted flex items-center gap-3">
              <svg className="h-5 w-5 animate-spin text-emerald-400" viewBox="0 0 24 24" fill="none">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
              </svg>
              {t("search.loading")}
            </div>
          </div>
        )}

        {error && !loading && (
          <div className="mb-4 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-400">{error}</div>
        )}

        {!loading && searched && turfs.length === 0 && !error && (
          <div className="theme-card flex h-60 flex-col items-center justify-center rounded-3xl border-dashed">
            <span className="mb-3 rounded-2xl border border-sky-500/20 bg-sky-500/10 px-4 py-3 text-sm font-black text-sky-500">NO MATCH</span>
            <p className="theme-title font-medium">{t("search.emptyTitle")}</p>
            <p className="theme-muted mt-1 text-sm">{t("search.emptyBody")}</p>
          </div>
        )}

        {!loading && turfs.length > 0 && filteredTurfs.length === 0 && (
          <div className="theme-card flex h-60 flex-col items-center justify-center rounded-3xl border-dashed">
            <span className="mb-3 rounded-2xl border border-sky-500/20 bg-sky-500/10 px-4 py-3 text-sm font-black text-sky-500">FILTER</span>
            <p className="theme-title font-medium">{t("search.emptySportTitle")}</p>
            <p className="theme-muted mt-1 text-sm">{t("search.emptySportBody")}</p>
          </div>
        )}

        {!loading && filteredTurfs.length > 0 && (
          <>
            <div className="mb-6 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <p className="theme-title text-lg font-black">
                  {t("search.heroTitlePrefix")} <span className="text-emerald-400">{t("search.heroTitleAccent")}</span>
                </p>
                <p className="theme-muted text-sm">
                  {t("search.heroDescription")} - {t("search.resultsFound", {
                    count: filteredTurfs.length,
                    suffix: filteredTurfs.length !== 1 ? "s" : "",
                  })}
                </p>
              </div>
              <p className="theme-soft text-xs font-semibold uppercase tracking-[0.16em]">
                Showing {filteredTurfs.length} of {turfs.length}
              </p>
            </div>
            <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              {filteredTurfs.map((turf) => (
                <TurfResultCard key={turf.id} turf={turf} />
              ))}
            </div>
          </>
        )}
      </div>
    </div>
  );
}
