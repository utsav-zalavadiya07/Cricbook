import api from "@/services/api";

export const getWallet = () => api.get("/wallet");

/**
 * Initiates a top-up payment gateway order (Razorpay).
 */
export const createWalletTopupOrder = (amount: number) =>
  api.post("/wallet/create-order", { amount });

/**
 * Verifies Razorpay payment signature on backend and credits wallet upon verification.
 */
export const verifyWalletTopupPayment = (payload: {
  orderId: string;
  paymentId: string;
  signature: string;
}) => api.post("/wallet/verify-payment", payload);

/**
 * Cancels a pending top-up order when user abandons or cancels payment.
 */
export const cancelWalletTopupOrder = (orderId: string) =>
  api.post("/wallet/cancel-order", { orderId });


/**
 * Legacy direct add-money (restricted to ADMIN on backend).
 */
export const addMoney = (amount: number) =>
  api.post("/wallet/add-money", { amount });

export const getCoupons = () => api.get("/coupons");

export const validateCoupon = (code: string, bookingAmount: number) =>
  api.post("/coupons/validate", { code, bookingAmount });
