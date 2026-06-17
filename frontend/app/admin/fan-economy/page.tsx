"use client";

import { useEffect, useMemo, useState } from "react";
import {
  createAdminFanCollectible,
  createAdminPredictionGame,
  FanEconomyMap,
  getAdminFanEconomyAudit,
  getAdminFanEconomyDashboard,
  getAdminFanEconomyFans,
  scoreAdminPredictionEntry,
} from "@/modules/faneconomy/services/fanEconomyService";
import { CompactRow, DataTable, errorMessage, Field, formatNumber, MetricCard, Panel, SelectField } from "@/modules/faneconomy/components/FanEconomyUi";

const defaultCollectible = {
  title: "",
  collectibleType: "CARD",
  rarity: "COMMON",
  sourceModule: "FAN_ECONOMY",
  supplyLimit: "100",
  mediaUrl: "",
  credentialReference: "",
};

const defaultPrediction = {
  title: "",
  sport: "CRICKET",
  targetType: "MATCH",
  predictionType: "MATCH_WINNER",
  pointsForCorrect: "100",
  deadlineAt: "",
  settlementRule: "",
};

export default function AdminFanEconomyPage() {
  const [dashboard, setDashboard] = useState<FanEconomyMap | null>(null);
  const [fans, setFans] = useState<FanEconomyMap[]>([]);
  const [audit, setAudit] = useState<FanEconomyMap[]>([]);
  const [collectibleDraft, setCollectibleDraft] = useState(defaultCollectible);
  const [predictionDraft, setPredictionDraft] = useState(defaultPrediction);
  const [selectedEntryId, setSelectedEntryId] = useState("");
  const [resultStatus, setResultStatus] = useState("CORRECT");
  const [settlementNote, setSettlementNote] = useState("Official result matched.");
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  async function loadFanEconomy() {
    try {
      setLoading(true);
      const [dashboardRes, fansRes, auditRes] = await Promise.all([getAdminFanEconomyDashboard(), getAdminFanEconomyFans(), getAdminFanEconomyAudit()]);
      setDashboard(dashboardRes.data);
      setFans(fansRes.data);
      setAudit(auditRes.data);
      const firstPending = (dashboardRes.data.predictionEntries || []).find((entry: FanEconomyMap) => String(entry.resultStatus) === "PENDING") || (dashboardRes.data.predictionEntries || [])[0];
      if (firstPending?.id) setSelectedEntryId(String(firstPending.id));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void loadFanEconomy(), 0);
    return () => window.clearTimeout(timer);
  }, []);

  const metrics = useMemo(
    () => [
      { label: "Fans", value: formatNumber(dashboard?.kpis?.fans), detail: `${formatNumber(dashboard?.kpis?.activeFans)} active` },
      { label: "Fan Clubs", value: formatNumber(dashboard?.kpis?.fanClubs), detail: `${formatNumber(dashboard?.kpis?.fanClubMembers)} members` },
      { label: "Collectibles", value: formatNumber(dashboard?.kpis?.collectibles), detail: `${formatNumber(dashboard?.kpis?.mintedCollectibles)} minted` },
      { label: "Predictions", value: formatNumber(dashboard?.kpis?.predictionGames), detail: `${formatNumber(dashboard?.predictionEntries?.length)} scored or pending entries` },
      { label: "Marketplace", value: formatNumber(dashboard?.kpis?.marketplaceListings), detail: `Rs ${formatNumber(dashboard?.kpis?.marketplaceValue)} active value` },
      { label: "Audit Rows", value: formatNumber(audit.length), detail: "fan economy governance events" },
    ],
    [dashboard, audit.length]
  );

  async function runAction(action: () => Promise<unknown>, message: string) {
    setSaving(true);
    setStatus("");
    try {
      await action();
      setStatus(message);
      await loadFanEconomy();
    } catch (error) {
      setStatus(errorMessage(error, "Fan economy admin action failed."));
    } finally {
      setSaving(false);
    }
  }

  const entryId = Number(selectedEntryId);

  return (
    <main className="min-h-screen bg-slate-950 px-4 py-8 text-white sm:px-8" data-i18n-skip="true">
      <div className="mx-auto max-w-7xl space-y-8">
        <header className="rounded-[2rem] border border-white/10 bg-gradient-to-br from-emerald-500/15 via-slate-900 to-cyan-500/10 p-6 shadow-2xl">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.26em] text-emerald-300">EPIC 35 Fan Economy Governance</p>
              <h1 className="mt-3 text-3xl font-black text-white sm:text-4xl">Digital collectibles, prediction games, marketplace and fan ranking controls</h1>
              <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-300">
                Govern fan profiles, club growth, limited collectible supply, prediction scoring, marketplace transfers, creator content and watch party participation.
              </p>
            </div>
            <button onClick={() => void loadFanEconomy()} className="rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-emerald-300">
              Refresh Fan Economy
            </button>
          </div>
        </header>

        {status ? <p className="rounded-2xl border border-emerald-400/20 bg-emerald-400/10 px-4 py-3 text-sm font-bold text-emerald-200">{status}</p> : null}

        {loading ? (
          <section className="rounded-[2rem] border border-white/10 bg-white/[0.04] p-10 text-center text-sm font-bold text-slate-300">Loading fan economy governance...</section>
        ) : (
          <>
            <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {metrics.map((metric) => (
                <MetricCard key={metric.label} {...metric} />
              ))}
            </section>

            <section className="grid gap-6 xl:grid-cols-[1fr_1fr]">
              <Panel title="Create Digital Collectible" subtitle="Create cards, moments, badges and digital merch with rarity and supply controls.">
                <div className="grid gap-3 sm:grid-cols-2">
                  <Field label="Title" value={collectibleDraft.title} onChange={(value) => setCollectibleDraft((current) => ({ ...current, title: value }))} placeholder="Final winning moment" />
                  <SelectField label="Type" value={collectibleDraft.collectibleType} onChange={(value) => setCollectibleDraft((current) => ({ ...current, collectibleType: value }))} options={["CARD", "MOMENT", "BADGE", "DIGITAL_MERCH", "TROPHY"]} />
                  <SelectField label="Rarity" value={collectibleDraft.rarity} onChange={(value) => setCollectibleDraft((current) => ({ ...current, rarity: value }))} options={["COMMON", "RARE", "EPIC", "LEGENDARY"]} />
                  <Field label="Supply limit" type="number" value={collectibleDraft.supplyLimit} onChange={(value) => setCollectibleDraft((current) => ({ ...current, supplyLimit: value }))} />
                  <Field label="Media URL" value={collectibleDraft.mediaUrl} onChange={(value) => setCollectibleDraft((current) => ({ ...current, mediaUrl: value }))} />
                  <Field label="Credential reference" value={collectibleDraft.credentialReference} onChange={(value) => setCollectibleDraft((current) => ({ ...current, credentialReference: value }))} />
                </div>
                <button
                  disabled={saving}
                  onClick={() => void runAction(() => createAdminFanCollectible({ ...collectibleDraft, supplyLimit: Number(collectibleDraft.supplyLimit || 1) }), "Collectible created.")}
                  className="mt-4 w-full rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-emerald-300 disabled:opacity-60"
                >
                  Create Collectible
                </button>
              </Panel>

              <Panel title="Create Prediction Game" subtitle="Prediction games power fantasy-style engagement and fan ranking points.">
                <div className="grid gap-3 sm:grid-cols-2">
                  <Field label="Title" value={predictionDraft.title} onChange={(value) => setPredictionDraft((current) => ({ ...current, title: value }))} placeholder="Who wins the final?" />
                  <SelectField label="Sport" value={predictionDraft.sport} onChange={(value) => setPredictionDraft((current) => ({ ...current, sport: value }))} options={["CRICKET", "FOOTBALL", "BADMINTON", "TENNIS"]} />
                  <SelectField label="Target type" value={predictionDraft.targetType} onChange={(value) => setPredictionDraft((current) => ({ ...current, targetType: value }))} options={["MATCH", "TOURNAMENT", "PLAYER", "CUSTOM"]} />
                  <SelectField label="Prediction type" value={predictionDraft.predictionType} onChange={(value) => setPredictionDraft((current) => ({ ...current, predictionType: value }))} options={["MATCH_WINNER", "TOP_SCORER", "SCORE_RANGE", "PLAYER_OF_MATCH", "CUSTOM"]} />
                  <Field label="Points" type="number" value={predictionDraft.pointsForCorrect} onChange={(value) => setPredictionDraft((current) => ({ ...current, pointsForCorrect: value }))} />
                  <Field label="Deadline" type="datetime-local" value={predictionDraft.deadlineAt} onChange={(value) => setPredictionDraft((current) => ({ ...current, deadlineAt: value }))} />
                  <Field label="Settlement rule" value={predictionDraft.settlementRule} onChange={(value) => setPredictionDraft((current) => ({ ...current, settlementRule: value }))} />
                </div>
                <button
                  disabled={saving}
                  onClick={() =>
                    void runAction(
                      () => createAdminPredictionGame({ ...predictionDraft, pointsForCorrect: Number(predictionDraft.pointsForCorrect || 1), deadlineAt: predictionDraft.deadlineAt || undefined }),
                      "Prediction game created."
                    )
                  }
                  className="mt-4 w-full rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-emerald-300 disabled:opacity-60"
                >
                  Create Prediction
                </button>
              </Panel>
            </section>

            <section className="grid gap-6 xl:grid-cols-[0.8fr_1.2fr]">
              <Panel title="Score Prediction Entry" subtitle="Awards rank points once and records an audit event.">
                <div className="grid gap-3">
                  <SelectField label="Entry id" value={selectedEntryId} onChange={setSelectedEntryId} options={(dashboard?.predictionEntries || []).map((entry: FanEconomyMap) => String(entry.id))} />
                  <SelectField label="Result" value={resultStatus} onChange={setResultStatus} options={["CORRECT", "PARTIAL", "WRONG", "VOID"]} />
                  <Field label="Settlement note" value={settlementNote} onChange={setSettlementNote} />
                </div>
                <button
                  disabled={saving || !entryId}
                  onClick={() => void runAction(() => scoreAdminPredictionEntry(entryId, { resultStatus, settlementNote }), "Prediction entry scored.")}
                  className="mt-4 w-full rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-emerald-300 disabled:opacity-60"
                >
                  Score Entry
                </button>
              </Panel>

              <Panel title="Governance Controls" subtitle="Controls mapped to supply, ownership, predictions, capacity and auditability.">
                <div className="grid gap-3 sm:grid-cols-2">
                  {(dashboard?.economyControls || []).map((item: FanEconomyMap) => (
                    <CompactRow key={String(item.name)} title={String(item.name)} meta={String(item.detail)} status={String(item.status)} />
                  ))}
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 lg:grid-cols-3">
              <Panel title="Fans" subtitle="Fan profile and ranking state.">
                <DataTable rows={fans} columns={[["fanCode", "Fan"], ["displayName", "Name"], ["rankPoints", "Rank"], ["status", "Status"]]} empty="No fans yet." />
              </Panel>
              <Panel title="Prediction Entries" subtitle="Pending and scored prediction entries.">
                <DataTable rows={dashboard?.predictionEntries || []} columns={[["id", "Entry"], ["gameId", "Game"], ["fanProfileId", "Fan"], ["resultStatus", "Result"], ["pointsAwarded", "Points"]]} empty="No prediction entries yet." />
              </Panel>
              <Panel title="Audit" subtitle="Immutable fan economy operations.">
                <DataTable rows={audit} columns={[["action", "Action"], ["entityType", "Entity"], ["actor", "Actor"], ["createdAt", "Created"]]} empty="No audit rows yet." />
              </Panel>
            </section>
          </>
        )}
      </div>
    </main>
  );
}
