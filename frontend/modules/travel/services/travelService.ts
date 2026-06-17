import api from "@/services/api";

export type TravelPackage = {
  id: number;
  packageCode: string;
  title: string;
  sport: string;
  originCity?: string;
  destination: string;
  packageType: string;
  startDate: string;
  endDate: string;
  cancellationDeadline?: string;
  pricePerPerson: number;
  capacity: number;
  bookedCount: number;
  availableCapacity: number;
  includesAccommodation: boolean;
  includesTransport: boolean;
  linkedTournamentId?: number;
  partnerName?: string;
  status: string;
  itinerary?: string;
};

export type TravelEvent = {
  id: number;
  title: string;
  eventType: "EVENT" | "CAMP" | "EXPERIENCE" | "FESTIVAL" | string;
  sport: string;
  city: string;
  venue?: string;
  startsAt: string;
  endsAt: string;
  capacity: number;
  registeredCount: number;
  availableCapacity: number;
  basePrice: number;
  status: string;
  description?: string;
  ticketTypes: TravelTicketType[];
};

export type TravelTicketType = {
  id: number;
  eventId: number;
  name: string;
  price: number;
  totalInventory: number;
  soldInventory: number;
  availableInventory: number;
  active: boolean;
};

export type TravelAccommodation = {
  id: number;
  partnerName?: string;
  name: string;
  accommodationType: string;
  city: string;
  roomType: string;
  pricePerNight: number;
  totalRooms: number;
  bookedRooms: number;
  availableRooms: number;
  status: string;
};

export const getTravelPackages = (params?: {
  query?: string;
  sport?: string;
  originCity?: string;
  destination?: string;
  maxPrice?: number;
}) => api.get<TravelPackage[]>("/travel/packages", { params });

export const getTravelEvents = (params?: {
  eventType?: "EVENT" | "CAMP" | "EXPERIENCE" | "FESTIVAL";
  query?: string;
  sport?: string;
  city?: string;
}) => api.get<TravelEvent[]>("/travel/events", { params });

export const getTravelAccommodations = (params?: { city?: string; accommodationType?: string }) =>
  api.get<TravelAccommodation[]>("/travel/accommodations", { params });

export const bookTravelPackage = (packageId: number, payload: {
  bookingType?: string;
  participantCount?: number;
  travelerNames?: string;
  couponCode?: string;
  paymentReference?: string;
}) => api.post(`/travel/packages/${packageId}/book`, payload);

export const buyTravelTicket = (eventId: number, payload: { ticketTypeId: number; quantity?: number }) =>
  api.post(`/travel/events/${eventId}/tickets`, payload);

export const bookTravelAccommodation = (accommodationId: number, payload: { checkIn: string; checkOut: string; rooms: number }) =>
  api.post(`/travel/accommodations/${accommodationId}/book`, payload);
