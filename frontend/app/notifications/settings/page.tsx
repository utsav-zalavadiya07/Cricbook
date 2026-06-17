"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { getPreferences, updatePreference } from "@/modules/notification/services/notificationService";
import { NotificationPreference } from "@/modules/notification/types/notification.types";

const channelIcons: Record<string, string> = {
  EMAIL: "✉️",
  PUSH: "🔔",
  SMS: "💬",
  WHATSAPP: "🟢",
};

const categories = [
  "BOOKING",
  "PAYMENT",
  "TEAM",
  "MATCH",
  "TOURNAMENT",
  "REVIEW",
  "SOCIAL",
  "MARKETING",
];

export default function NotificationSettingsPage() {
  const [preferences, setPreferences] = useState<NotificationPreference[]>([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");

  const loadPrefs = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getPreferences();
      setPreferences(res.data || []);
    } catch (err) {
      console.error("Failed loading preferences", err);
      setMessage("Could not load notification preferences.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => void loadPrefs(), 0);
    return () => window.clearTimeout(timer);
  }, [loadPrefs]);

  const replacePreference = (preference: NotificationPreference) => {
    setPreferences((prev) => prev.map((item) => (item.channel === preference.channel ? preference : item)));
  };

  const showMessage = (text: string) => {
    setMessage(text);
    window.setTimeout(() => setMessage(""), 3000);
  };

  const handleToggle = async (channel: string, currentEnabled: boolean, frequency: string) => {
    try {
      const res = await updatePreference(channel, !currentEnabled, frequency);
      replacePreference(res.data);
      showMessage(`${channel} alerts ${!currentEnabled ? "enabled" : "disabled"}.`);
    } catch (err) {
      console.error("Failed updating preferences", err);
      showMessage("Could not update this channel preference.");
    }
  };

  const handleFrequencyChange = async (channel: string, enabled: boolean, newFrequency: string) => {
    try {
      const res = await updatePreference(channel, enabled, newFrequency);
      replacePreference(res.data);
      showMessage(`${channel} frequency updated to ${newFrequency}.`);
    } catch (err) {
      console.error("Failed updating frequency", err);
      showMessage("Could not update digest frequency.");
    }
  };

  const handleCategoryToggle = async (preference: NotificationPreference, category: string) => {
    const disabledCategories = preference.disabledCategories ?? [];
    const currentlyEnabled = !disabledCategories.includes(category);
    try {
      const res = await updatePreference(preference.channel, preference.enabled, preference.frequency, category, !currentlyEnabled);
      replacePreference(res.data);
      showMessage(`${category} alerts ${currentlyEnabled ? "disabled" : "enabled"} for ${preference.channel}.`);
    } catch (err) {
      console.error("Failed updating category preference", err);
      showMessage("Could not update category preference.");
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans">
      <header className="sticky top-0 z-50 flex items-center justify-between border-b border-white/5 bg-slate-900/60 px-6 py-4 backdrop-blur-md">
        <div className="flex items-center gap-2">
          <Link href="/notifications" className="text-slate-400 transition-all hover:text-white">← Back</Link>
          <h1 className="ml-2 text-lg font-bold tracking-wide text-white">Notification Preferences</h1>
        </div>
        <div className="rounded-full border border-emerald-500/20 bg-emerald-500/10 px-3 py-1 text-xs font-semibold text-emerald-400">
          Engagement Console
        </div>
      </header>

      <main className="mx-auto flex-1 w-full max-w-4xl space-y-6 px-6 py-10">
        <div className="space-y-2">
          <h2 className="text-2xl font-bold tracking-tight text-white">Channels and alert categories</h2>
          <p className="text-xs leading-relaxed text-slate-400">
            Control delivery channels and suppress noisy categories without disabling critical account alerts.
          </p>
        </div>

        {message && (
          <div className="rounded-xl border border-emerald-500/25 bg-emerald-500/10 p-3 text-xs text-emerald-400">
            {message}
          </div>
        )}

        {loading ? (
          <div className="flex h-48 items-center justify-center text-xs text-slate-400">
            Fetching notification preference registry...
          </div>
        ) : (
          <div className="space-y-4">
            {preferences.map((preference) => {
              const disabledCategories = preference.disabledCategories ?? [];
              return (
                <div
                  key={preference.channel}
                  className="rounded-2xl border border-white/5 bg-slate-900/40 p-5 backdrop-blur-lg transition-all hover:border-emerald-500/10"
                >
                  <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
                    <div className="flex items-start gap-4">
                      <span className="rounded-xl bg-slate-950 p-2 text-2xl">{channelIcons[preference.channel] || "📢"}</span>
                      <div>
                        <h3 className="text-sm font-bold text-white">{preference.channel} Alerts</h3>
                        <p className="mt-0.5 text-xs text-slate-500">
                          Receive booking, payment, team, match, tournament, and community alerts on {preference.channel.toLowerCase()}.
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-4 self-end md:self-auto">
                      <select
                        value={preference.frequency}
                        onChange={(event) => handleFrequencyChange(preference.channel, preference.enabled, event.target.value)}
                        className="cursor-pointer rounded-xl border border-white/10 bg-slate-950 px-2.5 py-1.5 text-xs text-slate-300 outline-none transition-all hover:border-white/20"
                      >
                        <option value="INSTANT">Instant</option>
                        <option value="DAILY">Daily Digest</option>
                        <option value="WEEKLY">Weekly Digest</option>
                      </select>

                      <button
                        onClick={() => handleToggle(preference.channel, preference.enabled, preference.frequency)}
                        className={`relative h-6 w-11 rounded-full border border-white/5 transition-all outline-none ${
                          preference.enabled ? "bg-emerald-500" : "bg-slate-800"
                        }`}
                        aria-label={`${preference.enabled ? "Disable" : "Enable"} ${preference.channel} notifications`}
                      >
                        <div className={`absolute top-1 h-4 w-4 rounded-full bg-white transition-all ${preference.enabled ? "left-6" : "left-1"}`} />
                      </button>
                    </div>
                  </div>

                  <div className="mt-5 grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
                    {categories.map((category) => {
                      const enabled = !disabledCategories.includes(category);
                      return (
                        <button
                          key={category}
                          onClick={() => handleCategoryToggle(preference, category)}
                          className={`rounded-xl border px-3 py-2 text-left text-xs font-bold transition ${
                            enabled
                              ? "border-emerald-500/25 bg-emerald-500/10 text-emerald-200"
                              : "border-white/10 bg-slate-950 text-slate-500"
                          }`}
                        >
                          <span className="block">{category}</span>
                          <span className="mt-1 block font-normal">{enabled ? "Enabled" : "Muted"}</span>
                        </button>
                      );
                    })}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </main>
    </div>
  );
}
