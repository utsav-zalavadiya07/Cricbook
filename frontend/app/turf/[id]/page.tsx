"use client";
import { Button, Card, SlotPriceDisplay, TileInfo } from "@/components/ui";

import { useEffect, useState } from "react";
import type { ReactNode } from "react";
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
  const startingPrice = turf?.startingSlotPrice ?? turf?.pricePerHour;
  const maximumPrice = turf?.maximumSlotPrice ?? startingPrice;
  const hasMixedSpecialPrices = startingPrice != null && maximumPrice != null && turf?.pricePerHour != null
    && Number(startingPrice) < Number(turf.pricePerHour)
    && Number(startingPrice) !== Number(maximumPrice);

  if (loadingTurf) {
    return (
      <div className="theme-page-player flex min-h-screen items-center justify-center theme-muted">
        Loading turf details...
      </div>
    );
  }

  if (!turf) {
    return (
      <main className="theme-page-player flex min-h-screen items-center justify-center px-4">
        <Card className="w-full max-w-lg p-8 text-center">
          <h1 className="theme-title text-2xl font-bold">Turf not found</h1>
          <p className="theme-muted mt-2 text-sm">This venue may no longer be available.</p>
          <Button className="mt-6" onClick={() => router.push("/search")}>Back to search</Button>
        </Card>
      </main>
    );
  }

  return (
    <main className="theme-page-player min-h-screen">
      <div className="w-full space-y-5 px-2 py-4 sm:px-4 sm:py-5 xl:px-5 2xl:px-6">
        <section className="theme-panel overflow-hidden rounded-[24px]">
          <div className="grid xl:grid-cols-[minmax(0,1.65fr)_minmax(360px,0.72fr)]">
            <div className="relative min-h-[320px] overflow-hidden bg-gradient-to-br from-emerald-100 to-sky-100 sm:min-h-[420px] xl:min-h-[500px] dark:from-emerald-900/30 dark:to-slate-800">
              {turf.images?.length ? (
                <Image
                  src={getImageUrl(turf.images[activeImage])}
                  alt={`${turf.name} venue`}
                  fill
                  sizes="(min-width: 1280px) 68vw, 100vw"
                  className="object-cover"
                  priority
                  unoptimized
                />
              ) : (
                <div className="theme-soft flex h-full min-h-[320px] items-center justify-center text-sm sm:min-h-[420px] xl:min-h-[500px]">No image available</div>
              )}
              <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-slate-950/65 via-transparent to-slate-950/10" />

              {turf.images && turf.images.length > 1 && (
                <div className="absolute bottom-4 left-4 right-4 flex items-center gap-2 overflow-x-auto rounded-2xl bg-slate-950/40 p-2 backdrop-blur-md sm:right-auto">
                  {turf.images.map((image, index) => (
                    <button
                      key={`${image}-${index}`}
                      type="button"
                      aria-label={`View venue photo ${index + 1}`}
                      aria-pressed={activeImage === index}
                      onClick={() => setActiveImage(index)}
                      className={`relative h-14 w-20 shrink-0 overflow-hidden rounded-xl border-2 transition ${activeImage === index ? "border-emerald-400 shadow-lg shadow-emerald-950/30" : "border-white/50 hover:border-white"}`}
                    >
                      <Image src={getImageUrl(image)} alt="" fill sizes="80px" className="object-cover" unoptimized />
                    </button>
                  ))}
                </div>
              )}
            </div>

            <div className="flex flex-col p-5 sm:p-7 xl:p-8">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-xs font-bold uppercase tracking-[0.18em] text-emerald-600 dark:text-emerald-300">Book this venue</p>
                  <h1 className="theme-title mt-2 flex items-center gap-2 text-3xl font-bold sm:text-4xl">
                    <span>{turf.name}</span>
                    <TileInfo label={`${turf.name} information`} align="left" tooltipClassName="whitespace-normal">
                      {turf.location}, {turf.city}
                    </TileInfo>
                  </h1>
                </div>
                {turf.rating > 0 && (
                  <div className="shrink-0 rounded-2xl border border-amber-500/25 bg-amber-500/10 px-3 py-2 text-center">
                    <p className="font-bold text-amber-600 dark:text-amber-300">★ {turf.rating.toFixed(1)}</p>
                    <p className="theme-soft text-[10px]">{turf.totalReviews || reviews.length} reviews</p>
                  </div>
                )}
              </div>

              <p className="theme-muted mt-4 flex items-center gap-2 text-sm">
                <span aria-hidden="true" className="text-emerald-500">●</span>
                {turf.location}, {turf.city}
              </p>

              <div className="mt-5 flex flex-wrap gap-2">
                <Pill>{turf.pitchType}</Pill>
                <Pill>{turf.boxType}</Pill>
                {(turf.sports?.length ? turf.sports : turf.sport ? [turf.sport] : []).map((sport) => <Pill key={sport}>{sport}</Pill>)}
              </div>

              <div className="theme-panel-soft mt-7 rounded-2xl p-5">
                {startingPrice != null && (
                  <SlotPriceDisplay
                    price={Number(startingPrice)}
                    compareAtPrice={hasMixedSpecialPrices ? Number(turf.pricePerHour) : undefined}
                    prefix={Number(startingPrice) === Number(maximumPrice) ? "Slot price" : "Slots from"}
                    priceClassName="text-3xl sm:text-4xl"
                  />
                )}
                {hasMixedSpecialPrices && <p className="mt-2 text-sm font-semibold text-emerald-700 dark:text-emerald-300">Selected times are available below the regular listing price.</p>}
                <p className="theme-muted mt-2 text-xs leading-5">Choose a date to compare live slot prices. Any eligible membership discount is applied before payment.</p>
              </div>

              <div className="mt-auto grid gap-3 pt-6 sm:grid-cols-2 xl:grid-cols-1 2xl:grid-cols-2">
                <Button onClick={() => document.getElementById("available-slots")?.scrollIntoView({ behavior: "smooth", block: "start" })}>
                  View available slots
                </Button>
                <button
                  type="button"
                  onClick={toggleFavorite}
                  disabled={savingFavorite}
                  className={`min-h-11 rounded-xl border px-4 py-2.5 text-sm font-bold transition hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-60 ${
                    favorite ? "border-red-500/40 bg-red-500/10 text-red-600 dark:text-red-200" : "theme-card theme-text hover:border-emerald-500/50"
                  }`}
                >
                  {savingFavorite ? "Saving..." : favorite ? "Remove saved turf" : "Save turf"}
                </button>
              </div>
            </div>
          </div>
        </section>

        <div className="grid gap-5 xl:grid-cols-[minmax(0,1.45fr)_minmax(360px,0.55fr)]">
          <Card className="p-5 sm:p-6">
            <SectionHeading title="Venue details" description="Everything you need to know before arriving." />
            <div className="mt-5 grid gap-5 lg:grid-cols-2">
              <div className="theme-panel-soft rounded-2xl p-5">
                <h3 className="theme-title text-base font-bold">Amenities</h3>
                <div className="mt-4 flex flex-wrap gap-2">
                  {turf.amenities?.length ? (turf.amenities as TurfAmenity[]).map((amenity) => (
                    <Pill key={typeof amenity === "string" ? amenity : amenity.amenity}>
                      {typeof amenity === "string" ? amenity : amenity.amenity}
                    </Pill>
                  )) : <p className="theme-muted text-sm">Amenities have not been listed yet.</p>}
                </div>
              </div>

              <div className="theme-panel-soft rounded-2xl p-5">
                <h3 className="theme-title text-base font-bold">Before you play</h3>
                <ul className="theme-muted mt-4 space-y-3 text-sm">
                  <RuleItem>Arrive 10 minutes before your slot.</RuleItem>
                  <RuleItem>Carry a valid booking confirmation.</RuleItem>
                  <RuleItem>Follow venue guidance for safe equipment and footwear.</RuleItem>
                </ul>
              </div>
            </div>
          </Card>

          <Card className="overflow-hidden p-5 sm:p-6">
            <SectionHeading title="Location" description={`${turf.location}, ${turf.city}`} />
            <div className="mt-5 overflow-hidden rounded-2xl border border-white/10 bg-white">
              <iframe title={`${turf.name} map`} src={`https://www.google.com/maps?q=${encodeURIComponent(mapQuery)}&output=embed`} className="h-60 w-full xl:h-[226px]" loading="lazy" />
            </div>
            <a
              href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(mapQuery)}`}
              target="_blank"
              rel="noreferrer"
              className="mt-4 inline-flex min-h-10 items-center rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 text-sm font-bold text-emerald-700 transition hover:bg-emerald-500/15 dark:text-emerald-300"
            >
              Open directions ↗
            </a>
          </Card>
        </div>

        <Card id="available-slots" className="scroll-mt-6 p-5 sm:p-6">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <SectionHeading title="Available slots" description="Choose a date, compare live prices, and select one or more times." />
            <label className="block w-full lg:max-w-[280px]">
              <span className="theme-muted mb-2 block text-xs font-bold uppercase tracking-wider">Playing date</span>
              <input
                type="date"
                min={today}
                value={date}
                onChange={(event) => {
                  setSelectedSlots([]);
                  setDate(event.target.value);
                }}
                className="theme-input w-full rounded-xl px-4 py-3 text-sm outline-none transition focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500"
              />
            </label>
          </div>

          <div className="mt-5 flex flex-col gap-2 rounded-2xl border border-emerald-500/20 bg-emerald-500/10 px-4 py-3 text-sm sm:flex-row sm:items-center sm:justify-between">
            <p className="font-semibold text-emerald-700 dark:text-emerald-300">Live slot prices are set by the turf owner.</p>
            <p className="theme-muted text-xs">Membership savings appear before payment.</p>
          </div>

          <div className="mt-5">
            {!date ? (
              <div className="theme-panel-soft flex min-h-36 flex-col items-center justify-center rounded-2xl border border-dashed px-4 text-center">
                <p className="theme-title font-semibold">Select a playing date</p>
                <p className="theme-muted mt-1 text-sm">Available times and current prices will appear here.</p>
              </div>
            ) : loadingSlots ? (
              <div className="theme-panel-soft flex min-h-36 items-center justify-center rounded-2xl">
                <p className="theme-muted text-sm">Loading available slots...</p>
              </div>
            ) : (
              <SlotList slots={slots} selectedSlots={selectedSlots} onSelect={toggleSelectedSlot} />
            )}
          </div>

          {selectedSlots.length > 0 && (
            <div className="mt-5 flex flex-col gap-4 rounded-2xl border border-emerald-500/30 bg-emerald-500/10 p-4 sm:flex-row sm:items-center sm:justify-between sm:p-5">
              <div>
                <p className="font-bold text-emerald-700 dark:text-emerald-200">
                  {selectedSlots.length} slot{selectedSlots.length !== 1 ? "s" : ""} selected
                </p>
                <p className="theme-muted mt-1 text-sm">
                  {formatSelectedDate(date)} · Regular total ₹{selectedSlots.reduce((sum, slot) => sum + Number(slot.price || 0), 0).toLocaleString("en-IN")}
                </p>
              </div>
              <Button onClick={handleBook} className="w-full sm:w-auto">Continue to booking</Button>
            </div>
          )}
        </Card>

        <Card className="p-5 sm:p-6">
          <SectionHeading title="Reviews" description="Verified feedback from players who booked this turf." trailing={`${reviews.length} review${reviews.length !== 1 ? "s" : ""}`} />
          <div className="mt-5 grid gap-5 2xl:grid-cols-[minmax(0,0.95fr)_minmax(520px,1.05fr)]">
            <div className="space-y-5">
              <div className="theme-panel-soft rounded-2xl p-5">
                <RatingSummary avgRating={avgRating} total={reviews.length} summary={summary} />
              </div>
              <ReviewForm turfId={turfId} onSuccess={refreshReviews} />
            </div>

            <div className="theme-panel-soft rounded-2xl p-4 sm:p-5">
              <div className="mb-4 flex items-center justify-between gap-3">
                <div>
                  <h3 className="theme-title font-bold">Player feedback</h3>
                  <p className="theme-muted mt-1 text-xs">Recent experiences shared by the community.</p>
                </div>
                <span className="rounded-full border border-white/10 px-3 py-1 text-xs font-semibold theme-muted">{reviews.length}</span>
              </div>
              <ReviewList reviews={reviews} loading={reviewsLoading} onChanged={refreshReviews} />
            </div>
          </div>
        </Card>
      </div>
    </main>
  );
}

function SectionHeading({ title, description, trailing }: { title: string; description: string; trailing?: string }) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-3">
      <div>
        <h2 className="theme-title text-xl font-bold sm:text-2xl">{title}</h2>
        <p className="theme-muted mt-1 text-sm">{description}</p>
      </div>
      {trailing && <span className="theme-card rounded-full px-3 py-1.5 text-xs font-semibold theme-muted">{trailing}</span>}
    </div>
  );
}

function Pill({ children }: { children: ReactNode }) {
  return <span className="theme-card rounded-full px-3 py-1.5 text-xs font-semibold theme-muted">{children}</span>;
}

function RuleItem({ children }: { children: ReactNode }) {
  return (
    <li className="flex items-start gap-3">
      <span aria-hidden="true" className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-emerald-500/15 text-xs font-bold text-emerald-600 dark:text-emerald-300">✓</span>
      <span>{children}</span>
    </li>
  );
}

function formatSelectedDate(value: string) {
  if (!value) return "";
  return new Intl.DateTimeFormat("en-IN", { day: "numeric", month: "short", year: "numeric" }).format(new Date(`${value}T00:00:00`));
}
