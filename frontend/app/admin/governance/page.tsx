"use client";

import { useEffect, useMemo, useState } from "react";
import {
  AdminFederationGovernanceDashboard,
  AdminFederationGovernanceMap,
  createAdminFederationGovernanceAuction,
  createAdminFederationGovernanceBody,
  createAdminFederationGovernanceElection,
  createAdminFederationGovernanceFranchise,
  createAdminFederationGovernancePlayer,
  getAdminFederationGovernanceAudit,
  getAdminFederationGovernanceAuctions,
  getAdminFederationGovernanceBodies,
  getAdminFederationGovernanceCases,
  getAdminFederationGovernanceDashboard,
  getAdminFederationGovernanceElections,
  getAdminFederationGovernanceFranchises,
  getAdminFederationGovernanceIntegrityAlerts,
  getAdminFederationGovernanceLicenses,
  getAdminFederationGovernanceOfficials,
  getAdminFederationGovernancePlayers,
  getAdminFederationGovernanceRankings,
  getAdminFederationGovernanceTransfers,
  GovernanceAuctionPayload,
  GovernanceBodyPayload,
  GovernanceElectionPayload,
  GovernanceFranchisePayload,
  GovernancePlayerPayload,
  publishAdminFederationGovernanceElectionResults,
  updateAdminFederationGovernanceIntegrityAlertStatus,
  updateAdminFederationGovernancePlayerVerification,
  updateAdminFederationGovernanceTransferStatus,
} from "@/modules/admin/services/adminService";

const defaultBody: GovernanceBodyPayload = {
  name: "",
  bodyType: "NATIONAL_FEDERATION",
  sport: "CRICKET",
  region: "India",
  contactEmail: "",
};

const defaultFranchise: GovernanceFranchisePayload = {
  federationId: 1,
  name: "",
  ownerName: "",
  city: "",
  homeVenue: "",
  salaryCap: 0,
};

const defaultPlayer: GovernancePlayerPayload = {
  federationId: 1,
  playerName: "",
  email: "",
  sport: "CRICKET",
  city: "",
  ageGroup: "OPEN",
};

const defaultAuction: GovernanceAuctionPayload = {
  federationId: 1,
  playerRegistrationId: 1,
  auctionName: "",
  basePrice: 100000,
};

const defaultElection: GovernanceElectionPayload = {
  federationId: 1,
  title: "",
  positionName: "President",
  candidateNames: "",
};

