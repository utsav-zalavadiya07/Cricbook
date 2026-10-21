"use client";
import { TileInfo } from "@/components/ui";

import { useEffect, useState } from "react";
import { useI18n } from "@/modules/i18n/I18nProvider";
import { awardGamificationXp, claimGamificationReward, getGamificationProfile } from "@/modules/gamification/services/gamificationService";
import { GamificationProfile } from "@/modules/gamification/types/gamification.types";
import { getPlayerPerformance, getPlayerPerformanceLeaderboard } from "@/modules/performance/services/playerPerformanceService";
import { PlayerPerformance, PlayerPerformanceLeaderboardEntry } from "@/modules/performance/types/playerPerformance.types";

export default function ProgressPage() {
  const { t } = useI18n();
  const [profile, setProfile] = useState<GamificationProfile | null>(null);
  const [performance, setPerformance] = useState<PlayerPerformance | null>(null);
  const [performanceLeaderboard, setPerformanceLeaderboard] = useState<PlayerPerformanceLeaderboardEntry[]>([]);
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    try {
      const [gamificationRes, performanceRes, leaderboardRes] = await Promise.all([
        getGamificationProfile(),
        getPlayerPerformance(),
        getPlayerPerformanceLeaderboard({ type: "IMPACT", limit: 5 }),
      ]);
      setProfile(gamificationRes.data);
      setPerformance(performanceRes.data);
      setPerformanceLeaderboard(leaderboardRes.data);
      setStatus("");
    } catch (error: unknown) {
      setStatus(errorMessage(error, "Unable to load your cricket progress."));
    } finally {
      setLoading(false);
    }
  }

  async function trackMatch() {
    try {
      const res = await awardGamificationXp({ activityType: "MATCH_PLAYED", reason: "Manual match progress update" });
      setProfile(res.data);
      setStatus(t("gamification.matchTracked"));
    } catch (error: unknown) {
      setStatus(errorMessage(error, "Could not track this match."));
    }
  }

  async function claim(rewardId: number) {
    try {
      await claimGamificationReward(rewardId);
      await load();
      setStatus(t("gamification.rewardClaimed"));
    } catch (error: unknown) {
      setStatus(errorMessage(error, "Could not claim this reward."));
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, []);

  return (
    <main className="theme-page-player min-h-screen px-4 py-8 sm:px-6 lg:px-10">
      <div className="mx-auto max-w-7xl space-y-8">
        <section className="theme-panel-strong overflow-hidden rounded-[2rem] p-6 sm:p-8">
          <p className="theme-accent text-xs font-black uppercase tracking-[0.34em]">{t("gamification.epic")}</p>
          <div className="mt-4 grid gap-8 xl:grid-cols-[1.05fr_0.95fr] xl:items-end">
            <div>
              <h1 className="theme-title text-4xl font-black sm:text-6xl">{t("gamification.title")}
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Page information" focusable={false}>{t("gamification.subtitle")}</TileInfo>
              </span></h1>
            </div>
            {profile && (
              <div className="theme-panel-soft rounded-3xl p-5">
                <div className="flex items-center justify-between gap-4">
                  <div>
                    <p className="theme-soft text-xs font-black uppercase tracking-[0.24em]">{profile.xp.title}</p>
                    <h2 className="theme-title mt-2 text-4xl font-black">Level {profile.xp.level}</h2>
                    <p className="theme-muted mt-1 text-sm">{profile.xp.xp} XP - {profile.xp.nextLevelXp} XP target</p>
                  </div>
                  <button onClick={trackMatch} className="rounded-2xl bg-emerald-500 px-5 py-3 text-sm font-black text-white shadow-lg shadow-emerald-500/20">{t("gamification.trackMatch")}</button>
                </div>
                <div className="mt-5 h-3 overflow-hidden rounded-full bg-slate-200 dark:bg-white/10">
                  <div className="h-full rounded-full bg-gradient-to-r from-emerald-400 to-cyan-400" style={{ width: `${profile.xp.progressPercent}%` }} />
                </div>
              </div>
            )}
          </div>
        </section>

        {loading && <p className="theme-muted py-12 text-center">{t("common.loading")}</p>}
        {status && <div className="theme-panel-soft rounded-2xl p-4 text-sm font-semibold text-emerald-700 dark:text-emerald-200">{status}</div>}

        {!loading && profile && (
          <>
            <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <Metric label={t("gamification.metric.xp")} value={profile.xp.xp} />
              <Metric label={t("gamification.metric.level")} value={profile.xp.level} />
              <Metric label={t("gamification.metric.streak")} value={profile.streaks[0]?.currentStreak ?? 0} />
              <Metric label={t("gamification.metric.badges")} value={profile.badges.length} />
            </section>

            {performance && (
              <>
                <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
                  <Metric label="Tracked matches" value={performance.career.matches} />
                  <Metric label="Career runs" value={performance.batting.runs} />
                  <Metric label="Career wickets" value={performance.bowling.wickets} />
                  <Metric label="Impact score" value={`${performance.career.impactScore}/100`} />
                </section>

                <section className="grid gap-6 xl:grid-cols-[1fr_1fr]">
                  <Panel title="Cricket Profile" body={performance.insight}>
                    <div className="grid gap-3 sm:grid-cols-2">
                      <Detail label="Player" value={performance.profile.name} />
                      <Detail label="City" value={performance.profile.city || "Not set"} />
                      <Detail label="Role" value={performance.profile.position || "Not set"} />
                      <Detail label="Reputation" value={`${performance.career.reputationScore}/100`} />
                    </div>
                  </Panel>

                  <Panel title="Batting & Bowling" body="Aggregated from completed live scorecards.">
                    <div className="grid gap-3 sm:grid-cols-2">
                      <Detail label="Highest score" value={performance.batting.highestScore} />
                      <Detail label="Strike rate" value={performance.batting.strikeRate} />
                      <Detail label="Best bowling" value={performance.bowling.bestFigures} />
                      <Detail label="Economy" value={performance.bowling.economyRate} />
                    </div>
                  </Panel>
                </section>

                <section className="grid gap-6 xl:grid-cols-3">
                  <Panel title="Recent Matches" body="Latest scored matches connected to your cricket profile.">
                    <div className="space-y-3">
                      {performance.recentMatches.length === 0 && <p className="theme-muted rounded-2xl border border-dashed p-6 text-center">No completed scored matches yet.</p>}
                      {performance.recentMatches.map((match) => (
                        <div key={`${match.liveScoreId}-${match.playedAt}`} className="theme-panel-soft rounded-2xl p-4">
                          <p className="theme-title font-black">{match.title}</p>
                          <p className="theme-muted mt-1 text-xs">{match.result} - {match.runs} runs - {match.wickets} wickets</p>
                        </div>
                      ))}
                    </div>
                  </Panel>

                  <Panel title="Career Achievements" body="Unlocked from scored match history.">
                    <div className="space-y-3">
                      {performance.achievements.map((achievement) => (
                        <div key={achievement.code} className={`rounded-2xl border p-4 ${achievement.unlocked ? "border-emerald-400/40 bg-emerald-500/10" : "theme-panel-soft"}`}>
                          <p className="theme-title font-black">{achievement.unlocked ? "Unlocked" : "Locked"} - {achievement.name}</p>
                          <p className="theme-muted mt-1 text-xs">{achievement.progress}/{achievement.target} - {achievement.description}</p>
                        </div>
                      ))}
                    </div>
                  </Panel>

                  <Panel title="Impact Leaderboard" body="Career ranking from live score performance.">
                    <div className="space-y-3">
                      {performanceLeaderboard.map((entry) => (
                        <div key={entry.userId} className="theme-panel-soft flex items-center justify-between rounded-2xl p-4">
                          <p className="theme-title font-black">#{entry.rank} {entry.playerName}</p>
                          <p className="theme-muted text-sm">{entry.statLabel}</p>
                        </div>
                      ))}
                    </div>
                  </Panel>
                </section>
              </>
            )}

            <section className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
              <Panel title={t("gamification.challenges")} body={profile.insight}>
                <div className="space-y-4">
                  {profile.challenges.map((challenge) => (
                    <article key={challenge.id} className="theme-panel-soft rounded-3xl p-5">
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <h3 className="theme-title font-black">{challenge.title}</h3>
                          <p className="theme-muted mt-1 text-sm">{challenge.description}</p>
                        </div>
                        <span className="theme-badge rounded-full px-3 py-1 text-xs font-black">{challenge.status}</span>
                      </div>
                      <div className="mt-4 h-2 overflow-hidden rounded-full bg-slate-200 dark:bg-white/10">
                        <div className="h-full rounded-full bg-emerald-500" style={{ width: `${challenge.progressPercent}%` }} />
                      </div>
                      <p className="theme-muted mt-2 text-xs">{challenge.progress}/{challenge.target} - +{challenge.xpReward} XP</p>
                    </article>
                  ))}
                </div>
              </Panel>

              <Panel title={t("gamification.achievements")} body={t("gamification.achievementsBody")}>
                <div className="grid gap-4 md:grid-cols-2">
                  {profile.achievements.map((achievement) => (
                    <article key={achievement.id} className={`rounded-3xl border p-5 ${achievement.earned ? "border-emerald-400/40 bg-emerald-500/10" : "theme-panel-soft"}`}>
                      <p className="theme-title font-black">{achievement.earned ? "Unlocked" : "Locked"} - {achievement.name}</p>
                      <p className="theme-muted mt-2 text-sm">{achievement.category} - +{achievement.xpReward} XP</p>
                    </article>
                  ))}
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 xl:grid-cols-3">
              <Panel title={t("gamification.rewards")} body={t("gamification.rewardsBody")}>
                <div className="space-y-3">
                  {profile.rewards.length === 0 && <p className="theme-muted rounded-2xl border border-dashed p-6 text-center">{t("gamification.emptyRewards")}</p>}
                  {profile.rewards.map((reward) => (
                    <div key={reward.id} className="theme-panel-soft rounded-2xl p-4">
                      <p className="theme-title font-black">{reward.label}</p>
                      <p className="theme-muted mt-1 text-xs">{reward.rewardType} - {reward.status}</p>
                      {reward.status === "AVAILABLE" && <button onClick={() => claim(reward.id)} className="mt-3 rounded-xl bg-emerald-500 px-4 py-2 text-xs font-black text-white">{t("gamification.claim")}</button>}
                    </div>
                  ))}
                </div>
              </Panel>

              <Panel title={t("gamification.badges")} body={t("gamification.badgesBody")}>
                <div className="flex flex-wrap gap-2">
                  {profile.badges.length === 0 && <p className="theme-muted text-sm">{t("gamification.emptyBadges")}</p>}
                  {profile.badges.map((badge) => <span key={badge.id} className="theme-badge rounded-full px-3 py-2 text-xs font-black">{badge.badge}</span>)}
                </div>
              </Panel>

              <Panel title={t("gamification.leaderboard")} body={t("gamification.leaderboardBody")}>
                <div className="space-y-3">
                  {profile.leaderboard.map((entry) => (
                    <div key={entry.userId} className="theme-panel-soft flex items-center justify-between rounded-2xl p-4">
                      <p className="theme-title font-black">#{entry.rank} {entry.playerName}</p>
                      <p className="theme-muted text-sm">{entry.score} XP</p>
                    </div>
                  ))}
                </div>
              </Panel>
            </section>
          </>
        )}
      </div>
    </main>
  );
}

function Panel({ title, body, children }: { title: string; body: string; children: React.ReactNode }) {
  return (
    <section className="theme-panel rounded-3xl p-6">
      <h2 className="theme-title text-xl font-black">{title}</h2>
      <p className="theme-muted mt-1 text-sm">{body}</p>
      <div className="mt-5">{children}</div>
    </section>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="theme-panel rounded-3xl p-5">
      <p className="theme-soft text-xs font-black uppercase tracking-[0.24em]">{label}</p>
      <p className="theme-title mt-4 text-3xl font-black">{value}</p>
    </div>
  );
}

function Detail({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="theme-panel-soft rounded-2xl p-4">
      <p className="theme-soft text-[0.65rem] font-black uppercase tracking-[0.2em]">{label}</p>
      <p className="theme-title mt-2 text-lg font-black">{value}</p>
    </div>
  );
}

function errorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { message?: string; error?: string } } };
  return apiError.response?.data?.message || apiError.response?.data?.error || fallback;
}
