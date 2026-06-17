import api from "@/services/api";

export const getMembershipPlans = () => api.get("/memberships/plans");

export const getMyMemberships = () => api.get("/memberships/my");

export const subscribeMembership = (planId: number, turfId?: number) =>
  api.post("/memberships/subscribe", { planId, turfId });
