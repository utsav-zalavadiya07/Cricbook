export type Subscription = {
  id: number;
  planId: number;
  planName: string;
  type: string;
  price: number;
  durationDays: number;
  status: string;
  startDate: string;
  endDate: string;
  autoRenew: boolean;
  turfId?: number;
  turfName?: string;
};

export type Invoice = {
  id: number;
  invoiceNumber: string;
  planName: string;
  planType?: string;
  invoiceUrl?: string;
  amount: number;
  taxAmount: number;
  totalAmount: number;
  status: string;
  createdAt: string;
};

export type MembershipPlan = {
  id: number;
  name: string;
  type: string;
  price: number;
  originalPrice: number;
  durationDays: number;
  discountPercent: number;
  prioritySlots: boolean;
  featuredListing: boolean;
  description: string;
};
