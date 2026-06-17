"use client";

import { useEffect, useState } from "react";
import { useI18n } from "@/modules/i18n/I18nProvider";
import { getWeatherDashboard } from "@/modules/weather/services/weatherService";
import { WeatherDashboard } from "@/modules/weather/types/weather.types";

export default function WeatherPage() {
  const { t } = useI18n();
  const [city, setCity] = useState(() => (typeof window === "undefined" ? "Ahmedabad" : localStorage.getItem("globalCity") || "Ahmedabad"));
  const [dashboard, setDashboard] = useState<WeatherDashboard | null>(null);
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);

  async function load(nextCity = city) {
    setLoading(true);
    try {
      const res = await getWeatherDashboard(nextCity);
      setDashboard(res.data);
      setStatus("");
    } catch (error: unknown) {
      setStatus(errorMessage(error, "Unable to load weather dashboard."));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <main className="theme-page-player min-h-screen px-4 py-8 sm:px-6 lg:px-10">
      <div className="mx-auto max-w-7xl space-y-8">
        <section className="theme-panel-strong rounded-[2rem] p-6 sm:p-8">
          <p className="theme-accent text-xs font-black uppercase tracking-[0.34em]">{t("weather.epic")}</p>
          <div className="mt-4 grid gap-8 xl:grid-cols-[1.3fr_0.7fr] xl:items-end">
            <div>
              <h1 className="theme-title text-4xl font-black sm:text-6xl">{t("weather.title")}</h1>
              <p className="theme-muted mt-4 max-w-3xl text-base leading-7">{t("weather.subtitle")}</p>
            </div>
            <div className="theme-panel-soft rounded-3xl p-5">
              <label className="theme-title text-xs font-black uppercase tracking-wider">{t("label.city")}</label>
              <div className="mt-2 grid grid-cols-[1fr_auto] gap-2">
                <input value={city} onChange={(e) => setCity(e.target.value)} className="theme-input rounded-xl px-3 py-2 text-sm" />
                <button onClick={() => load(city)} className="rounded-xl bg-emerald-500 px-4 py-2 text-sm font-black text-white">{t("common.search")}</button>
              </div>
            </div>
          </div>
        </section>

        {loading && <p className="theme-muted py-12 text-center">{t("common.loading")}</p>}
        {status && <div className="theme-panel-soft rounded-2xl p-4 text-sm font-semibold text-amber-600 dark:text-amber-200">{status}</div>}

        {!loading && dashboard && (
          <>
            <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <Metric label={t("weather.playability")} value={dashboard.current.summary} />
              <Metric label={t("weather.rainRisk")} value={`${Math.round(dashboard.current.rainProbability)}%`} tone={riskTone(dashboard.current.riskLevel)} />
              <Metric label={t("weather.temperature")} value={`${Math.round(dashboard.current.temperature)}C`} />
              <Metric label={t("weather.highRisk")} value={dashboard.highRiskBookings} tone="amber" />
            </section>

            <section className="grid gap-6 xl:grid-cols-[0.85fr_1.15fr]">
              <Panel title={t("weather.forecast")} body={dashboard.insight}>
                <div className="space-y-3">
                  {dashboard.forecast.map((item) => (
                    <div key={`${item.date}-${item.hour}`} className="theme-panel-soft rounded-2xl p-4">
                      <div className="flex items-center justify-between gap-3">
                        <p className="theme-title font-black">{item.hour}:00 - {item.summary}</p>
                        <span className={`rounded-full px-3 py-1 text-xs font-black ${riskClass(item.riskLevel)}`}>{item.riskLevel}</span>
                      </div>
                      <p className="theme-muted mt-2 text-sm">Rain {Math.round(item.rainProbability)}% - Heat {Math.round(item.temperature)}C - Humidity {Math.round(item.humidity)}%</p>
                    </div>
                  ))}
                </div>
              </Panel>

              <Panel title={t("weather.recommendations")} body={t("weather.recommendationsBody")}>
                <div className="grid gap-4 lg:grid-cols-2">
                  {dashboard.recommendations.length === 0 && <p className="theme-card theme-muted rounded-2xl border-dashed p-8 text-center">{t("weather.emptyRecommendations")}</p>}
                  {dashboard.recommendations.map((item) => (
                    <article key={`${item.turfId}-${item.slotId}`} className="theme-panel-soft rounded-3xl p-5">
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <h3 className="theme-title text-lg font-black">{item.turfName}</h3>
                          <p className="theme-muted mt-1 text-sm">{item.date} - {item.startTime} to {item.endTime}</p>
                        </div>
                        <span className="theme-badge rounded-full px-3 py-1 text-xs font-black">{item.score}/100</span>
                      </div>
                      <p className="theme-muted mt-3 text-sm">{item.reason}</p>
                      <p className="mt-3 text-sm font-bold text-emerald-600 dark:text-emerald-200">{item.recommendation}</p>
                    </article>
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

function Metric({ label, value, tone = "emerald" }: { label: string; value: string | number; tone?: string }) {
  return (
    <div className="theme-panel rounded-3xl p-5">
      <p className="theme-soft text-xs font-black uppercase tracking-[0.24em]">{label}</p>
      <p className={`mt-4 text-2xl font-black ${tone === "amber" ? "text-amber-600 dark:text-amber-200" : "theme-title"}`}>{value}</p>
    </div>
  );
}

function riskTone(risk: string) {
  return risk === "HIGH" || risk === "UNSAFE" ? "amber" : "emerald";
}

function riskClass(risk: string) {
  if (risk === "UNSAFE" || risk === "HIGH") return "bg-red-500/15 text-red-600 dark:text-red-200";
  if (risk === "MEDIUM") return "bg-amber-500/15 text-amber-600 dark:text-amber-200";
  return "bg-emerald-500/15 text-emerald-600 dark:text-emerald-200";
}

function errorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { message?: string; error?: string } } };
  return apiError.response?.data?.message || apiError.response?.data?.error || fallback;
}
