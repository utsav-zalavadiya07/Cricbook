"use client";

import { ReactNode, useEffect, useState } from "react";
import {
  acceptMatchChallenge,
  applyRecruitment,
  cancelPublicMatch,
  connectPlayer,
  createRecruitment,
  createPublicMatch,
  discoverPlayers,
  getCommunityAnalytics,
  getMatchMessages,
  getMyMatchHistory,
  getMyNetwork,
  getMyTeams,
  getNearbyPlayers,
  getOpenMatches,
  getOpenRecruitments,
  getSocialFeed,
  getTeamMatchStats,
  getTeamMessages,
  joinPublicMatch,
  rejectMatchChallenge,
  schedulePublicMatch,
  sendMatchMessage,
  sendTeamMessage,
  startPublicMatch,
  submitMatchResult,
} from "@/modules/social/services/socialService";
import {
  ChatMessage,
  CommunityAnalytics,
  FeedEvent,
  MatchFormat,
  MatchVisibility,
  PlayerConnection,
  PlayerDiscover,
  PublicMatch,
  Team,
  TeamMatchStats,
  TeamRecruitment,
} from "@/modules/social/types/social.types";

type ChatRoom = { type: "TEAM"; id: number; title: string } | { type: "MATCH"; id: number; title: string };

const emptyMatchForm = {
  title: "",
  city: "",
  location: "",
  matchDate: "",
  startTime: "",
  endTime: "",
  maxPlayers: 14,
  visibility: "PUBLIC" as MatchVisibility,
  format: "BOX_CRICKET" as MatchFormat,
  overs: 10,
  entryFee: 0,
  homeTeamId: "",
  awayTeamId: "",
  challengeMessage: "",
  notes: "",
};

