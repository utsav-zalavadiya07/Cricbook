import api from "@/services/api";
import type { Coupon, Wallet, WalletTopupOrder } from "@/modules/wallet/types";

export const getWallet = () => api.get<Wallet>("/wallet");

/**
 * Initiates a top-up payment gateway order (Razorpay).
 */
export const createWalletTopupOrder = (amount: number) =>
  api.post<WalletTopupOrder>("/wallet/create-order", { amount });

/**
 * Verifies Razorpay payment signature on backend and credits wallet upon verification.
 */
export const verifyWalletTopupPayment = (payload: {
  orderId: string;
  paymentId: string;
  signature: string;
}) => api.post<Wallet>("/wallet/verify-payment", payload);

/**
 * Cancels a pending top-up order when user abandons or cancels payment.
 */
export const cancelWalletTopupOrder = (orderId: string) =>
  api.post<Wallet>("/wallet/cancel-order", { orderId });


/**
 * Legacy direct add-money (restricted to ADMIN on backend).
 */
export const addMoney = (amount: number) =>
  api.post("/wallet/add-money", { amount });

export const getCoupons = () => api.get<Coupon[]>("/coupons");

export const validateCoupon = (code: string, bookingAmount: number) =>
  api.post<Coupon>("/coupons/validate", { code, bookingAmount });
