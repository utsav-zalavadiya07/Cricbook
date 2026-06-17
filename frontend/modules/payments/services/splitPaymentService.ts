import api from "@/services/api";

export type PaymentSplitStatus = "PENDING" | "PAID" | "OVERDUE" | "FAILED" | "REFUNDED" | string;

export type PaymentSplit = {
  id: number;
  bookingId: number;
  turfName?: string;
  bookingDate?: string;
  startTime?: string;
  endTime?: string;
  userId: number;
  playerName: string;
  amount: number;
  status: PaymentSplitStatus;
  dueDate?: string;
  paidAt?: string;
  paymentLink?: string;
  paymentReference?: string;
};

export type TeamCollection = {
  id: number;
  bookingId: number;
  captainId: number;
  captainName: string;
  totalAmount: number;
  collectedAmount: number;
  pendingAmount: number;
  paidPlayers: number;
  pendingPlayers: number;
  collectionRate: number;
  dueDate?: string;
  status: string;
};

export type SplitAnalytics = {
  totalSplits: number;
  paidSplits: number;
  pendingSplits: number;
  totalCollected: number;
  totalPending: number;
  collectionSuccessRate: number;
  insight: string;
};

export type SplitPaymentResponse = {
  bookingId: number;
  collection?: TeamCollection | null;
  splits: PaymentSplit[];
  analytics: SplitAnalytics;
};

export type PaymentRule = {
  id?: number;
  ownerId: number;
  minimumPercentage: number;
  autoCancel: boolean;
  fullPaymentRequired: boolean;
  reminderHoursBefore: number;
};

export const createSplitPayment = (payload: {
  bookingId: number;
  splitType?: "EQUAL" | "CUSTOM" | string;
  dueDate?: string;
  shares: Array<{ userId?: number; loginId?: string; amount?: number }>;
}) => api.post<SplitPaymentResponse>("/payment/split", payload);

export const getBookingSplit = (bookingId: number) => api.get<SplitPaymentResponse>(`/payment/split/${bookingId}`);
export const getMySplits = () => api.get<PaymentSplit[]>("/payment/splits/my");
export const payShare = (payload: { splitId: number; paymentReference?: string; useWallet?: boolean }) =>
  api.post<SplitPaymentResponse>("/payment/pay-share", payload);
export const remindSplit = (bookingId: number) => api.post<SplitPaymentResponse>(`/payment/split/${bookingId}/remind`);
export const getPaymentAnalytics = () => api.get<SplitAnalytics>("/payment/analytics");
export const getAdminPaymentMonitoring = () => api.get<SplitAnalytics>("/admin/payment-monitoring");
export const getOwnerPaymentRules = () => api.get<PaymentRule>("/owner/payment-rules");
export const updateOwnerPaymentRules = (payload: Partial<PaymentRule>) => api.put<PaymentRule>("/owner/payment-rules", payload);
