"use client";

import { type Dispatch, type SetStateAction, useCallback, useEffect, useMemo, useState } from "react";
import { getMyTeams } from "@/modules/social/services/socialService";
import { Team } from "@/modules/social/types/social.types";
import { getOwnerTurfs } from "@/modules/owner/services/ownerService";
import { Turf } from "@/modules/turf/types/turf.types";
import {
  addTournamentReward,
  approveTournamentRegistration,
  cancelTournamentRegistration,
  generateTournamentFixtures,
  getTournamentDashboard,
  recordFixtureResult,
  registerTeamForTournament,
  rejectTournamentRegistration,
  regenerateTournamentFixtures,
  scheduleTournamentFixture,
  updateTournamentStatus,
} from "../services/tournamentService";
import { TournamentDashboard, TournamentStatus } from "../types/tournament.types";
import {
  Empty,
  Hero,
  Panel,
  PrimaryLink,
  SecondaryLink,
  SmallButton,
  Stat,
  Surface,
  TournamentPageShell,
  errorMessage,
  formatMoney,
  formatTournamentDate,
  inputClass,
} from "./TournamentShell";

type ScoreDraftMap = Record<number, { a: string; b: string }>;
type ScheduleDraftMap = Record<number, { turfId: string; slotId: string; matchDate: string; startTime: string; scheduleNote: string }>;

