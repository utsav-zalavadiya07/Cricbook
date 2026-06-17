import api from "@/services/api";

export type AdminPlanPayload = {
  name?: string;
  type?: string;
  price?: number;
  durationDays?: number;
  discountPercent?: number;
  prioritySlots?: boolean;
  featuredListing?: boolean;
  description?: string;
};

export type SubscriptionPlanType = "USER_PREMIUM" | "OWNER_FEATURED";

// PLANS & SUBSCRIPTIONS
export const getPricingPage = (type?: SubscriptionPlanType) =>
  api.get("/subscription/pricing", { params: type ? { type } : undefined });
export const startSubscription = (planId: number, turfId?: number) =>
  api.post("/subscription/start", { planId, turfId });
export const getCurrentSubscription = (type?: SubscriptionPlanType) =>
  api.get("/subscription/current", { params: type ? { type } : undefined });
export const cancelSubscription = (type?: SubscriptionPlanType) =>
  api.post("/subscription/cancel", null, { params: type ? { type } : undefined });

// BILLING & INVOICES
export const getMyInvoices = (type?: SubscriptionPlanType) =>
  api.get("/billing/invoices", { params: type ? { type } : undefined });
export const downloadInvoice = (invoiceId: number) => api.get(`/billing/invoices/${invoiceId}/download`);
export const triggerRenewalTest = () => api.post("/billing/renew/test");

// LOYALTY & REWARDS
export const getLoyaltyBalance = () => api.get("/loyalty/balance");
export const getLoyaltyHistory = () => api.get("/loyalty/history");
export const redeemLoyaltyPoints = (points: number) => api.post("/loyalty/redeem", { points });
export const getGrowthDashboard = () => api.get("/growth/dashboard");

// REFERRALS
export const getReferralCode = () => api.get("/referral/code");
export const claimReferral = (code: string) => api.post("/referral/claim", { code });
export const getMyReferrals = () => api.get("/referral/list");

// ADMIN MONETIZATION PANEL
export const getAdminRevenue = () => api.get("/admin/revenue");
export const getAdminPlans = () => api.get("/admin/plans");
export const createAdminPlan = (plan: AdminPlanPayload) => api.post("/admin/plans", plan);
export const updateAdminPlan = (id: number, plan: AdminPlanPayload) => api.put(`/admin/plans/${id}`, plan);
export const deleteAdminPlan = (id: number) => api.delete(`/admin/plans/${id}`);
