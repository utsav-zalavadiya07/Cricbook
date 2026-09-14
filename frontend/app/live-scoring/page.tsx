"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import {  Button, Card, Field, Input, Select, Textarea , TileInfo } from "@/components/ui";
import {
  completeLiveScore,
  createLiveScore,
  getLiveScore,
  getLiveScores,
  getScoreArchive,
  pauseLiveScore,
  recordScoreEvent,
  resumeLiveScore,
  SCORING_API_BASE_URL,
  startLiveScore,
  undoScoreEvent,
} from "@/modules/scoring/services/scoringService";
import { LiveScore, LiveScoreDetail, ScoreEventPayload } from "@/modules/scoring/types/scoring.types";

const scoreEvents = ["snapshot", "created", "started", "ball-recorded", "ball-undone", "ball-edited", "ball-deleted", "paused", "resumed", "cancelled", "completed", "locked", "result-overridden"];
const runButtons = [0, 1, 2, 3, 4, 6];
const extraButtons = [
  { label: "Wide", type: "WIDE" },
  { label: "No Ball", type: "NO_BALL" },
  { label: "Bye", type: "BYE" },
  { label: "Leg Bye", type: "LEG_BYE" },
];

export default function LiveScoringPage() {
  const searchParams = useSearchParams();
  const [scores, setScores] = useState<LiveScore[]>([]);
  const [archive, setArchive] = useState<LiveScore[]>([]);
  const [detail, setDetail] = useState<LiveScoreDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState("");
  const [streamConnected, setStreamConnected] = useState(false);
  const [form, setForm] = useState({ title: "", teamAName: "", teamBName: "", battingTeam: "", maxOvers: 20 });
  const [ballForm, setBallForm] = useState({ batter: "", bowler: "", runs: 0, extraRuns: 0, extraType: "", wicket: false, wicketType: "BOWLED", note: "" });

  const activeScore = detail?.score ?? null;
  const recentEvents = useMemo(() => [...(detail?.events ?? [])].reverse().slice(0, 10), [detail?.events]);

  const loadScores = useCallback(async () => {
    setLoading(true);
    try {
      const [liveResponse, archiveResponse] = await Promise.all([getLiveScores(), getScoreArchive()]);
      setScores(liveResponse.data);
      setArchive(archiveResponse.data);
    } finally {
      setLoading(false);
    }
  }, []);

  const openScore = useCallback(async (scoreId: number) => {
    const response = await getLiveScore(scoreId);
    setDetail(response.data);
  }, []);

  useEffect(() => {
    void loadScores();
  }, [loadScores]);

  useEffect(() => {
    const scoreId = Number(searchParams.get("scoreId"));
    if (scoreId) {
      void openScore(scoreId);
    }
  }, [openScore, searchParams]);

  useEffect(() => {
    if (!activeScore || typeof window === "undefined") {
      return undefined;
    }
    const stream = new EventSource(toApiUrl(detail?.streamUrl ?? `/live-scores/${activeScore.id}/stream`));
    const handleEvent = (event: MessageEvent) => {
      setDetail(JSON.parse(event.data));
      setStreamConnected(true);
    };
    scoreEvents.forEach((eventName) => stream.addEventListener(eventName, handleEvent));
    stream.onerror = () => setStreamConnected(false);
    const fallback = window.setInterval(() => void openScore(activeScore.id), 10000);
    return () => {
      scoreEvents.forEach((eventName) => stream.removeEventListener(eventName, handleEvent));
      stream.close();
      window.clearInterval(fallback);
      setStreamConnected(false);
    };
  }, [activeScore?.id, detail?.streamUrl, openScore]);

  async function submitScore(event: React.FormEvent) {
    event.preventDefault();
    setStatus("");
    try {
      const response = await createLiveScore(form);
      setForm({ title: "", teamAName: "", teamBName: "", battingTeam: "", maxOvers: 20 });
      setStatus("Scorecard created. Start the match before recording deliveries.");
      await loadScores();
      await openScore(response.data.id);
    } catch (error) {
      setStatus(errorMessage(error, "Live score creation failed"));
    }
  }

  async function runScoreAction(action: () => Promise<{ data: LiveScore }>, message: string) {
    if (!activeScore) return;
    try {
      const response = await action();
      setDetail((current) => current ? { ...current, score: response.data } : current);
      setStatus(message);
      await loadScores();
      await openScore(activeScore.id);
    } catch (error) {
      setStatus(errorMessage(error, "Score action failed"));
    }
  }

  async function submitBall(event: React.FormEvent) {
    event.preventDefault();
    await recordBall({
      runs: Number(ballForm.runs),
      extraRuns: Number(ballForm.extraRuns),
      extraType: ballForm.extraType || undefined,
      wicket: ballForm.wicket,
      wicketType: ballForm.wicket ? ballForm.wicketType : undefined,
      batter: ballForm.batter || undefined,
      bowler: ballForm.bowler || undefined,
      note: ballForm.note || undefined,
    });
  }

  async function recordBall(payload: ScoreEventPayload) {
    if (!activeScore) return;
    setStatus("");
    try {
      const response = await recordScoreEvent(activeScore.id, payload);
      setDetail(response.data);
      setBallForm((current) => ({ ...current, runs: 0, extraRuns: 0, extraType: "", wicket: false, wicketType: "BOWLED", note: "" }));
      await loadScores();
    } catch (error) {
      setStatus(errorMessage(error, "Score update failed"));
    }
  }

  return (
    <main className="theme-page-player min-h-screen px-4 py-6 sm:px-6 lg:px-8">
      <div className="w-full space-y-6">
        <header className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="theme-soft text-xs font-black uppercase tracking-[0.2em]">EPIC 28 Match Experience</p>
            <h1 className="theme-title mt-1 text-3xl font-black">Live Scoring Center
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Live Scoring Center information" focusable={false}>Ball-by-ball scoring, digital scorecards, commentary, live stream updates, and tournament sync.</TileInfo>
              </span></h1>
          </div>
          <div className="flex flex-wrap gap-2">
            <StatusPill label={streamConnected ? "Stream live" : "Polling fallback"} tone={streamConnected ? "success" : "warning"} />
            <StatusPill label={`${scores.length} active`} />
            <StatusPill label={`${archive.length} archived`} />
          </div>
        </header>

        {status ? <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/10 px-4 py-3 text-sm font-semibold text-emerald-100">{status}</div> : null}

        <div className="grid gap-6 xl:grid-cols-[390px_1fr]">
          <aside className="space-y-6">
            <Card className="p-5">
              <h2 className="theme-title text-lg font-black">New Scorecard</h2>
              <form onSubmit={submitScore} className="mt-4 space-y-3">
                <Field label="Match title">
                  <Input value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} placeholder="Weekend Cup Final" />
                </Field>
                <Field label="Team A">
                  <Input required value={form.teamAName} onChange={(event) => setForm({ ...form, teamAName: event.target.value })} placeholder="Team A" />
                </Field>
                <Field label="Team B">
                  <Input required value={form.teamBName} onChange={(event) => setForm({ ...form, teamBName: event.target.value })} placeholder="Team B" />
                </Field>
                <Field label="Batting first" hint="Leave blank to use Team A.">
                  <Input value={form.battingTeam} onChange={(event) => setForm({ ...form, battingTeam: event.target.value })} placeholder="Team A" />
                </Field>
                <Field label="Max overs">
                  <Input type="number" min={1} value={form.maxOvers} onChange={(event) => setForm({ ...form, maxOvers: Number(event.target.value) })} />
                </Field>
                <Button type="submit" className="w-full">Create scorecard</Button>
              </form>
            </Card>

            <ScoreList title="Live Match Center" scores={scores} loading={loading} onOpen={openScore} empty="No live or scheduled scorecards." />
            <ScoreList title="Historical Archive" scores={archive.slice(0, 6)} loading={loading} onOpen={openScore} empty="No archived scorecards yet." />
          </aside>

          <section className="space-y-6">
            {!detail ? (
              <Card className="p-8 text-center">
                <h2 className="theme-title text-xl font-black">Open a scorecard</h2>
                <p className="theme-muted mt-2 text-sm">Create a scorecard or open an existing match to start live scoring.</p>
              </Card>
            ) : (
              <>
                <Card className="overflow-hidden">
                  <div className="border-b border-[var(--theme-border)] p-5">
                    <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                      <div>
                        <div className="flex flex-wrap items-center gap-2">
                          <StatusPill label={detail.score.status} tone={detail.score.status === "LIVE" ? "success" : detail.score.status === "COMPLETED" ? "neutral" : "warning"} />
                          <StatusPill label={detail.verificationStatus ?? "SCORER_CONTROLLED"} />
                          {detail.score.locked ? <StatusPill label="Locked" tone="danger" /> : null}
                        </div>
                        <h2 className="theme-title mt-3 text-2xl font-black">{detail.score.title}</h2>
                        <p className="theme-muted mt-1 text-sm">{detail.resultSummary ?? detail.score.resultSummary}</p>
                      </div>
                      <div className="flex flex-wrap gap-2">
                        {detail.score.status === "NOT_STARTED" ? (
                          <Button onClick={() => runScoreAction(() => startLiveScore(detail.score.id), "Match started.")}>Start match</Button>
                        ) : null}
                        {detail.score.status === "LIVE" ? (
                          <Button tone="secondary" onClick={() => runScoreAction(() => pauseLiveScore(detail.score.id), "Match paused.")}>Pause</Button>
                        ) : null}
                        {detail.score.status === "PAUSED" || detail.score.status === "INNINGS_BREAK" ? (
                          <Button tone="secondary" onClick={() => runScoreAction(() => resumeLiveScore(detail.score.id), "Match resumed.")}>Resume</Button>
                        ) : null}
                        <Button tone="secondary" disabled={!detail.events.length || detail.score.locked} onClick={async () => {
                          try {
                            const response = await undoScoreEvent(detail.score.id);
                            setDetail(response.data);
                            setStatus("Last delivery undone.");
                            await loadScores();
                          } catch (error) {
                            setStatus(errorMessage(error, "Undo failed"));
                          }
                        }}>Undo ball</Button>
                        <Button tone="danger" disabled={detail.score.status === "COMPLETED"} onClick={() => runScoreAction(() => completeLiveScore(detail.score.id), "Match completed and archived.")}>Complete</Button>
                      </div>
                    </div>

                    <div className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
                      <Metric title="Score" value={`${detail.score.runs}/${detail.score.wickets}`} />
                      <Metric title="Overs" value={detail.score.overs} />
                      <Metric title="Run Rate" value={detail.score.runRate} />
                      <Metric title="Target" value={detail.score.targetRuns ?? "-"} />
                      <Metric title="Projected" value={detail.score.projectedScore} />
                    </div>
                  </div>

                  <div className="grid gap-6 p-5 2xl:grid-cols-[380px_1fr]">
                    <form onSubmit={submitBall} className="theme-panel-soft rounded-2xl p-4">
                      <h3 className="theme-title text-base font-black">Scorer Console</h3>
                      <div className="mt-4 grid gap-3 sm:grid-cols-2 2xl:grid-cols-1">
                        <Field label="Batter">
                          <Input value={ballForm.batter} onChange={(event) => setBallForm({ ...ballForm, batter: event.target.value })} placeholder="Striker" />
                        </Field>
                        <Field label="Bowler">
                          <Input value={ballForm.bowler} onChange={(event) => setBallForm({ ...ballForm, bowler: event.target.value })} placeholder="Bowler" />
                        </Field>
                      </div>
                      <div className="mt-4 grid grid-cols-3 gap-2">
                        {runButtons.map((runs) => (
                          <Button key={runs} type="button" disabled={detail.score.status !== "LIVE" || detail.score.locked} onClick={() => recordBall({ runs, extraRuns: 0, wicket: false, batter: ballForm.batter || undefined, bowler: ballForm.bowler || undefined, note: ballForm.note || undefined })}>
                            {runs}
                          </Button>
                        ))}
                      </div>
                      <div className="mt-3 grid grid-cols-2 gap-2">
                        {extraButtons.map((extra) => (
                          <Button key={extra.type} type="button" tone="secondary" disabled={detail.score.status !== "LIVE" || detail.score.locked} onClick={() => recordBall({ runs: 0, extraRuns: extra.type === "BYE" || extra.type === "LEG_BYE" ? 1 : 1, extraType: extra.type, wicket: false, batter: ballForm.batter || undefined, bowler: ballForm.bowler || undefined, note: ballForm.note || undefined })}>
                            {extra.label}
                          </Button>
                        ))}
                      </div>
                      <div className="mt-4 grid gap-3 sm:grid-cols-2 2xl:grid-cols-1">
                        <Field label="Manual runs">
                          <Input type="number" min={0} max={6} value={ballForm.runs} onChange={(event) => setBallForm({ ...ballForm, runs: Number(event.target.value) })} />
                        </Field>
                        <Field label="Extra runs">
                          <Input type="number" min={0} max={7} value={ballForm.extraRuns} onChange={(event) => setBallForm({ ...ballForm, extraRuns: Number(event.target.value) })} />
                        </Field>
                      </div>
                      <div className="mt-3 grid gap-3 sm:grid-cols-2 2xl:grid-cols-1">
                        <Field label="Extra type">
                          <Select value={ballForm.extraType} onChange={(event) => setBallForm({ ...ballForm, extraType: event.target.value })} options={[{ label: "No extra", value: "" }, { label: "Wide", value: "WIDE" }, { label: "No ball", value: "NO_BALL" }, { label: "Bye", value: "BYE" }, { label: "Leg bye", value: "LEG_BYE" }]} />
                        </Field>
                        <Field label="Wicket type">
                          <Select value={ballForm.wicketType} onChange={(event) => setBallForm({ ...ballForm, wicketType: event.target.value })} options={[{ label: "Bowled", value: "BOWLED" }, { label: "Caught", value: "CAUGHT" }, { label: "Run out", value: "RUN_OUT" }, { label: "LBW", value: "LBW" }, { label: "Stumped", value: "STUMPED" }, { label: "Hit wicket", value: "HIT_WICKET" }]} />
                        </Field>
                      </div>
                      <label className="mt-3 flex items-center gap-2 text-sm font-semibold text-[var(--theme-muted)]">
                        <input type="checkbox" checked={ballForm.wicket} onChange={(event) => setBallForm({ ...ballForm, wicket: event.target.checked })} />
                        Wicket on this delivery
                      </label>
                      <Field label="Commentary note" className="mt-3">
                        <Textarea rows={3} value={ballForm.note} onChange={(event) => setBallForm({ ...ballForm, note: event.target.value })} placeholder="Optional scorer note" />
                      </Field>
                      <Button type="submit" disabled={detail.score.status !== "LIVE" || detail.score.locked} className="mt-4 w-full">Record manual delivery</Button>
                    </form>

                    <div className="space-y-5">
                      <div className="grid gap-3 md:grid-cols-3">
                        {(detail.innings ?? []).map((innings) => (
                          <Card key={innings.inningsNumber} variant="soft" className="p-4">
                            <p className="theme-soft text-xs font-black uppercase tracking-[0.16em]">Innings {innings.inningsNumber}</p>
                            <p className="theme-title mt-1 text-lg font-black">{innings.battingTeamName}</p>
                            <p className="mt-2 text-2xl font-black text-white">{innings.runs}/{innings.wickets}</p>
                            <p className="theme-muted text-sm">{innings.overs} overs | Extras {innings.extras}</p>
                          </Card>
                        ))}
                      </div>

                      <div className="grid gap-4 xl:grid-cols-2">
                        <ScorecardTable title="Batting Scorecard" headers={["Batter", "R", "B", "4s", "6s", "SR"]} rows={(detail.battingScorecard ?? []).map((row) => [row.out ? `${row.playerName} (${row.dismissal})` : `${row.playerName} *`, row.runs, row.balls, row.fours, row.sixes, row.strikeRate])} />
                        <ScorecardTable title="Bowling Scorecard" headers={["Bowler", "O", "R", "W", "Econ"]} rows={(detail.bowlingScorecard ?? []).map((row) => [row.playerName, row.overs, row.runsConceded, row.wickets, row.economy])} />
                      </div>

                      <div className="grid gap-4 xl:grid-cols-2">
                        <Card variant="soft" className="p-4">
                          <h3 className="theme-title text-base font-black">Recent Overs</h3>
                          <div className="mt-3 space-y-2">
                            {(detail.recentOvers ?? []).length === 0 ? <Empty text="No overs yet." /> : null}
                            {(detail.recentOvers ?? []).map((over) => (
                              <div key={`${over.overNumber}-${over.events.length}`} className="rounded-xl border border-[var(--theme-border)] bg-black/10 p-3">
                                <div className="flex items-center justify-between gap-3">
                                  <p className="font-bold text-[var(--theme-text)]">{over.label}</p>
                                  <p className="theme-muted text-sm">{over.runs} runs | {over.wickets} wkts</p>
                                </div>
                                <div className="mt-2 flex flex-wrap gap-1.5">
                                  {over.events.map((event) => <span key={event.id ?? event.eventSequence} className="rounded-lg bg-white/10 px-2 py-1 text-xs font-bold text-white">{event.wicket ? "W" : event.extraType ? event.extraType.replace("_", " ") : event.runs + event.extraRuns}</span>)}
                                </div>
                              </div>
                            ))}
                          </div>
                        </Card>

                        <Card variant="soft" className="p-4">
                          <h3 className="theme-title text-base font-black">Fall of Wickets</h3>
                          <div className="mt-3 space-y-2">
                            {(detail.fallOfWickets ?? []).length === 0 ? <Empty text="No wickets yet." /> : null}
                            {(detail.fallOfWickets ?? []).map((wicket) => (
                              <div key={wicket.wicketNumber} className="flex items-center justify-between rounded-xl border border-[var(--theme-border)] bg-black/10 px-3 py-2 text-sm">
                                <span className="font-bold text-white">{wicket.wicketNumber}. {wicket.batter}</span>
                                <span className="theme-muted">{wicket.score} at {wicket.over}</span>
                              </div>
                            ))}
                          </div>
                        </Card>
                      </div>

                      <Card variant="soft" className="p-4">
                        <h3 className="theme-title text-base font-black">Live Commentary</h3>
                        <div className="mt-3 space-y-2">
                          {recentEvents.length === 0 ? <Empty text="No commentary yet." /> : null}
                          {recentEvents.map((event) => (
                            <div key={event.id ?? event.eventSequence} className="rounded-xl border border-[var(--theme-border)] bg-black/10 p-3 text-sm">
                              <p className="font-black text-emerald-200">Ball {event.eventSequence}</p>
                              <p className="theme-muted mt-1">{event.note ?? `${event.batter ?? "Batter"} scored ${event.runs + event.extraRuns}`}</p>
                            </div>
                          ))}
                        </div>
                      </Card>
                    </div>
                  </div>
                </Card>
              </>
            )}
          </section>
        </div>
      </div>
    </main>
  );
}

