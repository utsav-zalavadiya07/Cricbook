export interface Turf {
  id?: number;
  name: string;
  description?: string;
  location: string;
  city: string;
  capacity?: number;
  latitude?: number;
  longitude?: number;
  pricePerHour: number;
  pitchType: "TURF" | "CONCRETE" | "MATTING";
  boxType: "FIVE_V_FIVE" | "SIX_V_SIX" | "SEVEN_V_SEVEN" | "ELEVEN_V_ELEVEN";
  status?: "ACTIVE" | "INACTIVE" | "PENDING" | "MAINTENANCE";
  rating?: number;
  totalReviews?: number;
  images?: string[];
  amenities?: Array<TurfAmenity | string>;
  sport?: string;
  sports?: string[];
  state?: string;
  country?: string;
  timezone?: string;
  holidaySource?: string;
}

export interface TurfAmenity {
  id?: number;
  amenity: string;
}

export interface TurfSport {
  id?: number;
  name: string;
  code: string;
  icon?: string;
  active?: boolean;
}

export interface SlotRequest {
  startTime: string;
  endTime: string;
  slotDuration: number;
  price: number;
  replaceExisting?: boolean;
  fullDay?: boolean;
}

export interface Slot {
  id: number;
  startTime: string;
  endTime: string;
  price: number;
  isActive: boolean;
}
