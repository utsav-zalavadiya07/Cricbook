"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import {
  AdminAiCopilotResponse,
  AdminAiDashboard,
  AdminAiGovernance,
  AdminAiMap,
  askAdminAiCopilot,
  getAdminAiDashboard,
  getAdminAiGovernance,
} from "@/modules/admin/services/adminService";

const copilotPrompts = [
  "Which city is performing best?",
  "Why is revenue down?",
  "Show fraud risk summary",
  "How is user growth trending?",
];

export default function AdminAiDashboardPage() {
  const [dashboard, setDashboard] = useState<AdminAiDashboard | null>(null);
  const [governance, setGovernance] = useState<AdminAiGovernance | null>(null);
  const [copilotQuery, setCopilotQuery] = useState(copilotPrompts[0]);
  const [copilotResponse, setCopilotResponse] = useState<AdminAiCopilotResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [asking, setAsking] = useState(false);

  async function loadAiWorkspace() {
    try {
      setLoading(true);
      const [dashboardRes, governanceRes] = await Promise.all([getAdminAiDashboard(), getAdminAiGovernance()]);
      setDashboard(dashboardRes.data);
      setGovernance(governanceRes.data);
    } finally {
      setLoading(false);
    }
  }

  async function askCopilot(query = copilotQuery) {
    try {
      setAsking(true);
      setCopilotQuery(query);
      const response = await askAdminAiCopilot(query);
      setCopilotResponse(response.data);
    } finally {
      setAsking(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void loadAiWorkspace(), 0);
    return () => window.clearTimeout(timer);
  }, []);

  const metrics = useMemo(
    () => [
      { label: "Recommendation Coverage", value: `${formatNumber(dashboard?.kpis?.recommendationCoverage)}%`, detail: "turf, slot, team, tournament fit" },
      { label: "Forecast Confidence", value: `${formatNumber(dashboard?.kpis?.forecastConfidence)}%`, detail: "revenue and booking prediction" },
      { label: "Chat SLA", value: `${formatNumber(dashboard?.kpis?.chatSlaMs)} ms`, detail: "assistant response target" },
      { label: "Fraud Alerts", value: formatNumber(dashboard?.kpis?.fraudAlerts), detail: "review, refund, account risk" },
      { label: "High Churn", value: formatNumber(dashboard?.kpis?.highRiskChurnUsers), detail: "retention automation queue" },
      { label: "Explainability", value: `${formatNumber(dashboard?.kpis?.explainabilityCoverage)}%`, detail: "reasoned outputs enabled" },
    ],
    [dashboard]
  );

  return (
    <main className="min-h-screen bg-slate-950 px-4 py-8 text-white sm:px-8" data-i18n-skip="true">
      <div className="mx-auto max-w-7xl space-y-8">
        <header className="rounded-[2rem] border border-white/10 bg-gradient-to-br from-violet-500/20 via-slate-900 to-cyan-500/10 p-6 shadow-2xl">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.26em] text-violet-300">EPIC 22 AI Intelligence</p>
              <h1 className="mt-3 text-3xl font-black text-white sm:text-4xl">AI Assistant & Platform Intelligence</h1>
              <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-300">
                Monitor AI assistant relevance, smart recommendations, forecasting, fraud, churn, pricing advice, review sentiment, governance, and responsible-AI controls.
              </p>
            </div>
            <div className="flex flex-wrap gap-3">
              <Link href="/admin/experiments" className="rounded-2xl border border-white/10 px-4 py-3 text-sm font-black text-slate-200 hover:bg-white/10">
                A/B Experiments
              </Link>
              <button onClick={() => void loadAiWorkspace()} className="rounded-2xl bg-violet-500 px-4 py-3 text-sm font-black text-white hover:bg-violet-400">
                Refresh AI
              </button>
            </div>
          </div>
        </header>

        {loading ? (
          <section className="rounded-[2rem] border border-white/10 bg-white/[0.04] p-10 text-center text-sm font-bold text-slate-300">Loading AI intelligence workspace...</section>
        ) : (
          <>
            <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {metrics.map((metric) => (
                <MetricCard key={metric.label} {...metric} />
              ))}
            </section>

            <section className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
              <Panel title="Admin Copilot" subtitle="Ask executive, fraud, growth, and revenue questions with audited responses.">
                <div className="grid gap-3 sm:grid-cols-[1fr_auto]">
                  <input
                    value={copilotQuery}
                    onChange={(event) => setCopilotQuery(event.target.value)}
                    className="rounded-2xl border border-white/10 bg-slate-950 px-4 py-3 text-sm font-bold text-white outline-none focus:border-violet-400"
                    placeholder="Ask admin copilot..."
                  />
                  <button disabled={asking} onClick={() => void askCopilot()} className="rounded-2xl bg-cyan-400 px-5 py-3 text-sm font-black text-slate-950 disabled:cursor-wait disabled:opacity-60">
                    {asking ? "Thinking..." : "Ask"}
                  </button>
                </div>
                <div className="mt-3 flex flex-wrap gap-2">
                  {copilotPrompts.map((prompt) => (
                    <button key={prompt} onClick={() => void askCopilot(prompt)} className="rounded-full border border-white/10 bg-white/[0.04] px-3 py-2 text-xs font-black text-slate-300 hover:bg-white/10">
                      {prompt}
                    </button>
                  ))}
                </div>
                {copilotResponse ? (
                  <div className="mt-5 rounded-3xl border border-cyan-400/20 bg-cyan-400/10 p-5">
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <p className="text-xs font-black uppercase tracking-[0.2em] text-cyan-200">{copilotResponse.intent}</p>
                      <span className="rounded-full bg-cyan-400 px-3 py-1 text-xs font-black text-slate-950">{formatNumber(copilotResponse.confidence * 100)}% confidence</span>
                    </div>
                    <p className="mt-3 text-sm leading-6 text-slate-100">{copilotResponse.answer}</p>
                    <div className="mt-4 grid gap-3 sm:grid-cols-2">
                      <ListBlock title="Sources" items={copilotResponse.sources} />
                      <ListBlock title="Actions" items={copilotResponse.recommendedActions} />
                    </div>
                  </div>
                ) : null}
              </Panel>

              <Panel title="Responsible AI Governance" subtitle="Drift, bias, guardrails, fallback, and human-review readiness.">
                <div className="grid gap-3 sm:grid-cols-2">
                  <MetricCard label="Drift Status" value={String(governance?.driftStatus || "STABLE")} detail={String(governance?.modelFamily || "RULE_HYBRID")} compact />
                  <MetricCard label="Model" value={String(governance?.modelVersion || dashboard?.modelVersion || "--")} detail="versioned and rollback-ready" compact />
                </div>
                <div className="mt-4 grid gap-3 sm:grid-cols-2">
                  <ListBlock title="Guardrails" items={objectEntries(governance?.guardrails)} />
                  <ListBlock title="Controls" items={governance?.responsibleAiControls || []} />
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 lg:grid-cols-3">
              <Panel title="Forecasting" subtitle="Revenue forecast, confidence, and explainability.">
                <div className="space-y-3">
                  {(dashboard?.forecasts || []).map((item) => (
                    <CompactRow
                      key={String(item.period)}
                      title={String(item.period)}
                      meta={`Expected revenue ${formatCurrency(item.expectedRevenue)} | bookings ${formatNumber(item.expectedBookings)}`}
                      status={`${formatNumber(item.confidence)}%`}
                    />
                  ))}
                </div>
              </Panel>

              <Panel title="Fraud & Churn" subtitle="Risk scoring and retention queue.">
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-1">
                  <MetricCard label="Fraud Score" value={formatNumber(dashboard?.fraud?.fraudScore)} detail={`${formatNumber(dashboard?.fraud?.highSeverity)} high severity`} compact />
                  <MetricCard label="Churn Coverage" value={`${formatNumber(dashboard?.churn?.coveragePercent)}%`} detail={`${formatNumber(dashboard?.churn?.highRisk)} high-risk users`} compact />
                </div>
              </Panel>

              <Panel title="Review Intelligence" subtitle="Sentiment and complaint clustering.">
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-1">
                  <MetricCard label="Positive" value={formatNumber(dashboard?.reviewInsights?.positiveSentiment)} detail="rating >= 4" compact />
                  <MetricCard label="Negative" value={formatNumber(dashboard?.reviewInsights?.negativeSentiment)} detail={`top: ${String(dashboard?.reviewInsights?.topComplaint || "none")}`} compact />
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
              <Panel title="Pricing Advisor" subtitle="Demand-sensitive suggestions with owner approval and rollback controls.">
                <div className="space-y-3">
                  {(dashboard?.pricingAdvisor?.suggestions || []).slice(0, 6).map((item) => (
                    <CompactRow
                      key={String(item.turfId)}
                      title={String(item.turfName || "Turf")}
                      meta={`${formatCurrency(item.currentPrice)} to ${formatCurrency(item.recommendedPrice)} | ${String(item.reason || "AI pricing suggestion")}`}
                      status={`${formatNumber(item.confidence)}%`}
                    />
                  ))}
                  {(dashboard?.pricingAdvisor?.suggestions || []).length === 0 ? <p className="text-sm text-slate-400">No pricing suggestions yet.</p> : null}
                </div>
              </Panel>

              <Panel title="Priority Actions" subtitle="Operational next steps generated from AI risk and feedback signals.">
                <div className="space-y-3">
                  {(dashboard?.priorityActions || []).map((item) => (
                    <CompactRow key={String(item.title)} title={String(item.title)} meta={String(item.message)} status={String(item.severity)} />
                  ))}
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 lg:grid-cols-2">
              <Panel title="Assistant Coverage" subtitle="Intents, escalation, and low-confidence handling.">
                <div className="grid gap-3 sm:grid-cols-3">
                  <MetricCard label="Turns" value={formatNumber(dashboard?.assistant?.totalTurns)} detail="persisted conversations" compact />
                  <MetricCard label="Escalations" value={formatNumber(dashboard?.assistant?.humanEscalations)} detail="human support handoff" compact />
                  <MetricCard label="Low Confidence" value={formatNumber(dashboard?.assistant?.lowConfidenceTurns)} detail="needs review" compact />
                </div>
                <ListBlock title="Supported Intents" items={asStringArray(dashboard?.assistant?.supportedIntents)} />
              </Panel>

              <Panel title="Recommendations" subtitle="Personalization, feedback, and recommendation engine controls.">
                <div className="grid gap-3 sm:grid-cols-3">
                  <MetricCard label="Active Turfs" value={formatNumber(dashboard?.recommendations?.activeTurfs)} detail="candidate pool" compact />
                  <MetricCard label="Signals" value={formatNumber(dashboard?.recommendations?.behaviorSignals)} detail="booking behavior" compact />
                  <MetricCard label="Feedback" value={formatNumber(dashboard?.recommendations?.feedbackSignals)} detail="likes/dislikes/ignores" compact />
                </div>
                <ListBlock title="Engines" items={asStringArray(dashboard?.recommendations?.engines)} />
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
    <section className="rounded-[2rem] border border-white/10 bg-white/[0.04] p-5 shadow-2xl shadow-black/20">
      <div className="mb-5">
        <h2 className="text-lg font-black text-white">{title}</h2>
        <p className="mt-1 text-sm leading-5 text-slate-400">{subtitle}</p>
      </div>
      {children}
    </section>
  );
}

function MetricCard({ label, value, detail, compact = false }: { label: string; value: string; detail: string; compact?: boolean }) {
  return (
    <div className={`rounded-[1.5rem] border border-white/10 bg-slate-900/80 ${compact ? "p-4" : "p-5"}`}>
      <p className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">{label}</p>
      <p className={`mt-2 break-words font-black text-white ${compact ? "text-xl" : "text-3xl"}`}>{value}</p>
      <p className="mt-2 text-xs leading-5 text-slate-400">{detail}</p>
    </div>
  );
}

function CompactRow({ title, meta, status }: { title: string; meta: string; status: string }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-slate-950/50 p-4">
      <div className="flex items-start justify-between gap-3">
        <p className="font-black text-white">{title}</p>
        <span className="shrink-0 rounded-full bg-white/10 px-3 py-1 text-[10px] font-black uppercase tracking-[0.16em] text-slate-300">{status}</span>
      </div>
      <p className="mt-2 text-xs leading-5 text-slate-400">{meta}</p>
    </div>
  );
}

