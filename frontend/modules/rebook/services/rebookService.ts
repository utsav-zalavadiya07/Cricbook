import api from "@/services/api";
import { Booking } from "@/modules/booking/types/booking.types";
import {
  AutoWaitlist,
  BookingIntelligence,
  BookingPrediction,
  BookingTemplate,
  FrequentBooking,
  Frequency,
  RecurringBooking,
} from "../types/rebook.types";

export const rebookBooking = (bookingId: number, targetDate?: string) =>
  api.post<Booking>("/bookings/rebook", { bookingId, targetDate });

export const getFrequentBookings = () => api.get<FrequentBooking[]>("/bookings/frequent");

export const getBookingPredictions = () => api.get<BookingPrediction[]>("/bookings/predictions");

export const getRecurringBookings = () => api.get<RecurringBooking[]>("/bookings/recurring");

export const createRecurringBooking = (payload: {
  turfId: number;
  slotId: number;
  teamId?: number;
  frequency: Frequency;
  preferredDay?: string;
  nextBookingDate: string;
  autoConfirm?: boolean;
}) => api.post<RecurringBooking>("/bookings/recurring", payload);

export const getBookingTemplates = () => api.get<BookingTemplate[]>("/bookings/templates");

export const createBookingTemplate = (payload: {
  templateName: string;
  turfId: number;
  teamId?: number;
  preferredDay?: string;
  preferredTime?: string;
  durationMinutes?: number;
  players?: number;
}) => api.post<BookingTemplate>("/bookings/templates", payload);

export const getAutoWaitlists = () => api.get<AutoWaitlist[]>("/bookings/auto-waitlist");

export const createAutoWaitlist = (payload: {
  slotId: number;
  preferredDate: string;
  autoBookEnabled?: boolean;
}) => api.post<AutoWaitlist>("/bookings/auto-waitlist", payload);

export const getBookingIntelligence = () => api.get<BookingIntelligence>("/bookings/intelligence");

export const getOwnerBookingIntelligence = () => api.get<BookingIntelligence>("/bookings/owner/intelligence");

export const getAdminBookingIntelligence = () => api.get<BookingIntelligence>("/bookings/admin/intelligence");
