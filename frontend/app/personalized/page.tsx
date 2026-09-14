"use client";
import { TileInfo } from "@/components/ui";

import { useEffect, useState } from "react";
import Link from "next/link";
import api from "@/services/api";
import { Turf } from "@/modules/turf/types/turf.types";

export default function AIRecommendationDashboard() {
  const [feeds, setFeeds] = useState<Record<string, Turf[]>>({
    FOR_YOU: [],
    NEARBY: [],
    WEEKEND: [],
  });
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<Turf[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [searchCity, setSearchCity] = useState("Surat");
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState("");

  async function fetchRecommendations() {
    try {
      setLoading(true);
      setStatus("");
      const res = await api.get("/ai/recommendations");
      setFeeds(res.data);
    } catch (err) {
      console.error("Error loading recommendations", err);
      setStatus("Unable to load personalized recommendations right now.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void fetchRecommendations(), 0);
    return () => window.clearTimeout(timer);
  }, []);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!searchQuery.trim()) return;

    try {
      setIsSearching(true);
      setStatus("");
      const res = await api.get(`/ai/search/smart?query=${encodeURIComponent(searchQuery)}&city=${searchCity}`);
      setSearchResults(res.data);
    } catch (err) {
      console.error("Error doing smart search", err);
      setStatus("Smart search is unavailable right now.");
    } finally {
      setIsSearching(false);
    }
  };

  return (
    <main className="theme-page-player min-h-screen px-4 py-8 sm:px-8">
      <div className="mx-auto max-w-7xl space-y-8">
        <section className="theme-panel relative overflow-hidden rounded-[28px] p-8">
          <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_82%_18%,rgba(16,185,129,0.16),transparent_28%),radial-gradient(circle_at_10%_90%,rgba(14,165,233,0.12),transparent_24%)]" />
          <div className="relative z-10 flex flex-col gap-7 md:flex-row md:items-end md:justify-between">
            <div className="max-w-3xl">
              <span className="theme-badge inline-flex items-center rounded-full px-3 py-1 text-xs font-bold">
                Match-day intelligence active
              </span>
              <h1 className="theme-title mt-4 text-4xl font-black tracking-tight sm:text-5xl">Your cricket booking cockpit
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Your cricket booking cockpit information" focusable={false}>Find turfs based on budget, timing, location, team needs, and previous booking behavior.</TileInfo>
              </span></h1>
              <div className="mt-6 grid gap-3 sm:grid-cols-3">
                <HeroBenefit title="Smarter picks" body="Recommended turfs for your city and play pattern." />
                <HeroBenefit title="Peak slot signals" body="Spot high-demand timings before they fill up." />
                <HeroBenefit title="Fast rebooking" body="Return to familiar venues and preferred slots quickly." />
              </div>
            </div>

            <div className="flex flex-col gap-3 sm:flex-row">
              <Link
                href="/assistant"
                className="inline-flex items-center justify-center rounded-xl bg-gradient-to-r from-emerald-500 to-sky-500 px-5 py-3 text-sm font-bold text-white shadow-lg shadow-emerald-500/20 transition hover:brightness-105"
              >
                Open Assistant
              </Link>
              <Link
                href="/personalized/insights"
                className="theme-card inline-flex items-center justify-center rounded-xl px-5 py-3 text-sm font-bold theme-text transition hover:border-sky-400/40"
              >
                Player Insights
              </Link>
            </div>
          </div>
        </section>

        <section className="theme-panel rounded-[24px] p-6">
          <div className="mb-5">
            <h2 className="theme-title text-xl font-bold">Intent-based turf search</h2>
            <p className="theme-muted mt-1 text-sm">Describe the match you want to play and let Cricbook rank matching cricket venues.</p>
          </div>

          <form onSubmit={handleSearch} className="grid gap-3 lg:grid-cols-[1fr_240px_190px]">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Try: premium evening turf in Surat under Rs 1200"
              className="theme-input min-h-12 rounded-xl px-4 py-3 text-sm outline-none focus:border-sky-500 focus:ring-2 focus:ring-sky-500/10"
            />
            <select
              value={searchCity}
              onChange={(e) => setSearchCity(e.target.value)}
              className="theme-input min-h-12 rounded-xl px-4 py-3 text-sm outline-none focus:border-sky-500 focus:ring-2 focus:ring-sky-500/10"
            >
              <option value="Surat">Surat</option>
              <option value="Mumbai">Mumbai</option>
              <option value="Delhi">Delhi</option>
            </select>
            <button
              type="submit"
              className="min-h-12 rounded-xl bg-sky-600 px-6 py-3 text-sm font-bold text-white shadow-lg shadow-sky-500/20 transition hover:bg-sky-500 disabled:opacity-60"
              disabled={isSearching}
            >
              {isSearching ? "Analyzing..." : "Analyze Intent"}
            </button>
          </form>

          {status && <p className="mt-4 rounded-xl border border-amber-500/20 bg-amber-500/10 px-4 py-3 text-sm text-amber-700 dark:text-amber-200">{status}</p>}

          {searchQuery && (
            <div className="mt-6 space-y-4">
              <h3 className="theme-muted text-sm font-semibold">Search results matching your query</h3>
              {isSearching ? (
                <p className="theme-muted text-sm">Computing recommendations...</p>
              ) : searchResults.length === 0 ? (
                <p className="theme-soft text-sm">No results found for this intent search.</p>
              ) : (
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  {searchResults.map((turf) => (
                    <SearchResultCard key={turf.id} turf={turf} />
                  ))}
                </div>
              )}
            </div>
          )}
        </section>

        {loading ? (
          <div className="theme-panel flex flex-col items-center justify-center rounded-[24px] py-16">
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-sky-500 border-t-transparent" />
            <p className="theme-muted mt-4 text-sm">Synthesizing personalization vectors...</p>
          </div>
        ) : (
          <div className="space-y-9">
            <FeedSection
              title="Recommended For You"
              body="Pitches tailored to your price point, location, and slot history."
              empty="Book standard slots to generate personalized recommendations."
              turfs={feeds.FOR_YOU || []}
              label="98% Match Score"
            />
            <FeedSection
              title="Trending Nearby"
              body="High-occupancy and featured pitches generating peak bookings in your area."
              empty="No nearby active pitches matched."
              turfs={feeds.NEARBY || []}
              label="Hot Booking"
            />
            <FeedSection
              title="Weekend Suggestions"
              body="Top-rated slots for competitive practice, tournament prep, and squad matches."
              empty="No weekend slots parsed."
              turfs={feeds.WEEKEND || []}
              label="Weekend Choice"
            />
          </div>
        )}
      </div>
    </main>
  );
}

