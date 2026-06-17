"use client";

import PlayerSettingsCenter from "@/modules/settings/components/PlayerSettingsCenter";

export default function SettingsPage() {
  return (
    <main className="theme-page-player min-h-screen px-4 py-8 sm:px-6">
      <div className="mx-auto max-w-7xl">
        <PlayerSettingsCenter />
      </div>
    </main>
  );
}
