"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getOwnerTurfs } from "@/modules/owner/services/ownerService";
import { Turf } from "@/modules/turf/types/turf.types";
import { createTournament } from "../services/tournamentService";
import { CreateTournamentPayload, TournamentFormat, TournamentStatus } from "../types/tournament.types";
import {
  Field,
  Hero,
  PrimaryLink,
  SecondaryLink,
  Surface,
  TournamentPageShell,
  errorMessage,
  inputClass,
} from "./TournamentShell";

const emptyForm: CreateTournamentPayload = {
  name: "",
  sport: "Cricket",
  city: "",
  venue: "",
  format: "LEAGUE",
  status: "REGISTRATION_OPEN",
  maxTeams: 8,
  entryFee: 0,
  prizePool: 0,
  startDate: "",
  endDate: "",
  description: "",
  bannerUrl: "",
  rules: "",
  requiresApproval: true,
};

export function TournamentCreateForm() {
  const router = useRouter();
  const [form, setForm] = useState<CreateTournamentPayload>(emptyForm);
  const [turfs, setTurfs] = useState<Turf[]>([]);
  const [status, setStatus] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let active = true;

    getOwnerTurfs()
      .then((response) => {
        if (active) {
          setTurfs(response.data as Turf[]);
        }
      })
      .catch(() => {
        if (active) {
          setTurfs([]);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  const updateFormat = (value: string) => {
    setForm((current) => ({ ...current, format: value as TournamentFormat }));
  };

  const updateStatus = (value: string) => {
    setForm((current) => ({ ...current, status: value as TournamentStatus }));
  };

  const applyTurf = (turfId: string) => {
    if (!turfId) {
      setForm((current) => ({ ...current, turfId: undefined }));
      return;
    }

    const turf = turfs.find((item) => item.id === Number(turfId));
    setForm((current) => ({
      ...current,
      turfId: Number(turfId),
      city: turf?.city || current.city,
      venue: turf?.location || current.venue,
    }));
  };

  return (
    <TournamentPageShell>
      <Hero
        eyebrow="EPIC 14 - Tournament Creation"
        title="Launch a tournament with registration, fixtures, standings, and rewards already wired in."
        description="Set the format, team cap, fees, rules, and dates. Knockout and hybrid tournaments now expect power-of-two team counts so the bracket can auto-advance cleanly."
        action={
          <div className="flex flex-wrap gap-3">
            <SecondaryLink href="/tournaments" label="Back to Tournaments" />
            <PrimaryLink href="/owner/dashboard" label="Owner Dashboard" />
          </div>
        }
      />

      {status ? <p className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-emerald-200">{status}</p> : null}

      <Surface title="Create Tournament">
        <div className="grid gap-4 lg:grid-cols-2">
          <Field label="Tournament name">
            <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} className={inputClass} />
          </Field>

          <Field label="Linked turf">
            <select value={form.turfId || ""} onChange={(e) => applyTurf(e.target.value)} className={inputClass}>
              <option value="">Select turf later</option>
              {turfs.map((turf) => (
                <option key={turf.id} value={turf.id}>
                  {turf.name}
                </option>
              ))}
            </select>
          </Field>

          <Field label="City">
            <input value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} className={inputClass} />
          </Field>

          <Field label="Venue">
            <input value={form.venue} onChange={(e) => setForm({ ...form, venue: e.target.value })} className={inputClass} />
          </Field>

          <Field label="Format">
            <select value={form.format} onChange={(e) => updateFormat(e.target.value)} className={inputClass}>
              <option value="LEAGUE">League</option>
              <option value="KNOCKOUT">Knockout</option>
              <option value="GROUP_STAGE">Group Stage</option>
              <option value="HYBRID">Hybrid</option>
            </select>
          </Field>

          <Field label="Requires approval">
            <select value={form.requiresApproval ? "YES" : "NO"} onChange={(e) => setForm({ ...form, requiresApproval: e.target.value === "YES" })} className={inputClass}>
              <option value="YES">Yes</option>
              <option value="NO">No</option>
            </select>
          </Field>

          <Field label="Initial status">
            <select value={form.status || "REGISTRATION_OPEN"} onChange={(e) => updateStatus(e.target.value)} className={inputClass}>
              <option value="DRAFT">Draft</option>
              <option value="UPCOMING">Upcoming</option>
              <option value="REGISTRATION_OPEN">Registration open</option>
            </select>
          </Field>

          <Field label="Max teams">
            <input type="number" min={2} value={form.maxTeams} onChange={(e) => setForm({ ...form, maxTeams: Number(e.target.value) })} className={inputClass} />
          </Field>

          <Field label="Entry fee">
            <input type="number" min={0} value={form.entryFee} onChange={(e) => setForm({ ...form, entryFee: Number(e.target.value) })} className={inputClass} />
          </Field>

          <Field label="Prize pool">
            <input type="number" min={0} value={form.prizePool || 0} onChange={(e) => setForm({ ...form, prizePool: Number(e.target.value) })} className={inputClass} />
          </Field>

          <Field label="Banner URL">
            <input value={form.bannerUrl || ""} onChange={(e) => setForm({ ...form, bannerUrl: e.target.value })} className={inputClass} />
          </Field>

          <Field label="Start date">
            <input type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} className={inputClass} />
          </Field>

          <Field label="End date">
            <input type="date" value={form.endDate || ""} onChange={(e) => setForm({ ...form, endDate: e.target.value })} className={inputClass} />
          </Field>
        </div>

        <Field label="Rules">
          <textarea value={form.rules || ""} onChange={(e) => setForm({ ...form, rules: e.target.value })} className={`${inputClass} min-h-[120px]`} />
        </Field>

        <Field label="Description">
          <textarea value={form.description || ""} onChange={(e) => setForm({ ...form, description: e.target.value })} className={`${inputClass} min-h-[140px]`} />
        </Field>

        <button
          type="button"
          disabled={submitting}
          onClick={async () => {
            setSubmitting(true);
            try {
              const response = await createTournament(form);
              const tournament = response.data as { id: number };
              router.push(`/tournaments/${tournament.id}`);
            } catch (error) {
              setStatus(errorMessage(error, "Tournament creation failed"));
            } finally {
              setSubmitting(false);
            }
          }}
          className="w-full rounded-2xl bg-sky-500 px-4 py-3 text-sm font-bold text-white hover:bg-sky-400 disabled:opacity-60"
        >
          {submitting ? "Saving..." : form.status === "DRAFT" ? "Save Draft Tournament" : "Publish Tournament"}
        </button>
      </Surface>
    </TournamentPageShell>
  );
}
