export interface BookingRequest {
  turfId: number;
  slotId: number;
  bookingDate: string;
}

export interface BatchBookingRequest {
  turfId: number;
  slotIds: number[];
  bookingDate: string;
}

export interface Booking {
  id: number;
  playerId?: number;
  playerName?: string;
  playerEmail?: string;
  playerPhone?: string;
  turfId: number;
  turfName?: string;
  turfCity?: string;
  turfLocation?: string;
  slotId: number;
  slotIds?: number[];
  slotCount?: number;
  startTime?: string;
  endTime?: string;
  bookingDate: string;
  amount?: number;
  totalAmount?: number;
  status: "PENDING" | "CONFIRMED" | "CHECKED_IN" | "COMPLETED" | "CANCELLED" | "EXPIRED" | "NO_SHOW" | "REFUNDED";
  paymentStatus?: "PENDING" | "PAID" | "FAILED" | "REFUNDED";
  attendanceStatus?: "ARRIVED" | "LATE" | "NO_SHOW" | "CANCELLED";
  checkedInAt?: string;
  createdAt?: string;
}