export default function AdminGovernancePage() {
  const [dashboard, setDashboard] = useState<AdminFederationGovernanceDashboard | null>(null);
  const [bodies, setBodies] = useState<AdminFederationGovernanceMap[]>([]);
  const [franchises, setFranchises] = useState<AdminFederationGovernanceMap[]>([]);
  const [players, setPlayers] = useState<AdminFederationGovernanceMap[]>([]);
  const [auctions, setAuctions] = useState<AdminFederationGovernanceMap[]>([]);
  const [transfers, setTransfers] = useState<AdminFederationGovernanceMap[]>([]);
  const [licenses, setLicenses] = useState<AdminFederationGovernanceMap[]>([]);
  const [officials, setOfficials] = useState<AdminFederationGovernanceMap[]>([]);
  const [cases, setCases] = useState<AdminFederationGovernanceMap[]>([]);
  const [elections, setElections] = useState<AdminFederationGovernanceMap[]>([]);
  const [rankings, setRankings] = useState<AdminFederationGovernanceMap[]>([]);
  const [alerts, setAlerts] = useState<AdminFederationGovernanceMap[]>([]);
  const [audit, setAudit] = useState<AdminFederationGovernanceMap[]>([]);
  const [bodyDraft, setBodyDraft] = useState<GovernanceBodyPayload>(defaultBody);
  const [franchiseDraft, setFranchiseDraft] = useState<GovernanceFranchisePayload>(defaultFranchise);
  const [playerDraft, setPlayerDraft] = useState<GovernancePlayerPayload>(defaultPlayer);
  const [auctionDraft, setAuctionDraft] = useState<GovernanceAuctionPayload>(defaultAuction);
  const [electionDraft, setElectionDraft] = useState<GovernanceElectionPayload>(defaultElection);
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState("");

  async function loadGovernance() {
    try {
      setLoading(true);
      const [dashboardRes, bodiesRes, franchisesRes, playersRes, auctionsRes, transfersRes, licensesRes, officialsRes, casesRes, electionsRes, rankingsRes, alertsRes, auditRes] =
        await Promise.all([
          getAdminFederationGovernanceDashboard(),
          getAdminFederationGovernanceBodies(),
          getAdminFederationGovernanceFranchises(),
          getAdminFederationGovernancePlayers(),
          getAdminFederationGovernanceAuctions(),
          getAdminFederationGovernanceTransfers(),
          getAdminFederationGovernanceLicenses(),
          getAdminFederationGovernanceOfficials(),
          getAdminFederationGovernanceCases(),
          getAdminFederationGovernanceElections(),
          getAdminFederationGovernanceRankings(),
          getAdminFederationGovernanceIntegrityAlerts(),
          getAdminFederationGovernanceAudit(),
        ]);
      setDashboard(dashboardRes.data);
      setBodies(bodiesRes.data);
      setFranchises(franchisesRes.data);
      setPlayers(playersRes.data);
      setAuctions(auctionsRes.data);
      setTransfers(transfersRes.data);
      setLicenses(licensesRes.data);
      setOfficials(officialsRes.data);
      setCases(casesRes.data);
      setElections(electionsRes.data);
      setRankings(rankingsRes.data);
      setAlerts(alertsRes.data);
      setAudit(auditRes.data);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void loadGovernance(), 0);
    return () => window.clearTimeout(timer);
  }, []);

  const metrics = useMemo(
    () => [
      { label: "Federations", value: dashboard?.kpis?.federations ?? 0, detail: `${dashboard?.kpis?.associations ?? 0} associations` },
      { label: "Registered Players", value: dashboard?.kpis?.registeredPlayers ?? players.length, detail: `${dashboard?.kpis?.verifiedPlayers ?? 0} verified` },
      { label: "Franchises", value: dashboard?.kpis?.franchises ?? franchises.length, detail: `${dashboard?.kpis?.activeDrafts ?? 0} active drafts` },
      { label: "Live Auctions", value: dashboard?.kpis?.liveAuctions ?? 0, detail: `${auctions.length} auction records` },
      { label: "Transfers", value: dashboard?.kpis?.pendingTransfers ?? 0, detail: "pending approvals" },
      { label: "Licenses", value: dashboard?.kpis?.activeLicenses ?? licenses.length, detail: "active lifecycle" },
      { label: "Open Cases", value: dashboard?.kpis?.openCases ?? cases.length, detail: "disciplinary workflow" },
      { label: "Integrity Alerts", value: dashboard?.kpis?.openIntegrityAlerts ?? alerts.length, detail: "open monitoring" },
    ],
    [alerts.length, auctions.length, cases.length, dashboard, franchises.length, licenses.length, players.length]
  );

  async function createBody() {
    if (!bodyDraft.name.trim()) {
      setStatus("Federation or association name is required.");
      return;
    }
    await createAdminFederationGovernanceBody(bodyDraft);
    setBodyDraft(defaultBody);
    setStatus("Federation body created.");
    await loadGovernance();
  }

  async function createFranchise() {
    if (!franchiseDraft.name.trim()) {
      setStatus("Franchise name is required.");
      return;
    }
    await createAdminFederationGovernanceFranchise(franchiseDraft);
    setFranchiseDraft(defaultFranchise);
    setStatus("Franchise created.");
    await loadGovernance();
  }

  async function registerPlayer() {
    if (!playerDraft.playerName.trim() || !playerDraft.email.trim()) {
      setStatus("Player name and email are required.");
      return;
    }
    await createAdminFederationGovernancePlayer(playerDraft);
    setPlayerDraft(defaultPlayer);
    setStatus("Central sports ID generated.");
    await loadGovernance();
  }

  async function createAuction() {
    if (!auctionDraft.auctionName.trim()) {
      setStatus("Auction name is required.");
      return;
    }
    await createAdminFederationGovernanceAuction(auctionDraft);
    setAuctionDraft(defaultAuction);
    setStatus("Player auction published.");
    await loadGovernance();
  }

  async function createElection() {
    if (!electionDraft.title.trim() || !electionDraft.candidateNames.trim()) {
      setStatus("Election title and candidates are required.");
      return;
    }
    await createAdminFederationGovernanceElection(electionDraft);
    setElectionDraft(defaultElection);
    setStatus("Election created.");
    await loadGovernance();
  }

  return (
    <main className="min-h-screen bg-slate-950 px-4 py-8 text-white sm:px-8" data-i18n-skip="true">
      <div className="mx-auto max-w-7xl space-y-8">
        <header className="rounded-[2rem] border border-white/10 bg-gradient-to-br from-emerald-500/15 via-slate-900 to-cyan-500/10 p-6 shadow-2xl">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.26em] text-emerald-300">EPIC 32 Federation, League & Sports Governance</p>
              <h1 className="mt-3 text-3xl font-black text-white sm:text-4xl">National Governance Control Tower</h1>
              <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-300">
                Manage federation hierarchy, franchise leagues, central player IDs, drafts, auctions, transfers, licenses, officials, disciplinary cases, elections, rankings, and integrity monitoring.
              </p>
            </div>
            <button onClick={() => void loadGovernance()} className="rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-emerald-300">
              Refresh Governance OS
            </button>
          </div>
        </header>

        {status ? <div className="rounded-2xl border border-emerald-400/20 bg-emerald-400/10 px-4 py-3 text-sm font-bold text-emerald-100">{status}</div> : null}

        {loading ? (
          <section className="rounded-[2rem] border border-white/10 bg-white/[0.04] p-10 text-center text-sm font-bold text-slate-300">Loading governance control plane...</section>
        ) : (
          <>
            <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              {metrics.map((metric) => (
                <MetricCard key={metric.label} {...metric} />
              ))}
            </section>

            <section className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
              <Panel title="Create Federation Body" subtitle="Configure national federations, state associations, district associations, university leagues, and corporate league bodies.">
                <div className="grid gap-3 sm:grid-cols-2">
                  <Field label="Name" value={bodyDraft.name} onChange={(value) => setBodyDraft((current) => ({ ...current, name: value }))} />
                  <Field label="Type" value={bodyDraft.bodyType || ""} onChange={(value) => setBodyDraft((current) => ({ ...current, bodyType: value }))} />
                  <Field label="Sport" value={bodyDraft.sport || ""} onChange={(value) => setBodyDraft((current) => ({ ...current, sport: value }))} />
                  <Field label="Region" value={bodyDraft.region || ""} onChange={(value) => setBodyDraft((current) => ({ ...current, region: value }))} />
                  <Field label="Registration No." value={bodyDraft.registrationNumber || ""} onChange={(value) => setBodyDraft((current) => ({ ...current, registrationNumber: value }))} />
                  <Field label="Contact Email" value={bodyDraft.contactEmail || ""} onChange={(value) => setBodyDraft((current) => ({ ...current, contactEmail: value }))} />
                </div>
                <button onClick={() => void createBody()} className="mt-4 w-full rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-emerald-300">
                  Create Body
                </button>
              </Panel>

              <Panel title="Register Central Player" subtitle="Generate unique Sports ID records and route players into verification, licensing, draft, and transfer workflows.">
                <div className="grid gap-3 sm:grid-cols-2">
                  <Field label="Federation ID" value={String(playerDraft.federationId || "")} onChange={(value) => setPlayerDraft((current) => ({ ...current, federationId: Number(value) || undefined }))} />
                  <Field label="Player Name" value={playerDraft.playerName} onChange={(value) => setPlayerDraft((current) => ({ ...current, playerName: value }))} />
                  <Field label="Email" value={playerDraft.email} onChange={(value) => setPlayerDraft((current) => ({ ...current, email: value }))} />
                  <Field label="Sport" value={playerDraft.sport || ""} onChange={(value) => setPlayerDraft((current) => ({ ...current, sport: value }))} />
                  <Field label="City" value={playerDraft.city || ""} onChange={(value) => setPlayerDraft((current) => ({ ...current, city: value }))} />
                  <Field label="Age Group" value={playerDraft.ageGroup || ""} onChange={(value) => setPlayerDraft((current) => ({ ...current, ageGroup: value }))} />
                </div>
                <button onClick={() => void registerPlayer()} className="mt-4 w-full rounded-2xl bg-sky-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-sky-300">
                  Generate Sports ID
                </button>
              </Panel>
            </section>

            <section className="grid gap-6 xl:grid-cols-3">
              <Panel title="Franchise League Setup" subtitle="Create franchises with budget and roster controls.">
                <div className="space-y-3">
                  <Field label="Federation ID" value={String(franchiseDraft.federationId || "")} onChange={(value) => setFranchiseDraft((current) => ({ ...current, federationId: Number(value) || 0 }))} />
                  <Field label="Franchise Name" value={franchiseDraft.name} onChange={(value) => setFranchiseDraft((current) => ({ ...current, name: value }))} />
                  <Field label="Owner" value={franchiseDraft.ownerName || ""} onChange={(value) => setFranchiseDraft((current) => ({ ...current, ownerName: value }))} />
                  <Field label="Salary Cap" value={String(franchiseDraft.salaryCap || "")} onChange={(value) => setFranchiseDraft((current) => ({ ...current, salaryCap: Number(value) || 0 }))} />
                  <button onClick={() => void createFranchise()} className="w-full rounded-2xl border border-emerald-400/30 bg-emerald-400/10 px-4 py-3 text-sm font-black text-emerald-100 hover:bg-emerald-400/20">
                    Add Franchise
                  </button>
                </div>
              </Panel>

              <Panel title="Player Auction" subtitle="Publish player auctions with base price and bid controls.">
                <div className="space-y-3">
                  <Field label="Federation ID" value={String(auctionDraft.federationId || "")} onChange={(value) => setAuctionDraft((current) => ({ ...current, federationId: Number(value) || 0 }))} />
                  <Field label="Player Registration ID" value={String(auctionDraft.playerRegistrationId || "")} onChange={(value) => setAuctionDraft((current) => ({ ...current, playerRegistrationId: Number(value) || 0 }))} />
                  <Field label="Auction Name" value={auctionDraft.auctionName} onChange={(value) => setAuctionDraft((current) => ({ ...current, auctionName: value }))} />
                  <Field label="Base Price" value={String(auctionDraft.basePrice || "")} onChange={(value) => setAuctionDraft((current) => ({ ...current, basePrice: Number(value) || 0 }))} />
                  <button onClick={() => void createAuction()} className="w-full rounded-2xl border border-sky-400/30 bg-sky-400/10 px-4 py-3 text-sm font-black text-sky-100 hover:bg-sky-400/20">
                    Publish Auction
                  </button>
                </div>
              </Panel>

              <Panel title="Election Management" subtitle="Create federation elections and publish audited results.">
                <div className="space-y-3">
                  <Field label="Federation ID" value={String(electionDraft.federationId || "")} onChange={(value) => setElectionDraft((current) => ({ ...current, federationId: Number(value) || 0 }))} />
                  <Field label="Title" value={electionDraft.title} onChange={(value) => setElectionDraft((current) => ({ ...current, title: value }))} />
                  <Field label="Position" value={electionDraft.positionName} onChange={(value) => setElectionDraft((current) => ({ ...current, positionName: value }))} />
                  <Field label="Candidates CSV" value={electionDraft.candidateNames} onChange={(value) => setElectionDraft((current) => ({ ...current, candidateNames: value }))} />
                  <button onClick={() => void createElection()} className="w-full rounded-2xl border border-purple-400/30 bg-purple-400/10 px-4 py-3 text-sm font-black text-purple-100 hover:bg-purple-400/20">
                    Create Election
                  </button>
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 xl:grid-cols-[1fr_0.9fr]">
              <Panel title="Central Player Registry" subtitle="Identity verification, Sports ID, eligibility, current franchise, and ranking score.">
                <DataTable
                  rows={players}
                  columns={[
                    ["sportsId", "Sports ID"],
                    ["playerName", "Player"],
                    ["sport", "Sport"],
                    ["verificationStatus", "Verify"],
                    ["currentFranchiseId", "Franchise"],
                  ]}
                  empty="No central player registrations yet."
                  actionLabel="Verify"
                  onAction={(row) => void updateAdminFederationGovernancePlayerVerification(Number(row.id), { status: "VERIFIED", notes: "Verified from governance portal." }).then(loadGovernance)}
                />
              </Panel>

              <Panel title="Compliance Controls" subtitle="Operational checks for IDs, salary caps, licenses, disciplinary enforcement, and integrity monitoring.">
                <div className="space-y-3">
                  {(dashboard?.compliance || []).map((item) => (
                    <CompactRow key={String(item.title)} title={String(item.title)} meta={`${String(item.passed)} / ${String(item.total)} passed`} status={String(item.status)} />
                  ))}
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 xl:grid-cols-2">
              <Panel title="Drafts, Auctions & Transfers" subtitle="Franchise allocation pipeline and roster movement controls.">
                <div className="grid gap-4 lg:grid-cols-3">
                  <MiniList title="Drafts" rows={dashboard?.drafts || []} primary="name" secondary="status" />
                  <MiniList title="Auctions" rows={auctions} primary="auctionName" secondary="currentBid" />
                  <MiniList title="Transfers" rows={transfers} primary="transferType" secondary="status" />
                </div>
                <div className="mt-4 space-y-3">
                  {transfers.slice(0, 4).map((item) => (
                    <CompactRow
                      key={String(item.id)}
                      title={`Player ${String(item.playerRegistrationId)} to franchise ${String(item.toFranchiseId)}`}
                      meta={String(item.reason || "Transfer review")}
                      status={String(item.status)}
                      actionLabel="Approve"
                      onAction={() => void updateAdminFederationGovernanceTransferStatus(Number(item.id), { status: "APPROVED", notes: "Approved from governance portal." }).then(loadGovernance)}
                    />
                  ))}
                </div>
              </Panel>

              <Panel title="Integrity, Discipline & Elections" subtitle="Integrity alerts, disciplinary cases, official certification, and election governance.">
                <div className="grid gap-4 md:grid-cols-2">
                  <MiniList title="Cases" rows={cases} primary="caseType" secondary="status" />
                  <MiniList title="Officials" rows={officials} primary="name" secondary="status" />
                  <MiniList title="Licenses" rows={licenses} primary="licenseNumber" secondary="status" />
                  <MiniList title="Rankings" rows={rankings} primary="entityType" secondary="points" />
                </div>
                <div className="mt-4 space-y-3">
                  {alerts.slice(0, 4).map((item) => (
                    <CompactRow
                      key={String(item.id)}
                      title={String(item.alertType)}
                      meta={`${String(item.subjectReference)} - risk ${String(item.riskScore)}`}
                      status={String(item.status)}
                      actionLabel="Resolve"
                      onAction={() => void updateAdminFederationGovernanceIntegrityAlertStatus(Number(item.id), { status: "RESOLVED", notes: "Resolved from governance portal." }).then(loadGovernance)}
                    />
                  ))}
                  {elections.slice(0, 4).map((item) => (
                    <CompactRow
                      key={`election-${String(item.id)}`}
                      title={String(item.title)}
                      meta={`${String(item.positionName)} - ${String(item.voteCount || 0)} votes`}
                      status={String(item.status)}
                      actionLabel="Publish"
                      onAction={() => void publishAdminFederationGovernanceElectionResults(Number(item.id)).then(loadGovernance)}
                    />
                  ))}
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 lg:grid-cols-2">
              <Panel title="Priority Actions" subtitle="Highest-risk governance tasks requiring admin attention.">
                <div className="space-y-3">
                  {(dashboard?.priorityActions || []).map((item) => (
                    <CompactRow key={String(item.title)} title={String(item.title)} meta={String(item.message)} status={String(item.severity)} />
                  ))}
                </div>
              </Panel>
              <Panel title="Governance Audit" subtitle="Immutable audit stream for federation, draft, auction, transfer, case, election, and integrity events.">
                <DataTable rows={audit} columns={[["action", "Action"], ["entityType", "Entity"], ["actor", "Actor"], ["createdAt", "Created"]]} empty="No governance audit events yet." />
              </Panel>
            </section>
          </>
        )}
      </div>
    </main>
  );
}

