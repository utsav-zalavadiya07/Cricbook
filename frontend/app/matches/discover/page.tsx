"use client";

import { FormEvent, useEffect, useState } from "react";
import {
  createMatchmakingRequest,
  getMatchRecommendations,
  getOpenMatchmaking,
  joinMatchmakingMatch,
  MatchmakingMatch,
} from "@/modules/matchmaking/services/matchmakingService";
import { useToast } from "@/modules/ui/ToastProvider";

const skillLevels = ["Beginner", "Intermediate", "Advanced", "Professional"];

export default function MatchDiscoverPage() {
  const { toast } = useToast();
  const [matches, setMatches] = useState<MatchmakingMatch[]>([]);
  const [recommendations, setRecommendations] = useState<MatchmakingMatch[]>([]);
  const [city, setCity] = useState(() => (typeof window === "undefined" ? "Ahmedabad" : localStorage.getItem("selectedCity") || "Ahmedabad"));
  const [skillLevel, setSkillLevel] = useState("Intermediate");
  const [trustedOnly, setTrustedOnly] = useState(false);
  const [loading, setLoading] = useState(true);
  const [draft, setDraft] = useState({
    title: "Evening box cricket open match",
    location: "Community Turf",
    matchDate: new Date().toISOString().slice(0, 10),
    startTime: "20:00",
    endTime: "22:00",
    maxPlayers: 14,
    requiredPlayers: 2,
  });

  async function load() {
    setLoading(true);
    try {
      const [openRes, recommendationRes] = await Promise.all([getOpenMatchmaking({ city, skillLevel, trustedOnly }), getMatchRecommendations()]);
      setMatches(openRes.data || []);
      setRecommendations(recommendationRes.data || []);
    } catch (error: unknown) {
      toast(apiMessage(error, "Could not load matchmaking."), "error");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const createMatch = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await createMatchmakingRequest({
        title: draft.title,
        sport: "Cricket",
        city,
        location: draft.location,
        matchDate: draft.matchDate,
        startTime: draft.startTime,
        endTime: draft.endTime,
        maxPlayers: Number(draft.maxPlayers),
        requiredPlayers: Number(draft.requiredPlayers),
        skillLevel,
        trustedOnly,
        autoApprove: !trustedOnly,
        visibility: "PUBLIC",
        format: "BOX_CRICKET",
      });
      toast("Open match created and candidates ranked.", "success");
      await load();
    } catch (error: unknown) {
      toast(apiMessage(error, "Could not create match."), "error");
    }
  };

  const join = async (matchId: number) => {
    try {
      await joinMatchmakingMatch(matchId);
      toast("Joined match. Match chat is now available.", "success");
      await load();
    } catch (error: unknown) {
      toast(apiMessage(error, "Could not join match."), "error");
    }
  };

  return (
    <main className="theme-page-player min-h-screen px-4 py-8 sm:px-6 lg:px-10">
      <div className="mx-auto max-w-7xl space-y-8">
        <section className="theme-panel-strong rounded-[2rem] p-6 sm:p-8">
          <p className="theme-accent text-xs font-black uppercase tracking-[0.34em]">EPIC 24 · Smart Matchmaking</p>
          <div className="mt-3 grid gap-8 lg:grid-cols-[1.1fr_0.9fr] lg:items-end">
            <div>
              <h1 className="theme-title text-4xl font-black tracking-tight sm:text-6xl">Fill your match without WhatsApp spam.</h1>
              <p className="theme-muted mt-4 max-w-3xl text-base leading-7">
                Create open cricket matches, rank nearby available players, filter by trust score, and let players join instantly.
              </p>
            </div>
            <div className="theme-card-strong rounded-3xl p-4">
              <div className="grid gap-3 sm:grid-cols-3">
                <Field label="City" value={city} onChange={setCity} />
                <label>
                  <span className="theme-title text-xs font-black uppercase tracking-wider">Skill</span>
                  <select value={skillLevel} onChange={(event) => setSkillLevel(event.target.value)} className="theme-input mt-2 w-full rounded-xl px-3 py-2 text-sm outline-none">
                    {skillLevels.map((item) => <option key={item}>{item}</option>)}
                  </select>
                </label>
                <button onClick={load} className="mt-6 rounded-xl bg-emerald-500 px-4 py-2 text-sm font-black text-white">Refresh</button>
              </div>
              <label className="theme-muted mt-3 flex items-center gap-2 text-sm">
                <input type="checkbox" checked={trustedOnly} onChange={(event) => setTrustedOnly(event.target.checked)} />
                Trusted players only
              </label>
            </div>
          </div>
        </section>

        <section className="grid gap-6 xl:grid-cols-[0.8fr_1.2fr]">
          <form onSubmit={createMatch} className="theme-panel rounded-3xl p-6">
            <h2 className="theme-title text-xl font-black">Create Open Match</h2>
            <p className="theme-muted mt-1 text-sm">Captain creates the request; Cricbook ranks the fill candidates.</p>
            <div className="mt-5 grid gap-4">
              <Field label="Title" value={draft.title} onChange={(value) => setDraft((current) => ({ ...current, title: value }))} />
              <Field label="Location" value={draft.location} onChange={(value) => setDraft((current) => ({ ...current, location: value }))} />
              <div className="grid gap-4 sm:grid-cols-2">
                <Field label="Date" value={draft.matchDate} onChange={(value) => setDraft((current) => ({ ...current, matchDate: value }))} type="date" />
                <Field label="Start Time" value={draft.startTime} onChange={(value) => setDraft((current) => ({ ...current, startTime: value }))} type="time" />
                <Field label="Max Players" value={String(draft.maxPlayers)} onChange={(value) => setDraft((current) => ({ ...current, maxPlayers: Number(value) }))} type="number" />
                <Field label="Need Players" value={String(draft.requiredPlayers)} onChange={(value) => setDraft((current) => ({ ...current, requiredPlayers: Number(value) }))} type="number" />
              </div>
            </div>
            <button className="mt-5 w-full rounded-2xl bg-emerald-500 px-5 py-3 text-sm font-black text-white shadow-lg shadow-emerald-500/20">Create & Find Players</button>
          </form>

          <div className="space-y-6">
            <MatchList title="Recommended For You" loading={loading} matches={recommendations} onJoin={join} />
            <MatchList title="Open Matches Nearby" loading={loading} matches={matches} onJoin={join} />
          </div>
        </section>
      </div>
    </main>
  );
}

