"use client";

import { useEffect, useMemo, useState } from "react";
import {
  SuperAppMap,
  addSportsTimelineEvent,
  createSportsIdentity,
  createUnifiedMembership,
  creditUniversalWallet,
  earnUniversalRewards,
  generateSportsPassport,
  getSuperAppDashboard,
  recordReputationEvent,
  searchSuperAppIdentities,
} from "@/modules/superapp/services/superAppService";

const defaultIdentity = {
  displayName: "",
  primaryEmail: "",
  phone: "",
  city: "",
  primarySport: "CRICKET",
  ageGroup: "OPEN",
  profileVisibility: "PUBLIC",
};

export default function SuperAppPage() {
  const [dashboard, setDashboard] = useState<SuperAppMap | null>(null);
  const [identityDraft, setIdentityDraft] = useState(defaultIdentity);
  const [selectedIdentityId, setSelectedIdentityId] = useState("");
  const [walletAmount, setWalletAmount] = useState(500);
  const [rewardPoints, setRewardPoints] = useState(100);
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<SuperAppMap[]>([]);
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  async function loadDashboard() {
    try {
      setLoading(true);
      const response = await getSuperAppDashboard();
      setDashboard(response.data);
      const firstIdentity = response.data.myIdentity || response.data.identities?.[0];
      if (firstIdentity?.id) {
        setSelectedIdentityId(String(firstIdentity.id));
      }
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void loadDashboard(), 0);
    return () => window.clearTimeout(timer);
  }, []);

  const identities = useMemo(() => (Array.isArray(dashboard?.identities) ? dashboard?.identities : []), [dashboard]);
  const wallets = useMemo(() => (Array.isArray(dashboard?.wallets) ? dashboard?.wallets : []), [dashboard]);
  const memberships = useMemo(() => (Array.isArray(dashboard?.memberships) ? dashboard?.memberships : []), [dashboard]);
  const rewards = useMemo(() => (Array.isArray(dashboard?.rewards) ? dashboard?.rewards : []), [dashboard]);
  const timeline = useMemo(() => (Array.isArray(dashboard?.timeline) ? dashboard?.timeline : []), [dashboard]);
  const controls = useMemo(() => (Array.isArray(dashboard?.priorityControls) ? dashboard?.priorityControls : []), [dashboard]);
  const selectedIdentity = identities.find((identity) => String(identity.id) === selectedIdentityId) || dashboard?.myIdentity || identities[0];
  const selectedWallet = wallets.find((wallet) => String(wallet.identityId) === String(selectedIdentity?.id));
  const selectedReward = rewards.find((reward) => String(reward.identityId) === String(selectedIdentity?.id));
  const selectedMembership = memberships.find((membership) => String(membership.identityId) === String(selectedIdentity?.id));

  async function runAction(action: () => Promise<unknown>, message: string) {
    setSaving(true);
    setStatus("");
    try {
      await action();
      setStatus(message);
      await loadDashboard();
    } catch (error) {
      setStatus(errorMessage(error, "Super App action failed."));
    } finally {
      setSaving(false);
    }
  }

  async function handleCreateIdentity() {
    if (!identityDraft.displayName.trim() || !identityDraft.primaryEmail.trim()) {
      setStatus("Display name and email are required.");
      return;
    }
    await runAction(async () => {
      const response = await createSportsIdentity(identityDraft);
      setSelectedIdentityId(String(response.data.id || ""));
      setIdentityDraft(defaultIdentity);
    }, "Sports Identity created and linked to universal wallet/rewards.");
  }

  async function handleSearch() {
    if (!searchQuery.trim()) {
      setSearchResults([]);
      return;
    }
    const response = await searchSuperAppIdentities(searchQuery.trim());
    setSearchResults(response.data);
  }

  const identityId = Number(selectedIdentityId || selectedIdentity?.id);

  return (
    <main className="theme-page-player min-h-screen px-4 py-8 sm:px-6" data-i18n-skip="true">
      <div className="mx-auto max-w-7xl space-y-6">
        <section className="theme-card rounded-[2rem] border p-6 shadow-2xl">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.26em] text-emerald-300">EPIC 33 Sports Super App</p>
              <h1 className="mt-3 text-3xl font-black theme-title sm:text-4xl">One identity, wallet, membership, rewards and sports passport</h1>
              <p className="mt-3 max-w-3xl text-sm leading-6 theme-soft">
                Unify bookings, tournaments, academy, career, governance, rewards, membership, reputation, family accounts, and timeline into one sports profile.
              </p>
            </div>
            <button onClick={() => void loadDashboard()} className="rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-black text-white hover:bg-emerald-400">
              Refresh Super App
            </button>
          </div>
        </section>

        {status ? <p className="theme-card rounded-2xl border border-emerald-400/20 px-4 py-3 text-sm font-bold text-emerald-200">{status}</p> : null}

        {loading ? (
          <section className="theme-card rounded-[2rem] border p-10 text-center text-sm font-bold theme-soft">Loading Super App ecosystem...</section>
        ) : (
          <>
            <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <MetricCard label="Sports IDs" value={formatNumber(dashboard?.kpis?.sportsIdentities)} detail={`${formatNumber(dashboard?.kpis?.activeIdentities)} active`} />
              <MetricCard label="Public Passports" value={formatNumber(dashboard?.kpis?.publicPassports)} detail="shareable sports profile" />
              <MetricCard label="Wallets" value={formatNumber(dashboard?.kpis?.walletAccounts)} detail="universal balance accounts" />
              <MetricCard label="Reputation" value={`${formatNumber(dashboard?.kpis?.averageReputation)}/100`} detail="ecosystem trust score" />
            </section>

            <section className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
              <Panel title="Create Sports Identity" subtitle="Auto-generates a globally unique Sports ID and creates wallet/rewards accounts.">
                <div className="grid gap-3 sm:grid-cols-2">
                  <Field label="Display name" value={identityDraft.displayName} onChange={(value) => setIdentityDraft((current) => ({ ...current, displayName: value }))} placeholder="Player name" />
                  <Field label="Email" value={identityDraft.primaryEmail} onChange={(value) => setIdentityDraft((current) => ({ ...current, primaryEmail: value }))} placeholder="player@example.com" />
                  <Field label="Phone" value={identityDraft.phone} onChange={(value) => setIdentityDraft((current) => ({ ...current, phone: value }))} placeholder="9999999999" />
                  <Field label="City" value={identityDraft.city} onChange={(value) => setIdentityDraft((current) => ({ ...current, city: value }))} placeholder="Ahmedabad" />
                  <Select label="Sport" value={identityDraft.primarySport} onChange={(value) => setIdentityDraft((current) => ({ ...current, primarySport: value }))} options={["CRICKET", "FOOTBALL", "BADMINTON", "TENNIS", "BASKETBALL"]} />
                  <Select label="Age group" value={identityDraft.ageGroup} onChange={(value) => setIdentityDraft((current) => ({ ...current, ageGroup: value }))} options={["OPEN", "U12", "U16", "U19", "YOUTH"]} />
                </div>
                <button onClick={() => void handleCreateIdentity()} disabled={saving} className="mt-4 w-full rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-black text-white hover:bg-emerald-400 disabled:opacity-60">
                  Create Sports Identity
                </button>
              </Panel>

              <Panel title="Identity Command Center" subtitle="Select a Sports ID and operate passport, wallet, membership, rewards, reputation and timeline.">
                <Select label="Sports Identity" value={String(selectedIdentity?.id || "")} onChange={setSelectedIdentityId} options={identities.map((identity) => ({ label: `${identity.displayName} - ${identity.sportsId}`, value: String(identity.id) }))} />
                {selectedIdentity ? (
                  <div className="mt-4 rounded-3xl border border-white/10 bg-white/[0.04] p-4">
                    <p className="text-lg font-black theme-title">{String(selectedIdentity.displayName)}</p>
                    <p className="mt-1 text-xs font-black uppercase tracking-[0.18em] text-emerald-300">{String(selectedIdentity.sportsId)}</p>
                    <div className="mt-4 grid gap-3 sm:grid-cols-3">
                      <MiniMetric label="Identity" value={`${formatNumber(selectedIdentity.identityScore)}/100`} />
                      <MiniMetric label="Reputation" value={`${formatNumber(selectedIdentity.reputationScore)}/100`} />
                      <MiniMetric label="Wallet" value={`Rs ${formatNumber(selectedWallet?.balance)}`} />
                      <MiniMetric label="Rewards" value={`${formatNumber(selectedReward?.pointsBalance)} pts`} />
                      <MiniMetric label="Membership" value={String(selectedMembership?.tier || "FREE")} />
                      <MiniMetric label="Sport" value={String(selectedIdentity.primarySport || "--")} />
                    </div>
                  </div>
                ) : (
                  <p className="mt-4 text-sm font-bold theme-soft">Create or search a Sports Identity to unlock Super App actions.</p>
                )}
                <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
                  <ActionButton label="Generate Passport" disabled={!identityId || saving} onClick={() => runAction(() => generateSportsPassport(identityId, { visibility: "PUBLIC" }), "Sports Passport generated.")} />
                  <ActionButton label={`Credit Rs ${walletAmount}`} disabled={!identityId || saving} onClick={() => runAction(() => creditUniversalWallet(identityId, { amount: walletAmount, sourceModule: "SUPER_APP", note: "Manual super app credit" }), "Universal wallet credited.")} />
                  <ActionButton label={`Earn ${rewardPoints} pts`} disabled={!identityId || saving} onClick={() => runAction(() => earnUniversalRewards(identityId, { points: rewardPoints, sourceModule: "SUPER_APP", note: "Manual super app reward" }), "Universal rewards credited.")} />
                  <ActionButton label="Activate Gold" disabled={!identityId || saving} onClick={() => runAction(() => createUnifiedMembership(identityId, { tier: "GOLD", billingCycle: "MONTHLY", price: 999, autoRenew: true }), "Unified membership activated.")} />
                  <ActionButton label="+5 Reputation" disabled={!identityId || saving} onClick={() => runAction(() => recordReputationEvent(identityId, { eventType: "GOOD_STANDING", sourceModule: "SUPER_APP", impact: 5, note: "Manual good standing update" }), "Reputation updated.")} />
                  <ActionButton label="Add Timeline" disabled={!identityId || saving} onClick={() => runAction(() => addSportsTimelineEvent(identityId, { eventType: "SUPER_APP_MILESTONE", sourceModule: "SUPER_APP", title: "Super App profile updated", visibility: "PRIVATE" }), "Timeline updated.")} />
                </div>
                <div className="mt-4 grid gap-3 sm:grid-cols-2">
                  <Field label="Wallet amount" type="number" value={String(walletAmount)} onChange={(value) => setWalletAmount(Number(value || 0))} />
                  <Field label="Reward points" type="number" value={String(rewardPoints)} onChange={(value) => setRewardPoints(Number(value || 0))} />
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 lg:grid-cols-3">
              <Panel title="Sports Passport & Controls" subtitle="Sharing, privacy, uniqueness, youth guardrails and double-spend protection.">
                <div className="space-y-3">
                  {controls.map((item) => (
                    <CompactRow key={String(item.control)} title={String(item.control)} meta={String(item.detail)} status={String(item.status)} />
                  ))}
                </div>
              </Panel>

              <Panel title="Unified Search" subtitle="Search Sports IDs across player, academy, tournament and governance contexts.">
                <div className="flex gap-2">
                  <input value={searchQuery} onChange={(event) => setSearchQuery(event.target.value)} placeholder="Search name, email or Sports ID" className="theme-input min-w-0 flex-1 rounded-2xl px-4 py-3 text-sm outline-none" />
                  <button onClick={() => void handleSearch()} className="rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-black text-white">Search</button>
                </div>
                <div className="mt-4 space-y-3">
                  {searchResults.map((item) => (
                    <button key={String(item.id)} onClick={() => setSelectedIdentityId(String(item.id))} className="theme-card w-full rounded-2xl border p-4 text-left">
                      <p className="font-black theme-title">{String(item.displayName)}</p>
                      <p className="mt-1 text-xs font-bold theme-soft">{String(item.sportsId)} - {String(item.primarySport)} - {String(item.city || "Any city")}</p>
                    </button>
                  ))}
                  {!searchResults.length ? <p className="text-sm font-bold theme-soft">No search results loaded yet.</p> : null}
                </div>
              </Panel>

              <Panel title="Timeline" subtitle="Unified activity stream across booking, tournaments, rewards and profile milestones.">
                <div className="space-y-3">
                  {timeline.slice(0, 8).map((item) => (
                    <CompactRow key={String(item.id)} title={String(item.title)} meta={`${String(item.sourceModule)} | ${String(item.occurredAt || "").slice(0, 16)}`} status={String(item.eventType)} />
                  ))}
                  {!timeline.length ? <p className="text-sm font-bold theme-soft">Timeline events will appear after actions are recorded.</p> : null}
                </div>
              </Panel>
            </section>
          </>
        )}
      </div>
    </main>
  );
}

