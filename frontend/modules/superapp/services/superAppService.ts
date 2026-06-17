import api from "@/services/api";

export type SuperAppMap = Record<string, any>;

export type SportsIdentityPayload = {
  userId?: number;
  displayName: string;
  primaryEmail: string;
  phone?: string;
  city?: string;
  primarySport?: string;
  ageGroup?: string;
  guardianEmail?: string;
  profileVisibility?: string;
  linkedModules?: string;
};

export type WalletLedgerPayload = {
  amount: number;
  sourceModule?: string;
  referenceType?: string;
  referenceId?: string;
  note?: string;
  expiresAt?: string;
};

export type UnifiedMembershipPayload = {
  tier: string;
  billingCycle?: string;
  price?: number;
  startsAt?: string;
  expiresAt?: string;
  autoRenew?: boolean;
  benefitsSummary?: string;
};

export type RewardPayload = {
  points: number;
  sourceModule?: string;
  note?: string;
  expiresAt?: string;
};

export type ReputationPayload = {
  eventType: string;
  sourceModule?: string;
  impact: number;
  note?: string;
};

export type TimelinePayload = {
  eventType: string;
  sourceModule?: string;
  title: string;
  occurredAt?: string;
  visibility?: string;
  metadata?: string;
};

export type FamilyPayload = {
  parentIdentityId: number;
  childIdentityId: number;
  guardianEmail: string;
  relationshipType?: string;
  approvalRequired?: boolean;
};

export const getSuperAppDashboard = () => api.get<SuperAppMap>("/super-app/dashboard");
export const createSportsIdentity = (payload: SportsIdentityPayload) => api.post<SuperAppMap>("/super-app/identities", payload);
export const generateSportsPassport = (identityId: number, payload?: { visibility?: string; expiresAt?: string }) =>
  api.post<SuperAppMap>(`/super-app/identities/${identityId}/passport`, payload || {});
export const searchSuperAppIdentities = (query: string) => api.get<SuperAppMap[]>("/super-app/search", { params: { query } });
export const creditUniversalWallet = (identityId: number, payload: WalletLedgerPayload) => api.post<SuperAppMap>(`/super-app/identities/${identityId}/wallet/credit`, payload);
export const debitUniversalWallet = (identityId: number, payload: WalletLedgerPayload) => api.post<SuperAppMap>(`/super-app/identities/${identityId}/wallet/debit`, payload);
export const createUnifiedMembership = (identityId: number, payload: UnifiedMembershipPayload) => api.post<SuperAppMap>(`/super-app/identities/${identityId}/membership`, payload);
export const earnUniversalRewards = (identityId: number, payload: RewardPayload) => api.post<SuperAppMap>(`/super-app/identities/${identityId}/rewards/earn`, payload);
export const redeemUniversalRewards = (identityId: number, payload: RewardPayload) => api.post<SuperAppMap>(`/super-app/identities/${identityId}/rewards/redeem`, payload);
export const recordReputationEvent = (identityId: number, payload: ReputationPayload) => api.post<SuperAppMap>(`/super-app/identities/${identityId}/reputation`, payload);
export const addSportsTimelineEvent = (identityId: number, payload: TimelinePayload) => api.post<SuperAppMap>(`/super-app/identities/${identityId}/timeline`, payload);
export const createFamilyAccount = (payload: FamilyPayload) => api.post<SuperAppMap>("/super-app/family", payload);

export const getAdminSuperAppDashboard = () => api.get<SuperAppMap>("/admin/super-app/dashboard");
export const getAdminSuperAppIdentities = () => api.get<SuperAppMap[]>("/admin/super-app/identities");
export const getAdminSuperAppWallets = () => api.get<SuperAppMap[]>("/admin/super-app/wallets");
export const getAdminSuperAppWalletLedger = () => api.get<SuperAppMap[]>("/admin/super-app/wallet-ledger");
export const getAdminSuperAppMemberships = () => api.get<SuperAppMap[]>("/admin/super-app/memberships");
export const getAdminSuperAppRewards = () => api.get<SuperAppMap[]>("/admin/super-app/rewards");
export const getAdminSuperAppReputation = () => api.get<SuperAppMap[]>("/admin/super-app/reputation");
export const getAdminSuperAppTimeline = () => api.get<SuperAppMap[]>("/admin/super-app/timeline");
export const getAdminSuperAppFamily = () => api.get<SuperAppMap[]>("/admin/super-app/family");
export const getAdminSuperAppAudit = () => api.get<SuperAppMap[]>("/admin/super-app/audit");