export function TournamentDashboardClient({ tournamentId }: { tournamentId: number }) {
  const [dashboard, setDashboard] = useState<TournamentDashboard | null>(null);
  const [teams, setTeams] = useState<Team[]>([]);
  const [turfs, setTurfs] = useState<Turf[]>([]);
  const [selectedTeam, setSelectedTeam] = useState("");
  const [scores, setScores] = useState<ScoreDraftMap>({});
  const [scheduleDrafts, setScheduleDrafts] = useState<ScheduleDraftMap>({});
  const [rewardDraft, setRewardDraft] = useState({
    rewardType: "PRIZE" as "PRIZE" | "MEDAL" | "CERTIFICATE",
    title: "",
    description: "",
    position: 1,
    cashAmount: 0,
    winnerTeamId: "",
  });
  const [nextStatus, setNextStatus] = useState<TournamentStatus>("REGISTRATION_CLOSED");
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const dashboardPromise = getTournamentDashboard(tournamentId);
      const teamsPromise = getMyTeams();
      const turfsPromise = getOwnerTurfs().catch(() => ({ data: [] as Turf[] }));
      const [dashboardRes, teamRes, turfRes] = await Promise.all([dashboardPromise, teamsPromise, turfsPromise]);

      const nextDashboard = dashboardRes.data as TournamentDashboard;
      setDashboard(nextDashboard);
      setTeams(teamRes.data as Team[]);
      setTurfs(turfRes.data as Turf[]);
      setScheduleDrafts((current) => buildScheduleDrafts(nextDashboard, current));
    } catch (error) {
      setStatus(errorMessage(error, "Unable to load tournament dashboard"));
    } finally {
      setLoading(false);
    }
  }, [tournamentId]);

  useEffect(() => {
    const timer = window.setTimeout(() => void loadData(), 0);
    return () => window.clearTimeout(timer);
  }, [loadData]);

  const groupedFixtures = useMemo(() => {
    if (!dashboard) {
      return [];
    }

    const groups = new Map<string, TournamentDashboard["fixtures"]>();
    dashboard.fixtures.forEach((fixture) => {
      const key = fixture.stageLabel || `Round ${fixture.round}`;
      groups.set(key, [...(groups.get(key) || []), fixture]);
    });
    return Array.from(groups.entries());
  }, [dashboard]);

  return (
    <TournamentPageShell>
      <Hero
        eyebrow="EPIC 14 - Tournament Dashboard"
        title={dashboard?.tournament.name || "Tournament Dashboard"}
        description="Manage registrations, auto-advancing knockout brackets, fixture scheduling, results, standings, rewards, and tournament feed activity from a dedicated control room."
        action={
          <div className="flex flex-wrap gap-3">
            <SecondaryLink href="/tournaments" label="All Tournaments" />
            <PrimaryLink href="/tournaments/create" label="Create Another" />
          </div>
        }
        stats={
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <Stat label="Approved" value={dashboard?.approvedTeams || 0} />
            <Stat label="Pending" value={dashboard?.pendingTeams || 0} />
            <Stat label="Upcoming" value={dashboard?.upcomingFixtures || 0} />
            <Stat label="Revenue" value={formatMoney(dashboard?.registrationRevenue || 0)} />
          </div>
        }
      />

      {status ? <p className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-emerald-200">{status}</p> : null}
      {loading ? <p className="rounded-3xl border border-white/10 bg-white/[0.04] p-8 text-center text-slate-400">Loading tournament dashboard...</p> : null}

      {!loading && dashboard ? (
        <div className="space-y-6">
          <Surface title="Overview">
            <div className="grid gap-6 xl:grid-cols-[1.3fr_1fr]">
              <div className="rounded-3xl border border-white/10 bg-slate-950/60 p-5">
                <p className="text-sm uppercase tracking-[0.25em] text-sky-300/70">{dashboard.tournament.format}</p>
                <h2 className="mt-3 text-2xl font-black text-white">{dashboard.tournament.name}</h2>
                <p className="mt-3 text-sm text-slate-300">{dashboard.tournament.description || "No description set."}</p>
                <div className="mt-4 grid gap-3 sm:grid-cols-2">
                  <Meta label="Venue" value={dashboard.tournament.venue} />
                  <Meta label="Dates" value={formatTournamentDate(dashboard.tournament.startDate, dashboard.tournament.endDate)} />
                  <Meta label="Registration" value={dashboard.tournament.requiresApproval ? "Approval required" : "Instant"} />
                  <Meta label="Prize Pool" value={formatMoney(dashboard.tournament.prizePool)} />
                </div>
                {dashboard.tournament.rules ? (
                  <div className="mt-4 rounded-2xl border border-white/10 bg-white/[0.03] p-4">
                    <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">Rules</p>
                    <p className="mt-2 whitespace-pre-wrap text-sm text-slate-300">{dashboard.tournament.rules}</p>
                  </div>
                ) : null}
              </div>

              <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-1">
                <Stat label="Registered" value={`${dashboard.tournament.registeredTeams}/${dashboard.tournament.maxTeams}`} />
                <Stat label="Completed" value={dashboard.completedFixtures} />
                <Stat label="Waitlist" value={dashboard.waitlistTeams} />
                <Stat label="Status" value={dashboard.tournament.status} />
                <div className="rounded-2xl border border-white/10 bg-white/[0.03] p-4">
                  <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-slate-500">Lifecycle control</p>
                  <div className="mt-3 flex gap-2">
                    <select value={nextStatus} onChange={(event) => setNextStatus(event.target.value as TournamentStatus)} className={inputClass}>
                      <option value="REGISTRATION_OPEN">Open registration</option>
                      <option value="REGISTRATION_CLOSED">Close registration</option>
                      <option value="ONGOING">Start tournament</option>
                      <option value="COMPLETED">Complete tournament</option>
                      <option value="CANCELLED">Cancel tournament</option>
                    </select>
                    <SmallButton
                      label="Apply"
                      tone="primary"
                      onClick={() =>
                        updateTournamentStatus(tournamentId, nextStatus, "Updated from tournament dashboard")
                          .then(() => loadData())
                          .catch((error) => setStatus(errorMessage(error, "Status update failed")))
                      }
                    />
                  </div>
                </div>
              </div>
            </div>
          </Surface>

          <div className="grid gap-6 xl:grid-cols-2">
            <Panel title="Registrations">
              {dashboard.registrations.length === 0 ? <Empty text="No registrations yet." /> : null}
              {dashboard.registrations.map((registration) => (
                <div key={registration.id} className="rounded-2xl border border-white/10 bg-slate-950/60 p-4">
                  <p className="font-semibold text-white">{registration.teamName}</p>
                  <p className="mt-1 text-sm text-slate-400">
                    {registration.registeredByName} - {registration.status}
                  </p>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {registration.status === "PENDING" ? (
                      <>
                        <SmallButton
                          label="Approve"
                          tone="primary"
                          onClick={() =>
                            approveTournamentRegistration(tournamentId, registration.id)
                              .then(() => loadData())
                              .catch((error) => setStatus(errorMessage(error, "Approve failed")))
                          }
                        />
                        <SmallButton
                          label="Reject"
                          tone="danger"
                          onClick={() =>
                            rejectTournamentRegistration(tournamentId, registration.id)
                              .then(() => loadData())
                              .catch((error) => setStatus(errorMessage(error, "Reject failed")))
                          }
                        />
                      </>
                    ) : null}
                    {registration.status !== "CANCELLED" ? (
                      <SmallButton
                        label="Cancel"
                        tone="secondary"
                        onClick={() =>
                          cancelTournamentRegistration(tournamentId, registration.id)
                            .then(() => loadData())
                            .catch((error) => setStatus(errorMessage(error, "Cancel failed")))
                        }
                      />
                    ) : null}
                  </div>
                </div>
              ))}
            </Panel>

            <Panel title="Standings">
              {dashboard.standings.length === 0 ? <Empty text="No standings yet." /> : null}
              {dashboard.standings.map((row) => (
                <div key={row.teamId} className="flex items-center justify-between rounded-2xl border border-white/10 bg-slate-950/60 px-4 py-3 text-sm text-slate-200">
                  <span>{row.teamName}</span>
                  <span>
                    P {row.played} - W {row.wins} - D {row.draws || 0} - L {row.losses} - PTS {row.points} - NR {row.netRuns} - NRR {row.netRunRate ?? 0}
                  </span>
                </div>
              ))}
            </Panel>
          </div>

          <Panel
            title="Fixtures"
            action={
              <div className="flex gap-2">
                <SmallButton
                  label="Generate"
                  tone="primary"
                  onClick={() => generateTournamentFixtures(tournamentId).then(() => loadData()).catch((error) => setStatus(errorMessage(error, "Generate failed")))}
                />
                <SmallButton
                  label="Regenerate"
                  tone="secondary"
                  onClick={() => regenerateTournamentFixtures(tournamentId).then(() => loadData()).catch((error) => setStatus(errorMessage(error, "Regenerate failed")))}
                />
              </div>
            }
          >
            {groupedFixtures.length === 0 ? <Empty text="No fixtures yet." /> : null}
            {groupedFixtures.map(([label, fixtures]) => (
              <div key={label} className="space-y-3">
                <div className="flex items-center justify-between">
                  <h4 className="text-sm font-bold uppercase tracking-[0.24em] text-sky-300/80">{label}</h4>
                  <span className="text-xs text-slate-500">{fixtures.length} fixture(s)</span>
                </div>
                {fixtures.map((fixture) => (
                  <div key={fixture.id} className="rounded-2xl border border-white/10 bg-slate-950/60 p-4">
                    <div className="flex flex-col gap-3 xl:flex-row xl:items-start xl:justify-between">
                      <div>
                        <p className="font-semibold text-white">
                          {fixture.teamAName} vs {fixture.teamBName}
                        </p>
                        <p className="mt-1 text-sm text-slate-400">
                          {fixture.stageLabel} - {fixture.status} - {fixture.matchDate || "Date pending"} {fixture.startTime || ""}
                        </p>
                        <p className="mt-1 text-sm text-slate-500">
                          {fixture.turfName || "Turf not assigned"} {fixture.slotId ? `- Slot ${fixture.slotId}` : ""}
                        </p>
                        {fixture.scheduleNote ? <p className="mt-2 text-sm text-slate-400">{fixture.scheduleNote}</p> : null}
                      </div>

                      {fixture.status !== "COMPLETED" ? (
                        <div className="grid gap-2 sm:grid-cols-3 xl:w-[360px]">
                          <input
                            type="number"
                            placeholder={`${fixture.teamAName} score`}
                            value={scores[fixture.id]?.a || ""}
                            onChange={(e) => setScores({ ...scores, [fixture.id]: { ...scores[fixture.id], a: e.target.value } })}
                            className={inputClass}
                          />
                          <input
                            type="number"
                            placeholder={`${fixture.teamBName} score`}
                            value={scores[fixture.id]?.b || ""}
                            onChange={(e) => setScores({ ...scores, [fixture.id]: { ...scores[fixture.id], b: e.target.value } })}
                            className={inputClass}
                          />
                          <SmallButton
                            label="Save Result"
                            tone="primary"
                            onClick={() =>
                              recordFixtureResult(fixture.id, Number(scores[fixture.id]?.a || 0), Number(scores[fixture.id]?.b || 0))
                                .then(() => loadData())
                                .catch((error) => setStatus(errorMessage(error, "Result save failed")))
                            }
                          />
                        </div>
                      ) : (
                        <p className="text-sm font-semibold text-emerald-300">
                          {fixture.winnerTeamName ? `Winner: ${fixture.winnerTeamName}` : "Draw"} ({fixture.teamAScore}-{fixture.teamBScore})
                        </p>
                      )}
                    </div>

                    <div className="mt-4 grid gap-3 rounded-2xl border border-white/10 bg-white/[0.02] p-4 md:grid-cols-2 xl:grid-cols-5">
                      <select
                        value={scheduleDrafts[fixture.id]?.turfId || ""}
                        onChange={(e) => updateScheduleDraft(fixture.id, "turfId", e.target.value, setScheduleDrafts)}
                        className={inputClass}
                      >
                        <option value="">Assign turf</option>
                        {turfs.map((turf) => (
                          <option key={turf.id} value={turf.id}>
                            {turf.name}
                          </option>
                        ))}
                      </select>
                      <input
                        type="number"
                        placeholder="Slot ID"
                        value={scheduleDrafts[fixture.id]?.slotId || ""}
                        onChange={(e) => updateScheduleDraft(fixture.id, "slotId", e.target.value, setScheduleDrafts)}
                        className={inputClass}
                      />
                      <input
                        type="date"
                        value={scheduleDrafts[fixture.id]?.matchDate || ""}
                        onChange={(e) => updateScheduleDraft(fixture.id, "matchDate", e.target.value, setScheduleDrafts)}
                        className={inputClass}
                      />
                      <input
                        type="time"
                        value={scheduleDrafts[fixture.id]?.startTime || ""}
                        onChange={(e) => updateScheduleDraft(fixture.id, "startTime", e.target.value, setScheduleDrafts)}
                        className={inputClass}
                      />
                      <SmallButton
                        label="Save Schedule"
                        tone="secondary"
                        onClick={() =>
                          scheduleTournamentFixture(fixture.id, {
                            turfId: parseOptionalNumber(scheduleDrafts[fixture.id]?.turfId),
                            slotId: parseOptionalNumber(scheduleDrafts[fixture.id]?.slotId),
                            matchDate: scheduleDrafts[fixture.id]?.matchDate || undefined,
                            startTime: scheduleDrafts[fixture.id]?.startTime || undefined,
                            scheduleNote: scheduleDrafts[fixture.id]?.scheduleNote || undefined,
                          })
                            .then(() => loadData())
                            .catch((error) => setStatus(errorMessage(error, "Schedule save failed")))
                        }
                      />
                      <textarea
                        value={scheduleDrafts[fixture.id]?.scheduleNote || ""}
                        onChange={(e) => updateScheduleDraft(fixture.id, "scheduleNote", e.target.value, setScheduleDrafts)}
                        placeholder="Schedule note"
                        className={`${inputClass} min-h-[96px] md:col-span-2 xl:col-span-5`}
                      />
                    </div>
                  </div>
                ))}
              </div>
            ))}
          </Panel>

          <div className="grid gap-6 xl:grid-cols-2">
            <Panel title="Rewards & Feed">
              <div className="rounded-2xl border border-white/10 bg-slate-950/60 p-4">
                <div className="grid gap-3 md:grid-cols-2">
                  <input value={rewardDraft.title} onChange={(e) => setRewardDraft({ ...rewardDraft, title: e.target.value })} placeholder="Reward title" className={inputClass} />
                  <select value={rewardDraft.rewardType} onChange={(e) => setRewardDraft({ ...rewardDraft, rewardType: e.target.value as typeof rewardDraft.rewardType })} className={inputClass}>
                    <option value="PRIZE">Prize</option>
                    <option value="MEDAL">Medal</option>
                    <option value="CERTIFICATE">Certificate</option>
                  </select>
                  <input type="number" min={1} value={rewardDraft.position} onChange={(e) => setRewardDraft({ ...rewardDraft, position: Number(e.target.value) })} placeholder="Position" className={inputClass} />
                  <input type="number" min={0} value={rewardDraft.cashAmount} onChange={(e) => setRewardDraft({ ...rewardDraft, cashAmount: Number(e.target.value) })} placeholder="Cash amount" className={inputClass} />
                  <select value={rewardDraft.winnerTeamId} onChange={(e) => setRewardDraft({ ...rewardDraft, winnerTeamId: e.target.value })} className={`${inputClass} md:col-span-2`}>
                    <option value="">Winner team optional</option>
                    {dashboard.standings.map((row) => (
                      <option key={row.teamId} value={row.teamId}>
                        {row.teamName}
                      </option>
                    ))}
                  </select>
                  <textarea value={rewardDraft.description} onChange={(e) => setRewardDraft({ ...rewardDraft, description: e.target.value })} placeholder="Reward note" className={`${inputClass} min-h-[90px] md:col-span-2`} />
                </div>
                <button
                  type="button"
                  onClick={() =>
                    addTournamentReward(tournamentId, {
                      ...rewardDraft,
                      winnerTeamId: rewardDraft.winnerTeamId ? Number(rewardDraft.winnerTeamId) : undefined,
                    })
                      .then(() => loadData())
                      .catch((error) => setStatus(errorMessage(error, "Reward save failed")))
                  }
                  className="mt-4 rounded-2xl bg-sky-500 px-4 py-2 text-sm font-bold text-white hover:bg-sky-400"
                >
                  Add Reward
                </button>
              </div>

              {dashboard.rewards.length === 0 && dashboard.feed.length === 0 ? <Empty text="No rewards or feed activity yet." /> : null}
              {dashboard.rewards.map((reward) => (
                <div key={reward.id} className="rounded-2xl border border-white/10 bg-slate-950/60 p-4 text-sm text-slate-200">
                  {reward.title} - {reward.rewardType} - Position {reward.position}
                  {reward.winnerTeamName ? ` - ${reward.winnerTeamName}` : ""}
                </div>
              ))}
              {dashboard.feed.map((item) => (
                <div key={item.id} className="rounded-2xl border border-white/10 bg-slate-950/60 p-4">
                  <p className="font-semibold text-white">{item.title}</p>
                  <p className="mt-1 text-sm text-slate-400">{item.summary}</p>
                </div>
              ))}
            </Panel>

            <Panel title="Register Team">
              <div className="rounded-2xl border border-white/10 bg-slate-950/60 p-4">
                <div className="flex flex-wrap gap-3">
                  <select value={selectedTeam} onChange={(e) => setSelectedTeam(e.target.value)} className="min-w-[240px] rounded-2xl border border-white/10 bg-slate-900/90 px-4 py-3 text-sm text-white outline-none">
                    <option value="">Select team</option>
                    {teams.map((team) => (
                      <option key={team.id} value={team.id}>
                        {team.name}
                      </option>
                    ))}
                  </select>
                  <button
                    type="button"
                    onClick={() => {
                      if (!selectedTeam) {
                        return;
                      }
                      registerTeamForTournament(tournamentId, Number(selectedTeam))
                        .then(() => loadData())
                        .catch((error) => setStatus(errorMessage(error, "Registration failed")));
                    }}
                    className="rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-bold text-white hover:bg-emerald-400"
                  >
                    Register Team
                  </button>
                </div>
              </div>
            </Panel>
          </div>
        </div>
      ) : null}
    </TournamentPageShell>
  );
}

