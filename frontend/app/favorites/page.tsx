"use client";
import { TileInfo } from "@/components/ui";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { getFavorites, removeFavorite } from "@/modules/favorites/services/favoriteService";
import { Turf } from "@/modules/turf/types/turf.types";
import { getImageUrl } from "@/utils/image";
import { useToast } from "@/modules/ui/ToastProvider";

export default function FavoritesPage() {
  const [turfs, setTurfs] = useState<Turf[]>([]);
  const [loading, setLoading] = useState(true);
  const { toast } = useToast();

  const loadFavorites = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getFavorites();
      setTurfs(res.data);
    } catch {
      toast("Could not load saved turfs.", "error");
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    const timer = window.setTimeout(() => void loadFavorites(), 0);
    return () => window.clearTimeout(timer);
  }, [loadFavorites]);

  const handleRemove = async (turfId?: number) => {
    if (!turfId) return;
    await removeFavorite(turfId);
    setTurfs((prev) => prev.filter((turf) => turf.id !== turfId));
    toast("Removed from favorites.", "success");
  };

  return (
    <main className="theme-page-player min-h-screen px-4 py-8 sm:px-6">
      <div className="mx-auto max-w-7xl">
        <div className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h1 className="theme-title text-3xl font-bold">Saved Turfs
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Saved Turfs information" focusable={false}>Keep track of turfs you want to compare or book later.</TileInfo>
              </span></h1>
          </div>
          <Link href="/search" className="w-fit rounded-xl bg-emerald-500 px-5 py-2.5 text-sm font-semibold text-white hover:bg-emerald-400">
            Find More Turfs
          </Link>
        </div>

        {loading && <div className="theme-muted py-24 text-center">Loading saved turfs...</div>}

        {!loading && turfs.length === 0 && (
          <div className="theme-panel flex min-h-72 flex-col items-center justify-center rounded-3xl border border-dashed text-center">
            <p className="theme-title text-lg font-semibold">No saved turfs yet</p>
            <p className="theme-muted mt-1 text-sm">Use Save Turf while browsing to build your shortlist.</p>
          </div>
        )}

        {!loading && turfs.length > 0 && (
          <div className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
            {turfs.map((turf) => (
              <article key={turf.id} className="theme-panel overflow-hidden rounded-2xl">
                <Link href={`/turf/${turf.id}`} className="relative block h-48 overflow-hidden bg-slate-800">
                  {turf.images?.length ? (
                    <Image src={getImageUrl(turf.images[0])} alt={turf.name} fill sizes="(min-width: 1280px) 33vw, (min-width: 768px) 50vw, 100vw" className="object-cover" unoptimized />
                  ) : (
                    <div className="theme-soft flex h-full items-center justify-center">No image</div>
                  )}
                </Link>
                <div className="space-y-3 p-4">
                  <div>
                    <h2 className="theme-title font-semibold">{turf.name}</h2>
                    <p className="theme-muted mt-1 text-sm">{turf.city} / {turf.location}</p>
                  </div>
                  <div className="flex items-center justify-between">
                    <p className="font-bold text-emerald-600 dark:text-emerald-300">Rs {turf.pricePerHour}/hr</p>
                    <button onClick={() => handleRemove(turf.id)} className="rounded-lg border border-red-500/30 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-500/10 dark:text-red-200">
                      Remove
                    </button>
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}
      </div>
    </main>
  );
}
