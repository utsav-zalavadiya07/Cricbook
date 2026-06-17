import api from "@/services/api";

export type AnalyticsPoint = {
  date: string;
  bookings: number;
  revenue: number;
};

export type OwnerExpense = {
  id: number;
  type: string;
  amount: number;
  expenseDate: string;
  note?: string;
};

export type OwnerInsights = {
  revenue: {
    todayRevenue: number;
    weekRevenue: number;
    lastWeekRevenue: number;
    monthRevenue: number;
    averageBookingValue: number;
    weeklyGrowthPercent: number;
    dailyTrend: AnalyticsPoint[];
  };
  occupancy: {
    occupancyPercent: number;
    bookedSlots: number;
    availableSlotCapacity: number;
    unusedSlotCapacity: number;
    peakUtilizationPercent: number;
  };
  heatmap: Array<{
    dayOfWeek: string;
    hour: number;
    hourLabel: string;
    bookingCount: number;
    utilizationPercent: number;
    intensity: "LOW" | "MEDIUM" | "HIGH" | string;
  }>;
  customers: Array<{
    customerId: number;
    customerName: string;
    bookings: number;
    lifetimeValue: number;
    lastVisit: string;
    segment: string;
    trustScore: number;
  }>;
  pricingSuggestions: Array<{
    turfId: number;
    turfName: string;
    slotId: number;
    currentPrice: number;
    recommendedPrice: number;
    reason: string;
    confidence: number;
  }>;
  bookingPatterns: Array<{ label: string; metric: string; insight: string; direction: string }>;
  profit: { revenue: number; expenses: number; profit: number; marginPercent: number };
  forecast: { period: string; expectedRevenue: number; expectedOccupancy: number; demandLevel: string; insight: string };
  promotions: Array<{ title: string; description: string; suggestedDiscountPercent: number; expectedImpact: string }>;
  community: { repeatCustomers: number; frequentCaptains: number; activeTeams: number; communityScore: string };
  coach: { answer: string; recommendedActions: string[]; confidence: number };
  recentExpenses: OwnerExpense[];
};

export const getOwnerInsights = () => api.get<OwnerInsights>("/owner/insights");
export const getAdminOwnerInsights = () => api.get<OwnerInsights>("/admin/owner-insights");
export const addOwnerExpense = (payload: { type: string; amount: number; expenseDate?: string; note?: string }) =>
  api.post<OwnerExpense>("/owner/insights/expenses", payload);
export const askBusinessCoach = (question: string) =>
  api.post<OwnerInsights["coach"]>("/owner/insights/coach", { question });
