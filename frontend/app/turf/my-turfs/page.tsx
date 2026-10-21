"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import TurfCard from "@/modules/turf/components/TurfCard";
import TurfFormDialog from "@/modules/turf/components/TurfFormDialog";
import { useMyTurfs } from "@/modules/turf/hooks/useTurf";
import { useTurfTaxonomy } from "@/modules/turf/hooks/useTurfTaxonomy";
import { activateTurf, deactivateTurf, maintenanceTurf } from "@/modules/turf/services/turfService";
import { Button, Input, Select } from "@/components/ui";
import { useConfirmDialog } from "@/components/ui/ConfirmDialog";
import { PageMetricCard, PageMetricGrid, PageSection, PageSectionHeader } from "@/components/ui/PageSurface";
import type { Turf } from "@/modules/turf/types/turf.types";

const STATUS_FILTERS = [
  { value: "ALL", label: "All turfs" },
  { value: "ACTIVE", label: "Active" },
  { value: "MAINTENANCE", label: "Maintenance" },
  { value: "INACTIVE", label: "Inactive" },
  { value: "PENDING", label: "Pending" },
];

const SORT_OPTIONS = [
  { value: "name", label: "Name A-Z" },
  { value: "price-high", label: "Price high to low" },
  { value: "price-low", label: "Price low to high" },
  { value: "status", label: "Status" },
];

function normalizeStatus(status?: string) {
  return status || "ACTIVE";
}

