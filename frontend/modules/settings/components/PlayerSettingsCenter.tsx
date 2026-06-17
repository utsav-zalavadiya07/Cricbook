"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import ThemeToggle from "@/modules/theme/ThemeToggle";
import AvatarUploader from "@/modules/media/components/AvatarUploader";
import { removeProfilePhoto, uploadProfilePhoto } from "@/modules/media/services/mediaService";
import { useI18n } from "@/modules/i18n/I18nProvider";
import { useAuthStore } from "@/store/authStore";
import { languageOptions, LocaleCode, normalizeLocale } from "@/modules/i18n/messages";
import { getPlayerSettings, PlayerSettingsPayload, PlayerSettingsResponse, requestDataExport, updatePlayerSettings } from "../services/settingsService";
import { getSecuritySessions, logoutAllSecuritySessions, revokeSecuritySession, SecuritySessionsResponse } from "../services/sessionService";

type SectionKey = "account" | "privacy" | "booking" | "matchmaking" | "notifications" | "payments" | "language" | "security" | "data";

const sections: Array<{ key: SectionKey; title: string; body: string }> = [
  { key: "account", title: "settings.account", body: "settings.accountBody" },
  { key: "privacy", title: "settings.privacy", body: "settings.privacyBody" },
  { key: "booking", title: "settings.booking", body: "settings.bookingBody" },
  { key: "matchmaking", title: "settings.matchmaking", body: "settings.matchmakingBody" },
  { key: "notifications", title: "settings.notifications", body: "settings.notificationsBody" },
  { key: "payments", title: "settings.payments", body: "settings.paymentsBody" },
  { key: "language", title: "settings.language", body: "settings.languageBody" },
  { key: "security", title: "settings.security", body: "settings.securityBody" },
  { key: "data", title: "settings.data", body: "settings.dataBody" },
];

const days = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
const skillLevels = ["ANY", "BEGINNER", "INTERMEDIATE", "ADVANCED", "PROFESSIONAL"];

