"use client";
import { TileInfo } from "@/components/ui";

import { useParams, useRouter } from "next/navigation";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import SlotManager from "@/modules/turf/components/SlotManager";

export default function SlotManagementPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const turfId = Number(id);

  return (
    <DashboardLayout>
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <div className="flex items-center gap-2 text-sm text-slate-400 mb-2">
              <span className="hover:text-white cursor-pointer" onClick={() => router.push("/turf/my-turfs")}>My Turfs</span>
              <span>/</span>
              <span className="hover:text-white cursor-pointer" onClick={() => router.push(`/turf/${turfId}/edit`)}>Turf</span>
              <span>/</span>
              <span className="text-sky-400">Slots</span>
            </div>
            <h1 className="text-white text-2xl font-bold">Manage Turf Slots
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Manage Turf Slots information" focusable={false}>Edit availability, pricing and blocking rules for this turf.</TileInfo>
              </span></h1>
          </div>
          <button onClick={() => router.back()} className="text-slate-400 hover:text-white text-sm transition border border-white/10 rounded-xl px-4 py-2 hover:bg-white/5">
            ← Back
          </button>
        </div>

        {/* Slot Manager */}
        <div className="bg-white/5 border border-white/10 rounded-3xl p-6 sm:p-8">
          <SlotManager turfId={turfId} />
        </div>
      </div>
    </DashboardLayout>
  );
}
