"use client";

import { useEffect, useMemo, useState } from "react";
import {
  applyGovernmentGrant,
  getSportsOsDashboard,
  registerSportsVolunteer,
  searchSportsOsFacilities,
  SportsOsMap,
} from "@/modules/sportsos/services/sportsOsService";
import { CompactRow, DataTable, errorMessage, Field, formatNumber, MetricCard, Panel, SelectField } from "@/modules/sportsos/components/SportsOsUi";

const defaultGrant = {
  applicantName: "",
  applicantType: "DISTRICT_AUTHORITY",
  programType: "SPORTS_INFRASTRUCTURE",
  stateName: "Gujarat",
  districtName: "Ahmedabad",
  requestedAmount: "100000",
  supportingDocuments: "",
};

const defaultVolunteer = {
  fullName: "",
  email: "",
  phone: "",
  stateName: "Gujarat",
  districtName: "Ahmedabad",
  category: "EVENT",
};

export default function SportsOsPage() {
  const [dashboard, setDashboard] = useState<SportsOsMap | null>(null);
  const [facilities, setFacilities] = useState<SportsOsMap[]>([]);
  const [query, setQuery] = useState("");
  const [grantDraft, setGrantDraft] = useState(defaultGrant);
  const [volunteerDraft, setVolunteerDraft] = useState(defaultVolunteer);
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  async function loadSportsOs() {
    try {
      setLoading(true);
      const [dashboardRes, facilitiesRes] = await Promise.all([getSportsOsDashboard(), searchSportsOsFacilities(query.trim() || undefined)]);
      setDashboard(dashboardRes.data);
      setFacilities(facilitiesRes.data);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void loadSportsOs(), 0);
    return () => window.clearTimeout(timer);
  }, []);

  const metrics = useMemo(
    () => [
      { label: "Facilities", value: formatNumber(dashboard?.kpis?.facilities), detail: `${formatNumber(dashboard?.kpis?.activeFacilities)} active registry records` },
      { label: "Public Booking", value: formatNumber(dashboard?.kpis?.publicBookableFacilities), detail: "government facilities ready for citizens" },
      { label: "Census Players", value: formatNumber(dashboard?.kpis?.censusPlayers), detail: `${formatNumber(dashboard?.kpis?.censusCoaches)} coaches recorded` },
      { label: "Grant Funding", value: `Rs ${formatNumber(dashboard?.kpis?.approvedGrantValue)}`, detail: `Rs ${formatNumber(dashboard?.kpis?.disbursedGrantValue)} disbursed` },
      { label: "Volunteers", value: formatNumber(dashboard?.kpis?.volunteers), detail: `${formatNumber(dashboard?.kpis?.verifiedVolunteers)} verified` },
      { label: "Athletes", value: formatNumber(dashboard?.kpis?.athleteRecords), detail: `${formatNumber(dashboard?.kpis?.nationalAthletes)} national tracked` },
    ],
    [dashboard]
  );

  async function runAction(action: () => Promise<unknown>, message: string) {
    setSaving(true);
    setStatus("");
    try {
      await action();
      setStatus(message);
      await loadSportsOs();
    } catch (error) {
      setStatus(errorMessage(error, "Sports OS action failed."));
    } finally {
      setSaving(false);
    }
  }

  async function handleSearch() {
    setLoading(true);
    try {
      const response = await searchSportsOsFacilities(query.trim() || undefined);
      setFacilities(response.data);
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="theme-page-player min-h-screen px-4 py-8 sm:px-6" data-i18n-skip="true">
      <div className="mx-auto max-w-7xl space-y-6">
        <section className="theme-card rounded-[2rem] border p-6 shadow-2xl">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.26em] text-emerald-300">EPIC 34 Sports OS</p>
              <h1 className="mt-3 text-3xl font-black theme-title sm:text-4xl">Public sports infrastructure, grants, census and impact tracking</h1>
              <p className="mt-3 max-w-3xl text-sm leading-6 theme-soft">
                Discover public facilities, track civic sports infrastructure, register volunteers, and apply for government sports development grants through one Sports OS layer.
              </p>
            </div>
            <button onClick={() => void loadSportsOs()} className="rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-black text-white hover:bg-emerald-400">
              Refresh Sports OS
            </button>
          </div>
        </section>

        {status ? <p className="theme-card rounded-2xl border border-emerald-400/20 px-4 py-3 text-sm font-bold text-emerald-200">{status}</p> : null}

        {loading ? (
          <section className="theme-card rounded-[2rem] border p-10 text-center text-sm font-bold theme-soft">Loading national sports infrastructure...</section>
        ) : (
          <>
            <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {metrics.map((metric) => (
                <MetricCard key={metric.label} {...metric} />
              ))}
            </section>

            <section className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
              <Panel title="Public Facility Discovery" subtitle="Search government, municipal, school, university and PPP sports facilities.">
                <div className="mb-4 grid gap-3 sm:grid-cols-[1fr_auto]">
                  <Field label="Search facility" value={query} onChange={setQuery} placeholder="Facility, city, district or sport" />
                  <button onClick={() => void handleSearch()} className="mt-6 rounded-2xl bg-emerald-500 px-5 py-3 text-sm font-black text-white hover:bg-emerald-400">
                    Search
                  </button>
                </div>
                <DataTable
                  rows={facilities}
                  columns={[
                    ["facilityCode", "Code"],
                    ["name", "Facility"],
                    ["facilityType", "Type"],
                    ["districtName", "District"],
                    ["publicBookingEnabled", "Public"],
                    ["maintenanceStatus", "Maintenance"],
                  ]}
                  empty="No public sports facilities found."
                />
              </Panel>

              <Panel title="Public Accountability Controls" subtitle="Core controls used by the Sports OS foundation.">
                <div className="space-y-3">
                  {(dashboard?.priorityControls || []).map((item: SportsOsMap) => (
                    <CompactRow key={String(item.name)} title={String(item.name)} status={String(item.status)} meta={String(item.detail)} />
                  ))}
                </div>
              </Panel>
            </section>

            <section className="grid gap-6 xl:grid-cols-2">
              <Panel title="Apply for Development Grant" subtitle="Submit a district, school, university or sports infrastructure funding request.">
                <div className="grid gap-3 sm:grid-cols-2">
                  <Field label="Applicant" value={grantDraft.applicantName} onChange={(value) => setGrantDraft((current) => ({ ...current, applicantName: value }))} placeholder="District Sports Office" />
                  <SelectField label="Applicant type" value={grantDraft.applicantType} onChange={(value) => setGrantDraft((current) => ({ ...current, applicantType: value }))} options={["DISTRICT_AUTHORITY", "SCHOOL", "UNIVERSITY", "MUNICIPAL_BODY"]} />
                  <SelectField label="Program type" value={grantDraft.programType} onChange={(value) => setGrantDraft((current) => ({ ...current, programType: value }))} options={["SPORTS_INFRASTRUCTURE", "YOUTH_SPORTS", "GIRLS_SPORTS", "RURAL_SPORTS"]} />
                  <Field label="Requested amount" type="number" value={grantDraft.requestedAmount} onChange={(value) => setGrantDraft((current) => ({ ...current, requestedAmount: value }))} />
                  <Field label="State" value={grantDraft.stateName} onChange={(value) => setGrantDraft((current) => ({ ...current, stateName: value }))} />
                  <Field label="District" value={grantDraft.districtName} onChange={(value) => setGrantDraft((current) => ({ ...current, districtName: value }))} />
                </div>
                <button
                  disabled={saving}
                  onClick={() =>
                    void runAction(
                      () => applyGovernmentGrant({ ...grantDraft, requestedAmount: Number(grantDraft.requestedAmount || 0) }),
                      "Grant application submitted for government review."
                    )
                  }
                  className="mt-4 w-full rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-black text-white hover:bg-emerald-400 disabled:opacity-60"
                >
                  Submit Grant Application
                </button>
              </Panel>

              <Panel title="Register Volunteer" subtitle="Create a verified volunteer pipeline for public sports programs and events.">
                <div className="grid gap-3 sm:grid-cols-2">
                  <Field label="Full name" value={volunteerDraft.fullName} onChange={(value) => setVolunteerDraft((current) => ({ ...current, fullName: value }))} placeholder="Volunteer name" />
                  <Field label="Email" value={volunteerDraft.email} onChange={(value) => setVolunteerDraft((current) => ({ ...current, email: value }))} placeholder="volunteer@example.com" />
                  <Field label="Phone" value={volunteerDraft.phone} onChange={(value) => setVolunteerDraft((current) => ({ ...current, phone: value }))} />
                  <SelectField label="Category" value={volunteerDraft.category} onChange={(value) => setVolunteerDraft((current) => ({ ...current, category: value }))} options={["EVENT", "FACILITY", "DISTRICT_ACTIVITY", "SCHOOL_SPORTS"]} />
                  <Field label="State" value={volunteerDraft.stateName} onChange={(value) => setVolunteerDraft((current) => ({ ...current, stateName: value }))} />
                  <Field label="District" value={volunteerDraft.districtName} onChange={(value) => setVolunteerDraft((current) => ({ ...current, districtName: value }))} />
                </div>
                <button
                  disabled={saving}
                  onClick={() => void runAction(() => registerSportsVolunteer(volunteerDraft), "Volunteer registration submitted.")}
                  className="mt-4 w-full rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-black text-white hover:bg-emerald-400 disabled:opacity-60"
                >
                  Register Volunteer
                </button>
              </Panel>
            </section>

            <section className="grid gap-6 lg:grid-cols-3">
              <Panel title="State Rollups" subtitle="Facilities, census participation and funding by state.">
                <DataTable rows={dashboard?.stateRollups || []} columns={[["stateName", "State"], ["facilities", "Facilities"], ["players", "Players"], ["approvedFunding", "Approved"]]} empty="No state rollups yet." />
              </Panel>
              <Panel title="Recent Grants" subtitle="Grant lifecycle from application to disbursement.">
                <DataTable rows={dashboard?.grants || []} columns={[["grantCode", "Grant"], ["applicantName", "Applicant"], ["requestedAmount", "Requested"], ["status", "Status"]]} empty="No grants yet." />
              </Panel>
              <Panel title="Athlete Pipeline" subtitle="School to district, state and national athlete tracking.">
                <DataTable rows={dashboard?.athletes || []} columns={[["athleteName", "Athlete"], ["sport", "Sport"], ["currentLevel", "Level"], ["progressScore", "Score"]]} empty="No athletes tracked yet." />
              </Panel>
            </section>
          </>
        )}
      </div>
    </main>
  );
}
