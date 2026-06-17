import api from "@/services/api";

export type CareerMap = Record<string, string | number | boolean | null | undefined | string[] | Record<string, unknown> | Array<Record<string, unknown>>>;

export type CareerProfilePayload = {
  playerName: string;
  playerEmail: string;
  phone?: string;
  sport?: string;
  specialization?: string;
  playerRole?: string;
  city?: string;
  ageGroup?: string;
  profilePhotoUrl?: string;
  bio?: string;
  statisticsSummary?: string;
  certificatesSummary?: string;
  scoutOptIn?: boolean;
  visibility?: string;
};

export type CareerAchievementPayload = {
  title: string;
  achievementType?: string;
  issuer?: string;
  eventDate?: string;
  evidenceUrl?: string;
  notes?: string;
};

export const getCareerMarketplace = (params?: { city?: string; sport?: string; ageGroup?: string; minScore?: number }) =>
  api.get<CareerMap[]>("/career/marketplace", { params });

export const getCareerOpportunities = (params?: { type?: string; sport?: string; location?: string }) =>
  api.get<CareerMap[]>("/career/opportunities", { params });

export const createCareerProfile = (payload: CareerProfilePayload) =>
  api.post<CareerMap>("/career/profiles", payload);

export const updateCareerProfile = (profileId: number, payload: CareerProfilePayload) =>
  api.put<CareerMap>(`/career/profiles/${profileId}`, payload);

export const generateCareerResume = (profileId: number) =>
  api.post<CareerMap>(`/career/profiles/${profileId}/resume`);

export const addCareerAchievement = (profileId: number, payload: CareerAchievementPayload) =>
  api.post<CareerMap>(`/career/profiles/${profileId}/achievements`, payload);

export const applyCareerOpportunity = (opportunityId: number, payload: { profileId: number; statement?: string; documentsUrl?: string }) =>
  api.post<CareerMap>(`/career/opportunities/${opportunityId}/applications`, payload);

export const registerScout = (payload: {
  scoutName: string;
  email: string;
  phone?: string;
  organizationName: string;
  clubType?: string;
  sportSpecialization?: string;
  region?: string;
  credentialsUrl?: string;
}) => api.post<CareerMap>("/career/scouts/register", payload);
