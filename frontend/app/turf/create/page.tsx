import { TileInfo } from "@/components/ui";
import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import TurfForm from "@/modules/turf/components/TurfForm";

export const metadata = { title: "Create Turf | Cricbook" };

export default function CreateTurfPage() {
  return (
    <DashboardLayout>
      <div className="mx-auto max-w-2xl">
        {/* Header */}
        <div className="mb-8">
          <h1 className="theme-title text-2xl font-bold">Create New Turf
              <span className="inline-flex align-middle ml-3 mb-1">
                <TileInfo label="Create New Turf information" focusable={false}>Fill in the details to list your cricket turf on Cricbook.</TileInfo>
              </span></h1>
        </div>

        {/* Form card */}
        <div className="theme-panel rounded-3xl p-8">
          <TurfForm />
        </div>
      </div>
    </DashboardLayout>
  );
}
