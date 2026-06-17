"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { getPlatformEpics, PlatformEpic } from "@/modules/admin/services/adminService";

const statusClass: Record<string, string> = {
  ACTIVE: "border-emerald-400/30 bg-emerald-500/10 text-emerald-300",
  READY: "border-emerald-400/30 bg-emerald-500/10 text-emerald-300",
  PASS: "border-emerald-400/30 bg-emerald-500/10 text-emerald-300",
  PLANNED: "border-sky-400/30 bg-sky-500/10 text-sky-300",
};

function Badge({ value }: { value: string }) {
  return (
    <span className={`inline-flex rounded-full border px-3 py-1 text-xs font-black uppercase tracking-[0.16em] ${statusClass[value] || "theme-card theme-muted"}`}>
      {value}
    </span>
  );
}

export default function PlatformEpicsPage() {
  const router = useRouter();
  const [epics, setEpics] = useState<PlatformEpic[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const activeEpic = useMemo(() => epics.find((epic) => epic.releaseStage === "ACTIVE") || epics[0], [epics]);

  useEffect(() => {
    const role = localStorage.getItem("role");
    const token = localStorage.getItem("token");
    if (!token || role !== "ADMIN") {
      router.push("/auth/login");
      return;
    }

    getPlatformEpics()
      .then((response) => setEpics(response.data))
      .catch((err) => {
        const message = err?.response?.data?.message || "Unable to load platform epic registry.";
        setError(message);
      })
      .finally(() => setLoading(false));
  }, [router]);

  if (loading) {
    return <main className="theme-page-admin min-h-screen px-6 py-10"><p className="theme-muted">Loading platform epic registry...</p></main>;
  }

  return (
    <main className="theme-page-admin min-h-screen px-4 py-8 sm:px-6 lg:px-10">
      <section className="mx-auto max-w-7xl">
        <div className="theme-panel-strong overflow-hidden rounded-[2rem] p-6 sm:p-8">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="theme-accent-owner text-xs font-black uppercase tracking-[0.34em]">Platform governance</p>
              <h1 className="theme-title mt-3 text-3xl font-black tracking-tight sm:text-5xl">Epic delivery cockpit</h1>
              <p className="theme-muted mt-3 max-w-3xl text-base leading-7">
                Every new Cricbook epic must declare backend, player web, owner web, admin, mobile, notification, analytics,
                theme, and offline/realtime coverage before implementation starts.
              </p>
            </div>
            {activeEpic ? <Badge value={activeEpic.releaseStage} /> : null}
          </div>

          {error ? <div className="mt-6 rounded-2xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">{error}</div> : null}

          {activeEpic ? (
            <div className="mt-8 grid gap-5 lg:grid-cols-[1fr_0.85fr]">
              <article className="theme-card-strong rounded-[1.6rem] p-5">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p className="theme-soft text-xs font-black uppercase tracking-[0.24em]">{activeEpic.epicId}</p>
                    <h2 className="theme-title mt-2 text-2xl font-black">{activeEpic.name}</h2>
                  </div>
                  <Badge value={activeEpic.releaseStage} />
                </div>
                <p className="theme-muted mt-4 leading-7">{activeEpic.objective}</p>
                <div className="mt-5 flex flex-wrap gap-2">
                  {activeEpic.sharedDomains.map((domain) => (
                    <span key={domain} className="theme-owner-badge rounded-full px-3 py-1 text-xs font-bold">{domain}</span>
                  ))}
                </div>
              </article>

              <article className="theme-card-strong rounded-[1.6rem] p-5">
                <h3 className="theme-title text-lg font-black">Readiness Gate</h3>
                <div className="mt-4 grid gap-3">
                  {activeEpic.readiness.map((item) => (
                    <div key={item.key} className="theme-card flex items-center justify-between gap-3 rounded-2xl px-4 py-3">
                      <span className="theme-muted text-sm font-semibold">{item.label}</span>
                      <Badge value={item.status} />
                    </div>
                  ))}
                </div>
              </article>
            </div>
          ) : null}

          <div className="mt-8 grid gap-5 lg:grid-cols-5">
            {activeEpic?.surfaces.map((surface) => (
              <article key={surface.surface} className="theme-card rounded-[1.4rem] p-4">
                <div className="flex items-center justify-between gap-3">
                  <h3 className="theme-title text-sm font-black">{surface.surface.replaceAll("_", " ")}</h3>
                  <Badge value={surface.status} />
                </div>
                <p className="theme-soft mt-2 text-xs font-bold uppercase tracking-[0.18em]">{surface.owner}</p>
                <ul className="mt-4 space-y-2">
                  {surface.capabilities.map((capability) => (
                    <li key={capability} className="theme-muted text-sm leading-5">{capability}</li>
                  ))}
                </ul>
              </article>
            ))}
          </div>

          <div className="mt-8 grid gap-5 lg:grid-cols-2">
            <article className="theme-card rounded-[1.4rem] p-5">
              <h3 className="theme-title text-lg font-black">Notification Events</h3>
              <div className="mt-4 flex flex-wrap gap-2">
                {activeEpic?.notificationEvents.map((event) => (
                  <span key={event} className="rounded-full border border-emerald-400/20 bg-emerald-500/10 px-3 py-1 text-xs font-bold text-emerald-300">{event}</span>
                ))}
              </div>
            </article>
            <article className="theme-card rounded-[1.4rem] p-5">
              <h3 className="theme-title text-lg font-black">Analytics Events</h3>
              <div className="mt-4 flex flex-wrap gap-2">
                {activeEpic?.analyticsEvents.map((event) => (
                  <span key={event} className="rounded-full border border-sky-400/20 bg-sky-500/10 px-3 py-1 text-xs font-bold text-sky-300">{event}</span>
                ))}
              </div>
            </article>
          </div>

          <div className="mt-8">
            <h3 className="theme-title text-xl font-black">Upcoming Epic Templates</h3>
            <div className="mt-4 grid gap-4 md:grid-cols-2">
              {epics.filter((epic) => epic.epicId !== activeEpic?.epicId).map((epic) => (
                <article key={epic.epicId} className="theme-card rounded-[1.4rem] p-5">
                  <div className="flex items-center justify-between gap-3">
                    <p className="theme-soft text-xs font-black uppercase tracking-[0.2em]">{epic.epicId}</p>
                    <Badge value={epic.releaseStage} />
                  </div>
                  <h4 className="theme-title mt-2 text-lg font-black">{epic.name}</h4>
                  <p className="theme-muted mt-2 text-sm leading-6">{epic.objective}</p>
                </article>
              ))}
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
