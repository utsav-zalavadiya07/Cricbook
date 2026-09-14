"use client";

import { useEffect, useMemo, useState } from "react";
import { TileInfo } from "@/components/ui";
import {
  CareerMap,
  CareerProfilePayload,
  addCareerAchievement,
  applyCareerOpportunity,
  createCareerProfile,
  generateCareerResume,
  getCareerMarketplace,
  getCareerOpportunities,
} from "@/modules/career/services/careerService";

const defaultProfile: CareerProfilePayload = {
  playerName: "",
  playerEmail: "",
  sport: "CRICKET",
  specialization: "Batting",
  playerRole: "Top order batter",
  city: "",
  ageGroup: "OPEN",
  bio: "",
  statisticsSummary: "",
  certificatesSummary: "",
  scoutOptIn: true,
  visibility: "PUBLIC",
};

export default function CareerPage() {
  const [marketplace, setMarketplace] = useState<CareerMap[]>([]);
  const [opportunities, setOpportunities] = useState<CareerMap[]>([]);
  const [profileDraft, setProfileDraft] = useState<CareerProfilePayload>(defaultProfile);
  const [currentProfile, setCurrentProfile] = useState<CareerMap | null>(null);
  const [resume, setResume] = useState<CareerMap | null>(null);
  const [achievementTitle, setAchievementTitle] = useState("");
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);

  async function loadCareer() {
    try {
      setLoading(true);
      const [marketplaceRes, opportunityRes] = await Promise.all([
        getCareerMarketplace(),
        getCareerOpportunities(),
      ]);
      setMarketplace(marketplaceRes.data);
      setOpportunities(opportunityRes.data);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void loadCareer(), 0);
    return () => window.clearTimeout(timer);
  }, []);

  const metrics = useMemo(
    () => [
      { label: "Scout-ready profiles", value: marketplace.length, detail: "career + academy talent" },
      { label: "Open opportunities", value: opportunities.length, detail: "trials, scholarships, recruitment" },
      { label: "Trials", value: opportunities.filter((item) => String(item.opportunityType || "").toUpperCase() === "TRIAL").length, detail: "capacity tracked" },
      { label: "Scholarships", value: opportunities.filter((item) => String(item.opportunityType || "").toUpperCase() === "SCHOLARSHIP").length, detail: "eligibility checked" },
    ],
    [marketplace, opportunities]
  );

  async function handleCreateProfile() {
    if (!profileDraft.playerName.trim() || !profileDraft.playerEmail.trim()) {
      setStatus("Player name and email are required.");
      return;
    }
    try {
      const response = await createCareerProfile(profileDraft);
      setCurrentProfile(response.data);
      setStatus("Career profile created and made scout-ready.");
      await loadCareer();
    } catch (error) {
      setStatus(errorMessage(error, "Could not create career profile."));
    }
  }

  async function handleGenerateResume() {
    const profileId = Number(currentProfile?.id);
    if (!profileId) {
      setStatus("Create a profile first.");
      return;
    }
    try {
      const response = await generateCareerResume(profileId);
      setResume(response.data);
      setStatus("Sports resume generated with share link and QR code.");
    } catch (error) {
      setStatus(errorMessage(error, "Resume generation failed."));
    }
  }

  async function handleAddAchievement() {
    const profileId = Number(currentProfile?.id);
    if (!profileId || !achievementTitle.trim()) {
      setStatus("Create a profile and add achievement title first.");
      return;
    }
    try {
      await addCareerAchievement(profileId, {
        title: achievementTitle,
        achievementType: "TOURNAMENT",
        issuer: "Truf Sports",
        notes: "Submitted from player career page.",
      });
      setAchievementTitle("");
      setStatus("Achievement submitted for verification.");
    } catch (error) {
      setStatus(errorMessage(error, "Achievement submission failed."));
    }
  }

  async function handleApply(opportunityId: number) {
    const profileId = Number(currentProfile?.id);
    if (!profileId) {
      setStatus("Create a career profile before applying.");
      return;
    }
    try {
      const response = await applyCareerOpportunity(opportunityId, {
        profileId,
        statement: "Interested in this opportunity from Truf Sports career marketplace.",
      });
      setStatus(`Application ${String(response.data.status || "submitted").toLowerCase()}.`);
      await loadCareer();
    } catch (error) {
      setStatus(errorMessage(error, "Application failed."));
    }
  }

  return (
    <main className="min-h-screen theme-bg px-4 py-8 sm:px-6" data-i18n-skip="true">
      <div className="mx-auto max-w-7xl space-y-8">
        <header className="theme-card rounded-[2rem] border p-6 shadow-2xl">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.26em] text-emerald-300">EPIC 31 · Sports Career</p>
              <h1 className="theme-title mt-3 text-3xl font-black sm:text-4xl">Build your verified sports career profile
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Build your verified sports career profile information" focusable={false}>Create a digital sports profile, generate a resume, submit achievements for verification, and apply to trials, scholarships, and recruitment opportunities.</TileInfo>
              </span></h1>
            </div>
            <button onClick={() => void loadCareer()} className="rounded-2xl bg-emerald-400 px-5 py-3 text-sm font-black text-slate-950 hover:bg-emerald-300">
              Refresh marketplace
            </button>
          </div>
        </header>

        <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {metrics.map((metric) => (
            <MetricCard key={metric.label} {...metric} />
          ))}
        </section>

        {status ? <div className="theme-card rounded-2xl border px-4 py-3 text-sm font-bold text-emerald-200">{status}</div> : null}

        <section className="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
          <Panel title="Sports Profile Builder" subtitle="Digital profile, scout opt-in, resume sharing, and achievement verification.">
            <div className="grid gap-3 sm:grid-cols-2">
              <Field label="Player Name" value={profileDraft.playerName} onChange={(value) => setProfileDraft((current) => ({ ...current, playerName: value }))} />
              <Field label="Email" value={profileDraft.playerEmail} onChange={(value) => setProfileDraft((current) => ({ ...current, playerEmail: value }))} />
              <Field label="Sport" value={profileDraft.sport || ""} onChange={(value) => setProfileDraft((current) => ({ ...current, sport: value }))} />
              <Field label="City" value={profileDraft.city || ""} onChange={(value) => setProfileDraft((current) => ({ ...current, city: value }))} />
              <Field label="Role" value={profileDraft.playerRole || ""} onChange={(value) => setProfileDraft((current) => ({ ...current, playerRole: value }))} />
              <Field label="Age Group" value={profileDraft.ageGroup || ""} onChange={(value) => setProfileDraft((current) => ({ ...current, ageGroup: value }))} />
            </div>
            <textarea
              value={profileDraft.bio || ""}
              onChange={(event) => setProfileDraft((current) => ({ ...current, bio: event.target.value }))}
              placeholder="Career bio, playing style, goals..."
              className="theme-input mt-3 min-h-24 w-full rounded-2xl px-4 py-3 text-sm outline-none"
            />
            <div className="mt-4 grid gap-3 sm:grid-cols-3">
              <button onClick={() => void handleCreateProfile()} className="rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-emerald-300">
                Create Profile
              </button>
              <button onClick={() => void handleGenerateResume()} className="theme-card rounded-2xl border px-4 py-3 text-sm font-black">
                Generate Resume
              </button>
              <button onClick={() => setProfileDraft(defaultProfile)} className="theme-card rounded-2xl border px-4 py-3 text-sm font-black">
                Reset
              </button>
            </div>
            {currentProfile ? (
              <div className="mt-5 rounded-2xl border border-emerald-400/20 bg-emerald-400/10 p-4">
                <p className="text-sm font-black text-emerald-100">{String(currentProfile.playerName)} · {String(currentProfile.rankingBand)}</p>
                <p className="mt-1 text-xs text-emerald-100/80">Completeness {String(currentProfile.profileCompleteness)}% · Ranking score {String(currentProfile.rankingScore)}</p>
              </div>
            ) : null}
            {resume ? (
              <div className="mt-4 rounded-2xl border border-white/10 bg-white/[0.04] p-4 text-sm">
                <p className="font-black">Resume share link</p>
                <p className="theme-muted mt-1 break-all">{String(resume.shareLink)}</p>
                <p className="theme-muted mt-1 break-all">{String(resume.qrCode)}</p>
              </div>
            ) : null}
          </Panel>

          <Panel title="Achievement Verification" subtitle="Submit achievements that admin/scouts can verify before they improve ranking trust.">
            <div className="grid gap-3 sm:grid-cols-[1fr_auto]">
              <Field label="Achievement Title" value={achievementTitle} onChange={setAchievementTitle} />
              <button onClick={() => void handleAddAchievement()} className="mt-6 rounded-2xl bg-emerald-400 px-5 py-3 text-sm font-black text-slate-950 hover:bg-emerald-300">
                Submit
              </button>
            </div>
            <div className="mt-5 grid gap-3 sm:grid-cols-3">
              <MetricCard label="Completeness" value={currentProfile ? `${String(currentProfile.profileCompleteness)}%` : "0%"} detail="profile readiness" compact />
              <MetricCard label="Score" value={currentProfile ? String(currentProfile.rankingScore || 0) : "0"} detail="ranking score" compact />
              <MetricCard label="Views" value={currentProfile ? String(currentProfile.scoutViews || 0) : "0"} detail="scout views" compact />
            </div>
          </Panel>
        </section>

        <section className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
          <Panel title="Talent Marketplace" subtitle="Scout-visible career and academy talent profiles ranked by verified signals.">
            {loading ? <p className="theme-muted py-8 text-center text-sm">Loading talent marketplace...</p> : null}
            <div className="grid gap-4 md:grid-cols-2">
              {marketplace.slice(0, 6).map((profile) => (
                <div key={`${String(profile.source)}-${String(profile.id)}`} className="theme-card rounded-3xl border p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="theme-title text-base font-black">{String(profile.playerName || "Unnamed player")}</p>
                      <p className="theme-muted mt-1 text-xs">{String(profile.sport || "--")} · {String(profile.city || "--")} · {String(profile.ageGroup || "OPEN")}</p>
                    </div>
                    <span className="rounded-full border border-emerald-400/20 bg-emerald-400/10 px-3 py-1 text-[10px] font-black text-emerald-200">{String(profile.rankingBand || "FOUNDATION")}</span>
                  </div>
                  <p className="mt-4 text-3xl font-black text-white">{String(profile.rankingScore || 0)}</p>
                  <p className="theme-muted mt-1 text-xs">Ranking score · {String(profile.source || "CAREER")}</p>
                </div>
              ))}
            </div>
          </Panel>

          <Panel title="Trials, Scholarships & Recruitment" subtitle="Apply with your career profile; capacity and waitlist rules are enforced.">
            <div className="space-y-3">
              {opportunities.slice(0, 8).map((opportunity) => (
                <div key={String(opportunity.id)} className="theme-card rounded-2xl border p-4">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <p className="theme-title text-sm font-black">{String(opportunity.title)}</p>
                      <p className="theme-muted mt-1 text-xs">{String(opportunity.organizationName)} · {String(opportunity.location || "--")}</p>
                    </div>
                    <span className="rounded-full border border-white/10 px-3 py-1 text-[10px] font-black">{String(opportunity.opportunityType)}</span>
                  </div>
                  <p className="theme-muted mt-3 text-xs leading-5">{String(opportunity.eligibility || opportunity.description || "Eligibility details will be shared by organizer.")}</p>
                  <button onClick={() => void handleApply(Number(opportunity.id))} className="mt-4 rounded-2xl bg-emerald-400 px-4 py-2 text-xs font-black text-slate-950 hover:bg-emerald-300">
                    Apply
                  </button>
                </div>
              ))}
            </div>
          </Panel>
        </section>
      </div>
    </main>
  );
}

