import { Booking } from "@/modules/booking/types/booking.types";

export type Frequency = "WEEKLY" | "BIWEEKLY" | "MONTHLY" | "CUSTOM";

export interface FrequentBooking {
  sourceBookingId: number;
  turfId: number;
  turfName: string;
  city?: string;
  slotId: number;
  startTime?: string;
  endTime?: string;
  amount?: number;
  timesBooked: number;
  lastBookedDate?: string;
  nextSuggestedDate?: string;
  recommendationReason?: string;
}

export interface BookingPrediction {
  turfId?: number;
  turfName?: string;
  slotId?: number;
  predictedDate?: string;
  startTime?: string;
  endTime?: string;
  confidence?: number;
  reason?: string;
}

export interface RecurringBooking {
  id: number;
  turfId: number;
  turfName: string;
  slotId: number;
  startTime?: string;
  endTime?: string;
  teamId?: number;
  frequency: Frequency;
  preferredDay?: string;
  nextBookingDate?: string;
  autoConfirm?: boolean;
  status: "ACTIVE" | "PAUSED" | "CANCELLED" | string;
}

export interface BookingTemplate {
  id: number;
  templateName: string;
  turfId: number;
  turfName: string;
  teamId?: number;
  preferredDay?: string;
  preferredTime?: string;
  durationMinutes?: number;
  players?: number;
  status?: string;
}

export interface AutoWaitlist {
  id: number;
  slotId: number;
  turfName?: string;
  preferredDate: string;
  autoBookEnabled: boolean;
  status: "ACTIVE" | "PAUSED" | "FULFILLED" | "CANCELLED" | string;
}

export interface BookingIntelligence {
  frequentBookings: number;
  recurringBookings: number;
  templates: number;
  autoWaitlists: number;
  conflicts: number;
  rebookReadiness: number;
  topFrequentBookings: FrequentBooking[];
  predictions: BookingPrediction[];
}

export interface RebookDashboard {
  frequent: FrequentBooking[];
  predictions: BookingPrediction[];
  recurring: RecurringBooking[];
  templates: BookingTemplate[];
  autoWaitlists: AutoWaitlist[];
  analytics: BookingIntelligence | null;
  recentBookings: Booking[];
}