function FeedSection({
  title,
  body,
  empty,
  turfs,
  label,
}: {
  title: string;
  body: string;
  empty: string;
  turfs: Turf[];
  label: string;
}) {
  return (
    <section className="space-y-4">
      <div>
        <h2 className="theme-title text-2xl font-black tracking-tight">{title}</h2>
        <p className="theme-muted mt-1 text-sm">{body}</p>
      </div>
      <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {turfs.length === 0 && <EmptyFeeds text={empty} />}
        {turfs.map((turf) => (
          <TurfCard key={turf.id} turf={turf} label={label} />
        ))}
      </div>
    </section>
  );
}

function HeroBenefit({ title, body }: { title: string; body: string }) {
  return (
    <div className="theme-card rounded-2xl p-4">
      <p className="theme-title text-sm font-bold">{title}</p>
      <p className="theme-muted mt-1 text-xs leading-5">{body}</p>
    </div>
  );
}

function SearchResultCard({ turf }: { turf: Turf }) {
  return (
    <article className="theme-card-strong rounded-[20px] p-5 transition hover:-translate-y-0.5 hover:border-sky-400/40">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h4 className="theme-title font-bold">{turf.name}</h4>
          <p className="theme-muted mt-1 text-xs">{turf.location} / {turf.city}</p>
        </div>
        <span className="rounded-full bg-sky-500/10 px-2.5 py-1 text-xs font-bold text-sky-700 dark:text-sky-300">
          {turf.rating || "N/A"} rating
        </span>
      </div>
      <div className="mt-4 flex items-center justify-between border-t border-white/10 pt-3">
        <span className="text-sm font-bold text-emerald-700 dark:text-emerald-300">Rs {turf.pricePerHour}/hr</span>
        <Link href={`/turf/${turf.id}`} className="text-xs font-bold text-sky-700 hover:text-sky-500 dark:text-sky-300">
          Book Now
        </Link>
      </div>
    </article>
  );
}

function TurfCard({ turf, label }: { turf: Turf; label: string }) {
  return (
    <article className="theme-card-strong group rounded-[22px] p-6 shadow-sm transition hover:-translate-y-0.5 hover:border-emerald-400/40">
      <div className="flex items-center justify-between gap-2">
        <span className="rounded-full bg-emerald-500/10 px-2.5 py-1 text-xs font-bold text-emerald-700 dark:text-emerald-300">
          {label}
        </span>
        <span className="theme-muted text-xs font-medium">{turf.city}</span>
      </div>

      <h3 className="theme-title mt-4 text-xl font-black leading-snug transition group-hover:text-emerald-700 dark:group-hover:text-emerald-300">
        {turf.name}
      </h3>
      <p className="theme-muted mt-2 line-clamp-2 text-sm">
        {turf.description || "Premium sports facility with custom slots, box layout, and night match support."}
      </p>

      <div className="mt-6 flex items-center justify-between border-t border-white/10 pt-4">
        <div>
          <span className="theme-soft text-xs">Starting from</span>
          <p className="text-lg font-black text-emerald-700 dark:text-emerald-300">Rs {turf.pricePerHour}/hr</p>
        </div>
        <Link href={`/turf/${turf.id}`} className="theme-card rounded-xl px-4 py-2 text-xs font-bold theme-text hover:border-emerald-400/40">
          View Turf
        </Link>
      </div>
    </article>
  );
}

function EmptyFeeds({ text }: { text: string }) {
  return (
    <div className="theme-panel-soft col-span-full rounded-[18px] border border-dashed p-10 text-center text-sm theme-muted">
      {text}
    </div>
  );
}
