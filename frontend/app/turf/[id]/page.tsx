"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import { useParams, useRouter } from "next/navigation";
import SlotList from "@/modules/search/components/SlotList";
import { getAvailability, getTurfDetail } from "@/modules/search/services/searchService";
import { SearchTurf, SlotAvailability } from "@/modules/search/types/search.types";
import useReviews from "@/modules/review/hooks/useReviews";
import ReviewForm from "@/modules/review/components/ReviewForm";
import ReviewList from "@/modules/review/components/ReviewList";
import RatingSummary from "@/modules/review/components/RatingSummary";
import { getImageUrl } from "@/utils/image";
import { getFavoriteStatus, removeFavorite, saveFavorite } from "@/modules/favorites/services/favoriteService";
import { useToast } from "@/modules/ui/ToastProvider";
import { useAuthStore } from "@/store/authStore";

type TurfAmenity = string | { amenity: string };

export default function TurfDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const turfId = Number(id);
  const token = useAuthStore((state) => state.token);
  const { toast } = useToast();

  const [turf, setTurf] = useState<SearchTurf | null>(null);
  const [date, setDate] = useState("");
  const [slots, setSlots] = useState<SlotAvailability[]>([]);
  const [selectedSlots, setSelectedSlots] = useState<SlotAvailability[]>([]);
  const [loadingTurf, setLoadingTurf] = useState(true);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [activeImage, setActiveImage] = useState(0);
  const [favorite, setFavorite] = useState(false);
  const [savingFavorite, setSavingFavorite] = useState(false);

  const { reviews, summary, loading: reviewsLoading, avgRating, refresh: refreshReviews } = useReviews(turfId);

  useEffect(() => {
    getTurfDetail(turfId)
      .then((res) => {
        setTurf(res.data);
        setActiveImage(0);
      })
      .finally(() => setLoadingTurf(false));
  }, [turfId]);

  useEffect(() => {
    if (!token) return;
    getFavoriteStatus(turfId)
      .then((res) => setFavorite(Boolean(res.data.favorite)))
      .catch(() => undefined);
  }, [token, turfId]);

  useEffect(() => {
    if (!date) return;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoadingSlots(true);
    getAvailability(turfId, date)
      .then((res) => setSlots(res.data))
      .catch(() => setSlots([]))
      .finally(() => setLoadingSlots(false));
  }, [date, turfId]);

  const handleBook = () => {
    if (selectedSlots.length === 0 || !date) return;
    const sortedSlots = [...selectedSlots].sort((a, b) => a.startTime.localeCompare(b.startTime));
    const totalPrice = sortedSlots.reduce((sum, slot) => sum + Number(slot.price || 0), 0);
    const params = new URLSearchParams({
      turfId: String(turfId),
      slotIds: sortedSlots.map((slot) => slot.slotId).join(","),
      date,
      price: String(totalPrice),
      startTime: sortedSlots[0].startTime,
      endTime: sortedSlots[sortedSlots.length - 1].endTime,
      slots: JSON.stringify(sortedSlots.map((slot) => ({
        slotId: slot.slotId,
        startTime: slot.startTime,
        endTime: slot.endTime,
        price: slot.price,
      }))),
      ...(turf?.name ? { turfName: turf.name } : {}),
    });
    router.push(`/booking?${params.toString()}`);
  };

  const toggleSelectedSlot = (slot: SlotAvailability) => {
    setSelectedSlots((current) =>
      current.some((item) => item.slotId === slot.slotId)
        ? current.filter((item) => item.slotId !== slot.slotId)
        : [...current, slot]
    );
  };

  const toggleFavorite = async () => {
    if (!token) {
      router.push("/auth/login");
      return;
    }
    setSavingFavorite(true);
    try {
      if (favorite) {
        await removeFavorite(turfId);
        setFavorite(false);
        toast("Removed from saved turfs.", "success");
      } else {
        await saveFavorite(turfId);
        setFavorite(true);
        toast("Turf saved.", "success");
      }
    } catch {
      toast("Could not update favorite.", "error");
    } finally {
      setSavingFavorite(false);
    }
  };

  const today = new Date().toISOString().split("T")[0];
  const mapQuery = turf ? `${turf.location}, ${turf.city}` : "";

  if (loadingTurf) {
    return (
      <div className="theme-page-player flex min-h-screen items-center justify-center theme-muted">
        Loading turf details...
      </div>
    );
  }

  return (
    <main className="theme-page-player min-h-screen">
      <div className="mx-auto max-w-6xl space-y-8 px-4 py-8 sm:px-6">
        {turf && (
          <>
            <section className="theme-panel overflow-hidden rounded-[24px]">
              <div className="relative flex h-64 items-center justify-center bg-gradient-to-br from-emerald-100 to-sky-100 sm:h-80 dark:from-emerald-900/30 dark:to-slate-800">
                {turf.images?.length ? (
                  <Image src={getImageUrl(turf.images[activeImage])} alt={turf.name} fill sizes="(min-width: 1024px) 896px, 100vw" className="object-cover" unoptimized />
                ) : (
                  <span className="theme-soft text-sm">No image available</span>
                )}
                {turf.images && turf.images.length > 1 && (
                  <div className="absolute bottom-4 left-4 right-4 flex gap-2 overflow-x-auto">
                    {turf.images.map((image, index) => (
                      <button
                        key={image}
                        onClick={() => setActiveImage(index)}
                        className={`relative h-14 w-20 shrink-0 overflow-hidden rounded-lg border ${activeImage === index ? "border-emerald-400" : "border-white/40"}`}
                      >
                        <Image src={getImageUrl(image)} alt="" fill sizes="80px" className="object-cover" unoptimized />
                      </button>
                    ))}
                  </div>
                )}
              </div>

              <div className="flex flex-col gap-5 p-6 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <h1 className="theme-title text-3xl font-bold">{turf.name}</h1>
                  <p className="theme-muted mt-2 text-sm">{turf.city} / {turf.location}</p>
                  <div className="mt-4 flex flex-wrap gap-2">
                    <Pill>{turf.pitchType}</Pill>
                    <Pill>{turf.boxType}</Pill>
                    {turf.rating > 0 && <span className="rounded-full border border-amber-500/30 bg-amber-500/10 px-3 py-1 text-xs text-amber-700 dark:text-amber-200">Rating {turf.rating.toFixed(1)}</span>}
                  </div>
                </div>
                <div className="sm:text-right">
                  <p className="text-3xl font-bold text-emerald-700 dark:text-emerald-300">Rs {turf.pricePerHour}</p>
                  <p className="theme-soft text-xs">per hour</p>
                  <button
                    onClick={toggleFavorite}
                    disabled={savingFavorite}
                    className={`mt-4 rounded-xl border px-4 py-2 text-sm font-semibold disabled:opacity-60 ${
                      favorite ? "border-red-500/40 bg-red-500/10 text-red-600 dark:text-red-200" : "border-emerald-500/40 bg-emerald-500/10 text-emerald-700 dark:text-emerald-200"
                    }`}
                  >
                    {savingFavorite ? "Saving..." : favorite ? "Saved" : "Save Turf"}
                  </button>
                </div>
              </div>
            </section>

            <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
              <section className="theme-panel rounded-[24px] p-6 lg:col-span-2">
                <h2 className="theme-title text-lg font-bold">Amenities</h2>
                <div className="mt-4 flex flex-wrap gap-2">
                  {turf.amenities?.length ? (turf.amenities as TurfAmenity[]).map((amenity) => (
                    <Pill key={typeof amenity === "string" ? amenity : amenity.amenity}>
                      {typeof amenity === "string" ? amenity : amenity.amenity}
                    </Pill>
                  )) : <p className="theme-muted text-sm">Amenities have not been listed yet.</p>}
                </div>
                <h2 className="theme-title mt-6 text-lg font-bold">Rules</h2>
                <ul className="theme-muted mt-3 space-y-2 text-sm">
                  <li>Arrive 10 minutes before your slot.</li>
                  <li>Carry valid booking confirmation.</li>
                  <li>Venue staff may restrict unsafe equipment or footwear.</li>
                </ul>
              </section>

              <section className="theme-panel rounded-[24px] p-6">
                <h2 className="theme-title text-lg font-bold">Map Location</h2>
                <div className="mt-4 overflow-hidden rounded-2xl border border-white/10 bg-white">
                  <iframe title={`${turf.name} map`} src={`https://www.google.com/maps?q=${encodeURIComponent(mapQuery)}&output=embed`} className="h-64 w-full" loading="lazy" />
                </div>
                <a href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(mapQuery)}`} target="_blank" className="mt-3 inline-block text-sm font-medium text-emerald-700 hover:text-emerald-600 dark:text-emerald-300">
                  Directions
                </a>
              </section>
            </div>

            <section className="theme-panel rounded-[24px] p-6">
              <h2 className="theme-title text-lg font-bold">Available Slots</h2>
              <p className="theme-muted mt-1 text-xs">Select a date to see available time slots.</p>
              <input
                type="date"
                min={today}
                value={date}
                onChange={(event) => {
                  setSelectedSlots([]);
                  setDate(event.target.value);
                }}
                className="theme-input mt-5 w-full max-w-xs rounded-xl px-4 py-3 text-sm outline-none focus:border-emerald-500"
              />

              {date && (
                <div className="mt-5">
                  {loadingSlots ? <p className="theme-muted text-sm">Loading slots...</p> : (
                    <SlotList slots={slots} selectedSlots={selectedSlots} onSelect={toggleSelectedSlot} />
                  )}
                </div>
              )}

              {selectedSlots.length > 0 && (
                <div className="mt-5 flex flex-col gap-4 rounded-2xl border border-emerald-500/30 bg-emerald-500/10 p-4 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p className="font-semibold text-emerald-700 dark:text-emerald-200">
                      {selectedSlots.length} slot{selectedSlots.length !== 1 ? "s" : ""} selected
                    </p>
                    <p className="theme-muted text-xs">
                      {date} / Rs {selectedSlots.reduce((sum, slot) => sum + Number(slot.price || 0), 0)}
                    </p>
                  </div>
                  <button onClick={handleBook} className="rounded-xl bg-emerald-500 px-5 py-2 text-sm font-semibold text-white hover:bg-emerald-400">
                    Book Selected Slots
                  </button>
                </div>
              )}
            </section>

            <section className="theme-panel rounded-[24px] p-6">
              <div className="flex items-center justify-between">
                <h2 className="theme-title text-lg font-bold">Reviews</h2>
                <span className="theme-soft text-xs">{reviews.length} review{reviews.length !== 1 ? "s" : ""}</span>
              </div>
              <div className="mt-5">
                <RatingSummary avgRating={avgRating} total={reviews.length} summary={summary} />
              </div>
              <div className="mt-6 border-t border-white/10 pt-5">
                <ReviewForm turfId={turfId} onSuccess={refreshReviews} />
              </div>
              <div className="mt-6 border-t border-white/10 pt-5">
                <ReviewList reviews={reviews} loading={reviewsLoading} onChanged={refreshReviews} />
              </div>
            </section>
          </>
        )}
      </div>
    </main>
  );
}

function Pill({ children }: { children: React.ReactNode }) {
  return <span className="theme-card rounded-full px-3 py-1.5 text-xs theme-muted">{children}</span>;
}
