import api from "@/services/api";

export const getWallet = () => api.get("/wallet");

export const addMoney = (amount: number) =>
  api.post("/wallet/add-money", { amount });

export const getCoupons = () => api.get("/coupons");

export const validateCoupon = (code: string, bookingAmount: number) =>
  api.post("/coupons/validate", { code, bookingAmount });
