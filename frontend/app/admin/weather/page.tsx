"use client";

import { useEffect, useState } from "react";
import { useI18n } from "@/modules/i18n/I18nProvider";
import { getAdminWeatherDashboard } from "@/modules/weather/services/weatherService";
import { WeatherDashboard } from "@/modules/weather/types/weather.types";

export default function AdminWeatherPage() {
  const { t } = useI18n();
  const [city, setCity] = useState("Ahmedabad");
  const [dashboard, setDashboard] = useState<WeatherDashboard | null>(null);
  const [status, setStatus] = useState("");

  async function load() {
    try {
      const res = await getAdminWeatherDashboard(city);
      setDashboard(res.data);
      setStatus("");
    } catch (error: unknown) {
      setStatus(errorMessage(error, "Unable to load weather analytics."));
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <main className="theme-page-owner min-h-screen p-4 sm:p-6 lg:p-8">
      <div className="mx-auto max-w-6xl space-y-6">
        <section className="theme-panel rounded-[2rem] p-6 sm:p-8">
          <p className="theme-accent-owner text-xs font-black uppercase tracking-[0.34em]">{t("weather.adminEpic")}</p>
          <h1 className="theme-title mt-3 text-3xl font-black sm:text-5xl">{t("weather.adminTitle")}</h1>
          <p className="theme-muted mt-3 max-w-3xl text-sm leading-6">{t("weather.adminSubtitle")}</p>
          <div className="mt-5 grid max-w-xl grid-cols-[1fr_auto] gap-2">
            <input value={city} onChange={(e) => setCity(e.target.value)} className="theme-input rounded-xl px-3 py-2 text-sm" />
            <button onClick={load} className="rounded-xl bg-sky-500 px-4 py-2 text-sm font-black text-white">{t("common.search")}</button>
          </div>
        </section>

        {status && <div className="theme-panel-soft rounded-2xl p-4 text-sm font-semibold text-amber-600 dark:text-amber-200">{status}</div>}

        <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Metric label={t("weather.rainRisk")} value={`${Math.round(dashboard?.current?.rainProbability || 0)}%`} />
          <Metric label={t("weather.temperature")} value={`${Math.round(dashboard?.current?.temperature || 0)}C`} />
          <Metric label={t("weather.highRisk")} value={dashboard?.highRiskBookings ?? 0} />
          <Metric label={t("weather.weatherRefunds")} value={dashboard?.weatherRefunds ?? 0} />
        </section>

        <section className="theme-panel rounded-3xl p-6">
          <h2 className="theme-title text-xl font-black">{t("weather.forecast")}</h2>
          <p className="theme-muted mt-2 text-sm">{dashboard?.insight}</p>
          <div className="mt-5 grid gap-3 sm:grid-cols-2">
            {dashboard?.forecast.map((item) => (
              <div key={`${item.date}-${item.hour}`} className="theme-panel-soft rounded-2xl p-4">
                <p className="theme-title font-black">{item.hour}:00 - {item.summary}</p>
                <p className="theme-muted mt-2 text-sm">Rain {Math.round(item.rainProbability)}% - Heat {Math.round(item.temperature)}C - {item.riskLevel}</p>
              </div>
            ))}
          </div>
        </section>
      </div>
    </main>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="theme-panel rounded-3xl p-5">
      <p className="theme-soft text-xs font-bold uppercase tracking-[0.24em]">{label}</p>
      <p className="theme-title mt-4 text-3xl font-black">{value}</p>
    </div>
  );
}

function errorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { message?: string; error?: string } } };
  return apiError.response?.data?.message || apiError.response?.data?.error || fallback;
}