function MetricCard({ label, value, detail }: { label: string; value: unknown; detail: string }) {
  return (
    <div className="rounded-3xl border border-white/10 bg-white/[0.04] p-5">
      <p className="text-xs font-black uppercase tracking-[0.18em] text-slate-500">{label}</p>
      <p className="mt-3 text-3xl font-black text-white">{formatNumber(value)}</p>
      <p className="mt-1 text-xs font-bold text-slate-400">{detail}</p>
    </div>
  );
}

function Panel({ title, subtitle, children }: { title: string; subtitle: string; children: React.ReactNode }) {
  return (
    <section className="rounded-[2rem] border border-white/10 bg-white/[0.04] p-5 shadow-xl">
      <p className="text-lg font-black text-white">{title}</p>
      <p className="mt-1 text-sm text-slate-400">{subtitle}</p>
      <div className="mt-5">{children}</div>
    </section>
  );
}

function Field({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <label className="block">
      <span className="text-xs font-black uppercase tracking-[0.14em] text-slate-500">{label}</span>
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-2 w-full rounded-2xl border border-white/10 bg-slate-950 px-4 py-3 text-sm font-bold text-white outline-none focus:border-emerald-300/60"
      />
    </label>
  );
}

function CompactRow({
  title,
  meta,
  status,
  actionLabel,
  onAction,
}: {
  title: string;
  meta: string;
  status: string;
  actionLabel?: string;
  onAction?: () => void;
}) {
  return (
    <div className="rounded-2xl border border-white/10 bg-slate-950/50 p-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="font-black text-white">{title}</p>
          <p className="mt-1 text-xs font-bold text-slate-500">{meta}</p>
        </div>
        <div className="flex items-center gap-2">
          <span className="rounded-full border border-white/10 bg-white/[0.04] px-3 py-1 text-[11px] font-black uppercase text-slate-300">{status}</span>
          {actionLabel && onAction ? (
            <button onClick={onAction} className="rounded-full bg-emerald-400 px-3 py-1 text-[11px] font-black text-slate-950 hover:bg-emerald-300">
              {actionLabel}
            </button>
          ) : null}
        </div>
      </div>
    </div>
  );
}

