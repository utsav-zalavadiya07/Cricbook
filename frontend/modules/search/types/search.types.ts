export interface SearchFilters {
  query?: string;
  city?: string;
  date?: string;
  minPrice?: number;
  maxPrice?: number;
  boxType?: string;
  pitchType?: string;
  sport?: string;
  sportCode?: string;
  rating?: number | string;
  amenities?: string[];
  sortBy?: string;
  latitude?: number;
  longitude?: number;
  radius?: number;
  page?: number;
  size?: number;
}

export interface SearchTurf {
  id: number;
  name: string;
  city: string;
  location: string;
  rating: number;
  totalReviews: number;
  pricePerHour: number;
  boxType: string;
  pitchType: string;
  sport?: string;
  sports?: string[];
  amenities?: string[];
  status: string;
  images?: string[];
  distance?: number;
  available?: boolean;
}

export interface SearchSport {
  id?: number;
  name: string;
  code: string;
  icon?: string;
  active?: boolean;
}

export interface SlotAvailability {
  slotId: number;
  startTime: string;
  endTime: string;
  price: number;
  available: boolean;
  blocked: boolean;
  locked?: boolean;
  status?: "AVAILABLE" | "LOCKED" | "BOOKED" | "BLOCKED" | "EXPIRED" | "UNAVAILABLE" | string;
}