function ScoreList({ title, scores, loading, onOpen, empty }: { title: string; scores: LiveScore[]; loading: boolean; onOpen: (scoreId: number) => void; empty: string }) {
  return (
    <Card className="p-5">
      <h2 className="theme-title text-lg font-black">{title}</h2>
      <div className="mt-4 space-y-3">
        {loading ? <Empty text="Loading scorecards..." /> : null}
        {!loading && scores.length === 0 ? <Empty text={empty} /> : null}
        {scores.map((score) => (
          <button key={score.id} onClick={() => onOpen(score.id)} className="w-full rounded-2xl border border-[var(--theme-border)] bg-black/10 p-4 text-left transition hover:border-emerald-500/50">
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="theme-title font-black">{score.title}</p>
                <p className="theme-muted mt-1 text-sm">{score.teamAName} vs {score.teamBName}</p>
              </div>
              <StatusPill label={score.status} tone={score.status === "LIVE" ? "success" : score.status === "COMPLETED" ? "neutral" : "warning"} />
            </div>
            <p className="mt-3 text-lg font-black text-white">{score.runs}/{score.wickets} <span className="theme-muted text-sm font-semibold">in {score.overs} overs</span></p>
            {score.resultSummary ? <p className="theme-muted mt-1 text-xs">{score.resultSummary}</p> : null}
          </button>
        ))}
      </div>
    </Card>
  );
}