function Panel({ title, subtitle, children }: { title: string; subtitle: string; children: React.ReactNode }) {
  return (
    <section className="theme-card rounded-[2rem] border p-5 shadow-2xl">
      <div className="mb-5">
        <h2 className="theme-title text-lg font-black">{title}</h2>
        <p className="theme-muted mt-1 text-sm leading-5">{subtitle}</p>
      </div>
      {children}
    </section>
  );
}

function MetricCard({ label, value, detail, compact = false }: { label: string; value: unknown; detail: string; compact?: boolean }) {
  return (
    <div className={`theme-card rounded-[1.5rem] border ${compact ? "p-4" : "p-5"}`}>
      <div className="flex min-w-0 items-start justify-between gap-2">
        <p className="theme-muted min-w-0 break-words text-[10px] font-black uppercase leading-5 tracking-[0.22em]">{label}</p>
        <TileInfo label={`${label} information`}>{detail}</TileInfo>
      </div>
      <p className={`theme-title mt-2 font-black ${compact ? "text-2xl" : "text-3xl"}`}>{formatNumber(value)}</p>
    </div>
  );
}

function Field({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <label className="theme-muted block text-xs font-black uppercase tracking-[0.18em]">
      {label}
      <input value={value} onChange={(event) => onChange(event.target.value)} className="theme-input mt-2 w-full rounded-2xl px-4 py-3 text-sm font-semibold normal-case tracking-normal outline-none" />
    </label>
  );
}

function formatNumber(value: unknown) {
  const number = Number(value ?? 0);
  if (!Number.isFinite(number)) {
    return "0";
  }
  return new Intl.NumberFormat("en-IN", { maximumFractionDigits: 1 }).format(number);
}

function errorMessage(error: unknown, fallback: string) {
  const maybeError = error as { response?: { data?: { message?: string } } };
  return maybeError.response?.data?.message || fallback;
}
