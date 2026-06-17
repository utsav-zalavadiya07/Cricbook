"use client";

import { useEffect, useMemo, useState } from "react";
import {
  claimFanCollectible,
  createFanClub,
  createFanProfile,
  createWatchParty,
  FanEconomyMap,
  getFanEconomyClubs,
  getFanEconomyCollectibles,
  getFanEconomyDashboard,
  getFanEconomyMarketplaceListings,
  getFanEconomyPredictions,
  getFanEconomyWatchParties,
  joinFanClub,
  joinWatchParty,
  submitFanPrediction,
} from "@/modules/faneconomy/services/fanEconomyService";
import { CompactRow, DataTable, errorMessage, Field, formatNumber, MetricCard, Panel, SelectField } from "@/modules/faneconomy/components/FanEconomyUi";

const defaultFan = {
  displayName: "",
  email: "",
  city: "Ahmedabad",
  favoriteSport: "CRICKET",
  favoriteTeam: "",
  favoritePlayer: "",
};

const defaultClub = {
  name: "",
  sport: "CRICKET",
  clubType: "TEAM",
  city: "Ahmedabad",
  visibility: "PUBLIC",
  description: "",
};

const defaultWatchParty = {
  title: "",
  sport: "CRICKET",
  platform: "IN_APP",
  capacity: "50",
  accessMode: "PUBLIC",
};

