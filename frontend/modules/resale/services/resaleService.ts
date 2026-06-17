import api from "@/services/api";

export type ResaleListingStatus = "ACTIVE" | "SOLD" | "EXPIRED" | "CANCELLED";

export type ResaleListing = {
  id: number;
  bookingId: number;
  sellerId: number;
  sellerName: string;
  buyerId?: number;
  buyerName?: string;
  turfId: number;
  turfName: string;
  city: string;
  location: string;
  slotId: number;
  bookingDate: string;
  startTime: string;
  endTime: string;
  price: number;
  originalAmount: number;
  commissionAmount: number;
  status: ResaleListingStatus;
  expiresAt: string;
  soldAt?: string;
  notes?: string;
  sellerTrustScore: number;
  buyerTrustScore?: number;
  recommendationScore: number;
  recommendationReason: string;
  createdAt?: string;
};

export type SlotWaitlist = {
  id: number;
  slotId: number;
  turfId: number;
  turfName: string;
  city: string;
  date: string;
  startTime: string;
  endTime: string;
  priority: number;
  active: boolean;
  createdAt?: string;
};

export type ResaleRule = {
  id?: number;
  ownerId: number;
  allowResale: boolean;
  autoApproveTransfer: boolean;
  restrictLowTrustUsers: boolean;
  maxMarkupPercent: number;
  minHoursBeforeStart: number;
  updatedAt?: string;
};

export type ResaleAnalytics = {
  activeListings: number;
  soldListings: number;
  expiredListings: number;
  waitlistUsers: number;
  marketplaceGmv: number;
  recoveredRevenue: number;
  successRate: number;
  recentListings: ResaleListing[];
};

export type CreateResalePayload = {
  bookingId: number;
  price: number;
  expiresAt?: string;
  notes?: string;
};

export type WaitlistPayload = {
  slotId: number;
  date: string;
  city?: string;
  priority?: number;
};

export const createResaleListing = (payload: CreateResalePayload) => api.post<ResaleListing>("/resale/create", payload);
export const getResaleListings = (city?: string) => api.get<ResaleListing[]>("/resale/list", { params: city ? { city } : undefined });
export const getRecommendedResaleListings = () => api.get<ResaleListing[]>("/resale/recommended");
export const getMyResaleListings = () => api.get<ResaleListing[]>("/resale/my");
export const getResaleListing = (listingId: number) => api.get<ResaleListing>(`/resale/${listingId}`);
export const buyResaleListing = (listingId: number, paymentReference?: string) =>
  api.post<ResaleListing>(`/resale/${listingId}/buy`, { paymentReference });
export const joinResaleWaitlist = (payload: WaitlistPayload) => api.post<SlotWaitlist>("/resale/waitlist", payload);
export const getMyResaleWaitlist = () => api.get<SlotWaitlist[]>("/resale/waitlist/my");
export const getOwnerResaleRules = () => api.get<ResaleRule>("/resale/owner/rules");
export const updateOwnerResaleRules = (payload: Partial<ResaleRule>) => api.put<ResaleRule>("/resale/owner/rules", payload);
export const getOwnerResaleAnalytics = () => api.get<ResaleAnalytics>("/resale/owner/analytics");
export const getAdminResaleAnalytics = () => api.get<ResaleAnalytics>("/resale/admin/analytics");