function apiMessage(error: unknown, fallback: string) {
  const response = (error as { response?: { data?: { message?: string } } } | null)?.response;
  return response?.data?.message || fallback;
}

function MatchList({ title, loading, matches, onJoin }: { title: string; loading: boolean; matches: MatchmakingMatch[]; onJoin: (id: number) => void }) {
  return (
    <section className="theme-panel rounded-3xl p-6">
      <h2 className="theme-title text-xl font-black">{title}</h2>
      <div className="mt-5 grid gap-4 lg:grid-cols-2">
        {loading && <p className="theme-muted col-span-full py-8 text-center">Loading matches...</p>}
        {!loading && matches.length === 0 && <p className="theme-card theme-muted col-span-full rounded-2xl border-dashed p-8 text-center">No matches found.</p>}
        {matches.map((match) => (
          <article key={match.id} className="theme-panel-soft rounded-2xl p-4">
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="theme-title font-black">{match.title}</p>
                <p className="theme-muted mt-1 text-sm">{match.city} · {match.matchDate} · {match.startTime}</p>
              </div>
              <span className="theme-badge rounded-full px-3 py-1 text-xs font-black">{match.matchScore}%</span>
            </div>
            <p className="theme-muted mt-3 text-sm">{match.recommendationReason}</p>
            <div className="mt-4 flex flex-wrap gap-2 text-xs font-bold">
              <span className="theme-card rounded-full px-3 py-1">{match.slotsLeft} slots left</span>
              <span className="theme-card rounded-full px-3 py-1">{match.skillLevel || "Any skill"}</span>
              {match.trustedOnly && <span className="theme-card rounded-full px-3 py-1">Trusted only</span>}
            </div>
            {match.candidates?.length > 0 && (
              <div className="mt-4 space-y-2">
                {match.candidates.slice(0, 3).map((candidate) => (
                  <p key={candidate.userId} className="theme-card rounded-xl px-3 py-2 text-xs">
                    {candidate.name} · Trust {candidate.trustScore} · {candidate.skillLevel}
                  </p>
                ))}
              </div>
            )}
            <button onClick={() => onJoin(match.id)} className="mt-4 w-full rounded-xl bg-emerald-500 px-4 py-2 text-sm font-black text-white">Quick Join</button>
          </article>
        ))}
      </div>
    </section>
  );
}

function Field({ label, value, onChange, type = "text" }: { label: string; value: string; onChange: (value: string) => void; type?: string }) {
  return (
    <label>
      <span className="theme-title text-xs font-black uppercase tracking-wider">{label}</span>
      <input type={type} value={value} onChange={(event) => onChange(event.target.value)} className="theme-input mt-2 w-full rounded-xl px-3 py-2 text-sm outline-none" />
    </label>
  );
}
