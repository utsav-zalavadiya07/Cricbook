import api from "@/services/api";

export type SportsOsMap = Record<string, any>;

export type SportsOsFacilityPayload = {
  name: string;
  facilityType?: string;
  ownershipType?: string;
  jurisdictionLevel?: string;
  stateName: string;
  districtName: string;
  cityName?: string;
  latitude: number;
  longitude: number;
  capacity?: number;
  activeSlots?: number;
  publicBookingEnabled?: boolean;
  ownerDepartment?: string;
  images?: string;
};

export type CensusPayload = {
  cycleCode?: string;
  jurisdictionLevel?: string;
  stateName: string;
  districtName: string;
  submittedBy?: string;
  playerCount?: number;
  coachCount?: number;
  academyCount?: number;
  facilityCount?: number;
  schoolCount?: number;
  universityCount?: number;
};

export type GrantPayload = {
  applicantName: string;
  applicantType?: string;
  programType?: string;
  stateName: string;
  districtName: string;
  requestedAmount: number;
  supportingDocuments?: string;
};

export type GrantReviewPayload = {
  action: "APPROVE" | "REJECT";
  approvedAmount?: number;
  reviewNote?: string;
};

export type GrantDisbursementPayload = {
  stageName?: string;
  amount: number;
  note?: string;
  disbursedAt?: string;
};

export type VolunteerPayload = {
  fullName: string;
  email: string;
  phone?: string;
  stateName: string;
  districtName: string;
  category?: string;
  verificationStatus?: string;
};

export type AthletePayload = {
  sportsIdentityId?: number;
  athleteName: string;
  schoolOrInstitution?: string;
  sport?: string;
  ageGroup?: string;
  districtName: string;
  stateName: string;
  currentLevel?: string;
  selectionStatus?: string;
  selectionHistory?: string;
  progressScore?: number;
};

export type InspectionPayload = {
  facilityId: number;
  inspectorName?: string;
  inspectionType?: string;
  score?: number;
  safetyStatus?: string;
  maintenanceStatus?: string;
  recommendation?: string;
  status?: string;
  scheduledAt?: string;
  completedAt?: string;
};

export const getSportsOsDashboard = () => api.get<SportsOsMap>("/sports-os/dashboard");
export const searchSportsOsFacilities = (query?: string) => api.get<SportsOsMap[]>("/sports-os/facilities", { params: query ? { query } : undefined });
export const getSportsOsFacility = (facilityId: number) => api.get<SportsOsMap>(`/sports-os/facilities/${facilityId}`);
export const applyGovernmentGrant = (payload: GrantPayload) => api.post<SportsOsMap>("/sports-os/grants", payload);
export const registerSportsVolunteer = (payload: VolunteerPayload) => api.post<SportsOsMap>("/sports-os/volunteers", payload);

export const getAdminSportsOsDashboard = () => api.get<SportsOsMap>("/admin/sports-os/dashboard");
export const getAdminSportsOsFacilities = () => api.get<SportsOsMap[]>("/admin/sports-os/facilities");
export const getAdminSportsOsCensus = () => api.get<SportsOsMap[]>("/admin/sports-os/census");
export const getAdminSportsOsGrants = () => api.get<SportsOsMap[]>("/admin/sports-os/grants");
export const getAdminSportsOsGrantDisbursements = () => api.get<SportsOsMap[]>("/admin/sports-os/grant-disbursements");
export const getAdminSportsOsAthletes = () => api.get<SportsOsMap[]>("/admin/sports-os/athletes");
export const getAdminSportsOsVolunteers = () => api.get<SportsOsMap[]>("/admin/sports-os/volunteers");
export const getAdminSportsOsInspections = () => api.get<SportsOsMap[]>("/admin/sports-os/inspections");
export const getAdminSportsOsAudit = () => api.get<SportsOsMap[]>("/admin/sports-os/audit");
export const registerSportsOsFacility = (payload: SportsOsFacilityPayload) => api.post<SportsOsMap>("/sports-os/facilities", payload);
export const updateSportsOsFacilityStatus = (facilityId: number, payload: { status?: string; maintenanceStatus?: string; utilizationPercent?: number; publicBookingEnabled?: boolean }) =>
  api.post<SportsOsMap>(`/admin/sports-os/facilities/${facilityId}/status`, payload);
export const submitSportsCensus = (payload: CensusPayload) => api.post<SportsOsMap>("/sports-os/census", payload);
export const reviewGovernmentGrant = (grantId: number, payload: GrantReviewPayload) => api.post<SportsOsMap>(`/admin/sports-os/grants/${grantId}/review`, payload);
export const disburseGovernmentGrant = (grantId: number, payload: GrantDisbursementPayload) => api.post<SportsOsMap>(`/admin/sports-os/grants/${grantId}/disburse`, payload);
export const trackSportsOsAthlete = (payload: AthletePayload) => api.post<SportsOsMap>("/sports-os/athletes", payload);
export const scheduleFacilityInspection = (payload: InspectionPayload) => api.post<SportsOsMap>("/admin/sports-os/inspections", payload);
export const completeFacilityInspection = (inspectionId: number, payload?: Partial<InspectionPayload>) => api.post<SportsOsMap>(`/admin/sports-os/inspections/${inspectionId}/complete`, payload || {});