function Panel({ title, subtitle, children }: { title: string; subtitle: string; children: React.ReactNode }) {
  return (
    <section className="theme-card rounded-[2rem] border p-5">
      <h2 className="text-lg font-black theme-title">{title}</h2>
      <p className="mt-1 text-sm leading-6 theme-soft">{subtitle}</p>
      <div className="mt-5">{children}</div>
    </section>
  );
}

function MetricCard({ label, value, detail }: { label: string; value: string; detail: string }) {
  return (
    <div className="theme-card rounded-3xl border p-5">
      <p className="text-xs font-black uppercase tracking-[0.18em] theme-soft">{label}</p>
      <p className="mt-3 text-3xl font-black theme-title">{value}</p>
      <p className="mt-2 text-xs font-bold theme-soft">{detail}</p>
    </div>
  );
}

function MiniMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="theme-panel-soft rounded-2xl p-3">
      <p className="text-[10px] font-black uppercase tracking-[0.18em] theme-soft">{label}</p>
      <p className="mt-1 text-sm font-black theme-title">{value}</p>
    </div>
  );
}

function Field({ label, value, onChange, placeholder, type = "text" }: { label: string; value: string; onChange: (value: string) => void; placeholder?: string; type?: string }) {
  return (
    <label className="block">
      <span className="text-xs font-black theme-title">{label}</span>
      <input type={type} value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} className="theme-input mt-2 w-full rounded-2xl px-4 py-3 text-sm outline-none" />
    </label>
  );
}