export default function SocialPage() {
  const [teams, setTeams] = useState<Team[]>([]);
  const [matches, setMatches] = useState<PublicMatch[]>([]);
  const [history, setHistory] = useState<PublicMatch[]>([]);
  const [players, setPlayers] = useState<PlayerDiscover[]>([]);
  const [nearbyPlayers, setNearbyPlayers] = useState<PlayerDiscover[]>([]);
  const [network, setNetwork] = useState<PlayerConnection[]>([]);
  const [recruitments, setRecruitments] = useState<TeamRecruitment[]>([]);
  const [analytics, setAnalytics] = useState<CommunityAnalytics | null>(null);
  const [feed, setFeed] = useState<FeedEvent[]>([]);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [room, setRoom] = useState<ChatRoom | null>(null);
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState("");
  const [message, setMessage] = useState("");
  const [matchForm, setMatchForm] = useState(emptyMatchForm);
  const [matchResultDrafts, setMatchResultDrafts] = useState<Record<number, { homeScore: string; awayScore: string; winnerTeamId: string; resultSummary: string }>>({});
  const [teamStats, setTeamStats] = useState<Record<number, TeamMatchStats>>({});
  const [discoverFilters, setDiscoverFilters] = useState({ city: "", position: "", availability: "" });
  const [recruitmentForm, setRecruitmentForm] = useState({ teamId: "", roleNeeded: "", skillLevel: "Intermediate", city: "", availability: "Weekend evening", description: "", matchDate: "" });

  const loadData = async () => {
    setLoading(true);
    try {
      const [teamRes, matchRes, historyRes, feedRes, playerRes, nearbyRes, networkRes, recruitmentRes, analyticsRes] = await Promise.all([
        getMyTeams(),
        getOpenMatches(),
        getMyMatchHistory(),
        getSocialFeed(),
        discoverPlayers(),
        getNearbyPlayers(),
        getMyNetwork(),
        getOpenRecruitments(),
        getCommunityAnalytics(),
      ]);
      setTeams(teamRes.data as Team[]);
      setMatches(matchRes.data as PublicMatch[]);
      setHistory(historyRes.data as PublicMatch[]);
      setFeed(feedRes.data as FeedEvent[]);
      setPlayers(playerRes.data as PlayerDiscover[]);
      setNearbyPlayers(nearbyRes.data as PlayerDiscover[]);
      setNetwork(networkRes.data as PlayerConnection[]);
      setRecruitments(recruitmentRes.data as TeamRecruitment[]);
      setAnalytics(analyticsRes.data as CommunityAnalytics);
    } catch (error) {
      setStatus(errorMessage(error, "Unable to load social workspace"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let active = true;

    const bootstrap = async () => {
      setLoading(true);
      try {
        const [teamRes, matchRes, historyRes, feedRes, playerRes, nearbyRes, networkRes, recruitmentRes, analyticsRes] = await Promise.all([
          getMyTeams(),
          getOpenMatches(),
          getMyMatchHistory(),
          getSocialFeed(),
          discoverPlayers(),
          getNearbyPlayers(),
          getMyNetwork(),
          getOpenRecruitments(),
          getCommunityAnalytics(),
        ]);
        if (!active) return;
        setTeams(teamRes.data as Team[]);
        setMatches(matchRes.data as PublicMatch[]);
        setHistory(historyRes.data as PublicMatch[]);
        setFeed(feedRes.data as FeedEvent[]);
        setPlayers(playerRes.data as PlayerDiscover[]);
        setNearbyPlayers(nearbyRes.data as PlayerDiscover[]);
        setNetwork(networkRes.data as PlayerConnection[]);
        setRecruitments(recruitmentRes.data as TeamRecruitment[]);
        setAnalytics(analyticsRes.data as CommunityAnalytics);
      } catch (error) {
        if (active) setStatus(errorMessage(error, "Unable to load social workspace"));
      } finally {
        if (active) setLoading(false);
      }
    };

    void bootstrap();

    return () => {
      active = false;
    };
  }, []);

  const openChat = (nextRoom: ChatRoom) => {
    setRoom(nextRoom);
    const request = nextRoom.type === "TEAM" ? getTeamMessages(nextRoom.id) : getMatchMessages(nextRoom.id);
    void request.then((res) => setMessages(res.data as ChatMessage[])).catch(() => setMessages([]));
  };

  const runMatchAction = async (action: () => Promise<unknown>, successMessage: string) => {
    try {
      setStatus("");
      await action();
      setStatus(successMessage);
      await loadData();
    } catch (error) {
      setStatus(errorMessage(error, "Match action failed"));
    }
  };

  const loadTeamStats = async (teamId: number) => {
    try {
      const response = await getTeamMatchStats(teamId);
      setTeamStats((current) => ({ ...current, [teamId]: response.data }));
    } catch (error) {
      setStatus(errorMessage(error, "Could not load team match stats"));
    }
  };

  const updateResultDraft = (matchId: number, patch: Partial<{ homeScore: string; awayScore: string; winnerTeamId: string; resultSummary: string }>) => {
    setMatchResultDrafts((current) => ({
      ...current,
      [matchId]: {
        homeScore: current[matchId]?.homeScore ?? "",
        awayScore: current[matchId]?.awayScore ?? "",
        winnerTeamId: current[matchId]?.winnerTeamId ?? "",
        resultSummary: current[matchId]?.resultSummary ?? "",
        ...patch,
      },
    }));
  };

  const submitResult = (match: PublicMatch) => {
    const draft = matchResultDrafts[match.id];
    const homeScore = Number(draft?.homeScore);
    const awayScore = Number(draft?.awayScore);
    if (!Number.isFinite(homeScore) || !Number.isFinite(awayScore)) {
      setStatus("Enter valid home and away scores before submitting the result.");
      return;
    }

    void runMatchAction(
      () =>
        submitMatchResult(match.id, {
          homeScore,
          awayScore,
          winnerTeamId: draft?.winnerTeamId ? Number(draft.winnerTeamId) : undefined,
          resultSummary: draft?.resultSummary || undefined,
        }),
      "Match result submitted",
    );
  };

  return (
    <main className="theme-page-player min-h-screen px-4 py-8 sm:px-6">
      <div className="mx-auto max-w-7xl space-y-6">
        <section className="theme-panel rounded-[32px] p-6">
          <p className="theme-kicker text-xs font-semibold uppercase tracking-[0.3em]">Epic 13 / Social Ecosystem</p>
          <div className="mt-4 flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <h1 className="theme-title text-3xl font-black tracking-tight sm:text-4xl">Create teams, fill matches, keep players inside Cricbook.</h1>
              <p className="theme-muted mt-2 max-w-3xl text-sm">
                This workspace covers match setup, player discovery, social activity, team and match chat, and personal match history so the product behaves like a community platform instead of a one-off booking tool.
              </p>
            </div>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              <HeroStat label="Open matches" value={matches.length} />
              <HeroStat label="Players" value={analytics?.discoverablePlayers ?? players.length} />
              <HeroStat label="Network" value={analytics?.acceptedConnections ?? network.length} />
              <HeroStat label="Open roles" value={analytics?.openRecruitments ?? recruitments.length} />
            </div>
          </div>
        </section>

        {status && <p className="theme-panel rounded-2xl px-4 py-3 text-sm text-emerald-700 dark:text-emerald-200">{status}</p>}
        {loading && <p className="theme-panel rounded-3xl p-8 text-center theme-muted">Loading social workspace...</p>}

        {!loading && (
          <div className="grid gap-6 xl:grid-cols-[1fr_360px]">
            <div className="space-y-6">
              <section className="theme-panel rounded-[28px] p-6">
                <h2 className="theme-title text-xl font-bold">Create Match</h2>
                <p className="theme-muted mt-1 text-sm">Publish public, private, or paid matches and optionally attach team-vs-team context.</p>
                <div className="mt-5 grid gap-3 md:grid-cols-2 xl:grid-cols-4">
                  <Field label="Title"><input value={matchForm.title} onChange={(e) => setMatchForm({ ...matchForm, title: e.target.value })} className={inputClass} /></Field>
                  <Field label="City"><input value={matchForm.city} onChange={(e) => setMatchForm({ ...matchForm, city: e.target.value })} className={inputClass} /></Field>
                  <Field label="Location"><input value={matchForm.location} onChange={(e) => setMatchForm({ ...matchForm, location: e.target.value })} className={inputClass} /></Field>
                  <Field label="Visibility">
                    <select value={matchForm.visibility} onChange={(e) => setMatchForm({ ...matchForm, visibility: e.target.value as MatchVisibility })} className={inputClass}>
                      <option value="PUBLIC">Public</option>
                      <option value="PRIVATE">Private</option>
                      <option value="PAID">Paid</option>
                    </select>
                  </Field>
                  <Field label="Format">
                    <select value={matchForm.format} onChange={(e) => setMatchForm({ ...matchForm, format: e.target.value as MatchFormat })} className={inputClass}>
                      <option value="FRIENDLY">Friendly</option>
                      <option value="PRACTICE">Practice</option>
                      <option value="T20">T20</option>
                      <option value="ONE_DAY">One Day</option>
                      <option value="BOX_CRICKET">Box Cricket</option>
                    </select>
                  </Field>
                  <Field label="Match date"><input type="date" value={matchForm.matchDate} onChange={(e) => setMatchForm({ ...matchForm, matchDate: e.target.value })} className={inputClass} /></Field>
                  <Field label="Start time"><input type="time" value={matchForm.startTime} onChange={(e) => setMatchForm({ ...matchForm, startTime: e.target.value })} className={inputClass} /></Field>
                  <Field label="End time"><input type="time" value={matchForm.endTime} onChange={(e) => setMatchForm({ ...matchForm, endTime: e.target.value })} className={inputClass} /></Field>
                  <Field label="Max players"><input type="number" min={2} value={matchForm.maxPlayers} onChange={(e) => setMatchForm({ ...matchForm, maxPlayers: Number(e.target.value) })} className={inputClass} /></Field>
                  <Field label="Overs"><input type="number" min={1} value={matchForm.overs} onChange={(e) => setMatchForm({ ...matchForm, overs: Number(e.target.value) })} className={inputClass} /></Field>
                  <Field label="Entry fee"><input type="number" min={0} value={matchForm.entryFee} onChange={(e) => setMatchForm({ ...matchForm, entryFee: Number(e.target.value) })} className={inputClass} /></Field>
                  <Field label="Home team">
                    <select value={matchForm.homeTeamId} onChange={(e) => setMatchForm({ ...matchForm, homeTeamId: e.target.value })} className={inputClass}>
                      <option value="">No team</option>
                      {teams.map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}
                    </select>
                  </Field>
                  <Field label="Away team">
                    <select value={matchForm.awayTeamId} onChange={(e) => setMatchForm({ ...matchForm, awayTeamId: e.target.value })} className={inputClass}>
                      <option value="">No team</option>
                      {teams.map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}
                    </select>
                  </Field>
                  <Field label="Challenge message" className="md:col-span-2 xl:col-span-4">
                    <textarea value={matchForm.challengeMessage} onChange={(e) => setMatchForm({ ...matchForm, challengeMessage: e.target.value })} className={`${inputClass} min-h-[90px]`} placeholder="Optional message for the opponent captain" />
                  </Field>
                  <Field label="Notes" className="md:col-span-2 xl:col-span-4">
                    <textarea value={matchForm.notes} onChange={(e) => setMatchForm({ ...matchForm, notes: e.target.value })} className={`${inputClass} min-h-[110px]`} />
                  </Field>
                </div>
                <div className="mt-5 flex justify-end">
                  <button
                    type="button"
                    onClick={async () => {
                      try {
                        setStatus("");
                        await createPublicMatch({
                          ...matchForm,
                          sport: "Cricket",
                          homeTeamId: matchForm.homeTeamId ? Number(matchForm.homeTeamId) : undefined,
                          awayTeamId: matchForm.awayTeamId ? Number(matchForm.awayTeamId) : undefined,
                        });
                        setMatchForm(emptyMatchForm);
                        setStatus("Match created");
                        await loadData();
                      } catch (error) {
                        setStatus(errorMessage(error, "Match creation failed"));
                      }
                    }}
                    className="rounded-2xl bg-emerald-500 px-5 py-3 text-sm font-bold text-white shadow-sm hover:bg-emerald-400"
                  >
                    Publish Match
                  </button>
                </div>
              </section>

              <section className="grid gap-6 lg:grid-cols-2">
                <div className="theme-panel rounded-[28px] p-6">
                  <div className="flex items-center justify-between gap-3">
                    <h2 className="theme-title text-xl font-bold">Open Matches</h2>
                    <span className="theme-muted text-sm">{matches.length} live opportunities</span>
                  </div>
                  <div className="mt-4 space-y-3">
                    {matches.length === 0 && <Empty text="No public matches found." />}
                    {matches.map((match) => (
                      <div key={match.id} className="theme-card-strong rounded-3xl p-4">
                        <p className="theme-title font-semibold">{match.title}</p>
                        <p className="theme-muted mt-1 text-sm">{match.city} / {match.location}</p>
                        <p className="theme-muted mt-1 text-sm">{match.matchDate} / {match.startTime} {match.endTime ? `- ${match.endTime}` : ""}</p>
                        <p className="theme-soft mt-2 text-xs uppercase tracking-wide">
                          {match.visibility} / {match.format} / {match.joinedPlayers}/{match.maxPlayers} players / {match.overs || "--"} overs / Rs {match.entryFee || 0}
                        </p>
                        {(match.homeTeamName || match.awayTeamName) && (
                          <p className="mt-2 text-sm text-emerald-700 dark:text-emerald-200">{match.homeTeamName || "Open Squad"} vs {match.awayTeamName || "Need Opponent"}</p>
                        )}
                        {match.challengeMessage && <p className="theme-muted mt-2 text-sm">{match.challengeMessage}</p>}
                        <p className="theme-soft mt-2 text-xs uppercase tracking-wide">
                          {match.status} {match.winnerTeamName ? `/ Winner: ${match.winnerTeamName}` : ""} {match.homeScore !== undefined && match.awayScore !== undefined ? `/ ${match.homeScore}-${match.awayScore}` : ""}
                        </p>
                        {match.status === "LIVE" && (
                          <div className="mt-4 grid gap-2 md:grid-cols-2">
                            <input value={matchResultDrafts[match.id]?.homeScore ?? ""} onChange={(e) => updateResultDraft(match.id, { homeScore: e.target.value })} placeholder={`${match.homeTeamName || "Home"} score`} className={inputClass} type="number" />
                            <input value={matchResultDrafts[match.id]?.awayScore ?? ""} onChange={(e) => updateResultDraft(match.id, { awayScore: e.target.value })} placeholder={`${match.awayTeamName || "Away"} score`} className={inputClass} type="number" />
                            <select value={matchResultDrafts[match.id]?.winnerTeamId ?? ""} onChange={(e) => updateResultDraft(match.id, { winnerTeamId: e.target.value })} className={inputClass}>
                              <option value="">Draw / no winner</option>
                              {match.homeTeamId && <option value={match.homeTeamId}>{match.homeTeamName || "Home team"}</option>}
                              {match.awayTeamId && <option value={match.awayTeamId}>{match.awayTeamName || "Away team"}</option>}
                            </select>
                            <input value={matchResultDrafts[match.id]?.resultSummary ?? ""} onChange={(e) => updateResultDraft(match.id, { resultSummary: e.target.value })} placeholder="Result summary" className={inputClass} />
                          </div>
                        )}
                        <div className="mt-4 flex flex-wrap gap-2">
                          {match.status === "OPEN" && <button onClick={() => joinPublicMatch(match.id).then(loadData).catch((error) => setStatus(errorMessage(error, "Join failed")))} className="rounded-xl bg-emerald-500 px-3 py-2 text-sm font-semibold text-white">Join</button>}
                          {match.status === "PENDING" && (
                            <>
                              <button onClick={() => runMatchAction(() => acceptMatchChallenge(match.id), "Challenge accepted")} className="rounded-xl bg-emerald-500 px-3 py-2 text-sm font-semibold text-white">Accept</button>
                              <button onClick={() => runMatchAction(() => rejectMatchChallenge(match.id), "Challenge rejected")} className="theme-card rounded-xl px-3 py-2 text-sm theme-text">Reject</button>
                            </>
                          )}
                          {["PENDING", "ACCEPTED"].includes(match.status) && (
                            <button
                              onClick={() =>
                                runMatchAction(
                                  () =>
                                    schedulePublicMatch(match.id, {
                                      turfId: match.turfId,
                                      matchDate: match.matchDate,
                                      startTime: match.startTime,
                                      endTime: match.endTime,
                                      location: match.location,
                                      note: "Schedule confirmed from social match center",
                                    }),
                                  "Match schedule confirmed",
                                )
                              }
                              className="theme-card rounded-xl px-3 py-2 text-sm theme-text"
                            >
                              Confirm Schedule
                            </button>
                          )}
                          {["ACCEPTED", "SCHEDULED", "FULL"].includes(match.status) && <button onClick={() => runMatchAction(() => startPublicMatch(match.id), "Match started")} className="rounded-xl bg-sky-500 px-3 py-2 text-sm font-semibold text-white">Start</button>}
                          {["PENDING", "ACCEPTED", "SCHEDULED", "OPEN", "FULL"].includes(match.status) && <button onClick={() => runMatchAction(() => cancelPublicMatch(match.id), "Match cancelled")} className="rounded-xl border border-rose-400/40 px-3 py-2 text-sm font-semibold text-rose-300">Cancel</button>}
                          {match.status === "LIVE" && <button onClick={() => submitResult(match)} className="rounded-xl bg-emerald-500 px-3 py-2 text-sm font-semibold text-white">Submit Result</button>}
                          <button onClick={() => openChat({ type: "MATCH", id: match.id, title: match.title })} className="theme-card rounded-xl px-3 py-2 text-sm theme-text hover:border-emerald-400/40">Chat</button>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="theme-panel rounded-[28px] p-6">
                  <div className="flex items-center justify-between gap-3">
                    <h2 className="theme-title text-xl font-bold">My Match History</h2>
                    <span className="theme-muted text-sm">{history.length} joined matches</span>
                  </div>
                  <div className="mt-4 space-y-3">
                    {history.length === 0 && <Empty text="No history yet." />}
                    {history.map((match) => (
                      <div key={match.id} className="theme-card-strong rounded-3xl p-4">
                        <p className="theme-title font-semibold">{match.title}</p>
                        <p className="theme-muted mt-1 text-sm">{match.matchDate} / {match.city}</p>
                        <p className="theme-soft mt-1 text-xs uppercase tracking-wide">
                          {match.status} / {match.joinedPlayers}/{match.maxPlayers} players {match.homeScore !== undefined && match.awayScore !== undefined ? `/ ${match.homeScore}-${match.awayScore}` : ""}
                        </p>
                      </div>
                    ))}
                  </div>
                </div>
              </section>

              <section className="grid gap-6 lg:grid-cols-2">
                <div className="theme-panel rounded-[28px] p-6">
                  <h2 className="theme-title text-xl font-bold">Find Players</h2>
                  <p className="theme-muted mt-1 text-sm">Use player identity and availability to fill matches quickly.</p>
                  <div className="mt-4 grid gap-3 md:grid-cols-3">
                    <input value={discoverFilters.city} onChange={(e) => setDiscoverFilters({ ...discoverFilters, city: e.target.value })} placeholder="City" className={inputClass} />
                    <input value={discoverFilters.position} onChange={(e) => setDiscoverFilters({ ...discoverFilters, position: e.target.value })} placeholder="Position" className={inputClass} />
                    <input value={discoverFilters.availability} onChange={(e) => setDiscoverFilters({ ...discoverFilters, availability: e.target.value })} placeholder="Availability" className={inputClass} />
                  </div>
                  <div className="mt-4 flex justify-end">
                    <button
                      type="button"
                      onClick={() =>
                        discoverPlayers(discoverFilters)
                          .then((res) => setPlayers(res.data as PlayerDiscover[]))
                          .catch((error) => setStatus(errorMessage(error, "Player discovery failed")))
                      }
                      className="rounded-2xl border border-emerald-400/30 bg-emerald-500/10 px-4 py-2 text-sm font-semibold text-emerald-700 dark:text-emerald-200"
                    >
                      Search Players
                    </button>
                  </div>
                  <div className="mt-4 space-y-3">
                    {players.length === 0 && <Empty text="No players matched your filter." />}
                    {players.map((player) => (
                      <div key={player.userId} className="theme-card-strong rounded-3xl p-4">
                        <div className="flex items-start justify-between gap-3">
                          <div>
                            <p className="theme-title font-semibold">{player.name}</p>
                            <p className="theme-muted mt-1 text-sm">{player.city || player.availability?.homeCity || "City not set"}</p>
                          </div>
                          <span className="theme-card rounded-full px-2.5 py-1 text-[11px] font-bold uppercase tracking-wide theme-muted">
                            {player.position || "Flexible"}
                          </span>
                        </div>
                        <p className="theme-text mt-3 text-sm">
                          {pretty(player.battingStyle)} / {pretty(player.bowlingStyle)} / {player.skillLevel || player.experience || "Skill not set"}
                        </p>
                        <p className="theme-muted mt-1 text-sm">
                          {player.matches} matches / {player.wins} wins / rating {player.rating} / trust {player.trustScore ?? 70}
                        </p>
                        <p className="theme-soft mt-2 text-xs uppercase tracking-wide">
                          Match {player.matchScore ?? 0}% / Network {player.networkScore ?? 0}% / {player.connectionStatus || "NONE"}
                        </p>
                        <p className="theme-muted mt-2 text-sm">{player.recommendationReason || "Good community player fit."}</p>
                        {player.availability && (
                          <p className="mt-2 text-xs uppercase tracking-wide text-emerald-700 dark:text-emerald-200">
                            {player.availability.preferredWindow || player.preferredTime || "Any time"} / {player.availability.availableDays || "Flexible days"}
                          </p>
                        )}
                        <div className="mt-4 flex flex-wrap gap-2">
                          <button
                            type="button"
                            onClick={() =>
                              connectPlayer({ userId: player.userId, type: "FRIEND", message: "Let's connect on Cricbook." })
                                .then(loadData)
                                .catch((error) => setStatus(errorMessage(error, "Connection request failed")))
                            }
                            className="rounded-xl bg-emerald-500 px-3 py-2 text-xs font-bold text-white"
                          >
                            Connect
                          </button>
                          <button
                            type="button"
                            onClick={() =>
                              connectPlayer({ userId: player.userId, type: "FOLLOW" })
                                .then(loadData)
                                .catch((error) => setStatus(errorMessage(error, "Follow failed")))
                            }
                            className="theme-card rounded-xl px-3 py-2 text-xs font-bold theme-text"
                          >
                            Follow
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="theme-panel rounded-[28px] p-6">
                  <h2 className="theme-title text-xl font-bold">Community Feed</h2>
                  <p className="theme-muted mt-1 text-sm">Activity stream for teams, invites, and match creation.</p>
                  <div className="mt-4 space-y-3">
                    {feed.length === 0 && <Empty text="No feed activity yet." />}
                    {feed.map((item) => (
                      <div key={item.id} className="theme-card-strong rounded-3xl p-4">
                        <p className="theme-title font-semibold">{item.title}</p>
                        <p className="theme-text mt-2 text-sm">{item.summary}</p>
                        <p className="theme-soft mt-2 text-xs uppercase tracking-wide">{item.eventType} / {item.createdAt?.replace("T", " ").slice(0, 16)}</p>
                      </div>
                    ))}
                  </div>
                </div>
              </section>

              <section className="grid gap-6 lg:grid-cols-3">
                <div className="theme-panel rounded-[28px] p-6">
                  <h2 className="theme-title text-xl font-bold">Nearby Players</h2>
                  <p className="theme-muted mt-1 text-sm">Hyperlocal player discovery ranked by city, trust, and availability.</p>
                  <div className="mt-4 space-y-3">
                    {nearbyPlayers.length === 0 && <Empty text="No nearby players found yet." />}
                    {nearbyPlayers.slice(0, 5).map((player) => (
                      <div key={`nearby-${player.userId}`} className="theme-card-strong rounded-3xl p-4">
                        <p className="theme-title font-semibold">{player.name}</p>
                        <p className="theme-muted mt-1 text-sm">{player.city || player.availability?.homeCity || "Nearby city"} / {player.position || "Flexible"}</p>
                        <p className="theme-soft mt-2 text-xs uppercase tracking-wide">Fit {player.matchScore ?? 0}% / Trust {player.trustScore ?? 70}</p>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="theme-panel rounded-[28px] p-6">
                  <h2 className="theme-title text-xl font-bold">Cricket Network</h2>
                  <p className="theme-muted mt-1 text-sm">Friends, follows, teammates, and pending connection requests.</p>
                  <div className="mt-4 space-y-3">
                    {network.length === 0 && <Empty text="No network connections yet." />}
                    {network.slice(0, 6).map((item) => (
                      <div key={item.id} className="theme-card-strong rounded-3xl p-4">
                        <p className="theme-title font-semibold">{item.fromUserName} to {item.toUserName}</p>
                        <p className="theme-soft mt-1 text-xs uppercase tracking-wide">{item.type} / {item.status}</p>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="theme-panel rounded-[28px] p-6">
                  <h2 className="theme-title text-xl font-bold">Team Recruitment</h2>
                  <p className="theme-muted mt-1 text-sm">Captains can post open roles and players can apply without WhatsApp.</p>
                  <div className="mt-4 grid gap-3">
                    <select value={recruitmentForm.teamId} onChange={(e) => setRecruitmentForm({ ...recruitmentForm, teamId: e.target.value })} className={inputClass}>
                      <option value="">Select captain team</option>
                      {teams.map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}
                    </select>
                    <input value={recruitmentForm.roleNeeded} onChange={(e) => setRecruitmentForm({ ...recruitmentForm, roleNeeded: e.target.value })} placeholder="Need fast bowler" className={inputClass} />
                    <input value={recruitmentForm.city} onChange={(e) => setRecruitmentForm({ ...recruitmentForm, city: e.target.value })} placeholder="City" className={inputClass} />
                    <input value={recruitmentForm.availability} onChange={(e) => setRecruitmentForm({ ...recruitmentForm, availability: e.target.value })} placeholder="Availability" className={inputClass} />
                    <button
                      type="button"
                      onClick={() =>
                        createRecruitment({
                          teamId: Number(recruitmentForm.teamId),
                          roleNeeded: recruitmentForm.roleNeeded,
                          skillLevel: recruitmentForm.skillLevel,
                          city: recruitmentForm.city,
                          availability: recruitmentForm.availability,
                          description: recruitmentForm.description,
                          matchDate: recruitmentForm.matchDate || undefined,
                        })
                          .then(loadData)
                          .catch((error) => setStatus(errorMessage(error, "Recruitment post failed")))
                      }
                      className="rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-bold text-white"
                    >
                      Post open role
                    </button>
                  </div>
                  <div className="mt-4 space-y-3">
                    {recruitments.length === 0 && <Empty text="No open positions yet." />}
                    {recruitments.slice(0, 5).map((item) => (
                      <div key={item.id} className="theme-card-strong rounded-3xl p-4">
                        <p className="theme-title font-semibold">{item.roleNeeded}</p>
                        <p className="theme-muted mt-1 text-sm">{item.teamName} / {item.city || "Any city"} / {item.skillLevel}</p>
                        <p className="theme-soft mt-2 text-xs uppercase tracking-wide">{item.applicationCount} applications / {item.status}</p>
                        <button
                          type="button"
                          onClick={() => applyRecruitment(item.id, "I am interested in this role.").then(loadData).catch((error) => setStatus(errorMessage(error, "Application failed")))}
                          className="mt-3 rounded-xl border border-emerald-400/30 px-3 py-2 text-xs font-bold text-emerald-700 dark:text-emerald-200"
                        >
                          Apply
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              </section>
            </div>

            <section className="theme-panel rounded-[28px] p-6">
              <h2 className="theme-title text-xl font-bold">{room ? room.title : "Social Chat"}</h2>
              <p className="theme-muted mt-1 text-sm">Open a team or match room to keep coordination inside Cricbook.</p>
              <div className="mt-5 space-y-5">
                <div>
                  <h3 className="theme-soft text-sm font-semibold uppercase tracking-[0.28em]">Quick rooms</h3>
                  <div className="mt-3 space-y-2">
                    {teams.slice(0, 4).map((team) => (
                      <div key={`team-${team.id}`} className="theme-card-strong rounded-2xl px-4 py-3">
                        <button type="button" onClick={() => openChat({ type: "TEAM", id: team.id, title: team.name })} className="w-full text-left text-sm font-semibold theme-text">
                          Team / {team.name}
                        </button>
                        {teamStats[team.id] ? (
                          <p className="theme-soft mt-2 text-[11px] uppercase tracking-wide">
                            Team record: {teamStats[team.id].wins}W / {teamStats[team.id].losses}L / {teamStats[team.id].draws}D / {teamStats[team.id].pendingChallenges} pending
                          </p>
                        ) : (
                          <button type="button" onClick={() => loadTeamStats(team.id)} className="mt-2 text-xs font-bold text-emerald-700 dark:text-emerald-200">
                            Load match stats
                          </button>
                        )}
                      </div>
                    ))}
                    {matches.slice(0, 4).map((match) => (
                      <button key={`match-${match.id}`} type="button" onClick={() => openChat({ type: "MATCH", id: match.id, title: match.title })} className="theme-card-strong w-full rounded-2xl px-4 py-3 text-left text-sm theme-text hover:border-emerald-400/30">
                        Match / {match.title}
                      </button>
                    ))}
                  </div>
                </div>
                <div className="theme-panel-soft h-[420px] space-y-3 overflow-y-auto rounded-3xl p-4">
                  {!room && <Empty text="Select a chat room to view messages." />}
                  {room && messages.length === 0 && <Empty text="No messages yet." />}
                  {messages.map((item) => (
                    <div key={item.id} className="theme-card rounded-2xl p-3">
                      <p className="text-xs font-semibold uppercase tracking-wide text-emerald-700 dark:text-emerald-200">{item.senderName}</p>
                      <p className="theme-text mt-2 text-sm">{item.message}</p>
                    </div>
                  ))}
                </div>
                <form
                  onSubmit={(event) => {
                    event.preventDefault();
                    if (!room || !message.trim()) return;
                    const request = room.type === "TEAM" ? sendTeamMessage(room.id, message) : sendMatchMessage(room.id, message);
                    void request.then(() => {
                      setMessage("");
                      openChat(room);
                    }).catch((error) => setStatus(errorMessage(error, "Message failed")));
                  }}
                  className="flex gap-2"
                >
                  <input disabled={!room} value={message} onChange={(e) => setMessage(e.target.value)} placeholder="Write a message" className={`${inputClass} disabled:opacity-60`} />
                  <button disabled={!room} className="rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-bold text-white disabled:opacity-60">Send</button>
                </form>
              </div>
            </section>
          </div>
        )}
      </div>
    </main>
  );
}

function HeroStat({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="theme-card rounded-2xl px-4 py-3">
      <p className="theme-soft text-[11px] font-semibold uppercase tracking-[0.28em]">{label}</p>
      <p className="theme-title mt-2 text-lg font-bold">{value}</p>
    </div>
  );
}

function Field({ label, children, className }: { label: string; children: ReactNode; className?: string }) {
  return (
    <label className={`block ${className || ""}`}>
      <span className="theme-text mb-2 block text-sm font-semibold">{label}</span>
      {children}
    </label>
  );
}

function Empty({ text }: { text: string }) {
  return <p className="theme-panel-soft rounded-2xl border border-dashed p-4 text-center text-sm theme-muted">{text}</p>;
}

function pretty(value?: string) {
  if (!value) return "Style not set";
  return value.toLowerCase().split("_").map((part) => part.charAt(0).toUpperCase() + part.slice(1)).join(" ");
}

function errorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { error?: string; message?: string } } };
  return apiError.response?.data?.message || apiError.response?.data?.error || fallback;
}

const inputClass =
  "theme-input w-full rounded-2xl px-4 py-3 text-sm outline-none transition focus:border-emerald-400/40 focus:ring-2 focus:ring-emerald-500/10";
