"use client";
import { TileInfo } from "@/components/ui";

import { FormEvent, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { getMyBookings } from "@/modules/booking/services/bookingService";
import { Booking } from "@/modules/booking/types/booking.types";
import { useI18n } from "@/modules/i18n/I18nProvider";
import {
  createAutoWaitlist,
  createBookingTemplate,
  createRecurringBooking,
  getAutoWaitlists,
  getBookingIntelligence,
  getBookingPredictions,
  getBookingTemplates,
  getFrequentBookings,
  getRecurringBookings,
  rebookBooking,
} from "@/modules/rebook/services/rebookService";
import { AutoWaitlist, BookingIntelligence, BookingPrediction, BookingTemplate, FrequentBooking, RecurringBooking } from "@/modules/rebook/types/rebook.types";
import { useToast } from "@/modules/ui/ToastProvider";

const nextWeek = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);

export default function RebookPage() {
  const { t } = useI18n();
  const { toast } = useToast();
  const [frequent, setFrequent] = useState<FrequentBooking[]>([]);
  const [predictions, setPredictions] = useState<BookingPrediction[]>([]);
  const [recurring, setRecurring] = useState<RecurringBooking[]>([]);
  const [templates, setTemplates] = useState<BookingTemplate[]>([]);
  const [autoWaitlists, setAutoWaitlists] = useState<AutoWaitlist[]>([]);
  const [analytics, setAnalytics] = useState<BookingIntelligence | null>(null);
  const [recentBookings, setRecentBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState("");
  const [targetDate, setTargetDate] = useState(nextWeek);
  const [templateDraft, setTemplateDraft] = useState({ templateName: "", turfId: "", preferredDay: "FRIDAY", preferredTime: "20:00", durationMinutes: "120", players: "12" });
  const [recurringDraft, setRecurringDraft] = useState({ turfId: "", slotId: "", frequency: "WEEKLY", preferredDay: "FRIDAY", nextBookingDate: nextWeek, autoConfirm: false });
  const [waitlistDraft, setWaitlistDraft] = useState({ slotId: "", preferredDate: nextWeek, autoBookEnabled: false });

  const selectedBooking = useMemo(
    () => recentBookings.find((booking) => booking.id === Number(frequent[0]?.sourceBookingId)) || recentBookings[0],
    [frequent, recentBookings]
  );

  async function load() {
    setLoading(true);
    try {
      const [frequentRes, predictionRes, recurringRes, templateRes, waitRes, analyticsRes, bookingRes] = await Promise.all([
        getFrequentBookings(),
        getBookingPredictions(),
        getRecurringBookings(),
        getBookingTemplates(),
        getAutoWaitlists(),
        getBookingIntelligence(),
        getMyBookings(),
      ]);
      setFrequent(frequentRes.data || []);
      setPredictions(predictionRes.data || []);
      setRecurring(recurringRes.data || []);
      setTemplates(templateRes.data || []);
      setAutoWaitlists(waitRes.data || []);
      setAnalytics(analyticsRes.data || null);
      setRecentBookings(bookingRes.data || []);
      setStatus("");
    } catch (error: unknown) {
      setStatus(errorMessage(error, "Unable to load smart booking data."));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, []);

  const quickRebook = async (bookingId: number, date?: string) => {
    try {
      await rebookBooking(bookingId, date || targetDate);
      toast("Rebook reserved. Complete payment from bookings.", "success");
      await load();
    } catch (error: unknown) {
      toast(errorMessage(error, "Could not rebook this slot."), "error");
    }
  };

  const createTemplate = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await createBookingTemplate({
        templateName: templateDraft.templateName,
        turfId: Number(templateDraft.turfId),
        preferredDay: templateDraft.preferredDay,
        preferredTime: templateDraft.preferredTime,
        durationMinutes: Number(templateDraft.durationMinutes),
        players: Number(templateDraft.players),
      });
      toast("Booking template saved.", "success");
      setTemplateDraft({ templateName: "", turfId: "", preferredDay: "FRIDAY", preferredTime: "20:00", durationMinutes: "120", players: "12" });
      await load();
    } catch (error: unknown) {
      toast(errorMessage(error, "Could not save booking template."), "error");
    }
  };

  const createRecurring = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await createRecurringBooking({
        turfId: Number(recurringDraft.turfId),
        slotId: Number(recurringDraft.slotId),
        frequency: recurringDraft.frequency as "WEEKLY" | "BIWEEKLY" | "MONTHLY" | "CUSTOM",
        preferredDay: recurringDraft.preferredDay,
        nextBookingDate: recurringDraft.nextBookingDate,
        autoConfirm: recurringDraft.autoConfirm,
      });
      toast("Recurring booking schedule saved.", "success");
      setRecurringDraft({ turfId: "", slotId: "", frequency: "WEEKLY", preferredDay: "FRIDAY", nextBookingDate: nextWeek, autoConfirm: false });
      await load();
    } catch (error: unknown) {
      toast(errorMessage(error, "Could not create recurring booking."), "error");
    }
  };

  const createWaitlist = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await createAutoWaitlist({
        slotId: Number(waitlistDraft.slotId),
        preferredDate: waitlistDraft.preferredDate,
        autoBookEnabled: waitlistDraft.autoBookEnabled,
      });
      toast("Auto waitlist preference saved.", "success");
      setWaitlistDraft({ slotId: "", preferredDate: nextWeek, autoBookEnabled: false });
      await load();
    } catch (error: unknown) {
      toast(errorMessage(error, "Could not save auto waitlist."), "error");
    }
  };

  return (
    <main className="theme-page-player min-h-screen px-4 py-8 sm:px-6 lg:px-10">
      <div className="mx-auto max-w-7xl space-y-8">
        <section className="theme-panel-strong overflow-hidden rounded-[2rem] p-6 sm:p-8">
          <p className="theme-accent text-xs font-black uppercase tracking-[0.34em]">{t("rebook.epic")}</p>
          <div className="mt-4 grid gap-8 xl:grid-cols-[1.25fr_0.75fr] xl:items-end">
            <div>
              <h1 className="theme-title max-w-4xl text-4xl font-black tracking-tight sm:text-6xl">{t("rebook.title")}
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Page information" focusable={false}>{t("rebook.subtitle")}</TileInfo>
              </span></h1>
              <div className="mt-6 flex flex-wrap gap-3">
                {selectedBooking && (
                  <button onClick={() => quickRebook(selectedBooking.id)} className="rounded-2xl bg-emerald-500 px-5 py-3 text-sm font-black text-white shadow-lg shadow-emerald-500/20">
                    {t("rebook.action.quickRebook")}
                  </button>
                )}
                <Link href="/booking/history" className="theme-card rounded-2xl px-5 py-3 text-sm font-black">{t("rebook.action.history")}</Link>
              </div>
            </div>
            <div className="theme-panel-soft rounded-3xl p-5">
              <p className="theme-soft text-xs font-black uppercase tracking-[0.22em]">{t("rebook.readiness")}</p>
              <p className="theme-title mt-3 text-5xl font-black">{Math.round(analytics?.rebookReadiness || 0)}%</p>
              <p className="theme-muted mt-2 text-sm">{t("rebook.readinessBody")}</p>
              <div className="mt-5 grid grid-cols-2 gap-3">
                <Metric label={t("rebook.metric.frequent")} value={analytics?.frequentBookings ?? 0} />
                <Metric label={t("rebook.metric.recurring")} value={analytics?.recurringBookings ?? 0} />
                <Metric label={t("rebook.metric.templates")} value={analytics?.templates ?? 0} />
                <Metric label={t("rebook.metric.waitlists")} value={analytics?.autoWaitlists ?? 0} />
              </div>
            </div>
          </div>
        </section>

        {loading && <p className="theme-muted py-12 text-center">{t("common.loading")}</p>}
        {status && <div className="theme-panel-soft rounded-2xl p-4 text-sm font-semibold text-amber-600 dark:text-amber-200">{status}</div>}

        {!loading && (
          <>
            <section className="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
              <Panel title={t("rebook.usualMatch")} body={t("rebook.usualMatchBody")}>
                <div className="grid gap-4 lg:grid-cols-2">
                  {frequent.length === 0 && <Empty text={t("rebook.emptyFrequent")} />}
                  {frequent.map((item) => (
                    <article key={`${item.sourceBookingId}-${item.slotId}`} className="theme-panel-soft rounded-3xl p-5">
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <h3 className="theme-title text-lg font-black">{item.turfName}</h3>
                          <p className="theme-muted mt-1 text-sm">{item.city} - {item.startTime} to {item.endTime}</p>
                        </div>
                        <span className="theme-badge rounded-full px-3 py-1 text-xs font-black">{item.timesBooked}x</span>
                      </div>
                      <p className="theme-muted mt-3 text-sm">{item.recommendationReason}</p>
                      <div className="mt-4 grid grid-cols-[1fr_auto] gap-2">
                        <input type="date" value={targetDate} onChange={(event) => setTargetDate(event.target.value)} className="theme-input rounded-xl px-3 py-2 text-sm" />
                        <button onClick={() => quickRebook(item.sourceBookingId, targetDate)} className="rounded-xl bg-emerald-500 px-4 py-2 text-sm font-black text-white">{t("rebook.action.rebook")}</button>
                      </div>
                    </article>
                  ))}
                </div>
              </Panel>

              <Panel title={t("rebook.predictions")} body={t("rebook.predictionsBody")}>
                <div className="space-y-3">
                  {predictions.length === 0 && <Empty text={t("rebook.emptyPredictions")} />}
                  {predictions.map((item, index) => (
                    <div key={`${item.turfId}-${item.slotId}-${index}`} className="theme-panel-soft rounded-2xl p-4">
                      <div className="flex items-center justify-between gap-3">
                        <p className="theme-title font-black">{item.turfName || "Suggested turf"}</p>
                        <span className="rounded-full bg-emerald-500/15 px-3 py-1 text-xs font-black text-emerald-600 dark:text-emerald-200">{Math.round((item.confidence || 0) * 100)}%</span>
                      </div>
                      <p className="theme-muted mt-2 text-sm">{item.predictedDate} - {item.startTime} to {item.endTime}</p>
                      <p className="theme-soft mt-2 text-xs">{item.reason}</p>
                    </div>
                  ))}
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 xl:grid-cols-3">
              <form onSubmit={createRecurring} className="theme-panel rounded-3xl p-6">
                <h2 className="theme-title text-xl font-black">{t("rebook.recurringTitle")}</h2>
                <p className="theme-muted mt-1 text-sm">{t("rebook.recurringBody")}</p>
                <div className="mt-5 grid gap-3">
                  <Field label="Turf ID" value={recurringDraft.turfId} onChange={(value) => setRecurringDraft((current) => ({ ...current, turfId: value }))} type="number" />
                  <Field label="Slot ID" value={recurringDraft.slotId} onChange={(value) => setRecurringDraft((current) => ({ ...current, slotId: value }))} type="number" />
                  <Field label="Next date" value={recurringDraft.nextBookingDate} onChange={(value) => setRecurringDraft((current) => ({ ...current, nextBookingDate: value }))} type="date" />
                  <select value={recurringDraft.frequency} onChange={(event) => setRecurringDraft((current) => ({ ...current, frequency: event.target.value }))} className="theme-input rounded-xl px-3 py-2 text-sm">
                    <option value="WEEKLY">Weekly</option>
                    <option value="BIWEEKLY">Biweekly</option>
                    <option value="MONTHLY">Monthly</option>
                    <option value="CUSTOM">Custom</option>
                  </select>
                  <label className="theme-muted flex items-center gap-2 text-sm">
                    <input type="checkbox" checked={recurringDraft.autoConfirm} onChange={(event) => setRecurringDraft((current) => ({ ...current, autoConfirm: event.target.checked }))} />
                    {t("rebook.autoConfirm")}
                  </label>
                </div>
                <button className="mt-5 w-full rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-black text-white">{t("rebook.saveRecurring")}</button>
              </form>

              <form onSubmit={createTemplate} className="theme-panel rounded-3xl p-6">
                <h2 className="theme-title text-xl font-black">{t("rebook.templateTitle")}</h2>
                <p className="theme-muted mt-1 text-sm">{t("rebook.templateBody")}</p>
                <div className="mt-5 grid gap-3">
                  <Field label="Template name" value={templateDraft.templateName} onChange={(value) => setTemplateDraft((current) => ({ ...current, templateName: value }))} />
                  <Field label="Turf ID" value={templateDraft.turfId} onChange={(value) => setTemplateDraft((current) => ({ ...current, turfId: value }))} type="number" />
                  <Field label="Time" value={templateDraft.preferredTime} onChange={(value) => setTemplateDraft((current) => ({ ...current, preferredTime: value }))} type="time" />
                  <Field label="Players" value={templateDraft.players} onChange={(value) => setTemplateDraft((current) => ({ ...current, players: value }))} type="number" />
                </div>
                <button className="mt-5 w-full rounded-2xl bg-sky-500 px-4 py-3 text-sm font-black text-white">{t("rebook.saveTemplate")}</button>
              </form>

              <form onSubmit={createWaitlist} className="theme-panel rounded-3xl p-6">
                <h2 className="theme-title text-xl font-black">{t("rebook.waitlistTitle")}</h2>
                <p className="theme-muted mt-1 text-sm">{t("rebook.waitlistBody")}</p>
                <div className="mt-5 grid gap-3">
                  <Field label="Slot ID" value={waitlistDraft.slotId} onChange={(value) => setWaitlistDraft((current) => ({ ...current, slotId: value }))} type="number" />
                  <Field label="Preferred date" value={waitlistDraft.preferredDate} onChange={(value) => setWaitlistDraft((current) => ({ ...current, preferredDate: value }))} type="date" />
                  <label className="theme-muted flex items-center gap-2 text-sm">
                    <input type="checkbox" checked={waitlistDraft.autoBookEnabled} onChange={(event) => setWaitlistDraft((current) => ({ ...current, autoBookEnabled: event.target.checked }))} />
                    {t("rebook.autoBook")}
                  </label>
                </div>
                <button className="mt-5 w-full rounded-2xl border border-emerald-500/40 px-4 py-3 text-sm font-black text-emerald-600 dark:text-emerald-200">{t("rebook.saveWaitlist")}</button>
              </form>
            </section>

            <section className="grid gap-6 xl:grid-cols-3">
              <ListPanel title={t("rebook.activeRecurring")} items={recurring.map((item) => `${item.turfName} - ${item.frequency} - ${item.nextBookingDate}`)} empty={t("rebook.emptyRecurring")} />
              <ListPanel title={t("rebook.savedTemplates")} items={templates.map((item) => `${item.templateName} - ${item.turfName}`)} empty={t("rebook.emptyTemplates")} />
              <ListPanel title={t("rebook.autoWaitlists")} items={autoWaitlists.map((item) => `${item.turfName || `Slot ${item.slotId}`} - ${item.preferredDate} - ${item.status}`)} empty={t("rebook.emptyWaitlists")} />
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

function ListPanel({ title, items, empty }: { title: string; items: string[]; empty: string }) {
  return (
    <section className="theme-panel rounded-3xl p-6">
      <h2 className="theme-title text-xl font-black">{title}</h2>
      <div className="mt-4 space-y-3">
        {items.length === 0 && <Empty text={empty} />}
        {items.map((item) => <p key={item} className="theme-panel-soft rounded-2xl p-3 text-sm font-semibold">{item}</p>)}
      </div>
    </section>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="theme-card rounded-2xl p-3">
      <p className="theme-soft text-[10px] font-black uppercase tracking-[0.18em]">{label}</p>
      <p className="theme-title mt-2 text-2xl font-black">{value}</p>
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

function Empty({ text }: { text: string }) {
  return <p className="theme-card theme-muted rounded-2xl border-dashed p-6 text-center text-sm">{text}</p>;
}

function errorMessage(error: unknown, fallback: string) {
  const apiError = error as { response?: { data?: { message?: string; error?: string } } };
  return apiError.response?.data?.message || apiError.response?.data?.error || fallback;
}