function Select({ label, value, onChange, options }: { label: string; value: string; onChange: (value: string) => void; options: Array<string | { label: string; value: string }> }) {
  return (
    <label className="block">
      <span className="text-xs font-black theme-title">{label}</span>
      <select value={value} onChange={(event) => onChange(event.target.value)} className="theme-input mt-2 w-full rounded-2xl px-4 py-3 text-sm outline-none">
        {options.map((option) => {
          const normalized = typeof option === "string" ? { label: option, value: option } : option;
          return (
            <option key={normalized.value} value={normalized.value}>
              {normalized.label}
            </option>
          );
        })}
      </select>
    </label>
  );
}

function ActionButton({ label, onClick, disabled }: { label: string; onClick: () => void; disabled?: boolean }) {
  return (
    <button onClick={onClick} disabled={disabled} className="rounded-2xl border border-emerald-400/20 bg-emerald-400/10 px-3 py-3 text-xs font-black text-emerald-100 hover:bg-emerald-400/20 disabled:opacity-50">
      {label}
    </button>
  );
}

function CompactRow({ title, meta, status }: { title: string; meta: string; status: string }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/[0.04] p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <p className="text-sm font-black theme-title">{title}</p>
        <span className="rounded-full border border-emerald-400/20 bg-emerald-400/10 px-3 py-1 text-[10px] font-black uppercase tracking-[0.16em] text-emerald-200">{status}</span>
      </div>
      <p className="mt-2 text-xs leading-5 theme-soft">{meta}</p>
    </div>
  );
}

function formatNumber(value: unknown) {
  const numberValue = Number(value || 0);
  return Number.isFinite(numberValue) ? numberValue.toLocaleString("en-IN") : "0";
}

function errorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { message?: string; error?: string } } };
  return apiError.response?.data?.message || apiError.response?.data?.error || fallback;
}