function ListBlock({ title, items }: { title: string; items: string[] }) {
  return (
    <div className="mt-4 rounded-2xl border border-white/10 bg-slate-950/40 p-4">
      <p className="text-[10px] font-black uppercase tracking-[0.2em] text-violet-300">{title}</p>
      <div className="mt-3 flex flex-wrap gap-2">
        {items.length === 0 ? <span className="text-xs text-slate-500">No signals yet.</span> : null}
        {items.map((item) => (
          <span key={item} className="rounded-full bg-white/10 px-3 py-1 text-xs font-bold text-slate-300">{item}</span>
        ))}
      </div>
    </div>
  );
}

function asStringArray(value: unknown) {
  return Array.isArray(value) ? value.map(String) : [];
}

function objectEntries(value: Record<string, string> | undefined) {
  if (!value) {
    return [];
  }
  return Object.entries(value).map(([key, item]) => `${key}: ${item}`);
}

function formatCurrency(value: unknown) {
  const numericValue = Number(value ?? 0);
  return `Rs ${(Number.isFinite(numericValue) ? numericValue : 0).toLocaleString("en-IN", { maximumFractionDigits: 0 })}`;
}

function formatNumber(value: unknown) {
  const numericValue = Number(value ?? 0);
  return (Number.isFinite(numericValue) ? numericValue : 0).toLocaleString("en-IN", { maximumFractionDigits: 2 });
}