function DataTable({
  rows,
  columns,
  empty,
  actionLabel,
  onAction,
}: {
  rows: AdminFederationGovernanceMap[];
  columns: Array<[string, string]>;
  empty: string;
  actionLabel?: string;
  onAction?: (row: AdminFederationGovernanceMap) => void;
}) {
  if (!rows.length) {
    return <p className="rounded-2xl border border-dashed border-white/10 p-5 text-sm font-bold text-slate-500">{empty}</p>;
  }
  return (
    <div className="overflow-hidden rounded-2xl border border-white/10">
      <table className="min-w-full divide-y divide-white/10 text-sm">
        <thead className="bg-white/[0.03] text-left text-xs uppercase tracking-[0.12em] text-slate-500">
          <tr>
            {columns.map(([, label]) => (
              <th key={label} className="px-4 py-3 font-black">
                {label}
              </th>
            ))}
            {actionLabel ? <th className="px-4 py-3 font-black">Action</th> : null}
          </tr>
        </thead>
        <tbody className="divide-y divide-white/10">
          {rows.slice(0, 10).map((row, index) => (
            <tr key={String(row.id || index)} className="bg-slate-950/40">
              {columns.map(([key]) => (
                <td key={key} className="px-4 py-3 font-semibold text-slate-300">
                  {formatNumber(row[key])}
                </td>
              ))}
              {actionLabel && onAction ? (
                <td className="px-4 py-3">
                  <button onClick={() => onAction(row)} className="rounded-full bg-emerald-400 px-3 py-1 text-[11px] font-black text-slate-950 hover:bg-emerald-300">
                    {actionLabel}
                  </button>
                </td>
              ) : null}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function MiniList({ title, rows, primary, secondary }: { title: string; rows: AdminFederationGovernanceMap[]; primary: string; secondary: string }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-slate-950/40 p-4">
      <p className="text-xs font-black uppercase tracking-[0.16em] text-slate-500">{title}</p>
      <div className="mt-3 space-y-2">
        {rows.slice(0, 4).map((row, index) => (
          <div key={String(row.id || index)} className="flex items-center justify-between gap-3 text-xs">
            <span className="truncate font-bold text-slate-300">{String(row[primary] || "--")}</span>
            <span className="shrink-0 rounded-full bg-white/[0.06] px-2 py-1 font-black text-slate-400">{formatNumber(row[secondary])}</span>
          </div>
        ))}
        {!rows.length ? <p className="text-xs font-bold text-slate-600">No records</p> : null}
      </div>
    </div>
  );
}

function formatNumber(value: unknown) {
  if (typeof value === "number") {
    return new Intl.NumberFormat("en-IN").format(value);
  }
  if (value === null || value === undefined || value === "") {
    return "--";
  }
  return String(value);
}