function Metric({ title, value }: { title: string; value: string | number }) {
  return (
    <div className="theme-panel-soft rounded-2xl p-4">
      <p className="theme-soft text-[11px] font-black uppercase tracking-[0.18em]">{title}</p>
      <p className="mt-2 text-2xl font-black text-white">{value}</p>
    </div>
  );
}

function ScorecardTable({ title, headers, rows }: { title: string; headers: string[]; rows: Array<Array<string | number | undefined>> }) {
  return (
    <Card variant="soft" className="overflow-hidden">
      <div className="border-b border-[var(--theme-border)] p-4">
        <h3 className="theme-title text-base font-black">{title}</h3>
      </div>
      {rows.length === 0 ? <div className="p-4"><Empty text="No scorecard rows yet." /></div> : (
        <div className="overflow-x-auto">
          <table className="min-w-full text-sm">
            <thead className="bg-black/10 text-[var(--theme-soft)]">
              <tr>
                {headers.map((header) => <th key={header} className="px-3 py-3 text-left text-[11px] font-black uppercase tracking-[0.16em]">{header}</th>)}
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--theme-border)]">
              {rows.map((row, index) => (
                <tr key={index}>
                  {row.map((cell, cellIndex) => <td key={cellIndex} className="px-3 py-3 font-semibold text-[var(--theme-text)]">{cell ?? "-"}</td>)}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  );
}

function StatusPill({ label, tone = "neutral" }: { label: string; tone?: "success" | "warning" | "danger" | "neutral" }) {
  const classes = {
    success: "border-emerald-500/30 bg-emerald-500/10 text-emerald-200",
    warning: "border-amber-500/30 bg-amber-500/10 text-amber-200",
    danger: "border-red-500/30 bg-red-500/10 text-red-200",
    neutral: "border-white/10 bg-white/5 text-slate-200",
  };
  return <span className={`inline-flex rounded-full border px-3 py-1 text-[11px] font-black uppercase tracking-[0.14em] ${classes[tone]}`}>{label}</span>;
}

function Empty({ text }: { text: string }) {
  return <p className="rounded-xl border border-dashed border-[var(--theme-border)] p-4 text-center text-sm text-[var(--theme-muted)]">{text}</p>;
}

function toApiUrl(path: string) {
  if (path.startsWith("http")) return path;
  const normalized = path.startsWith("/api/v1") ? path.slice("/api/v1".length) : path;
  return `${SCORING_API_BASE_URL}${normalized.startsWith("/") ? normalized : `/${normalized}`}`;
}

function errorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { error?: string; message?: string } } };
  return apiError.response?.data?.error || apiError.response?.data?.message || fallback;
}
