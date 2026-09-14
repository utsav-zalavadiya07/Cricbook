"use client";
import { TileInfo } from "@/components/ui";

import { useEffect, useState } from "react";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import { useI18n } from "@/modules/i18n/I18nProvider";
import { getOwnerBookingIntelligence } from "@/modules/rebook/services/rebookService";
import { BookingIntelligence } from "@/modules/rebook/types/rebook.types";

export default function OwnerBookingIntelligencePage() {
  const { t } = useI18n();
  const [analytics, setAnalytics] = useState<BookingIntelligence | null>(null);
  const [status, setStatus] = useState("");

  useEffect(() => {
    let active = true;
    getOwnerBookingIntelligence()
      .then((res) => {
        if (active) setAnalytics(res.data);
      })
      .catch((error) => {
        if (active) setStatus(errorMessage(error, "Unable to load recurring customer metrics."));
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <section className="theme-panel rounded-[2rem] p-6 sm:p-8">
          <p className="theme-accent-owner text-xs font-black uppercase tracking-[0.34em]">{t("rebook.ownerEpic")}</p>
          <h1 className="theme-title mt-3 text-3xl font-black sm:text-5xl">{t("rebook.ownerTitle")}
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Page information" focusable={false}>{t("rebook.ownerSubtitle")}</TileInfo>
              </span></h1>
        </section>

        {status && <div className="theme-panel-soft rounded-2xl p-4 text-sm font-semibold text-amber-600 dark:text-amber-200">{status}</div>}

        <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <Metric label={t("rebook.metric.recurring")} value={analytics?.recurringBookings ?? 0} />
          <Metric label={t("rebook.ownerRecurringRevenue")} value={`${Math.round(analytics?.rebookReadiness || 0)}%`} />
          <Metric label={t("rebook.metric.frequent")} value={analytics?.frequentBookings ?? 0} />
          <Metric label={t("rebook.metric.conflicts")} value={analytics?.conflicts ?? 0} />
        </section>

        <section className="theme-panel rounded-3xl p-6">
          <h2 className="theme-title text-xl font-black">{t("rebook.ownerRetention")}</h2>
          <p className="theme-muted mt-2 text-sm">
            {analytics && analytics.recurringBookings > 0
              ? t("rebook.ownerRetentionActive")
              : t("rebook.ownerRetentionEmpty")}
          </p>
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

function errorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { message?: string; error?: string } } };
  return apiError.response?.data?.message || apiError.response?.data?.error || fallback;
}
