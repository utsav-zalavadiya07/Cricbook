"use client";

import { useEffect, useMemo, useState } from "react";
import {
  completeFacilityInspection,
  disburseGovernmentGrant,
  getAdminSportsOsAthletes,
  getAdminSportsOsAudit,
  getAdminSportsOsCensus,
  getAdminSportsOsDashboard,
  getAdminSportsOsFacilities,
  getAdminSportsOsGrants,
  getAdminSportsOsInspections,
  getAdminSportsOsVolunteers,
  registerSportsOsFacility,
  reviewGovernmentGrant,
  scheduleFacilityInspection,
  SportsOsMap,
  submitSportsCensus,
  trackSportsOsAthlete,
  updateSportsOsFacilityStatus,
} from "@/modules/sportsos/services/sportsOsService";
import { CompactRow, DataTable, errorMessage, Field, formatNumber, MetricCard, Panel, SelectField } from "@/modules/sportsos/components/SportsOsUi";

const defaultFacility = {
  name: "",
  facilityType: "SPORTS_COMPLEX",
  ownershipType: "GOVERNMENT",
  jurisdictionLevel: "DISTRICT",
  stateName: "Gujarat",
  districtName: "Ahmedabad",
  cityName: "Ahmedabad",
  latitude: "23.0225",
  longitude: "72.5714",
  capacity: "500",
  activeSlots: "12",
};

const defaultCensus = {
  cycleCode: "CENSUS-2026",
  jurisdictionLevel: "DISTRICT",
  stateName: "Gujarat",
  districtName: "Ahmedabad",
  submittedBy: "District Sports Office",
  playerCount: "1000",
  coachCount: "100",
  academyCount: "10",
  facilityCount: "25",
  schoolCount: "50",
  universityCount: "5",
};

const defaultAthlete = {
  athleteName: "",
  schoolOrInstitution: "",
  sport: "CRICKET",
  ageGroup: "U16",
  districtName: "Ahmedabad",
  stateName: "Gujarat",
  currentLevel: "DISTRICT",
  selectionStatus: "TRACKING",
  selectionHistory: "",
  progressScore: "70",
};

