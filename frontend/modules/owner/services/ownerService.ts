import api from "@/services/api";
import { Booking } from "@/modules/booking/types/booking.types";
import { Review } from "@/modules/review/types/review.types";

export type OwnerBookingFilters = {
  status?: string;
  date?: string;
  turfId?: number | string;
  query?: string;
};

export type OwnerBookingTimelineItem = {
  id: number;
  bookingId: number;
  eventType: string;
  performedBy: string;
  details: string;
  createdAt?: string;
};

export type OwnerBookingSummary = {
  totalBookings: number;
  todayBookings: number;
  upcomingBookings: number;
  activeMatches: number;
  pendingArrivals: number;
  checkedInToday: number;
  completedBookings: number;
  cancelledBookings: number;
  noShowBookings: number;
  todayRevenue: number;
  attendanceRate: number;
  noShowRate: number;
  recentActivity: OwnerBookingTimelineItem[];
};

export type OwnerBookingDetails = {
  booking: Booking;
  latestPaymentStatus?: string;
  latestCancellationStatus?: string;
  timeline: OwnerBookingTimelineItem[];
};

export type OwnerFinanceFilters = {
  start?: string;
  end?: string;
  turfId?: number | string;
};

export type OwnerFinanceDashboard = {
  todayRevenue: number;
  monthlyRevenue: number;
  lifetimeRevenue: number;
  grossRevenue: number;
  refundedAmount: number;
  netCollectedRevenue: number;
  commissionRate: number;
  commissionDue: number;
  gstRate: number;
  gstOnCommission: number;
  netPayable: number;
  pendingSettlementAmount: number;
  paidSettlementAmount: number;
  paidBookings: number;
  refundedPayments: number;
  pendingSettlements: number;
  paidSettlements: number;
  revenueTrend: Array<{ date: string; revenue: number; payments: number }>;
  turfRevenue: Array<{ turfId: number; turfName: string; revenue: number; paidBookings: number; averageBookingValue: number }>;
  settlements: Array<{
    id: number;
    bookingId?: number;
    grossAmount: number;
    commission: number;
    netAmount: number;
    status: string;
    reference?: string;
    createdAt?: string;
    updatedAt?: string;
  }>;
  transactions: Array<{
    paymentId: number;
    bookingId?: number;
    turfId?: number;
    turfName?: string;
    playerName?: string;
    amount: number;
    refundedAmount: number;
    status: string;
    paidAt?: string;
  }>;
  insight: string;
};

const cleanParams = (filters?: OwnerBookingFilters) =>
  Object.fromEntries(
    Object.entries(filters ?? {}).filter(([, value]) => value !== undefined && value !== null && value !== "")
  );

const cleanFinanceParams = (filters?: OwnerFinanceFilters) =>
  Object.fromEntries(
    Object.entries(filters ?? {}).filter(([, value]) => value !== undefined && value !== null && value !== "")
  );

export const getOwnerBookings = (filters?: string | OwnerBookingFilters) => {
  const params = typeof filters === "string" ? { status: filters } : cleanParams(filters);
  return api.get<Booking[]>("/bookings/owner", { params });
};

export const getOwnerBookingSummary = () => api.get<OwnerBookingSummary>("/bookings/owner/summary");

export const getOwnerBookingDetails = (bookingId: number) =>
  api.get<OwnerBookingDetails>(`/bookings/owner/${bookingId}/details`);

export const getOwnerReviews = () => api.get<Review[]>("/reviews/owner");

export const getOwnerTurfs = () => api.get("/turfs/my");

export const getOwnerDashboard = (days?: number) =>
  api.get("/owner/dashboard", { params: days ? { days } : undefined });

export const getOwnerFinanceDashboard = (filters?: OwnerFinanceFilters) =>
  api.get<OwnerFinanceDashboard>("/owner/finance/dashboard", { params: cleanFinanceParams(filters) });

export const generateOwnerSettlements = () =>
  api.post<{ created: number; commissionRate: number; dashboard: OwnerFinanceDashboard }>("/owner/finance/settlements/generate");

export const exportOwnerFinanceCsv = (filters?: OwnerFinanceFilters) =>
  api.get<Blob>("/owner/finance/export", { params: cleanFinanceParams(filters), responseType: "blob" });

export const cancelOwnerBooking = (bookingId: number) =>
  api.post(`/bookings/owner/${bookingId}/cancel`);

export const checkInOwnerBooking = (bookingId: number, note?: string) =>
  api.post<Booking>(`/bookings/owner/${bookingId}/check-in`, { note });

export const completeOwnerBooking = (bookingId: number, note?: string) =>
  api.post<Booking>(`/bookings/owner/${bookingId}/complete`, { note });

export const markOwnerNoShow = (bookingId: number, reason?: string) =>
  api.post<Booking>(`/bookings/owner/${bookingId}/no-show`, { reason });

export const addOwnerBookingNote = (bookingId: number, note: string) =>
  api.post<OwnerBookingTimelineItem>(`/bookings/owner/${bookingId}/notes`, { note });

export const triggerRefund = (bookingId: number) =>
  api.post(`/payments/refund/${bookingId}`);
