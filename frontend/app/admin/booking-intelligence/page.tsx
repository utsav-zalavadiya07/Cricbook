"use client";

import { useEffect, useState } from "react";
import { useI18n } from "@/modules/i18n/I18nProvider";
import { getAdminBookingIntelligence } from "@/modules/rebook/services/rebookService";
import { BookingIntelligence } from "@/modules/rebook/types/rebook.types";

export default function AdminBookingIntelligencePage() {
  const { t } = useI18n();
  const [analytics, setAnalytics] = useState<BookingIntelligence | null>(null);
  const [status, setStatus] = useState("");

  useEffect(() => {
    let active = true;
    getAdminBookingIntelligence()
      .then((res) => {
        if (active) setAnalytics(res.data);
      })
      .catch((error) => {
        if (active) setStatus(errorMessage(error, "Unable to load booking intelligence metrics."));
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <main className="theme-page-owner min-h-screen p-4 sm:p-6 lg:p-8">
      <div className="mx-auto max-w-6xl space-y-6">
        <section className="theme-panel rounded-[2rem] p-6 sm:p-8">
          <p className="theme-accent-owner text-xs font-black uppercase tracking-[0.34em]">{t("rebook.adminEpic")}</p>
          <h1 className="theme-title mt-3 text-3xl font-black sm:text-5xl">{t("rebook.adminTitle")}</h1>
          <p className="theme-muted mt-3 max-w-3xl text-sm leading-6">{t("rebook.adminSubtitle")}</p>
        </section>

        {status && <div className="theme-panel-soft rounded-2xl p-4 text-sm font-semibold text-amber-600 dark:text-amber-200">{status}</div>}

        <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Metric label={t("rebook.metric.frequent")} value={analytics?.frequentBookings ?? 0} />
          <Metric label={t("rebook.metric.recurring")} value={analytics?.recurringBookings ?? 0} />
          <Metric label={t("rebook.metric.templates")} value={analytics?.templates ?? 0} />
          <Metric label={t("rebook.metric.waitlists")} value={analytics?.autoWaitlists ?? 0} />
          <Metric label={t("rebook.metric.conflicts")} value={analytics?.conflicts ?? 0} />
          <Metric label={t("rebook.readiness")} value={`${Math.round(analytics?.rebookReadiness || 0)}%`} />
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