export default function PlayerSettingsCenter() {
  const { t, translateText, setLocale } = useI18n();
  const [active, setActive] = useState<SectionKey>("account");
  const [settings, setSettings] = useState<PlayerSettingsResponse | null>(null);
  const [draft, setDraft] = useState<PlayerSettingsPayload>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [sessionLoading, setSessionLoading] = useState(false);
  const [sessions, setSessions] = useState<SecuritySessionsResponse | null>(null);
  const [error, setError] = useState("");
  const logout = useAuthStore((state) => state.logout);

  useEffect(() => {
    let mounted = true;
    getPlayerSettings()
      .then((response) => {
        if (!mounted) return;
        setSettings(response.data);
        setDraft(toPayload(response.data));
      })
      .catch((err) => setError(readApiError(err, t("settings.loadError"))))
      .finally(() => mounted && setLoading(false));
    return () => {
      mounted = false;
    };
  }, [t]);

  const loadSessions = useCallback(async () => {
    setSessionLoading(true);
    try {
      const response = await getSecuritySessions();
      setSessions(response.data);
    } catch (err) {
      setError(readApiError(err, "Could not load active sessions."));
    } finally {
      setSessionLoading(false);
    }
  }, []);

  const readiness = useMemo(() => {
    if (!draft.profile) return 0;
    const values = [draft.profile.displayName, draft.profile.phone, draft.profile.bio, draft.profile.city, draft.booking?.preferredTime, draft.localization?.language];
    return Math.round((values.filter(Boolean).length / values.length) * 100);
  }, [draft]);

  const update = <K extends keyof PlayerSettingsPayload>(section: K, values: Partial<NonNullable<PlayerSettingsPayload[K]>>) => {
    setDraft((current) => ({ ...current, [section]: { ...(current[section] as object), ...values } }));
    setError("");
  };

  const save = async () => {
    setSaving(true);
    setError("");
    try {
      const response = await updatePlayerSettings(draft);
      setSettings(response.data);
      setDraft(toPayload(response.data));
      const nextLocale = normalizeLocale(response.data.localization.language);
      setLocale(nextLocale, { source: "manual" });
    } catch (err) {
      setError(readApiError(err, t("settings.saveError")));
    } finally {
      setSaving(false);
    }
  };

  const exportData = async () => {
    setSaving(true);
    setError("");
    try {
      const response = await requestDataExport();
      setSettings(response.data);
      setDraft(toPayload(response.data));
    } catch (err) {
      setError(readApiError(err, t("settings.exportError")));
    } finally {
      setSaving(false);
    }
  };

  const selectSection = (section: SectionKey) => {
    setActive(section);
    if (section === "security" && !sessions && !sessionLoading) {
      void loadSessions();
    }
  };

  const revokeSession = async (sessionId: string, current: boolean) => {
    setSaving(true);
    setError("");
    try {
      await revokeSecuritySession(sessionId);
      if (current) {
        await logout();
        window.location.assign("/auth/login");
        return;
      }
      await loadSessions();
    } catch (err) {
      setError(readApiError(err, "Could not revoke this session."));
    } finally {
      setSaving(false);
    }
  };

  const logoutAllSessions = async () => {
    setSaving(true);
    setError("");
    try {
      await logoutAllSecuritySessions();
      await logout();
      window.location.assign("/auth/login");
    } catch (err) {
      setError(readApiError(err, "Could not logout all devices."));
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div className="theme-panel rounded-3xl p-8 text-center text-sm font-semibold theme-muted">{t("settings.loading")}</div>;
  }

  if (!settings) {
    return <div className="theme-panel rounded-3xl p-8 text-center text-sm font-semibold text-red-500">{error || t("settings.loadError")}</div>;
  }

  return (
    <div className="space-y-6">
      <section className="theme-panel-strong overflow-hidden rounded-[2rem] p-6 sm:p-8">
        <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-xs font-black uppercase tracking-[0.34em] text-emerald-500">{t("settings.epic")}</p>
            <h2 className="theme-title mt-3 text-3xl font-black tracking-tight sm:text-4xl">{t("settings.title")}</h2>
            <p className="theme-muted mt-3 max-w-2xl text-sm leading-6">{t("settings.subtitle")}</p>
          </div>
          <div className="grid gap-3 sm:grid-cols-3 lg:min-w-[420px]">
            <Metric label={t("settings.readiness")} value={`${readiness}%`} />
            <Metric label={t("settings.language")} value={String(draft.localization?.language || "en").toUpperCase()} />
            <Metric label={t("settings.sessions")} value={settings.devices.activeSessions} />
          </div>
        </div>
      </section>

      {error && <Banner tone="error" text={error} />}

      <div className="grid gap-6 lg:grid-cols-[280px_1fr]">
        <aside className="theme-panel h-fit rounded-3xl p-3">
          <div className="grid gap-2">
            {sections.map((item) => (
              <button
                key={item.key}
                type="button"
                onClick={() => selectSection(item.key)}
                className={`rounded-2xl px-4 py-3 text-left transition ${active === item.key ? "bg-emerald-500 text-white shadow-lg shadow-emerald-500/20" : "theme-panel-soft theme-text hover:translate-x-1"}`}
              >
                <span className="block text-sm font-black">{t(item.title)}</span>
                <span className={`mt-1 block text-xs leading-5 ${active === item.key ? "text-emerald-50" : "theme-muted"}`}>{t(item.body)}</span>
              </button>
            ))}
          </div>
        </aside>

        <main className="space-y-6">
          {active === "account" && (
            <Panel title={t("settings.account")} eyebrow={t("settings.profileEyebrow")} body={t("settings.profileBody")}>
              <AvatarUploader
                title="Profile Photo"
                body="Use a clear cricket profile photo so teams, owners, and players can recognize you across bookings, chat, and community features."
                imageUrl={draft.profile?.photoUrl}
                fallbackText={draft.profile?.displayName || settings.profile.displayName || "CB"}
                onUpload={async (file) => {
                  const response = await uploadProfilePhoto(file, draft.profile?.visibility || "PUBLIC");
                  return response.data;
                }}
                onRemove={async () => {
                  const response = await removeProfilePhoto();
                  return response.data;
                }}
                onChange={(asset) => update("profile", { photoUrl: asset.imageUrl })}
              />
              <div className="grid gap-4 md:grid-cols-2">
                <Field label={t("settings.displayName")} value={draft.profile?.displayName || ""} onChange={(value) => update("profile", { displayName: value })} />
                <Field label={t("settings.phone")} value={draft.profile?.phone || ""} onChange={(value) => update("profile", { phone: value })} />
                <Field label={t("settings.city")} value={draft.profile?.city || ""} onChange={(value) => update("profile", { city: value })} />
                <Field label={t("settings.role")} value={draft.profile?.position || ""} onChange={(value) => update("profile", { position: value })} />
                <Field label={t("settings.bio")} value={draft.profile?.bio || ""} onChange={(value) => update("profile", { bio: value })} multiline />
              </div>
            </Panel>
          )}

          {active === "privacy" && (
            <Panel title={t("settings.privacy")} eyebrow={t("settings.privacyEyebrow")} body={t("settings.privacyBodyLong")}>
              <div className="grid gap-4 md:grid-cols-2">
                <SelectField label={t("settings.profileVisibility")} value={draft.privacy?.profileVisibility || "PUBLIC"} onChange={(value) => update("privacy", { profileVisibility: value })} options={["PUBLIC", "FRIENDS", "PRIVATE"]} />
                <Toggle label={t("settings.showTrustScore")} checked={!!draft.privacy?.showTrustScore} onChange={(value) => update("privacy", { showTrustScore: value })} />
                <Toggle label={t("settings.showStats")} checked={!!draft.privacy?.showStats} onChange={(value) => update("privacy", { showStats: value })} />
                <Toggle label={t("settings.showAvailability")} checked={!!draft.privacy?.showAvailability} onChange={(value) => update("privacy", { showAvailability: value })} />
                <Toggle label={t("settings.showLocation")} checked={!!draft.privacy?.showLocation} onChange={(value) => update("privacy", { showLocation: value })} />
              </div>
            </Panel>
          )}

          {active === "booking" && (
            <Panel title={t("settings.booking")} eyebrow={t("settings.bookingEyebrow")} body={t("settings.bookingBodyLong")}>
              <div className="grid gap-4 md:grid-cols-2">
                <SelectField label={t("settings.preferredDay")} value={draft.booking?.preferredDay || ""} onChange={(value) => update("booking", { preferredDay: value })} options={days} />
                <Field label={t("settings.preferredTime")} value={draft.booking?.preferredTime || ""} onChange={(value) => update("booking", { preferredTime: value })} type="time" />
                <Field label={t("settings.preferredCity")} value={draft.booking?.preferredCity || ""} onChange={(value) => update("booking", { preferredCity: value })} />
                <Field label={t("settings.preferredDuration")} value={String(draft.booking?.preferredDurationMinutes || "")} onChange={(value) => update("booking", { preferredDurationMinutes: Number(value) || undefined })} type="number" />
                <Toggle label={t("settings.quickRebook")} checked={!!draft.booking?.quickRebookEnabled} onChange={(value) => update("booking", { quickRebookEnabled: value })} />
                <Toggle label={t("settings.autoWaitlist")} checked={!!draft.booking?.autoWaitlistEnabled} onChange={(value) => update("booking", { autoWaitlistEnabled: value })} />
              </div>
            </Panel>
          )}

          {active === "matchmaking" && (
            <Panel title={t("settings.matchmaking")} eyebrow={t("settings.matchmakingEyebrow")} body={t("settings.matchmakingBodyLong")}>
              <div className="grid gap-4 md:grid-cols-2">
                <SelectField label={t("settings.competitionLevel")} value={draft.matchmaking?.competitionLevel || "CASUAL"} onChange={(value) => update("matchmaking", { competitionLevel: value })} options={["CASUAL", "COMPETITIVE", "TOURNAMENT", "CORPORATE"]} />
                <SelectField label={t("settings.preferredSkill")} value={draft.matchmaking?.preferredSkillLevel || "ANY"} onChange={(value) => update("matchmaking", { preferredSkillLevel: value })} options={skillLevels} />
                <Toggle label={t("settings.trustedOnly")} checked={!!draft.matchmaking?.trustedOnly} onChange={(value) => update("matchmaking", { trustedOnly: value })} />
                <Toggle label={t("settings.nearbyOnly")} checked={!!draft.matchmaking?.nearbyOnly} onChange={(value) => update("matchmaking", { nearbyOnly: value })} />
              </div>
            </Panel>
          )}

          {active === "notifications" && (
            <Panel title={t("settings.notifications")} eyebrow={t("settings.notificationsEyebrow")} body={t("settings.notificationsBodyLong")}>
              <div className="grid gap-4 md:grid-cols-2">
                <Toggle label={t("settings.push")} checked={!!draft.notifications?.push} onChange={(value) => update("notifications", { push: value })} />
                <Toggle label={t("settings.email")} checked={!!draft.notifications?.email} onChange={(value) => update("notifications", { email: value })} />
                <Toggle label={t("settings.sms")} checked={!!draft.notifications?.sms} onChange={(value) => update("notifications", { sms: value })} />
                <Toggle label={t("settings.whatsapp")} checked={!!draft.notifications?.whatsapp} onChange={(value) => update("notifications", { whatsapp: value })} />
                <SelectField label={t("settings.frequency")} value={draft.notifications?.frequency || "INSTANT"} onChange={(value) => update("notifications", { frequency: value })} options={["INSTANT", "DAILY", "WEEKLY"]} />
              </div>
            </Panel>
          )}

          {active === "payments" && (
            <Panel title={t("settings.payments")} eyebrow={t("settings.paymentsEyebrow")} body={t("settings.paymentsBodyLong")}>
              <div className="grid gap-4 md:grid-cols-2">
                <SelectField label={t("settings.defaultPaymentMethod")} value={draft.payments?.defaultPaymentMethod || ""} onChange={(value) => update("payments", { defaultPaymentMethod: value })} options={["UPI", "WALLET", "CARD"]} />
                <Field label={t("settings.savedUpi")} value={draft.payments?.savedUpiId || ""} onChange={(value) => update("payments", { savedUpiId: value })} />
                <Toggle label={t("settings.autoPaySplits")} checked={!!draft.payments?.autoPaySplits} onChange={(value) => update("payments", { autoPaySplits: value })} />
              </div>
            </Panel>
          )}

          {active === "language" && (
            <Panel title={t("settings.language")} eyebrow={t("settings.languageEyebrow")} body={t("settings.languageBodyLong")}>
              <div className="grid gap-4 md:grid-cols-2">
                <label>
                  <span className="theme-title text-sm font-bold">{t("settings.language")}</span>
                  <select
                    className="theme-input mt-2 w-full rounded-2xl px-4 py-3 text-sm outline-none"
                    value={draft.localization?.language || "en"}
                    onChange={(event) => {
                      const locale = normalizeLocale(event.target.value) as LocaleCode;
                      update("localization", { language: locale });
                      setLocale(locale, { source: "manual" });
                    }}
                  >
                    {languageOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                  </select>
                </label>
                <Field label={t("settings.regionCity")} value={draft.localization?.regionCity || ""} onChange={(value) => update("localization", { regionCity: value })} />
                <SelectField label={t("settings.currency")} value={draft.localization?.currency || "INR"} onChange={(value) => update("localization", { currency: value })} options={["INR", "USD", "AED"]} />
                <Field label={t("settings.timezone")} value={draft.localization?.timezone || "Asia/Kolkata"} onChange={(value) => update("localization", { timezone: value })} />
              </div>
            </Panel>
          )}

          {active === "security" && (
            <Panel title={t("settings.security")} eyebrow={t("settings.securityEyebrow")} body={t("settings.securityBodyLong")}>
              <div className="grid gap-4 md:grid-cols-2">
                <div className="theme-panel-soft rounded-3xl p-5">
                  <h3 className="theme-title text-base font-black">{t("settings.appearance")}</h3>
                  <p className="theme-muted mt-2 text-sm">{t("settings.appearanceBody")}</p>
                  <ThemeToggle className="mt-4" />
                </div>
                <div className="theme-panel-soft rounded-3xl p-5">
                  <h3 className="theme-title text-base font-black">{t("settings.devices")}</h3>
                  <Info label={t("settings.currentDevice")} value={settings.devices.currentDevice} />
                  <Info label={t("settings.lastActivity")} value={settings.devices.lastActivity} />
                  <Toggle label={t("settings.twoFactor")} checked={!!draft.security?.twoFactorEnabled} onChange={(value) => update("security", { twoFactorEnabled: value })} />
                </div>
              </div>
              <div className="theme-panel-soft rounded-3xl p-5">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <h3 className="theme-title text-base font-black">Active Sessions</h3>
                    <p className="theme-muted mt-1 text-sm">Review current devices and revoke sessions you do not recognize.</p>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <button type="button" onClick={loadSessions} disabled={sessionLoading} className="rounded-2xl border border-[var(--theme-border)] px-4 py-2 text-xs font-black theme-text disabled:opacity-60">
                      {sessionLoading ? "Loading" : "Refresh"}
                    </button>
                    <button type="button" onClick={logoutAllSessions} disabled={saving} className="rounded-2xl border border-red-500/40 px-4 py-2 text-xs font-black text-red-500 disabled:opacity-60">
                      Logout all devices
                    </button>
                  </div>
                </div>
                <div className="mt-4 grid gap-3">
                  {sessionLoading && <p className="theme-muted text-sm">Loading active sessions...</p>}
                  {!sessionLoading && sessions?.sessions.length === 0 && <p className="theme-muted text-sm">No active sessions found.</p>}
                  {!sessionLoading && sessions?.sessions.map((session) => (
                    <div key={session.sessionId} className="rounded-2xl border border-[var(--theme-border)] bg-[var(--theme-card)] p-4">
                      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                        <div>
                          <div className="flex flex-wrap items-center gap-2">
                            <p className="theme-title text-sm font-black">{session.deviceName}</p>
                            {session.current && <span className="rounded-full bg-emerald-500/15 px-2 py-1 text-[0.65rem] font-black uppercase tracking-[0.16em] text-emerald-500">Current</span>}
                            {session.rememberMe && <span className="rounded-full bg-sky-500/15 px-2 py-1 text-[0.65rem] font-black uppercase tracking-[0.16em] text-sky-500">Remembered</span>}
                          </div>
                          <p className="theme-muted mt-1 text-xs">{session.browser} • {session.os} • {session.ipAddress}</p>
                          <p className="theme-muted mt-1 text-xs">Last activity: {formatSessionTime(session.lastActivity)} • Expires: {formatSessionTime(session.expiresAt)}</p>
                        </div>
                        <button
                          type="button"
                          onClick={() => revokeSession(session.sessionId, session.current)}
                          disabled={saving}
                          className="rounded-2xl border border-[var(--theme-border)] px-4 py-2 text-xs font-black theme-text transition hover:border-red-500/50 hover:text-red-500 disabled:opacity-60"
                        >
                          {session.current ? "Sign out" : "Revoke"}
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </Panel>
          )}

          {active === "data" && (
            <Panel title={t("settings.data")} eyebrow={t("settings.dataEyebrow")} body={t("settings.dataBodyLong")}>
              <div className="theme-panel-soft rounded-3xl p-5">
                <p className="theme-title text-lg font-black">{t("settings.exportData")}</p>
                <p className="theme-muted mt-2 text-sm">{t("settings.exportBody")}</p>
                <p className="theme-soft mt-3 text-xs">{t("settings.lastExport")}: {settings.data.lastExportRequestedAt || t("settings.never")}</p>
                <button type="button" onClick={exportData} disabled={saving} className="mt-5 rounded-2xl bg-emerald-500 px-5 py-3 text-sm font-black text-white shadow-lg shadow-emerald-500/20 disabled:opacity-60">
                  {t("settings.requestExport")}
                </button>
              </div>
              <div className="grid gap-3 md:grid-cols-2">
                {settings.smartSuggestions.map((suggestion) => (
                  <div key={suggestion} className="theme-card rounded-2xl p-4 text-sm font-semibold theme-text">
                    {translateText(suggestion)}
                  </div>
                ))}
              </div>
            </Panel>
          )}

          <div className="flex flex-col gap-3 rounded-3xl border border-[var(--theme-border)] bg-[var(--theme-card-strong)] p-4 shadow-xl sm:flex-row sm:items-center sm:justify-between">
            <p className="theme-muted text-sm">{t("settings.saveHint")}</p>
            <button type="button" onClick={save} disabled={saving} className="rounded-2xl bg-gradient-to-r from-emerald-500 to-teal-600 px-6 py-3 text-sm font-black text-white shadow-lg shadow-emerald-500/20 disabled:opacity-60">
              {saving ? t("settings.saving") : t("settings.save")}
            </button>
          </div>
        </main>
      </div>
    </div>
  );
}

function toPayload(settings: PlayerSettingsResponse): PlayerSettingsPayload {
  return {
    profile: settings.profile,
    security: settings.security,
    notifications: settings.notifications,
    privacy: settings.privacy,
    booking: settings.booking,
    matchmaking: settings.matchmaking,
    community: settings.community,
    payments: settings.payments,
    localization: settings.localization,
    data: settings.data,
  };
}

function Panel({ eyebrow, title, body, children }: { eyebrow: string; title: string; body: string; children: React.ReactNode }) {
  return (
    <section className="theme-panel rounded-[2rem] p-6 sm:p-8">
      <p className="text-xs font-black uppercase tracking-[0.26em] text-emerald-500">{eyebrow}</p>
      <h3 className="theme-title mt-2 text-2xl font-black">{title}</h3>
      <p className="theme-muted mt-2 text-sm leading-6">{body}</p>
      <div className="mt-6 space-y-5">{children}</div>
    </section>
  );
}

function Field({ label, value, onChange, type = "text", multiline = false }: { label: string; value: string; onChange: (value: string) => void; type?: string; multiline?: boolean }) {
  return (
    <label className={multiline ? "md:col-span-2" : ""}>
      <span className="theme-title text-sm font-bold">{label}</span>
      {multiline ? (
        <textarea value={value} onChange={(event) => onChange(event.target.value)} className="theme-input mt-2 min-h-28 w-full resize-none rounded-2xl px-4 py-3 text-sm outline-none" />
      ) : (
        <input type={type} value={value} onChange={(event) => onChange(event.target.value)} className="theme-input mt-2 w-full rounded-2xl px-4 py-3 text-sm outline-none" />
      )}
    </label>
  );
}

function SelectField({ label, value, onChange, options }: { label: string; value: string; onChange: (value: string) => void; options: string[] }) {
  return (
    <label>
      <span className="theme-title text-sm font-bold">{label}</span>
      <select value={value} onChange={(event) => onChange(event.target.value)} className="theme-input mt-2 w-full rounded-2xl px-4 py-3 text-sm outline-none">
        <option value="">-</option>
        {options.map((option) => <option key={option} value={option}>{option.replace(/_/g, " ")}</option>)}
      </select>
    </label>
  );
}

function Toggle({ label, checked, onChange }: { label: string; checked: boolean; onChange: (value: boolean) => void }) {
  return (
    <button type="button" onClick={() => onChange(!checked)} className="theme-panel-soft flex items-center justify-between gap-4 rounded-2xl p-4 text-left">
      <span className="theme-title text-sm font-bold">{label}</span>
      <span className={`inline-flex h-7 w-12 items-center rounded-full p-1 transition ${checked ? "bg-emerald-500" : "bg-slate-300 dark:bg-slate-700"}`}>
        <span className={`h-5 w-5 rounded-full bg-white transition ${checked ? "translate-x-5" : ""}`} />
      </span>
    </button>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return <div className="theme-panel-soft rounded-2xl p-4"><p className="theme-soft text-xs font-black uppercase tracking-[0.2em]">{label}</p><p className="theme-title mt-2 text-2xl font-black">{value}</p></div>;
}

function Info({ label, value }: { label: string; value: string | number }) {
  return <p className="theme-muted mt-3 text-sm"><span className="theme-title font-bold">{label}:</span> {value}</p>;
}

function Banner({ tone, text }: { tone: "success" | "error"; text: string }) {
  return <div className={`rounded-2xl border px-4 py-3 text-sm font-bold ${tone === "success" ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-300" : "border-red-500/30 bg-red-500/10 text-red-600 dark:text-red-300"}`}>{text}</div>;
}

function readApiError(error: unknown, fallback: string) {
  if (typeof error === "object" && error && "response" in error) {
    const response = (error as { response?: { data?: { message?: string; error?: string } } }).response;
    return response?.data?.message || response?.data?.error || fallback;
  }
  return fallback;
}

function formatSessionTime(value: string | null | undefined) {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}
