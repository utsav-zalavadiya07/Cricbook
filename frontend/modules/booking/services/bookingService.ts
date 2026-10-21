import api from "@/services/api";
import { BatchBookingRequest, BookingRequest } from "../types/booking.types";

export const createBooking = (data: BookingRequest) =>
  api.post("/bookings", data);

export const createBatchBooking = (data: BatchBookingRequest) =>
  api.post("/bookings/batch", data);

export const getMyBookings = () =>
  api.get("/bookings/my");

export const getBookingById = (id: number) =>
  api.get(`/bookings/${id}`);

// Backend uses POST /{id}/cancel (not PATCH)
export const cancelBooking = (id: number) =>
  api.post(`/bookings/${id}/cancel`, null, {
    notification: { error: false },
  });
