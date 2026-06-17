"use client";

import { useEffect, useState } from "react";
import {
  Empty,
  Hero,
  PrimaryLink,
  SecondaryLink,
  Stat,
  Surface,
  TournamentCard,
  TournamentPageShell,
  errorMessage,
  formatMoney,
  inputClass,
} from "./TournamentShell";
import { getMyTournaments, getOpenTournaments } from "../services/tournamentService";
import { Tournament, TournamentFormat, TournamentStatus } from "../types/tournament.types";

export function TournamentHub() {
  const [openTournaments, setOpenTournaments] = useState<Tournament[]>([]);
  const [myTournaments, setMyTournaments] = useState<Tournament[]>([]);
  const [filters, setFilters] = useState({ query: "", city: "", format: "" as TournamentFormat | "", status: "" as TournamentStatus | "", maxEntryFee: "" });
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);

  const loadTournaments = async (currentFilters = filters, active = true) => {
    setLoading(true);
    try {
      const [openRes, myRes] = await Promise.all([
        getOpenTournaments({
          query: currentFilters.query || undefined,
          city: currentFilters.city || undefined,
          format: currentFilters.format || undefined,
          status: currentFilters.status || undefined,
          maxEntryFee: currentFilters.maxEntryFee ? Number(currentFilters.maxEntryFee) : undefined,
        }),
        getMyTournaments(),
      ]);
      if (!active) {
        return;
      }
      setOpenTournaments(openRes.data as Tournament[]);
      setMyTournaments(myRes.data as Tournament[]);
    } catch (error) {
      if (active) {
        setStatus(errorMessage(error, "Unable to load tournaments"));
      }
    } finally {
      if (active) {
        setLoading(false);
      }
    }
  };

  useEffect(() => {
    let active = true;

    const loadInitial = async () => {
      setLoading(true);
      try {
        const [openRes, myRes] = await Promise.all([getOpenTournaments(), getMyTournaments()]);
        if (!active) {
          return;
        }
        setOpenTournaments(openRes.data as Tournament[]);
        setMyTournaments(myRes.data as Tournament[]);
      } catch (error) {
        if (active) {
          setStatus(errorMessage(error, "Unable to load tournaments"));
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    void loadInitial();

    return () => {
      active = false;
    };
  }, []);

  const activeRevenue = myTournaments.reduce((sum, item) => sum + (item.entryFee || 0) * (item.registeredTeams || 0), 0);

  return (
    <TournamentPageShell>
      <Hero
        eyebrow="EPIC 14 - Tournament Management"
        title="Turn turf bookings into a recurring tournament ecosystem."
        description="Browse live competitions, launch a new tournament, and drill into registrations, fixtures, standings, rewards, and feed activity from dedicated tournament workspaces."
        action={
          <div className="flex flex-wrap gap-3">
            <PrimaryLink href="/tournaments/create" label="Create Tournament" />
            <SecondaryLink href="/teams" label="Manage Teams" />
          </div>
        }
        stats={
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <Stat label="Open" value={openTournaments.length} />
            <Stat label="Mine" value={myTournaments.length} />
            <Stat label="Spots" value={openTournaments.reduce((sum, item) => sum + Math.max(item.maxTeams - item.registeredTeams, 0), 0)} />
            <Stat label="Revenue" value={formatMoney(activeRevenue)} />
          </div>
        }
      />

      {status ? <p className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-emerald-200">{status}</p> : null}
      <Surface title="Find tournaments">
        <div className="grid gap-3 md:grid-cols-5">
          <input value={filters.query} onChange={(event) => setFilters({ ...filters, query: event.target.value })} placeholder="Name or organizer" className={inputClass} />
          <input value={filters.city} onChange={(event) => setFilters({ ...filters, city: event.target.value })} placeholder="City" className={inputClass} />
          <select value={filters.format} onChange={(event) => setFilters({ ...filters, format: event.target.value as TournamentFormat | "" })} className={inputClass}>
            <option value="">Any format</option>
            <option value="LEAGUE">League</option>
            <option value="KNOCKOUT">Knockout</option>
            <option value="GROUP_STAGE">Group Stage</option>
            <option value="HYBRID">Hybrid</option>
          </select>
          <select value={filters.status} onChange={(event) => setFilters({ ...filters, status: event.target.value as TournamentStatus | "" })} className={inputClass}>
            <option value="">Any status</option>
            <option value="REGISTRATION_OPEN">Registration open</option>
            <option value="REGISTRATION_CLOSED">Registration closed</option>
            <option value="ONGOING">Ongoing</option>
            <option value="COMPLETED">Completed</option>
          </select>
          <input value={filters.maxEntryFee} onChange={(event) => setFilters({ ...filters, maxEntryFee: event.target.value })} placeholder="Max entry fee" type="number" min={0} className={inputClass} />
        </div>
        <div className="mt-3 flex flex-wrap gap-2">
          <button type="button" onClick={() => void loadTournaments(filters)} className="rounded-xl border border-sky-400/30 bg-sky-500/15 px-4 py-2 text-sm font-semibold text-sky-200">Apply filters</button>
          <button
            type="button"
            onClick={() => {
              setFilters({ query: "", city: "", format: "", status: "", maxEntryFee: "" });
              void getOpenTournaments().then((openRes) => setOpenTournaments(openRes.data as Tournament[])).catch((error) => setStatus(errorMessage(error, "Unable to reset tournaments")));
            }}
            className="rounded-xl border border-white/10 bg-white/[0.03] px-4 py-2 text-sm font-semibold text-slate-200"
          >
            Clear
          </button>
        </div>
      </Surface>
      {loading ? <p className="rounded-3xl border border-white/10 bg-white/[0.04] p-8 text-center text-slate-400">Loading tournament workspace...</p> : null}

      {!loading ? (
        <div className="grid gap-6 xl:grid-cols-2">
          <Surface title="Open Tournaments" action={<SecondaryLink href="/tournaments/create" label="Owner Console" />}>
            {openTournaments.length === 0 ? <Empty text="No tournaments published yet." /> : null}
            <div className="grid gap-3">
              {openTournaments.map((item) => (
                <TournamentCard
                  key={item.id}
                  href={`/tournaments/${item.id}`}
                  title={item.name}
                  meta={`${item.city} - ${item.format} - ${item.registeredTeams}/${item.maxTeams} teams`}
                  description={item.description}
                />
              ))}
            </div>
          </Surface>

          <Surface title="My Tournaments" action={<PrimaryLink href="/tournaments/create" label="New Tournament" />}>
            {myTournaments.length === 0 ? <Empty text="You have not created a tournament yet." /> : null}
            <div className="grid gap-3">
              {myTournaments.map((item) => (
                <TournamentCard
                  key={item.id}
                  href={`/tournaments/${item.id}`}
                  title={item.name}
                  meta={`${item.status} - ${item.registeredTeams}/${item.maxTeams} teams - ${formatMoney((item.entryFee || 0) * (item.registeredTeams || 0))}`}
                  description={item.description}
                />
              ))}
            </div>
          </Surface>
        </div>
      ) : null}
    </TournamentPageShell>
  );
}