function LoadingGrid() {
  return (
    <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3">
      {Array.from({ length: 3 }).map((_, index) => (
        <div key={index} className="theme-panel overflow-hidden rounded-[1.75rem]">
          <div className="aspect-[16/9] min-h-48 animate-pulse bg-[var(--theme-panel-soft)]" />
          <div className="space-y-4 p-5">
            <div className="flex items-center justify-between gap-4">
              <div className="h-6 w-1/3 animate-pulse rounded-full bg-[var(--theme-panel-soft)]" />
              <div className="h-7 w-20 animate-pulse rounded-full bg-[var(--theme-panel-soft)]" />
            </div>
            <div className="grid grid-cols-3 gap-3">
              <div className="h-14 animate-pulse rounded-2xl bg-[var(--theme-panel-soft)]" />
              <div className="h-14 animate-pulse rounded-2xl bg-[var(--theme-panel-soft)]" />
              <div className="h-14 animate-pulse rounded-2xl bg-[var(--theme-panel-soft)]" />
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

export default function MyTurfsPage() {
  const router = useRouter();
  const { turfs, loading, error, fetchTurfs } = useMyTurfs();
  const confirmAction = useConfirmDialog();
  const [dialogMode, setDialogMode] = useState<"create" | "edit" | null>(null);
  const [editingTurf, setEditingTurf] = useState<Turf | null>(null);
  const [requestedEditId, setRequestedEditId] = useState<string | null>(() =>
    typeof window === "undefined" ? null : new URLSearchParams(window.location.search).get("edit"),
  );
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [sortBy, setSortBy] = useState("name");
  const { sportOptions } = useTurfTaxonomy();

  useEffect(() => {
    fetchTurfs();
  }, [fetchTurfs]);

  const metrics = useMemo(() => {
    const total = turfs.length;
    const active = turfs.filter((turf) => normalizeStatus(turf.status) === "ACTIVE").length;
    const maintenance = turfs.filter((turf) => normalizeStatus(turf.status) === "MAINTENANCE").length;
    const inactive = turfs.filter((turf) => normalizeStatus(turf.status) === "INACTIVE").length;
    const averagePrice = total
      ? Math.round(turfs.reduce((sum, turf) => sum + Number(turf.pricePerHour || 0), 0) / total)
      : 0;

    return { total, active, maintenance, inactive, averagePrice };
  }, [turfs]);

  const filteredTurfs = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    return [...turfs]
      .filter((turf) => statusFilter === "ALL" || normalizeStatus(turf.status) === statusFilter)
      .filter((turf) => {
        if (!normalizedQuery) return true;
        return [
          turf.name,
          turf.city,
          turf.location,
          turf.pitchType,
          turf.boxType,
          turf.sport,
          ...(turf.sports || []),
        ]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(normalizedQuery));
      })
      .sort((a, b) => {
        if (sortBy === "price-high") return Number(b.pricePerHour || 0) - Number(a.pricePerHour || 0);
        if (sortBy === "price-low") return Number(a.pricePerHour || 0) - Number(b.pricePerHour || 0);
        if (sortBy === "status") return normalizeStatus(a.status).localeCompare(normalizeStatus(b.status));
        return a.name.localeCompare(b.name);
      });
  }, [query, sortBy, statusFilter, turfs]);

  const routedEditTurf = useMemo(
    () => (requestedEditId ? turfs.find((item) => String(item.id) === requestedEditId) || null : null),
    [requestedEditId, turfs],
  );
  const activeDialogMode = dialogMode || (routedEditTurf ? "edit" : null);
  const activeEditingTurf = dialogMode === "edit" ? editingTurf : routedEditTurf;

  const handleActivate = async (id: number) => {
    const confirmed = await confirmAction({
      title: "Activate this turf?",
      description: "Players will be able to discover and book this venue again.",
      confirmLabel: "Activate",
      tone: "owner",
    });
    if (!confirmed) return;
    await activateTurf(id);
    fetchTurfs();
  };

  const handleDeactivate = async (id: number) => {
    const confirmed = await confirmAction({
      title: "Deactivate this turf?",
      description: "This will hide the turf from player booking flows until you activate it again.",
      confirmLabel: "Deactivate",
      tone: "danger",
    });
    if (!confirmed) return;
    await deactivateTurf(id);
    fetchTurfs();
  };

  const handleMaintenance = async (id: number) => {
    const confirmed = await confirmAction({
      title: "Move turf to maintenance?",
      description: "The venue will be paused for bookings while maintenance is active.",
      confirmLabel: "Set maintenance",
      tone: "owner",
    });
    if (!confirmed) return;
    await maintenanceTurf(id);
    fetchTurfs();
  };

  const openCreateDialog = () => {
    setRequestedEditId(null);
    setEditingTurf(null);
    setDialogMode("create");
    router.replace("/turf/my-turfs", { scroll: false });
  };

  const openEditDialog = (turf: Turf) => {
    setEditingTurf(turf);
    setDialogMode("edit");
    if (turf.id) {
      setRequestedEditId(String(turf.id));
      router.replace(`/turf/my-turfs?edit=${turf.id}`, { scroll: false });
    }
  };

  const closeDialog = () => {
    setDialogMode(null);
    setEditingTurf(null);
    setRequestedEditId(null);
    router.replace("/turf/my-turfs", { scroll: false });
  };

  const handleDialogSaved = () => {
    closeDialog();
    fetchTurfs();
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <PageSection>
          <PageSectionHeader
            eyebrow="Owner turf inventory"
            title="My Turfs"
            description="Manage venues, slot readiness, pricing, and operational status from one focused owner workspace."
            actions={
              <Button tone="owner" onClick={openCreateDialog} className="w-full sm:w-auto">
                + Add Turf
              </Button>
            }
          />

          <PageMetricGrid>
            <PageMetricCard label="Total" value={metrics.total} helper="Registered venues" />
            <PageMetricCard label="Active" value={metrics.active} helper="Bookable now" />
            <PageMetricCard label="Maintenance" value={metrics.maintenance} helper="Temporarily paused" />
            <PageMetricCard label="Inactive" value={metrics.inactive} helper="Hidden from users" />
            <PageMetricCard label="Avg price" value={metrics.averagePrice ? `Rs ${metrics.averagePrice}` : "-"} helper="Per hour" />
          </PageMetricGrid>
        </PageSection>

        <PageSection className="space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="theme-title text-lg font-black">Turf portfolio</h2>
              <p className="theme-muted mt-1 text-xs">Find a venue and jump directly into its daily operations.</p>
            </div>
            <span className="theme-owner-badge rounded-full px-3 py-1.5 text-xs font-black">{turfs.length} venues</span>
          </div>
          <div className="grid grid-cols-1 gap-3 lg:grid-cols-[1fr_220px_220px]">
            <Input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search by turf, city, location, sport, pitch..."
              aria-label="Search my turfs"
            />
            <Select
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value)}
              options={STATUS_FILTERS}
              accent="owner"
              aria-label="Filter turfs by status"
            />
            <Select
              value={sortBy}
              onChange={(event) => setSortBy(event.target.value)}
              options={SORT_OPTIONS}
              accent="owner"
              aria-label="Sort turfs"
            />
          </div>
          <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
            <p className="theme-muted text-sm">
              Showing <span className="theme-title font-bold">{filteredTurfs.length}</span> of{" "}
              <span className="theme-title font-bold">{turfs.length}</span> turfs
            </p>
            {(query || statusFilter !== "ALL" || sortBy !== "name") && (
              <button
                type="button"
                onClick={() => {
                  setQuery("");
                  setStatusFilter("ALL");
                  setSortBy("name");
                }}
                className="theme-card theme-text rounded-xl px-4 py-2 text-sm font-bold transition hover:bg-[var(--theme-panel-soft)]"
              >
                Reset filters
              </button>
            )}
          </div>
        </PageSection>

        {error && <div className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-400">{error}</div>}

        {loading && <LoadingGrid />}

        {!loading && !error && turfs.length === 0 && (
          <div className="theme-panel flex min-h-80 flex-col items-center justify-center rounded-[2rem] border-dashed p-8 text-center">
            <span className="theme-owner-badge mb-4 grid h-14 w-14 place-items-center rounded-2xl text-xl font-black">CB</span>
            <p className="theme-title text-xl font-black">No turfs yet</p>
            <p className="theme-muted mt-2 max-w-md text-sm">Create your first venue, add sports and amenities, then open slots for bookings.</p>
            <Button tone="owner" onClick={openCreateDialog} className="mt-5">
              Create Turf
            </Button>
          </div>
        )}

        {!loading && !error && turfs.length > 0 && filteredTurfs.length === 0 && (
          <div className="theme-panel rounded-[2rem] p-8 text-center">
            <p className="theme-title text-lg font-black">No turfs match these filters</p>
            <p className="theme-muted mt-2 text-sm">Clear the search or status filter to see the full inventory.</p>
          </div>
        )}

        {!loading && !error && filteredTurfs.length > 0 && (
          <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3">
            {filteredTurfs.map((turf) => (
              <TurfCard
                key={turf.id}
                turf={turf}
                onActivate={handleActivate}
                onDeactivate={handleDeactivate}
                onMaintenance={handleMaintenance}
                onEdit={openEditDialog}
                sportOptions={sportOptions}
              />
            ))}
          </div>
        )}
      </div>

      <TurfFormDialog
        open={activeDialogMode !== null}
        mode={activeDialogMode || "create"}
        turf={activeEditingTurf}
        onClose={closeDialog}
        onSaved={handleDialogSaved}
      />
    </DashboardLayout>
  );
}