export default function AdminSportsOsPage() {
  const [dashboard, setDashboard] = useState<SportsOsMap | null>(null);
  const [facilities, setFacilities] = useState<SportsOsMap[]>([]);
  const [census, setCensus] = useState<SportsOsMap[]>([]);
  const [grants, setGrants] = useState<SportsOsMap[]>([]);
  const [athletes, setAthletes] = useState<SportsOsMap[]>([]);
  const [volunteers, setVolunteers] = useState<SportsOsMap[]>([]);
  const [inspections, setInspections] = useState<SportsOsMap[]>([]);
  const [audit, setAudit] = useState<SportsOsMap[]>([]);
  const [facilityDraft, setFacilityDraft] = useState(defaultFacility);
  const [censusDraft, setCensusDraft] = useState(defaultCensus);
  const [athleteDraft, setAthleteDraft] = useState(defaultAthlete);
  const [selectedGrantId, setSelectedGrantId] = useState("");
  const [selectedFacilityId, setSelectedFacilityId] = useState("");
  const [selectedInspectionId, setSelectedInspectionId] = useState("");
  const [approvedAmount, setApprovedAmount] = useState("50000");
  const [disbursementAmount, setDisbursementAmount] = useState("25000");
  const [inspectionScore, setInspectionScore] = useState("80");
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  async function loadSportsOs() {
    try {
      setLoading(true);
      const [dashboardRes, facilitiesRes, censusRes, grantsRes, athletesRes, volunteersRes, inspectionsRes, auditRes] = await Promise.all([
        getAdminSportsOsDashboard(),
        getAdminSportsOsFacilities(),
        getAdminSportsOsCensus(),
        getAdminSportsOsGrants(),
        getAdminSportsOsAthletes(),
        getAdminSportsOsVolunteers(),
        getAdminSportsOsInspections(),
        getAdminSportsOsAudit(),
      ]);
      setDashboard(dashboardRes.data);
      setFacilities(facilitiesRes.data);
      setCensus(censusRes.data);
      setGrants(grantsRes.data);
      setAthletes(athletesRes.data);
      setVolunteers(volunteersRes.data);
      setInspections(inspectionsRes.data);
      setAudit(auditRes.data);
      const firstGrant = grantsRes.data.find((grant) => String(grant.status) === "SUBMITTED") || grantsRes.data[0];
      const firstFacility = facilitiesRes.data[0];
      const firstInspection = inspectionsRes.data.find((inspection) => String(inspection.status) !== "COMPLETED") || inspectionsRes.data[0];
      if (firstGrant?.id) setSelectedGrantId(String(firstGrant.id));
      if (firstFacility?.id) setSelectedFacilityId(String(firstFacility.id));
      if (firstInspection?.id) setSelectedInspectionId(String(firstInspection.id));
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
      { label: "Facilities", value: formatNumber(dashboard?.kpis?.facilities), detail: `${formatNumber(dashboard?.kpis?.activeFacilities)} active` },
      { label: "Public Ready", value: formatNumber(dashboard?.kpis?.publicBookableFacilities), detail: "citizen booking enabled" },
      { label: "Census Players", value: formatNumber(dashboard?.kpis?.censusPlayers), detail: "submitted player counts" },
      { label: "Pending Grants", value: formatNumber(dashboard?.kpis?.pendingGrants), detail: `Rs ${formatNumber(dashboard?.kpis?.approvedGrantValue)} approved` },
      { label: "Athlete Records", value: formatNumber(dashboard?.kpis?.athleteRecords), detail: `${formatNumber(dashboard?.kpis?.nationalAthletes)} national` },
      { label: "Open Inspections", value: formatNumber(dashboard?.kpis?.openInspections), detail: "facility safety workflow" },
      { label: "Volunteers", value: formatNumber(dashboard?.kpis?.volunteers), detail: `${formatNumber(dashboard?.kpis?.verifiedVolunteers)} verified` },
      { label: "Audit Rows", value: formatNumber(audit.length), detail: "governance events" },
    ],
    [dashboard, audit.length]
  );

  async function runAction(action: () => Promise<unknown>, message: string) {
    setSaving(true);
    setStatus("");
    try {
      await action();
      setStatus(message);
      await loadSportsOs();
    } catch (error) {
      setStatus(errorMessage(error, "Sports OS admin action failed."));
    } finally {
      setSaving(false);
    }
  }

  const grantId = Number(selectedGrantId);
  const facilityId = Number(selectedFacilityId);
  const inspectionId = Number(selectedInspectionId);

  return (
    <main className="min-h-screen bg-slate-950 px-4 py-8 text-white sm:px-8" data-i18n-skip="true">
      <div className="mx-auto max-w-7xl space-y-8">
        <header className="rounded-[2rem] border border-white/10 bg-gradient-to-br from-emerald-500/15 via-slate-900 to-cyan-500/10 p-6 shadow-2xl">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.26em] text-emerald-300">EPIC 34 Sports OS Governance</p>
              <h1 className="mt-3 text-3xl font-black text-white sm:text-4xl">Smart City, Sports Census, Grants and Public Facility Control Center</h1>
              <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-300">
                Govern public sports infrastructure, district/state rollups, census submissions, funding workflows, inspections, volunteers and athlete development tracking.
              </p>
            </div>
            <button onClick={() => void loadSportsOs()} className="rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-emerald-300">
              Refresh Sports OS
            </button>
          </div>
        </header>

        {status ? <p className="rounded-2xl border border-emerald-400/20 bg-emerald-400/10 px-4 py-3 text-sm font-bold text-emerald-200">{status}</p> : null}

        {loading ? (
          <section className="rounded-[2rem] border border-white/10 bg-white/[0.04] p-10 text-center text-sm font-bold text-slate-300">Loading Sports OS governance...</section>
        ) : (
          <>
            <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              {metrics.map((metric) => (
                <MetricCard key={metric.label} {...metric} />
              ))}
            </section>

            <section className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
              <Panel title="Register Public Sports Facility" subtitle="Creates the infrastructure registry record with ownership, coordinates, utilization and booking readiness.">
                <div className="grid gap-3 sm:grid-cols-2">
                  <Field label="Facility name" value={facilityDraft.name} onChange={(value) => setFacilityDraft((current) => ({ ...current, name: value }))} placeholder="District Sports Complex" />
                  <SelectField label="Facility type" value={facilityDraft.facilityType} onChange={(value) => setFacilityDraft((current) => ({ ...current, facilityType: value }))} options={["SPORTS_COMPLEX", "CRICKET_GROUND", "FOOTBALL_GROUND", "INDOOR_STADIUM", "SWIMMING_POOL"]} />
                  <SelectField label="Ownership" value={facilityDraft.ownershipType} onChange={(value) => setFacilityDraft((current) => ({ ...current, ownershipType: value }))} options={["GOVERNMENT", "MUNICIPAL", "SCHOOL", "UNIVERSITY", "PPP"]} />
                  <SelectField label="Jurisdiction" value={facilityDraft.jurisdictionLevel} onChange={(value) => setFacilityDraft((current) => ({ ...current, jurisdictionLevel: value }))} options={["MUNICIPAL", "CITY", "DISTRICT", "STATE", "NATIONAL", "SCHOOL", "UNIVERSITY"]} />
                  <Field label="State" value={facilityDraft.stateName} onChange={(value) => setFacilityDraft((current) => ({ ...current, stateName: value }))} />
                  <Field label="District" value={facilityDraft.districtName} onChange={(value) => setFacilityDraft((current) => ({ ...current, districtName: value }))} />
                  <Field label="Latitude" type="number" value={facilityDraft.latitude} onChange={(value) => setFacilityDraft((current) => ({ ...current, latitude: value }))} />
                  <Field label="Longitude" type="number" value={facilityDraft.longitude} onChange={(value) => setFacilityDraft((current) => ({ ...current, longitude: value }))} />
                  <Field label="Capacity" type="number" value={facilityDraft.capacity} onChange={(value) => setFacilityDraft((current) => ({ ...current, capacity: value }))} />
                  <Field label="Active slots" type="number" value={facilityDraft.activeSlots} onChange={(value) => setFacilityDraft((current) => ({ ...current, activeSlots: value }))} />
                </div>
                <button
                  disabled={saving}
                  onClick={() =>
                    void runAction(
                      () =>
                        registerSportsOsFacility({
                          ...facilityDraft,
                          latitude: Number(facilityDraft.latitude || 0),
                          longitude: Number(facilityDraft.longitude || 0),
                          capacity: Number(facilityDraft.capacity || 0),
                          activeSlots: Number(facilityDraft.activeSlots || 0),
                          publicBookingEnabled: true,
                        }),
                      "Facility registered in Sports OS registry."
                    )
                  }
                  className="mt-4 w-full rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-emerald-300 disabled:opacity-60"
                >
                  Register Facility
                </button>
              </Panel>

              <Panel title="Submit Sports Census" subtitle="Captures district-level participation, coaching, academy, school, university and facility counts.">
                <div className="grid gap-3 sm:grid-cols-2">
                  <Field label="Cycle" value={censusDraft.cycleCode} onChange={(value) => setCensusDraft((current) => ({ ...current, cycleCode: value }))} />
                  <Field label="Submitted by" value={censusDraft.submittedBy} onChange={(value) => setCensusDraft((current) => ({ ...current, submittedBy: value }))} />
                  <Field label="State" value={censusDraft.stateName} onChange={(value) => setCensusDraft((current) => ({ ...current, stateName: value }))} />
                  <Field label="District" value={censusDraft.districtName} onChange={(value) => setCensusDraft((current) => ({ ...current, districtName: value }))} />
                  <Field label="Players" type="number" value={censusDraft.playerCount} onChange={(value) => setCensusDraft((current) => ({ ...current, playerCount: value }))} />
                  <Field label="Coaches" type="number" value={censusDraft.coachCount} onChange={(value) => setCensusDraft((current) => ({ ...current, coachCount: value }))} />
                  <Field label="Facilities" type="number" value={censusDraft.facilityCount} onChange={(value) => setCensusDraft((current) => ({ ...current, facilityCount: value }))} />
                  <Field label="Schools" type="number" value={censusDraft.schoolCount} onChange={(value) => setCensusDraft((current) => ({ ...current, schoolCount: value }))} />
                </div>
                <button
                  disabled={saving}
                  onClick={() =>
                    void runAction(
                      () =>
                        submitSportsCensus({
                          ...censusDraft,
                          playerCount: Number(censusDraft.playerCount || 0),
                          coachCount: Number(censusDraft.coachCount || 0),
                          academyCount: Number(censusDraft.academyCount || 0),
                          facilityCount: Number(censusDraft.facilityCount || 0),
                          schoolCount: Number(censusDraft.schoolCount || 0),
                          universityCount: Number(censusDraft.universityCount || 0),
                        }),
                      "Sports census submitted."
                    )
                  }
                  className="mt-4 w-full rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-emerald-300 disabled:opacity-60"
                >
                  Submit Census
                </button>
              </Panel>
            </section>

            <section className="grid gap-6 xl:grid-cols-3">
              <Panel title="Grant Review & Disbursement" subtitle="Approval cannot exceed requested amount; disbursement cannot exceed approved balance.">
                <SelectField label="Grant" value={selectedGrantId} onChange={setSelectedGrantId} options={grants.map((grant) => String(grant.id))} />
                <div className="mt-3 grid gap-3 sm:grid-cols-2">
                  <Field label="Approved amount" type="number" value={approvedAmount} onChange={setApprovedAmount} />
                  <Field label="Disbursement" type="number" value={disbursementAmount} onChange={setDisbursementAmount} />
                </div>
                <div className="mt-4 grid gap-3 sm:grid-cols-3">
                  <button disabled={!grantId || saving} onClick={() => void runAction(() => reviewGovernmentGrant(grantId, { action: "APPROVE", approvedAmount: Number(approvedAmount || 0), reviewNote: "Approved by Sports OS admin" }), "Grant approved.")} className="rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 disabled:opacity-50">
                    Approve
                  </button>
                  <button disabled={!grantId || saving} onClick={() => void runAction(() => reviewGovernmentGrant(grantId, { action: "REJECT", reviewNote: "Rejected by Sports OS admin" }), "Grant rejected.")} className="rounded-2xl border border-white/10 bg-white/[0.06] px-4 py-3 text-sm font-black text-white disabled:opacity-50">
                    Reject
                  </button>
                  <button disabled={!grantId || saving} onClick={() => void runAction(() => disburseGovernmentGrant(grantId, { stageName: "STAGE_1", amount: Number(disbursementAmount || 0), note: "Sports OS disbursement" }), "Grant disbursed.")} className="rounded-2xl border border-emerald-400/30 bg-emerald-400/10 px-4 py-3 text-sm font-black text-emerald-100 disabled:opacity-50">
                    Disburse
                  </button>
                </div>
              </Panel>

              <Panel title="Facility Status & Inspection" subtitle="Maintenance and inspection workflow for public facility governance.">
                <SelectField label="Facility ID" value={selectedFacilityId} onChange={setSelectedFacilityId} options={facilities.map((facility) => String(facility.id))} />
                <SelectField label="Inspection ID" value={selectedInspectionId} onChange={setSelectedInspectionId} options={inspections.map((inspection) => String(inspection.id))} />
                <Field label="Inspection score" type="number" value={inspectionScore} onChange={setInspectionScore} />
                <div className="mt-4 grid gap-3">
                  <button disabled={!facilityId || saving} onClick={() => void runAction(() => updateSportsOsFacilityStatus(facilityId, { status: "ACTIVE", maintenanceStatus: "OPERATIONAL", utilizationPercent: 70, publicBookingEnabled: true }), "Facility status updated.")} className="rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 disabled:opacity-50">
                    Mark Operational
                  </button>
                  <button disabled={!facilityId || saving} onClick={() => void runAction(() => scheduleFacilityInspection({ facilityId, inspectorName: "Sports OS Inspector", inspectionType: "ROUTINE", status: "SCHEDULED" }), "Inspection scheduled.")} className="rounded-2xl border border-white/10 bg-white/[0.06] px-4 py-3 text-sm font-black text-white disabled:opacity-50">
                    Schedule Inspection
                  </button>
                  <button disabled={!inspectionId || saving} onClick={() => void runAction(() => completeFacilityInspection(inspectionId, { score: Number(inspectionScore || 0), safetyStatus: "CLEAR", maintenanceStatus: "OPERATIONAL", recommendation: "Cleared for public operations" }), "Inspection completed.")} className="rounded-2xl border border-emerald-400/30 bg-emerald-400/10 px-4 py-3 text-sm font-black text-emerald-100 disabled:opacity-50">
                    Complete Inspection
                  </button>
                </div>
              </Panel>

              <Panel title="Athlete Progression" subtitle="Track school to district, state and national athlete development.">
                <div className="grid gap-3">
                  <Field label="Athlete name" value={athleteDraft.athleteName} onChange={(value) => setAthleteDraft((current) => ({ ...current, athleteName: value }))} placeholder="Athlete name" />
                  <Field label="Institution" value={athleteDraft.schoolOrInstitution} onChange={(value) => setAthleteDraft((current) => ({ ...current, schoolOrInstitution: value }))} />
                  <SelectField label="Level" value={athleteDraft.currentLevel} onChange={(value) => setAthleteDraft((current) => ({ ...current, currentLevel: value }))} options={["SCHOOL", "DISTRICT", "STATE", "NATIONAL"]} />
                  <Field label="Progress score" type="number" value={athleteDraft.progressScore} onChange={(value) => setAthleteDraft((current) => ({ ...current, progressScore: value }))} />
                </div>
                <button
                  disabled={saving}
                  onClick={() => void runAction(() => trackSportsOsAthlete({ ...athleteDraft, progressScore: Number(athleteDraft.progressScore || 0) }), "Athlete development record tracked.")}
                  className="mt-4 w-full rounded-2xl bg-emerald-400 px-4 py-3 text-sm font-black text-slate-950 hover:bg-emerald-300 disabled:opacity-60"
                >
                  Track Athlete
                </button>
              </Panel>
            </section>

            <section className="grid gap-6 xl:grid-cols-2">
              <Panel title="Facility Registry" subtitle="Government, municipal, school, university and PPP sports infrastructure.">
                <DataTable rows={facilities} columns={[["facilityCode", "Code"], ["name", "Facility"], ["ownershipType", "Owner"], ["status", "Status"], ["maintenanceStatus", "Maintenance"]]} empty="No facilities yet." />
              </Panel>
              <Panel title="Grant Queue" subtitle="Funding approval, rejection, partial disbursement and final disbursement status.">
                <DataTable rows={grants} columns={[["grantCode", "Code"], ["applicantName", "Applicant"], ["requestedAmount", "Requested"], ["approvedAmount", "Approved"], ["status", "Status"]]} empty="No grants yet." />
              </Panel>
            </section>

            <section className="grid gap-6 xl:grid-cols-3">
              <Panel title="Census Submissions" subtitle="Latest district, state and national sports counts.">
                <DataTable rows={census} columns={[["cycleCode", "Cycle"], ["districtName", "District"], ["playerCount", "Players"], ["facilityCount", "Facilities"]]} empty="No census submissions yet." />
              </Panel>
              <Panel title="Volunteers" subtitle="Volunteer registration and verification pipeline.">
                <DataTable rows={volunteers} columns={[["volunteerCode", "Code"], ["fullName", "Name"], ["category", "Category"], ["verificationStatus", "Status"]]} empty="No volunteers yet." />
              </Panel>
              <Panel title="Audit & Controls" subtitle="Immutable Sports OS governance and public accountability events.">
                <div className="mb-4 space-y-3">
                  {(dashboard?.governanceControls || []).map((item: SportsOsMap) => (
                    <CompactRow key={String(item.name)} title={String(item.name)} status={String(item.status)} meta={String(item.detail)} />
                  ))}
                </div>
                <DataTable rows={audit} columns={[["action", "Action"], ["entityType", "Entity"], ["actor", "Actor"]]} empty="No audit rows yet." />
              </Panel>
            </section>
          </>
        )}
      </div>
    </main>
  );
}
