"use client";

import { FormEvent, useEffect, useState } from "react";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import { useI18n } from "@/modules/i18n/I18nProvider";
import { getOwnerWeatherDashboard, getOwnerWeatherPolicy, updateOwnerWeatherPolicy, updateTurfEnvironment } from "@/modules/weather/services/weatherService";
import { WeatherDashboard, WeatherPolicy } from "@/modules/weather/types/weather.types";

export default function OwnerWeatherPage() {
  const { t } = useI18n();
  const [dashboard, setDashboard] = useState<WeatherDashboard | null>(null);
  const [policy, setPolicy] = useState<WeatherPolicy | null>(null);
  const [status, setStatus] = useState("");
  const [environment, setEnvironment] = useState({ turfId: "", type: "OUTDOOR", rainProtection: false, heatProtection: false });

  async function load() {
    try {
      const [dashRes, policyRes] = await Promise.all([getOwnerWeatherDashboard(), getOwnerWeatherPolicy()]);
      setDashboard(dashRes.data);
      setPolicy(policyRes.data);
      setStatus("");
    } catch (error: unknown) {
      setStatus(errorMessage(error, "Unable to load weather policy."));
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, []);

  const savePolicy = async (event: FormEvent) => {
    event.preventDefault();
    if (!policy) return;
    try {
      const res = await updateOwnerWeatherPolicy(policy);
      setPolicy(res.data);
      setStatus("Weather policy saved.");
    } catch (error: unknown) {
      setStatus(errorMessage(error, "Could not save weather policy."));
    }
  };

  const saveEnvironment = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await updateTurfEnvironment({
        turfId: Number(environment.turfId),
        type: environment.type as "INDOOR" | "SEMI_INDOOR" | "OUTDOOR",
        rainProtection: environment.rainProtection,
        heatProtection: environment.heatProtection,
      });
      setEnvironment({ turfId: "", type: "OUTDOOR", rainProtection: false, heatProtection: false });
      setStatus("Turf environment saved.");
    } catch (error: unknown) {
      setStatus(errorMessage(error, "Could not save turf environment."));
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <section className="theme-panel rounded-[2rem] p-6 sm:p-8">
          <p className="theme-accent-owner text-xs font-black uppercase tracking-[0.34em]">{t("weather.ownerEpic")}</p>
          <h1 className="theme-title mt-3 text-3xl font-black sm:text-5xl">{t("weather.ownerTitle")}</h1>
          <p className="theme-muted mt-3 max-w-3xl text-sm leading-6">{t("weather.ownerSubtitle")}</p>
        </section>

        {status && <div className="theme-panel-soft rounded-2xl p-4 text-sm font-semibold text-sky-600 dark:text-sky-200">{status}</div>}

        <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <Metric label={t("weather.rainRisk")} value={`${Math.round(dashboard?.current?.rainProbability || 0)}%`} />
          <Metric label={t("weather.temperature")} value={`${Math.round(dashboard?.current?.temperature || 0)}C`} />
          <Metric label={t("weather.highRisk")} value={dashboard?.highRiskBookings ?? 0} />
          <Metric label={t("weather.weatherRefunds")} value={dashboard?.weatherRefunds ?? 0} />
        </section>

        <section className="grid gap-6 xl:grid-cols-2">
          <form onSubmit={savePolicy} className="theme-panel rounded-3xl p-6">
            <h2 className="theme-title text-xl font-black">{t("weather.policy")}</h2>
            <p className="theme-muted mt-1 text-sm">{dashboard?.insight}</p>
            {policy && (
              <div className="mt-5 grid gap-4">
                <Field label="Rain threshold %" value={String(policy.rainThreshold)} onChange={(value) => setPolicy((current) => current ? { ...current, rainThreshold: Number(value) } : current)} type="number" />
                <Field label="Heat threshold C" value={String(policy.heatThresholdCelsius)} onChange={(value) => setPolicy((current) => current ? { ...current, heatThresholdCelsius: Number(value) } : current)} type="number" />
                <select value={policy.refundType} onChange={(e) => setPolicy((current) => current ? { ...current, refundType: e.target.value as WeatherPolicy["refundType"] } : current)} className="theme-input rounded-xl px-3 py-2 text-sm">
                  <option value="RESCHEDULE_ONLY">Reschedule only</option>
                  <option value="WALLET_CREDIT">Wallet credit</option>
                  <option value="PARTIAL">Partial refund</option>
                  <option value="FULL">Full refund</option>
                </select>
                <label className="theme-muted flex items-center gap-2 text-sm">
                  <input type="checkbox" checked={policy.rainRefundAllowed} onChange={(e) => setPolicy((current) => current ? { ...current, rainRefundAllowed: e.target.checked } : current)} />
                  Rain refund allowed
                </label>
                <label className="theme-muted flex items-center gap-2 text-sm">
                  <input type="checkbox" checked={policy.flexibleReschedule} onChange={(e) => setPolicy((current) => current ? { ...current, flexibleReschedule: e.target.checked } : current)} />
                  Flexible reschedule
                </label>
              </div>
            )}
            <button className="mt-5 rounded-2xl bg-sky-500 px-5 py-3 text-sm font-black text-white">{t("weather.savePolicy")}</button>
          </form>

          <form onSubmit={saveEnvironment} className="theme-panel rounded-3xl p-6">
            <h2 className="theme-title text-xl font-black">{t("weather.environment")}</h2>
            <p className="theme-muted mt-1 text-sm">{t("weather.environmentBody")}</p>
            <div className="mt-5 grid gap-4">
              <Field label="Turf ID" value={environment.turfId} onChange={(value) => setEnvironment((current) => ({ ...current, turfId: value }))} type="number" />
              <select value={environment.type} onChange={(e) => setEnvironment((current) => ({ ...current, type: e.target.value }))} className="theme-input rounded-xl px-3 py-2 text-sm">
                <option value="OUTDOOR">Outdoor</option>
                <option value="SEMI_INDOOR">Semi indoor</option>
                <option value="INDOOR">Indoor</option>
              </select>
              <label className="theme-muted flex items-center gap-2 text-sm">
                <input type="checkbox" checked={environment.rainProtection} onChange={(e) => setEnvironment((current) => ({ ...current, rainProtection: e.target.checked }))} />
                Rain protection
              </label>
              <label className="theme-muted flex items-center gap-2 text-sm">
                <input type="checkbox" checked={environment.heatProtection} onChange={(e) => setEnvironment((current) => ({ ...current, heatProtection: e.target.checked }))} />
                Heat protection
              </label>
            </div>
            <button className="mt-5 rounded-2xl border border-sky-500/40 px-5 py-3 text-sm font-black text-sky-600 dark:text-sky-200">{t("weather.saveEnvironment")}</button>
          </form>
        </section>
      </div>
    </DashboardLayout>
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

function Field({ label, value, onChange, type = "text" }: { label: string; value: string; onChange: (value: string) => void; type?: string }) {
  return (
    <label>
      <span className="theme-title text-xs font-black uppercase tracking-wider">{label}</span>
      <input type={type} value={value} onChange={(event) => onChange(event.target.value)} className="theme-input mt-2 w-full rounded-xl px-3 py-2 text-sm outline-none" />
    </label>
  );
}

function errorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { message?: string; error?: string } } };
  return apiError.response?.data?.message || apiError.response?.data?.error || fallback;
}
