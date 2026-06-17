import DashboardLayout from "@/modules/turf/components/DashboardLayout";
import TurfForm from "@/modules/turf/components/TurfForm";

export const metadata = { title: "Create Turf | Cricbook" };

export default function CreateTurfPage() {
  return (
    <DashboardLayout>
      <div className="mx-auto max-w-2xl">
        {/* Header */}
        <div className="mb-8">
          <h1 className="theme-title text-2xl font-bold">Create New Turf</h1>
          <p className="theme-muted mt-1 text-sm">
            Fill in the details to list your cricket turf on Cricbook.
          </p>
        </div>

        {/* Form card */}
        <div className="theme-panel rounded-3xl p-8">
          <TurfForm />
        </div>
      </div>
    </DashboardLayout>
  );
}