export default function FanEconomyPage() {
  const [dashboard, setDashboard] = useState<FanEconomyMap | null>(null);
  const [clubs, setClubs] = useState<FanEconomyMap[]>([]);
  const [collectibles, setCollectibles] = useState<FanEconomyMap[]>([]);
  const [predictions, setPredictions] = useState<FanEconomyMap[]>([]);
  const [listings, setListings] = useState<FanEconomyMap[]>([]);
  const [watchParties, setWatchParties] = useState<FanEconomyMap[]>([]);
  const [fanDraft, setFanDraft] = useState(defaultFan);
  const [clubDraft, setClubDraft] = useState(defaultClub);
  const [watchPartyDraft, setWatchPartyDraft] = useState(defaultWatchParty);
  const [activeFanId, setActiveFanId] = useState("");
  const [selectedClubId, setSelectedClubId] = useState("");
  const [selectedCollectibleId, setSelectedCollectibleId] = useState("");
  const [selectedPredictionId, setSelectedPredictionId] = useState("");
  const [selectedWatchPartyId, setSelectedWatchPartyId] = useState("");
  const [predictionValue, setPredictionValue] = useState("Team A wins");
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  async function loadFanEconomy() {
    try {
      setLoading(true);
      const [dashboardRes, clubsRes, collectiblesRes, predictionsRes, listingsRes, watchPartiesRes] = await Promise.all([
        getFanEconomyDashboard(),
        getFanEconomyClubs(),
        getFanEconomyCollectibles(),
        getFanEconomyPredictions(),
        getFanEconomyMarketplaceListings(),
        getFanEconomyWatchParties(),
      ]);
      setDashboard(dashboardRes.data);
      setClubs(clubsRes.data);
      setCollectibles(collectiblesRes.data);
      setPredictions(predictionsRes.data);
      setListings(listingsRes.data);
      setWatchParties(watchPartiesRes.data);
      if (clubsRes.data[0]?.id) setSelectedClubId(String(clubsRes.data[0].id));
      if (collectiblesRes.data[0]?.id) setSelectedCollectibleId(String(collectiblesRes.data[0].id));
      if (predictionsRes.data[0]?.id) setSelectedPredictionId(String(predictionsRes.data[0].id));
      if (watchPartiesRes.data[0]?.id) setSelectedWatchPartyId(String(watchPartiesRes.data[0].id));
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
      { label: "Fans", value: formatNumber(dashboard?.kpis?.fans), detail: `${formatNumber(dashboard?.kpis?.activeFans)} active profiles` },
      { label: "Fan Clubs", value: formatNumber(dashboard?.kpis?.fanClubs), detail: `${formatNumber(dashboard?.kpis?.fanClubMembers)} joined members` },
      { label: "Collectibles", value: formatNumber(dashboard?.kpis?.collectibles), detail: `${formatNumber(dashboard?.kpis?.mintedCollectibles)} minted` },
      { label: "Predictions", value: formatNumber(dashboard?.kpis?.predictionGames), detail: "live fan engagement games" },
      { label: "Marketplace", value: formatNumber(dashboard?.kpis?.marketplaceListings), detail: `Rs ${formatNumber(dashboard?.kpis?.marketplaceValue)} active value` },
      { label: "Watch Parties", value: formatNumber(dashboard?.kpis?.watchParties), detail: `${formatNumber(dashboard?.kpis?.watchPartyCapacity)} total capacity` },
    ],
    [dashboard]
  );

  async function runAction(action: () => Promise<unknown>, message: string) {
    setSaving(true);
    setStatus("");
    try {
      const result = await action();
      const data = result && typeof result === "object" && "data" in result ? (result as { data?: FanEconomyMap }).data : null;
      if (data?.id && !activeFanId && data.fanCode) {
        setActiveFanId(String(data.id));
      }
      setStatus(message);
      await loadFanEconomy();
    } catch (error) {
      setStatus(errorMessage(error, "Fan economy action failed."));
    } finally {
      setSaving(false);
    }
  }

  const fanId = Number(activeFanId);
  const clubId = Number(selectedClubId);
  const collectibleId = Number(selectedCollectibleId);
  const predictionId = Number(selectedPredictionId);
  const watchPartyId = Number(selectedWatchPartyId);

  return (
    <main className="theme-page-player min-h-screen px-4 py-8 sm:px-6" data-i18n-skip="true">
      <div className="mx-auto max-w-7xl space-y-6">
        <section className="theme-card rounded-[2rem] border p-6 shadow-2xl">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.26em] text-emerald-300">EPIC 35 Fan Economy</p>
              <h1 className="mt-3 text-3xl font-black theme-title sm:text-4xl">Fan clubs, collectibles, predictions and watch parties</h1>
              <p className="mt-3 max-w-3xl text-sm leading-6 theme-soft">
                Build a fan profile, join sports communities, claim limited digital collectibles, submit predictions, and coordinate live watch parties from one engagement layer.
              </p>
            </div>
            <button onClick={() => void loadFanEconomy()} className="rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-black text-white hover:bg-emerald-400">
              Refresh Fan Economy
            </button>
          </div>
        </section>

        {status ? <p className="theme-card rounded-2xl border border-emerald-400/20 px-4 py-3 text-sm font-bold text-emerald-200">{status}</p> : null}

        {loading ? (
          <section className="theme-card rounded-[2rem] border p-10 text-center text-sm font-bold theme-soft">Loading fan economy...</section>
        ) : (
          <>
            <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {metrics.map((metric) => (
                <MetricCard key={metric.label} {...metric} />
              ))}
            </section>

            <section className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
              <Panel title="Create Fan Profile" subtitle="Profile id is used for club joins, collectible claims, predictions and watch party participation.">
                <div className="grid gap-3 sm:grid-cols-2">
                  <Field label="Display name" value={fanDraft.displayName} onChange={(value) => setFanDraft((current) => ({ ...current, displayName: value }))} placeholder="Fan name" />
                  <Field label="Email" value={fanDraft.email} onChange={(value) => setFanDraft((current) => ({ ...current, email: value }))} placeholder="fan@example.com" />
                  <Field label="City" value={fanDraft.city} onChange={(value) => setFanDraft((current) => ({ ...current, city: value }))} />
                  <SelectField label="Favorite sport" value={fanDraft.favoriteSport} onChange={(value) => setFanDraft((current) => ({ ...current, favoriteSport: value }))} options={["CRICKET", "FOOTBALL", "BADMINTON", "TENNIS"]} />
                  <Field label="Favorite team" value={fanDraft.favoriteTeam} onChange={(value) => setFanDraft((current) => ({ ...current, favoriteTeam: value }))} />
                  <Field label="Favorite player" value={fanDraft.favoritePlayer} onChange={(value) => setFanDraft((current) => ({ ...current, favoritePlayer: value }))} />
                </div>
                <button
                  disabled={saving}
                  onClick={() => void runAction(() => createFanProfile(fanDraft), "Fan profile created.")}
                  className="mt-4 w-full rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-black text-white hover:bg-emerald-400 disabled:opacity-60"
                >
                  Create Fan Profile
                </button>
                <Field label="Active fan profile id" value={activeFanId} onChange={setActiveFanId} placeholder="Create or paste fan id" />
              </Panel>

              <Panel title="Fan Clubs and Creator Communities" subtitle="Create a public club, then join the selected club with the active fan profile.">
                <div className="grid gap-3 sm:grid-cols-2">
                  <Field label="Club name" value={clubDraft.name} onChange={(value) => setClubDraft((current) => ({ ...current, name: value }))} placeholder="Ahmedabad Super Fans" />
                  <SelectField label="Club type" value={clubDraft.clubType} onChange={(value) => setClubDraft((current) => ({ ...current, clubType: value }))} options={["TEAM", "PLAYER", "TOURNAMENT", "CITY", "SPORT"]} />
                  <SelectField label="Sport" value={clubDraft.sport} onChange={(value) => setClubDraft((current) => ({ ...current, sport: value }))} options={["CRICKET", "FOOTBALL", "BADMINTON", "TENNIS"]} />
                  <Field label="City" value={clubDraft.city} onChange={(value) => setClubDraft((current) => ({ ...current, city: value }))} />
                  <SelectField label="Join club id" value={selectedClubId} onChange={setSelectedClubId} options={clubs.map((club) => String(club.id))} />
                  <Field label="Description" value={clubDraft.description} onChange={(value) => setClubDraft((current) => ({ ...current, description: value }))} />
                </div>
                <div className="mt-4 grid gap-3 sm:grid-cols-2">
                  <button disabled={saving} onClick={() => void runAction(() => createFanClub(clubDraft), "Fan club created.")} className="rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-black text-white hover:bg-emerald-400 disabled:opacity-60">
                    Create Club
                  </button>
                  <button disabled={saving || !fanId || !clubId} onClick={() => void runAction(() => joinFanClub(clubId, fanId), "Fan club joined.")} className="rounded-2xl bg-white/10 px-4 py-3 text-sm font-black theme-title hover:bg-white/15 disabled:opacity-60">
                    Join Selected Club
                  </button>
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 xl:grid-cols-3">
              <Panel title="Claim Digital Collectible" subtitle="Supply-limited ownership records with serial numbers and transfer history.">
                <SelectField label="Collectible id" value={selectedCollectibleId} onChange={setSelectedCollectibleId} options={collectibles.map((item) => String(item.id))} />
                <button disabled={saving || !fanId || !collectibleId} onClick={() => void runAction(() => claimFanCollectible(collectibleId, fanId), "Collectible claimed.")} className="mt-4 w-full rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-black text-white hover:bg-emerald-400 disabled:opacity-60">
                  Claim Collectible
                </button>
                <DataTable rows={collectibles} columns={[["title", "Collectible"], ["rarity", "Rarity"], ["mintedCount", "Minted"], ["supplyLimit", "Supply"]]} empty="No collectibles available." />
              </Panel>

              <Panel title="Submit Prediction" subtitle="One prediction per fan per game before deadline.">
                <SelectField label="Prediction game id" value={selectedPredictionId} onChange={setSelectedPredictionId} options={predictions.map((item) => String(item.id))} />
                <Field label="Prediction" value={predictionValue} onChange={setPredictionValue} placeholder="Team A wins" />
                <button disabled={saving || !fanId || !predictionId} onClick={() => void runAction(() => submitFanPrediction(predictionId, fanId, predictionValue), "Prediction submitted.")} className="mt-4 w-full rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-black text-white hover:bg-emerald-400 disabled:opacity-60">
                  Submit Prediction
                </button>
                <DataTable rows={predictions} columns={[["title", "Game"], ["predictionType", "Type"], ["pointsForCorrect", "Points"], ["deadlineAt", "Deadline"]]} empty="No prediction games live." />
              </Panel>

              <Panel title="Watch Party" subtitle="Create or join a scheduled watch party with capacity enforcement.">
                <div className="grid gap-3">
                  <Field label="Title" value={watchPartyDraft.title} onChange={(value) => setWatchPartyDraft((current) => ({ ...current, title: value }))} placeholder="Final night watch party" />
                  <Field label="Capacity" type="number" value={watchPartyDraft.capacity} onChange={(value) => setWatchPartyDraft((current) => ({ ...current, capacity: value }))} />
                  <SelectField label="Watch party id" value={selectedWatchPartyId} onChange={setSelectedWatchPartyId} options={watchParties.map((item) => String(item.id))} />
                </div>
                <div className="mt-4 grid gap-3 sm:grid-cols-2">
                  <button disabled={saving || !fanId} onClick={() => void runAction(() => createWatchParty({ ...watchPartyDraft, hostFanProfileId: fanId, capacity: Number(watchPartyDraft.capacity || 1) }), "Watch party created.")} className="rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-black text-white hover:bg-emerald-400 disabled:opacity-60">
                    Create
                  </button>
                  <button disabled={saving || !fanId || !watchPartyId} onClick={() => void runAction(() => joinWatchParty(watchPartyId, fanId), "Watch party joined.")} className="rounded-2xl bg-white/10 px-4 py-3 text-sm font-black theme-title hover:bg-white/15 disabled:opacity-60">
                    Join
                  </button>
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 lg:grid-cols-3">
              <Panel title="Fan Ranking" subtitle="Leaderboard driven by prediction points and fan score.">
                <DataTable rows={dashboard?.fanRanking || []} columns={[["displayName", "Fan"], ["favoriteSport", "Sport"], ["rankPoints", "Rank"], ["fanScore", "Score"]]} empty="No ranked fans yet." />
              </Panel>
              <Panel title="Marketplace" subtitle="Open collectible listings and fan-to-fan transfers.">
                <DataTable rows={listings} columns={[["listingCode", "Listing"], ["price", "Price"], ["currency", "Currency"], ["status", "Status"]]} empty="No active listings." />
              </Panel>
              <Panel title="Controls" subtitle="Safety controls for economy integrity.">
                <div className="space-y-3">
                  {(dashboard?.economyControls || []).map((item: FanEconomyMap) => (
                    <CompactRow key={String(item.name)} title={String(item.name)} meta={String(item.detail)} status={String(item.status)} />
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