function Meta({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/[0.03] p-4">
      <p className="text-[11px] font-semibold uppercase tracking-[0.25em] text-slate-500">{label}</p>
      <p className="mt-2 text-sm text-slate-200">{value}</p>
    </div>
  );
}

function buildScheduleDrafts(dashboard: TournamentDashboard, current: ScheduleDraftMap) {
  return dashboard.fixtures.reduce<ScheduleDraftMap>((accumulator, fixture) => {
    accumulator[fixture.id] = current[fixture.id] || {
      turfId: fixture.turfId ? String(fixture.turfId) : "",
      slotId: fixture.slotId ? String(fixture.slotId) : "",
      matchDate: fixture.matchDate || "",
      startTime: fixture.startTime || "",
      scheduleNote: fixture.scheduleNote || "",
    };
    return accumulator;
  }, {});
}

function updateScheduleDraft(
  fixtureId: number,
  key: keyof ScheduleDraftMap[number],
  value: string,
  setScheduleDrafts: Dispatch<SetStateAction<ScheduleDraftMap>>,
) {
  setScheduleDrafts((current) => ({
    ...current,
    [fixtureId]: {
      ...current[fixtureId],
      [key]: value,
    },
  }));
}

function parseOptionalNumber(value?: string) {
  if (!value) {
    return undefined;
  }
  const parsed = Number(value);
  return Number.isNaN(parsed) ? undefined : parsed;
}
